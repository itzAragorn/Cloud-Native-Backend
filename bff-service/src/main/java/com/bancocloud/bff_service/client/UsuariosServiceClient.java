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
public class UsuariosServiceClient {

    private final String usuariosServiceUrl;
    private final RestTemplate restTemplate;

    public UsuariosServiceClient(
            @Value("${microservices.usuarios.url}") String usuariosServiceUrl,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        this.usuariosServiceUrl = usuariosServiceUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * Crea un nuevo usuario
     */
    public Map<String, Object> crearUsuario(Map<String, Object> usuarioRequest) {
        log.info("Creando usuario en Usuarios-Service");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios",
                HttpMethod.POST,
                new HttpEntity<>(usuarioRequest),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "USUARIO_CREATE_ERROR",
                e.getStatusCode().value(),
                "usuarios-service",
                e
            );
        } catch (HttpServerErrorException e) {
            throw new ServiceException(
                "Error en Usuarios-Service: " + e.getMessage(),
                "USUARIO_SERVICE_ERROR",
                e.getStatusCode().value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Obtiene todos los usuarios (solo ADMIN)
     */
    public List<Map<String, Object>> obtenerTodosUsuarios() {
        log.info("Obteniendo todos los usuarios de Usuarios-Service");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener usuarios: " + e.getMessage(),
                "USUARIO_LIST_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Obtiene usuarios por rol
     */
    public List<Map<String, Object>> obtenerUsuariosPorRol(String rol) {
        log.info("Obteniendo usuarios con rol {} de Usuarios-Service", rol);
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/por-rol?rol=" + rol,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener usuarios por rol: " + e.getMessage(),
                "USUARIO_LIST_BY_ROLE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Obtiene usuarios pendientes de autorización
     */
    public List<Map<String, Object>> obtenerClientesPendientesAutorizacion() {
        log.info("Obteniendo clientes pendientes de autorización de Usuarios-Service");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/pendientes-autorizacion",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener usuarios pendientes: " + e.getMessage(),
                "USUARIO_PENDING_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Obtiene un usuario por ID
     */
    public Map<String, Object> obtenerUsuarioPorId(Long usuarioId) {
        log.info("Obteniendo usuario {} de Usuarios-Service", usuarioId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/" + usuarioId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ServiceException(
                    "Usuario no encontrado: " + usuarioId,
                    "USUARIO_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value(),
                    "usuarios-service"
                );
            }
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "USUARIO_GET_ERROR",
                e.getStatusCode().value(),
                "usuarios-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener usuario: " + e.getMessage(),
                "USUARIO_GET_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Obtiene un usuario por email
     */
    public Map<String, Object> obtenerUsuarioPorEmail(String email) {
        log.info("Obteniendo usuario con email {} de Usuarios-Service", email);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/por-email?email=" + email,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al obtener usuario por email: " + e.getMessage(),
                "USUARIO_GET_BY_EMAIL_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Autoriza un usuario (solo ADMIN)
     */
    public Map<String, Object> autorizarUsuario(Long usuarioId, Map<String, Object> request) {
        log.info("Autorizando usuario {} en Usuarios-Service", usuarioId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/" + usuarioId + "/autorizar",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "USUARIO_AUTHORIZE_ERROR",
                e.getStatusCode().value(),
                "usuarios-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al autorizar usuario: " + e.getMessage(),
                "USUARIO_AUTHORIZE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Bloquea un usuario (solo ADMIN)
     */
    public Map<String, Object> bloquearUsuario(Long usuarioId) {
        log.info("Bloqueando usuario {} en Usuarios-Service", usuarioId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/" + usuarioId + "/bloquear",
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "USUARIO_BLOCK_ERROR",
                e.getStatusCode().value(),
                "usuarios-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al bloquear usuario: " + e.getMessage(),
                "USUARIO_BLOCK_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Desbloquea un usuario (solo ADMIN)
     */
    public Map<String, Object> desbloquearUsuario(Long usuarioId) {
        log.info("Desbloqueando usuario {} en Usuarios-Service", usuarioId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/" + usuarioId + "/desbloquear",
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ServiceException(
                e.getResponseBodyAsString(),
                "USUARIO_UNBLOCK_ERROR",
                e.getStatusCode().value(),
                "usuarios-service",
                e
            );
        } catch (Exception e) {
            throw new ServiceException(
                "Error al desbloquear usuario: " + e.getMessage(),
                "USUARIO_UNBLOCK_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }

    /**
     * Verifica el acceso de un usuario
     */
    public Map<String, Object> verificarAcceso(Long usuarioId) {
        log.info("Verificando acceso del usuario {} en Usuarios-Service", usuarioId);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                usuariosServiceUrl + "/api/v1/usuarios/" + usuarioId + "/verificar-acceso",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException(
                "Error al verificar acceso: " + e.getMessage(),
                "USUARIO_ACCESS_VERIFY_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "usuarios-service",
                e
            );
        }
    }
}