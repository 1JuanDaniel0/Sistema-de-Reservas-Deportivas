package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "intentos_ip")
public class IntentoIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Long id;

    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "ultima_solicitud", nullable = false)
    private LocalDateTime ultimaSolicitud;

    @Column(name = "contador", nullable = false)
    private int contador = 0;

}
