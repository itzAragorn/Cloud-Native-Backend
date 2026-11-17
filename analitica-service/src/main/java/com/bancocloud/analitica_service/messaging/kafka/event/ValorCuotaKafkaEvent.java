package com.bancocloud.analitica_service.messaging.kafka.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje del tópico {@code bancocloud.fondos.valorcuota}, publicado por fondos-service
 * (key = {@code fondoId}). Copia del contrato del productor: cada microservicio tiene la suya.
 *
 * @param variacionPct variación porcentual; {@code null} si el valor anterior era 0
 */
public record ValorCuotaKafkaEvent(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotNull @Positive Long fondoId,
        @NotBlank String nombreFondo,
        @PositiveOrZero BigDecimal valorAnterior,
        @NotNull @Positive BigDecimal valorNuevo,
        BigDecimal variacionPct,
        @NotNull LocalDateTime fecha) implements EventoKafka {

    public static final String EVENT_TYPE = "VALOR_CUOTA_ACTUALIZADO";
}
