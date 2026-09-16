package com.bancocloud.bff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitud de registro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroRequestDTO {

    @NotBlank(message = "Nombre es requerido")
    private String nombre;

    @NotBlank(message = "Apellido es requerido")
    private String apellido;

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    private String email;

    @NotBlank(message = "Password es requerido")
    @Size(min = 6, message = "Password debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "Confirmación de password es requerida")
    private String passwordConfirm;
}
