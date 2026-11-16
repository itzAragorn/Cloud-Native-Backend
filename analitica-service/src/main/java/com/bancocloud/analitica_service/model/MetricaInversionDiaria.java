package com.bancocloud.analitica_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Monto total y cantidad de inversiones de un fondo en un día.
 */
@Entity
@Table(name = "metrica_inversion_diaria",
        uniqueConstraints = @UniqueConstraint(name = "uk_metrica_fondo_dia", columnNames = {"fondo_id", "dia"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetricaInversionDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fondo_id", nullable = false)
    private Long fondoId;

    @Column(name = "dia", nullable = false)
    private LocalDate dia;

    @Column(name = "monto_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(name = "cantidad", nullable = false)
    private long cantidad;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public MetricaInversionDiaria(Long fondoId, LocalDate dia) {
        this.fondoId = fondoId;
        this.dia = dia;
        this.actualizadoEn = LocalDateTime.now();
    }

    public void sumar(BigDecimal monto) {
        this.montoTotal = this.montoTotal.add(monto);
        this.cantidad++;
        this.actualizadoEn = LocalDateTime.now();
    }
}
