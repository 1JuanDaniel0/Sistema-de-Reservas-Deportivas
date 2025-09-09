package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "actividad")
public class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idActividad", nullable = false)
    private int idActividad;

    @ManyToOne
    @JoinColumn(name = "idUsuario")
    private Usuarios usuario;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "detalle")
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public String getColorClass() {
        if (descripcion == null) return "timeline-point-secondary";

        switch (descripcion) {
            case "Agregó una observación", "Creación de Coordinador":
                return "timeline-point-info";
            case "Cambio de estado":
                return "timeline-point-warning";
            case "Inició una Asistencia":
                return "timeline-point-primary";
            case "Registró una Asistencia", "Creación de espacio":
                return "timeline-point-success";
            default:
                return "timeline-point-danger";
        }
    }


}