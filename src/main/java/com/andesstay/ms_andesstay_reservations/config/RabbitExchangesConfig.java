package com.andesstay.ms_andesstay_reservations.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * reservations es productor de los comandos que ms-andesstay-notify consume (ver su
 * RabbitTopologyConfig para el detalle completo de colas/DLQ, que notify es quien declara).
 * Aqui solo se declaran los exchanges (cmd.direct / cmd.topic) para poder publicar sin
 * depender del orden de arranque entre servicios; declarar un exchange ya existente con
 * la misma configuracion es una operacion idempotente en RabbitMQ.
 */
@Configuration
public class RabbitExchangesConfig {

    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_CMD_TOPIC = "cmd.topic";

    public static final String ROUTING_EMAIL = "email.send";
    public static final String ROUTING_HOUSEKEEPING = "housekeeping.ticket";
    public static final String ROUTING_VOUCHER = "voucher.gen";

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }
}
