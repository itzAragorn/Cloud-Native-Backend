package com.bancocloud.notificaciones_service.consumer.inversiones;

import static com.bancocloud.notificaciones_service.TestFixtures.JSON;
import static com.bancocloud.notificaciones_service.TestFixtures.VALIDATOR;
import static com.bancocloud.notificaciones_service.TestFixtures.inversionJson;
import static com.bancocloud.notificaciones_service.TestFixtures.mensaje;
import static com.bancocloud.notificaciones_service.TestFixtures.props;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancocloud.notificaciones_service.consumer.support.MensajeAckHandler;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** Listener + política real de ACK, con Channel simulado. */
class ComprobanteListenerTest {

    private static final String COLA = "inversiones.comprobante.q";

    private final Channel channel = mock(Channel.class);
    private final ComprobanteService comprobanteService = mock(ComprobanteService.class);
    private final ComprobanteListener listener = new ComprobanteListener(
            new MensajeAckHandler(JSON, VALIDATOR, mock(RabbitTemplate.class), props()), comprobanteService);

    @Test
    void comprobanteGeneradoHaceAck() throws Exception {
        listener.onInversionCreada(mensaje(inversionJson(), COLA), channel, 1L);

        verify(comprobanteService).generar(any(InversionCreadaEvent.class));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void montoNegativoHaceNackYNoGeneraComprobante() throws Exception {
        String json = inversionJson().replace("100000.00", "-5");

        listener.onInversionCreada(mensaje(json, COLA), channel, 2L);

        verify(comprobanteService, never()).generar(any());
        verify(channel).basicNack(2L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void fallaDeServicioDesconocidaHaceNack() throws Exception {
        when(comprobanteService.generar(any())).thenThrow(new IllegalStateException("bug"));

        listener.onInversionCreada(mensaje(inversionJson(), COLA), channel, 3L);

        verify(channel).basicNack(3L, false, false);
    }
}
