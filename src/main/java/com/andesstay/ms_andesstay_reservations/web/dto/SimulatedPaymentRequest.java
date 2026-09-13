package com.andesstay.ms_andesstay_reservations.web.dto;

import com.andesstay.ms_andesstay_reservations.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record SimulatedPaymentRequest(
        @NotNull PaymentMethod method
) {
}
