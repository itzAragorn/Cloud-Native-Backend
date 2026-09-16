package com.bancocloud.inversiones_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearInversionRequestDTO {
    
    @NotNull(message = "El ID del usuario es requerido")
    private Long usuarioId;
    
    @NotNull(message = "El ID del fondo es requerido")
    private Long fondoId;
    
    @NotNull(message = "El monto invertido es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal montoInvertido;
}
