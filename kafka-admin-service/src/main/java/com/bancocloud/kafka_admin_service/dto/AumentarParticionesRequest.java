package com.bancocloud.kafka_admin_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /topics/{name}/partitions}: el nuevo total de particiones. Kafka solo
 * permite aumentarlas; ojo, cambia en qué partición cae cada key desde ese momento.
 */
public record AumentarParticionesRequest(
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 100, message = "debe ser como máximo 100") Integer total) {
}
