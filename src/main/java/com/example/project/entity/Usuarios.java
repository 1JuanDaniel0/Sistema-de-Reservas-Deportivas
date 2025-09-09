package com.example.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import com.example.project.validation.OnCreate;
import com.example.project.validation.OnUpdate;

import javax.management.StringValueExp;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="usuarios")
public class Usuarios implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idUsuarios", nullable = false)
    private Integer idUsuarios;

    @Column(name = "nombres")
    private String nombres;

    @Column(name = "apellidos")
    private String apellidos;

    @Column(name = "dni", nullable = false, length = 8, unique = true)
    @NotBlank(message = "{usuario.dni.notblank}")
    @Size(min = 8, max = 8, message = "{usuario.dni.size}")
    @Pattern(regexp = "\\d{8}", message = "{usuario.dni.pattern}")
    private String dni;


    @Size(max = 100, message = "{usuario.correo.long}") // Mensaje si excede los 100 caracteres
    @Email(message = "{usuario.correo.invalid}")
    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "contrasena", length = 100)
    @NotBlank(message = "{usuario.contrasena.notblank}") // No puede estar en blanco
    @Size(min = 3, max = 100, message = "{usuario.contrasena.size}")
    private String contrasena;

    @NotBlank(groups = OnCreate.class, message = "{usuario.confirmContrasena.notblank}")
    @Transient // Esta anotación indica a JPA que no se mapee a la base de datos
    private String confirmContrasena;

    @ManyToOne
    @JoinColumn(name = "rol")
    private Rol rol;

    @ManyToOne
    @JoinColumn(name="estado")
    private EstadoUsu estado;

    @Column(name = "telefono", length = 20, unique = true)
    private String telefono;

    @Column(name = "fechaCreacion")
    private Timestamp fechaCreacion;

    @ManyToMany(mappedBy = "coordinadores")
    @JsonIgnore
    private List<Lugar> lugaresAsignados;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

}
