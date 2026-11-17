package com.bancocloud.inversiones_service.messaging.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;
import com.bancocloud.inversiones_service.messaging.kafka.config.BancoKafkaProperties;
import com.bancocloud.inversiones_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
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

class KafkaInversionStreamPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final BancoKafkaProperties props = new BancoKafkaProperties(
            new BancoKafkaProperties.Topics(new BancoKafkaProperties.TopicSpec(
                    "bancocloud.inversiones.eventos", 3, 3, 2, "delete", 604_800_000L)),
            new BancoKafkaProperties.Dlt(".DLT", 1_209_600_000L));
    private final KafkaInversionStreamPublisher publisher = new KafkaInversionStreamPublisher(
            kafkaTemplate, props, Validation.buildDefaultValidatorFactory().getValidator(), "inversiones-service");

    private final InversionCreadaEvent event = new InversionCreadaEvent(
            "evt-1", 10L, 7L, 2L, new BigDecimal("100000.00"),
            new BigDecimal("80.0000"), new BigDecimal("1250.00"), LocalDateTime.now());

    @Test
    @SuppressWarnings("unchecked")
    void publicaDtoConKeyUsuarioIdYHeaders() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<SendResult<String, Object>>());

        publisher.publicarInversionRegistrada(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, Object> enviado = captor.getValue();
        assertThat(enviado.topic()).isEqualTo("bancocloud.inversiones.eventos");
        assertThat(enviado.key()).isEqualTo("7");
        assertThat(enviado.value()).isInstanceOfSatisfying(InversionRegistradaKafkaEvent.class, dto -> {
            assertThat(dto.eventId()).isEqualTo("evt-1");
            assertThat(dto.eventType()).isEqualTo(InversionRegistradaKafkaEvent.EVENT_TYPE);
            assertThat(dto.inversionId()).isEqualTo(10L);
            assertThat(dto.monto()).isEqualByComparingTo("100000.00");
        });
        assertThat(new String(enviado.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8))
                .isEqualTo("INVERSION_REGISTRADA");
        assertThat(new String(enviado.headers().lastHeader("source").value(), StandardCharsets.UTF_8))
                .isEqualTo("inversiones-service");
    }

    @Test
    void noPublicaPayloadInvalido() {
        InversionCreadaEvent sinUsuario = new InversionCreadaEvent(
                "evt-2", 11L, null, 2L, new BigDecimal("-5"), BigDecimal.ONE, BigDecimal.ONE, LocalDateTime.now());

        publisher.publicarInversionRegistrada(sinUsuario);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void noPropagaErrorSiKafkaNoEstaDisponible() {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new KafkaException("timeout"));

        assertThatCode(() -> publisher.publicarInversionRegistrada(event)).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void noPropagaErrorAsincronoDelBroker() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("NOT_ENOUGH_REPLICAS")));

        assertThatCode(() -> publisher.publicarInversionRegistrada(event)).doesNotThrowAnyException();
    }
}
