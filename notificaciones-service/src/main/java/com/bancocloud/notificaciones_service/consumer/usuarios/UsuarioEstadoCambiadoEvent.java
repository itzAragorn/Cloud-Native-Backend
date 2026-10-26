package com.bancocloud.notificaciones_service.consumer.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Mensaje publicado por usuarios-service en {@code usuarios.estado.q}.
 *
 * @param cambio AUTORIZADO, DESAUTORIZADO, BLOQUEADO o DESBLOQUEADO
 */
public record UsuarioEstadoCambiadoEvent(
        @NotBlank String eventId,
        @NotNull Long usuarioId,
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotBlank String cambio,
        @NotBlank String estado,
        @NotNull Boolean accesoAutorizado,
        @NotNull LocalDateTime fecha) {
}
