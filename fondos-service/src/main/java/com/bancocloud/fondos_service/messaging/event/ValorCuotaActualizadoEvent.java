package com.bancocloud.fondos_service.messaging.event;

import com.bancocloud.fondos_service.model.Fondo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando cambia el valor cuota de un fondo.
 * notificaciones-service lo usa para avisar a los inversionistas y registrar el cambio.
 *
 * @param eventId identificador único del evento (correlación de publisher confirms)
 */
public record ValorCuotaActualizadoEvent(
        String eventId,
        Long fondoId,
        String nombreFondo,
        BigDecimal valorAnterior,
        BigDecimal valorNuevo,
        LocalDateTime fecha) {

    public static ValorCuotaActualizadoEvent from(Fondo fondo, BigDecimal valorAnterior) {
        return new ValorCuotaActualizadoEvent(
                UUID.randomUUID().toString(),
                fondo.getId(),
                fondo.getNombre(),
                valorAnterior,
                fondo.getValorCuota(),
                LocalDateTime.now());
    }
}
