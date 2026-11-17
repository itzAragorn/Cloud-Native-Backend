package com.bancocloud.analitica_service.consumer.fondos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bancocloud.analitica_service.TestEventos;
import com.bancocloud.analitica_service.consumer.support.EventoValidator;
import com.bancocloud.analitica_service.exception.EventoInvalidoException;
import com.bancocloud.analitica_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import com.bancocloud.analitica_service.model.ValorCuotaHistorico;
import com.bancocloud.analitica_service.repository.EventoProcesadoRepository;
import com.bancocloud.analitica_service.repository.ValorCuotaHistoricoRepository;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnaliticaFondoServiceTest {

    private final EventoProcesadoRepository eventos = mock(EventoProcesadoRepository.class);
    private final ValorCuotaHistoricoRepository historico = mock(ValorCuotaHistoricoRepository.class);
    private final AnaliticaFondoService service = new AnaliticaFondoService(
            new EventoValidator(Validation.buildDefaultValidatorFactory().getValidator()), eventos, historico);

    @Test
    void guardaPuntoDeLaSerieHistorica() {
        assertThat(service.registrar(TestEventos.valorCuota("e1", 5, "1000", "1025"))).isTrue();

        ArgumentCaptor<ValorCuotaHistorico> captor = ArgumentCaptor.forClass(ValorCuotaHistorico.class);
        verify(historico).save(captor.capture());
        assertThat(captor.getValue().getFondoId()).isEqualTo(5L);
        assertThat(captor.getValue().getValorNuevo()).isEqualByComparingTo("1025");
        assertThat(captor.getValue().getEventId()).isEqualTo("e1");
    }

    @Test
    void descartaDuplicado() {
        when(eventos.existsById("e1")).thenReturn(true);

        assertThat(service.registrar(TestEventos.valorCuota("e1", 5, "1000", "1025"))).isFalse();
        verifyNoInteractions(historico);
    }

    @Test
    void rechazaValorNuevoNoPositivo() {
        ValorCuotaKafkaEvent malo = new ValorCuotaKafkaEvent("e2", ValorCuotaKafkaEvent.EVENT_TYPE, 5L, "F",
                BigDecimal.ONE, BigDecimal.ZERO, null, TestEventos.FECHA);

        assertThatThrownBy(() -> service.registrar(malo)).isInstanceOf(EventoInvalidoException.class);
        verify(eventos, org.mockito.Mockito.never()).save(any());
    }
}
