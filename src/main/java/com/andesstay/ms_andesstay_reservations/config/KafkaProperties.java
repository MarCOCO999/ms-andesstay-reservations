package com.andesstay.ms_andesstay_reservations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "andesstay.kafka")
public record KafkaProperties(String topicReservationsEvents) {
}
