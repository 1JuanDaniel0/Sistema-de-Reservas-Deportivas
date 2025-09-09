package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idReserva", nullable = false)
    private Integer idReserva;

    @Column(name="horaInicio")
    private LocalTime horaInicio;

    @Column(name="horaFin")
    private LocalTime horaFin;

    @Column(name="fecha")
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name="coordinador")
    private Usuarios coordinador;

    @Column(name="costo")
    private Double costo;

    @ManyToOne
    @JoinColumn(name="vecino")
    private Usuarios vecino;

    @ManyToOne
    @JoinColumn(name="estado")
    private EstadoReserva estado;

    @Column(name="estado_reembolso")
    @Enumerated(EnumType.STRING)
    private EstadoReembolso estadoReembolso; // NO_APLICA, PENDIENTE, APROBADO, RECHAZADO

    @ManyToOne
    @JoinColumn(name="espacio")
    private Espacio espacio;

    @Column(name = "captura_url")
    private String capturaKey; // ahora guarda solo la "key" del archivo en S3

    @Column(name="momentoReserva")
    private LocalDateTime momentoReserva;

    @Column(name="tipoPago")
    private String tipoPago; // 'En línea', 'En banco'

    @Column(name = "estado_pago")
    private String estadoPago; // "Pendiente", "Pagado", "Anulado"

    @Column(name = "id_transaccion_pago")
    private String idTransaccionPago; // ID del pago de MercadoPago

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @OneToOne(mappedBy = "reserva")
    private Calificacion calificacion;

    @Getter
    public enum EstadoReembolso {
        NO_APLICA,      // Para reservas no pagadas o completadas normalmente
        PENDIENTE,      // Solicitud enviada, esperando respuesta
        APROBADO,       // Reembolso aprobado
        RECHAZADO       // Reembolso rechazado
    }
}
