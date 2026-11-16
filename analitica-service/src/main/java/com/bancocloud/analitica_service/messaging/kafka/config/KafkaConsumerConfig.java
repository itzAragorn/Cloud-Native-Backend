package com.bancocloud.analitica_service.messaging.kafka.config;

import com.bancocloud.analitica_service.exception.EventoInvalidoException;
import com.bancocloud.analitica_service.messaging.kafka.event.EventoKafka;
import com.bancocloud.analitica_service.messaging.kafka.event.InversionRegistradaKafkaEvent;
import com.bancocloud.analitica_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Cliente consumidor de Kafka y manejo de errores.
 * <p>
 * La base sale de {@code spring.kafka} en application.yml (bootstrap servers, {@code earliest},
 * sin auto commit, {@code ErrorHandlingDeserializer} sobre {@code JacksonJsonDeserializer},
 * {@code ack-mode record}, {@code concurrency 3}). Hay una ContainerFactory por tópico porque
 * cada uno se deserializa a un DTO distinto ({@code spring.json.value.default.type}).
 * <p>
 * <b>Manejo de errores</b> ({@link #kafkaErrorHandler}): un mensaje que falla se reintenta con
 * backoff exponencial (1 s, 2 s, 4 s) y después se publica en {@code <tópico>.DLT}, en la misma
 * partición. Los errores que no se arreglan reintentando (JSON roto, payload inválido) van
 * directo al .DLT. En ningún caso el mensaje malo detiene el consumo de los siguientes.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    // ---------------------------------------------------------------- container factories

    /** Tópico de inversiones: mensajes {@link InversionRegistradaKafkaEvent}. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> inversionesListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler kafkaErrorHandler) {
        return factory(configurer, consumerFactoryJson(kafkaProperties, InversionRegistradaKafkaEvent.class),
                kafkaErrorHandler);
    }

    /** Tópico de valor cuota: mensajes {@link ValorCuotaKafkaEvent}. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> valorCuotaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler kafkaErrorHandler) {
        return factory(configurer, consumerFactoryJson(kafkaProperties, ValorCuotaKafkaEvent.class),
                kafkaErrorHandler);
    }

    /**
     * Tópicos .DLT: se leen como texto, sin deserializar a un DTO (pueden traer JSON roto).
     * Sin reintentos ni otro DLT: si registrar un mensaje muerto falla, solo se deja en el log.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> dltListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.remove(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS);
        DefaultErrorHandler soloLog = new DefaultErrorHandler(
                (rec, ex) -> log.error("[DLT] no se pudo registrar el mensaje topic={} partition={} offset={}: {}",
                        rec.topic(), rec.partition(), rec.offset(), ex.getMessage()),
                new FixedBackOff(0L, 0L));
        return factory(configurer, new DefaultKafkaConsumerFactory<>(config), soloLog);
    }

    private static ConcurrentKafkaListenerContainerFactory<Object, Object> factory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // Aplica spring.kafka.listener: ack-mode record, concurrency 3, missing-topics-fatal
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    private static ConsumerFactory<Object, Object> consumerFactoryJson(KafkaProperties kafkaProperties, Class<?> tipo) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, tipo.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    // ---------------------------------------------------------------- manejo de errores

    /**
     * Reintentos + DLT, compartido por los listeners de inversiones y valor cuota.
     * <ul>
     *   <li>Backoff exponencial desde {@code app.kafka.reintentos}: errores transitorios (MySQL
     *       caído, conflicto de concurrencia) se resuelven solos.</li>
     *   <li>No reintentables: {@code DeserializationException} (ya lo es por defecto),
     *       {@link EventoInvalidoException}, {@link ValidationException} y
     *       {@link MethodArgumentNotValidException}.</li>
     *   <li>Agotados los intentos: log con contexto y {@link DeadLetterPublishingRecoverer} a
     *       {@code <tópico>.DLT} en la misma partición, con headers {@code kafka_dlt-*}.</li>
     * </ul>
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(BancoKafkaProperties props, KafkaProperties kafkaProperties) {
        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(
                plantillasDlt(kafkaProperties),
                (rec, ex) -> new TopicPartition(props.dltDe(rec.topic()), rec.partition()));

        ConsumerRecordRecoverer recoverer = (rec, ex) -> {
            log.error("Mensaje enviado a {}: topic={} partition={} offset={} key={} eventId={} motivo={}",
                    props.dltDe(rec.topic()), rec.topic(), rec.partition(), rec.offset(), rec.key(),
                    eventId(rec), causaRaiz(ex));
            dlt.accept(rec, ex);
        };

        BancoKafkaProperties.Reintentos reintentos = props.reintentos();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(reintentos.maxIntentos());
        backOff.setInitialInterval(reintentos.intervaloInicialMs());
        backOff.setMultiplier(reintentos.multiplicador());
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                EventoInvalidoException.class,
                ValidationException.class,
                MethodArgumentNotValidException.class);
        handler.setRetryListeners((rec, ex, intento) ->
                log.warn("Intento {} fallido: topic={} partition={} offset={} key={} eventId={} motivo={}",
                        intento, rec.topic(), rec.partition(), rec.offset(), rec.key(), eventId(rec), causaRaiz(ex)));
        return handler;
    }

    /**
     * Plantillas para publicar en el .DLT: los bytes originales cuando el JSON no se pudo leer,
     * y el DTO como JSON cuando el mensaje se leyó pero no se pudo procesar.
     */
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
        return rec.value() instanceof EventoKafka evento ? evento.eventId() : "desconocido";
    }

    private static String causaRaiz(Throwable ex) {
        Throwable causa = ex;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
