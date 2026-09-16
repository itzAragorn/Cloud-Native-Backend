package com.bancocloud.usuarios_service.security;

import com.bancocloud.usuarios_service.model.Usuario;
import com.bancocloud.usuarios_service.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementación personalizada de UserDetailsService
 * Consulta la base de datos para autenticación de usuarios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UsuarioRepository usuarioRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Cargando detalles del usuario por email: {}", email);
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con email: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado: " + email);
                });
        
        // Validar que el usuario esté activo y autorizado
        if (!"ACTIVO".equals(usuario.getEstado().toString())) {
            log.warn("Usuario {} está en estado: {}", email, usuario.getEstado());
            throw new UsernameNotFoundException("Usuario no está activo");
        }
        
        if (!usuario.getAccesoAutorizado()) {
            log.warn("Usuario {} no tiene acceso autorizado", email);
            throw new UsernameNotFoundException("Usuario no tiene acceso autorizado");
        }
        
        // Construir authorities basado en el rol
        Collection<GrantedAuthority> authorities = 
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()));
        
        log.debug("Usuario {} cargado exitosamente con rol: {}", email, usuario.getRol());
        
        return User.builder()
                .username(email)
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
