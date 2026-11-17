package com.bancocloud.inversiones_service.messaging.kafka.config;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Cliente productor de Kafka del servicio.
 * <p>
 * Todos los valores salen de {@code spring.kafka} en application.yml: bootstrap servers,
 * {@code StringSerializer} para la key, {@code JacksonJsonSerializer} para el DTO,
 * {@code acks=all}, idempotencia, compresión y timeouts. Aquí no hay valores escritos a mano,
 * así que cambiar el clúster o un parámetro no requiere recompilar.
 * <p>
 * Solo las clases de {@code messaging.kafka.producer} usan el {@link KafkaTemplate};
 * la capa de servicio no conoce Kafka.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }
}
