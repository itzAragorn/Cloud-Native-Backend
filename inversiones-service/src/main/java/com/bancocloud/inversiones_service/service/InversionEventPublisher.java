package com.bancocloud.inversiones_service.service;

import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;

/**
 * Publicación de eventos del dominio inversiones hacia otros servicios.
 * La implementación vive en messaging.producer; la capa de servicio no conoce RabbitMQ.
 */
public interface InversionEventPublisher {

    void publicarInversionCreada(InversionCreadaEvent event);
}
