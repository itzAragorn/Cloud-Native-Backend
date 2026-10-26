package com.bancocloud.usuarios_service.service;

import com.bancocloud.usuarios_service.messaging.event.UsuarioEstadoCambiadoEvent;
import com.bancocloud.usuarios_service.messaging.event.UsuarioRegistradoEvent;

/**
 * Publicación de eventos del dominio usuarios hacia otros servicios.
 * La implementación vive en messaging.producer; la capa de servicio no conoce RabbitMQ.
 */
public interface UsuarioEventPublisher {

    void publicarUsuarioRegistrado(UsuarioRegistradoEvent event);

    void publicarEstadoCambiado(UsuarioEstadoCambiadoEvent event);
}
