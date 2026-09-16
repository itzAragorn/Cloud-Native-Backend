package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.InversionesServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/inversiones")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class InversionesProxyController {

    private final InversionesServiceClient inversionesServiceClient;

    public InversionesProxyController(InversionesServiceClient inversionesServiceClient) {
        this.inversionesServiceClient = inversionesServiceClient;
    }

    /**
     * Crear una nueva inversión
     * Solo CLIENTE y ADMIN autorizados pueden crear inversiones
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> crearInversion(
            @RequestBody Map<String, Object> inversionRequest) {
        log.info("BFF: POST /api/v1/inversiones - Crear inversión");
        Map<String, Object> response = inversionesServiceClient.crearInversion(inversionRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener mis inversiones
     * El usuario debe ser CLIENTE o ADMIN
     */
    @GetMapping("/mis-inversiones")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerMisInversiones(
            @RequestParam Long usuarioId) {
        log.info("BFF: GET /api/v1/inversiones/mis-inversiones - Obtener mis inversiones del usuario {}", usuarioId);
        List<Map<String, Object>> response = inversionesServiceClient.obtenerMisInversiones(usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener mi portafolio completo
     */
    @GetMapping("/mi-portafolio")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerMiPortafolio(
            @RequestParam Long usuarioId) {
        log.info("BFF: GET /api/v1/inversiones/mi-portafolio - Obtener portafolio del usuario {}", usuarioId);
        Map<String, Object> response = inversionesServiceClient.obtenerMiPortafolio(usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener una inversión específica
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerInversion(
            @PathVariable Long id) {
        log.info("BFF: GET /api/v1/inversiones/{} - Obtener inversión por ID", id);
        Map<String, Object> response = inversionesServiceClient.obtenerInversion(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener el rendimiento de una inversión específica
     */
    @GetMapping("/{id}/rendimiento")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerRendimientoInversion(
            @PathVariable Long id) {
        log.info("BFF: GET /api/v1/inversiones/{}/rendimiento - Obtener rendimiento de inversión", id);
        Map<String, Object> response = inversionesServiceClient.obtenerRendimientoInversion(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener el rendimiento total del usuario
     */
    @GetMapping("/rendimiento")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerRendimientoUsuario(
            @RequestParam Long usuarioId) {
        log.info("BFF: GET /api/v1/inversiones/rendimiento - Obtener rendimiento total del usuario {}", usuarioId);
        List<Map<String, Object>> response = inversionesServiceClient.obtenerRendimientoUsuario(usuarioId);
        return ResponseEntity.ok(response);
    }
}
