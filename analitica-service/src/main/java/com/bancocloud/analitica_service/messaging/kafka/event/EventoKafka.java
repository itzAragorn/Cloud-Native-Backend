package com.bancocloud.analitica_service.messaging.kafka.event;

/**
 * Lo común a los mensajes de Kafka que consume el servicio: un id único para la idempotencia
 * y el log, y su tipo.
 */
public interface EventoKafka {

    String eventId();

    String eventType();
}
