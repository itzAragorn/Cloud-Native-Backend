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
public class InversionesServiceClient {

    private final String inversionesServiceUrl;
    private final RestTemplate restTemplate;

    public InversionesServiceClient(
            @Value("${microservices.inversiones.url}") String inversionesServiceUrl,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        this.inversionesServiceUrl = inversionesServiceUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * Crea una nueva inversión
     */
    public Map<String, Object> crearInversion(Map<String, Object> inversionRequest) {
        log.info("Creando inversión en Inversiones-Service");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                inversionesServiceUrl + "/api/v1/inversiones",
                HttpMethod.POST,
                new HttpEntity<>(inversionRequest),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "INVERSION_CREATE_ERROR",
                e.getStatusCode().value(),
                "inversiones-service",
                e
            );
        } catch (HttpServerErrorException e) {
            throw new ServiceException(
                "Error en Inversiones-Service: " + e.getMessage(),
                "INVERSION_SERVICE_ERROR",
                e.getStatusCode().value(),
                "inversiones-service",
                e
            );
        }
    }

    /**
     * Obtiene todas las inversiones del usuario
     */
    public List<Map<String, Object>> obtenerMisInversiones(Long usuarioId) {
        log.info("Obteniendo inversiones del usuario {} de Inversiones-Service", usuarioId);
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                inversionesServiceUrl + "/api/v1/inversiones/mis-inversiones?usuarioId=" + usuarioId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener inversiones: " + e.getMessage(),
                "INVERSION_LIST_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "inversiones-service",
                e
            );
        }
    }


    /**
     * Obtiene el portafolio del usuario
     */
    public Map<String, Object> obtenerMiPortafolio(Long usuarioId) {
        log.info("Obteniendo portafolio del usuario {} de Inversiones-Service", usuarioId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                inversionesServiceUrl + "/api/v1/inversiones/mi-portafolio?usuarioId=" + usuarioId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener portafolio: " + e.getMessage(),
                "PORTAFOLIO_GET_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "inversiones-service",
                e
            );
        }
    }

    /**
     * Obtiene una inversión específica
     */
    public Map<String, Object> obtenerInversion(Long inversionId) {
        log.info("Obteniendo inversión {} de Inversiones-Service", inversionId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                inversionesServiceUrl + "/api/v1/inversiones/" + inversionId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ServiceException(
                    "Inversión no encontrada: " + inversionId,
                    "INVERSION_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value(),
                    "inversiones-service"
                );
            }
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "INVERSION_GET_ERROR",
                e.getStatusCode().value(),
                "inversiones-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener inversión: " + e.getMessage(),
                "INVERSION_GET_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "inversiones-service",
                e
            );
        }
    }

    /**
     * Obtiene el rendimiento de una inversión específica
     */
    public Map<String, Object> obtenerRendimientoInversion(Long inversionId) {
        log.info("Obteniendo rendimiento de inversión {} de Inversiones-Service", inversionId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                inversionesServiceUrl + "/api/v1/inversiones/" + inversionId + "/rendimiento",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener rendimiento: " + e.getMessage(),
                "RENDIMIENTO_GET_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "inversiones-service",
                e
            );
        }
    }

    /**
     * Obtiene el rendimiento total del usuario
     */
    public List<Map<String, Object>> obtenerRendimientoUsuario(Long usuarioId) {
        log.info("Obteniendo rendimiento del usuario {} de Inversiones-Service", usuarioId);
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                inversionesServiceUrl + "/api/v1/inversiones/rendimiento?usuarioId=" + usuarioId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener rendimiento del usuario: " + e.getMessage(),
                "USER_RENDIMIENTO_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "inversiones-service",
                e
            );
        }
    }
}
