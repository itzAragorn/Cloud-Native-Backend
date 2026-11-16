package com.bancocloud.notificaciones_service.consumer.auditoria;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bancocloud.notificaciones_service.exception.EventoInvalidoException;
import com.bancocloud.notificaciones_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AuditoriaInversionServiceTest {

    private final AuditoriaInversionService service =
            new AuditoriaInversionService(Validation.buildDefaultValidatorFactory().getValidator());

    private static InversionRegistradaKafkaEvent evento(String tipo, String monto) {
        return new InversionRegistradaKafkaEvent("e1", tipo, 10L, 7L, 2L, new BigDecimal(monto),
                new BigDecimal("80"), new BigDecimal("1250"), LocalDateTime.now());
    }

    @Test
    void auditaEventoValido() {
        assertThatCode(() -> service.auditar(evento(InversionRegistradaKafkaEvent.EVENT_TYPE, "100000"), 1, 5L))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaPayloadVacio() {
        assertThatThrownBy(() -> service.auditar(null, 0, 0L)).isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    void rechazaMontoNegativo() {
        assertThatThrownBy(() -> service.auditar(evento(InversionRegistradaKafkaEvent.EVENT_TYPE, "-1"), 0, 0L))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("monto");
    }

    @Test
    void rechazaTipoDeEventoEquivocado() {
        assertThatThrownBy(() -> service.auditar(evento("OTRO", "100"), 0, 0L))
                .isInstanceOf(EventoInvalidoException.class);
    }
}
