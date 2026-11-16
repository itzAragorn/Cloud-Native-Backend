package com.bancocloud.inversiones_service.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Tópicos de Kafka de los que este servicio es dueño. Al arrancar, el {@code KafkaAdmin} de
 * Spring los crea si no existen (el clúster tiene {@code auto.create.topics.enable=false}).
 * Los valores salen de {@code app.kafka} en application.yml.
 * <p>
 * Si el tópico ya existe con más particiones, Kafka no las reduce; los cambios de configuración
 * de un tópico existente se hacen con kafka-admin-service o con {@code kafka/scripts/kafka-admin.sh}.
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * {@code bancocloud.inversiones.eventos}: historial de inversiones para analítica y auditoría.
     * <ul>
     *   <li><b>3 particiones</b>: cada consumer group reparte la carga en hasta 3 hebras o
     *       instancias. La key es el {@code usuarioId}: las inversiones de un usuario quedan en
     *       orden en su partición.</li>
     *   <li><b>3 réplicas, min.insync.replicas 2</b>: una copia por broker; con {@code acks=all}
     *       una escritura se confirma con 2 copias, así la caída de un broker no pierde datos
     *       ni detiene las escrituras.</li>
     *   <li><b>cleanup.policy delete</b>: es un historial de transacciones, cada evento cuenta y
     *       ninguno reemplaza a otro, por eso no se compacta.</li>
     *   <li><b>retention.ms 7 días</b>: margen para releer y reprocesar si un consumidor falla,
     *       sin que el tópico crezca sin límite (el registro permanente está en MySQL).</li>
     * </ul>
     */
    @Bean
    public NewTopic inversionesTopic(BancoKafkaProperties props) {
        BancoKafkaProperties.TopicSpec spec = props.topics().inversiones();
        return TopicBuilder.name(spec.name())
                .partitions(spec.partitions())
                .replicas(spec.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, spec.cleanupPolicy())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(spec.retentionMs()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(spec.minInsyncReplicas()))
                .build();
    }

    /**
     * {@code bancocloud.inversiones.eventos.DLT}: mensajes que los consumidores no pudieron
     * procesar. Mismas particiones que el original, porque el {@code DeadLetterPublishingRecoverer}
     * los deja en la misma partición; {@code delete} con 14 días para revisarlos y reprocesarlos.
     */
    @Bean
    public NewTopic inversionesDltTopic(BancoKafkaProperties props) {
        BancoKafkaProperties.TopicSpec spec = props.topics().inversiones();
        return TopicBuilder.name(props.dlt().nombrePara(spec))
                .partitions(spec.partitions())
                .replicas(spec.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(props.dlt().retentionMs()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(spec.minInsyncReplicas()))
                .build();
    }
}
