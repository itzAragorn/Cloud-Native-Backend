package com.bancocloud.analitica_service.consumer.fondos;

import com.bancocloud.analitica_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico de valor cuota en el grupo {@code analitica-fondos}.
 * <p>
 * La key es el fondoId: cada fondo cae siempre en la misma partición y la hebra que la atiende
 * recibe sus cambios en orden. El tópico es compactado, así que un mensaje con valor nulo
 * (tombstone) es legítimo: indica que se borró el último valor de ese fondo y se ignora.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValorCuotaAnaliticaListener {

    private final AnaliticaFondoService service;

    @KafkaListener(
            id = "analitica-fondos",
            topics = "${app.kafka.topics.valor-cuota}",
            groupId = "${app.kafka.groups.analitica-fondos}",
            containerFactory = "valorCuotaListenerContainerFactory")
    public void onValorCuota(@Payload(required = false) ValorCuotaKafkaEvent evento,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        if (evento == null) {
            log.info("Tombstone del fondo {} ignorado: partition={} offset={}", key, partition, offset);
            return;
        }
        log.debug("Valor cuota recibido: partition={} offset={} key={} eventId={}",
                partition, offset, key, evento.eventId());
        if (service.registrar(evento)) {
            log.info("Valor cuota del fondo {} registrado: {} -> {} ({}%) partition={} offset={}",
                    evento.fondoId(), evento.valorAnterior(), evento.valorNuevo(), evento.variacionPct(),
                    partition, offset);
        }
    }
}
