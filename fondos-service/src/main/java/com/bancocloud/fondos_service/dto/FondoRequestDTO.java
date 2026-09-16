package com.bancocloud.fondos_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FondoRequestDTO {
    
    @NotBlank(message = "El nombre del fondo es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;
    
    @NotBlank(message = "La descripción es requerida")
    @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
    private String descripcion;
    
    @NotNull(message = "El valor de la cuota es requerido")
    @DecimalMin(value = "0.01", message = "El valor de la cuota debe ser mayor a 0")
    private BigDecimal valorCuota;
}
