package com.bancocloud.analitica_service.consumer.dlt;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

/**
 * Registra los mensajes muertos con los headers que agrega el {@code DeadLetterPublishingRecoverer}:
 * tópico, partición y offset de origen, grupo que falló y motivo. Nunca lanza excepciones.
 */
@Slf4j
@Service
public class DltService {

    public void registrar(ConsumerRecord<String, String> rec) {
        Headers headers = rec.headers();
        log.error("[DLT] {} partition={} offset={} key={} | origen={} partition={} offset={} grupo={} "
                        + "excepcion={} motivo={} | payload={}",
                rec.topic(), rec.partition(), rec.offset(), rec.key(),
                texto(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                entero(headers, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                largo(headers, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                texto(headers, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP),
                texto(headers, KafkaHeaders.DLT_EXCEPTION_FQCN),
                texto(headers, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                recortar(rec.value()));
    }

    static String texto(Headers headers, String nombre) {
        Header h = headers.lastHeader(nombre);
        return h == null || h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    static Integer entero(Headers headers, String nombre) {
        Header h = headers.lastHeader(nombre);
        return h == null || h.value() == null || h.value().length != Integer.BYTES
                ? null : ByteBuffer.wrap(h.value()).getInt();
    }

    static Long largo(Headers headers, String nombre) {
        Header h = headers.lastHeader(nombre);
        return h == null || h.value() == null || h.value().length != Long.BYTES
                ? null : ByteBuffer.wrap(h.value()).getLong();
    }

    private static String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= 500 ? valor : valor.substring(0, 500) + "...";
    }
}
