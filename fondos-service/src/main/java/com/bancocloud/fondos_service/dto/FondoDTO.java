package com.bancocloud.fondos_service.dto;

import com.bancocloud.fondos_service.model.EstadoFondo;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FondoDTO {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal valorCuota;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private EstadoFondo estado;
}
