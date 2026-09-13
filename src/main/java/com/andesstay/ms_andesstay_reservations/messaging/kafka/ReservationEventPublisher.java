package com.andesstay.ms_andesstay_reservations.messaging.kafka;

import com.andesstay.ms_andesstay_reservations.config.KafkaProperties;
import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Fuente de verdad de eventos de la reserva (caso, seccion 9): cada cambio de estado se
 * publica en reservations.events, keyed por reservationId, para que
 * ms-andesstay-audit y ms-andesstay-report lo consuman de forma independiente.
 */
@Component
public class ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public ReservationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(Reservation reservation, String eventType, String previousStatus, String actorId, String actorRole) {
        ReservationEvent event = new ReservationEvent(
                eventType,
                UUID.randomUUID().toString(),
                Instant.now(),
                UUID.randomUUID().toString(),
                reservation.getId().toString(),
                reservation.getId().toString(),
                previousStatus,
                reservation.getStatus().name(),
                actorId,
                actorRole,
                reservation.getUnitId()
        );
        kafkaTemplate.send(kafkaProperties.topicReservationsEvents(), reservation.getId().toString(), event);
        log.info("Evento publicado en {}: reserva {} {} -> {}",
                kafkaProperties.topicReservationsEvents(), reservation.getId(), previousStatus, reservation.getStatus());
    }
}
