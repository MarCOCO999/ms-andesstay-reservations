package com.andesstay.ms_andesstay_reservations.messaging.kafka;

import java.time.Instant;

/** Mismo contrato que consumen ms-andesstay-audit y ms-andesstay-report desde reservations.events. */
public record ReservationEvent(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        String previousStatus,
        String newStatus,
        String actorId,
        String actorRole,
        Long unitId
) {
}
