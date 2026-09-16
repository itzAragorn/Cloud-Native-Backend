package com.bancocloud.bff_service.client;

import com.bancocloud.bff_service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FondosServiceClient {

    private final String fondosServiceUrl;
    private final RestTemplate restTemplate;

    public FondosServiceClient(
            @Value("${microservices.fondos.url}") String fondosServiceUrl,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        this.fondosServiceUrl = fondosServiceUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * Crea un nuevo fondo
     */
    public Map<String, Object> crearFondo(Map<String, Object> fondoRequest) {
        log.info("Creando fondo en Fondos-Service");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                fondosServiceUrl + "/api/v1/fondos",
                HttpMethod.POST,
                new HttpEntity<>(fondoRequest),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "FONDO_CREATE_ERROR",
                e.getStatusCode().value(),
                "fondos-service",
                e
            );
        } catch (HttpServerErrorException e) {
            throw new ServiceException(
                "Error en Fondos-Service: " + e.getMessage(),
                "FONDO_SERVICE_ERROR",
                e.getStatusCode().value(),
                "fondos-service",
                e
            );
        }
    }

    /**
     * Obtiene todos los fondos
     */
    public List<Map<String, Object>> obtenerTodosFondos() {
        log.info("Obteniendo todos los fondos de Fondos-Service");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                fondosServiceUrl + "/api/v1/fondos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener fondos: " + e.getMessage(),
                "FONDO_LIST_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "fondos-service",
                e
            );
        }
    }

    /**
     * Obtiene fondos activos
     */
    public List<Map<String, Object>> obtenerFondosActivos() {
        log.info("Obteniendo fondos activos de Fondos-Service");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                fondosServiceUrl + "/api/v1/fondos/activos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener fondos activos: " + e.getMessage(),
                "FONDO_LIST_ACTIVE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "fondos-service",
                e
            );
        }
    }

    /**
     * Obtiene un fondo por ID
     */
    public Map<String, Object> obtenerFondoPorId(Long fondoId) {
        log.info("Obteniendo fondo {} de Fondos-Service", fondoId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                fondosServiceUrl + "/api/v1/fondos/" + fondoId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ServiceException(
                    "Fondo no encontrado: " + fondoId,
                    "FONDO_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value(),
                    "fondos-service"
                );
            }
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "FONDO_GET_ERROR",
                e.getStatusCode().value(),
                "fondos-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener fondo: " + e.getMessage(),
                "FONDO_GET_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "fondos-service",
                e
            );
        }
    }

    /**
     * Obtiene el valor de la cuota de un fondo
     */
    public Map<String, Object> obtenerValorCuota(Long fondoId) {
        log.info("Obteniendo valor de cuota para fondo {} de Fondos-Service", fondoId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                fondosServiceUrl + "/api/v1/fondos/" + fondoId + "/valor-cuota",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener valor de cuota: " + e.getMessage(),
                "FONDO_QUOTE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "fondos-service",
                e
            );
        }
    }

    /**
     * Actualiza el valor de la cuota de un fondo
     */
    public Map<String, Object> actualizarValorCuota(Long fondoId, Map<String, Object> request) {
        log.info("Actualizando valor de cuota para fondo {} en Fondos-Service", fondoId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                fondosServiceUrl + "/api/v1/fondos/" + fondoId + "/valor-cuota",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "FONDO_UPDATE_QUOTE_ERROR",
                e.getStatusCode().value(),
                "fondos-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al actualizar valor de cuota: " + e.getMessage(),
                "FONDO_UPDATE_QUOTE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "fondos-service",
                e
            );
        }
    }
}
