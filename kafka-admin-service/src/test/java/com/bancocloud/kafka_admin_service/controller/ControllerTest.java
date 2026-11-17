package com.bancocloud.kafka_admin_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.kafka_admin_service.dto.GrupoDetalle;
import com.bancocloud.kafka_admin_service.dto.TopicoDetalle;
import com.bancocloud.kafka_admin_service.dto.TopicoResumen;
import com.bancocloud.kafka_admin_service.exception.ConflictoException;
import com.bancocloud.kafka_admin_service.exception.GlobalExceptionHandler;
import com.bancocloud.kafka_admin_service.exception.KafkaNoDisponibleException;
import com.bancocloud.kafka_admin_service.service.KafkaAdminService;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Códigos HTTP y formato de error de los dos controladores. */
class ControllerTest {

    private final KafkaAdminService service = mock(KafkaAdminService.class);
    private final MockMvc mvc;

    ControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders
                .standaloneSetup(new TopicController(service), new ConsumerGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String json)
            throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static TopicoDetalle detalle(String nombre) {
        return new TopicoDetalle(nombre, 3, 3, List.of(), Map.of("retention.ms", "604800000"));
    }

    @Test
    void listarTopicos() throws Exception {
        when(service.listarTopicos()).thenReturn(List.of(new TopicoResumen("bancocloud.inversiones.eventos", 3, 3)));

        mvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("bancocloud.inversiones.eventos"))
                .andExpect(jsonPath("$[0].replicationFactor").value(3));
    }

    @Test
    void crearTopicoDa201() throws Exception {
        when(service.crearTopico(any())).thenReturn(detalle("demo"));

        enviar(post("/topics"), "{\"name\":\"demo\",\"partitions\":3,\"replicationFactor\":3,\"configs\":{\"retention.ms\":\"604800000\"}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.configs['retention.ms']").value("604800000"));
    }

    @Test
    void crearTopicoInvalidoDa400ConCampos() throws Exception {
        enviar(post("/topics"), "{\"name\":\"__x\",\"partitions\":0,\"replicationFactor\":1,\"configs\":{\"foo\":\"1\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo=='partitions')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo=='name')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo=='configs')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearTopicoExistenteDa409() throws Exception {
        when(service.crearTopico(any())).thenThrow(new TopicExistsException("Topic 'demo' already exists."));

        enviar(post("/topics"), "{\"name\":\"demo\",\"partitions\":1,\"replicationFactor\":1}")
                .andExpect(status().isConflict());
    }

    @Test
    void topicoInexistenteDa404() throws Exception {
        when(service.obtenerTopico("nada")).thenThrow(new UnknownTopicOrPartitionException("x"));

        mvc.perform(get("/topics/nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El tópico no existe"));
    }

    @Test
    void nombreInvalidoEnLaRutaDa400() throws Exception {
        mvc.perform(delete("/topics/__consumer_offsets"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void modificarConfigs() throws Exception {
        when(service.modificarConfigs(eq("demo"), any())).thenReturn(detalle("demo"));

        enviar(put("/topics/demo/configs"), "{\"configs\":{\"retention.ms\":\"86400000\",\"segment.bytes\":null}}")
                .andExpect(status().isOk());
    }

    @Test
    void aumentarParticiones() throws Exception {
        when(service.aumentarParticiones("demo", 6)).thenReturn(detalle("demo"));

        enviar(post("/topics/demo/partitions"), "{\"total\":6}").andExpect(status().isOk());
    }

    @Test
    void eliminarTopicoDa204() throws Exception {
        mvc.perform(delete("/topics/demo")).andExpect(status().isNoContent());
    }

    @Test
    void kafkaCaidoDa503() throws Exception {
        when(service.listarTopicos()).thenThrow(new KafkaNoDisponibleException("timeout", null));

        mvc.perform(get("/topics")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void detalleDeGrupoConLag() throws Exception {
        when(service.obtenerGrupo("analitica-inversiones")).thenReturn(new GrupoDetalle("analitica-inversiones", "STABLE",
                "kafka-2:29092 (broker 2)", List.of(),
                List.of(new GrupoDetalle.OffsetParticion("t", 0, 10, 12, 2)), 2));

        mvc.perform(get("/consumer-groups/analitica-inversiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lagTotal").value(2));
    }

    @Test
    void resetConGrupoActivoDa409() throws Exception {
        when(service.resetearOffsets(eq("g"), any())).thenThrow(new ConflictoException("tiene consumidores"));

        enviar(post("/consumer-groups/g/reset-offsets"), "{\"topic\":\"t\",\"estrategia\":\"EARLIEST\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void resetConEstrategiaDesconocidaDa400() throws Exception {
        enviar(post("/consumer-groups/g/reset-offsets"), "{\"topic\":\"t\",\"estrategia\":\"AYER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("estrategia"));
    }

    @Test
    void eliminarGrupoActivoDa409() throws Exception {
        doThrow(new GroupNotEmptyException("not empty")).when(service).eliminarGrupo("g");

        mvc.perform(delete("/consumer-groups/g")).andExpect(status().isConflict());
    }
}
