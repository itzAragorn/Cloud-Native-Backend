package com.bancocloud.notificaciones_service.consumer.fondos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Registra cada cambio de valor cuota y emite el aviso (simulado) a los inversionistas del fondo.
 */
@Slf4j
@Service
public class ValorCuotaNotificacionService {

    public void notificarCambio(ValorCuotaActualizadoEvent event) {
        BigDecimal variacion = event.valorNuevo().subtract(event.valorAnterior())
                .multiply(BigDecimal.valueOf(100))
                .divide(event.valorAnterior(), 2, RoundingMode.HALF_UP);
        log.info("[REGISTRO VALOR CUOTA] fondoId={} '{}': {} -> {} ({}%) el {}",
                event.fondoId(), event.nombreFondo(), event.valorAnterior(), event.valorNuevo(),
                variacion, event.fecha());
        log.info("[AVISO INVERSIONISTAS] El fondo '{}' actualizó su valor cuota a {} ({}%)",
                event.nombreFondo(), event.valorNuevo(), variacion);
    }
}
