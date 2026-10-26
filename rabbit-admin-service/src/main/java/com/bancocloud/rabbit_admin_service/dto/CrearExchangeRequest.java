package com.bancocloud.rabbit_admin_service.dto;

import com.bancocloud.rabbit_admin_service.validation.NombreRecurso;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /exchanges}.
 *
 * @param durable sobrevive a reinicios del broker; por defecto {@code true}
 */
public record CrearExchangeRequest(
        @NombreRecurso String name,
        @NotNull(message = "es obligatorio (DIRECT, TOPIC, FANOUT o HEADERS)") TipoExchange type,
        Boolean durable) {

    public boolean esDurable() {
        return durable == null || durable;
    }
}
