package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "mantenimiento")
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mantenimiento", nullable = false)
    private Integer idMantenimiento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_espacio")
    private Espacio espacio;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "tipo_mantenimiento", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoMantenimiento tipoMantenimiento;

    @Column(name = "prioridad", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PrioridadMantenimiento prioridad;

    @Column(name = "descripcion", columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private Usuarios responsable; // Coordinador asignado

    @ManyToOne(optional = false)
    @JoinColumn(name = "creado_por")
    private Usuarios creadoPor; // Administrador que programó

    @Column(name = "costo_estimado")
    private Double costoEstimado;

    @Column(name = "costo_real")
    private Double costoReal;

    @Column(name = "estado", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoMantenimiento estado = EstadoMantenimiento.PROGRAMADO;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_inicio_real")
    private LocalDateTime fechaInicioReal;

    @Column(name = "fecha_fin_real")
    private LocalDateTime fechaFinReal;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    // Métodos de utilidad
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Método para marcar inicio de mantenimiento
    public void iniciarMantenimiento() {
        this.estado = EstadoMantenimiento.EN_PROCESO;
        this.fechaInicioReal = LocalDateTime.now();
    }

    // Método para finalizar mantenimiento
    public void finalizarMantenimiento(String observaciones, Double costoReal) {
        this.estado = EstadoMantenimiento.COMPLETADO;
        this.fechaFinReal = LocalDateTime.now();
        this.observaciones = observaciones;
        this.costoReal = costoReal;
    }

    // Método para cancelar mantenimiento
    public void cancelarMantenimiento(String motivo) {
        this.estado = EstadoMantenimiento.CANCELADO;
        this.motivoCancelacion = motivo;
    }

    // Verificar si está activo (bloquea reservas)
    public boolean estaActivo() {
        return this.estado == EstadoMantenimiento.PROGRAMADO ||
                this.estado == EstadoMantenimiento.EN_PROCESO;
    }

    // Verificar si afecta una fecha/hora específica
    public boolean afectaFechaHora(LocalDate fecha, LocalTime hora) {
        if (fecha.isBefore(this.fechaInicio) || fecha.isAfter(this.fechaFin)) {
            return false;
        }

        if (fecha.equals(this.fechaInicio) && hora.isBefore(this.horaInicio)) {
            return false;
        }

        if (fecha.equals(this.fechaFin) && hora.isAfter(this.horaFin)) {
            return false;
        }

        return estaActivo();
    }

    // Enums
    @Getter
    public enum TipoMantenimiento {
        PREVENTIVO("Mantenimiento Preventivo"),
        CORRECTIVO("Mantenimiento Correctivo"),
        LIMPIEZA("Limpieza Profunda"),
        REPARACION("Reparación"),
        INSTALACION("Instalación de Equipos"),
        OTRO("Otro");

        private final String descripcion;

        TipoMantenimiento(String descripcion) {
            this.descripcion = descripcion;
        }

    }

    @Getter
    public enum PrioridadMantenimiento {
        BAJA("Baja"),
        MEDIA("Media"),
        ALTA("Alta"),
        URGENTE("Urgente");

        private final String descripcion;

        PrioridadMantenimiento(String descripcion) {
            this.descripcion = descripcion;
        }

    }

    @Getter
    public enum EstadoMantenimiento {
        PROGRAMADO("Programado"),
        EN_PROCESO("En Proceso"),
        COMPLETADO("Completado"),
        CANCELADO("Cancelado"),
        REPROGRAMADO("Reprogramado");

        private final String descripcion;

        EstadoMantenimiento(String descripcion) {
            this.descripcion = descripcion;
        }

    }
}