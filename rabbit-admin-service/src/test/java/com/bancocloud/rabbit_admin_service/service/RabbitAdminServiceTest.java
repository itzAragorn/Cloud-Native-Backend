package com.bancocloud.rabbit_admin_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancocloud.rabbit_admin_service.dto.BindingRequest;
import com.bancocloud.rabbit_admin_service.dto.ColaResponse;
import com.bancocloud.rabbit_admin_service.dto.CrearColaRequest;
import com.bancocloud.rabbit_admin_service.dto.CrearExchangeRequest;
import com.bancocloud.rabbit_admin_service.dto.TipoExchange;
import com.bancocloud.rabbit_admin_service.exception.ConfiguracionInvalidaException;
import com.bancocloud.rabbit_admin_service.exception.RecursoNoEncontradoException;
import com.bancocloud.rabbit_admin_service.exception.RecursoYaExisteException;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQImpl;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitAdminServiceTest {

    private final RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitAdminService service = new RabbitAdminService(rabbitAdmin);

    @BeforeEach
    void setUp() {
        when(rabbitAdmin.getRabbitTemplate()).thenReturn(rabbitTemplate);
    }

    /** Simula que el broker conoce (o no) los exchanges consultados con declaración pasiva. */
    @SuppressWarnings("unchecked")
    private void exchangeExiste(boolean existe) {
        if (existe) {
            when(rabbitTemplate.execute(any(ChannelCallback.class))).thenReturn(null);
        } else {
            ShutdownSignalException notFound = new ShutdownSignalException(false, false,
                    new AMQImpl.Channel.Close(404, "NOT_FOUND - no exchange", 40, 10), null);
            when(rabbitTemplate.execute(any(ChannelCallback.class)))
                    .thenThrow(new AmqpIOException(new IOException(notFound)));
        }
    }

    private void colaExiste(String nombre) {
        when(rabbitAdmin.getQueueInfo(nombre)).thenReturn(new QueueInformation(nombre, 3, 1));
    }

    // ---------------------------------------------------------------- colas

    @Test
    void crearColaConDlxYTtlDeclaraArgumentos() {
        exchangeExiste(true);
        when(rabbitAdmin.getQueueInfo("demo.q"))
                .thenReturn(null)
                .thenReturn(new QueueInformation("demo.q", 0, 0));

        ColaResponse response = service.crearCola(
                new CrearColaRequest("demo.q", true, "demo.dlx", "demo.q.dlq", 60000L));

        ArgumentCaptor<Queue> queue = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(queue.capture());
        assertThat(queue.getValue().isDurable()).isTrue();
        assertThat(queue.getValue().getArguments())
                .containsEntry("x-dead-letter-exchange", "demo.dlx")
                .containsEntry("x-dead-letter-routing-key", "demo.q.dlq")
                .containsEntry("x-message-ttl", 60000);
        assertThat(response.name()).isEqualTo("demo.q");
    }

    @Test
    void crearColaNoDurableSinArgumentos() {
        when(rabbitAdmin.getQueueInfo("tmp.q"))
                .thenReturn(null)
                .thenReturn(new QueueInformation("tmp.q", 0, 0));

        service.crearCola(new CrearColaRequest("tmp.q", false, null, null, null));

        ArgumentCaptor<Queue> queue = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(queue.capture());
        assertThat(queue.getValue().isDurable()).isFalse();
        assertThat(queue.getValue().getArguments()).isEmpty();
    }

    @Test
    void crearColaExistenteDa409() {
        colaExiste("demo.q");

        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("demo.q", true, null, null, null)))
                .isInstanceOf(RecursoYaExisteException.class);
        verify(rabbitAdmin, never()).declareQueue(any());
    }

    @Test
    void crearColaConDlxInexistenteEsConfiguracionInvalida() {
        exchangeExiste(false);

        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("demo.q", true, "no.existe", null, null)))
                .isInstanceOf(ConfiguracionInvalidaException.class)
                .extracting("campo").isEqualTo("deadLetterExchange");
        verify(rabbitAdmin, never()).declareQueue(any());
    }

    @Test
    void obtenerColaDevuelveMensajesYConsumidores() {
        colaExiste("demo.q");

        assertThat(service.obtenerCola("demo.q")).isEqualTo(new ColaResponse("demo.q", 3, 1));
    }

    @Test
    void obtenerColaInexistenteDa404() {
        assertThatThrownBy(() -> service.obtenerCola("no.q")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarColaExistente() {
        colaExiste("demo.q");

        service.eliminarCola("demo.q");

        verify(rabbitAdmin).deleteQueue("demo.q");
    }

    @Test
    void eliminarColaInexistenteDa404() {
        assertThatThrownBy(() -> service.eliminarCola("no.q")).isInstanceOf(RecursoNoEncontradoException.class);
        verify(rabbitAdmin, never()).deleteQueue(anyString());
    }

    // ---------------------------------------------------------------- exchanges

    @Test
    void crearExchangeDelTipoIndicado() {
        exchangeExiste(false);

        service.crearExchange(new CrearExchangeRequest("demo.x", TipoExchange.FANOUT, null));

        ArgumentCaptor<Exchange> exchange = ArgumentCaptor.forClass(Exchange.class);
        verify(rabbitAdmin).declareExchange(exchange.capture());
        assertThat(exchange.getValue().getType()).isEqualTo("fanout");
        assertThat(exchange.getValue().isDurable()).isTrue();
    }

    @Test
    void crearExchangeExistenteDa409() {
        exchangeExiste(true);

        assertThatThrownBy(() -> service.crearExchange(new CrearExchangeRequest("demo.x", TipoExchange.TOPIC, true)))
                .isInstanceOf(RecursoYaExisteException.class);
    }

    @Test
    void eliminarExchangeInexistenteDa404() {
        exchangeExiste(false);

        assertThatThrownBy(() -> service.eliminarExchange("no.x")).isInstanceOf(RecursoNoEncontradoException.class);
        verify(rabbitAdmin, never()).deleteExchange(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void brokerCaidoSePropagaComoErrorDeConexion() {
        when(rabbitTemplate.execute(any(ChannelCallback.class)))
                .thenThrow(new AmqpConnectException(new java.net.ConnectException("refused")));

        assertThatThrownBy(() -> service.eliminarExchange("demo.x")).isInstanceOf(AmqpConnectException.class);
    }

    // ---------------------------------------------------------------- bindings

    @Test
    void crearBindingEntreColaYExchangeExistentes() {
        colaExiste("demo.q");
        exchangeExiste(true);

        service.crearBinding(new BindingRequest("demo.q", "demo.x", "demo.#"));

        ArgumentCaptor<Binding> binding = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareBinding(binding.capture());
        assertThat(binding.getValue().getDestination()).isEqualTo("demo.q");
        assertThat(binding.getValue().getExchange()).isEqualTo("demo.x");
        assertThat(binding.getValue().getRoutingKey()).isEqualTo("demo.#");
    }

    @Test
    void crearBindingSinColaDa404() {
        assertThatThrownBy(() -> service.crearBinding(new BindingRequest("no.q", "demo.x", "k")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("cola");
        verify(rabbitAdmin, never()).declareBinding(any());
    }

    @Test
    void crearBindingSinExchangeDa404() {
        colaExiste("demo.q");
        exchangeExiste(false);

        assertThatThrownBy(() -> service.crearBinding(new BindingRequest("demo.q", "no.x", "k")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("exchange");
    }

    @Test
    void eliminarBinding() {
        colaExiste("demo.q");
        exchangeExiste(true);

        service.eliminarBinding(new BindingRequest("demo.q", "demo.x", "demo.#"));

        verify(rabbitAdmin).removeBinding(any(Binding.class));
    }
}
