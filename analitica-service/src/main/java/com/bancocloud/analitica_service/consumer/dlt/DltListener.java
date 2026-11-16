package com.bancocloud.analitica_service.consumer.dlt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Lee los dos tópicos de errores (.DLT) en el grupo {@code analitica-dlt} y los registra.
 * Recibe los mensajes como texto (pueden ser JSON roto) y nunca falla: un error al registrar
 * solo queda en el log, para no quedar atascado en un mensaje muerto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DltListener {

    private final DltService service;

    @KafkaListener(
            id = "analitica-dlt",
            topics = {
                    "${app.kafka.topics.inversiones}${app.kafka.dlt-suffix}",
                    "${app.kafka.topics.valor-cuota}${app.kafka.dlt-suffix}"},
            groupId = "${app.kafka.groups.analitica-dlt}",
            containerFactory = "dltListenerContainerFactory",
            concurrency = "1")
    public void onMensajeMuerto(ConsumerRecord<String, String> rec) {
        try {
            service.registrar(rec);
        } catch (RuntimeException e) {
            log.error("[DLT] error registrando mensaje de {} partition={} offset={}: {}",
                    rec.topic(), rec.partition(), rec.offset(), e.getMessage(), e);
        }
    }
}
