package com.bancocloud.inversiones_service.messaging.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;

/**
 * Verifica que los tópicos se declaran con las particiones, réplicas y configuraciones de
 * {@link BancoKafkaProperties}.
 */
class KafkaTopicConfigTest {

    private final BancoKafkaProperties props = new BancoKafkaProperties(
            new BancoKafkaProperties.Topics(new BancoKafkaProperties.TopicSpec(
                    "bancocloud.inversiones.eventos", 3, 3, 2, "delete", 604_800_000L)),
            new BancoKafkaProperties.Dlt(".DLT", 1_209_600_000L));

    private final KafkaTopicConfig config = new KafkaTopicConfig();

    @Test
    void topicoDeInversionesUsaParametrosDelFlujo() {
        NewTopic topic = config.inversionesTopic(props);

        assertThat(topic.name()).isEqualTo("bancocloud.inversiones.eventos");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 3);
        assertThat(topic.configs())
                .containsEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .containsEntry(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .containsEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2");
    }

    @Test
    void dltTieneLasMismasParticionesYRetencionPropia() {
        NewTopic dlt = config.inversionesDltTopic(props);

        assertThat(dlt.name()).isEqualTo("bancocloud.inversiones.eventos.DLT");
        assertThat(dlt.numPartitions()).isEqualTo(3);
        assertThat(dlt.replicationFactor()).isEqualTo((short) 3);
        assertThat(dlt.configs())
                .containsEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .containsEntry(TopicConfig.RETENTION_MS_CONFIG, "1209600000");
    }
}
