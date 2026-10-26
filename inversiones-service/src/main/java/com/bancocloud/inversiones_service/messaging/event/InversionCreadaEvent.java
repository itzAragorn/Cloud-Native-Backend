package com.bancocloud.inversiones_service.messaging.event;

import com.bancocloud.inversiones_service.model.Inversion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando se confirma una nueva inversión.
 * notificaciones-service lo usa para generar el comprobante.
 *
 * @param eventId identificador único del evento (correlación de publisher confirms)
 */
public record InversionCreadaEvent(
        String eventId,
        Long inversionId,
        Long usuarioId,
        Long fondoId,
        BigDecimal monto,
        BigDecimal cuotas,
        BigDecimal valorCuota,
        LocalDateTime fecha) {

    public static InversionCreadaEvent from(Inversion inversion) {
        return new InversionCreadaEvent(
                UUID.randomUUID().toString(),
                inversion.getId(),
                inversion.getUsuarioId(),
                inversion.getFondoId(),
                inversion.getMontoInvertido(),
                inversion.getCuotas(),
                inversion.getValorCuotaCompra(),
                inversion.getFechaInversion());
    }
}
