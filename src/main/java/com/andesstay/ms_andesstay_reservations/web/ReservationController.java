package com.andesstay.ms_andesstay_reservations.web;

import com.andesstay.ms_andesstay_reservations.domain.Reservation;
import com.andesstay.ms_andesstay_reservations.domain.ReservationStatus;
import com.andesstay.ms_andesstay_reservations.service.ReservationService;
import com.andesstay.ms_andesstay_reservations.web.dto.CreateReservationRequest;
import com.andesstay.ms_andesstay_reservations.web.dto.ReservationResponse;
import com.andesstay.ms_andesstay_reservations.web.dto.SimulatedPaymentRequest;
import com.andesstay.ms_andesstay_reservations.web.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
                                                        Authentication authentication) {
        Reservation reservation = service.create(request, subjectOf(authentication), emailOf(authentication),
                primaryRole(authentication), tokenValueOf(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public ReservationResponse getById(@PathVariable Long id, Authentication authentication) {
        Reservation reservation = service.findById(id);
        // Un huesped que no tiene tambien rol de Operador/Admin solo puede ver el detalle de sus propias reservas.
        if (isGuestOnly(authentication) && !reservation.getGuestId().equals(subjectOf(authentication))) {
            throw new AccessDeniedException("No puedes ver el detalle de una reserva de otro huesped");
        }
        return ReservationResponse.from(reservation);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public ReservationResponse pay(@PathVariable Long id, @Valid @RequestBody SimulatedPaymentRequest request,
                                    Authentication authentication) {
        Reservation reservation = service.pay(id, subjectOf(authentication), isGuestOnly(authentication), request.method());
        return ReservationResponse.from(reservation);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Operador', 'Admin')")
    public ReservationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request,
                                             Authentication authentication) {
        Reservation reservation = service.updateStatus(id, request.status(), subjectOf(authentication),
                primaryRole(authentication), tokenValueOf(authentication));
        return ReservationResponse.from(reservation);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public List<ReservationResponse> search(@RequestParam(required = false) ReservationStatus status,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             Authentication authentication) {
        // Un huesped que no tiene tambien rol de Operador/Admin solo puede ver sus propias reservas.
        String guestFilter = isGuestOnly(authentication) ? subjectOf(authentication) : null;
        return service.search(status, from, to, guestFilter).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    private String subjectOf(Authentication authentication) {
        return ((Jwt) authentication.getPrincipal()).getSubject();
    }

    private String emailOf(Authentication authentication) {
        return ((Jwt) authentication.getPrincipal()).getClaimAsString("preferred_username");
    }

    /** El JWT crudo, para reenviarlo tal cual a catalog (mismo criterio de propagacion que usa el BFF). */
    private String tokenValueOf(Authentication authentication) {
        return ((Jwt) authentication.getPrincipal()).getTokenValue();
    }

    /** Rol de mayor jerarquia entre los asignados al usuario, para el evento de auditoria/reporteria. */
    private String primaryRole(Authentication authentication) {
        List<String> priority = List.of("Admin", "Operador", "Cliente", "Auditor");
        for (String role : priority) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (authority.getAuthority().equals("ROLE_" + role)) {
                    return role;
                }
            }
        }
        return "Desconocido";
    }

    private boolean isGuestOnly(Authentication authentication) {
        boolean isCliente = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_Admin") || role.equals("ROLE_Operador")) {
                return false;
            }
            if (role.equals("ROLE_Cliente")) {
                isCliente = true;
            }
        }
        return isCliente;
    }
}
