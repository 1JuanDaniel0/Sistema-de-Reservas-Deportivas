package com.example.project.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Getter
@Setter
public class ReservaCalendarioDto {
    private Integer idReserva;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalDate fecha;
    private Double costo;
    private String tipoPago;
    private LocalDateTime momentoReserva;
    private boolean esPropia; // Solo necesario para vecino
    // Datos del estado
    private String estado;
    // Datos del vecino (solo lo necesario)
    private Integer idVecino;
    private String vecinoNombre;
    private String vecinoApellido;
    // Datos del espacio (solo lo necesario)
    private String espacioNombre;
    // Constructor vacío
    public ReservaCalendarioDto() {}
    // Constructor con todos los campos
    public ReservaCalendarioDto(Integer idReserva, LocalTime horaInicio, LocalTime horaFin,
                                LocalDate fecha, Double costo, String tipoPago,
                                LocalDateTime momentoReserva, String estado,
                                String vecinoNombre, String vecinoApellido, String espacioNombre,
                                Integer idVecino) {
        this.idReserva = idReserva;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.fecha = fecha;
        this.costo = costo;
        this.tipoPago = tipoPago;
        this.momentoReserva = momentoReserva;
        this.estado = estado;
        this.vecinoNombre = vecinoNombre;
        this.vecinoApellido = vecinoApellido;
        this.espacioNombre = espacioNombre;
        this.idVecino = idVecino;
    }
}