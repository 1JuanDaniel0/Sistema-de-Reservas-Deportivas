package com.example.project.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "espacio")
public class Espacio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idEspacio", nullable = false)
    private Integer idEspacio;

    @Column(name="nombre")
    private String nombre;

    @ManyToOne
    @JoinColumn(name="idLugar")
    private Lugar idLugar;

    @ManyToOne
    @JoinColumn(name="idEstadoEspacio")
    private EstadoEspacio idEstadoEspacio;

    @Column(name="observaciones", length = 1000)
    private String observaciones;

    @Column(name="costo")
    private Double costo;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "idTipoEspacio")
    private TipoEspacio tipoEspacio;

    @ManyToMany
    @JoinTable(
            name = "espacio_deporte",
            joinColumns = @JoinColumn(name = "id_espacio"),
            inverseJoinColumns = @JoinColumn(name = "id_deporte")
    )
    private List<Deporte> deportes = new ArrayList<>();

    @Column(name = "foto1_url")
    private String foto1Url;

    @Column(name = "foto2_url")
    private String foto2Url;

    @Column(name = "foto3_url")
    private String foto3Url;
}
