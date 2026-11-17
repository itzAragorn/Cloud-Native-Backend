package com.bancocloud.analitica_service.consumer.inversiones;

import com.bancocloud.analitica_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico de inversiones en el grupo {@code analitica-inversiones}.
 * <p>
 * <b>Consumer group:</b> el tópico tiene 3 particiones y el grupo corre con
 * {@code concurrency 3}, así que cada hebra recibe una partición (carga dividida). Si se
 * levanta otra instancia del servicio, Kafka reparte las particiones entre ambas. El grupo
 * {@code auditoria-inversiones} de notificaciones-service lee el mismo tópico por separado y
 * recibe también todos los mensajes (responsabilidad compartida).
 * <p>
 * Solo traduce el mensaje y deja el contexto en el log; la lógica está en
 * {@link AnaliticaInversionService}. Los errores los maneja el {@code DefaultErrorHandler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InversionAnaliticaListener {

    private final AnaliticaInversionService service;

    @KafkaListener(
            id = "analitica-inversiones",
            topics = "${app.kafka.topics.inversiones}",
            groupId = "${app.kafka.groups.analitica-inversiones}",
            containerFactory = "inversionesListenerContainerFactory")
    public void onInversion(@Payload(required = false) InversionRegistradaKafkaEvent evento,
                            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        log.debug("Inversión recibida: partition={} offset={} key={} eventId={}",
                partition, offset, key, evento == null ? null : evento.eventId());
        boolean nuevo = service.registrar(evento);
        if (nuevo) {
            log.info("Inversión {} registrada en analítica: fondoId={} monto={} partition={} offset={}",
                    evento.inversionId(), evento.fondoId(), evento.monto(), partition, offset);
        }
    }
}
