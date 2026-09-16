package com.bancocloud.usuarios_service.controller;

import com.bancocloud.usuarios_service.dto.*;
import com.bancocloud.usuarios_service.model.Rol;
import com.bancocloud.usuarios_service.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    
    /**
     * POST /api/v1/usuarios
     * Crear un nuevo usuario
     */
    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequestDTO request) {
        log.info("Solicitud para crear nuevo usuario: {}", request.getEmail());
        UsuarioDTO usuarioCreado = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCreado);
    }
    
    /**
     * GET /api/v1/usuarios
     * Obtener todos los usuarios
     */
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodosUsuarios() {
        log.info("Solicitud para obtener todos los usuarios");
        List<UsuarioDTO> usuarios = usuarioService.obtenerTodosUsuarios();
        return ResponseEntity.ok(usuarios);
    }
    
    /**
     * GET /api/v1/usuarios/por-rol
     * Obtener usuarios por rol
     */
    @GetMapping("/por-rol")
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuariosPorRol(
            @RequestParam Rol rol) {
        log.info("Solicitud para obtener usuarios con rol: {}", rol);
        List<UsuarioDTO> usuarios = usuarioService.obtenerUsuariosPorRol(rol);
        return ResponseEntity.ok(usuarios);
    }
    
    /**
     * GET /api/v1/usuarios/pendientes-autorizacion
     * Obtener clientes pendientes de autorización
     */
    @GetMapping("/pendientes-autorizacion")
    public ResponseEntity<List<UsuarioDTO>> obtenerClientesPendientesAutorizacion() {
        log.info("Solicitud para obtener clientes pendientes de autorización");
        List<UsuarioDTO> usuarios = usuarioService.obtenerClientesPendientesAutorizacion();
        return ResponseEntity.ok(usuarios);
    }
    
    /**
     * GET /api/v1/usuarios/{id}
     * Obtener un usuario por id
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuario(@PathVariable Long id) {
        log.info("Solicitud para obtener usuario con id: {}", id);
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorId(id);
        return ResponseEntity.ok(usuario);
    }
    
    /**
     * GET /api/v1/usuarios/por-email
     * Obtener un usuario por email
     */
    @GetMapping("/por-email")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorEmail(
            @RequestParam String email) {
        log.info("Solicitud para obtener usuario con email: {}", email);
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorEmail(email);
        return ResponseEntity.ok(usuario);
    }
    
    /**
     * PUT /api/v1/usuarios/{id}/autorizar
     * Autorizar acceso a un usuario
     */
    @PutMapping("/{id}/autorizar")
    public ResponseEntity<UsuarioDTO> autorizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody AutorizarUsuarioRequestDTO request) {
        log.info("Solicitud para autorizar usuario id: {}", id);
        UsuarioDTO usuarioActualizado = usuarioService.autorizarUsuario(id, request.getAutorizado());
        return ResponseEntity.ok(usuarioActualizado);
    }
    
    /**
     * PUT /api/v1/usuarios/{id}/bloquear
     * Bloquear un usuario
     */
    @PutMapping("/{id}/bloquear")
    public ResponseEntity<UsuarioDTO> bloquearUsuario(@PathVariable Long id) {
        log.info("Solicitud para bloquear usuario id: {}", id);
        UsuarioDTO usuarioActualizado = usuarioService.bloquearUsuario(id);
        return ResponseEntity.ok(usuarioActualizado);
    }
    
    /**
     * PUT /api/v1/usuarios/{id}/desbloquear
     * Desbloquear un usuario
     */
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<UsuarioDTO> desbloquearUsuario(@PathVariable Long id) {
        log.info("Solicitud para desbloquear usuario id: {}", id);
        UsuarioDTO usuarioActualizado = usuarioService.desbloquearUsuario(id);
        return ResponseEntity.ok(usuarioActualizado);
    }
    
    /**
     * GET /api/v1/usuarios/{id}/verificar-acceso
     * Verificar si un usuario tiene acceso autorizado
     */
    @GetMapping("/{id}/verificar-acceso")
    public ResponseEntity<Boolean> verificarAcceso(@PathVariable Long id) {
        log.info("Solicitud para verificar acceso del usuario id: {}", id);
        Boolean accesoAutorizado = usuarioService.verificarAcceso(id);
        return ResponseEntity.ok(accesoAutorizado);
    }
}
