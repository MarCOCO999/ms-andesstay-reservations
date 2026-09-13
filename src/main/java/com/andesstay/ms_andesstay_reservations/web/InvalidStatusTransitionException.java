package com.andesstay.ms_andesstay_reservations.web;

import com.andesstay.ms_andesstay_reservations.domain.ReservationStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ReservationStatus current, ReservationStatus target) {
        super("No se puede pasar de " + current + " a " + target);
    }
}
