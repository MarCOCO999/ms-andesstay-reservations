package com.andesstay.ms_andesstay_reservations.service;

import com.andesstay.ms_andesstay_reservations.client.CatalogClient;
import com.andesstay.ms_andesstay_reservations.domain.PaymentMethod;
import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import com.andesstay.ms_andesstay_reservations.domain.ReservationStateMachine;
import com.andesstay.ms_andesstay_reservations.domain.ReservationStatus;
import com.andesstay.ms_andesstay_reservations.messaging.kafka.ReservationEventPublisher;
import com.andesstay.ms_andesstay_reservations.messaging.rabbit.ReservationCommandPublisher;
import com.andesstay.ms_andesstay_reservations.repository.ReservationRepository;
import com.andesstay.ms_andesstay_reservations.repository.ReservationSpecifications;
import com.andesstay.ms_andesstay_reservations.web.InvalidReservationRequestException;
import com.andesstay.ms_andesstay_reservations.web.PaymentAlreadyProcessedException;
import com.andesstay.ms_andesstay_reservations.web.ReservationNotFoundException;
import com.andesstay.ms_andesstay_reservations.web.dto.CreateReservationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    /** Estados en los que la unidad ya esta efectivamente ocupada/comprometida en catalog. */
    private static final Set<ReservationStatus> UNIT_RESERVED_STATUSES =
            Set.of(ReservationStatus.CONFIRMADA, ReservationStatus.CHECKIN_PENDIENTE, ReservationStatus.EN_ESTADIA);

    private final ReservationRepository repository;
    private final ReservationEventPublisher eventPublisher;
    private final ReservationCommandPublisher commandPublisher;
    private final CatalogClient catalogClient;

    public ReservationService(ReservationRepository repository,
                               ReservationEventPublisher eventPublisher,
                               ReservationCommandPublisher commandPublisher,
                               CatalogClient catalogClient) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.commandPublisher = commandPublisher;
        this.catalogClient = catalogClient;
    }

    @Transactional
    public Reservation create(CreateReservationRequest request, String guestId, String guestEmail, String actorRole, String bearerToken) {
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new InvalidReservationRequestException("La fecha de checkout debe ser posterior a la fecha de check-in");
        }
        // La tarifa se consulta a catalog (fuente de verdad), nunca se confia en un monto
        // que pudiera venir del frontend.
        BigDecimal nightlyRate = catalogClient.getNightlyRate(request.unitId(), bearerToken);
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalAmount = nightlyRate.multiply(BigDecimal.valueOf(nights));

        Reservation reservation = Reservation.builder()
                .guestId(guestId)
                .guestEmail(guestEmail)
                .unitId(request.unitId())
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .guestCount(request.guestCount())
                .status(ReservationStatus.CREADA)
                .totalAmount(totalAmount)
                .paymentTiming(request.paymentTiming())
                .paid(false)
                .createdBy(guestId)
                .build();
        reservation = repository.save(reservation);
        eventPublisher.publish(reservation, "reservation.created", null, guestId, actorRole);
        return reservation;
    }

    @Transactional(readOnly = true)
    public Reservation findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
    }

    /**
     * Procesa el pago a traves de la pasarela simulada: el Cliente solo puede pagar sus propias
     * reservas; Operador/Admin pueden cobrar cualquiera (p.ej. en efectivo en el mesón al check-in).
     * No hay integracion real con ningun procesador: solo se genera un identificador de transaccion
     * ficticio para simular un comprobante.
     */
    @Transactional
    public Reservation pay(Long id, String actorId, boolean isGuestOnly, PaymentMethod method) {
        Reservation reservation = findById(id);
        if (isGuestOnly && !reservation.getGuestId().equals(actorId)) {
            throw new AccessDeniedException("No puedes pagar una reserva de otro huesped");
        }
        if (reservation.isPaid()) {
            throw new PaymentAlreadyProcessedException(id);
        }
        reservation.setPaid(true);
        reservation.setPaymentMethod(method);
        reservation.setTransactionId("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        reservation.setPaidAt(Instant.now());
        return repository.save(reservation);
    }

    @Transactional
    public Reservation updateStatus(Long id, ReservationStatus targetStatus, String actorId, String actorRole, String bearerToken) {
        Reservation reservation = findById(id);
        ReservationStatus previousStatus = reservation.getStatus();
        ReservationStateMachine.assertTransitionAllowed(previousStatus, targetStatus);

        // Se reserva la unidad ANTES de persistir el cambio de estado: si catalog no tiene
        // cupo (409), la excepcion aborta la transaccion y la reserva se queda en su estado
        // anterior, en vez de quedar "CONFIRMADA" sin una unidad realmente asignada.
        if (previousStatus == ReservationStatus.CREADA && targetStatus == ReservationStatus.CONFIRMADA) {
            catalogClient.reserveUnit(reservation.getUnitId(), bearerToken);
        }

        reservation.setStatus(targetStatus);
        reservation = repository.save(reservation);

        eventPublisher.publish(reservation, "reservation." + targetStatus.name().toLowerCase(),
                previousStatus.name(), actorId, actorRole);
        dispatchNotifications(reservation, previousStatus, targetStatus);
        releaseUnitIfApplicable(reservation, previousStatus, targetStatus, bearerToken);
        return reservation;
    }

    /**
     * Libera la unidad cuando el ciclo de la reserva termina (checkout) o se cancela
     * estando ya confirmada. A diferencia de la reserva, esto es best-effort: si catalog
     * no responde no se revierte el cambio de estado local (ya se le notifico al huesped),
     * solo se deja registrado en el log para reconciliar manualmente si hace falta.
     */
    private void releaseUnitIfApplicable(Reservation reservation, ReservationStatus previousStatus,
                                          ReservationStatus targetStatus, String bearerToken) {
        boolean wasReserved = UNIT_RESERVED_STATUSES.contains(previousStatus);
        boolean releases = targetStatus == ReservationStatus.CHECKOUT || targetStatus == ReservationStatus.CANCELADA;
        if (wasReserved && releases) {
            try {
                catalogClient.releaseUnit(reservation.getUnitId(), bearerToken);
            } catch (Exception ex) {
                log.warn("No se pudo liberar en catalog la unidad {} (reserva {}): {}",
                        reservation.getUnitId(), reservation.getId(), ex.getMessage());
            }
        }
    }

    /** Mapea las transiciones del caso (seccion 8) a los comandos que consume ms-andesstay-notify. */
    private void dispatchNotifications(Reservation reservation, ReservationStatus previousStatus, ReservationStatus newStatus) {
        if (previousStatus == ReservationStatus.CREADA && newStatus == ReservationStatus.CONFIRMADA) {
            commandPublisher.sendEmail(reservation, "confirmacion-reserva");
            commandPublisher.sendVoucher(reservation);
        } else if (previousStatus == ReservationStatus.CONFIRMADA && newStatus == ReservationStatus.CHECKIN_PENDIENTE) {
            commandPublisher.sendHousekeepingTicket(reservation, "Preparar unidad " + reservation.getUnitId() + " para check-in");
            commandPublisher.sendEmail(reservation, "recordatorio-checkin");
        } else if (previousStatus == ReservationStatus.EN_ESTADIA && newStatus == ReservationStatus.CHECKOUT) {
            commandPublisher.sendEmail(reservation, "checkout");
        }
    }

    @Transactional(readOnly = true)
    public List<Reservation> search(ReservationStatus status, LocalDate from, LocalDate to, String guestIdFilter) {
        Specification<Reservation> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and(ReservationSpecifications.hasStatus(status));
        }
        if (from != null) {
            spec = spec.and(ReservationSpecifications.checkInFrom(from));
        }
        if (to != null) {
            spec = spec.and(ReservationSpecifications.checkInTo(to));
        }
        if (guestIdFilter != null) {
            spec = spec.and(ReservationSpecifications.belongsToGuest(guestIdFilter));
        }
        return repository.findAll(spec);
    }
}
