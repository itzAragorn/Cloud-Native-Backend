package com.bancocloud.rabbit_admin_service.service;

import com.bancocloud.rabbit_admin_service.dto.BindingRequest;
import com.bancocloud.rabbit_admin_service.dto.ColaResponse;
import com.bancocloud.rabbit_admin_service.dto.CrearColaRequest;
import com.bancocloud.rabbit_admin_service.dto.CrearExchangeRequest;
import com.bancocloud.rabbit_admin_service.dto.ExchangeResponse;
import com.bancocloud.rabbit_admin_service.exception.ConfiguracionInvalidaException;
import com.bancocloud.rabbit_admin_service.exception.RecursoNoEncontradoException;
import com.bancocloud.rabbit_admin_service.exception.RecursoYaExisteException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

/**
 * Administración de colas, exchanges y bindings de RabbitMQ.
 * <p>
 * Es la única clase que conoce {@link RabbitAdmin}; los controladores solo llaman
 * a estos métodos y reciben DTOs o excepciones de negocio.
 * <p>
 * En AMQP borrar un recurso inexistente no es un error y no hay forma de listar
 * exchanges, por eso antes de cada operación se comprueba la existencia:
 * colas con {@code getQueueInfo} y exchanges con una declaración pasiva, que el
 * broker responde con 404 si el exchange no existe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private static final String COLA = "la cola";
    private static final String EXCHANGE = "el exchange";
    private static final int AMQP_NOT_FOUND = 404;

    private final RabbitAdmin rabbitAdmin;

    // ---------------------------------------------------------------- colas

    /**
     * Crea una cola con argumentos opcionales de dead letter y TTL.
     *
     * @throws RecursoYaExisteException       si la cola ya existe
     * @throws ConfiguracionInvalidaException si el dead letter exchange no existe
     */
    public ColaResponse crearCola(CrearColaRequest request) {
        if (existeCola(request.name())) {
            throw new RecursoYaExisteException(COLA, request.name());
        }
        if (request.deadLetterExchange() != null && !existeExchange(request.deadLetterExchange())) {
            throw new ConfiguracionInvalidaException("deadLetterExchange",
                    "el exchange '" + request.deadLetterExchange() + "' no existe; créalo antes de usarlo como DLX");
        }

        QueueBuilder builder = request.esDurable()
                ? QueueBuilder.durable(request.name())
                : QueueBuilder.nonDurable(request.name());
        if (request.deadLetterExchange() != null) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (request.deadLetterRoutingKey() != null) {
            builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        if (request.ttlMs() != null) {
            builder.ttl(request.ttlMs().intValue());
        }
        Queue queue = builder.build();

        rabbitAdmin.declareQueue(queue);
        log.info("Cola creada: {} durable={} args={}", queue.getName(), queue.isDurable(), queue.getArguments());
        return obtenerCola(request.name());
    }

    /** @throws RecursoNoEncontradoException si la cola no existe */
    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException(COLA, nombre);
        }
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    /** @throws RecursoNoEncontradoException si la cola no existe */
    public void eliminarCola(String nombre) {
        if (!existeCola(nombre)) {
            throw new RecursoNoEncontradoException(COLA, nombre);
        }
        rabbitAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    // ---------------------------------------------------------------- exchanges

    /** @throws RecursoYaExisteException si el exchange ya existe */
    public ExchangeResponse crearExchange(CrearExchangeRequest request) {
        if (existeExchange(request.name())) {
            throw new RecursoYaExisteException(EXCHANGE, request.name());
        }
        Exchange exchange = new ExchangeBuilder(request.name(), request.type().amqpType())
                .durable(request.esDurable())
                .build();

        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange creado: {} type={} durable={}", exchange.getName(), exchange.getType(), exchange.isDurable());
        return new ExchangeResponse(request.name(), request.type(), request.esDurable());
    }

    /** @throws RecursoNoEncontradoException si el exchange no existe */
    public void eliminarExchange(String nombre) {
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException(EXCHANGE, nombre);
        }
        rabbitAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ---------------------------------------------------------------- bindings

    /** @throws RecursoNoEncontradoException si falta la cola o el exchange */
    public BindingRequest crearBinding(BindingRequest request) {
        validarExtremosBinding(request);
        rabbitAdmin.declareBinding(toBinding(request));
        log.info("Binding creado: {} --[{}]--> {}", request.exchange(), request.routingKey(), request.queue());
        return request;
    }

    /** Elimina el binding. Es idempotente: si el binding no existía, no hace nada. */
    public void eliminarBinding(BindingRequest request) {
        validarExtremosBinding(request);
        rabbitAdmin.removeBinding(toBinding(request));
        log.info("Binding eliminado: {} --[{}]--> {}", request.exchange(), request.routingKey(), request.queue());
    }

    private void validarExtremosBinding(BindingRequest request) {
        if (!existeCola(request.queue())) {
            throw new RecursoNoEncontradoException(COLA, request.queue());
        }
        if (!existeExchange(request.exchange())) {
            throw new RecursoNoEncontradoException(EXCHANGE, request.exchange());
        }
    }

    private Binding toBinding(BindingRequest request) {
        return new Binding(request.queue(), Binding.DestinationType.QUEUE,
                request.exchange(), request.routingKey(), null);
    }

    // ---------------------------------------------------------------- existencia

    private boolean existeCola(String nombre) {
        return rabbitAdmin.getQueueInfo(nombre) != null;
    }

    /**
     * Declaración pasiva: el broker responde 404 y cierra el canal si el exchange no existe.
     * Un error de conexión se propaga (el handler lo traduce a 503).
     */
    boolean existeExchange(String nombre) {
        try {
            rabbitAdmin.getRabbitTemplate().execute(channel -> channel.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpConnectException e) {
            throw e;
        } catch (AmqpException e) {
            if (esNotFound(e)) {
                return false;
            }
            throw e;
        }
    }

    private boolean esNotFound(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ShutdownSignalException sse
                    && sse.getReason() instanceof AMQP.Channel.Close close
                    && close.getReplyCode() == AMQP_NOT_FOUND) {
                return true;
            }
        }
        return false;
    }
}
