package com.bancocloud.usuarios_service.dto;

import com.bancocloud.usuarios_service.model.Rol;
import com.bancocloud.usuarios_service.validator.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearUsuarioRequestDTO {
    
    @NotBlank(message = "El nombre del usuario es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;
    
    @Size(max = 100, message = "El apellido debe tener máximo 100 caracteres")
    private String apellido;
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El formato del email es inválido")
    private String email;
    
    @NotBlank(message = "La contraseña es requerida")
    @ValidPassword
    private String password;
    
    @NotNull(message = "El rol es requerido")
    private Rol rol;
}
