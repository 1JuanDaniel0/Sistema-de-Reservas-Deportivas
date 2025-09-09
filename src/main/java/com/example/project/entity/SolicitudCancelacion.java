package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Setter
@Table(name = "solicitud_cancelacion")
public class SolicitudCancelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motivo;

    @Column(length = 20, nullable = false)
    private String estado = "Pendiente";
    // "Pendiente" -> Esperando evaluación del coordinador
    // "Aprobado" -> Coordinador aprobó, va al admin (solo para pago en banco)
    // "Rechazado" -> Coordinador rechazó
    // "Completado" -> Admin completó el reembolso bancario

    @Column(name = "codigo_pago", length = 50)
    private String codigoPago;

    @Column(name = "comprobante_url")
    private String comprobanteUrl;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    // NUEVOS CAMPOS AGREGADOS
    @Column(name = "tiempo_respuesta")
    private LocalDateTime tiempoRespuesta;

    @Column(name = "motivo_respuesta", columnDefinition = "TEXT")
    private String motivoRespuesta;

    // Método helper para marcar cuando se procesa la solicitud
    public void marcarProcesada(String motivoRespuesta) {
        this.tiempoRespuesta = LocalDateTime.now();
        this.motivoRespuesta = motivoRespuesta;
    }

    // NUEVO MÉTODO: Verifica si la solicitud es urgente (menos de 24 horas)
    public boolean esUrgente() {
        if (this.reserva == null || !"Pendiente".equals(this.estado)) {
            return false;
        }

        // Combinar fecha y hora de la reserva
        LocalDateTime fechaHoraReserva = LocalDateTime.of(
                this.reserva.getFecha(),
                this.reserva.getHoraInicio()
        );

        // Calcular la duración hasta la reserva
        java.time.Duration duracion = java.time.Duration.between(LocalDateTime.now(), fechaHoraReserva);
        long horasHastaReserva = duracion.toHours();

        return horasHastaReserva < 24 && horasHastaReserva > 0;
    }

    // MÉTODO ADICIONAL: Obtener horas restantes hasta la reserva
    public long getHorasHastaReserva() {
        if (this.reserva == null) {
            return 0;
        }

        LocalDateTime fechaHoraReserva = LocalDateTime.of(
                this.reserva.getFecha(),
                this.reserva.getHoraInicio()
        );

        return ChronoUnit.HOURS.between(LocalDateTime.now(), fechaHoraReserva);
    }
}