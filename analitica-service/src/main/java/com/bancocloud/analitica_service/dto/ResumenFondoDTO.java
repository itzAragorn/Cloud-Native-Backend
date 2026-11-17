package com.bancocloud.analitica_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumen de un fondo: lo invertido (desde el tópico de inversiones) y su último valor cuota
 * (desde el tópico de valor cuota).
 *
 * @param ranking posición por monto total invertido (1 = el que más recibió); {@code null} si
 *                el fondo aún no recibe inversiones
 */
public record ResumenFondoDTO(
        Integer ranking,
        Long fondoId,
        String nombreFondo,
        BigDecimal montoTotalInvertido,
        long cantidadInversiones,
        BigDecimal ultimoValorCuota,
        BigDecimal ultimaVariacionPct,
        LocalDateTime ultimoCambioValorCuota) {
}
