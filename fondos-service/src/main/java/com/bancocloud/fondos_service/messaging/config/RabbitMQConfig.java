package com.bancocloud.fondos_service.messaging.config;

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
 * Topología RabbitMQ del dominio fondos.
 *
 * <pre>
 * Valor cuota actualizado
 *   fondos-service ── fondo.valorcuota.actualizado ──▶ bancocloud.fondos (topic)
 *                                                        └─▶ fondos.valorcuota.q ──▶ notificaciones-service
 *   rechazo / reintentos agotados ──▶ bancocloud.dlx (direct)
 *                                        └─ fondos.valorcuota.q.dlq ──▶ fondos.valorcuota.q.dlq
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

    /** Exchange de eventos del dominio fondos. */
    @Bean
    public TopicExchange fondosExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().fondos()).durable(true).build();
    }

    /** Dead letter exchange compartido: recibe los mensajes rechazados de todas las colas. */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.dlx()).durable(true).build();
    }

    /**
     * Cola de cambios de valor cuota. Un NACK sin reencolar envía el mensaje al DLX
     * con routing key {@code fondos.valorcuota.q.dlq}.
     */
    @Bean
    public Queue valorCuotaQueue() {
        return QueueBuilder.durable(props.queues().valorcuota())
                .deadLetterExchange(props.dlx())
                .deadLetterRoutingKey(props.queues().valorcuotaDlq())
                .build();
    }

    /** DLQ de cambios de valor cuota. */
    @Bean
    public Queue valorCuotaDlq() {
        return QueueBuilder.durable(props.queues().valorcuotaDlq()).build();
    }

    /** {@code bancocloud.fondos} ── fondo.valorcuota.actualizado ──▶ {@code fondos.valorcuota.q}. */
    @Bean
    public Binding valorCuotaBinding() {
        return BindingBuilder.bind(valorCuotaQueue())
                .to(fondosExchange())
                .with(props.routingKeys().valorcuotaActualizado());
    }

    /** {@code bancocloud.dlx} ── fondos.valorcuota.q.dlq ──▶ {@code fondos.valorcuota.q.dlq}. */
    @Bean
    public Binding valorCuotaDlqBinding() {
        return BindingBuilder.bind(valorCuotaDlq())
                .to(deadLetterExchange())
                .with(props.queues().valorcuotaDlq());
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
