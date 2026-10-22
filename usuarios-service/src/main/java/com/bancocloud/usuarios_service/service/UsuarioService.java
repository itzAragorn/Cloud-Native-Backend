package com.bancocloud.usuarios_service.service;

import com.bancocloud.usuarios_service.dto.*;
import com.bancocloud.usuarios_service.model.*;
import com.bancocloud.usuarios_service.repository.UsuarioRepository;
import com.bancocloud.usuarios_service.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Crear un nuevo usuario BancoCloud asociado a Cognito.
     */
    @Transactional
    public UsuarioDTO crearUsuario(CrearUsuarioRequestDTO request) {

        log.info(
                "Creando usuario BancoCloud para Cognito: {}",
                request.getEmail()
        );

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicadoException(request.getEmail());
        }

        if (usuarioRepository.existsByCognitoSub(request.getCognitoSub())) {
            throw new IllegalStateException(
                    "Ya existe un usuario BancoCloud asociado a este usuario de Cognito"
            );
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .cognitoSub(request.getCognitoSub())
                .rol(Rol.CLIENTE)
                .accesoAutorizado(true)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        Usuario usuarioGuardado =
                usuarioRepository.save(usuario);

        log.info(
                "Usuario BancoCloud creado exitosamente. ID: {}, Cognito Sub: {}",
                usuarioGuardado.getId(),
                usuarioGuardado.getCognitoSub()
        );

        return mapToDTO(usuarioGuardado);
    }

    /**
     * Obtener todos los usuarios.
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
     * Obtener usuarios por rol.
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerUsuariosPorRol(Rol rol) {

        log.info(
                "Obteniendo usuarios con rol: {}",
                rol
        );

        return usuarioRepository.findByRol(rol)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Obtener clientes pendientes de autorización.
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerClientesPendientesAutorizacion() {

        log.info(
                "Obteniendo clientes pendientes de autorización"
        );

        return usuarioRepository
                .findByRolAndAccesoAutorizado(
                        Rol.CLIENTE,
                        false
                )
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Obtener un usuario por ID.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(Long id) {

        log.info(
                "Obteniendo usuario con id: {}",
                id
        );

        Usuario usuario =
                usuarioRepository.findById(id)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(id)
                        );

        return mapToDTO(usuario);
    }

    /**
     * Obtener un usuario por email.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorEmail(String email) {

        log.info(
                "Obteniendo usuario con email: {}",
                email
        );

        Usuario usuario =
                usuarioRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(
                                        "Usuario no encontrado con email: "
                                                + email
                                )
                        );

        return mapToDTO(usuario);
    }

    /**
     * Obtener un usuario por Cognito Sub.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorCognitoSub(
            String cognitoSub) {

        log.info(
                "Obteniendo usuario BancoCloud por Cognito Sub"
        );

        Usuario usuario =
                usuarioRepository.findByCognitoSub(cognitoSub)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(
                                        "Usuario no encontrado para Cognito Sub"
                                )
                        );

        return mapToDTO(usuario);
    }

    /**
     * Autorizar o desautorizar acceso a un usuario.
     */
    @Transactional
    public UsuarioDTO autorizarUsuario(
            Long id,
            Boolean autorizado) {

        log.info(
                "Autorizando usuario id: {} con valor: {}",
                id,
                autorizado
        );

        Usuario usuario =
                usuarioRepository.findById(id)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(id)
                        );

        Boolean estadoAnterior =
                usuario.getAccesoAutorizado();

        usuario.setAccesoAutorizado(autorizado);

        Usuario usuarioActualizado =
                usuarioRepository.save(usuario);

        log.info(
                "Usuario autorizado. Estado anterior: {}, Nuevo estado: {}",
                estadoAnterior,
                autorizado
        );

        return mapToDTO(usuarioActualizado);
    }

    /**
     * Bloquear un usuario.
     *
     * Cambia el estado a BLOQUEADO y revoca
     * el acceso al sistema.
     */
    @Transactional
    public UsuarioDTO bloquearUsuario(Long id) {

        log.info(
                "Bloqueando usuario id: {}",
                id
        );

        Usuario usuario =
                usuarioRepository.findById(id)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(id)
                        );

        EstadoUsuario estadoAnterior =
                usuario.getEstado();

        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuario.setAccesoAutorizado(false);

        Usuario usuarioActualizado =
                usuarioRepository.save(usuario);

        log.info(
                "Usuario bloqueado. Estado anterior: {}, Nuevo estado: BLOQUEADO",
                estadoAnterior
        );

        return mapToDTO(usuarioActualizado);
    }

    /**
     * Desbloquear un usuario.
     *
     * Devuelve el usuario a estado ACTIVO
     * y restaura su acceso al sistema.
     */
    @Transactional
    public UsuarioDTO desbloquearUsuario(Long id) {

        log.info(
                "Desbloqueando usuario id: {}",
                id
        );

        Usuario usuario =
                usuarioRepository.findById(id)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(id)
                        );

        EstadoUsuario estadoAnterior =
                usuario.getEstado();

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setAccesoAutorizado(true);

        Usuario usuarioActualizado =
                usuarioRepository.save(usuario);

        log.info(
                "Usuario desbloqueado. Estado anterior: {}, Nuevo estado: ACTIVO",
                estadoAnterior
        );

        return mapToDTO(usuarioActualizado);
    }

    /**
     * Verificar si un usuario tiene acceso autorizado.
     *
     * El usuario debe estar autorizado y además
     * encontrarse en estado ACTIVO.
     */
    @Transactional(readOnly = true)
    public Boolean verificarAcceso(Long usuarioId) {

        log.info(
                "Verificando acceso para usuario id: {}",
                usuarioId
        );

        Usuario usuario =
                usuarioRepository.findById(usuarioId)
                        .orElseThrow(
                                () -> new UsuarioNotFoundException(usuarioId)
                        );

        boolean accesoAutorizado =
                Boolean.TRUE.equals(usuario.getAccesoAutorizado())
                        && usuario.getEstado() == EstadoUsuario.ACTIVO;

        log.info(
                "Acceso para usuario id: {} es: {}",
                usuarioId,
                accesoAutorizado
        );

        return accesoAutorizado;
    }

    /**
     * Convertir Entity a DTO.
     *
     * Nunca se expone información de autenticación
     * ni credenciales.
     */
    private UsuarioDTO mapToDTO(Usuario usuario) {

        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .accesoAutorizado(usuario.getAccesoAutorizado())
                .estado(usuario.getEstado())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}