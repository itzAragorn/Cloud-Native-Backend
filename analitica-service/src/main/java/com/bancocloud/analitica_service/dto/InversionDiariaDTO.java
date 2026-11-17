package com.bancocloud.analitica_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InversionDiariaDTO(LocalDate dia, BigDecimal montoTotal, long cantidad) {
}
