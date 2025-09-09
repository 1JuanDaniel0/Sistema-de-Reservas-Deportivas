package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "otpverification")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idOtpVerification;

    @Column(name = "identificador", nullable = false, length = 100, unique = true)
    private String identificador; // DNI o número de teléfono

    @Column(name = "otpCode", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "fechaCreacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fechaExpiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

}