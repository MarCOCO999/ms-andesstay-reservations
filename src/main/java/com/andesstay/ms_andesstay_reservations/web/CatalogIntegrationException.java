package com.andesstay.ms_andesstay_reservations.web;

/** El microservicio de catalogo no respondio correctamente a una llamada de reserve/release. */
public class CatalogIntegrationException extends RuntimeException {

    public CatalogIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
