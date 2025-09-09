package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tipoespacio")
public class TipoEspacio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoEspacio", nullable = false)
    private Integer idTipoEspacio;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;
}
