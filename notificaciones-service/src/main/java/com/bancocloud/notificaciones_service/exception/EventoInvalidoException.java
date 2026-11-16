package com.bancocloud.notificaciones_service.exception;

/**
 * Mensaje de Kafka bien formado pero con contenido inválido. No se reintenta: va directo al .DLT.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String message) {
        super(message);
    }
}
