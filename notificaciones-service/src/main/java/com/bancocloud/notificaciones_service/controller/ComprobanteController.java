package com.bancocloud.notificaciones_service.controller;

import com.bancocloud.notificaciones_service.consumer.inversiones.Comprobante;
import com.bancocloud.notificaciones_service.consumer.inversiones.ComprobanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de comprobantes de inversión. Solo es accesible dentro de la red interna;
 * el BFF la expone en {@code /api/v1/comprobantes} y controla qué usuario puede ver qué.
 */
@RestController
@RequestMapping("/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    @GetMapping
    public List<Comprobante> listarPorUsuario(@RequestParam Long usuarioId) {
        return comprobanteService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/inversion/{inversionId}")
    public ResponseEntity<Comprobante> buscarPorInversion(@PathVariable Long inversionId) {
        return ResponseEntity.of(comprobanteService.buscarPorInversion(inversionId));
    }
}
