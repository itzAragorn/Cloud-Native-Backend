package com.bancocloud.notificaciones_service.consumer.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.bancocloud.notificaciones_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * El grupo auditoria-inversiones recibe los eventos del tópico de inversiones y un JSON roto
 * termina en el .DLT marcado con su grupo, sin detener el consumo. RabbitMQ no participa
 * (sus listeners no se inician en este test).
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.kafka.reintentos.intervalo-inicial-ms=100",
        "COMPROBANTES_DIR=${java.io.tmpdir}/comprobantes-test"
})
@EmbeddedKafka(partitions = 3, topics = {"bancocloud.inversiones.eventos", "bancocloud.inversiones.eventos.DLT"})
class AuditoriaKafkaIntegrationTest {

    private static final String TOPIC = "bancocloud.inversiones.eventos";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @MockitoSpyBean
    private AuditoriaInversionService service;

    @Test
    void auditaEventoValidoYMandaJsonRotoAlDlt() throws Exception {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "test-auditoria-dlt", false);
        Consumer<String, String> lectorDlt = new DefaultKafkaConsumerFactory<>(consumerProps,
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromEmbeddedTopics(lectorDlt, TOPIC + ".DLT");

        KafkaTemplate<String, String> productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
        productor.send(TOPIC, "7", "no-json").get();
        productor.send(TOPIC, "7", """
                {"eventId":"aud-1","eventType":"INVERSION_REGISTRADA","inversionId":1,"usuarioId":7,"fondoId":2,
                 "monto":1000,"cuotas":1,"valorCuota":1000,"fecha":"2026-11-16T10:30:00"}
                """).get();

        ArgumentCaptor<InversionRegistradaKafkaEvent> captor = ArgumentCaptor.forClass(InversionRegistradaKafkaEvent.class);
        verify(service, timeout(20_000)).auditar(captor.capture(), anyInt(), anyLong());
        assertThat(captor.getValue().eventId()).isEqualTo("aud-1");

        ConsumerRecord<String, String> muerto = KafkaTestUtils.getSingleRecord(lectorDlt, TOPIC + ".DLT", Duration.ofSeconds(15));
        assertThat(muerto.value()).isEqualTo("no-json");
        assertThat(new String(muerto.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP).value(),
                StandardCharsets.UTF_8)).isEqualTo("auditoria-inversiones");
        verify(service, org.mockito.Mockito.never()).auditar(org.mockito.ArgumentMatchers.isNull(), anyInt(), anyLong());
        lectorDlt.close();
    }
}
