package com.bancocloud.notificaciones_service.consumer.fondos;

import com.bancocloud.notificaciones_service.consumer.support.MensajeAckHandler;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consume {@code fondos.valorcuota.q} y avisa a los inversionistas de cada cambio de valor cuota.
 * ACK/NACK según la política de {@link MensajeAckHandler}.
 */
@Component
@RequiredArgsConstructor
public class ValorCuotaListener {

    private final MensajeAckHandler ackHandler;
    private final ValorCuotaNotificacionService notificacionService;

    @RabbitListener(queues = "${app.rabbitmq.queues.valorcuota}")
    public void onValorCuotaActualizado(Message message, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        ackHandler.procesar(message, channel, tag, ValorCuotaActualizadoEvent.class,
                notificacionService::notificarCambio);
    }
}
