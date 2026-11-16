package com.bancocloud.inversiones_service.service;

import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;

/**
 * Publicación de las inversiones en el flujo de eventos para analítica (streaming).
 * La implementación vive en messaging.kafka.producer; la capa de servicio no conoce Kafka.
 * <p>
 * Es independiente de {@link InversionEventPublisher} (RabbitMQ): un fallo en un canal
 * no afecta al otro ni a la inversión, que ya quedó guardada.
 */
public interface InversionStreamPublisher {

    void publicarInversionRegistrada(InversionCreadaEvent event);
}
