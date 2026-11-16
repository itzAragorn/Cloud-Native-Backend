package com.bancocloud.analitica_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bancocloud.analitica_service.dto.ResumenFondoDTO;
import com.bancocloud.analitica_service.dto.TotalFondoFila;
import com.bancocloud.analitica_service.model.ValorCuotaHistorico;
import com.bancocloud.analitica_service.repository.MetricaInversionDiariaRepository;
import com.bancocloud.analitica_service.repository.ValorCuotaHistoricoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultaAnaliticaServiceTest {

    private final MetricaInversionDiariaRepository metricas = mock(MetricaInversionDiariaRepository.class);
    private final ValorCuotaHistoricoRepository historico = mock(ValorCuotaHistoricoRepository.class);
    private final ConsultaAnaliticaService service = new ConsultaAnaliticaService(metricas, historico);

    @Test
    void rankingPorMontoYFondosSinInversionesAlFinal() {
        when(metricas.totalesPorFondo()).thenReturn(List.of(
                new TotalFondoFila(3L, new BigDecimal("9000"), 4L),
                new TotalFondoFila(1L, new BigDecimal("500"), 1L)));
        when(historico.fondosConValorCuota()).thenReturn(List.of(1L, 8L));
        when(historico.findFirstByFondoIdOrderByFechaDescIdDesc(anyLong())).thenReturn(Optional.empty());
        when(historico.findFirstByFondoIdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.of(new ValorCuotaHistorico(
                "e", 1L, "Fondo Uno", new BigDecimal("10"), new BigDecimal("11"), new BigDecimal("10.0000"),
                LocalDateTime.now())));

        List<ResumenFondoDTO> resumen = service.resumenFondos();

        assertThat(resumen).extracting(ResumenFondoDTO::fondoId).containsExactly(3L, 1L, 8L);
        assertThat(resumen).extracting(ResumenFondoDTO::ranking).containsExactly(1, 2, null);
        assertThat(resumen.get(1).nombreFondo()).isEqualTo("Fondo Uno");
        assertThat(resumen.get(1).ultimoValorCuota()).isEqualByComparingTo("11");
        assertThat(resumen.get(2).montoTotalInvertido()).isEqualByComparingTo("0");
    }
}
