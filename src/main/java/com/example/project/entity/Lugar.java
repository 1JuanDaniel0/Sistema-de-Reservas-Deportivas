package com.example.project.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "lugar")
public class Lugar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idLugar", nullable = false)
    private Integer idLugar;

    @Column(name="nombre", nullable = false)
    private String lugar;

    @Column(name="direccion")
    private String direccion;

    // Almacena latitud,longitud en formato string para Google Maps
    @Column(name="ubicacion")
    private String ubicacion;

    @OneToMany(mappedBy = "idLugar", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Espacio> espacios;

    @ManyToMany
    @JoinTable(
            name = "lugar_coordinador",
            joinColumns = @JoinColumn(name = "id_lugar"),
            inverseJoinColumns = @JoinColumn(name = "id_coordinador")
    )
    private List<Usuarios> coordinadores;

}
