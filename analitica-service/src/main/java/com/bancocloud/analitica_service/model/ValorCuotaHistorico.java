package com.bancocloud.analitica_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un punto de la serie histórica del valor cuota de un fondo.
 */
@Entity
@Table(name = "valor_cuota_historico", indexes = @Index(name = "ix_valor_cuota_fondo_fecha", columnList = "fondo_id, fecha"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValorCuotaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "fondo_id", nullable = false)
    private Long fondoId;

    @Column(name = "nombre_fondo", nullable = false)
    private String nombreFondo;

    @Column(name = "valor_anterior", precision = 19, scale = 4)
    private BigDecimal valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorNuevo;

    @Column(name = "variacion_pct", precision = 12, scale = 4)
    private BigDecimal variacionPct;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public ValorCuotaHistorico(String eventId, Long fondoId, String nombreFondo, BigDecimal valorAnterior,
                               BigDecimal valorNuevo, BigDecimal variacionPct, LocalDateTime fecha) {
        this.eventId = eventId;
        this.fondoId = fondoId;
        this.nombreFondo = nombreFondo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.variacionPct = variacionPct;
        this.fecha = fecha;
    }
}
