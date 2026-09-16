package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.dto.LoginRequestDTO;
import com.bancocloud.bff_service.dto.LoginResponseDTO;
import com.bancocloud.bff_service.dto.RegistroRequestDTO;
import com.bancocloud.bff_service.exception.AuthenticationException;
import com.bancocloud.bff_service.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controlador de Autenticación
 * - Endpoint público para login
 * - Genera JWT token para cliente autenticado
 * - Valida credenciales contra usuarios-service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Endpoint de login
     * POST /api/v1/auth/login
     * 
     * Request:
     * {
     *   "email": "usuario@example.com",
     *   "password": "password123"
     * }
     * 
     * Response 200:
     * {
     *   "usuarioId": 1,
     *   "email": "usuario@example.com",
     *   "rol": "CLIENTE",
     *   "token": "eyJhbGciOiJIUzUxMiJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400,
     *   "mensaje": "Login exitoso"
     * }
     * 
     * Response 401:
     * {
     *   "timestamp": "2024-01-15T10:30:00",
     *   "status": 401,
     *   "error": "Unauthorized",
     *   "message": "Email o contraseña incorrectos",
     *   "path": "/api/v1/auth/login"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        log.info("Solicitud de login para: {}", loginRequest.getEmail());

        try {
            LoginResponseDTO response = authenticationService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            log.warn("Login fallido para {}: {}", loginRequest.getEmail(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Endpoint de verificación de token
     * GET /api/v1/auth/verify
     * 
     * Headers:
     * Authorization: Bearer <token>
     * 
     * Response 200:
     * {
     *   "válido": true,
     *   "mensaje": "Token válido"
     * }
     * 
     * Response 401:
     * {
     *   "válido": false,
     *   "mensaje": "Token inválido o expirado"
     * }
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken() {
        // Si llegamos aquí, el filtro JWT pasó, entonces el token es válido
        return ResponseEntity.ok("{\"válido\": true, \"mensaje\": \"Token válido\"}");
    }

    /**
     * Endpoint de salida (logout)
     * GET /api/v1/auth/logout
     * 
     * Response 200:
     * {
     *   "mensaje": "Logout exitoso"
     * }
     */
    @GetMapping("/logout")
    public ResponseEntity<String> logout() {
        log.info("Logout exitoso");
        return ResponseEntity.ok("{\"mensaje\": \"Logout exitoso\"}");
    }

    /**
     * Endpoint de registro
     * POST /api/v1/auth/registro
     * 
     * Request:
     * {
     *   "nombre": "Juan",
     *   "apellido": "Pérez",
     *   "email": "juan@example.com",
     *   "password": "password123",
     *   "passwordConfirm": "password123"
     * }
     * 
     * Response 200:
     * {
     *   "usuarioId": 2,
     *   "email": "juan@example.com",
     *   "rol": "CLIENTE",
     *   "token": "eyJhbGciOiJIUzUxMiJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400,
     *   "mensaje": "Registro exitoso. Bienvenido!"
     * }
     */
    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDTO> registro(@Valid @RequestBody RegistroRequestDTO registroRequest) {
        log.info("Solicitud de registro para: {}", registroRequest.getEmail());

        try {
            LoginResponseDTO response = authenticationService.registro(registroRequest);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            log.warn("Registro fallido para {}: {}", registroRequest.getEmail(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Endpoint de demo login - SOLO PARA DESARROLLO/PRUEBA
     * POST /api/v1/auth/demo-user
     * Loguea automáticamente con el usuario cliente2 para pruebas rápidas
     * Password: test123
     */
    @PostMapping("/demo-user")
    public ResponseEntity<LoginResponseDTO> crearUsuarioDemo() {
        log.info("Demo login solicitado");
        
        LoginRequestDTO demoRequest = new LoginRequestDTO();
        demoRequest.setEmail("cliente2@bancocloud.com");
        demoRequest.setPassword("test123");
        
        try {
            LoginResponseDTO response = authenticationService.authenticate(demoRequest);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.warn("Demo login fallido: {}", ex.getMessage());
            throw new AuthenticationException("Error en demo login: " + ex.getMessage());
        }
    }
}


