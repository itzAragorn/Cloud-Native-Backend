package com.bancocloud.kafka_admin_service.exception;

/** El clúster no respondió a tiempo (503). */
public class KafkaNoDisponibleException extends RuntimeException {

    public KafkaNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
