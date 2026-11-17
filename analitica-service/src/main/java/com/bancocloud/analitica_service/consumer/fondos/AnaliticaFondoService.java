package com.bancocloud.analitica_service.consumer.fondos;

import com.bancocloud.analitica_service.consumer.support.EventoValidator;
import com.bancocloud.analitica_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import com.bancocloud.analitica_service.model.EventoProcesado;
import com.bancocloud.analitica_service.model.ValorCuotaHistorico;
import com.bancocloud.analitica_service.repository.EventoProcesadoRepository;
import com.bancocloud.analitica_service.repository.ValorCuotaHistoricoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de analítica de fondos: guarda la serie histórica del valor cuota y su variación.
 * No conoce Kafka; recibe el DTO ya deserializado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnaliticaFondoService {

    private final EventoValidator validator;
    private final EventoProcesadoRepository eventosProcesados;
    private final ValorCuotaHistoricoRepository historico;

    /**
     * @return {@code false} si el evento ya se había procesado (duplicado)
     * @throws com.bancocloud.analitica_service.exception.EventoInvalidoException si el payload no es válido
     */
    @Transactional
    public boolean registrar(ValorCuotaKafkaEvent evento) {
        validator.validar(evento, ValorCuotaKafkaEvent.EVENT_TYPE);
        if (eventosProcesados.existsById(evento.eventId())) {
            log.info("Evento {} ya procesado, se descarta (fondoId={})", evento.eventId(), evento.fondoId());
            return false;
        }
        eventosProcesados.save(new EventoProcesado(evento.eventId(), evento.eventType()));
        historico.save(new ValorCuotaHistorico(evento.eventId(), evento.fondoId(), evento.nombreFondo(),
                evento.valorAnterior(), evento.valorNuevo(), evento.variacionPct(), evento.fecha()));
        return true;
    }
}
