package com.bancocloud.kafka_admin_service.dto;

import com.bancocloud.kafka_admin_service.validation.ConfigsPermitidas;
import com.bancocloud.kafka_admin_service.validation.NombreTopico;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.apache.kafka.common.config.TopicConfig;

/**
 * Cuerpo de {@code POST /topics}. Que {@code replicationFactor} no supere la cantidad de brokers
 * se revisa contra el clúster en el servicio.
 *
 * @param configs configuraciones de la lista blanca (por ejemplo {@code retention.ms})
 */
public record CrearTopicoRequest(
        @NombreTopico String name,
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 100, message = "debe ser como máximo 100") Integer partitions,
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 10, message = "debe ser como máximo 10") Short replicationFactor,
        @ConfigsPermitidas Map<String, String> configs) {

    public Map<String, String> configsOVacio() {
        return configs == null ? Map.of() : configs;
    }

    /** Con más réplicas en sincronía exigidas que réplicas, el tópico no aceptaría escrituras con acks=all. */
    @AssertTrue(message = "min.insync.replicas no puede ser mayor que replicationFactor")
    public boolean isMinInsyncCoherente() {
        String minIsr = configsOVacio().get(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG);
        if (minIsr == null || replicationFactor == null) {
            return true;
        }
        try {
            return Integer.parseInt(minIsr.trim()) <= replicationFactor;
        } catch (NumberFormatException e) {
            return true; // el formato lo informa @ConfigsPermitidas
        }
    }
}
