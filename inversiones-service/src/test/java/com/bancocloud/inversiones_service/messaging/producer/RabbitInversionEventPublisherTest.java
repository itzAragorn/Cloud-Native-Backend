package com.bancocloud.inversiones_service.messaging.producer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bancocloud.inversiones_service.messaging.config.RabbitMQProperties;
import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitInversionEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQProperties props = new RabbitMQProperties(
            "bancocloud.dlx",
            new RabbitMQProperties.Exchanges("bancocloud.inversiones"),
            new RabbitMQProperties.Queues("inversiones.comprobante.q"),
            new RabbitMQProperties.RoutingKeys("inversion.creada"));
    private final RabbitInversionEventPublisher publisher = new RabbitInversionEventPublisher(rabbitTemplate, props);

    private final InversionCreadaEvent event = new InversionCreadaEvent(
            "evt-1", 10L, 1L, 2L, new BigDecimal("100000.00"),
            new BigDecimal("80.0000"), new BigDecimal("1250.00"), LocalDateTime.now());

    @Test
    void publicaEnExchangeYRoutingKeyConfigurados() {
        publisher.publicarInversionCreada(event);

        verify(rabbitTemplate).convertAndSend(eq("bancocloud.inversiones"), eq("inversion.creada"),
                eq(event), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void noPropagaErrorSiRabbitNoEstaDisponible() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatCode(() -> publisher.publicarInversionCreada(event)).doesNotThrowAnyException();
    }
}
