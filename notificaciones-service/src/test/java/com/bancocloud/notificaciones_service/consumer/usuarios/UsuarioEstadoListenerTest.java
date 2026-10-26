package com.bancocloud.notificaciones_service.consumer.usuarios;

import static com.bancocloud.notificaciones_service.TestFixtures.JSON;
import static com.bancocloud.notificaciones_service.TestFixtures.VALIDATOR;
import static com.bancocloud.notificaciones_service.TestFixtures.mensaje;
import static com.bancocloud.notificaciones_service.TestFixtures.props;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bancocloud.notificaciones_service.consumer.support.MensajeAckHandler;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** Listener + servicio + política real de ACK, con Channel simulado. */
class UsuarioEstadoListenerTest {

    private static final String COLA = "usuarios.estado.q";

    private final Channel channel = mock(Channel.class);
    private final UsuarioEstadoListener listener = new UsuarioEstadoListener(
            new MensajeAckHandler(JSON, VALIDATOR, mock(RabbitTemplate.class), props()),
            new NotificacionUsuarioService());

    private String json(String cambio) {
        return """
                {"eventId":"evt-1","usuarioId":1,"nombre":"Ana","email":"ana@example.com",
                 "cambio":"%s","estado":"BLOQUEADO","accesoAutorizado":false,
                 "fecha":"2026-10-26T15:00:00"}
                """.formatted(cambio);
    }

    @Test
    void cambioConocidoHaceAck() throws Exception {
        listener.onEstadoCambiado(mensaje(json("BLOQUEADO"), COLA), channel, 1L);

        verify(channel).basicAck(1L, false);
    }

    @Test
    void cambioDesconocidoHaceNackSinReencolar() throws Exception {
        listener.onEstadoCambiado(mensaje(json("ELIMINADO"), COLA), channel, 2L);

        verify(channel).basicNack(2L, false, false);
    }

    @Test
    void emailInvalidoHaceNackSinReencolar() throws Exception {
        listener.onEstadoCambiado(mensaje(json("BLOQUEADO").replace("ana@example.com", "no-es-email"), COLA),
                channel, 3L);

        verify(channel).basicNack(3L, false, false);
    }
}
