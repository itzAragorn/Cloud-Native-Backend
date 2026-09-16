package com.bancocloud.usuarios_service.repository;

import com.bancocloud.usuarios_service.model.Usuario;
import com.bancocloud.usuarios_service.model.Rol;
import com.bancocloud.usuarios_service.model.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    /**
     * Buscar usuario por email
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
     * Verificar si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);
    
    /**
     * Buscar usuarios por rol
     */
    List<Usuario> findByRol(Rol rol);
    
    /**
     * Buscar usuarios por estado
     */
    List<Usuario> findByEstado(EstadoUsuario estado);
    
    /**
     * Buscar usuarios por acceso autorizado
     */
    List<Usuario> findByAccesoAutorizado(Boolean accesoAutorizado);
    
    /**
     * Buscar usuarios con rol CLIENTE que no han sido autorizados
     */
    List<Usuario> findByRolAndAccesoAutorizado(Rol rol, Boolean accesoAutorizado);
}
