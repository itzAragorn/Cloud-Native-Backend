package com.bancocloud.fondos_service.messaging.kafka.config;

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
                    "bancocloud.fondos.valorcuota", 3, 3, 2, "compact,delete", 2_592_000_000L)),
            new BancoKafkaProperties.Dlt(".DLT", 1_209_600_000L));

    private final KafkaTopicConfig config = new KafkaTopicConfig();

    @Test
    void topicoDeValorCuotaSeCompactaPorFondo() {
        NewTopic topic = config.valorCuotaTopic(props);

        assertThat(topic.name()).isEqualTo("bancocloud.fondos.valorcuota");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 3);
        assertThat(topic.configs())
                .containsEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "compact,delete")
                .containsEntry(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .containsEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2");
    }

    @Test
    void dltNoSeCompactaYTieneRetencionPropia() {
        NewTopic dlt = config.valorCuotaDltTopic(props);

        assertThat(dlt.name()).isEqualTo("bancocloud.fondos.valorcuota.DLT");
        assertThat(dlt.numPartitions()).isEqualTo(3);
        assertThat(dlt.configs())
                .containsEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .containsEntry(TopicConfig.RETENTION_MS_CONFIG, "1209600000");
    }
}
