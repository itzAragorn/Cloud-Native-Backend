package com.bancocloud.inversiones_service.messaging.producer;

import com.bancocloud.inversiones_service.messaging.config.RabbitMQProperties;
import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;
import com.bancocloud.inversiones_service.service.InversionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de inversiones en el exchange {@code bancocloud.inversiones}.
 * <p>
 * Si RabbitMQ no está disponible el error se registra y no se propaga:
 * la inversión ya quedó guardada y la operación de negocio no debe fallar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitInversionEventPublisher implements InversionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    @Override
    public void publicarInversionCreada(InversionCreadaEvent event) {
        String exchange = props.exchanges().inversiones();
        String routingKey = props.routingKeys().inversionCreada();
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setMessageId(event.eventId());
                return message;
            }, new CorrelationData(event.eventId()));
            log.info("Evento {} publicado: inversionId={} exchange={} routingKey={}",
                    event.eventId(), event.inversionId(), exchange, routingKey);
        } catch (RuntimeException e) {
            log.error("No se pudo publicar evento {} (inversionId={}) en exchange={} routingKey={}: {}",
                    event.eventId(), event.inversionId(), exchange, routingKey, e.getMessage(), e);
        }
    }
}
