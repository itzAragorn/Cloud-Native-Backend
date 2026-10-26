package com.bancocloud.rabbit_admin_service.dto;

import com.bancocloud.rabbit_admin_service.validation.NoReservado;
import com.bancocloud.rabbit_admin_service.validation.NombreRecurso;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /queues}.
 *
 * @param durable              sobrevive a reinicios del broker; por defecto {@code true}
 * @param deadLetterExchange   exchange al que van los mensajes rechazados o expirados; debe existir
 * @param deadLetterRoutingKey routing key con que se reenvían al DLX; requiere {@code deadLetterExchange}
 * @param ttlMs                tiempo de vida de cada mensaje en milisegundos
 */
public record CrearColaRequest(
        @NombreRecurso String name,
        Boolean durable,
        @Size(max = 100, message = "debe tener como máximo 100 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "solo admite letras, números, '.', '_' y '-'")
        @NoReservado
        String deadLetterExchange,
        @Size(max = 255, message = "debe tener como máximo 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._*#-]*$", message = "solo admite letras, números, '.', '_', '-', '*' y '#'")
        String deadLetterRoutingKey,
        @Positive(message = "debe ser mayor que 0")
        @Max(value = Integer.MAX_VALUE, message = "debe ser como máximo 2147483647 (límite de RabbitMQ)")
        Long ttlMs) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    /** Una routing key de dead letter sin exchange de dead letter no tiene efecto. */
    @AssertTrue(message = "requiere deadLetterExchange")
    public boolean isDeadLetterRoutingKey() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }
}
