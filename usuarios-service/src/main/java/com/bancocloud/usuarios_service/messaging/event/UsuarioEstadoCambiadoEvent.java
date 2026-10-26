package com.bancocloud.usuarios_service.messaging.event;

import com.bancocloud.usuarios_service.model.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando un administrador autoriza, desautoriza, bloquea o desbloquea
 * a un usuario. notificaciones-service lo usa para notificar al cliente.
 *
 * @param eventId identificador único del evento (correlación de publisher confirms)
 * @param cambio  acción realizada; define la routing key
 * @param estado  estado del usuario después del cambio
 */
public record UsuarioEstadoCambiadoEvent(
        String eventId,
        Long usuarioId,
        String nombre,
        String email,
        CambioEstadoUsuario cambio,
        String estado,
        Boolean accesoAutorizado,
        LocalDateTime fecha) {

    public static UsuarioEstadoCambiadoEvent from(Usuario usuario, CambioEstadoUsuario cambio) {
        return new UsuarioEstadoCambiadoEvent(
                UUID.randomUUID().toString(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                cambio,
                usuario.getEstado().name(),
                usuario.getAccesoAutorizado(),
                LocalDateTime.now());
    }
}
