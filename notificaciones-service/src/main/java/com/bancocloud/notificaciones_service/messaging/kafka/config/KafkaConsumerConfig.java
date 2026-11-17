package com.bancocloud.notificaciones_service.messaging.kafka.config;

import com.bancocloud.notificaciones_service.exception.EventoInvalidoException;
import com.bancocloud.notificaciones_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

/**
 * Manejo de errores del consumidor Kafka de auditoría.
 * <p>
 * El resto del cliente (deserializers, grupo, {@code ack-mode record}, {@code concurrency 3}) sale
 * de {@code spring.kafka} en application.yml y lo arma la ContainerFactory de Spring Boot, que toma
 * este {@link DefaultErrorHandler} automáticamente. No afecta a los listeners de RabbitMQ.
 * <p>
 * Reintentos con backoff exponencial y después {@code <tópico>.DLT} en la misma partición; el
 * header {@code kafka_dlt-original-consumer-group} indica que falló el grupo de auditoría.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(BancoKafkaProperties props, KafkaProperties kafkaProperties) {
        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(
                plantillasDlt(kafkaProperties),
                (rec, ex) -> new TopicPartition(props.dltDe(rec.topic()), rec.partition()));

        BancoKafkaProperties.Reintentos reintentos = props.reintentos();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(reintentos.maxIntentos());
        backOff.setInitialInterval(reintentos.intervaloInicialMs());
        backOff.setMultiplier(reintentos.multiplicador());
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler((rec, ex) -> {
            log.error("[AUDITORIA] mensaje enviado a {}: topic={} partition={} offset={} key={} eventId={} motivo={}",
                    props.dltDe(rec.topic()), rec.topic(), rec.partition(), rec.offset(), rec.key(),
                    eventId(rec), causaRaiz(ex));
            dlt.accept(rec, ex);
        }, backOff);
        handler.addNotRetryableExceptions(
                EventoInvalidoException.class,
                ValidationException.class,
                MethodArgumentNotValidException.class);
        handler.setRetryListeners((rec, ex, intento) ->
                log.warn("[AUDITORIA] intento {} fallido: topic={} partition={} offset={} key={} eventId={} motivo={}",
                        intento, rec.topic(), rec.partition(), rec.offset(), rec.key(), eventId(rec), causaRaiz(ex)));
        return handler;
    }

    private static Map<Class<?>, KafkaOperations<?, ?>> plantillasDlt(KafkaProperties kafkaProperties) {
        Map<String, Object> base = new HashMap<>(kafkaProperties.buildProducerProperties());
        base.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        Map<String, Object> bytes = new HashMap<>(base);
        bytes.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        Map<String, Object> json = new HashMap<>(base);
        json.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        json.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        Map<Class<?>, KafkaOperations<?, ?>> plantillas = new LinkedHashMap<>();
        plantillas.put(byte[].class, new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(bytes)));
        plantillas.put(Object.class, new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(json)));
        return plantillas;
    }

    private static String eventId(ConsumerRecord<?, ?> rec) {
        return rec.value() instanceof InversionRegistradaKafkaEvent evento ? evento.eventId() : "desconocido";
    }

    private static String causaRaiz(Throwable ex) {
        Throwable causa = ex;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
