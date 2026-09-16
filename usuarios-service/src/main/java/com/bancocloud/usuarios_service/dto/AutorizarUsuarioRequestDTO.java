package com.bancocloud.usuarios_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizarUsuarioRequestDTO {
    
    private Boolean autorizado;
}
