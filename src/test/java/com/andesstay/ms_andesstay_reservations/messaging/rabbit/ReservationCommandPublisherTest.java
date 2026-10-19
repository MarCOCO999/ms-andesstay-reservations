package com.andesstay.ms_andesstay_reservations.messaging.rabbit;

import com.andesstay.ms_andesstay_reservations.config.MessagingProperties;
import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationCommandPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private ReservationCommandPublisher publisher;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        MessagingProperties properties = new MessagingProperties(
                new MessagingProperties.Exchanges("cmd.direct", "cmd.topic"),
                new MessagingProperties.RoutingKeys("email.send", "housekeeping.ticket", "voucher.gen"));
        publisher = new ReservationCommandPublisher(rabbitTemplate, properties);

        reservation = mock(Reservation.class);
        when(reservation.getId()).thenReturn(15L);
        when(reservation.getGuestEmail()).thenReturn("huesped@correo.cl");
        when(reservation.getUnitId()).thenReturn(3L);
    }

    @Test
    void emailSePublicaEnElExchangeDirectoConSuRoutingKey() {
        when(reservation.getStatus()).thenReturn(com.andesstay.ms_andesstay_reservations.domain.ReservationStatus.CONFIRMADA);

        publisher.sendEmail(reservation, "confirmacion-reserva");

        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), any(Object.class));
    }

    @Test
    void voucherYHousekeepingUsanSusPropiasRoutingKeys() {
        publisher.sendVoucher(reservation);
        publisher.sendHousekeepingTicket(reservation, "Preparar unidad");

        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("voucher.gen"), any(Object.class));
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("housekeeping.ticket"), any(Object.class));
    }
}
