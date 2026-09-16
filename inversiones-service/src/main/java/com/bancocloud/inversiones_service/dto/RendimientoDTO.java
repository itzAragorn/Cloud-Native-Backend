package com.bancocloud.inversiones_service.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendimientoDTO {
    
    private Long inversionId;
    private Long fondoId;
    private String nombreFondo;
    private BigDecimal montoInvertido;
    private BigDecimal valorActualCuota;
    private BigDecimal valorCuotaCompra;
    private BigDecimal cuotas;
    private BigDecimal valorTotalActual;
    private BigDecimal rendimientoAbsoluto;
    private BigDecimal rendimientoPorcentaje;
}
