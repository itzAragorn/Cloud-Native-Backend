package com.bancocloud.usuarios_service.service;

import com.bancocloud.usuarios_service.dto.*;
import com.bancocloud.usuarios_service.model.*;
import com.bancocloud.usuarios_service.repository.UsuarioRepository;
import com.bancocloud.usuarios_service.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Crear un nuevo usuario
     */
    @Transactional
    public UsuarioDTO crearUsuario(CrearUsuarioRequestDTO request) {
        log.info("Creando nuevo usuario: {} ({})", request.getNombre(), request.getEmail());
        
        // Validar que no exista un usuario con el mismo email
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicadoException(request.getEmail());
        }
        
        // Hashear el password
        String passwordHash = passwordEncoder.encode(request.getPassword());
        
        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .rol(request.getRol())
                .accesoAutorizado(true) // Los usuarios nuevos nacen autorizados
                .estado(EstadoUsuario.ACTIVO)
                .build();
        
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente con id: {}", usuarioGuardado.getId());
        
        return mapToDTO(usuarioGuardado);
    }
    
    /**
     * Obtener todos los usuarios
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerTodosUsuarios() {
        log.info("Obteniendo todos los usuarios");
        return usuarioRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener usuarios por rol
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerUsuariosPorRol(Rol rol) {
        log.info("Obteniendo usuarios con rol: {}", rol);
        return usuarioRepository.findByRol(rol)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener clientes pendientes de autorización
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerClientesPendientesAutorizacion() {
        log.info("Obteniendo clientes pendientes de autorización");
        return usuarioRepository.findByRolAndAccesoAutorizado(Rol.CLIENTE, false)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener un usuario por id
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        log.info("Obteniendo usuario con id: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
        return mapToDTO(usuario);
    }
    
    /**
     * Obtener un usuario por email
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorEmail(String email) {
        log.info("Obteniendo usuario con email: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNotFoundException(
                        "Usuario no encontrado con email: " + email
                ));
        return mapToDTO(usuario);
    }
    
    /**
     * Autorizar acceso a un usuario
     * Solo el ADMIN puede autorizar a los CLIENTE
     */
    @Transactional
    public UsuarioDTO autorizarUsuario(Long id, Boolean autorizado) {
        log.info("Autorizando usuario id: {} con valor: {}", id, autorizado);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
        
        Boolean estadoAnterior = usuario.getAccesoAutorizado();
        usuario.setAccesoAutorizado(autorizado);
        
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Usuario autorizado. Estado anterior: {}, Nuevo estado: {}", 
                estadoAnterior, autorizado);
        
        return mapToDTO(usuarioActualizado);
    }
    
    /**
     * Bloquear un usuario
     * Cambiar estado a BLOQUEADO
     */
    @Transactional
    public UsuarioDTO bloquearUsuario(Long id) {
        log.info("Bloqueando usuario id: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
        
        EstadoUsuario estadoAnterior = usuario.getEstado();
        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuario.setAccesoAutorizado(false);
        
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Usuario bloqueado. Estado anterior: {}, Nuevo estado: BLOQUEADO", estadoAnterior);
        
        return mapToDTO(usuarioActualizado);
    }
    
    /**
     * Desbloquear un usuario
     */
    @Transactional
    public UsuarioDTO desbloquearUsuario(Long id) {
        log.info("Desbloqueando usuario id: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
        
        EstadoUsuario estadoAnterior = usuario.getEstado();
        usuario.setEstado(EstadoUsuario.ACTIVO);
        
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Usuario desbloqueado. Estado anterior: {}, Nuevo estado: ACTIVO", estadoAnterior);
        
        return mapToDTO(usuarioActualizado);
    }
    
    /**
     * Verificar si un usuario tiene acceso autorizado
     */
    @Transactional(readOnly = true)
    public Boolean verificarAcceso(Long usuarioId) {
        log.info("Verificando acceso para usuario id: {}", usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNotFoundException(usuarioId));
        
        boolean accesoAutorizado = usuario.getAccesoAutorizado() && 
                                   usuario.getEstado() == EstadoUsuario.ACTIVO;
        
        log.info("Acceso para usuario id: {} es: {}", usuarioId, accesoAutorizado);
        return accesoAutorizado;
    }
    
    // Método helper para mapear Entity a DTO
    private UsuarioDTO mapToDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .passwordHash(usuario.getPasswordHash())
                .rol(usuario.getRol())
                .accesoAutorizado(usuario.getAccesoAutorizado())
                .estado(usuario.getEstado())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
