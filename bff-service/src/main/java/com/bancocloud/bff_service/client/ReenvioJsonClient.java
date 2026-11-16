package com.bancocloud.bff_service.client;

import com.bancocloud.bff_service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Base de los clientes que reenvían una petición tal cual a un microservicio y devuelven su código
 * y cuerpo originales (201, 204, 400 con detalle de campos, 404, 409, 503), para que el frontend
 * reciba los mismos errores de validación.
 */
@Slf4j
public abstract class ReenvioJsonClient {

    private final String baseUrl;
    private final String servicio;
    private final String codigoNoDisponible;
    private final RestTemplate restTemplate;

    protected ReenvioJsonClient(String baseUrl, String servicio, String codigoNoDisponible, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.servicio = servicio;
        this.codigoNoDisponible = codigoNoDisponible;
        this.restTemplate = restTemplate;
    }

    /**
     * @param uriTemplate ruta en el microservicio, por ejemplo {@code /topics/{name}}
     * @param body        JSON a reenviar, o {@code null}
     * @param uriVars     valores de la ruta; se codifican al construir la URL
     */
    public ResponseEntity<String> reenviar(HttpMethod method, String uriTemplate, String body, Object... uriVars) {
        log.info("Reenviando {} {} a {}", method, uriTemplate, servicio);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + uriTemplate, method, new HttpEntity<>(body, headers), String.class, uriVars);
            return conCuerpoJson(response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException e) {
            return conCuerpoJson(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new ServiceException(servicio + " no está disponible", codigoNoDisponible,
                    HttpStatus.SERVICE_UNAVAILABLE.value(), servicio, e);
        }
    }

    private static ResponseEntity<String> conCuerpoJson(int status, String body) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (body == null || body.isEmpty()) {
            return builder.build();
        }
        return builder.contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
