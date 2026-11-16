package com.bancocloud.analitica_service.consumer.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bancocloud.analitica_service.TestEventos;
import com.bancocloud.analitica_service.exception.EventoInvalidoException;
import com.bancocloud.analitica_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import com.bancocloud.analitica_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EventoValidatorTest {

    private final EventoValidator validator = new EventoValidator(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void aceptaEventoValido() {
        assertThatCode(() -> validator.validar(TestEventos.inversion("e1", 2, "100"), InversionRegistradaKafkaEvent.EVENT_TYPE))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaPayloadVacio() {
        assertThatThrownBy(() -> validator.validar(null, InversionRegistradaKafkaEvent.EVENT_TYPE))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void rechazaCamposInvalidosConDetalle() {
        InversionRegistradaKafkaEvent malo = new InversionRegistradaKafkaEvent("e2", InversionRegistradaKafkaEvent.EVENT_TYPE,
                10L, null, 2L, new BigDecimal("-1"), BigDecimal.ONE, BigDecimal.ONE, TestEventos.FECHA);

        assertThatThrownBy(() -> validator.validar(malo, InversionRegistradaKafkaEvent.EVENT_TYPE))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("e2")
                .hasMessageContaining("monto")
                .hasMessageContaining("usuarioId");
    }

    @Test
    void rechazaTipoDeEventoEquivocado() {
        assertThatThrownBy(() -> validator.validar(TestEventos.valorCuota("e3", 1, "1", "2"), InversionRegistradaKafkaEvent.EVENT_TYPE))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining(ValorCuotaKafkaEvent.EVENT_TYPE);
    }
}
