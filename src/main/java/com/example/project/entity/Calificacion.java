package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "calificacion")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCalificacion;

    @ManyToOne
    @JoinColumn(name = "idEspacio", nullable = false)
    private Espacio espacio;

    @OneToOne
    @JoinColumn(name = "idReserva", nullable = false, unique = true)
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "idVecino", nullable = false)
    private Usuarios vecino;

    @Column(nullable = false)
    private Double puntaje;

    @Column(length = 300)
    private String comentario;

    @Column(name = "fechaCalificacion", columnDefinition = "DATETIME")
    private LocalDateTime fechaCalificacion = LocalDateTime.now();
}
