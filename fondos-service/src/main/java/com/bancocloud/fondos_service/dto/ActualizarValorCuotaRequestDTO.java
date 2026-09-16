package com.bancocloud.fondos_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarValorCuotaRequestDTO {
    
    @NotNull(message = "El nuevo valor de la cuota es requerido")
    @DecimalMin(value = "0.01", message = "El valor de la cuota debe ser mayor a 0")
    private BigDecimal nuevoValor;
}
