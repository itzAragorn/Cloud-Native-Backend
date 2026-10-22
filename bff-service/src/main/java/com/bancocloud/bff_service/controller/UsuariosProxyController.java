package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.UsuariosServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuariosProxyController {

    private final UsuariosServiceClient usuariosServiceClient;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerMiPerfil(
            JwtAuthenticationToken authentication) {

        String cognitoSub =
                authentication.getToken().getSubject();

        if (cognitoSub == null || cognitoSub.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> usuario =
                usuariosServiceClient
                        .obtenerUsuarioPorCognitoSub(cognitoSub);

        if (usuario == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "USER_NOT_REGISTERED",
                            "message",
                            "El usuario de Cognito no está registrado en BancoCloud"
                    ));
        }

        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrarUsuario(
            @RequestBody Map<String, Object> request,
            JwtAuthenticationToken authentication) {

        String cognitoSub =
                authentication.getToken().getSubject();

        if (cognitoSub == null || cognitoSub.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "COGNITO_SUB_NOT_FOUND",
                            "message",
                            "No se pudo identificar al usuario de Cognito"
                    ));
        }

        Map<String, Object> usuarioExistente =
                usuariosServiceClient
                        .obtenerUsuarioPorCognitoSub(cognitoSub);

        if (usuarioExistente != null) {
            return ResponseEntity.ok(usuarioExistente);
        }

        String nombre =
                request.get("nombre") != null
                        ? request.get("nombre").toString().trim()
                        : "";

        String apellido =
                request.get("apellido") != null
                        ? request.get("apellido").toString().trim()
                        : "";

        String email =
                request.get("email") != null
                        ? request.get("email")
                                .toString()
                                .trim()
                                .toLowerCase()
                        : "";

        if (nombre.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "NAME_REQUIRED",
                            "message",
                            "El nombre es requerido"
                    ));
        }

        if (email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "EMAIL_REQUIRED",
                            "message",
                            "El email es requerido"
                    ));
        }

        Map<String, Object> nuevoUsuario =
                new HashMap<>();

        nuevoUsuario.put("nombre", nombre);
        nuevoUsuario.put("apellido", apellido);
        nuevoUsuario.put("email", email);
        nuevoUsuario.put("cognitoSub", cognitoSub);

        Map<String, Object> usuarioCreado =
                usuariosServiceClient
                        .crearUsuario(nuevoUsuario);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioCreado);
    }

    // =========================
    // ADMINISTRACIÓN
    // =========================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerUsuarios() {

        return ResponseEntity.ok(
                usuariosServiceClient.obtenerTodosUsuarios()
        );
    }

    @PutMapping("/{id}/autorizar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> autorizarUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        return ResponseEntity.ok(
                usuariosServiceClient
                        .autorizarUsuario(id, request)
        );
    }

    @PutMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bloquearUsuario(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                usuariosServiceClient
                        .bloquearUsuario(id)
        );
    }

    @PutMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> desbloquearUsuario(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                usuariosServiceClient
                        .desbloquearUsuario(id)
        );
    }
}