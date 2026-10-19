package com.andesstay.ms_andesstay_reservations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Exchanges y routing keys de los comandos que publica reservations (andesstay.messaging). */
@ConfigurationProperties(prefix = "andesstay.messaging")
public record MessagingProperties(Exchanges exchanges, RoutingKeys routingKeys) {

    public record Exchanges(String direct, String topic) {
    }

    public record RoutingKeys(String email, String housekeeping, String voucher) {
    }
}
