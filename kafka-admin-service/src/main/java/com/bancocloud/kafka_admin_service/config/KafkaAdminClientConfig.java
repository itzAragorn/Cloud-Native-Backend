package com.bancocloud.kafka_admin_service.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Un único {@link Admin} de Kafka para todo el servicio, creado con la configuración del bean
 * {@link KafkaAdmin} de Spring (bootstrap servers y timeouts de {@code spring.kafka.admin}).
 * Se cierra al detener la aplicación.
 */
@Configuration
public class KafkaAdminClientConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
