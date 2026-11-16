package com.bancocloud.kafka_admin_service.exception;

import lombok.Getter;

/** Un valor válido por sí solo pero no para este clúster, por ejemplo más réplicas que brokers (400). */
@Getter
public class ConfiguracionInvalidaException extends RuntimeException {

    private final String campo;

    public ConfiguracionInvalidaException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
}
