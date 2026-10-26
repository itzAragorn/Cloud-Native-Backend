package com.bancocloud.notificaciones_service.consumer.inversiones;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante de inversión que se guarda como JSON en el directorio de comprobantes. */
public record Comprobante(
        String numero,
        LocalDateTime emitidoEn,
        String eventId,
        Long inversionId,
        Long usuarioId,
        Long fondoId,
        BigDecimal monto,
        BigDecimal cuotas,
        BigDecimal valorCuota,
        LocalDateTime fechaInversion) {
}
