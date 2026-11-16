package com.bancocloud.kafka_admin_service.service;

import com.bancocloud.kafka_admin_service.config.KafkaAdminProperties;
import com.bancocloud.kafka_admin_service.dto.CrearTopicoRequest;
import com.bancocloud.kafka_admin_service.dto.GrupoDetalle;
import com.bancocloud.kafka_admin_service.dto.GrupoResumen;
import com.bancocloud.kafka_admin_service.dto.ResetOffsetsRequest;
import com.bancocloud.kafka_admin_service.dto.TopicoDetalle;
import com.bancocloud.kafka_admin_service.dto.TopicoResumen;
import com.bancocloud.kafka_admin_service.exception.ConfiguracionInvalidaException;
import com.bancocloud.kafka_admin_service.exception.ConflictoException;
import com.bancocloud.kafka_admin_service.exception.KafkaNoDisponibleException;
import com.bancocloud.kafka_admin_service.exception.RecursoNoEncontradoException;
import com.bancocloud.kafka_admin_service.validation.ConfigsTopico;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Administración de Kafka: tópicos, sus configuraciones y consumer groups.
 * <p>
 * Encapsula el {@link Admin} de Kafka; los controladores solo llaman a estos métodos. Cada
 * operación espera como máximo {@code app.kafka-admin.timeout-ms}. Los errores de Kafka
 * ({@code UnknownTopicOrPartitionException}, {@code TopicExistsException}, ...) se propagan
 * tal cual y {@code GlobalExceptionHandler} los traduce a 404, 409 o 400.
 */
@Slf4j
@Service
public class KafkaAdminService {

    private static final Set<GroupState> ESTADOS_INACTIVOS = Set.of(GroupState.EMPTY, GroupState.DEAD);

    private final Admin admin;
    private final long timeoutMs;

    public KafkaAdminService(Admin admin, KafkaAdminProperties props) {
        this.admin = admin;
        this.timeoutMs = props.timeoutMs();
    }

    // ------------------------------------------------------------------ tópicos

    /** Tópicos de la aplicación, sin los internos ({@code __consumer_offsets}, ...). */
    public List<TopicoResumen> listarTopicos() {
        List<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names()).stream()
                .filter(n -> !n.startsWith("__"))
                .sorted()
                .toList();
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        return nombres.stream()
                .map(descripciones::get)
                .map(d -> new TopicoResumen(d.name(), d.partitions().size(), factorReplicacion(d)))
                .toList();
    }

    /** Particiones con líder, réplicas e ISR, y las configuraciones relevantes. */
    public TopicoDetalle obtenerTopico(String nombre) {
        TopicDescription d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);

        Map<String, String> configs = new TreeMap<>();
        for (ConfigEntry entry : config.entries()) {
            boolean fijadaEnTopico = entry.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG;
            if (ConfigsTopico.PERMITIDAS.contains(entry.name()) || fijadaEnTopico) {
                configs.put(entry.name(), entry.value());
            }
        }
        List<TopicoDetalle.ParticionInfo> particiones = d.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new TopicoDetalle.ParticionInfo(
                        p.partition(),
                        p.leader() == null ? null : p.leader().id(),
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        return new TopicoDetalle(d.name(), particiones.size(), factorReplicacion(d), particiones, configs);
    }

    public TopicoDetalle crearTopico(CrearTopicoRequest request) {
        int brokers = esperar(admin.describeCluster().nodes()).size();
        if (request.replicationFactor() > brokers) {
            throw new ConfiguracionInvalidaException("replicationFactor",
                    "no puede ser mayor que la cantidad de brokers del clúster (" + brokers + ")");
        }
        NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicationFactor())
                .configs(recortar(request.configsOVacio()));
        esperar(admin.createTopics(List.of(topic)).all());
        log.info("Tópico creado: {} particiones={} réplicas={} configs={}", request.name(), request.partitions(),
                request.replicationFactor(), request.configsOVacio());
        return obtenerTopico(request.name());
    }

    /**
     * Cambia solo las claves recibidas ({@code incrementalAlterConfigs}): un valor fija la clave
     * (SET) y {@code null} la devuelve al valor por defecto del clúster (DELETE).
     */
    public TopicoDetalle modificarConfigs(String nombre, Map<String, String> configs) {
        existeTopico(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        List<AlterConfigOp> operaciones = new ArrayList<>();
        configs.forEach((clave, valor) -> operaciones.add(valor == null
                ? new AlterConfigOp(new ConfigEntry(clave, null), AlterConfigOp.OpType.DELETE)
                : new AlterConfigOp(new ConfigEntry(clave, valor.trim()), AlterConfigOp.OpType.SET)));
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all());
        log.info("Configuración del tópico {} modificada: {}", nombre, configs);
        return obtenerTopico(nombre);
    }

    /** Kafka no permite disminuir particiones: un total menor o igual al actual responde 400. */
    public TopicoDetalle aumentarParticiones(String nombre, int total) {
        int actuales = existeTopico(nombre).partitions().size();
        if (total <= actuales) {
            throw new ConfiguracionInvalidaException("total",
                    "debe ser mayor que las particiones actuales (" + actuales + "); Kafka no permite disminuirlas");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("Tópico {}: particiones {} -> {}", nombre, actuales, total);
        return obtenerTopico(nombre);
    }

    public void eliminarTopico(String nombre) {
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("Tópico eliminado: {}", nombre);
    }

    // ------------------------------------------------------------------ consumer groups

    public List<GrupoResumen> listarGrupos() {
        Collection<GroupListing> grupos = esperar(admin.listGroups().all());
        return grupos.stream()
                .map(g -> new GrupoResumen(g.groupId(), g.groupState().map(Enum::name).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(GrupoResumen::groupId))
                .toList();
    }

    /** Miembros, particiones asignadas, offsets confirmados y lag por partición. */
    public GrupoDetalle obtenerGrupo(String groupId) {
        ConsumerGroupDescription grupo = describirGrupo(groupId);
        Map<TopicPartition, OffsetAndMetadata> confirmados = offsetsConfirmados(groupId);
        if (grupo.groupState() == GroupState.DEAD && confirmados.isEmpty()) {
            throw new RecursoNoEncontradoException("El consumer group '" + groupId + "' no existe");
        }

        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        confirmados.keySet().forEach(tp -> pedido.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> finales = pedido.isEmpty()
                ? Map.of() : esperar(admin.listOffsets(pedido).all());

        List<GrupoDetalle.OffsetParticion> offsets = confirmados.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(TopicPartition::topic)
                        .thenComparingInt(TopicPartition::partition)))
                .map(e -> {
                    long confirmado = e.getValue().offset();
                    ListOffsetsResultInfo info = finales.get(e.getKey());
                    long fin = info == null ? confirmado : info.offset();
                    return new GrupoDetalle.OffsetParticion(e.getKey().topic(), e.getKey().partition(),
                            confirmado, fin, Math.max(0, fin - confirmado));
                })
                .toList();

        List<GrupoDetalle.Miembro> miembros = grupo.members().stream()
                .map(m -> new GrupoDetalle.Miembro(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                                .map(TopicPartition::toString)
                                .toList()))
                .toList();

        Node coordinador = grupo.coordinator();
        return new GrupoDetalle(groupId, grupo.groupState().name(),
                coordinador == null ? null : coordinador.host() + ":" + coordinador.port() + " (broker " + coordinador.id() + ")",
                miembros, offsets, offsets.stream().mapToLong(GrupoDetalle.OffsetParticion::lag).sum());
    }

    /**
     * Mueve los offsets del grupo en un tópico. Kafka exige que el grupo no tenga consumidores
     * activos: hay que detener el servicio consumidor antes (si no, 409).
     *
     * @return el detalle del grupo con los offsets nuevos
     */
    public GrupoDetalle resetearOffsets(String groupId, ResetOffsetsRequest request) {
        ConsumerGroupDescription grupo = describirGrupo(groupId);
        if (!ESTADOS_INACTIVOS.contains(grupo.groupState())) {
            throw new ConflictoException("El consumer group '" + groupId + "' tiene " + grupo.members().size()
                    + " consumidor(es) activo(s) (estado " + grupo.groupState()
                    + "); detén el servicio consumidor antes de mover los offsets");
        }
        TopicDescription topic = existeTopico(request.topic());
        List<TopicPartition> particiones = topic.partitions().stream()
                .map(p -> new TopicPartition(request.topic(), p.partition()))
                .filter(tp -> request.partition() == null || tp.partition() == request.partition())
                .toList();
        if (particiones.isEmpty()) {
            throw new ConfiguracionInvalidaException("partition",
                    "el tópico " + request.topic() + " no tiene la partición " + request.partition());
        }

        Map<TopicPartition, OffsetAndMetadata> nuevos = new LinkedHashMap<>();
        if (request.estrategia() == ResetOffsetsRequest.EstrategiaReset.OFFSET) {
            particiones.forEach(tp -> nuevos.put(tp, new OffsetAndMetadata(request.offset())));
        } else {
            OffsetSpec spec = request.estrategia() == ResetOffsetsRequest.EstrategiaReset.EARLIEST
                    ? OffsetSpec.earliest() : OffsetSpec.latest();
            Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
            particiones.forEach(tp -> pedido.put(tp, spec));
            esperar(admin.listOffsets(pedido).all())
                    .forEach((tp, info) -> nuevos.put(tp, new OffsetAndMetadata(info.offset())));
        }
        esperar(admin.alterConsumerGroupOffsets(groupId, nuevos).all());
        log.info("Offsets del grupo {} en {} movidos a {}: {}", groupId, request.topic(), request.estrategia(), nuevos);
        return obtenerGrupo(groupId);
    }

    /** Solo grupos sin consumidores activos (si no, Kafka responde GroupNotEmptyException → 409). */
    public void eliminarGrupo(String groupId) {
        esperar(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.info("Consumer group eliminado: {}", groupId);
    }

    // ------------------------------------------------------------------ apoyo

    private TopicDescription existeTopico(String nombre) {
        return esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
    }

    private ConsumerGroupDescription describirGrupo(String groupId) {
        try {
            return esperar(admin.describeConsumerGroups(List.of(groupId)).describedGroups().get(groupId));
        } catch (GroupIdNotFoundException e) {
            throw new RecursoNoEncontradoException("El consumer group '" + groupId + "' no existe");
        }
    }

    private Map<TopicPartition, OffsetAndMetadata> offsetsConfirmados(String groupId) {
        Map<TopicPartition, OffsetAndMetadata> offsets = esperar(
                admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetAndMetadata> sinNulos = new HashMap<>();
        offsets.forEach((tp, om) -> {
            if (om != null) {
                sinNulos.put(tp, om);
            }
        });
        return sinNulos;
    }

    private static int factorReplicacion(TopicDescription d) {
        return d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
    }

    private static Map<String, String> recortar(Map<String, String> configs) {
        Map<String, String> limpias = new HashMap<>();
        configs.forEach((k, v) -> limpias.put(k, v.trim()));
        return limpias;
    }

    /**
     * Espera el resultado del AdminClient. Desenvuelve la causa real (las excepciones de Kafka son
     * runtime) y convierte la espera agotada en {@link KafkaNoDisponibleException}.
     */
    <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof org.apache.kafka.common.errors.TimeoutException) {
                throw new KafkaNoDisponibleException("Kafka no respondió: " + causa.getMessage(), causa);
            }
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(causa);
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondió en " + timeoutMs + " ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida", e);
        }
    }
}
