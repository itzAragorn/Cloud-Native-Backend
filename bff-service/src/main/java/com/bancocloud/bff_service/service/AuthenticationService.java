package com.bancocloud.bff_service.service;

import com.bancocloud.bff_service.client.UsuariosServiceClient;
import com.bancocloud.bff_service.dto.LoginRequestDTO;
import com.bancocloud.bff_service.dto.LoginResponseDTO;
import com.bancocloud.bff_service.exception.AuthenticationException;
import com.bancocloud.bff_service.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Servicio de autenticación para generar JWT tokens
 * - Valida credenciales contra usuarios-service
 * - Genera JWT token para cliente
 * - Valida estado y acceso del usuario
 */
@Slf4j
@Service
public class AuthenticationService {

    private final UsuariosServiceClient usuariosServiceClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long jwtExpirationMs;

    public AuthenticationService(
            UsuariosServiceClient usuariosServiceClient,
            JwtTokenProvider jwtTokenProvider,
            BCryptPasswordEncoder passwordEncoder,
            @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.usuariosServiceClient = usuariosServiceClient;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Autentica un usuario y genera JWT token
     * - Valida credenciales contra usuarios-service
     * - Valida el password contra el hash almacenado
     * - Valida estado y acceso del usuario
     * 
     * @param loginRequest Credenciales del usuario
     * @return LoginResponseDTO con JWT token
     * @throws AuthenticationException Si las credenciales son inválidas
     */
    public LoginResponseDTO authenticate(LoginRequestDTO loginRequest) {
        log.info("Intentando autenticación para: {}", loginRequest.getEmail());

        try {
            // Buscar usuario por email en usuarios-service
            Map<String, Object> usuario = usuariosServiceClient.obtenerUsuarioPorEmail(loginRequest.getEmail());

            if (usuario == null) {
                log.warn("Usuario no encontrado: {}", loginRequest.getEmail());
                throw new AuthenticationException("Email o contraseña incorrectos");
            }

            // Validar el password contra el hash almacenado
            String passwordHash = (String) usuario.get("passwordHash");
            if (passwordHash == null || !passwordEncoder.matches(loginRequest.getPassword(), passwordHash)) {
                log.warn("Contraseña incorrecta para usuario: {}", loginRequest.getEmail());
                throw new AuthenticationException("Email o contraseña incorrectos");
            }

            // Validar que el usuario tenga acceso autorizado
            Boolean accesoAutorizado = (Boolean) usuario.get("accesoAutorizado");
            if (accesoAutorizado == null || !accesoAutorizado) {
                log.warn("Usuario {} sin acceso autorizado", loginRequest.getEmail());
                throw new AuthenticationException("Usuario no autorizado para acceder al sistema");
            }

            // Validar estado del usuario
            String estado = (String) usuario.get("estado");
            if (!"ACTIVO".equals(estado)) {
                log.warn("Usuario {} está en estado: {}", loginRequest.getEmail(), estado);
                throw new AuthenticationException("Usuario " + estado);
            }

            // Extraer información del usuario
            Long usuarioId = extractLongId(usuario.get("id"));
            String rol = (String) usuario.get("rol");

            // Generar JWT token con la API nueva de jjwt
            String token = jwtTokenProvider.generateToken(usuarioId, loginRequest.getEmail(), rol);

            log.info("Usuario {} autenticado exitosamente", loginRequest.getEmail());

            // Construir respuesta
            return LoginResponseDTO.builder()
                    .usuarioId(usuarioId)
                    .email(loginRequest.getEmail())
                    .rol(rol)
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs / 1000) // Convertir a segundos
                    .mensaje("Login exitoso")
                    .build();

        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error durante autenticación: {}", ex.getMessage(), ex);
            throw new AuthenticationException("Error durante autenticación. Por favor intente nuevamente.");
        }
    }

    /**
     * Registra un nuevo usuario
     * - Valida que el email no exista
     * - Hashea la contraseña con BCrypt
     * - Crea el usuario en usuarios-service
     * - Auto-login del usuario
     */
    public LoginResponseDTO registro(com.bancocloud.bff_service.dto.RegistroRequestDTO registroRequest) {
        log.info("Solicitud de registro para: {}", registroRequest.getEmail());

        try {
            // Validar que las contraseñas coincidan
            if (!registroRequest.getPassword().equals(registroRequest.getPasswordConfirm())) {
                throw new AuthenticationException("Las contraseñas no coinciden");
            }

            // Verificar que el email no exista
            try {
                Map<String, Object> usuarioExistente = usuariosServiceClient.obtenerUsuarioPorEmail(registroRequest.getEmail());
                if (usuarioExistente != null) {
                    throw new AuthenticationException("El email ya está registrado");
                }
            } catch (Exception ex) {
                // Si el usuario no existe, continuamos
                if (!ex.getMessage().contains("no encontrado")) {
                    throw ex;
                }
            }

            // Crear objeto de usuario para enviar al usuarios-service
            Map<String, Object> nuevoUsuario = new java.util.HashMap<>();
            nuevoUsuario.put("nombre", registroRequest.getNombre());
            nuevoUsuario.put("apellido", registroRequest.getApellido());
            nuevoUsuario.put("email", registroRequest.getEmail());
            // Enviar password sin hashear - usuarios-service lo hasheará
            nuevoUsuario.put("password", registroRequest.getPassword());
            nuevoUsuario.put("rol", "CLIENTE");

            // Crear usuario en usuarios-service
            Map<String, Object> usuarioCreado = usuariosServiceClient.crearUsuario(nuevoUsuario);
            
            log.info("Usuario {} registrado exitosamente", registroRequest.getEmail());

            // Auto-login del usuario
            Long usuarioId = extractLongId(usuarioCreado.get("id"));
            String rol = (String) usuarioCreado.get("rol");
            String token = jwtTokenProvider.generateToken(usuarioId, registroRequest.getEmail(), rol);

            return LoginResponseDTO.builder()
                    .usuarioId(usuarioId)
                    .email(registroRequest.getEmail())
                    .rol(rol)
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs / 1000)
                    .mensaje("Registro exitoso. Bienvenido!")
                    .build();

        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error durante registro: {}", ex.getMessage(), ex);
            throw new AuthenticationException("Error durante registro. Por favor intente nuevamente.");
        }
    }

    /**
     * Helper para extraer Long del ID (puede venir como Integer o Long)
     */
    private Long extractLongId(Object id) {
        if (id instanceof Integer) {
            return ((Integer) id).longValue();
        }
        return (Long) id;
    }
}
