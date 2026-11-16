package com.bancocloud.kafka_admin_service.dto;

import com.bancocloud.kafka_admin_service.validation.ConfigsPermitidas;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Cuerpo de {@code PUT /topics/{name}/configs}. Solo cambia las claves enviadas; un valor
 * {@code null} devuelve esa clave al valor por defecto del clúster.
 */
public record ModificarConfigsRequest(
        @NotEmpty(message = "debe incluir al menos una configuración")
        @ConfigsPermitidas(permitirNulos = true)
        Map<String, String> configs) {
}
