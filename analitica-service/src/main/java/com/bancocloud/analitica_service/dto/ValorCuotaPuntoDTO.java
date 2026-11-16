package com.bancocloud.analitica_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ValorCuotaPuntoDTO(
        LocalDateTime fecha,
        BigDecimal valorAnterior,
        BigDecimal valorNuevo,
        BigDecimal variacionPct) {
}
