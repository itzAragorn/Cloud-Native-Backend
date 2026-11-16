package com.bancocloud.notificaciones_service.consumer.auditoria;

import com.bancocloud.notificaciones_service.exception.EventoInvalidoException;
import com.bancocloud.notificaciones_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Registro de auditoría de las inversiones que pasan por el flujo de eventos. Escribe una línea
 * por inversión en el logger {@code AUDITORIA}, separado del log técnico.
 * <p>
 * Es una responsabilidad distinta de la analítica: por eso lee el tópico con su propio consumer
 * group y recibe todos los eventos aunque analitica-service también los procese.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaInversionService {

    static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA");

    private final Validator validator;

    /**
     * @throws EventoInvalidoException si el payload no es válido (no se reintenta, va al .DLT)
     */
    public void auditar(InversionRegistradaKafkaEvent evento, int partition, long offset) {
        validar(evento);
        AUDITORIA.info("inversion={} usuario={} fondo={} monto={} cuotas={} valorCuota={} fecha={} eventId={} "
                        + "partition={} offset={}",
                evento.inversionId(), evento.usuarioId(), evento.fondoId(), evento.monto(), evento.cuotas(),
                evento.valorCuota(), evento.fecha(), evento.eventId(), partition, offset);
    }

    private void validar(InversionRegistradaKafkaEvent evento) {
        if (evento == null) {
            throw new EventoInvalidoException("payload vacío");
        }
        Set<ConstraintViolation<InversionRegistradaKafkaEvent>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            throw new EventoInvalidoException("evento " + evento.eventId() + " inválido: " + errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        if (!InversionRegistradaKafkaEvent.EVENT_TYPE.equals(evento.eventType())) {
            throw new EventoInvalidoException("evento " + evento.eventId() + " con eventType " + evento.eventType());
        }
    }
}
