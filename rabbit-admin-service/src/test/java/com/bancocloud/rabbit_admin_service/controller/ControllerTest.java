package com.bancocloud.rabbit_admin_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.rabbit_admin_service.dto.BindingRequest;
import com.bancocloud.rabbit_admin_service.dto.ColaResponse;
import com.bancocloud.rabbit_admin_service.dto.ExchangeResponse;
import com.bancocloud.rabbit_admin_service.dto.TipoExchange;
import com.bancocloud.rabbit_admin_service.exception.GlobalExceptionHandler;
import com.bancocloud.rabbit_admin_service.exception.RecursoNoEncontradoException;
import com.bancocloud.rabbit_admin_service.exception.RecursoYaExisteException;
import com.bancocloud.rabbit_admin_service.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Códigos HTTP y formato de error de los tres controladores. */
class ControllerTest {

    private final RabbitAdminService service = mock(RabbitAdminService.class);
    private final MockMvc mvc;

    ControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders
                .standaloneSetup(new QueueController(service), new ExchangeController(service),
                        new BindingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void crearColaDa201() throws Exception {
        when(service.crearCola(any())).thenReturn(new ColaResponse("demo.q", 0, 0));

        postJson("/queues", "{\"name\":\"demo.q\",\"durable\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("demo.q"));
    }

    @Test
    void crearColaInvalidaDa400ConCampos() throws Exception {
        postJson("/queues", "{\"name\":\"amq.x\",\"ttlMs\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores[?(@.campo=='name')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo=='ttlMs')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearColaExistenteDa409() throws Exception {
        when(service.crearCola(any())).thenThrow(new RecursoYaExisteException("la cola", "demo.q"));

        postJson("/queues", "{\"name\":\"demo.q\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe la cola 'demo.q'"));
    }

    @Test
    void obtenerColaDa200() throws Exception {
        when(service.obtenerCola("demo.q")).thenReturn(new ColaResponse("demo.q", 5, 2));

        mvc.perform(get("/queues/demo.q"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajes").value(5))
                .andExpect(jsonPath("$.consumidores").value(2));
    }

    @Test
    void eliminarColaDa204() throws Exception {
        mvc.perform(delete("/queues/demo.q")).andExpect(status().isNoContent());
        verify(service).eliminarCola("demo.q");
    }

    @Test
    void eliminarColaInexistenteDa404() throws Exception {
        doThrow(new RecursoNoEncontradoException("la cola", "no.q")).when(service).eliminarCola("no.q");

        mvc.perform(delete("/queues/no.q"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void eliminarColaReservadaDa400() throws Exception {
        mvc.perform(delete("/queues/amq.gen-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("name"));
        verifyNoInteractions(service);
    }

    @Test
    void crearExchangeDa201() throws Exception {
        when(service.crearExchange(any())).thenReturn(new ExchangeResponse("demo.x", TipoExchange.TOPIC, true));

        postJson("/exchanges", "{\"name\":\"demo.x\",\"type\":\"TOPIC\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TOPIC"));
    }

    @Test
    void tipoDeExchangeDesconocidoDa400Claro() throws Exception {
        postJson("/exchanges", "{\"name\":\"demo.x\",\"type\":\"XYZ\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("type"))
                .andExpect(jsonPath("$.errores[0].error").value(
                        "valor 'XYZ' inválido; usa uno de DIRECT, TOPIC, FANOUT, HEADERS"));
    }

    @Test
    void eliminarExchangeDa204() throws Exception {
        mvc.perform(delete("/exchanges/demo.x")).andExpect(status().isNoContent());
    }

    @Test
    void crearBindingDa201() throws Exception {
        when(service.crearBinding(any())).thenReturn(new BindingRequest("demo.q", "demo.x", "k"));

        postJson("/bindings", "{\"queue\":\"demo.q\",\"exchange\":\"demo.x\",\"routingKey\":\"k\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void crearBindingSinExchangeDa404() throws Exception {
        when(service.crearBinding(any())).thenThrow(new RecursoNoEncontradoException("el exchange", "no.x"));

        postJson("/bindings", "{\"queue\":\"demo.q\",\"exchange\":\"no.x\",\"routingKey\":\"k\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarBindingDa204() throws Exception {
        mvc.perform(delete("/bindings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queue\":\"demo.q\",\"exchange\":\"demo.x\",\"routingKey\":\"k\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        postJson("/queues", "{no json").andExpect(status().isBadRequest());
    }

    @Test
    void brokerCaidoDa503() throws Exception {
        when(service.obtenerCola("demo.q")).thenThrow(new AmqpConnectException(new java.net.ConnectException("x")));

        mvc.perform(get("/queues/demo.q")).andExpect(status().isServiceUnavailable());
    }
}
