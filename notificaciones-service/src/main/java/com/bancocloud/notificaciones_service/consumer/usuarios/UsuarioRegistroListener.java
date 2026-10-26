package com.bancocloud.notificaciones_service.consumer.usuarios;

import com.bancocloud.notificaciones_service.consumer.support.MensajeAckHandler;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consume {@code usuarios.registro.q} y avisa al administrador de cada nuevo usuario.
 * ACK/NACK según la política de {@link MensajeAckHandler}.
 */
@Component
@RequiredArgsConstructor
public class UsuarioRegistroListener {

    private final MensajeAckHandler ackHandler;
    private final NotificacionUsuarioService notificacionService;

    @RabbitListener(queues = "${app.rabbitmq.queues.registro}")
    public void onUsuarioRegistrado(Message message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        ackHandler.procesar(message, channel, tag, UsuarioRegistradoEvent.class,
                notificacionService::notificarRegistro);
    }
}
