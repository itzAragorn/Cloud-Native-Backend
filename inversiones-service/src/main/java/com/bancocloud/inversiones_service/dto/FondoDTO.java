package com.bancocloud.inversiones_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO que representa un Fondo desde el fondos-service
 */
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
    private String estado;
}
