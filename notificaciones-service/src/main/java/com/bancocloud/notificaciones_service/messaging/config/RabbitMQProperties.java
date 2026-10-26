package com.bancocloud.notificaciones_service.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys que consume notificaciones-service,
 * leídos desde {@code app.rabbitmq} en application.yml.
 * <p>
 * Es la única fuente de nombres de mensajería del servicio: ninguna otra
 * clase escribe un nombre de cola o exchange a mano. Si falta un valor,
 * el servicio no arranca.
 *
 * @param maxReintentos reintentos permitidos para errores recuperables antes de enviar a la DLQ
 */
@Validated
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMQProperties(
        @NotBlank String dlx,
        @Min(0) int maxReintentos,
        @NotNull @Valid Exchanges exchanges,
        @NotNull @Valid Queues queues,
        @NotNull @Valid RoutingKeys routingKeys) {

    /** Sufijo de las colas de mensajes muertos: {@code <cola>.dlq}. */
    public static final String DLQ_SUFFIX = ".dlq";

    public record Exchanges(
            @NotBlank String inversiones,
            @NotBlank String usuarios,
            @NotBlank String fondos) {
    }

    public record Queues(
            @NotBlank String comprobante,
            @NotBlank String registro,
            @NotBlank String estado,
            @NotBlank String valorcuota) {

        public String comprobanteDlq() {
            return comprobante + DLQ_SUFFIX;
        }

        public String registroDlq() {
            return registro + DLQ_SUFFIX;
        }

        public String estadoDlq() {
            return estado + DLQ_SUFFIX;
        }

        public String valorcuotaDlq() {
            return valorcuota + DLQ_SUFFIX;
        }
    }

    public record RoutingKeys(
            @NotBlank String inversionCreada,
            @NotBlank String usuarioRegistrado,
            @NotBlank String usuarioEstadoPattern,
            @NotBlank String valorcuotaActualizado) {
    }
}
