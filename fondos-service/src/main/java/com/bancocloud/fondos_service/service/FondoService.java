package com.bancocloud.fondos_service.service;

import com.bancocloud.fondos_service.dto.*;
import com.bancocloud.fondos_service.model.*;
import com.bancocloud.fondos_service.repository.FondoRepository;
import com.bancocloud.fondos_service.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FondoService {
    
    private final FondoRepository fondoRepository;
    
    /**
     * Crear un nuevo fondo
     */
    @Transactional
    public FondoDTO crearFondo(FondoRequestDTO request) {
        log.info("Creando nuevo fondo: {}", request.getNombre());
        
        // Validar que no exista un fondo con el mismo nombre
        if (fondoRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new FondoDuplicadoException(request.getNombre());
        }
        
        Fondo fondo = Fondo.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .valorCuota(request.getValorCuota())
                .estado(EstadoFondo.ACTIVO)
                .build();
        
        Fondo fondoGuardado = fondoRepository.save(fondo);
        log.info("Fondo creado exitosamente con id: {}", fondoGuardado.getId());
        
        return mapToDTO(fondoGuardado);
    }
    
    /**
     * Obtener todos los fondos
     */
    @Transactional(readOnly = true)
    public List<FondoDTO> obtenerTodosFondos() {
        log.info("Obteniendo todos los fondos");
        return fondoRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener fondos activos
     */
    @Transactional(readOnly = true)
    public List<FondoDTO> obtenerFondosActivos() {
        log.info("Obteniendo fondos activos");
        return fondoRepository.findByEstado(EstadoFondo.ACTIVO)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener un fondo por id
     */
    @Transactional(readOnly = true)
    public FondoDTO obtenerFondoPorId(Long id) {
        log.info("Obteniendo fondo con id: {}", id);
        Fondo fondo = fondoRepository.findById(id)
                .orElseThrow(() -> new FondoNotFoundException(id));
        return mapToDTO(fondo);
    }
    
    /**
     * Actualizar el valor de la cuota de un fondo
     */
    @Transactional
    public FondoDTO actualizarValorCuota(Long id, ActualizarValorCuotaRequestDTO request) {
        log.info("Actualizando valor de cuota para fondo id: {}", id);
        
        Fondo fondo = fondoRepository.findById(id)
                .orElseThrow(() -> new FondoNotFoundException(id));
        
        BigDecimal valorAnterior = fondo.getValorCuota();
        fondo.setValorCuota(request.getNuevoValor());
        
        Fondo fondoActualizado = fondoRepository.save(fondo);
        log.info("Valor de cuota actualizado para fondo id: {}. Valor anterior: {}, Nuevo valor: {}", 
                id, valorAnterior, request.getNuevoValor());
        
        return mapToDTO(fondoActualizado);
    }
    
    /**
     * Cambiar estado de un fondo
     */
    @Transactional
    public FondoDTO cambiarEstado(Long id, EstadoFondo nuevoEstado) {
        log.info("Cambiando estado del fondo id: {} a: {}", id, nuevoEstado);
        
        Fondo fondo = fondoRepository.findById(id)
                .orElseThrow(() -> new FondoNotFoundException(id));
        
        EstadoFondo estadoAnterior = fondo.getEstado();
        fondo.setEstado(nuevoEstado);
        
        Fondo fondoActualizado = fondoRepository.save(fondo);
        log.info("Estado actualizado para fondo id: {}. Estado anterior: {}, Nuevo estado: {}", 
                id, estadoAnterior, nuevoEstado);
        
        return mapToDTO(fondoActualizado);
    }
    
    /**
     * Obtener valor actual de la cuota de un fondo
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerValorCuota(Long fondoId) {
        log.info("Obteniendo valor de cuota para fondo id: {}", fondoId);
        Fondo fondo = fondoRepository.findById(fondoId)
                .orElseThrow(() -> new FondoNotFoundException(fondoId));
        return fondo.getValorCuota();
    }
    
    // Método helper para mapear Entity a DTO
    private FondoDTO mapToDTO(Fondo fondo) {
        return FondoDTO.builder()
                .id(fondo.getId())
                .nombre(fondo.getNombre())
                .descripcion(fondo.getDescripcion())
                .valorCuota(fondo.getValorCuota())
                .fechaCreacion(fondo.getFechaCreacion())
                .fechaActualizacion(fondo.getFechaActualizacion())
                .estado(fondo.getEstado())
                .build();
    }
}
