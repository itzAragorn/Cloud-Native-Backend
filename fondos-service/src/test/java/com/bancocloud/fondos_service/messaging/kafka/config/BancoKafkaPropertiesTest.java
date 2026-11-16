package com.bancocloud.fondos_service.messaging.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifica que application.yml define los parámetros del tópico de valor cuota
 * y que se enlazan y validan en {@link BancoKafkaProperties}.
 */
class BancoKafkaPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BancoKafkaProperties bind() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(env.getPropertySources()::addLast);
        return new Binder(ConfigurationPropertySources.get(env))
                .bind("app.kafka", BancoKafkaProperties.class)
                .get();
    }

    @Test
    void enlazaTopicoDeValorCuotaDesdeApplicationYml() throws Exception {
        BancoKafkaProperties props = bind();
        BancoKafkaProperties.TopicSpec topic = props.topics().valorCuota();

        assertThat(topic.name()).isEqualTo("bancocloud.fondos.valorcuota");
        assertThat(topic.partitions()).isEqualTo(3);
        assertThat(topic.replicas()).isEqualTo(3);
        assertThat(topic.minInsyncReplicas()).isEqualTo(2);
        assertThat(topic.cleanupPolicy()).isEqualTo("compact,delete");
        assertThat(topic.retentionMs()).isEqualTo(2_592_000_000L);
        assertThat(props.dlt().nombrePara(topic)).isEqualTo("bancocloud.fondos.valorcuota.DLT");
        assertThat(props.dlt().retentionMs()).isEqualTo(1_209_600_000L);
        assertThat(validator.validate(props)).isEmpty();
    }

    @Test
    void rechazaMinInsyncMayorQueReplicasYPoliticaDesconocida() {
        BancoKafkaProperties props = new BancoKafkaProperties(
                new BancoKafkaProperties.Topics(
                        new BancoKafkaProperties.TopicSpec("t", 3, 1, 2, "borrar", 1000)),
                new BancoKafkaProperties.Dlt(".DLT", 1000));

        assertThat(validator.validate(props))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "topics.valorCuota.minInsyncReplicasValido",
                        "topics.valorCuota.cleanupPolicy");
    }
}
