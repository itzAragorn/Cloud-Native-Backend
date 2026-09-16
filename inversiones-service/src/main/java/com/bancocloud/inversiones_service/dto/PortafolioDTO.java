package com.bancocloud.inversiones_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortafolioDTO {
    
    private Long usuarioId;
    private BigDecimal montoTotalInvertido;
    private BigDecimal valorTotalActual;
    private BigDecimal rendimientoTotal;
    private BigDecimal rendimientoPorcentaje;
    private List<InversionDTO> inversiones;
}
