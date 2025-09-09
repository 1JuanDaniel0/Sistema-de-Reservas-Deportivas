package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "estadoespacio")
public class EstadoEspacio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idEstadoEspacio", nullable = false)
    private Integer idEstadoEspacio;

    @Column(name="estado")
    private String estado;
}
