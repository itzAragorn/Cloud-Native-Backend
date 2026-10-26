package com.bancocloud.notificaciones_service.consumer.inversiones;

import com.bancocloud.notificaciones_service.consumer.support.MensajeAckHandler;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consume {@code inversiones.comprobante.q} y genera el comprobante de cada inversión.
 * ACK/NACK según la política de {@link MensajeAckHandler}.
 */
@Component
@RequiredArgsConstructor
public class ComprobanteListener {

    private final MensajeAckHandler ackHandler;
    private final ComprobanteService comprobanteService;

    @RabbitListener(queues = "${app.rabbitmq.queues.comprobante}")
    public void onInversionCreada(Message message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        ackHandler.procesar(message, channel, tag, InversionCreadaEvent.class, comprobanteService::generar);
    }
}
