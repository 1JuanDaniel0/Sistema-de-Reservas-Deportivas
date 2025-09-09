package com.example.project.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "estadomensaje")
public class EstadoMensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idEstadoMensaje", nullable = false)
    private Integer idEstadoMensaje;

    @Column(name="estado")
    private String estado;
}
