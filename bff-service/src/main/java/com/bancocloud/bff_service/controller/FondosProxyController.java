package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.FondosServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/fondos")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class FondosProxyController {

    private final FondosServiceClient fondosServiceClient;

    public FondosProxyController(FondosServiceClient fondosServiceClient) {
        this.fondosServiceClient = fondosServiceClient;
    }

    /**
     * Crear un nuevo fondo
     * Solo ADMIN puede crear fondos
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> crearFondo(
            @RequestBody Map<String, Object> fondoRequest) {
        log.info("BFF: POST /api/v1/fondos - Crear fondo");
        Map<String, Object> response = fondosServiceClient.crearFondo(fondoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener todos los fondos
     * Público (requiere autenticación)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodosFondos() {
        log.info("BFF: GET /api/v1/fondos - Obtener todos los fondos");
        List<Map<String, Object>> response = fondosServiceClient.obtenerTodosFondos();
        return ResponseEntity.ok(response);
    }
    /**
     * Obtener fondos activos
     * Público (requiere autenticación)
     */
    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerFondosActivos() {
        log.info("BFF: GET /api/v1/fondos/activos - Obtener fondos activos");
        List<Map<String, Object>> response = fondosServiceClient.obtenerFondosActivos();
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener un fondo por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerFondoPorId(
            @PathVariable Long id) {
        log.info("BFF: GET /api/v1/fondos/{} - Obtener fondo por ID", id);
        Map<String, Object> response = fondosServiceClient.obtenerFondoPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener valor de cuota de un fondo
     */
    @GetMapping("/{id}/valor-cuota")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerValorCuota(
            @PathVariable Long id) {
        log.info("BFF: GET /api/v1/fondos/{}/valor-cuota - Obtener valor de cuota", id);
        Map<String, Object> response = fondosServiceClient.obtenerValorCuota(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualizar valor de cuota de un fondo
     * Solo ADMIN puede actualizar
     */
    @PutMapping("/{id}/valor-cuota")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> actualizarValorCuota(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        log.info("BFF: PUT /api/v1/fondos/{}/valor-cuota - Actualizar valor de cuota", id);
        Map<String, Object> response = fondosServiceClient.actualizarValorCuota(id, request);
        return ResponseEntity.ok(response);
    }
}
