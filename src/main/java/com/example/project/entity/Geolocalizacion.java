package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "geolocalizacion")
public class Geolocalizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idGeolocalizacion", nullable = false)
    private Integer idGeolocalizacion;

    @Column(name="fecha")
    private LocalDate fecha;

    @Column(name="horaInicio")
    private LocalTime horaInicio;

    @Column(name="horaFin")
    private LocalTime horaFin;

    @JoinColumn(name="coordinador")
    @OneToOne
    private Usuarios coordinador;

    @JoinColumn(name="lugar")
    @ManyToOne
    private Lugar lugar;

    @Column(name="lugarExacto")
    private String lugarExacto;

    @Column(name="observacion")
    private String observacion;

    @ManyToOne
    @JoinColumn(name="estado")
    private EstadoGeo estado;
}
