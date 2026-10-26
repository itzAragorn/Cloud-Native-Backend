package com.bancocloud.rabbit_admin_service.dto;

import org.springframework.amqp.core.ExchangeTypes;

/** Tipos de exchange admitidos por la API. */
public enum TipoExchange {
    DIRECT(ExchangeTypes.DIRECT),
    TOPIC(ExchangeTypes.TOPIC),
    FANOUT(ExchangeTypes.FANOUT),
    HEADERS(ExchangeTypes.HEADERS);

    private final String amqpType;

    TipoExchange(String amqpType) {
        this.amqpType = amqpType;
    }

    /** Nombre del tipo en el protocolo AMQP ({@code direct}, {@code topic}, ...). */
    public String amqpType() {
        return amqpType;
    }
}
