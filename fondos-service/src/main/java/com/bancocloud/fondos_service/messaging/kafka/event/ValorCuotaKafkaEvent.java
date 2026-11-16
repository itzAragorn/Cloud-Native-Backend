package com.bancocloud.fondos_service.messaging.kafka.event;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Mensaje del tópico {@code bancocloud.fondos.valorcuota} (key = {@code fondoId}).
 * <p>
 * Es el contrato con analitica-service: se serializa como JSON con {@code JacksonJsonSerializer},
 * sin headers de tipo Java. Las validaciones se revisan antes de publicar y otra vez en el
 * consumidor.
 *
 * @param eventId      UUID del evento; el mismo del evento de RabbitMQ, para correlacionar ambos
 *                     flujos. Los consumidores lo usan para descartar duplicados.
 * @param eventType    siempre {@value #EVENT_TYPE}
 * @param variacionPct variación porcentual respecto del valor anterior (4 decimales, HALF_UP);
 *                     {@code null} si el valor anterior era 0 o no existía
 */
public record ValorCuotaKafkaEvent(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotNull @Positive Long fondoId,
        @NotBlank String nombreFondo,
        @PositiveOrZero BigDecimal valorAnterior,
        @NotNull @Positive BigDecimal valorNuevo,
        BigDecimal variacionPct,
        @NotNull LocalDateTime fecha) {

    public static final String EVENT_TYPE = "VALOR_CUOTA_ACTUALIZADO";

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public static ValorCuotaKafkaEvent from(ValorCuotaActualizadoEvent event) {
        return new ValorCuotaKafkaEvent(
                event.eventId(),
                EVENT_TYPE,
                event.fondoId(),
                event.nombreFondo(),
                event.valorAnterior(),
                event.valorNuevo(),
                variacion(event.valorAnterior(), event.valorNuevo()),
                event.fecha());
    }

    static BigDecimal variacion(BigDecimal anterior, BigDecimal nuevo) {
        if (anterior == null || nuevo == null || anterior.signum() == 0) {
            return null;
        }
        return nuevo.subtract(anterior)
                .multiply(CIEN)
                .divide(anterior, 4, RoundingMode.HALF_UP);
    }

    /** Key del mensaje: cada fondo en una partición fija; la compactación conserva su último valor. */
    public String key() {
        return String.valueOf(fondoId);
    }
}
