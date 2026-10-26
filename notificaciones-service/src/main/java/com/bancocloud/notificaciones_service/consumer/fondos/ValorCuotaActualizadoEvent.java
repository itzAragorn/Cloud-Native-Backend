package com.bancocloud.notificaciones_service.consumer.fondos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mensaje publicado por fondos-service en {@code fondos.valorcuota.q}. */
public record ValorCuotaActualizadoEvent(
        @NotBlank String eventId,
        @NotNull Long fondoId,
        @NotBlank String nombreFondo,
        @NotNull @Positive BigDecimal valorAnterior,
        @NotNull @Positive BigDecimal valorNuevo,
        @NotNull LocalDateTime fecha) {
}
