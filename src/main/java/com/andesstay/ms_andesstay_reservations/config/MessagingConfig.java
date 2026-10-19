package com.andesstay.ms_andesstay_reservations.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    /**
     * Spring Boot 4 usa Jackson 3 por defecto, por eso el converter es JacksonJsonMessageConverter
     * (Jackson 3 nativo) y no el Jackson2JsonMessageConverter legado. Al declarar este bean,
     * Spring Boot lo usa automaticamente en el RabbitTemplate autoconfigurado.
     */
    @Bean
    public MessageConverter rabbitJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Mensajes no entregados del lado del productor (requiere publisher-confirm-type y
     * publisher-returns en application.yml): un return significa que el exchange no tenia
     * ninguna cola enlazada para esa routing key (p.ej. notify aun no declaro su topologia), y
     * un confirm negativo que el broker no acepto el mensaje. Ambos quedan registrados en el log.
     */
    @Bean
    public RabbitTemplateCustomizer deliveryFailureLogging() {
        return template -> {
            template.setReturnsCallback(returned -> log.error(
                    "Comando no enrutable: exchange={} routingKey={} reply={} {}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText()));
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (!ack) {
                    log.error("El broker rechazo un comando (correlation={}): {}", correlation, cause);
                }
            });
        };
    }
}
