package com.bancocloud.kafka_admin_service.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param timeoutMs espera máxima por operación del AdminClient antes de responder 503
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka-admin")
public record KafkaAdminProperties(@Min(1000) long timeoutMs) {
}
