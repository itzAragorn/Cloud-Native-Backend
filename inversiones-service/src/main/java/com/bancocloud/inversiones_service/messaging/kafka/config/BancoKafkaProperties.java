package com.bancocloud.inversiones_service.messaging.kafka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tópicos de Kafka del dominio inversiones, leídos desde {@code app.kafka} en application.yml.
 * <p>
 * Es la única fuente de nombres y parámetros de tópicos del servicio: ninguna otra clase
 * escribe un nombre de tópico a mano. Si falta un valor o es inválido, el servicio no arranca.
 * La conexión al clúster (bootstrap servers, serializers, acks) está en {@code spring.kafka}.
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record BancoKafkaProperties(
        @NotNull @Valid Topics topics,
        @NotNull @Valid Dlt dlt) {

    public record Topics(@NotNull @Valid TopicSpec inversiones) {
    }

    /**
     * Parámetros de un tópico. Se aplican al crearlo (ver {@code KafkaTopicConfig}).
     *
     * @param cleanupPolicy {@code delete}, {@code compact} o {@code compact,delete}
     */
    public record TopicSpec(
            @NotBlank String name,
            @Min(1) int partitions,
            @Min(1) @Max(Short.MAX_VALUE) int replicas,
            @Min(1) int minInsyncReplicas,
            @NotBlank @Pattern(regexp = "delete|compact|compact,delete") String cleanupPolicy,
            @Positive long retentionMs) {

        /** Con más réplicas en sincronía que réplicas, ninguna escritura con acks=all sería aceptada. */
        @AssertTrue(message = "min-insync-replicas no puede ser mayor que replicas")
        public boolean isMinInsyncReplicasValido() {
            return minInsyncReplicas <= replicas;
        }
    }

    /**
     * Tópicos de errores (dead letter topics): {@code <tópico><suffix>}.
     */
    public record Dlt(@NotBlank String suffix, @Positive long retentionMs) {

        public String nombrePara(TopicSpec topic) {
            return topic.name() + suffix;
        }
    }
}
