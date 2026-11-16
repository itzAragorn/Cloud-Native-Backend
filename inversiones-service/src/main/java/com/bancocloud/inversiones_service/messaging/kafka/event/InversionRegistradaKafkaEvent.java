package com.bancocloud.inversiones_service.messaging.kafka.event;

import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje del tópico {@code bancocloud.inversiones.eventos} (key = {@code usuarioId}).
 * <p>
 * Es el contrato con los consumidores (analitica-service y notificaciones-service): se serializa
 * como JSON con {@code JacksonJsonSerializer}, sin headers de tipo Java. Las validaciones se
 * revisan antes de publicar y otra vez en cada consumidor.
 *
 * @param eventId   UUID del evento; el mismo del evento de RabbitMQ, para correlacionar ambos
 *                  flujos. Los consumidores lo usan para descartar duplicados.
 * @param eventType siempre {@value #EVENT_TYPE}
 * @param fecha     fecha de la inversión
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
        @NotNull LocalDateTime fecha) {

    public static final String EVENT_TYPE = "INVERSION_REGISTRADA";

    public static InversionRegistradaKafkaEvent from(InversionCreadaEvent event) {
        return new InversionRegistradaKafkaEvent(
                event.eventId(),
                EVENT_TYPE,
                event.inversionId(),
                event.usuarioId(),
                event.fondoId(),
                event.monto(),
                event.cuotas(),
                event.valorCuota(),
                event.fecha());
    }

    /** Key del mensaje: ordena las inversiones de cada usuario dentro de su partición. */
    public String key() {
        return String.valueOf(usuarioId);
    }
}
