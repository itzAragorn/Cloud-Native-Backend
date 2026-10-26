package com.bancocloud.fondos_service.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifica que application.yml define todos los nombres de mensajería
 * y que se enlazan a {@link RabbitMQProperties}.
 */
class RabbitMQPropertiesTest {

    private RabbitMQProperties bind() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(env.getPropertySources()::addLast);
        return new Binder(ConfigurationPropertySources.get(env))
                .bind("app.rabbitmq", RabbitMQProperties.class)
                .get();
    }

    @Test
    void enlazaNombresDesdeApplicationYml() throws Exception {
        RabbitMQProperties props = bind();

        assertThat(props.dlx()).isEqualTo("bancocloud.dlx");
        assertThat(props.exchanges().fondos()).isEqualTo("bancocloud.fondos");
        assertThat(props.queues().valorcuotaDlq()).isEqualTo("fondos.valorcuota.q.dlq");
        assertThat(props.routingKeys().valorcuotaActualizado()).isEqualTo("fondo.valorcuota.actualizado");
    }
}
