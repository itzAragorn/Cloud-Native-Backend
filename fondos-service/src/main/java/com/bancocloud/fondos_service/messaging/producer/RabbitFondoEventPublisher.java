package com.bancocloud.fondos_service.messaging.producer;

import com.bancocloud.fondos_service.messaging.config.RabbitMQProperties;
import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import com.bancocloud.fondos_service.service.FondoEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de fondos en el exchange {@code bancocloud.fondos}.
 * <p>
 * Si RabbitMQ no está disponible el error se registra y no se propaga:
 * el nuevo valor cuota ya quedó guardado y la operación de negocio no debe fallar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitFondoEventPublisher implements FondoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    @Override
    public void publicarValorCuotaActualizado(ValorCuotaActualizadoEvent event) {
        String exchange = props.exchanges().fondos();
        String routingKey = props.routingKeys().valorcuotaActualizado();
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setMessageId(event.eventId());
                return message;
            }, new CorrelationData(event.eventId()));
            log.info("Evento {} publicado: fondoId={} exchange={} routingKey={}",
                    event.eventId(), event.fondoId(), exchange, routingKey);
        } catch (RuntimeException e) {
            log.error("No se pudo publicar evento {} (fondoId={}) en exchange={} routingKey={}: {}",
                    event.eventId(), event.fondoId(), exchange, routingKey, e.getMessage(), e);
        }
    }
}
