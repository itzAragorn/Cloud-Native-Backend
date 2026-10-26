package com.bancocloud.notificaciones_service.consumer.dlq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Registra en log los mensajes que llegaron a una DLQ, usando el header {@code x-death}
 * que agrega RabbitMQ: cola de origen, motivo (rejected, expired, maxlen) y cantidad de veces.
 */
@Slf4j
@Service
public class DlqService {

    private static final int MAX_CUERPO_LOG = 500;

    public void registrar(Message message) {
        MessageProperties mp = message.getMessageProperties();
        List<Map<String, ?>> xDeath = mp.getXDeathHeader();
        Map<String, ?> muerte = (xDeath != null && !xDeath.isEmpty()) ? xDeath.get(0) : Map.of();

        log.error("[DLQ] mensaje {} en {} | cola origen={} motivo={} veces={} exchange={} routingKeys={} reintentos={} cuerpo={}",
                mp.getMessageId(),
                mp.getConsumerQueue(),
                muerte.get("queue"),
                muerte.get("reason"),
                muerte.get("count"),
                muerte.get("exchange"),
                muerte.get("routing-keys"),
                mp.getHeaders().getOrDefault("x-retry-count", 0),
                resumen(message.getBody()));
    }

    private String resumen(byte[] body) {
        if (body == null) {
            return "";
        }
        String texto = new String(body, StandardCharsets.UTF_8);
        return texto.length() > MAX_CUERPO_LOG ? texto.substring(0, MAX_CUERPO_LOG) + "..." : texto;
    }
}
