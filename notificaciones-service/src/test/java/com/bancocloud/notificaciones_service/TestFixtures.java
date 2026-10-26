package com.bancocloud.notificaciones_service;

import com.bancocloud.notificaciones_service.messaging.config.RabbitMQProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/** Objetos compartidos por los tests de consumidores. */
public final class TestFixtures {

    public static final JsonMapper JSON = JsonMapper.builder().build();
    public static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private TestFixtures() {
    }

    public static RabbitMQProperties props() {
        return new RabbitMQProperties("bancocloud.dlx", 3,
                new RabbitMQProperties.Exchanges("bancocloud.inversiones", "bancocloud.usuarios", "bancocloud.fondos"),
                new RabbitMQProperties.Queues("inversiones.comprobante.q", "usuarios.registro.q",
                        "usuarios.estado.q", "fondos.valorcuota.q"),
                new RabbitMQProperties.RoutingKeys("inversion.creada", "usuario.registrado",
                        "usuario.estado.*", "fondo.valorcuota.actualizado"));
    }

    public static Message mensaje(String json, String cola) {
        return mensaje(json, cola, null);
    }

    public static Message mensaje(String json, String cola, Integer retryCount) {
        MessageProperties mp = new MessageProperties();
        mp.setMessageId("evt-1");
        mp.setConsumerQueue(cola);
        mp.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        if (retryCount != null) {
            mp.setHeader("x-retry-count", retryCount);
        }
        return new Message(json.getBytes(StandardCharsets.UTF_8), mp);
    }

    public static String inversionJson() {
        return """
                {"eventId":"evt-1","inversionId":10,"usuarioId":1,"fondoId":2,
                 "monto":100000.00,"cuotas":80.0000,"valorCuota":1250.00,
                 "fecha":"2026-10-26T15:00:00"}
                """;
    }
}
