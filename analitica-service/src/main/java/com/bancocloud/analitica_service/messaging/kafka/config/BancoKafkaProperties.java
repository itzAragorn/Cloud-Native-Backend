package com.bancocloud.analitica_service.messaging.kafka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tópicos, consumer groups y reintentos de Kafka, leídos desde {@code app.kafka} en application.yml.
 * <p>
 * Es la única fuente de nombres de mensajería del servicio. Si falta un valor, no arranca.
 * La conexión al clúster y los deserializers están en {@code spring.kafka}.
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record BancoKafkaProperties(
        @NotNull @Valid Topics topics,
        @NotBlank String dltSuffix,
        @NotNull @Valid Groups groups,
        @NotNull @Valid Reintentos reintentos) {

    public record Topics(@NotBlank String inversiones, @NotBlank String valorCuota) {
    }

    /**
     * Un grupo por responsabilidad: cada grupo recibe todos los mensajes de su tópico y reparte
     * las particiones entre sus hebras e instancias.
     */
    public record Groups(
            @NotBlank String analiticaInversiones,
            @NotBlank String analiticaFondos,
            @NotBlank String analiticaDlt) {
    }

    /**
     * Reintentos con backoff exponencial ante errores transitorios, antes de enviar al .DLT.
     */
    public record Reintentos(
            @Min(0) int maxIntentos,
            @Min(1) long intervaloInicialMs,
            @DecimalMin("1.0") double multiplicador) {
    }

    public String dltDe(String topic) {
        return topic + dltSuffix;
    }
}
