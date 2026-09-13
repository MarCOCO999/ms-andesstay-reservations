package com.andesstay.ms_andesstay_reservations.web;

public class PaymentAlreadyProcessedException extends RuntimeException {

    public PaymentAlreadyProcessedException(Long reservationId) {
        super("La reserva " + reservationId + " ya fue pagada");
    }
}
