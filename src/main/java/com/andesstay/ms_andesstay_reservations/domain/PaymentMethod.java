package com.andesstay.ms_andesstay_reservations.domain;

/** Metodos que acepta la pasarela de pago simulada (sin integracion real con ningun procesador). */
public enum PaymentMethod {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    TRANSFERENCIA,
    EFECTIVO
}
