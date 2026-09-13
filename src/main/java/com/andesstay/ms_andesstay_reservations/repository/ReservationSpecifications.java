package com.andesstay.ms_andesstay_reservations.repository;

import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import com.andesstay.ms_andesstay_reservations.domain.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Filtra por el rango de fecha de check-in solicitado (from/to del endpoint de listado). */
    public static Specification<Reservation> checkInFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("checkInDate"), from);
    }

    public static Specification<Reservation> checkInTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("checkInDate"), to);
    }

    public static Specification<Reservation> belongsToGuest(String guestId) {
        return (root, query, cb) -> cb.equal(root.get("guestId"), guestId);
    }
}
