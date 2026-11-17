package com.bancocloud.analitica_service.consumer.support;

import com.bancocloud.analitica_service.exception.EventoInvalidoException;
import com.bancocloud.analitica_service.messaging.kafka.event.EventoKafka;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Valida los mensajes recibidos con las anotaciones de su DTO y el tipo de evento esperado.
 * Lanza {@link EventoInvalidoException}, que el error handler no reintenta.
 */
@Component
@RequiredArgsConstructor
public class EventoValidator {

    private final Validator validator;

    public void validar(EventoKafka evento, String eventTypeEsperado) {
        if (evento == null) {
            throw new EventoInvalidoException("payload vacío");
        }
        Set<ConstraintViolation<EventoKafka>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            throw new EventoInvalidoException("evento " + evento.eventId() + " inválido: " + errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        if (!eventTypeEsperado.equals(evento.eventType())) {
            throw new EventoInvalidoException("evento " + evento.eventId() + " con eventType "
                    + evento.eventType() + ", se esperaba " + eventTypeEsperado);
        }
    }
}
