package com.bancocloud.fondos_service.repository;

import com.bancocloud.fondos_service.model.Fondo;
import com.bancocloud.fondos_service.model.EstadoFondo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FondoRepository extends JpaRepository<Fondo, Long> {
    
    /**
     * Buscar fondos por estado
     */
    List<Fondo> findByEstado(EstadoFondo estado);
    
    /**
     * Buscar fondo por nombre (case insensitive)
     */
    Optional<Fondo> findByNombreIgnoreCase(String nombre);
    
    /**
     * Verificar si existe un fondo con el nombre dado
     */
    boolean existsByNombreIgnoreCase(String nombre);
}
