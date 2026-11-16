package com.bancocloud.kafka_admin_service.exception;

/** El tópico o consumer group no existe (404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
