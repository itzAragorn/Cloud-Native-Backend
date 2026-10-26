package com.bancocloud.notificaciones_service.consumer.usuarios;

import com.bancocloud.notificaciones_service.exception.MensajeNoRecuperableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notificaciones del dominio usuarios. El envío es simulado: se registra en log
 * el aviso que recibiría el administrador o el cliente.
 */
@Slf4j
@Service
public class NotificacionUsuarioService {

    public void notificarRegistro(UsuarioRegistradoEvent event) {
        String situacion = Boolean.TRUE.equals(event.accesoAutorizado())
                ? "con acceso habilitado"
                : "pendiente de autorizar";
        log.info("[AVISO ADMIN] Nuevo usuario {} {} <{}> (id={}, rol={}) {}",
                event.nombre(), event.apellido() != null ? event.apellido() : "",
                event.email(), event.usuarioId(), event.rol(), situacion);
    }

    public void notificarCambioEstado(UsuarioEstadoCambiadoEvent event) {
        String texto = switch (event.cambio()) {
            case "AUTORIZADO" -> "tu cuenta Banco Cloud fue autorizada; ya puedes invertir";
            case "DESAUTORIZADO" -> "el acceso de tu cuenta Banco Cloud fue suspendido por un administrador";
            case "BLOQUEADO" -> "tu cuenta Banco Cloud fue bloqueada; contacta a soporte";
            case "DESBLOQUEADO" -> "tu cuenta Banco Cloud fue desbloqueada";
            default -> throw new MensajeNoRecuperableException("cambio de estado desconocido: " + event.cambio());
        };
        log.info("[EMAIL SIMULADO] Para: {} | Hola {}, {} (usuarioId={}, estado={})",
                event.email(), event.nombre(), texto, event.usuarioId(), event.estado());
    }
}
