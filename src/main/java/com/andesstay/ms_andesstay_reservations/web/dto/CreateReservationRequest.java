package com.andesstay.ms_andesstay_reservations.web.dto;

import com.andesstay.ms_andesstay_reservations.domain.PaymentTiming;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull Long unitId,
        @NotNull @FutureOrPresent LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        @Positive Integer guestCount,
        @NotNull PaymentTiming paymentTiming
) {
}
