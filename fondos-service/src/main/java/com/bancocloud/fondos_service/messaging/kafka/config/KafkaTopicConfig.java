package com.bancocloud.fondos_service.messaging.kafka.config;

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
     * {@code bancocloud.fondos.valorcuota}: cambios del valor cuota de cada fondo.
     * <ul>
     *   <li><b>3 particiones</b>: la key es el {@code fondoId}, así cada fondo cae siempre en la
     *       misma partición y sus cambios se leen en orden; hasta 3 consumidores en paralelo.</li>
     *   <li><b>3 réplicas, min.insync.replicas 2</b>: una copia por broker; tolera la caída de
     *       un broker sin perder datos ni detener las escrituras con {@code acks=all}.</li>
     *   <li><b>cleanup.policy compact,delete</b>: lo que importa es el último valor de cada fondo.
     *       La compactación garantiza que el último mensaje de cada key se conserve, así un
     *       consumidor nuevo reconstruye el valor vigente de todos los fondos; {@code delete}
     *       acota además el historial antiguo.</li>
     *   <li><b>retention.ms 30 días</b>: alcanza para la serie histórica reciente del valor
     *       cuota (analítica la guarda en su base) sin crecer sin límite.</li>
     * </ul>
     */
    @Bean
    public NewTopic valorCuotaTopic(BancoKafkaProperties props) {
        BancoKafkaProperties.TopicSpec spec = props.topics().valorCuota();
        return TopicBuilder.name(spec.name())
                .partitions(spec.partitions())
                .replicas(spec.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, spec.cleanupPolicy())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(spec.retentionMs()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(spec.minInsyncReplicas()))
                .build();
    }

    /**
     * {@code bancocloud.fondos.valorcuota.DLT}: mensajes que los consumidores no pudieron
     * procesar. Mismas particiones que el original (el recoverer los deja en la misma partición);
     * {@code delete} y no compactado, porque cada fallo debe quedar registrado aunque repita key.
     */
    @Bean
    public NewTopic valorCuotaDltTopic(BancoKafkaProperties props) {
        BancoKafkaProperties.TopicSpec spec = props.topics().valorCuota();
        return TopicBuilder.name(props.dlt().nombrePara(spec))
                .partitions(spec.partitions())
                .replicas(spec.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(props.dlt().retentionMs()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(spec.minInsyncReplicas()))
                .build();
    }
}
