package com.bancocloud.inversiones_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Manejo de InversionNotFoundException
     */
    @ExceptionHandler(InversionNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarInversionNotFoundException(
            InversionNotFoundException ex,
            WebRequest request) {
        
        log.error("Inversión no encontrada: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Manejo de FondoServiceException
     */
    @ExceptionHandler(FondoServiceException.class)
    public ResponseEntity<ErrorResponse> manejarFondoServiceException(
            FondoServiceException ex,
            WebRequest request) {
        
        log.error("Error en fondos-service: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("BAD_GATEWAY")
                .message("Error al comunicarse con fondos-service: " + ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_GATEWAY);
    }
    
    /**
     * Manejo de errores de validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.error("Error de validación: {}", ex.getMessage());
        
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("timestamp", LocalDateTime.now());
        respuesta.put("status", HttpStatus.BAD_REQUEST.value());
        respuesta.put("error", "VALIDATION_ERROR");
        respuesta.put("message", "Errores de validación en los campos");
        respuesta.put("path", request.getDescription(false).replace("uri=", ""));
        respuesta.put("errores", errores);
        
        return new ResponseEntity<>(respuesta, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Manejo genérico de excepciones
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionGeneral(
            Exception ex,
            WebRequest request) {
        
        log.error("Error interno del servidor", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("Ha ocurrido un error interno. Por favor, intente más tarde.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
