package com.bancocloud.rabbit_admin_service.dto;

public record ExchangeResponse(String name, TipoExchange type, boolean durable) {
}
