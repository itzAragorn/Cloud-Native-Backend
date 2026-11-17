package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.AnaliticaServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analítica armada desde los tópicos de Kafka.
 * <ul>
 *   <li>Resumen y montos diarios: totales de todos los clientes, solo ADMIN.</li>
 *   <li>Historial del valor cuota: información pública del fondo, CLIENTE o ADMIN.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/analitica/fondos")
@RequiredArgsConstructor
public class AnaliticaProxyController {

    private final AnaliticaServiceClient client;

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> resumen() {
        return client.reenviar(HttpMethod.GET, "/analitica/fondos/resumen", null);
    }

    @GetMapping("/{id}/inversiones/diario")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> inversionesDiarias(@PathVariable Long id) {
        return client.reenviar(HttpMethod.GET, "/analitica/fondos/{id}/inversiones/diario", null, id);
    }

    @GetMapping("/{id}/valor-cuota/historial")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> historialValorCuota(@PathVariable Long id) {
        return client.reenviar(HttpMethod.GET, "/analitica/fondos/{id}/valor-cuota/historial", null, id);
    }
}
