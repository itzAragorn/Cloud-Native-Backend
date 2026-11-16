package com.bancocloud.kafka_admin_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La API completa contra un broker Kafka real (embebido): crear, describir, modificar,
 * aumentar particiones y eliminar un tópico; y ver el lag de un consumer group y moverle los offsets.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1)
class KafkaAdminIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void cicloDeVidaDeUnTopico() throws Exception {
        mvc.perform(post("/topics").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"it.demo","partitions":2,"replicationFactor":1,
                         "configs":{"retention.ms":"86400000","cleanup.policy":"compact,delete"}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partitions").value(2))
                .andExpect(jsonPath("$.configs['retention.ms']").value("86400000"))
                .andExpect(jsonPath("$.configs['cleanup.policy']").value("compact,delete"));

        mvc.perform(post("/topics").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"it.demo","partitions":1,"replicationFactor":1}"""))
                .andExpect(status().isConflict());

        mvc.perform(post("/topics").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"it.otro","partitions":1,"replicationFactor":3}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("replicationFactor"));

        mvc.perform(put("/topics/it.demo/configs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"configs":{"retention.ms":"3600000","cleanup.policy":null}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs['retention.ms']").value("3600000"))
                .andExpect(jsonPath("$.configs['cleanup.policy']").value("delete"));

        mvc.perform(post("/topics/it.demo/partitions").contentType(MediaType.APPLICATION_JSON).content("{\"total\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partitions").value(4));

        mvc.perform(post("/topics/it.demo/partitions").contentType(MediaType.APPLICATION_JSON).content("{\"total\":2}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='it.demo')].partitions").value(4))
                .andExpect(jsonPath("$[?(@.name=='__consumer_offsets')]").doesNotExist());

        mvc.perform(delete("/topics/it.demo")).andExpect(status().isNoContent());
        mvc.perform(get("/topics/no.existe")).andExpect(status().isNotFound());
    }

    @Test
    void lagYResetDeOffsetsDeUnGrupo() throws Exception {
        mvc.perform(post("/topics").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"it.grupos","partitions":1,"replicationFactor":1}"""))
                .andExpect(status().isCreated());

        KafkaTemplate<String, String> productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
        for (int i = 0; i < 5; i++) {
            productor.send("it.grupos", "k", "m" + i).get();
        }

        // Un consumidor lee 2 mensajes, confirma y se va: el grupo queda EMPTY con lag 3
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "it-grupo", false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2);
        try (Consumer<String, String> consumidor = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumidor.subscribe(java.util.List.of("it.grupos"));
            int leidos = 0;
            long limite = System.currentTimeMillis() + 15_000;
            while (leidos == 0 && System.currentTimeMillis() < limite) {
                leidos = consumidor.poll(Duration.ofMillis(500)).count();
            }
            assertThat(leidos).isEqualTo(2);
            consumidor.commitSync();
        }

        mvc.perform(get("/consumer-groups/it-grupo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMPTY"))
                .andExpect(jsonPath("$.lagTotal").value(3));

        mvc.perform(post("/consumer-groups/it-grupo/reset-offsets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"it.grupos\",\"estrategia\":\"EARLIEST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lagTotal").value(5));

        mvc.perform(get("/consumer-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.groupId=='it-grupo')]").exists());

        mvc.perform(delete("/consumer-groups/it-grupo")).andExpect(status().isNoContent());
        mvc.perform(get("/consumer-groups/it-grupo")).andExpect(status().isNotFound());
    }
}
