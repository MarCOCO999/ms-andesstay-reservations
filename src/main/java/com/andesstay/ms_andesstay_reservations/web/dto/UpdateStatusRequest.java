package com.andesstay.ms_andesstay_reservations.web.dto;

import com.andesstay.ms_andesstay_reservations.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ReservationStatus status) {
}
