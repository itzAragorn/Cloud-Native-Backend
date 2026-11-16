package com.bancocloud.kafka_admin_service.dto;

import java.util.List;

/**
 * Estado de un consumer group: quién consume qué partición y cuánto le falta (lag).
 *
 * @param lagTotal mensajes pendientes sumando todas las particiones
 */
public record GrupoDetalle(
        String groupId,
        String estado,
        String coordinador,
        List<Miembro> miembros,
        List<OffsetParticion> offsets,
        long lagTotal) {

    public record Miembro(String memberId, String clientId, String host, List<String> particiones) {
    }

    /**
     * @param lag {@code offsetFinal - offsetConfirmado}: mensajes que el grupo aún no procesa
     */
    public record OffsetParticion(String topic, int partition, long offsetConfirmado, long offsetFinal, long lag) {
    }
}
