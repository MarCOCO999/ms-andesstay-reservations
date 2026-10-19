package com.andesstay.ms_andesstay_reservations.messaging.rabbit;

import com.andesstay.ms_andesstay_reservations.config.MessagingProperties;
import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Publica los 3 comandos que ms-andesstay-notify consume, via el exchange directo cmd.direct. */
@Component
public class ReservationCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationCommandPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public ReservationCommandPublisher(RabbitTemplate rabbitTemplate, MessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void sendEmail(Reservation reservation, String template) {
        EmailCommand command = new EmailCommand(
                "email." + template,
                UUID.randomUUID().toString(),
                Instant.now(),
                UUID.randomUUID().toString(),
                reservation.getId().toString(),
                reservation.getId().toString(),
                reservation.getGuestEmail(),
                template,
                Map.of("reservationId", reservation.getId().toString(), "estado", reservation.getStatus().name())
        );
        rabbitTemplate.convertAndSend(properties.exchanges().direct(), properties.routingKeys().email(), command);
        log.info("Comando email '{}' encolado para reserva {}", template, reservation.getId());
    }

    public void sendHousekeepingTicket(Reservation reservation, String instructions) {
        HousekeepingTicketCommand command = new HousekeepingTicketCommand(
                "housekeeping.ticket",
                UUID.randomUUID().toString(),
                Instant.now(),
                UUID.randomUUID().toString(),
                reservation.getId().toString(),
                reservation.getId().toString(),
                reservation.getUnitId(),
                instructions
        );
        rabbitTemplate.convertAndSend(properties.exchanges().direct(), properties.routingKeys().housekeeping(), command);
        log.info("Ticket de housekeeping encolado para unidad {} (reserva {})", reservation.getUnitId(), reservation.getId());
    }

    public void sendVoucher(Reservation reservation) {
        VoucherCommand command = new VoucherCommand(
                "voucher.gen",
                UUID.randomUUID().toString(),
                Instant.now(),
                UUID.randomUUID().toString(),
                reservation.getId().toString(),
                reservation.getId().toString(),
                reservation.getGuestEmail()
        );
        rabbitTemplate.convertAndSend(properties.exchanges().direct(), properties.routingKeys().voucher(), command);
        log.info("Comando de voucher encolado para reserva {}", reservation.getId());
    }
}
