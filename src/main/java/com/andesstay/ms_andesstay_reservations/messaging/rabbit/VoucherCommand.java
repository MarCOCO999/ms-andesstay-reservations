package com.andesstay.ms_andesstay_reservations.messaging.rabbit;

import java.time.Instant;

/** Mismo contrato que consume ms-andesstay-notify desde q.cmd.voucher. */
public record VoucherCommand(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        String guestEmail
) {
}
