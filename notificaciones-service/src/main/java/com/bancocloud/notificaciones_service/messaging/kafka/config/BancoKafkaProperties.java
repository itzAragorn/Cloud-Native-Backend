package com.bancocloud.notificaciones_service.messaging.kafka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tópicos, consumer group y reintentos de Kafka, leídos desde {@code app.kafka} en application.yml.
 * Independiente de {@code RabbitMQProperties}.
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record BancoKafkaProperties(
        @NotNull @Valid Topics topics,
        @NotBlank String dltSuffix,
        @NotNull @Valid Groups groups,
        @NotNull @Valid Reintentos reintentos) {

    public record Topics(@NotBlank String inversiones) {
    }

    public record Groups(@NotBlank String auditoriaInversiones) {
    }

    public record Reintentos(
            @Min(0) int maxIntentos,
            @Min(1) long intervaloInicialMs,
            @DecimalMin("1.0") double multiplicador) {
    }

    public String dltDe(String topic) {
        return topic + dltSuffix;
    }
}
