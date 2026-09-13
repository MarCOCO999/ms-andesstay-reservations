package com.andesstay.ms_andesstay_reservations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "RESERVATIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del huesped: el "oid"/"sub" del JWT de Azure AD que creo la reserva. */
    @Column(name = "GUEST_ID", nullable = false, length = 100)
    private String guestId;

    /** Claim "preferred_username" del JWT; usado para las notificaciones (email, voucher). */
    @Column(name = "GUEST_EMAIL", length = 150)
    private String guestEmail;

    /** Referencia a la unidad (habitacion/cabana) del microservicio de catalogo. */
    @Column(name = "UNIT_ID", nullable = false)
    private Long unitId;

    @Column(name = "CHECK_IN_DATE", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "CHECK_OUT_DATE", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "GUEST_COUNT")
    private Integer guestCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private ReservationStatus status;

    /** nightlyRate (obtenido de catalog al crear, no confiado del cliente) * cantidad de noches. */
    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TIMING", nullable = false, length = 20)
    private PaymentTiming paymentTiming;

    /** Se marca al pasar por la pasarela de pago simulada (Cliente) o al cobrar en efectivo (Operador/Admin). */
    @Column(name = "PAID", nullable = false)
    private boolean paid;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD", length = 20)
    private PaymentMethod paymentMethod;

    /** Identificador ficticio generado por la simulacion, solo para mostrar un "comprobante" al usuario. */
    @Column(name = "TRANSACTION_ID", length = 40)
    private String transactionId;

    @Column(name = "PAID_AT")
    private Instant paidAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ReservationStatus.CREADA;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
