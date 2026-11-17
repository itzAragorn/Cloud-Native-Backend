package com.bancocloud.fondos_service.messaging.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * Verifica que el productor se arma con la configuración de application.yml
 * (sin conectarse a un broker).
 */
class KafkaProducerConfigTest {

    private Map<String, Object> configuracionProductor() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(env.getPropertySources()::addLast);
        KafkaProperties kafkaProperties = new Binder(ConfigurationPropertySources.get(env))
                .bind("spring.kafka", KafkaProperties.class)
                .get();
        return new KafkaProducerConfig()
                .kafkaProducerFactory(kafkaProperties)
                .getConfigurationProperties();
    }

    @Test
    void productorUsaSerializersAcksYIdempotenciaDeApplicationYml() throws Exception {
        Map<String, Object> config = configuracionProductor();

        assertThat(config.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
                .asString().contains("localhost:9092", "localhost:9093", "localhost:9094");
        assertThat(config.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(StringSerializer.class);
        assertThat(config.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(JacksonJsonSerializer.class);
        assertThat(config.get(ProducerConfig.ACKS_CONFIG)).isEqualTo("all");
        assertThat(config.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)).isEqualTo("true");
        assertThat(config.get(ProducerConfig.COMPRESSION_TYPE_CONFIG)).isEqualTo("lz4");
        assertThat(config.get(ProducerConfig.MAX_BLOCK_MS_CONFIG)).isEqualTo("5000");
        assertThat(config.get(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS)).isEqualTo("false");
    }
}
