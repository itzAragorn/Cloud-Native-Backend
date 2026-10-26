package com.bancocloud.rabbit_admin_service.dto;

import com.bancocloud.rabbit_admin_service.validation.NombreRecurso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /bindings} y {@code DELETE /bindings}.
 *
 * @param routingKey puede ser vacía (exchanges fanout) y admite comodines {@code *} y {@code #} (topic)
 */
public record BindingRequest(
        @NombreRecurso String queue,
        @NombreRecurso String exchange,
        @NotNull(message = "es obligatorio (puede ser vacío para exchanges fanout)")
        @Size(max = 255, message = "debe tener como máximo 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._*#-]*$", message = "solo admite letras, números, '.', '_', '-', '*' y '#'")
        String routingKey) {
}
