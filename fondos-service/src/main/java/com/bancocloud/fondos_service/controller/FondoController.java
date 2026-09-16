package com.bancocloud.fondos_service.controller;

import com.bancocloud.fondos_service.dto.*;
import com.bancocloud.fondos_service.model.EstadoFondo;
import com.bancocloud.fondos_service.service.FondoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fondos")
@RequiredArgsConstructor
@Slf4j
public class FondoController {
    
    private final FondoService fondoService;
    
    /**
     * POST /api/v1/fondos
     * Crear un nuevo fondo
     */
    @PostMapping
    public ResponseEntity<FondoDTO> crearFondo(@Valid @RequestBody FondoRequestDTO request) {
        log.info("Solicitud para crear nuevo fondo: {}", request.getNombre());
        FondoDTO fondoCreado = fondoService.crearFondo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(fondoCreado);
    }
    
    /**
     * GET /api/v1/fondos
     * Obtener todos los fondos
     */
    @GetMapping
    public ResponseEntity<List<FondoDTO>> obtenerTodosFondos() {
        log.info("Solicitud para obtener todos los fondos");
        List<FondoDTO> fondos = fondoService.obtenerTodosFondos();
        return ResponseEntity.ok(fondos);
    }
    
    /**
     * GET /api/v1/fondos/activos
     * Obtener solo los fondos activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<FondoDTO>> obtenerFondosActivos() {
        log.info("Solicitud para obtener fondos activos");
        List<FondoDTO> fondos = fondoService.obtenerFondosActivos();
        return ResponseEntity.ok(fondos);
    }
    
    /**
     * GET /api/v1/fondos/{id}
     * Obtener un fondo por id
     */
    @GetMapping("/{id}")
    public ResponseEntity<FondoDTO> obtenerFondo(@PathVariable Long id) {
        log.info("Solicitud para obtener fondo con id: {}", id);
        FondoDTO fondo = fondoService.obtenerFondoPorId(id);
        return ResponseEntity.ok(fondo);
    }
    
    /**
     * PUT /api/v1/fondos/{id}/valor-cuota
     * Actualizar el valor de la cuota
     */
    @PutMapping("/{id}/valor-cuota")
    public ResponseEntity<FondoDTO> actualizarValorCuota(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarValorCuotaRequestDTO request) {
        log.info("Solicitud para actualizar valor de cuota del fondo id: {}", id);
        FondoDTO fondoActualizado = fondoService.actualizarValorCuota(id, request);
        return ResponseEntity.ok(fondoActualizado);
    }
    
    /**
     * GET /api/v1/fondos/{id}/valor-cuota
     * Obtener el valor actual de la cuota
     */
    @GetMapping("/{id}/valor-cuota")
    public ResponseEntity<BigDecimal> obtenerValorCuota(@PathVariable Long id) {
        log.info("Solicitud para obtener valor de cuota del fondo id: {}", id);
        BigDecimal valorCuota = fondoService.obtenerValorCuota(id);
        return ResponseEntity.ok(valorCuota);
    }
    
    /**
     * PATCH /api/v1/fondos/{id}/estado
     * Cambiar el estado de un fondo
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<FondoDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoFondo nuevoEstado) {
        log.info("Solicitud para cambiar estado del fondo id: {} a: {}", id, nuevoEstado);
        FondoDTO fondoActualizado = fondoService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(fondoActualizado);
    }
}
