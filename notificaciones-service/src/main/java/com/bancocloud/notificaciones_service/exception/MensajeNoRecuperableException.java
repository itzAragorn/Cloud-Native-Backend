package com.bancocloud.notificaciones_service.exception;

/**
 * El mensaje nunca se podrá procesar: JSON inválido, campos faltantes o valores sin sentido.
 * Reintentar no sirve; el mensaje se rechaza sin reencolar y va directo a la DLQ.
 */
public class MensajeNoRecuperableException extends RuntimeException {

    public MensajeNoRecuperableException(String message) {
        super(message);
    }

    public MensajeNoRecuperableException(String message, Throwable cause) {
        super(message, cause);
    }
}
