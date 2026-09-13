package com.andesstay.ms_andesstay_reservations.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ciclo de vida de una reserva de hospedaje (ver caso AndesStay, seccion 3).
 * CREADA -> CONFIRMADA -> CHECKIN_PENDIENTE -> EN_ESTADIA -> CHECKOUT, con CANCELADA
 * como salida posible desde cualquier estado no terminal.
 * El nombre EN_ESTADIA se mantiene sin tilde en el codigo fuente por seguridad de
 * encoding entre entornos, pero se serializa/deserializa como "EN_ESTADÍA" en el JSON
 * para respetar el contrato de la API definido en el caso.
 */
public enum ReservationStatus {
    CREADA,
    CONFIRMADA,
    CHECKIN_PENDIENTE,
    @JsonProperty("EN_ESTADÍA") EN_ESTADIA,
    CHECKOUT,
    CANCELADA
}
