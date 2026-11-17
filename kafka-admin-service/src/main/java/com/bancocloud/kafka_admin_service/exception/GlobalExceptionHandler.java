package com.bancocloud.kafka_admin_service.exception;

import com.bancocloud.kafka_admin_service.exception.ErrorResponse.CampoError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicDeletionDisabledException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
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

/**
 * Traduce las excepciones a respuestas JSON claras: 400 validación (con el campo y el error),
 * 404 no existe, 409 ya existe o grupo activo, 503 Kafka no disponible.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacionCuerpo(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoError> errores = ex.getBindingResult().getAllErrors().stream()
                .map(e -> new CampoError(e instanceof FieldError fe ? fe.getField() : e.getObjectName(),
                        e.getDefaultMessage()))
                .sorted(Comparator.comparing(CampoError::campo).thenComparing(CampoError::error))
                .toList();
        return validacion(errores, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionRuta(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<CampoError> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new CampoError(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .sorted(Comparator.comparing(CampoError::campo).thenComparing(CampoError::error))
                .toList();
        return validacion(errores, req);
    }

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

    /** Kafka rechazó los valores (particiones, réplicas, configuración, nombre). */
    @ExceptionHandler({InvalidPartitionsException.class, InvalidReplicationFactorException.class,
            InvalidConfigurationException.class, InvalidRequestException.class, InvalidTopicException.class,
            PolicyViolationException.class, TopicDeletionDisabledException.class})
    public ResponseEntity<ErrorResponse> rechazadoPorKafka(ApiException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.BAD_REQUEST, "Kafka rechazó la operación: " + ex.getMessage(), req, List.of());
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, UnknownTopicOrPartitionException.class,
            GroupIdNotFoundException.class})
    public ResponseEntity<ErrorResponse> noEncontrado(RuntimeException ex, HttpServletRequest req) {
        String mensaje = ex instanceof UnknownTopicOrPartitionException
                ? "El tópico no existe" : ex instanceof GroupIdNotFoundException
                ? "El consumer group no existe" : ex.getMessage();
        return respuesta(HttpStatus.NOT_FOUND, mensaje, req, List.of());
    }

    @ExceptionHandler({TopicExistsException.class, GroupNotEmptyException.class, ConflictoException.class})
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest req) {
        String mensaje = ex instanceof GroupNotEmptyException
                ? "El consumer group tiene consumidores activos; detén el servicio antes de eliminarlo"
                : ex.getMessage();
        return respuesta(HttpStatus.CONFLICT, mensaje, req, List.of());
    }

    @ExceptionHandler({KafkaNoDisponibleException.class, org.apache.kafka.common.errors.TimeoutException.class})
    public ResponseEntity<ErrorResponse> noDisponible(RuntimeException ex, HttpServletRequest req) {
        log.error("Kafka no disponible: {}", ex.getMessage());
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no está disponible", req, List.of());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> errorKafka(ApiException ex, HttpServletRequest req) {
        log.error("Kafka rechazó la operación", ex);
        return respuesta(HttpStatus.BAD_GATEWAY, "Kafka rechazó la operación: " + ex.getMessage(), req, List.of());
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

    private String valoresEnum(Class<?> tipo) {
        return Arrays.stream(tipo.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
    }
}
