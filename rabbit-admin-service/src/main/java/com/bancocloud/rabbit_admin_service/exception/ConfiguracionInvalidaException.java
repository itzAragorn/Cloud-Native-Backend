package com.bancocloud.rabbit_admin_service.exception;

import lombok.Getter;

/**
 * La petición es sintácticamente válida pero no se puede aplicar en el broker,
 * por ejemplo un dead letter exchange que no existe (HTTP 400 con el campo afectado).
 */
@Getter
public class ConfiguracionInvalidaException extends RuntimeException {

    private final String campo;

    public ConfiguracionInvalidaException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }
}
