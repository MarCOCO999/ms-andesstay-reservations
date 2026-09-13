package com.andesstay.ms_andesstay_reservations.messaging.rabbit;

import java.time.Instant;

/** Mismo contrato que consume ms-andesstay-notify desde q.cmd.housekeeping. */
public record HousekeepingTicketCommand(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        Long unitId,
        String instructions
) {
}
