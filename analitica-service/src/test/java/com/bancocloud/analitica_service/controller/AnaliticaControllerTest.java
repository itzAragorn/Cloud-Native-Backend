package com.bancocloud.analitica_service.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.analitica_service.dto.ResumenFondoDTO;
import com.bancocloud.analitica_service.dto.ValorCuotaPuntoDTO;
import com.bancocloud.analitica_service.exception.GlobalExceptionHandler;
import com.bancocloud.analitica_service.service.ConsultaAnaliticaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AnaliticaControllerTest {

    private final ConsultaAnaliticaService service = mock(ConsultaAnaliticaService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AnaliticaController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void resumenDevuelveRanking() throws Exception {
        when(service.resumenFondos()).thenReturn(List.of(new ResumenFondoDTO(1, 3L, "Fondo Tres",
                new BigDecimal("9000"), 4, new BigDecimal("1100"), new BigDecimal("1.5"), LocalDateTime.now())));

        mvc.perform(get("/analitica/fondos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ranking").value(1))
                .andExpect(jsonPath("$[0].fondoId").value(3))
                .andExpect(jsonPath("$[0].cantidadInversiones").value(4));
    }

    @Test
    void historialDelValorCuota() throws Exception {
        when(service.historialValorCuota(5L)).thenReturn(List.of(new ValorCuotaPuntoDTO(
                LocalDateTime.of(2026, 11, 16, 10, 0), new BigDecimal("1000"), new BigDecimal("1025"), new BigDecimal("2.5"))));

        mvc.perform(get("/analitica/fondos/5/valor-cuota/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valorNuevo").value(1025));
    }

    @Test
    void idNoNumericoEs400() throws Exception {
        mvc.perform(get("/analitica/fondos/abc/valor-cuota/historial"))
                .andExpect(status().isBadRequest());
    }
}
