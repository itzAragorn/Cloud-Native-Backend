package com.bancocloud.notificaciones_service.consumer.auditoria;

import com.bancocloud.notificaciones_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del tópico de inversiones en el grupo {@code auditoria-inversiones}.
 * <p>
 * Comparte el tópico con el grupo {@code analitica-inversiones} de analitica-service: al ser
 * grupos distintos, cada uno recibe todos los mensajes para su propia responsabilidad. Dentro del
 * grupo, las 3 hebras ({@code concurrency 3}) se reparten las 3 particiones.
 * La lógica está en {@link AuditoriaInversionService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InversionAuditoriaKafkaListener {

    private final AuditoriaInversionService service;

    @KafkaListener(
            id = "auditoria-inversiones",
            topics = "${app.kafka.topics.inversiones}",
            groupId = "${app.kafka.groups.auditoria-inversiones}")
    public void onInversion(@Payload(required = false) InversionRegistradaKafkaEvent evento,
                            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        log.debug("Inversión para auditoría: partition={} offset={} key={} eventId={}",
                partition, offset, key, evento == null ? null : evento.eventId());
        service.auditar(evento, partition, offset);
    }
}
