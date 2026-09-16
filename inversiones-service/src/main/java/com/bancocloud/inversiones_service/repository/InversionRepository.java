package com.bancocloud.inversiones_service.repository;

import com.bancocloud.inversiones_service.model.Inversion;
import com.bancocloud.inversiones_service.model.EstadoInversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface InversionRepository extends JpaRepository<Inversion, Long> {
    
    /**
     * Obtener todas las inversiones de un usuario
     */
    List<Inversion> findByUsuarioId(Long usuarioId);
    
    /**
     * Obtener inversiones activas de un usuario
     */
    List<Inversion> findByUsuarioIdAndEstado(Long usuarioId, EstadoInversion estado);
    
    /**
     * Obtener inversiones por fondo
     */
    List<Inversion> findByFondoId(Long fondoId);
    
    /**
     * Verificar si un usuario tiene inversiones en un fondo
     */
    boolean existsByUsuarioIdAndFondoId(Long usuarioId, Long fondoId);
    
    /**
     * Contar inversiones activas por usuario
     */
    long countByUsuarioIdAndEstado(Long usuarioId, EstadoInversion estado);
    
    /**
     * Obtener inversión por usuario y fondo
     */
    @Query("SELECT i FROM Inversion i WHERE i.usuarioId = :usuarioId AND i.fondoId = :fondoId")
    List<Inversion> findByUsuarioAndFondo(
            @Param("usuarioId") Long usuarioId,
            @Param("fondoId") Long fondoId
    );
}
