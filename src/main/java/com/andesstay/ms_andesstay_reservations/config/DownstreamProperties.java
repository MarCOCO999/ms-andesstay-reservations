package com.andesstay.ms_andesstay_reservations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "andesstay.downstream")
public record DownstreamProperties(String catalogUrl) {
}
