package com.bancocloud.analitica_service.consumer.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bancocloud.analitica_service.TestEventos;
import com.bancocloud.analitica_service.consumer.support.EventoValidator;
import com.bancocloud.analitica_service.exception.EventoInvalidoException;
import com.bancocloud.analitica_service.model.MetricaInversionDiaria;
import com.bancocloud.analitica_service.repository.EventoProcesadoRepository;
import com.bancocloud.analitica_service.repository.MetricaInversionDiariaRepository;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnaliticaInversionServiceTest {

    private final EventoProcesadoRepository eventos = mock(EventoProcesadoRepository.class);
    private final MetricaInversionDiariaRepository metricas = mock(MetricaInversionDiariaRepository.class);
    private final AnaliticaInversionService service = new AnaliticaInversionService(
            new EventoValidator(Validation.buildDefaultValidatorFactory().getValidator()), eventos, metricas);

    @Test
    void sumaAMetricaExistenteDelDia() {
        MetricaInversionDiaria existente = new MetricaInversionDiaria(2L, LocalDate.of(2026, 11, 16));
        existente.sumar(new java.math.BigDecimal("500"));
        when(metricas.buscarParaActualizar(2L, LocalDate.of(2026, 11, 16))).thenReturn(Optional.of(existente));

        boolean nuevo = service.registrar(TestEventos.inversion("e1", 2, "1000"));

        assertThat(nuevo).isTrue();
        assertThat(existente.getMontoTotal()).isEqualByComparingTo("1500");
        assertThat(existente.getCantidad()).isEqualTo(2);
        verify(eventos).save(any());
    }

    @Test
    void creaMetricaSiEsLaPrimeraDelDia() {
        when(metricas.buscarParaActualizar(any(), any())).thenReturn(Optional.empty());

        service.registrar(TestEventos.inversion("e1", 3, "250.50"));

        ArgumentCaptor<MetricaInversionDiaria> captor = ArgumentCaptor.forClass(MetricaInversionDiaria.class);
        verify(metricas).save(captor.capture());
        assertThat(captor.getValue().getFondoId()).isEqualTo(3L);
        assertThat(captor.getValue().getMontoTotal()).isEqualByComparingTo("250.50");
        assertThat(captor.getValue().getCantidad()).isEqualTo(1);
    }

    @Test
    void descartaEventoDuplicado() {
        when(eventos.existsById("e1")).thenReturn(true);

        boolean nuevo = service.registrar(TestEventos.inversion("e1", 2, "1000"));

        assertThat(nuevo).isFalse();
        verify(eventos, never()).save(any());
        verifyNoInteractions(metricas);
    }

    @Test
    void eventoInvalidoNoTocaLaBase() {
        assertThatThrownBy(() -> service.registrar(TestEventos.inversion("e1", 2, "-10")))
                .isInstanceOf(EventoInvalidoException.class);

        verifyNoInteractions(eventos, metricas);
    }
}
