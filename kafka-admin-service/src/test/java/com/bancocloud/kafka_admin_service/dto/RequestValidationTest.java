package com.bancocloud.kafka_admin_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private <T> List<String> errores(T objeto) {
        Set<ConstraintViolation<T>> violaciones = validator.validate(objeto);
        return violaciones.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
    }

    @Test
    void crearTopicoValido() {
        assertThat(errores(new CrearTopicoRequest("bancocloud.demo", 3, (short) 3,
                Map.of("retention.ms", "604800000", "cleanup.policy", "compact,delete", "min.insync.replicas", "2"))))
                .isEmpty();
    }

    @Test
    void rechazaNombreInternoOConCaracteresInvalidos() {
        assertThat(errores(new CrearTopicoRequest("__interno", 1, (short) 1, null)))
                .anyMatch(e -> e.contains("'__'"));
        assertThat(errores(new CrearTopicoRequest("con espacio", 1, (short) 1, null)))
                .anyMatch(e -> e.contains("solo admite"));
        assertThat(errores(new CrearTopicoRequest("x".repeat(250), 1, (short) 1, null)))
                .anyMatch(e -> e.contains("249"));
    }

    @Test
    void rechazaParticionesYReplicasFueraDeRango() {
        assertThat(errores(new CrearTopicoRequest("t", 0, (short) 0, null)))
                .contains("partitions: debe ser al menos 1", "replicationFactor: debe ser al menos 1");
    }

    @Test
    void rechazaConfigsFueraDeLaListaBlancaYValoresInvalidos() {
        assertThat(errores(new CrearTopicoRequest("t", 1, (short) 1,
                Map.of("unclean.leader.election.enable", "true", "cleanup.policy", "borrar", "retention.ms", "0"))))
                .anyMatch(e -> e.contains("unclean.leader.election.enable: configuración no permitida"))
                .anyMatch(e -> e.contains("cleanup.policy: debe ser delete, compact o compact,delete"))
                .anyMatch(e -> e.contains("retention.ms: debe ser -1"));
    }

    @Test
    void rechazaMinInsyncMayorQueReplicas() {
        assertThat(errores(new CrearTopicoRequest("t", 1, (short) 2, Map.of("min.insync.replicas", "3"))))
                .anyMatch(e -> e.contains("min.insync.replicas no puede ser mayor que replicationFactor"));
    }

    @Test
    void modificarPermiteNulosParaVolverAlDefault() {
        Map<String, String> configs = new HashMap<>();
        configs.put("retention.ms", null);
        assertThat(errores(new ModificarConfigsRequest(configs))).isEmpty();
        assertThat(errores(new ModificarConfigsRequest(Map.of()))).isNotEmpty();
    }

    @Test
    void resetConOffsetSoloConEstrategiaOffset() {
        assertThat(errores(new ResetOffsetsRequest("t", ResetOffsetsRequest.EstrategiaReset.OFFSET, null, null)))
                .anyMatch(e -> e.contains("offset es obligatorio"));
        assertThat(errores(new ResetOffsetsRequest("t", ResetOffsetsRequest.EstrategiaReset.LATEST, null, 5L)))
                .anyMatch(e -> e.contains("offset es obligatorio"));
        assertThat(errores(new ResetOffsetsRequest("t", ResetOffsetsRequest.EstrategiaReset.EARLIEST, 0, null))).isEmpty();
    }
}
