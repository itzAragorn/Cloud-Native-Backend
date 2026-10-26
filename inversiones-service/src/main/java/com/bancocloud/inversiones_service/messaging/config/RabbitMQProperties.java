package com.bancocloud.inversiones_service.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys del dominio inversiones,
 * leídos desde {@code app.rabbitmq} en application.yml.
 * <p>
 * Es la única fuente de nombres de mensajería del servicio: ninguna otra
 * clase escribe un nombre de cola o exchange a mano. Si falta un valor,
 * el servicio no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMQProperties(
        @NotBlank String dlx,
        @NotNull @Valid Exchanges exchanges,
        @NotNull @Valid Queues queues,
        @NotNull @Valid RoutingKeys routingKeys) {

    /** Sufijo de las colas de mensajes muertos: {@code <cola>.dlq}. */
    public static final String DLQ_SUFFIX = ".dlq";

    public record Exchanges(@NotBlank String inversiones) {
    }

    public record Queues(@NotBlank String comprobante) {

        public String comprobanteDlq() {
            return comprobante + DLQ_SUFFIX;
        }
    }

    public record RoutingKeys(@NotBlank String inversionCreada) {
    }
}
