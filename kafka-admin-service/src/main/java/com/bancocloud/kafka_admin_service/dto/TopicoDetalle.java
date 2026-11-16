package com.bancocloud.kafka_admin_service.dto;

import java.util.List;
import java.util.Map;

/**
 * @param configs configuraciones efectivas de la lista blanca más las fijadas en el tópico
 */
public record TopicoDetalle(
        String name,
        int partitions,
        int replicationFactor,
        List<ParticionInfo> particiones,
        Map<String, String> configs) {

    /**
     * @param isr réplicas en sincronía; si es menor que {@code min.insync.replicas} el tópico no acepta escrituras
     */
    public record ParticionInfo(int partition, Integer leader, List<Integer> replicas, List<Integer> isr) {
    }
}
