package com.example.project.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;
@Getter
@Setter
@Entity
@Table(name = "mensaje")
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idMensaje", nullable = false)
    private Integer idMensaje;

    @Column(name="horaEnvio")
    private Timestamp horaEnvio;

    @OneToOne
    @JoinColumn(name="transmisor")
    private Usuarios transmisor;

    @OneToOne
    @JoinColumn(name="receptor")
    private Usuarios receptor;

    @Column(name="mensaje")
    private String mensaje;

    @ManyToOne
    @JoinColumn(name="estado")
    private EstadoMensaje estado;
}
