package com.bancocloud.usuarios_service.messaging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ del dominio usuarios.
 *
 * <pre>
 * Usuario registrado
 *   usuarios-service ── usuario.registrado ──▶ bancocloud.usuarios (topic)
 *                                                └─▶ usuarios.registro.q ──▶ notificaciones-service
 * Cambio de estado (autorizado, bloqueado, desbloqueado)
 *   usuarios-service ── usuario.estado.{estado} ──▶ bancocloud.usuarios (topic)
 *                                                    └─ usuario.estado.* ─▶ usuarios.estado.q ──▶ notificaciones-service
 * Rechazo / reintentos agotados ──▶ bancocloud.dlx (direct)
 *   ├─ usuarios.registro.q.dlq ──▶ usuarios.registro.q.dlq
 *   └─ usuarios.estado.q.dlq   ──▶ usuarios.estado.q.dlq
 * </pre>
 *
 * Los nombres salen de {@link RabbitMQProperties}. Las declaraciones son idempotentes:
 * notificaciones-service declara la misma topología, y el primero que se conecte la crea.
 * RabbitAdmin las aplica al abrir la primera conexión, así que el servicio arranca aunque
 * el broker no esté disponible.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    /** Exchange de eventos del dominio usuarios. */
    @Bean
    public TopicExchange usuariosExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().usuarios()).durable(true).build();
    }

    /** Dead letter exchange compartido: recibe los mensajes rechazados de todas las colas. */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.dlx()).durable(true).build();
    }

    /**
     * Cola de usuarios registrados pendientes de autorizar. Un NACK sin reencolar
     * envía el mensaje al DLX con routing key {@code usuarios.registro.q.dlq}.
     */
    @Bean
    public Queue registroQueue() {
        return QueueBuilder.durable(props.queues().registro())
                .deadLetterExchange(props.dlx())
                .deadLetterRoutingKey(props.queues().registroDlq())
                .build();
    }

    /** DLQ de usuarios registrados. */
    @Bean
    public Queue registroDlq() {
        return QueueBuilder.durable(props.queues().registroDlq()).build();
    }

    /**
     * Cola de cambios de estado de usuario. Un NACK sin reencolar envía el mensaje
     * al DLX con routing key {@code usuarios.estado.q.dlq}.
     */
    @Bean
    public Queue estadoQueue() {
        return QueueBuilder.durable(props.queues().estado())
                .deadLetterExchange(props.dlx())
                .deadLetterRoutingKey(props.queues().estadoDlq())
                .build();
    }

    /** DLQ de cambios de estado de usuario. */
    @Bean
    public Queue estadoDlq() {
        return QueueBuilder.durable(props.queues().estadoDlq()).build();
    }

    /** {@code bancocloud.usuarios} ── usuario.registrado ──▶ {@code usuarios.registro.q}. */
    @Bean
    public Binding registroBinding() {
        return BindingBuilder.bind(registroQueue())
                .to(usuariosExchange())
                .with(props.routingKeys().usuarioRegistrado());
    }

    /**
     * {@code bancocloud.usuarios} ── usuario.estado.* ──▶ {@code usuarios.estado.q}.
     * El comodín recibe autorizado, bloqueado y desbloqueado con una sola cola.
     */
    @Bean
    public Binding estadoBinding() {
        return BindingBuilder.bind(estadoQueue())
                .to(usuariosExchange())
                .with(props.routingKeys().usuarioEstadoPattern());
    }

    /** {@code bancocloud.dlx} ── usuarios.registro.q.dlq ──▶ {@code usuarios.registro.q.dlq}. */
    @Bean
    public Binding registroDlqBinding() {
        return BindingBuilder.bind(registroDlq())
                .to(deadLetterExchange())
                .with(props.queues().registroDlq());
    }

    /** {@code bancocloud.dlx} ── usuarios.estado.q.dlq ──▶ {@code usuarios.estado.q.dlq}. */
    @Bean
    public Binding estadoDlqBinding() {
        return BindingBuilder.bind(estadoDlq())
                .to(deadLetterExchange())
                .with(props.queues().estadoDlq());
    }

    /** Serializa los eventos como JSON (Jackson 3). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * RabbitTemplate con la configuración de application.yml (convertidor JSON, mandatory)
     * y callbacks que registran en log los mensajes que el broker no confirma o no puede enrutar.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connectionFactory);
        template.setConfirmCallback((correlation, ack, cause) -> {
            String id = correlation != null ? correlation.getId() : "sin-id";
            if (ack) {
                log.debug("Mensaje {} confirmado por el broker", id);
            } else {
                log.error("Mensaje {} NO confirmado por el broker: {}", id, cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "Mensaje devuelto: sin cola para exchange={} routingKey={} code={} motivo={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText()));
        return template;
    }
}
