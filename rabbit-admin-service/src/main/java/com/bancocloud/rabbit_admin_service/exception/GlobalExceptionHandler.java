package com.bancocloud.rabbit_admin_service.exception;

import com.bancocloud.rabbit_admin_service.exception.ErrorResponse.CampoError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones a respuestas JSON claras:
 * 400 validación (con el campo y el error), 404 no existe, 409 ya existe,
 * 503 RabbitMQ no disponible.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de anotaciones en el cuerpo (@NotBlank, @Pattern, @NoReservado, ...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacionCuerpo(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoError> errores = ex.getBindingResult().getAllErrors().stream()
                .map(e -> new CampoError(e instanceof FieldError fe ? fe.getField() : e.getObjectName(),
                        e.getDefaultMessage()))
                .sorted(Comparator.comparing(CampoError::campo).thenComparing(CampoError::error))
                .toList();
        return validacion(errores, req);
    }

    /** Errores de anotaciones en la ruta, por ejemplo {@code DELETE /queues/amq.algo}. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionRuta(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<CampoError> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new CampoError(r.getMethodParameter().getParameterName(), mensaje(e))))
                .sorted(Comparator.comparing(CampoError::campo).thenComparing(CampoError::error))
                .toList();
        return validacion(errores, req);
    }

    /** JSON mal formado o valor que no corresponde al tipo (por ejemplo {@code "type": "XYZ"}). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String campo = ife.getPath().get(ife.getPath().size() - 1).getPropertyName();
            String error = ife.getTargetType().isEnum()
                    ? "valor '" + ife.getValue() + "' inválido; usa uno de " + valoresEnum(ife.getTargetType())
                    : "valor '" + ife.getValue() + "' inválido";
            return validacion(List.of(new CampoError(campo, error)), req);
        }
        String detalle = ex.getCause() instanceof JacksonException je ? je.getOriginalMessage() : ex.getMessage();
        return respuesta(HttpStatus.BAD_REQUEST, "JSON inválido: " + detalle, req, List.of());
    }

    @ExceptionHandler(ConfiguracionInvalidaException.class)
    public ResponseEntity<ErrorResponse> configuracionInvalida(ConfiguracionInvalidaException ex, HttpServletRequest req) {
        return validacion(List.of(new CampoError(ex.getCampo(), ex.getMessage())), req);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(RecursoYaExisteException.class)
    public ResponseEntity<ErrorResponse> yaExiste(RecursoYaExisteException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<ErrorResponse> brokerNoDisponible(AmqpConnectException ex, HttpServletRequest req) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no está disponible", req, List.of());
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorResponse> errorBroker(AmqpException ex, HttpServletRequest req) {
        log.error("RabbitMQ rechazó la operación", ex);
        return respuesta(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazó la operación: " + ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, List.of());
    }

    private ResponseEntity<ErrorResponse> validacion(List<CampoError> errores, HttpServletRequest req) {
        return respuesta(HttpStatus.BAD_REQUEST, "Errores de validación en los campos", req, errores);
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String message, HttpServletRequest req,
                                                    List<CampoError> errores) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.name(), message, req.getRequestURI(), errores));
    }

    private String mensaje(MessageSourceResolvable error) {
        return error.getDefaultMessage();
    }

    private String valoresEnum(Class<?> tipo) {
        return Arrays.stream(tipo.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
    }
}
