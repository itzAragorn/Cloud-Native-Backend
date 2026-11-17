package com.bancocloud.analitica_service.exception;

/**
 * El mensaje llegó bien formado pero su contenido no sirve (campos faltantes, montos negativos,
 * tipo de evento equivocado). No tiene sentido reintentarlo: el error handler lo envía directo
 * al .DLT.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String message) {
        super(message);
    }
}
