package com.bancocloud.fondos_service.messaging.producer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bancocloud.fondos_service.messaging.config.RabbitMQProperties;
import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitFondoEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQProperties props = new RabbitMQProperties(
            "bancocloud.dlx",
            new RabbitMQProperties.Exchanges("bancocloud.fondos"),
            new RabbitMQProperties.Queues("fondos.valorcuota.q"),
            new RabbitMQProperties.RoutingKeys("fondo.valorcuota.actualizado"));
    private final RabbitFondoEventPublisher publisher = new RabbitFondoEventPublisher(rabbitTemplate, props);

    private final ValorCuotaActualizadoEvent event = new ValorCuotaActualizadoEvent(
            "evt-1", 5L, "Fondo Conservador", new BigDecimal("1000.00"),
            new BigDecimal("1010.00"), LocalDateTime.now());

    @Test
    void publicaEnExchangeYRoutingKeyConfigurados() {
        publisher.publicarValorCuotaActualizado(event);

        verify(rabbitTemplate).convertAndSend(eq("bancocloud.fondos"), eq("fondo.valorcuota.actualizado"),
                eq(event), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void noPropagaErrorSiRabbitNoEstaDisponible() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatCode(() -> publisher.publicarValorCuotaActualizado(event)).doesNotThrowAnyException();
    }
}
