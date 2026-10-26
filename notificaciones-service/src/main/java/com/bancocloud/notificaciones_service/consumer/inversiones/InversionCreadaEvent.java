package com.bancocloud.notificaciones_service.consumer.inversiones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje publicado por inversiones-service en {@code inversiones.comprobante.q}.
 * Un mensaje que no cumple estas validaciones es no recuperable y va a la DLQ.
 */
public record InversionCreadaEvent(
        @NotBlank String eventId,
        @NotNull Long inversionId,
        @NotNull Long usuarioId,
        @NotNull Long fondoId,
        @NotNull @Positive BigDecimal monto,
        @NotNull @Positive BigDecimal cuotas,
        @NotNull @Positive BigDecimal valorCuota,
        @NotNull LocalDateTime fecha) {
}
