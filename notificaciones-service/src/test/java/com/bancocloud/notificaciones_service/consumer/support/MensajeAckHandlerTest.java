package com.bancocloud.notificaciones_service.consumer.support;

import static com.bancocloud.notificaciones_service.TestFixtures.JSON;
import static com.bancocloud.notificaciones_service.TestFixtures.VALIDATOR;
import static com.bancocloud.notificaciones_service.TestFixtures.inversionJson;
import static com.bancocloud.notificaciones_service.TestFixtures.mensaje;
import static com.bancocloud.notificaciones_service.TestFixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bancocloud.notificaciones_service.consumer.inversiones.InversionCreadaEvent;
import com.bancocloud.notificaciones_service.exception.MensajeNoRecuperableException;
import com.bancocloud.notificaciones_service.exception.MensajeRecuperableException;
import com.rabbitmq.client.Channel;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class MensajeAckHandlerTest {

    private static final String COLA = "inversiones.comprobante.q";
    private static final long TAG = 7L;

    private final Channel channel = mock(Channel.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MensajeAckHandler handler = new MensajeAckHandler(JSON, VALIDATOR, rabbitTemplate, props());

    @Test
    void exitoHaceAck() throws Exception {
        AtomicReference<InversionCreadaEvent> recibido = new AtomicReference<>();

        handler.procesar(mensaje(inversionJson(), COLA), channel, TAG, InversionCreadaEvent.class, recibido::set);

        assertThat(recibido.get().inversionId()).isEqualTo(10L);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void jsonInvalidoHaceNackSinReencolar() throws Exception {
        handler.procesar(mensaje("{no es json", COLA), channel, TAG, InversionCreadaEvent.class, e -> { });

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void camposFaltantesHaceNackSinReencolar() throws Exception {
        handler.procesar(mensaje("{\"eventId\":\"evt-1\",\"inversionId\":10}", COLA), channel, TAG,
                InversionCreadaEvent.class, e -> { });

        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void errorNoRecuperableHaceNackSinReencolar() throws Exception {
        handler.procesar(mensaje(inversionJson(), COLA), channel, TAG, InversionCreadaEvent.class,
                e -> { throw new MensajeNoRecuperableException("dato sin sentido"); });

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void errorInesperadoHaceNackSinReencolar() throws Exception {
        handler.procesar(mensaje(inversionJson(), COLA), channel, TAG, InversionCreadaEvent.class,
                e -> { throw new IllegalStateException("bug"); });

        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void errorRecuperableRepublicaConContadorYHaceAck() throws Exception {
        handler.procesar(mensaje(inversionJson(), COLA, 1), channel, TAG, InversionCreadaEvent.class,
                e -> { throw new MensajeRecuperableException("disco lleno"); });

        ArgumentCaptor<Message> copia = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq(COLA), copia.capture());
        assertThat((Integer) copia.getValue().getMessageProperties().getHeader(MensajeAckHandler.RETRY_HEADER))
                .isEqualTo(2);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void primerErrorRecuperableEmpiezaContadorEnUno() throws Exception {
        handler.procesar(mensaje(inversionJson(), COLA), channel, TAG, InversionCreadaEvent.class,
                e -> { throw new MensajeRecuperableException("timeout"); });

        ArgumentCaptor<Message> copia = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq(COLA), copia.capture());
        assertThat((Integer) copia.getValue().getMessageProperties().getHeader(MensajeAckHandler.RETRY_HEADER))
                .isEqualTo(1);
    }

    @Test
    void reintentosAgotadosHaceNackSinReencolar() throws Exception {
        handler.procesar(mensaje(inversionJson(), COLA, 3), channel, TAG, InversionCreadaEvent.class,
                e -> { throw new MensajeRecuperableException("timeout"); });

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void siNoSePuedeRepublicarVaALaDlq() throws Exception {
        doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        handler.procesar(mensaje(inversionJson(), COLA), channel, TAG, InversionCreadaEvent.class,
                e -> { throw new MensajeRecuperableException("timeout"); });

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
