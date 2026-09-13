package com.andesstay.ms_andesstay_reservations.domain;

import com.andesstay.ms_andesstay_reservations.web.InvalidStatusTransitionException;

import java.util.Map;
import java.util.Set;

/**
 * Regla clave del caso: "No se puede hacer check-in sin CONFIRMAR". En general, solo se
 * permiten las transiciones hacia adelante del ciclo de vida, o una cancelacion mientras
 * la reserva no este en un estado terminal.
 */
public final class ReservationStateMachine {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReservationStatus.CREADA, Set.of(ReservationStatus.CONFIRMADA, ReservationStatus.CANCELADA),
            ReservationStatus.CONFIRMADA, Set.of(ReservationStatus.CHECKIN_PENDIENTE, ReservationStatus.CANCELADA),
            ReservationStatus.CHECKIN_PENDIENTE, Set.of(ReservationStatus.EN_ESTADIA, ReservationStatus.CANCELADA),
            ReservationStatus.EN_ESTADIA, Set.of(ReservationStatus.CHECKOUT),
            ReservationStatus.CHECKOUT, Set.of(),
            ReservationStatus.CANCELADA, Set.of()
    );

    private ReservationStateMachine() {
    }

    public static void assertTransitionAllowed(ReservationStatus current, ReservationStatus target) {
        Set<ReservationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
    }
}
