package com.andesstay.ms_andesstay_reservations.client;

import com.andesstay.ms_andesstay_reservations.config.DownstreamProperties;
import com.andesstay.ms_andesstay_reservations.web.CatalogIntegrationException;
import com.andesstay.ms_andesstay_reservations.web.NoAvailabilityException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * Llamada directa (sincrona, servicio-a-servicio) hacia ms-andesstay-catalog para aplicar
 * la regla "la disponibilidad disminuye al confirmar la reserva" (caso, seccion 3). Se
 * reenvia el mismo Bearer token del usuario que confirmo/cerro la reserva, para que catalog
 * vuelva a validar el JWT de forma independiente (mismo criterio de defensa en profundidad
 * que usa el BFF al llamar a los microservicios de dominio).
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder, DownstreamProperties downstreamProperties) {
        this.restClient = builder.baseUrl(downstreamProperties.catalogUrl()).build();
    }

    /** @throws NoAvailabilityException si la unidad ya no tiene cupos (evita sobreventa). */
    public void reserveUnit(Long unitId, String bearerToken) {
        call(unitId, "reserve", bearerToken);
    }

    /** Libera la unidad; a diferencia de reserveUnit, el llamador decide si tratar el fallo como critico. */
    public void releaseUnit(Long unitId, String bearerToken) {
        call(unitId, "release", bearerToken);
    }

    /**
     * Tarifa vigente de la unidad, para calcular el monto total al crear la reserva. Se
     * consulta a catalog en vez de confiar en un valor que mande el frontend, para que
     * nadie pueda manipular el precio final desde el navegador.
     */
    public BigDecimal getNightlyRate(Long unitId, String bearerToken) {
        try {
            CatalogUnitSnapshot unit = restClient.get()
                    .uri("/api/catalog/units/{id}", unitId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .body(CatalogUnitSnapshot.class);
            if (unit == null) {
                throw new CatalogIntegrationException("catalog no devolvio datos para la unidad " + unitId, null);
            }
            return unit.nightlyRate();
        } catch (RestClientResponseException ex) {
            throw new CatalogIntegrationException(
                    "catalog respondio " + ex.getStatusCode() + " al consultar la tarifa de la unidad " + unitId, ex);
        }
    }

    /** Solo los campos que reservations necesita leer de catalog; el resto del JSON se ignora. */
    private record CatalogUnitSnapshot(Long id, BigDecimal nightlyRate) {
    }

    private void call(Long unitId, String action, String bearerToken) {
        try {
            restClient.post()
                    .uri("/api/catalog/units/{id}/{action}", unitId, action)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        throw new NoAvailabilityException(unitId);
                    })
                    .toBodilessEntity();
        } catch (NoAvailabilityException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new CatalogIntegrationException(
                    "catalog respondio " + ex.getStatusCode() + " al intentar '" + action + "' la unidad " + unitId, ex);
        }
    }
}
