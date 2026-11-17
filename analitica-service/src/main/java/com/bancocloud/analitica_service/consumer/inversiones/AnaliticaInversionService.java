package com.bancocloud.analitica_service.consumer.inversiones;

import com.bancocloud.analitica_service.consumer.support.EventoValidator;
import com.bancocloud.analitica_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import com.bancocloud.analitica_service.model.EventoProcesado;
import com.bancocloud.analitica_service.model.MetricaInversionDiaria;
import com.bancocloud.analitica_service.repository.EventoProcesadoRepository;
import com.bancocloud.analitica_service.repository.MetricaInversionDiariaRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de analítica de inversiones: acumula monto y cantidad por fondo y por día.
 * No conoce Kafka; recibe el DTO ya deserializado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnaliticaInversionService {

    private final EventoValidator validator;
    private final EventoProcesadoRepository eventosProcesados;
    private final MetricaInversionDiariaRepository metricas;

    /**
     * Registra una inversión en las métricas. La marca de idempotencia y la suma se guardan en la
     * misma transacción: o quedan las dos o ninguna.
     *
     * @return {@code false} si el evento ya se había procesado (duplicado)
     * @throws com.bancocloud.analitica_service.exception.EventoInvalidoException si el payload no es válido
     */
    @Transactional
    public boolean registrar(InversionRegistradaKafkaEvent evento) {
        validator.validar(evento, InversionRegistradaKafkaEvent.EVENT_TYPE);
        if (eventosProcesados.existsById(evento.eventId())) {
            log.info("Evento {} ya procesado, se descarta (inversionId={})", evento.eventId(), evento.inversionId());
            return false;
        }
        eventosProcesados.save(new EventoProcesado(evento.eventId(), evento.eventType()));

        LocalDate dia = evento.fecha().toLocalDate();
        MetricaInversionDiaria metrica = metricas.buscarParaActualizar(evento.fondoId(), dia)
                .orElseGet(() -> new MetricaInversionDiaria(evento.fondoId(), dia));
        metrica.sumar(evento.monto());
        metricas.save(metrica);
        log.debug("Fondo {} día {}: total={} cantidad={}", evento.fondoId(), dia,
                metrica.getMontoTotal(), metrica.getCantidad());
        return true;
    }
}
