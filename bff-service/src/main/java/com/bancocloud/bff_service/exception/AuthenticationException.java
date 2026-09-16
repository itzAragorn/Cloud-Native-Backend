package com.bancocloud.bff_service.exception;

/**
 * Excepción para errores de autenticación
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
