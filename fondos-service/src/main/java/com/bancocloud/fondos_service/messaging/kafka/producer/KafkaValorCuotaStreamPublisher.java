package com.bancocloud.fondos_service.messaging.kafka.producer;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import com.bancocloud.fondos_service.messaging.kafka.config.BancoKafkaProperties;
import com.bancocloud.fondos_service.messaging.kafka.event.ValorCuotaKafkaEvent;
import com.bancocloud.fondos_service.service.ValorCuotaStreamPublisher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los cambios de valor cuota en {@code bancocloud.fondos.valorcuota} con key = {@code fondoId}.
 * <p>
 * Valida el DTO antes de enviarlo (un mensaje inválido no sale del servicio) y agrega los
 * headers {@code eventType} y {@code source}. El envío es asíncrono: el resultado (partición
 * y offset, o el motivo del error) queda en el log. Ningún error se propaga: el cambio ya
 * quedó guardado y RabbitMQ publica por su lado.
 */
@Slf4j
@Component
public class KafkaValorCuotaStreamPublisher implements ValorCuotaStreamPublisher {

    public static final String HEADER_EVENT_TYPE = "eventType";
    public static final String HEADER_SOURCE = "source";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BancoKafkaProperties props;
    private final Validator validator;
    private final String source;

    public KafkaValorCuotaStreamPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                          BancoKafkaProperties props,
                                          Validator validator,
                                          @Value("${spring.application.name}") String source) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
        this.validator = validator;
        this.source = source;
    }

    @Override
    public void publicarValorCuota(ValorCuotaActualizadoEvent event) {
        String topic = props.topics().valorCuota().name();
        ValorCuotaKafkaEvent dto = ValorCuotaKafkaEvent.from(event);

        Set<ConstraintViolation<ValorCuotaKafkaEvent>> errores = validator.validate(dto);
        if (!errores.isEmpty()) {
            log.error("Evento {} no publicado en {}: payload inválido (fondoId={}): {}",
                    dto.eventId(), topic, dto.fondoId(), describir(errores));
            return;
        }

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, dto.key(), dto);
        producerRecord.headers()
                .add(HEADER_EVENT_TYPE, dto.eventType().getBytes(StandardCharsets.UTF_8))
                .add(HEADER_SOURCE, source.getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Evento {} publicado en Kafka: topic={} partition={} offset={} key={} valorNuevo={}",
                            dto.eventId(), topic, result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(), dto.key(), dto.valorNuevo());
                } else {
                    log.error("Kafka rechazó el evento {} (fondoId={}) topic={} key={}: {}",
                            dto.eventId(), dto.fondoId(), topic, dto.key(), ex.getMessage());
                }
            });
        } catch (RuntimeException e) {
            // Errores al enviar (broker inalcanzable más de max.block.ms, serialización)
            log.error("No se pudo publicar evento {} (fondoId={}) en topic={} key={}: {}",
                    dto.eventId(), dto.fondoId(), topic, dto.key(), e.getMessage(), e);
        }
    }

    private static String describir(Set<? extends ConstraintViolation<?>> errores) {
        return errores.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
