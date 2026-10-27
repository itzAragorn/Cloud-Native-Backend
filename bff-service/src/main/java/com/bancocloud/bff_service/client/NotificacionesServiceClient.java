package com.bancocloud.bff_service.client;

import com.bancocloud.bff_service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Consulta de comprobantes de inversión en notificaciones-service. */
@Slf4j
@Component
public class NotificacionesServiceClient {

    private static final String SERVICE = "notificaciones-service";

    private final String notificacionesUrl;
    private final RestTemplate restTemplate;

    public NotificacionesServiceClient(
            @Value("${microservices.notificaciones.url}") String notificacionesUrl,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        this.notificacionesUrl = notificacionesUrl;
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> obtenerComprobantes(Long usuarioId) {
        log.info("Obteniendo comprobantes del usuario {} en {}", usuarioId, SERVICE);
        try {
            return restTemplate.exchange(
                    notificacionesUrl + "/comprobantes?usuarioId={usuarioId}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    usuarioId
            ).getBody();
        } catch (RestClientResponseException e) {
            throw new ServiceException(e.getResponseBodyAsString(), "COMPROBANTES_ERROR",
                    e.getStatusCode().value(), SERVICE, e);
        } catch (ResourceAccessException e) {
            throw new ServiceException("notificaciones-service no está disponible", "NOTIFICACIONES_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE.value(), SERVICE, e);
        }
    }
}
