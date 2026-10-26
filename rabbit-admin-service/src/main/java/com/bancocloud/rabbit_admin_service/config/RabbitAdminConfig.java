package com.bancocloud.rabbit_admin_service.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitAdmin usado por RabbitAdminService. Se declara con su tipo concreto
 * (no solo AmqpAdmin) porque el servicio necesita su RabbitTemplate para
 * comprobar si un exchange existe.
 */
@Configuration
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
