package com.bancocloud.notificaciones_service.exception;

/**
 * Falla transitoria (disco no disponible, timeout, dependencia caída): el mensaje es válido
 * y puede procesarse más tarde. Se reintenta hasta {@code app.rabbitmq.max-reintentos} veces;
 * agotados los intentos, va a la DLQ.
 */
public class MensajeRecuperableException extends RuntimeException {

    public MensajeRecuperableException(String message) {
        super(message);
    }

    public MensajeRecuperableException(String message, Throwable cause) {
        super(message, cause);
    }
}
