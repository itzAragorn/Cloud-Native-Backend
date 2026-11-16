package com.bancocloud.analitica_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro de idempotencia: cada eventId procesado una vez. Si Kafka entrega el mismo mensaje
 * otra vez (reintento, rebalanceo, offset sin confirmar), se descarta.
 */
@Entity
@Table(name = "evento_procesado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoProcesado {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn;

    public EventoProcesado(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.procesadoEn = LocalDateTime.now();
    }
}
