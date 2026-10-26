package com.bancocloud.fondos_service.service;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;

/**
 * Publicación de eventos del dominio fondos hacia otros servicios.
 * La implementación vive en messaging.producer; la capa de servicio no conoce RabbitMQ.
 */
public interface FondoEventPublisher {

    void publicarValorCuotaActualizado(ValorCuotaActualizadoEvent event);
}
