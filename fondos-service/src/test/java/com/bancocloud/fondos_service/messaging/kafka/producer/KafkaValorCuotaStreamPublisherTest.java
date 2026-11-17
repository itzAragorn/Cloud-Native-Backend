package com.bancocloud.fondos_service.messaging.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import com.bancocloud.fondos_service.messaging.kafka.config.BancoKafkaProperties;
import com.bancocloud.fondos_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaValorCuotaStreamPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final BancoKafkaProperties props = new BancoKafkaProperties(
            new BancoKafkaProperties.Topics(new BancoKafkaProperties.TopicSpec(
                    "bancocloud.fondos.valorcuota", 3, 3, 2, "compact,delete", 2_592_000_000L)),
            new BancoKafkaProperties.Dlt(".DLT", 1_209_600_000L));
    private final KafkaValorCuotaStreamPublisher publisher = new KafkaValorCuotaStreamPublisher(
            kafkaTemplate, props, Validation.buildDefaultValidatorFactory().getValidator(), "fondos-service");

    private final ValorCuotaActualizadoEvent event = new ValorCuotaActualizadoEvent(
            "evt-1", 5L, "Fondo Conservador", new BigDecimal("1000.00"), new BigDecimal("1025.50"),
            LocalDateTime.now());

    @Test
    @SuppressWarnings("unchecked")
    void publicaDtoConKeyFondoIdVariacionYHeaders() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<SendResult<String, Object>>());

        publisher.publicarValorCuota(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, Object> enviado = captor.getValue();
        assertThat(enviado.topic()).isEqualTo("bancocloud.fondos.valorcuota");
        assertThat(enviado.key()).isEqualTo("5");
        assertThat(enviado.value()).isInstanceOfSatisfying(ValorCuotaKafkaEvent.class, dto -> {
            assertThat(dto.eventType()).isEqualTo(ValorCuotaKafkaEvent.EVENT_TYPE);
            assertThat(dto.variacionPct()).isEqualByComparingTo("2.5500");
        });
        assertThat(new String(enviado.headers().lastHeader("source").value(), StandardCharsets.UTF_8))
                .isEqualTo("fondos-service");
    }

    @Test
    void variacionEsNulaSiElValorAnteriorEraCero() {
        ValorCuotaActualizadoEvent desdeCero = new ValorCuotaActualizadoEvent(
                "evt-2", 5L, "Fondo", BigDecimal.ZERO, BigDecimal.TEN, LocalDateTime.now());

        assertThat(ValorCuotaKafkaEvent.from(desdeCero).variacionPct()).isNull();
    }

    @Test
    void noPublicaPayloadInvalido() {
        ValorCuotaActualizadoEvent sinNombre = new ValorCuotaActualizadoEvent(
                "evt-3", 5L, " ", BigDecimal.ONE, new BigDecimal("-1"), LocalDateTime.now());

        publisher.publicarValorCuota(sinNombre);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void noPropagaErrorSiKafkaNoEstaDisponible() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new KafkaException("timeout"));

        assertThatCode(() -> publisher.publicarValorCuota(event)).doesNotThrowAnyException();
    }
}
