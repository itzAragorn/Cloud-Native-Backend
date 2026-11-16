package com.bancocloud.kafka_admin_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancocloud.kafka_admin_service.config.KafkaAdminProperties;
import com.bancocloud.kafka_admin_service.dto.CrearTopicoRequest;
import com.bancocloud.kafka_admin_service.dto.GrupoDetalle;
import com.bancocloud.kafka_admin_service.dto.ResetOffsetsRequest;
import com.bancocloud.kafka_admin_service.dto.TopicoResumen;
import com.bancocloud.kafka_admin_service.exception.ConfiguracionInvalidaException;
import com.bancocloud.kafka_admin_service.exception.ConflictoException;
import com.bancocloud.kafka_admin_service.exception.KafkaNoDisponibleException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** AdminClient simulado: se prueba la lógica del servicio sin un clúster. */
class KafkaAdminServiceTest {

    private static final Node B1 = new Node(1, "kafka-1", 29092);
    private static final Node B2 = new Node(2, "kafka-2", 29092);
    private static final Node B3 = new Node(3, "kafka-3", 29092);

    private final Admin admin = mock(Admin.class, RETURNS_DEEP_STUBS);
    private final KafkaAdminService service = new KafkaAdminService(admin, new KafkaAdminProperties(5000));

    private static <T> KafkaFuture<T> ok(T valor) {
        return KafkaFuture.completedFuture(valor);
    }

    private static <T> KafkaFuture<T> falla(Throwable error) {
        KafkaFutureImpl<T> futuro = new KafkaFutureImpl<>();
        futuro.completeExceptionally(error);
        return futuro;
    }

    private static TopicDescription topico(String nombre, int particiones) {
        List<TopicPartitionInfo> info = new java.util.ArrayList<>();
        for (int i = 0; i < particiones; i++) {
            info.add(new TopicPartitionInfo(i, B1, List.of(B1, B2, B3), List.of(B1, B2)));
        }
        return new TopicDescription(nombre, false, info);
    }

    /** Simula describeTopics + describeConfigs, que usan las operaciones al releer el tópico. */
    private void existe(String nombre, int particiones) {
        when(admin.describeTopics(List.of(nombre)).allTopicNames()).thenReturn(ok(Map.of(nombre, topico(nombre, particiones))));
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        when(admin.describeConfigs(List.of(recurso)).all()).thenReturn(ok(Map.of(recurso,
                new Config(List.of(new ConfigEntry("retention.ms", "604800000"))))));
    }

    private static ConsumerGroupDescription grupo(String id, GroupState estado, List<MemberDescription> miembros) {
        return new ConsumerGroupDescription(id, false, miembros, "range", GroupType.CLASSIC, estado, B2, Set.of(),
                Optional.empty(), Optional.empty());
    }

    @Test
    void listarTopicosOmiteInternosYOrdena() {
        when(admin.listTopics(any(ListTopicsOptions.class)).names())
                .thenReturn(ok(Set.of("z.topic", "__consumer_offsets", "a.topic")));
        when(admin.describeTopics(List.of("a.topic", "z.topic")).allTopicNames())
                .thenReturn(ok(Map.of("a.topic", topico("a.topic", 3), "z.topic", topico("z.topic", 1))));

        List<TopicoResumen> topicos = service.listarTopicos();

        assertThat(topicos).extracting(TopicoResumen::name).containsExactly("a.topic", "z.topic");
        assertThat(topicos.get(0).partitions()).isEqualTo(3);
        assertThat(topicos.get(0).replicationFactor()).isEqualTo(3);
    }

    @Test
    void crearTopicoRechazaMasReplicasQueBrokers() {
        when(admin.describeCluster().nodes()).thenReturn(ok(List.of(B1, B2)));

        assertThatThrownBy(() -> service.crearTopico(new CrearTopicoRequest("demo", 3, (short) 3, null)))
                .isInstanceOf(ConfiguracionInvalidaException.class)
                .hasMessageContaining("(2)");
        verify(admin, never()).createTopics(anyCollection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearTopicoEnviaParticionesReplicasYConfigs() {
        when(admin.describeCluster().nodes()).thenReturn(ok(List.of(B1, B2, B3)));
        when(admin.createTopics(anyCollection()).all()).thenReturn(ok(null));
        existe("demo", 3);

        var detalle = service.crearTopico(new CrearTopicoRequest("demo", 3, (short) 3, Map.of("retention.ms", " 86400000 ")));

        assertThat(detalle.particiones()).hasSize(3);
        assertThat(detalle.particiones().get(0).isr()).containsExactly(1, 2);
        assertThat(detalle.configs()).containsEntry("retention.ms", "604800000");

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin, org.mockito.Mockito.atLeastOnce()).createTopics(captor.capture());
        NewTopic enviado = captor.getAllValues().stream().filter(c -> !c.isEmpty()).findFirst().orElseThrow()
                .iterator().next();
        assertThat(enviado.name()).isEqualTo("demo");
        assertThat(enviado.numPartitions()).isEqualTo(3);
        assertThat(enviado.replicationFactor()).isEqualTo((short) 3);
        assertThat(enviado.configs()).containsEntry("retention.ms", "86400000");
    }

    @Test
    @SuppressWarnings("unchecked")
    void modificarConfigsUsaSetYDeleteIncrementales() {
        existe("demo", 3);
        when(admin.incrementalAlterConfigs(anyMap()).all()).thenReturn(ok(null));

        java.util.HashMap<String, String> cambios = new java.util.HashMap<>();
        cambios.put("retention.ms", "86400000");
        cambios.put("segment.bytes", null);
        service.modificarConfigs("demo", cambios);

        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(admin, org.mockito.Mockito.atLeastOnce()).incrementalAlterConfigs(captor.capture());
        Collection<AlterConfigOp> ops = captor.getAllValues().stream().filter(m -> !m.isEmpty()).findFirst().orElseThrow()
                .get(new ConfigResource(ConfigResource.Type.TOPIC, "demo"));
        assertThat(ops).extracting(op -> op.configEntry().name() + "=" + op.opType())
                .containsExactlyInAnyOrder("retention.ms=SET", "segment.bytes=DELETE");
    }

    @Test
    void noPermiteDisminuirParticiones() {
        existe("demo", 3);

        assertThatThrownBy(() -> service.aumentarParticiones("demo", 3))
                .isInstanceOf(ConfiguracionInvalidaException.class)
                .hasMessageContaining("disminuirlas");
    }

    @Test
    void topicoInexistentePropagaUnknownTopic() {
        when(admin.describeTopics(List.of("nada")).allTopicNames())
                .thenReturn(falla(new UnknownTopicOrPartitionException("no existe")));

        assertThatThrownBy(() -> service.aumentarParticiones("nada", 5))
                .isInstanceOf(UnknownTopicOrPartitionException.class);
    }

    @Test
    void timeoutDeKafkaEsNoDisponible() {
        when(admin.describeCluster().nodes()).thenReturn(falla(new TimeoutException("sin brokers")));

        assertThatThrownBy(() -> service.crearTopico(new CrearTopicoRequest("demo", 1, (short) 1, null)))
                .isInstanceOf(KafkaNoDisponibleException.class);
    }

    @Test
    void obtenerGrupoCalculaLagPorParticion() {
        TopicPartition p0 = new TopicPartition("bancocloud.inversiones.eventos", 0);
        TopicPartition p1 = new TopicPartition("bancocloud.inversiones.eventos", 1);
        MemberDescription miembro = new MemberDescription("m-1", "analitica-service-0", "/10.0.0.5",
                new MemberAssignment(Set.of(p1, p0)));
        when(admin.describeConsumerGroups(List.of("analitica-inversiones")).describedGroups().get("analitica-inversiones"))
                .thenReturn(ok(grupo("analitica-inversiones", GroupState.STABLE, List.of(miembro))));
        when(admin.listConsumerGroupOffsets("analitica-inversiones").partitionsToOffsetAndMetadata())
                .thenReturn(ok(Map.of(p0, new OffsetAndMetadata(10), p1, new OffsetAndMetadata(4))));
        when(admin.listOffsets(anyMap()).all()).thenReturn(ok(Map.of(
                p0, new ListOffsetsResultInfo(12, -1, Optional.empty()),
                p1, new ListOffsetsResultInfo(4, -1, Optional.empty()))));

        GrupoDetalle detalle = service.obtenerGrupo("analitica-inversiones");

        assertThat(detalle.estado()).isEqualTo("STABLE");
        assertThat(detalle.lagTotal()).isEqualTo(2);
        assertThat(detalle.offsets()).extracting(GrupoDetalle.OffsetParticion::lag).containsExactly(2L, 0L);
        assertThat(detalle.miembros().get(0).particiones())
                .containsExactly("bancocloud.inversiones.eventos-0", "bancocloud.inversiones.eventos-1");
        assertThat(detalle.coordinador()).contains("broker 2");
    }

    @Test
    void resetDeOffsetsExigeGrupoSinConsumidores() {
        MemberDescription miembro = new MemberDescription("m-1", "c", "/h", new MemberAssignment(Set.of()));
        when(admin.describeConsumerGroups(List.of("g")).describedGroups().get("g"))
                .thenReturn(ok(grupo("g", GroupState.STABLE, List.of(miembro))));

        assertThatThrownBy(() -> service.resetearOffsets("g",
                new ResetOffsetsRequest("t", ResetOffsetsRequest.EstrategiaReset.EARLIEST, null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("detén");
        verify(admin, never()).alterConsumerGroupOffsets(any(), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetAOffsetFijoEnUnaParticion() {
        when(admin.describeConsumerGroups(List.of("g")).describedGroups().get("g"))
                .thenReturn(ok(grupo("g", GroupState.EMPTY, List.of())));
        when(admin.describeTopics(List.of("t")).allTopicNames()).thenReturn(ok(Map.of("t", topico("t", 3))));
        when(admin.alterConsumerGroupOffsets(eq("g"), anyMap()).all()).thenReturn(ok(null));
        when(admin.listConsumerGroupOffsets("g").partitionsToOffsetAndMetadata()).thenReturn(ok(Map.of()));

        service.resetearOffsets("g", new ResetOffsetsRequest("t", ResetOffsetsRequest.EstrategiaReset.OFFSET, 1, 7L));

        ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> captor = ArgumentCaptor.forClass(Map.class);
        verify(admin, org.mockito.Mockito.atLeastOnce()).alterConsumerGroupOffsets(eq("g"), captor.capture());
        Map<TopicPartition, OffsetAndMetadata> enviados = captor.getAllValues().stream()
                .filter(m -> !m.isEmpty()).findFirst().orElseThrow();
        assertThat(enviados).containsOnlyKeys(new TopicPartition("t", 1));
        assertThat(enviados.get(new TopicPartition("t", 1)).offset()).isEqualTo(7L);
    }
}
