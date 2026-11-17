package com.bancocloud.analitica_service.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.bancocloud.analitica_service.repository.EventoProcesadoRepository;
import com.bancocloud.analitica_service.repository.MetricaInversionDiariaRepository;
import com.bancocloud.analitica_service.repository.ValorCuotaHistoricoRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Flujo real productor → Kafka → consumidor → MySQL (H2), con un broker embebido:
 * mensajes válidos actualizan las métricas, un duplicado se descarta, y JSON roto o un payload
 * inválido terminan en el .DLT sin detener el consumo.
 */
@SpringBootTest(properties = {
        "ANALITICA_DB_URL=jdbc:h2:mem:analitica;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.reintentos.intervalo-inicial-ms=100"
})
@EmbeddedKafka(partitions = 3, topics = {
        AnaliticaKafkaIntegrationTest.INVERSIONES, AnaliticaKafkaIntegrationTest.INVERSIONES + ".DLT",
        AnaliticaKafkaIntegrationTest.VALOR_CUOTA, AnaliticaKafkaIntegrationTest.VALOR_CUOTA + ".DLT"})
class AnaliticaKafkaIntegrationTest {

    static final String INVERSIONES = "bancocloud.inversiones.eventos";
    static final String VALOR_CUOTA = "bancocloud.fondos.valorcuota";

    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private MetricaInversionDiariaRepository metricas;
    @Autowired
    private ValorCuotaHistoricoRepository historico;
    @Autowired
    private EventoProcesadoRepository eventos;

    private KafkaTemplate<String, String> productor;
    private Consumer<String, String> lectorDlt;

    @BeforeEach
    void setUp() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "test-dlt-" + UUID.randomUUID(), false);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        lectorDlt = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromEmbeddedTopics(lectorDlt, INVERSIONES + ".DLT");
        lectorDlt.poll(Duration.ofMillis(500));
    }

    @AfterEach
    void tearDown() {
        lectorDlt.close();
    }

    @Test
    void inversionValidaActualizaMetricasYDuplicadoSeDescarta() throws Exception {
        String eventId = UUID.randomUUID().toString();
        String json = inversionJson(eventId, 91, "150000.00");

        productor.send(INVERSIONES, "7", json).get();
        productor.send(INVERSIONES, "7", json).get();

        esperar(() -> eventos.existsById(eventId) && metricas.findByFondoIdOrderByDiaAsc(91L).size() == 1);
        Thread.sleep(1000); // margen para que llegue el duplicado
        var metrica = metricas.findByFondoIdOrderByDiaAsc(91L).get(0);
        assertThat(metrica.getMontoTotal()).isEqualByComparingTo("150000.00");
        assertThat(metrica.getCantidad()).isEqualTo(1);
    }

    @Test
    void valorCuotaSeGuardaEnLaSerie() throws Exception {
        String json = """
                {"eventId":"%s","eventType":"VALOR_CUOTA_ACTUALIZADO","fondoId":92,"nombreFondo":"Fondo 92",
                 "valorAnterior":1000.00,"valorNuevo":1025.50,"variacionPct":2.55,"fecha":"2026-11-16T10:30:00"}
                """.formatted(UUID.randomUUID());

        productor.send(VALOR_CUOTA, "92", json).get();

        esperar(() -> historico.findByFondoIdOrderByFechaAscIdAsc(92L).size() == 1);
        assertThat(historico.findByFondoIdOrderByFechaAscIdAsc(92L).get(0).getValorNuevo()).isEqualByComparingTo("1025.50");
    }

    @Test
    void jsonRotoVaAlDltYElConsumoSigue() throws Exception {
        productor.send(INVERSIONES, "8", "{esto no es json").get();
        String eventoValido = UUID.randomUUID().toString();
        productor.send(INVERSIONES, "8", inversionJson(eventoValido, 93, "1000")).get();

        ConsumerRecord<String, String> muerto = KafkaTestUtils.getSingleRecord(lectorDlt, INVERSIONES + ".DLT", Duration.ofSeconds(15));
        assertThat(muerto.value()).isEqualTo("{esto no es json");
        assertThat(header(muerto, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(INVERSIONES);
        assertThat(header(muerto, KafkaHeaders.DLT_EXCEPTION_FQCN)).contains("DeserializationException");

        esperar(() -> eventos.existsById(eventoValido));
    }

    @Test
    void payloadInvalidoVaAlDltSinReintentos() throws Exception {
        String eventId = UUID.randomUUID().toString();
        productor.send(INVERSIONES, "9", inversionJson(eventId, 94, "-50")).get();

        ConsumerRecord<String, String> muerto = KafkaTestUtils.getSingleRecord(lectorDlt, INVERSIONES + ".DLT", Duration.ofSeconds(15));
        assertThat(muerto.value()).contains(eventId);
        assertThat(header(muerto, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).contains("monto");
        assertThat(eventos.existsById(eventId)).isFalse();
    }

    private static String inversionJson(String eventId, long fondoId, String monto) {
        return """
                {"eventId":"%s","eventType":"INVERSION_REGISTRADA","inversionId":1,"usuarioId":7,"fondoId":%d,
                 "monto":%s,"cuotas":80.0000,"valorCuota":1250.00,"fecha":"2026-11-16T10:30:00"}
                """.formatted(eventId, fondoId, monto);
    }

    private static String header(ConsumerRecord<?, ?> rec, String nombre) {
        var h = rec.headers().lastHeader(nombre);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private static void esperar(BooleanSupplier condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + 20_000;
        while (!condicion.getAsBoolean()) {
            if (System.currentTimeMillis() > limite) {
                throw new AssertionError("La condición no se cumplió en 20 s");
            }
            Thread.sleep(200);
        }
    }
}
