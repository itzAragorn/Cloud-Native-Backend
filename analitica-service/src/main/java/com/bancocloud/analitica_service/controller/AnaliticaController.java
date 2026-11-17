package com.bancocloud.analitica_service.controller;

import com.bancocloud.analitica_service.dto.InversionDiariaDTO;
import com.bancocloud.analitica_service.dto.ResumenFondoDTO;
import com.bancocloud.analitica_service.dto.ValorCuotaPuntoDTO;
import com.bancocloud.analitica_service.service.ConsultaAnaliticaService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura de la analítica. Expuesto por el BFF en {@code /api/v1/analitica/**}.
 */
@Validated
@RestController
@RequestMapping("/analitica/fondos")
@RequiredArgsConstructor
public class AnaliticaController {

    private final ConsultaAnaliticaService service;

    @GetMapping("/resumen")
    public List<ResumenFondoDTO> resumen() {
        return service.resumenFondos();
    }

    @GetMapping("/{id}/valor-cuota/historial")
    public List<ValorCuotaPuntoDTO> historialValorCuota(@PathVariable @Positive Long id) {
        return service.historialValorCuota(id);
    }

    @GetMapping("/{id}/inversiones/diario")
    public List<InversionDiariaDTO> inversionesDiarias(@PathVariable @Positive Long id) {
        return service.inversionesDiarias(id);
    }
}
