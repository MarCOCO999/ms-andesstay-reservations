package com.andesstay.ms_andesstay_reservations.messaging.rabbit;

import java.time.Instant;
import java.util.Map;

/** Mismo contrato que consume ms-andesstay-notify desde q.cmd.email. */
public record EmailCommand(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        String guestEmail,
        String template,
        Map<String, String> variables
) {
}
