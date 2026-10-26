package com.bancocloud.bff_service.client;

import com.bancocloud.bff_service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Reenvía las operaciones de administración de RabbitMQ a rabbit-admin-service.
 * <p>
 * Devuelve el código y el cuerpo originales (201, 204, 400 con detalle de campos,
 * 404, 409) para que el frontend reciba los mismos errores de validación.
 */
@Slf4j
@Component
public class RabbitAdminServiceClient {

    private static final String SERVICE = "rabbit-admin-service";

    private final String rabbitAdminUrl;
    private final RestTemplate restTemplate;

    public RabbitAdminServiceClient(
            @Value("${microservices.rabbit-admin.url}") String rabbitAdminUrl,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        this.rabbitAdminUrl = rabbitAdminUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * @param uriTemplate ruta en rabbit-admin-service, por ejemplo {@code /queues/{name}}
     * @param body        JSON a reenviar, o {@code null}
     * @param uriVars     valores de la ruta; se codifican al construir la URL
     */
    public ResponseEntity<String> reenviar(HttpMethod method, String uriTemplate, String body, Object... uriVars) {
        log.info("Reenviando {} {} a {}", method, uriTemplate, SERVICE);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    rabbitAdminUrl + uriTemplate, method, new HttpEntity<>(body, headers), String.class, uriVars);
            return conCuerpoJson(response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException e) {
            return conCuerpoJson(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new ServiceException("rabbit-admin-service no está disponible", "RABBIT_ADMIN_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE.value(), SERVICE, e);
        }
    }

    private ResponseEntity<String> conCuerpoJson(int status, String body) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (body == null || body.isEmpty()) {
            return builder.build();
        }
        return builder.contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
