package com.bancocloud.analitica_service.repository;

import com.bancocloud.analitica_service.dto.TotalFondoFila;
import com.bancocloud.analitica_service.model.MetricaInversionDiaria;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MetricaInversionDiariaRepository extends JpaRepository<MetricaInversionDiaria, Long> {

    /**
     * Bloquea la fila del día: las 3 hebras del grupo pueden recibir inversiones del mismo fondo
     * (la key es el usuario) y deben sumar sin pisarse.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MetricaInversionDiaria m where m.fondoId = :fondoId and m.dia = :dia")
    Optional<MetricaInversionDiaria> buscarParaActualizar(Long fondoId, LocalDate dia);

    /** Totales por fondo, del que más recibió al que menos (ranking). */
    @Query("""
            select new com.bancocloud.analitica_service.dto.TotalFondoFila(m.fondoId, sum(m.montoTotal), sum(m.cantidad))
            from MetricaInversionDiaria m
            group by m.fondoId
            order by sum(m.montoTotal) desc, m.fondoId
            """)
    List<TotalFondoFila> totalesPorFondo();

    List<MetricaInversionDiaria> findByFondoIdOrderByDiaAsc(Long fondoId);
}
