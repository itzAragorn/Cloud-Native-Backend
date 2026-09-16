package com.bancocloud.inversiones_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inversiones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inversion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "El ID del usuario es requerido")
    @Column(nullable = false)
    private Long usuarioId;
    
    @NotNull(message = "El ID del fondo es requerido")
    @Column(nullable = false)
    private Long fondoId;
    
    @NotNull(message = "El monto invertido es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montoInvertido;
    
    @NotNull(message = "La cantidad de cuotas es requerida")
    @DecimalMin(value = "0.01", message = "Las cuotas deben ser mayor a 0")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cuotas;
    
    @NotNull(message = "El valor de cuota de compra es requerido")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorCuotaCompra;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaInversion;
    
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoInversion estado = EstadoInversion.ACTIVA;
    
    @PrePersist
    protected void onCreate() {
        this.fechaInversion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
