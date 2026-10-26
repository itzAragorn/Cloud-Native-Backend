package com.bancocloud.notificaciones_service.consumer.dlq;

import static com.bancocloud.notificaciones_service.TestFixtures.mensaje;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

class DlqListenerTest {

    private final Channel channel = mock(Channel.class);

    @Test
    void registraXDeathYHaceAck() throws Exception {
        Message message = mensaje("{\"eventId\":\"evt-1\"}", "inversiones.comprobante.q.dlq");
        message.getMessageProperties().setHeader("x-death", List.of(Map.of(
                "queue", "inversiones.comprobante.q", "reason", "rejected", "count", 1L)));

        new DlqListener(new DlqService()).onMensajeMuerto(message, channel, 5L);

        verify(channel).basicAck(5L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void haceAckAunqueFalleElRegistro() throws Exception {
        DlqService dlqService = mock(DlqService.class);
        Message message = mensaje("{}", "usuarios.estado.q.dlq");
        doThrow(new IllegalStateException("log roto")).when(dlqService).registrar(message);

        new DlqListener(dlqService).onMensajeMuerto(message, channel, 6L);

        verify(channel).basicAck(6L, false);
    }
}
