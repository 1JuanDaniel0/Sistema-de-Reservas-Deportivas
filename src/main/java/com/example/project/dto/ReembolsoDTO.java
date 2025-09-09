package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class ReembolsoDTO {

    private Integer idReembolso;
    private Integer idSolicitud;
    private Integer idReserva;

    // Datos del vecino
    private String vecinoNombre;
    private String vecinoApellido;
    private String vecinoDni;
    private String vecinoCorreo;

    // Datos de la reserva
    private String espacioNombre;
    private LocalDate fechaReserva;
    private String horaReserva;
    private BigDecimal montoReembolso;
    private String tipoPagoOriginal;

    // Datos del reembolso
    private String estadoReembolso;
    private String metodoReembolso;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaProcesamiento;

    // Datos del coordinador que aprobó
    private String coordinadorNombre;
    private String motivoAprobacion;
    private LocalDateTime fechaAprobacion;

    // Datos del administrador (si aplica)
    private String adminNombre;
    private String numeroOperacion;
    private String entidadBancaria;
    private String observacionesAdmin;

    // Para identificar urgencia
    private boolean esUrgente;
    private long diasPendiente;

    // Campos adicionales necesarios para el historial
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String motivoSolicitud; // motivo de la solicitud original
    private LocalDateTime fechaSolicitud;
    private String codigoPago;
    private String comprobanteUrl;
    private String idTransaccionReembolso;

    // Constructor vacío
    public ReembolsoDTO() {}

    // Constructor para consultas simples
    public ReembolsoDTO(Integer idReembolso, Integer idSolicitud, Integer idReserva,
                        String vecinoNombre, String vecinoApellido, String vecinoDni, String vecinoCorreo,
                        String espacioNombre, LocalDate fechaReserva, LocalTime horaInicio, LocalTime horaFin,
                        BigDecimal montoReembolso, String tipoPagoOriginal, String estadoReembolso, String metodoReembolso,
                        LocalDateTime fechaCreacion, LocalDateTime fechaProcesamiento,
                        String coordinadorNombre, String coordinadorApellido, String motivoAprobacion, LocalDateTime fechaAprobacion,
                        String adminNombre, String adminApellido, String numeroOperacion, String entidadBancaria, String observacionesAdmin,
                        String motivoSolicitud, LocalDateTime fechaSolicitud, String codigoPago, String comprobanteUrl, String idTransaccionReembolso) {

        this.idReembolso = idReembolso;
        this.idSolicitud = idSolicitud;
        this.idReserva = idReserva;
        this.vecinoNombre = vecinoNombre;
        this.vecinoApellido = vecinoApellido;
        this.vecinoDni = vecinoDni;
        this.vecinoCorreo = vecinoCorreo;
        this.espacioNombre = espacioNombre;
        this.fechaReserva = fechaReserva;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.montoReembolso = montoReembolso;
        this.tipoPagoOriginal = tipoPagoOriginal;
        this.estadoReembolso = estadoReembolso;
        this.metodoReembolso = metodoReembolso;
        this.fechaCreacion = fechaCreacion;
        this.fechaProcesamiento = fechaProcesamiento;
        this.coordinadorNombre = coordinadorNombre != null && coordinadorApellido != null ?
                coordinadorNombre + " " + coordinadorApellido : null;
        this.motivoAprobacion = motivoAprobacion;
        this.fechaAprobacion = fechaAprobacion;
        this.adminNombre = adminNombre != null && adminApellido != null ?
                adminNombre + " " + adminApellido : null;
        this.numeroOperacion = numeroOperacion;
        this.entidadBancaria = entidadBancaria;
        this.observacionesAdmin = observacionesAdmin;
        this.motivoSolicitud = motivoSolicitud;
        this.fechaSolicitud = fechaSolicitud;
        this.codigoPago = codigoPago;
        this.comprobanteUrl = comprobanteUrl;
        this.idTransaccionReembolso = idTransaccionReembolso;

        // Calcular campos derivados
        if (fechaCreacion != null) {
            this.diasPendiente = java.time.Duration.between(fechaCreacion, LocalDateTime.now()).toDays();
            this.esUrgente = this.diasPendiente > 3 && "PENDIENTE".equals(estadoReembolso);
        }
    }

    public String getHoraReserva() {
        if (horaInicio != null && horaFin != null) {
            return horaInicio.toString() + " - " + horaFin.toString();
        }
        return "";
    }

    // Método para obtener nombre completo del vecino
    public String getVecinoNombreCompleto() {
        return (vecinoNombre != null ? vecinoNombre : "") + " " +
                (vecinoApellido != null ? vecinoApellido : "");
    }

    // Método para formatear el monto
    public String getMontoFormateado() {
        return montoReembolso != null ? "S/ " + montoReembolso.toString() : "S/ 0.00";
    }

    // Método para obtener descripción del estado
    public String getEstadoDescripcion() {
        return switch (estadoReembolso != null ? estadoReembolso : "") {
            case "PENDIENTE" -> "Pendiente de procesamiento";
            case "COMPLETADO" -> "Reembolso completado";
            case "FALLIDO" -> "Falló el procesamiento";
            default -> "Estado desconocido";
        };
    }

    // Método para obtener CSS class según el estado
    public String getEstadoCssClass() {
        return switch (estadoReembolso != null ? estadoReembolso : "") {
            case "PENDIENTE" -> esUrgente ? "badge bg-warning text-dark" : "badge bg-secondary";
            case "COMPLETADO" -> "badge bg-success";
            case "FALLIDO" -> "badge bg-danger";
            default -> "badge bg-light text-dark";
        };
    }

    // Método para obtener descripción del método
    public String getMetodoDescripcion() {
        if (metodoReembolso == null) return "Por definir";

        return switch (metodoReembolso) {
            case "MERCADOPAGO_AUTOMATICO" -> "Automático - MercadoPago";
            case "DEPOSITO_MANUAL" -> "Depósito manual";
            case "TRANSFERENCIA_MANUAL" -> "Transferencia manual";
            default -> metodoReembolso;
        };
    }
}