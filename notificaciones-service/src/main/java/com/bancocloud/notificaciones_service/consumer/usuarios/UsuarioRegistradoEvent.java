package com.bancocloud.notificaciones_service.consumer.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Mensaje publicado por usuarios-service en {@code usuarios.registro.q}. */
public record UsuarioRegistradoEvent(
        @NotBlank String eventId,
        @NotNull Long usuarioId,
        @NotBlank String nombre,
        String apellido,
        @NotBlank @Email String email,
        @NotBlank String rol,
        @NotNull Boolean accesoAutorizado,
        @NotNull LocalDateTime fecha) {
}
