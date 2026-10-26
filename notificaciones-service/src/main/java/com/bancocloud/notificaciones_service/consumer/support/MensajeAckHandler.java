package com.bancocloud.notificaciones_service.consumer.support;

import com.bancocloud.notificaciones_service.exception.MensajeNoRecuperableException;
import com.bancocloud.notificaciones_service.exception.MensajeRecuperableException;
import com.bancocloud.notificaciones_service.messaging.config.RabbitMQProperties;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Política única de confirmación para todos los listeners (acknowledge-mode: manual).
 * <p>
 * Cada mensaje termina siempre en exactamente una de estas salidas:
 * <ol>
 *   <li><b>Éxito</b>: {@code basicAck(tag, false)}.</li>
 *   <li><b>Error no recuperable</b> ({@link MensajeNoRecuperableException}, JSON inválido,
 *       campos faltantes o cualquier error inesperado): {@code basicNack(tag, false, false)}.
 *       La cola tiene {@code x-dead-letter-exchange}, así que RabbitMQ lo mueve a su DLQ.</li>
 *   <li><b>Error recuperable</b> ({@link MensajeRecuperableException}): se republica una copia
 *       en la misma cola con el header {@code x-retry-count} incrementado y se hace ACK del
 *       original. Al superar {@code app.rabbitmq.max-reintentos}, {@code basicNack(tag, false, false)}
 *       y el mensaje va a la DLQ.</li>
 * </ol>
 * Nunca se usa {@code requeue=true}: un NACK con reencolado devuelve el mensaje sin modificar sus
 * headers, por lo que no se podrían contar los intentos y un mensaje defectuoso se repetiría sin fin.
 * <p>
 * El JSON se deserializa aquí y no en el contenedor: en modo manual, una excepción lanzada antes
 * del listener dejaría el mensaje sin confirmar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MensajeAckHandler {

    /** Header con el número de reintentos ya realizados. */
    public static final String RETRY_HEADER = "x-retry-count";

    private final JsonMapper jsonMapper;
    private final Validator validator;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    /**
     * Deserializa y valida el mensaje, ejecuta la acción de negocio y confirma el resultado.
     *
     * @param tipo   clase del evento esperado (con anotaciones de validación)
     * @param accion procesamiento de negocio; señala sus fallas con las excepciones de mensaje
     */
    public <T> void procesar(Message message, Channel channel, long tag, Class<T> tipo, Consumer<T> accion) {
        String messageId = message.getMessageProperties().getMessageId();
        String cola = message.getMessageProperties().getConsumerQueue();
        try {
            T evento = deserializar(message, tipo);
            validar(evento);
            accion.accept(evento);
            channel.basicAck(tag, false);
            log.info("ACK mensaje {} de cola {}", messageId, cola);
        } catch (MensajeRecuperableException e) {
            reintentarOEnviarADlq(message, channel, tag, e);
        } catch (MensajeNoRecuperableException e) {
            rechazar(channel, tag, messageId, cola, e.getMessage());
        } catch (IOException e) {
            log.error("No se pudo confirmar el mensaje {} de cola {}: {}", messageId, cola, e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Error inesperado procesando mensaje {} de cola {}", messageId, cola, e);
            rechazar(channel, tag, messageId, cola, "error inesperado: " + e.getMessage());
        }
    }

    private <T> T deserializar(Message message, Class<T> tipo) {
        try {
            T evento = jsonMapper.readValue(message.getBody(), tipo);
            if (evento == null) {
                throw new MensajeNoRecuperableException("cuerpo vacío");
            }
            return evento;
        } catch (JacksonException e) {
            throw new MensajeNoRecuperableException("JSON inválido para " + tipo.getSimpleName()
                    + ": " + e.getOriginalMessage(), e);
        }
    }

    private <T> void validar(T evento) {
        Set<ConstraintViolation<T>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MensajeNoRecuperableException("datos inválidos: " + detalle);
        }
    }

    private void reintentarOEnviarADlq(Message message, Channel channel, long tag, MensajeRecuperableException e) {
        String messageId = message.getMessageProperties().getMessageId();
        String cola = message.getMessageProperties().getConsumerQueue();
        int intentos = intentosPrevios(message);

        if (intentos >= props.maxReintentos()) {
            rechazar(channel, tag, messageId, cola,
                    "reintentos agotados (" + intentos + "/" + props.maxReintentos() + "): " + e.getMessage());
            return;
        }

        try {
            Message copia = MessageBuilder.fromMessage(message)
                    .setHeader(RETRY_HEADER, intentos + 1)
                    .build();
            // Exchange por defecto ("") con routing key = nombre de la cola: vuelve a la misma cola
            rabbitTemplate.send("", cola, copia);
            channel.basicAck(tag, false);
            log.warn("Mensaje {} de cola {} reintento {}/{}: {}",
                    messageId, cola, intentos + 1, props.maxReintentos(), e.getMessage());
        } catch (RuntimeException | IOException republishError) {
            rechazar(channel, tag, messageId, cola,
                    "no se pudo republicar para reintento: " + republishError.getMessage());
        }
    }

    private int intentosPrevios(Message message) {
        Object valor = message.getMessageProperties().getHeader(RETRY_HEADER);
        if (valor instanceof Number n) {
            return n.intValue();
        }
        if (valor != null) {
            try {
                return Integer.parseInt(valor.toString());
            } catch (NumberFormatException ignored) {
                // Header corrupto: se cuenta como primer intento
            }
        }
        return 0;
    }

    private void rechazar(Channel channel, long tag, String messageId, String cola, String motivo) {
        log.error("NACK mensaje {} de cola {} enviado a DLQ. Motivo: {}", messageId, cola, motivo);
        try {
            channel.basicNack(tag, false, false);
        } catch (IOException e) {
            log.error("No se pudo rechazar el mensaje {} de cola {}: {}", messageId, cola, e.getMessage(), e);
        }
    }
}
