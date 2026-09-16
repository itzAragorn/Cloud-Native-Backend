package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.UsuariosServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/usuarios")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class UsuariosProxyController {

    private final UsuariosServiceClient usuariosServiceClient;

    public UsuariosProxyController(UsuariosServiceClient usuariosServiceClient) {
        this.usuariosServiceClient = usuariosServiceClient;
    }

    // El passwordHash solo debe viajar entre bff-service y usuarios-service
    // para validar credenciales en el login; nunca debe llegar al cliente.
    private Map<String, Object> sinPassword(Map<String, Object> usuario) {
        if (usuario != null) {
            usuario.remove("passwordHash");
        }
        return usuario;
    }

    private List<Map<String, Object>> sinPassword(List<Map<String, Object>> usuarios) {
        if (usuarios != null) {
            usuarios.forEach(this::sinPassword);
        }
        return usuarios;
    }

    /**
     * Crear un nuevo usuario
     * Sin restricción de rol (pueda crearse nuevo usuario)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearUsuario(
            @RequestBody Map<String, Object> usuarioRequest) {
        log.info("BFF: POST /api/v1/usuarios - Crear usuario");
        Map<String, Object> response = usuariosServiceClient.crearUsuario(usuarioRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(sinPassword(response));
    }

    /**
     * Obtener todos los usuarios
     * Solo ADMIN
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodosUsuarios() {
        log.info("BFF: GET /api/v1/usuarios - Obtener todos los usuarios");
        List<Map<String, Object>> response = usuariosServiceClient.obtenerTodosUsuarios();
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Obtener usuarios por rol
     * Solo ADMIN
     */
    @GetMapping("/por-rol")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerUsuariosPorRol(
            @RequestParam String rol) {
        log.info("BFF: GET /api/v1/usuarios/por-rol - Obtener usuarios por rol: {}", rol);
        List<Map<String, Object>> response = usuariosServiceClient.obtenerUsuariosPorRol(rol);
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Obtener usuarios pendientes de autorización
     * Solo ADMIN
     */
    @GetMapping("/pendientes-autorizacion")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerClientesPendientesAutorizacion() {
        log.info("BFF: GET /api/v1/usuarios/pendientes-autorizacion - Obtener pendientes");
        List<Map<String, Object>> response = usuariosServiceClient.obtenerClientesPendientesAutorizacion();
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Obtener un usuario por ID
     * El usuario puede obtener su propio perfil, ADMIN puede obtener cualquiera
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioPorId(
            @PathVariable Long id) {
        log.info("BFF: GET /api/v1/usuarios/{} - Obtener usuario por ID", id);
        Map<String, Object> response = usuariosServiceClient.obtenerUsuarioPorId(id);
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Obtener un usuario por email
     * Solo ADMIN
     */
    @GetMapping("/por-email")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioPorEmail(
            @RequestParam String email) {
        log.info("BFF: GET /api/v1/usuarios/por-email - Obtener usuario por email");
        Map<String, Object> response = usuariosServiceClient.obtenerUsuarioPorEmail(email);
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Autorizar un usuario
     * Solo ADMIN
     */
    @PutMapping("/{id}/autorizar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> autorizarUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        log.info("BFF: PUT /api/v1/usuarios/{}/autorizar - Autorizar usuario", id);
        Map<String, Object> response = usuariosServiceClient.autorizarUsuario(id, request);
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Bloquear un usuario
     * Solo ADMIN
     */
    @PutMapping("/{id}/bloquear")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> bloquearUsuario(
            @PathVariable Long id) {
        log.info("BFF: PUT /api/v1/usuarios/{}/bloquear - Bloquear usuario", id);
        Map<String, Object> response = usuariosServiceClient.bloquearUsuario(id);
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Desbloquear un usuario
     * Solo ADMIN
     */
    @PutMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> desbloquearUsuario(
            @PathVariable Long id) {
        log.info("BFF: PUT /api/v1/usuarios/{}/desbloquear - Desbloquear usuario", id);
        Map<String, Object> response = usuariosServiceClient.desbloquearUsuario(id);
        return ResponseEntity.ok(sinPassword(response));
    }

    /**
     * Verificar acceso de un usuario
     */
    @GetMapping("/{id}/verificar-acceso")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> verificarAcceso(
            @PathVariable Long id) {
        log.info("BFF: GET /api/v1/usuarios/{}/verificar-acceso - Verificar acceso", id);
        Map<String, Object> response = usuariosServiceClient.verificarAcceso(id);
        return ResponseEntity.ok(response);
    }
}