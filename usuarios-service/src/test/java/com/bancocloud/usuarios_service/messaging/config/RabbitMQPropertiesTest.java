package com.bancocloud.usuarios_service.messaging.config;

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
        assertThat(props.exchanges().usuarios()).isEqualTo("bancocloud.usuarios");
        assertThat(props.queues().registro()).isEqualTo("usuarios.registro.q");
        assertThat(props.queues().registroDlq()).isEqualTo("usuarios.registro.q.dlq");
        assertThat(props.queues().estadoDlq()).isEqualTo("usuarios.estado.q.dlq");
        assertThat(props.routingKeys().usuarioRegistrado()).isEqualTo("usuario.registrado");
        assertThat(props.routingKeys().usuarioEstadoPattern()).isEqualTo("usuario.estado.*");
        assertThat(props.routingKeys().usuarioDesbloqueado()).isEqualTo("usuario.estado.desbloqueado");
        assertThat(props.routingKeys().usuarioDesautorizado()).isEqualTo("usuario.estado.desautorizado");
    }
}
