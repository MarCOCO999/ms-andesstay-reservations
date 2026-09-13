package com.andesstay.ms_andesstay_reservations.web;

public class InvalidReservationRequestException extends RuntimeException {

    public InvalidReservationRequestException(String message) {
        super(message);
    }
}
