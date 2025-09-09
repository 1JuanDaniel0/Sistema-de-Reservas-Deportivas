package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class MantenimientoDTO {
    private Integer espacioId;

    // Usar Strings para evitar problemas de deserialización
    private String fechaInicio;  // formato: yyyy-MM-dd
    private String fechaFin;     // formato: yyyy-MM-dd
    private String horaInicio;   // formato: HH:mm
    private String horaFin;      // formato: HH:mm

    private String tipoMantenimiento;
    private String prioridad;
    private String descripcion;
    private Integer responsableId;
    private Double costoEstimado;

    // Constructor vacío
    public MantenimientoDTO() {}

    // Constructor con todos los parámetros
    public MantenimientoDTO(Integer espacioId, String fechaInicio, String fechaFin,
                            String horaInicio, String horaFin, String tipoMantenimiento,
                            String prioridad, String descripcion, Integer responsableId,
                            Double costoEstimado) {
        this.espacioId = espacioId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.tipoMantenimiento = tipoMantenimiento;
        this.prioridad = prioridad;
        this.descripcion = descripcion;
        this.responsableId = responsableId;
        this.costoEstimado = costoEstimado;
    }

    // Métodos helper para convertir a LocalDate y LocalTime
    public LocalDate getFechaInicioAsLocalDate() {
        return fechaInicio != null ? LocalDate.parse(fechaInicio) : null;
    }

    public LocalDate getFechaFinAsLocalDate() {
        return fechaFin != null ? LocalDate.parse(fechaFin) : null;
    }

    public LocalTime getHoraInicioAsLocalTime() {
        return horaInicio != null ? LocalTime.parse(horaInicio) : null;
    }

    public LocalTime getHoraFinAsLocalTime() {
        return horaFin != null ? LocalTime.parse(horaFin) : null;
    }
}