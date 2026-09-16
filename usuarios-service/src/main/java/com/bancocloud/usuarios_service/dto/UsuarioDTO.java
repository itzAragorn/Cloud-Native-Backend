package com.bancocloud.usuarios_service.dto;

import com.bancocloud.usuarios_service.model.Rol;
import com.bancocloud.usuarios_service.model.EstadoUsuario;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash; // Incluido SOLO para endpoints internos (BFF)
    private Rol rol;
    private Boolean accesoAutorizado;
    private EstadoUsuario estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
