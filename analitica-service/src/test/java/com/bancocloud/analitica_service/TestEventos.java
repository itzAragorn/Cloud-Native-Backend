package com.bancocloud.analitica_service;

import com.bancocloud.analitica_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import com.bancocloud.analitica_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestEventos {

    public static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 16, 10, 30);

    private TestEventos() {
    }

    public static InversionRegistradaKafkaEvent inversion(String eventId, long fondoId, String monto) {
        return new InversionRegistradaKafkaEvent(eventId, InversionRegistradaKafkaEvent.EVENT_TYPE, 10L, 7L, fondoId,
                new BigDecimal(monto), new BigDecimal("80.0000"), new BigDecimal("1250.00"), FECHA);
    }

    public static ValorCuotaKafkaEvent valorCuota(String eventId, long fondoId, String anterior, String nuevo) {
        return new ValorCuotaKafkaEvent(eventId, ValorCuotaKafkaEvent.EVENT_TYPE, fondoId, "Fondo " + fondoId,
                new BigDecimal(anterior), new BigDecimal(nuevo), new BigDecimal("2.5000"), FECHA);
    }
}
