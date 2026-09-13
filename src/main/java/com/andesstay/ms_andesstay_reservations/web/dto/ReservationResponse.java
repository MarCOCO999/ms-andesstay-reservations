package com.andesstay.ms_andesstay_reservations.web.dto;

import com.andesstay.ms_andesstay_reservations.domain.PaymentMethod;
import com.andesstay.ms_andesstay_reservations.domain.PaymentTiming;
import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import com.andesstay.ms_andesstay_reservations.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        Long id,
        String guestId,
        Long unitId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guestCount,
        ReservationStatus status,
        BigDecimal totalAmount,
        PaymentTiming paymentTiming,
        boolean paid,
        PaymentMethod paymentMethod,
        String transactionId,
        Instant paidAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getGuestId(), r.getUnitId(), r.getCheckInDate(), r.getCheckOutDate(),
                r.getGuestCount(), r.getStatus(), r.getTotalAmount(), r.getPaymentTiming(), r.isPaid(),
                r.getPaymentMethod(), r.getTransactionId(), r.getPaidAt(),
                r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
