package com.bancocloud.analitica_service.consumer.dlt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;

class DltServiceTest {

    @Test
    void leeLosHeadersDelRecoverer() {
        RecordHeaders headers = new RecordHeaders();
        headers.add(KafkaHeaders.DLT_ORIGINAL_TOPIC, "bancocloud.inversiones.eventos".getBytes(StandardCharsets.UTF_8));
        headers.add(KafkaHeaders.DLT_ORIGINAL_PARTITION, ByteBuffer.allocate(4).putInt(2).array());
        headers.add(KafkaHeaders.DLT_ORIGINAL_OFFSET, ByteBuffer.allocate(8).putLong(42L).array());

        assertThat(DltService.texto(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo("bancocloud.inversiones.eventos");
        assertThat(DltService.entero(headers, KafkaHeaders.DLT_ORIGINAL_PARTITION)).isEqualTo(2);
        assertThat(DltService.largo(headers, KafkaHeaders.DLT_ORIGINAL_OFFSET)).isEqualTo(42L);
        assertThat(DltService.texto(headers, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).isNull();
    }

    @Test
    void registrarNoFallaSinHeadersNiPayload() {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("x.DLT", 0, 0L, null, null);

        assertThatCode(() -> new DltService().registrar(rec)).doesNotThrowAnyException();
    }
}
