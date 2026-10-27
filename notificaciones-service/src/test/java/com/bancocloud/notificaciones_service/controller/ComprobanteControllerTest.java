package com.bancocloud.notificaciones_service.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.notificaciones_service.consumer.inversiones.Comprobante;
import com.bancocloud.notificaciones_service.consumer.inversiones.ComprobanteService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ComprobanteControllerTest {

    private final ComprobanteService service = mock(ComprobanteService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ComprobanteController(service)).build();

    private final Comprobante comprobante = new Comprobante("BC-2026-000001", LocalDateTime.now(), "evt-1",
            10L, 1L, 2L, new BigDecimal("1000"), new BigDecimal("1.0000"), new BigDecimal("1000"),
            LocalDateTime.now());

    @Test
    void listaComprobantesDelUsuario() throws Exception {
        when(service.listarPorUsuario(1L)).thenReturn(List.of(comprobante));

        mvc.perform(get("/comprobantes").param("usuarioId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("BC-2026-000001"))
                .andExpect(jsonPath("$[0].inversionId").value(10));
    }

    @Test
    void buscaPorInversion() throws Exception {
        when(service.buscarPorInversion(10L)).thenReturn(Optional.of(comprobante));

        mvc.perform(get("/comprobantes/inversion/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("BC-2026-000001"));
    }

    @Test
    void inversionSinComprobanteDa404() throws Exception {
        when(service.buscarPorInversion(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/comprobantes/inversion/99")).andExpect(status().isNotFound());
    }
}
