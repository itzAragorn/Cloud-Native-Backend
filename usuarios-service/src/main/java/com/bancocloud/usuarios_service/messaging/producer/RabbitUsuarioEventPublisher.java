package com.bancocloud.usuarios_service.messaging.producer;

import com.bancocloud.usuarios_service.messaging.config.RabbitMQProperties;
import com.bancocloud.usuarios_service.messaging.event.CambioEstadoUsuario;
import com.bancocloud.usuarios_service.messaging.event.UsuarioEstadoCambiadoEvent;
import com.bancocloud.usuarios_service.messaging.event.UsuarioRegistradoEvent;
import com.bancocloud.usuarios_service.service.UsuarioEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de usuarios en el exchange {@code bancocloud.usuarios}.
 * <p>
 * Si RabbitMQ no está disponible el error se registra y no se propaga:
 * el cambio ya quedó guardado y la operación de negocio no debe fallar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitUsuarioEventPublisher implements UsuarioEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    @Override
    public void publicarUsuarioRegistrado(UsuarioRegistradoEvent event) {
        enviar(props.routingKeys().usuarioRegistrado(), event.eventId(), event.usuarioId(), event);
    }

    @Override
    public void publicarEstadoCambiado(UsuarioEstadoCambiadoEvent event) {
        enviar(routingKeyDe(event.cambio()), event.eventId(), event.usuarioId(), event);
    }

    private String routingKeyDe(CambioEstadoUsuario cambio) {
        RabbitMQProperties.RoutingKeys keys = props.routingKeys();
        return switch (cambio) {
            case AUTORIZADO -> keys.usuarioAutorizado();
            case DESAUTORIZADO -> keys.usuarioDesautorizado();
            case BLOQUEADO -> keys.usuarioBloqueado();
            case DESBLOQUEADO -> keys.usuarioDesbloqueado();
        };
    }

    private void enviar(String routingKey, String eventId, Long usuarioId, Object event) {
        String exchange = props.exchanges().usuarios();
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setMessageId(eventId);
                return message;
            }, new CorrelationData(eventId));
            log.info("Evento {} publicado: usuarioId={} exchange={} routingKey={}",
                    eventId, usuarioId, exchange, routingKey);
        } catch (RuntimeException e) {
            log.error("No se pudo publicar evento {} (usuarioId={}) en exchange={} routingKey={}: {}",
                    eventId, usuarioId, exchange, routingKey, e.getMessage(), e);
        }
    }
}
