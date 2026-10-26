package com.bancocloud.notificaciones_service.messaging.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ completa que consume notificaciones-service.
 *
 * <pre>
 * bancocloud.inversiones (topic) ── inversion.creada ─────────────▶ inversiones.comprobante.q ─▶ ComprobanteListener
 * bancocloud.usuarios    (topic) ── usuario.registrado ───────────▶ usuarios.registro.q       ─▶ UsuarioRegistroListener
 * bancocloud.usuarios    (topic) ── usuario.estado.* ─────────────▶ usuarios.estado.q         ─▶ UsuarioEstadoListener
 * bancocloud.fondos      (topic) ── fondo.valorcuota.actualizado ─▶ fondos.valorcuota.q       ─▶ ValorCuotaListener
 *
 * bancocloud.dlx (direct) ── &lt;cola&gt;.dlq ──▶ &lt;cola&gt;.dlq ─▶ DlqListener
 * </pre>
 *
 * Cada cola principal declara {@code x-dead-letter-exchange} y {@code x-dead-letter-routing-key}:
 * un {@code basicNack(requeue=false)} la desvía a su DLQ. Los productores declaran la misma
 * topología de su dominio; las declaraciones son idempotentes.
 * <p>
 * No se define un convertidor JSON: los listeners reciben el {@code Message} crudo y
 * {@code MensajeAckHandler} lo deserializa, para que un JSON inválido también termine en NACK.
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    // ---------------------------------------------------------------- exchanges

    /** Eventos del dominio inversiones. */
    @Bean
    public TopicExchange inversionesExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().inversiones()).durable(true).build();
    }

    /** Eventos del dominio usuarios. */
    @Bean
    public TopicExchange usuariosExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().usuarios()).durable(true).build();
    }

    /** Eventos del dominio fondos. */
    @Bean
    public TopicExchange fondosExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().fondos()).durable(true).build();
    }

    /** Dead letter exchange compartido por todas las colas. */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.dlx()).durable(true).build();
    }

    // ---------------------------------------------------------------- comprobante de inversión

    /** Inversiones creadas pendientes de generar comprobante. */
    @Bean
    public Queue comprobanteQueue() {
        return colaConDlq(props.queues().comprobante(), props.queues().comprobanteDlq());
    }

    @Bean
    public Queue comprobanteDlq() {
        return QueueBuilder.durable(props.queues().comprobanteDlq()).build();
    }

    /** {@code bancocloud.inversiones} ── inversion.creada ──▶ {@code inversiones.comprobante.q}. */
    @Bean
    public Binding comprobanteBinding() {
        return BindingBuilder.bind(comprobanteQueue()).to(inversionesExchange())
                .with(props.routingKeys().inversionCreada());
    }

    @Bean
    public Binding comprobanteDlqBinding() {
        return BindingBuilder.bind(comprobanteDlq()).to(deadLetterExchange())
                .with(props.queues().comprobanteDlq());
    }

    // ---------------------------------------------------------------- usuario registrado

    /** Usuarios registrados: aviso al administrador. */
    @Bean
    public Queue registroQueue() {
        return colaConDlq(props.queues().registro(), props.queues().registroDlq());
    }

    @Bean
    public Queue registroDlq() {
        return QueueBuilder.durable(props.queues().registroDlq()).build();
    }

    /** {@code bancocloud.usuarios} ── usuario.registrado ──▶ {@code usuarios.registro.q}. */
    @Bean
    public Binding registroBinding() {
        return BindingBuilder.bind(registroQueue()).to(usuariosExchange())
                .with(props.routingKeys().usuarioRegistrado());
    }

    @Bean
    public Binding registroDlqBinding() {
        return BindingBuilder.bind(registroDlq()).to(deadLetterExchange())
                .with(props.queues().registroDlq());
    }

    // ---------------------------------------------------------------- cambio de estado de usuario

    /** Autorizaciones, bloqueos y desbloqueos: notificación al cliente. */
    @Bean
    public Queue estadoQueue() {
        return colaConDlq(props.queues().estado(), props.queues().estadoDlq());
    }

    @Bean
    public Queue estadoDlq() {
        return QueueBuilder.durable(props.queues().estadoDlq()).build();
    }

    /** {@code bancocloud.usuarios} ── usuario.estado.* ──▶ {@code usuarios.estado.q}. */
    @Bean
    public Binding estadoBinding() {
        return BindingBuilder.bind(estadoQueue()).to(usuariosExchange())
                .with(props.routingKeys().usuarioEstadoPattern());
    }

    @Bean
    public Binding estadoDlqBinding() {
        return BindingBuilder.bind(estadoDlq()).to(deadLetterExchange())
                .with(props.queues().estadoDlq());
    }

    // ---------------------------------------------------------------- valor cuota actualizado

    /** Cambios de valor cuota: aviso a inversionistas y registro. */
    @Bean
    public Queue valorCuotaQueue() {
        return colaConDlq(props.queues().valorcuota(), props.queues().valorcuotaDlq());
    }

    @Bean
    public Queue valorCuotaDlq() {
        return QueueBuilder.durable(props.queues().valorcuotaDlq()).build();
    }

    /** {@code bancocloud.fondos} ── fondo.valorcuota.actualizado ──▶ {@code fondos.valorcuota.q}. */
    @Bean
    public Binding valorCuotaBinding() {
        return BindingBuilder.bind(valorCuotaQueue()).to(fondosExchange())
                .with(props.routingKeys().valorcuotaActualizado());
    }

    @Bean
    public Binding valorCuotaDlqBinding() {
        return BindingBuilder.bind(valorCuotaDlq()).to(deadLetterExchange())
                .with(props.queues().valorcuotaDlq());
    }

    private Queue colaConDlq(String cola, String dlq) {
        return QueueBuilder.durable(cola)
                .deadLetterExchange(props.dlx())
                .deadLetterRoutingKey(dlq)
                .build();
    }
}
