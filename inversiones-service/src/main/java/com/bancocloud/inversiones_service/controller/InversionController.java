package com.bancocloud.inversiones_service.controller;

import com.bancocloud.inversiones_service.dto.*;
import com.bancocloud.inversiones_service.service.InversionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inversiones")
@RequiredArgsConstructor
@Slf4j
public class InversionController {
    
    private final InversionService inversionService;
    
    /**
     * POST /api/v1/inversiones
     * Crear una nueva inversión
     */
    @PostMapping
    public ResponseEntity<InversionDTO> crearInversion(
            @Valid @RequestBody CrearInversionRequestDTO request) {
        log.info("Solicitud para crear inversión");
        InversionDTO inversionCreada = inversionService.crearInversion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inversionCreada);
    }
    
    /**
     * GET /api/v1/inversiones/mis-inversiones
     * Obtener todas las inversiones del usuario actual
     */
    @GetMapping("/mis-inversiones")
    public ResponseEntity<List<InversionDTO>> obtenerMisInversiones(
            @RequestParam Long usuarioId) {
        log.info("Solicitud para obtener inversiones del usuario: {}", usuarioId);
        List<InversionDTO> inversiones = inversionService.obtenerMisInversiones(usuarioId);
        return ResponseEntity.ok(inversiones);
    }
    
    /**
     * GET /api/v1/inversiones/{id}
     * Obtener una inversión específica por id
     */
    @GetMapping("/{id}")
    public ResponseEntity<InversionDTO> obtenerInversion(@PathVariable Long id) {
        log.info("Solicitud para obtener inversión con id: {}", id);
        InversionDTO inversion = inversionService.obtenerInversion(id);
        return ResponseEntity.ok(inversion);
    }
    
    /**
     * GET /api/v1/inversiones/mi-portafolio
     * Obtener el portafolio completo del usuario
     */
    @GetMapping("/mi-portafolio")
    public ResponseEntity<PortafolioDTO> obtenerMiPortafolio(
            @RequestParam Long usuarioId) {
        log.info("Solicitud para obtener portafolio del usuario: {}", usuarioId);
        PortafolioDTO portafolio = inversionService.obtenerMiPortafolio(usuarioId);
        return ResponseEntity.ok(portafolio);
    }
    
    /**
     * GET /api/v1/inversiones/{id}/rendimiento
     * Obtener rendimiento de una inversión específica
     */
    @GetMapping("/{id}/rendimiento")
    public ResponseEntity<RendimientoDTO> obtenerRendimientoInversion(@PathVariable Long id) {
        log.info("Solicitud para obtener rendimiento de inversión: {}", id);
        RendimientoDTO rendimiento = inversionService.obtenerRendimientoInversion(id);
        return ResponseEntity.ok(rendimiento);
    }
    
    /**
     * GET /api/v1/inversiones/rendimiento
     * Obtener rendimiento de todas las inversiones del usuario
     */
    @GetMapping("/rendimiento")
    public ResponseEntity<List<RendimientoDTO>> obtenerRendimientoUsuario(
            @RequestParam Long usuarioId) {
        log.info("Solicitud para obtener rendimiento del usuario: {}", usuarioId);
        List<RendimientoDTO> rendimientos = inversionService.obtenerRendimientoUsuario(usuarioId);
        return ResponseEntity.ok(rendimientos);
    }
}
