package com.bancocloud.usuarios_service.messaging.producer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bancocloud.usuarios_service.messaging.config.RabbitMQProperties;
import com.bancocloud.usuarios_service.messaging.event.CambioEstadoUsuario;
import com.bancocloud.usuarios_service.messaging.event.UsuarioEstadoCambiadoEvent;
import com.bancocloud.usuarios_service.messaging.event.UsuarioRegistradoEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitUsuarioEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQProperties props = new RabbitMQProperties(
            "bancocloud.dlx",
            new RabbitMQProperties.Exchanges("bancocloud.usuarios"),
            new RabbitMQProperties.Queues("usuarios.registro.q", "usuarios.estado.q"),
            new RabbitMQProperties.RoutingKeys(
                    "usuario.registrado", "usuario.estado.*", "usuario.estado.autorizado",
                    "usuario.estado.desautorizado", "usuario.estado.bloqueado", "usuario.estado.desbloqueado"));
    private final RabbitUsuarioEventPublisher publisher = new RabbitUsuarioEventPublisher(rabbitTemplate, props);

    private final UsuarioRegistradoEvent registrado = new UsuarioRegistradoEvent(
            "evt-1", 1L, "Ana", "Rojas", "ana@example.com", "CLIENTE", true, LocalDateTime.now());

    @Test
    void publicaRegistroConSuRoutingKey() {
        publisher.publicarUsuarioRegistrado(registrado);

        verify(rabbitTemplate).convertAndSend(eq("bancocloud.usuarios"), eq("usuario.registrado"),
                eq(registrado), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @ParameterizedTest
    @CsvSource({
            "AUTORIZADO, usuario.estado.autorizado",
            "DESAUTORIZADO, usuario.estado.desautorizado",
            "BLOQUEADO, usuario.estado.bloqueado",
            "DESBLOQUEADO, usuario.estado.desbloqueado"
    })
    void cadaCambioDeEstadoUsaSuRoutingKey(CambioEstadoUsuario cambio, String routingKey) {
        UsuarioEstadoCambiadoEvent event = new UsuarioEstadoCambiadoEvent(
                "evt-2", 1L, "Ana", "ana@example.com", cambio, "ACTIVO", true, LocalDateTime.now());

        publisher.publicarEstadoCambiado(event);

        verify(rabbitTemplate).convertAndSend(eq("bancocloud.usuarios"), eq(routingKey),
                eq(event), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void noPropagaErrorSiRabbitNoEstaDisponible() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatCode(() -> publisher.publicarUsuarioRegistrado(registrado)).doesNotThrowAnyException();
    }
}
