package com.bancocloud.usuarios_service.messaging.event;

/**
 * Cambios de estado de usuario que se notifican. Cada uno se publica con su
 * propia routing key {@code usuario.estado.<cambio>}.
 */
public enum CambioEstadoUsuario {
    AUTORIZADO,
    DESAUTORIZADO,
    BLOQUEADO,
    DESBLOQUEADO
}
