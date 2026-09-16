package com.bancocloud.bff_service.exception;

import com.bancocloud.bff_service.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Maneja excepciones de servicios remotos
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(
            ServiceException ex, WebRequest request) {
        
        log.error("Service Exception en {}: {} - {}", 
            ex.getServiceName(), ex.getErrorCode(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(ex.getHttpStatus())
            .error("SERVICE_ERROR")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .serviceName(ex.getServiceName())
            .errorCode(ex.getErrorCode())
            .build();
        
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(errorResponse);
    }

    /**
     * Maneja errores de autenticación personalizados (credenciales inválidas, usuario no autorizado)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.warn("Autenticación fallida: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("UNAUTHORIZED")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .errorCode("AUTH_FAILED")
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse);
    }

    /**
     * Maneja errores de autorización (sin permisos)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Acceso denegado: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("FORBIDDEN")
            .message("No tienes permisos para acceder a este recurso")
            .path(request.getDescription(false).replace("uri=", ""))
            .errorCode("AUTH_FORBIDDEN")
            .build();
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(errorResponse);
    }

    /**
     * Maneja excepciones genéricas no capturadas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        
        log.error("Error interno no manejado: ", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("INTERNAL_SERVER_ERROR")
            .message("Ocurrió un error interno del servidor")
            .path(request.getDescription(false).replace("uri=", ""))
            .errorCode("INTERNAL_ERROR")
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}
