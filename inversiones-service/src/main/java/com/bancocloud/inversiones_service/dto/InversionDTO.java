package com.bancocloud.inversiones_service.dto;

import com.bancocloud.inversiones_service.model.EstadoInversion;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InversionDTO {
    
    private Long id;
    private Long usuarioId;
    private Long fondoId;
    private BigDecimal montoInvertido;
    private BigDecimal cuotas;
    private BigDecimal valorCuotaCompra;
    private LocalDateTime fechaInversion;
    private LocalDateTime fechaActualizacion;
    private EstadoInversion estado;
}
