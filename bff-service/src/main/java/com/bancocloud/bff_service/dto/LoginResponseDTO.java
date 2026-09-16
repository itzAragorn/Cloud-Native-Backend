package com.bancocloud.bff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de login con JWT token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private Long usuarioId;

    private String email;

    private String rol;

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    @Builder.Default
    private String mensaje = "Login exitoso";
}
