package com.bancocloud.analitica_service.service;

import com.bancocloud.analitica_service.dto.InversionDiariaDTO;
import com.bancocloud.analitica_service.dto.ResumenFondoDTO;
import com.bancocloud.analitica_service.dto.TotalFondoFila;
import com.bancocloud.analitica_service.dto.ValorCuotaPuntoDTO;
import com.bancocloud.analitica_service.model.ValorCuotaHistorico;
import com.bancocloud.analitica_service.repository.MetricaInversionDiariaRepository;
import com.bancocloud.analitica_service.repository.ValorCuotaHistoricoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura de las métricas que arman los consumidores de Kafka.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaAnaliticaService {

    private final MetricaInversionDiariaRepository metricas;
    private final ValorCuotaHistoricoRepository historico;

    /**
     * Ranking de fondos por monto invertido, con su último valor cuota. Los fondos que solo
     * tienen cambios de valor cuota (sin inversiones) van al final, sin ranking.
     */
    public List<ResumenFondoDTO> resumenFondos() {
        List<ResumenFondoDTO> resumen = new ArrayList<>();
        Set<Long> conInversiones = new LinkedHashSet<>();
        int ranking = 1;
        for (TotalFondoFila fila : metricas.totalesPorFondo()) {
            conInversiones.add(fila.fondoId());
            resumen.add(resumen(ranking++, fila.fondoId(), fila.montoTotal(), fila.cantidadInversiones()));
        }
        historico.fondosConValorCuota().stream()
                .filter(id -> !conInversiones.contains(id))
                .sorted()
                .forEach(id -> resumen.add(resumen(null, id, BigDecimal.ZERO, 0L)));
        return resumen;
    }

    public List<ValorCuotaPuntoDTO> historialValorCuota(Long fondoId) {
        return historico.findByFondoIdOrderByFechaAscIdAsc(fondoId).stream()
                .map(v -> new ValorCuotaPuntoDTO(v.getFecha(), v.getValorAnterior(), v.getValorNuevo(), v.getVariacionPct()))
                .toList();
    }

    public List<InversionDiariaDTO> inversionesDiarias(Long fondoId) {
        return metricas.findByFondoIdOrderByDiaAsc(fondoId).stream()
                .map(m -> new InversionDiariaDTO(m.getDia(), m.getMontoTotal(), m.getCantidad()))
                .toList();
    }

    private ResumenFondoDTO resumen(Integer ranking, Long fondoId, BigDecimal monto, Long cantidad) {
        Optional<ValorCuotaHistorico> ultimo = historico.findFirstByFondoIdOrderByFechaDescIdDesc(fondoId);
        return new ResumenFondoDTO(
                ranking,
                fondoId,
                ultimo.map(ValorCuotaHistorico::getNombreFondo).orElse(null),
                monto,
                cantidad == null ? 0L : cantidad,
                ultimo.map(ValorCuotaHistorico::getValorNuevo).orElse(null),
                ultimo.map(ValorCuotaHistorico::getVariacionPct).orElse(null),
                ultimo.map(ValorCuotaHistorico::getFecha).orElse(null));
    }
}
