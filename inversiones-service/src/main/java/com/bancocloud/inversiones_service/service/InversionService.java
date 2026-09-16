package com.bancocloud.inversiones_service.service;

import com.bancocloud.inversiones_service.client.FondoServiceClient;
import com.bancocloud.inversiones_service.dto.*;
import com.bancocloud.inversiones_service.model.*;
import com.bancocloud.inversiones_service.repository.InversionRepository;
import com.bancocloud.inversiones_service.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InversionService {
    
    private final InversionRepository inversionRepository;
    private final FondoServiceClient fondoServiceClient;
    
    /**
     * Crear una nueva inversión
     * - Valida el fondo en fondos-service
     * - Obtiene el valor actual de la cuota
     * - Calcula cuotas = monto / valorCuota
     * - Guarda la inversión
     */
    @Transactional
    public InversionDTO crearInversion(CrearInversionRequestDTO request) {
        log.info("Creando nueva inversión para usuario: {}, fondo: {}, monto: {}", 
                request.getUsuarioId(), request.getFondoId(), request.getMontoInvertido());
        
        // Validar que el fondo existe y obtener su valor de cuota actual
        BigDecimal valorCuotaActual = fondoServiceClient.obtenerValorCuota(request.getFondoId());
        
        // Calcular cuotas = monto / valorCuota
        BigDecimal cuotas = request.getMontoInvertido()
                .divide(valorCuotaActual, 4, RoundingMode.HALF_UP);
        
        // Crear la inversión
        Inversion inversion = Inversion.builder()
                .usuarioId(request.getUsuarioId())
                .fondoId(request.getFondoId())
                .montoInvertido(request.getMontoInvertido())
                .cuotas(cuotas)
                .valorCuotaCompra(valorCuotaActual)
                .estado(EstadoInversion.ACTIVA)
                .build();
        
        Inversion inversionGuardada = inversionRepository.save(inversion);
        log.info("Inversión creada exitosamente con id: {}, cuotas: {}", inversionGuardada.getId(), cuotas);
        
        return mapToDTO(inversionGuardada);
    }
    
    /**
     * Obtener todas las inversiones de un usuario
     */
    @Transactional(readOnly = true)
    public List<InversionDTO> obtenerMisInversiones(Long usuarioId) {
        log.info("Obteniendo inversiones del usuario: {}", usuarioId);
        return inversionRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener inversiones activas de un usuario
     */
    @Transactional(readOnly = true)
    public List<InversionDTO> obtenerInversionesActivas(Long usuarioId) {
        log.info("Obteniendo inversiones activas del usuario: {}", usuarioId);
        return inversionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoInversion.ACTIVA)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Obtener una inversión por id
     */
    @Transactional(readOnly = true)
    public InversionDTO obtenerInversion(Long id) {
        log.info("Obteniendo inversión con id: {}", id);
        Inversion inversion = inversionRepository.findById(id)
                .orElseThrow(() -> new InversionNotFoundException(id));
        return mapToDTO(inversion);
    }
    
    /**
     * Obtener el portafolio completo de un usuario
     * - Obtiene todas sus inversiones activas
     * - Calcula montos totales
     * - Calcula rendimiento actual
     */
    @Transactional(readOnly = true)
    public PortafolioDTO obtenerMiPortafolio(Long usuarioId) {
        log.info("Obteniendo portafolio del usuario: {}", usuarioId);
        
        List<Inversion> inversiones = inversionRepository.findByUsuarioIdAndEstado(
                usuarioId, EstadoInversion.ACTIVA
        );
        
        BigDecimal montoTotalInvertido = BigDecimal.ZERO;
        BigDecimal valorTotalActual = BigDecimal.ZERO;
        
        for (Inversion inversion : inversiones) {
            BigDecimal valorActualCuota = fondoServiceClient.obtenerValorCuota(inversion.getFondoId());
            BigDecimal valorActualInversion = inversion.getCuotas().multiply(valorActualCuota);
            
            montoTotalInvertido = montoTotalInvertido.add(inversion.getMontoInvertido());
            valorTotalActual = valorTotalActual.add(valorActualInversion);
        }
        
        BigDecimal rendimientoTotal = valorTotalActual.subtract(montoTotalInvertido);
        
        BigDecimal rendimientoPorcentaje = BigDecimal.ZERO;
        if (montoTotalInvertido.compareTo(BigDecimal.ZERO) > 0) {
            rendimientoPorcentaje = rendimientoTotal
                    .divide(montoTotalInvertido, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        return PortafolioDTO.builder()
                .usuarioId(usuarioId)
                .montoTotalInvertido(montoTotalInvertido)
                .valorTotalActual(valorTotalActual)
                .rendimientoTotal(rendimientoTotal)
                .rendimientoPorcentaje(rendimientoPorcentaje)
                .inversiones(inversiones.stream().map(this::mapToDTO).toList())
                .build();
    }
    
    /**
     * Obtener rendimiento de una inversión específica
     */
    @Transactional(readOnly = true)
    public RendimientoDTO obtenerRendimientoInversion(Long inversionId) {
        log.info("Obteniendo rendimiento de inversión: {}", inversionId);
        
        Inversion inversion = inversionRepository.findById(inversionId)
                .orElseThrow(() -> new InversionNotFoundException(inversionId));
        
        // Obtener datos actuales del fondo
        FondoDTO fondo = fondoServiceClient.obtenerFondo(inversion.getFondoId());
        BigDecimal valorActualCuota = fondo.getValorCuota();
        
        // Calcular rendimiento
        BigDecimal valorTotalActual = inversion.getCuotas().multiply(valorActualCuota);
        BigDecimal rendimientoAbsoluto = valorTotalActual.subtract(inversion.getMontoInvertido());
        
        BigDecimal rendimientoPorcentaje = BigDecimal.ZERO;
        if (inversion.getMontoInvertido().compareTo(BigDecimal.ZERO) > 0) {
            rendimientoPorcentaje = rendimientoAbsoluto
                    .divide(inversion.getMontoInvertido(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        return RendimientoDTO.builder()
                .inversionId(inversionId)
                .fondoId(inversion.getFondoId())
                .nombreFondo(fondo.getNombre())
                .montoInvertido(inversion.getMontoInvertido())
                .valorActualCuota(valorActualCuota)
                .valorCuotaCompra(inversion.getValorCuotaCompra())
                .cuotas(inversion.getCuotas())
                .valorTotalActual(valorTotalActual)
                .rendimientoAbsoluto(rendimientoAbsoluto)
                .rendimientoPorcentaje(rendimientoPorcentaje)
                .build();
    }
    
    /**
     * Obtener rendimiento de todas las inversiones de un usuario
     */
    @Transactional(readOnly = true)
    public List<RendimientoDTO> obtenerRendimientoUsuario(Long usuarioId) {
        log.info("Obteniendo rendimiento de todas las inversiones del usuario: {}", usuarioId);
        
        List<Inversion> inversiones = inversionRepository.findByUsuarioId(usuarioId);
        
        return inversiones.stream()
                .map(inversion -> obtenerRendimientoInversion(inversion.getId()))
                .toList();
    }
    
    /**
     * Cambiar estado de una inversión (retirada, cancelada, etc)
     */
    @Transactional
    public InversionDTO cambiarEstado(Long id, EstadoInversion nuevoEstado) {
        log.info("Cambiando estado de inversión id: {} a: {}", id, nuevoEstado);
        
        Inversion inversion = inversionRepository.findById(id)
                .orElseThrow(() -> new InversionNotFoundException(id));
        
        EstadoInversion estadoAnterior = inversion.getEstado();
        inversion.setEstado(nuevoEstado);
        
        Inversion inversionActualizada = inversionRepository.save(inversion);
        log.info("Estado actualizado. Anterior: {}, Nuevo: {}", estadoAnterior, nuevoEstado);
        
        return mapToDTO(inversionActualizada);
    }
    
    // Método helper para mapear Entity a DTO
    private InversionDTO mapToDTO(Inversion inversion) {
        return InversionDTO.builder()
                .id(inversion.getId())
                .usuarioId(inversion.getUsuarioId())
                .fondoId(inversion.getFondoId())
                .montoInvertido(inversion.getMontoInvertido())
                .cuotas(inversion.getCuotas())
                .valorCuotaCompra(inversion.getValorCuotaCompra())
                .fechaInversion(inversion.getFechaInversion())
                .fechaActualizacion(inversion.getFechaActualizacion())
                .estado(inversion.getEstado())
                .build();
    }
}
