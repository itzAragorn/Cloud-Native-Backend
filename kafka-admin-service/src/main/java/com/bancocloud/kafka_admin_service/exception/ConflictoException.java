package com.bancocloud.kafka_admin_service.exception;

/** La operación choca con el estado actual: el grupo tiene consumidores activos, etc. (409). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }
}
