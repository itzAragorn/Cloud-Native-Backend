package com.bancocloud.analitica_service.messaging.kafka.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje del tópico {@code bancocloud.inversiones.eventos}, publicado por inversiones-service
 * (key = {@code usuarioId}). Copia del contrato del productor: cada microservicio tiene la suya.
 */
public record InversionRegistradaKafkaEvent(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotNull @Positive Long inversionId,
        @NotNull @Positive Long usuarioId,
        @NotNull @Positive Long fondoId,
        @NotNull @Positive BigDecimal monto,
        @NotNull @Positive BigDecimal cuotas,
        @NotNull @Positive BigDecimal valorCuota,
        @NotNull LocalDateTime fecha) implements EventoKafka {

    public static final String EVENT_TYPE = "INVERSION_REGISTRADA";
}
