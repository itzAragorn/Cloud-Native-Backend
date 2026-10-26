package com.bancocloud.notificaciones_service.consumer.dlq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consume las cuatro DLQ, registra cada mensaje muerto en log y lo confirma con ACK.
 * <p>
 * Los nombres de las DLQ se toman de los beans {@code Queue} de RabbitMQConfig
 * (derivados de application.yml), así que aquí no hay nombres escritos a mano.
 * Siempre se hace ACK: un mensaje en la DLQ ya es terminal y rechazarlo de nuevo
 * solo lo haría circular.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqListener {

    private final DlqService dlqService;

    @RabbitListener(queues = {
            "#{comprobanteDlq.name}",
            "#{registroDlq.name}",
            "#{estadoDlq.name}",
            "#{valorCuotaDlq.name}"
    })
    public void onMensajeMuerto(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            dlqService.registrar(message);
        } catch (RuntimeException e) {
            log.error("Error registrando mensaje de DLQ {}", message.getMessageProperties().getConsumerQueue(), e);
        } finally {
            channel.basicAck(tag, false);
        }
    }
}
