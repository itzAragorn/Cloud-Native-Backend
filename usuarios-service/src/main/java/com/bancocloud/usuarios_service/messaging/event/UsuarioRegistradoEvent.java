package com.bancocloud.usuarios_service.messaging.event;

import com.bancocloud.usuarios_service.model.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando se registra un nuevo usuario BancoCloud.
 * notificaciones-service lo usa para avisar al administrador.
 *
 * @param eventId identificador único del evento (correlación de publisher confirms)
 */
public record UsuarioRegistradoEvent(
        String eventId,
        Long usuarioId,
        String nombre,
        String apellido,
        String email,
        String rol,
        Boolean accesoAutorizado,
        LocalDateTime fecha) {

    public static UsuarioRegistradoEvent from(Usuario usuario) {
        return new UsuarioRegistradoEvent(
                UUID.randomUUID().toString(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol().name(),
                usuario.getAccesoAutorizado(),
                LocalDateTime.now());
    }
}
