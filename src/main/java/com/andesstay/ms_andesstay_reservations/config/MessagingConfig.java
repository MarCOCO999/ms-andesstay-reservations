package com.andesstay.ms_andesstay_reservations.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    /**
     * Spring Boot 4 usa Jackson 3 por defecto, por eso el converter es JacksonJsonMessageConverter
     * (Jackson 3 nativo) y no el Jackson2JsonMessageConverter legado. Al declarar este bean,
     * Spring Boot lo usa automaticamente en el RabbitTemplate autoconfigurado.
     */
    @Bean
    public MessageConverter rabbitJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
