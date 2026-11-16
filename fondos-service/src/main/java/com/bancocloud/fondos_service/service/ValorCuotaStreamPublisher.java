package com.bancocloud.fondos_service.service;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;

/**
 * Publicación de los cambios de valor cuota en el flujo de eventos para analítica (streaming).
 * La implementación vive en messaging.kafka.producer; la capa de servicio no conoce Kafka.
 * <p>
 * Es independiente de {@link FondoEventPublisher} (RabbitMQ): un fallo en un canal
 * no afecta al otro ni al cambio de valor cuota, que ya quedó guardado.
 */
public interface ValorCuotaStreamPublisher {

    void publicarValorCuota(ValorCuotaActualizadoEvent event);
}
