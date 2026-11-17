package com.bancocloud.analitica_service.dto;

import java.math.BigDecimal;

/** Fila de la consulta de totales por fondo. */
public record TotalFondoFila(Long fondoId, BigDecimal montoTotal, Long cantidadInversiones) {
}
