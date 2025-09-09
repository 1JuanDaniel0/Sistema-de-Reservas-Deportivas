package com.example.project.service;

import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class ReservaCancelacionService {

    @Autowired private ReservaRepositoryVecino reservaRepositoryVecino;
    @Autowired private EstadoReservaRepository estadoReservaRepository;
    @Autowired private SolicitudCancelacionRepository solicitudCancelacionRepository;
    @Autowired private PagoRepository pagoRepository;

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    // Estados que no permiten cancelación
    private static final List<String> ESTADOS_NO_CANCELABLES = Arrays.asList(
            "Cancelada", "Cancelada con reembolso", "Finalizada",
            "Cancelada sin reembolso", "Reembolso solicitado"
    );

    /**
     * Determina qué acciones de cancelación están disponibles para una reserva
     */
    public AccionesCancelacion evaluarAccionesCancelacion(Reserva reserva, Usuarios vecino) {
        AccionesCancelacion acciones = new AccionesCancelacion();

        // Validaciones básicas
        if (reserva == null || !reserva.getVecino().getIdUsuarios().equals(vecino.getIdUsuarios())) {
            return acciones; // Todo en false
        }

        String estadoActual = reserva.getEstado().getEstado();

        // Si está en estado no cancelable, no permitir nada
        if (ESTADOS_NO_CANCELABLES.contains(estadoActual)) {
            return acciones;
        }

        // Si ya existe solicitud de cancelación, no permitir más acciones
        if (solicitudCancelacionRepository.existsByReserva_IdReserva(reserva.getIdReserva())) {
            return acciones;
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        LocalDateTime inicioReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());

        long minutosRestantes = ChronoUnit.MINUTES.between(ahora, inicioReserva);
        long horasRestantes = ChronoUnit.HOURS.between(ahora, inicioReserva);

        // Si la reserva ya empezó, no se puede cancelar
        if (minutosRestantes <= 0) {
            return acciones;
        }

        String tipoPago = reserva.getTipoPago();

        // LÓGICA PRINCIPAL
        if ("Pendiente de confirmación".equals(estadoActual)) {
            // Reservas no confirmadas siempre se pueden cancelar directamente
            acciones.setPuedeCancelarDirecto(true);
            acciones.setTipoReembolso("Sin reembolso - No confirmada");

        } else if ("Confirmada".equals(estadoActual)) {

            if ("En línea".equals(tipoPago)) {
                if (horasRestantes >= 24) {
                    // Pago online con 24+ horas: cancelación directa con reembolso automático
                    acciones.setPuedeCancelarDirecto(true);
                    acciones.setTipoReembolso("Reembolso automático vía MercadoPago");
                } else {
                    // Pago online con menos de 24 horas: solicitud al coordinador
                    acciones.setPuedeSolicitarCancelacion(true);
                    acciones.setTipoReembolso("Sujeto a evaluación del coordinador");
                }

            } else if ("En banco".equals(tipoPago)) {
                // Pago en banco: siempre requiere solicitud al coordinador
                acciones.setPuedeSolicitarCancelacion(true);
                if (horasRestantes >= 24) {
                    acciones.setTipoReembolso("Reembolso manual - Más de 24 horas");
                } else {
                    acciones.setTipoReembolso("Sujeto a evaluación del coordinador");
                }
            }
        }

        // Información adicional
        acciones.setHorasRestantes(horasRestantes);
        acciones.setMinutosRestantes(minutosRestantes);
        acciones.setTipoPago(tipoPago);
        acciones.setEstado(estadoActual);

        return acciones;
    }

    /**
     * Valida si una reserva puede ser cancelada directamente
     */
    public ResultadoValidacion validarCancelacionDirecta(Reserva reserva, Usuarios vecino) {
        AccionesCancelacion acciones = evaluarAccionesCancelacion(reserva, vecino);

        if (!acciones.isPuedeCancelarDirecto()) {
            return new ResultadoValidacion(false, "Esta reserva no puede cancelarse directamente");
        }

        return new ResultadoValidacion(true, "Reserva válida para cancelación directa");
    }

    /**
     * Valida si se puede solicitar cancelación
     */
    public ResultadoValidacion validarSolicitudCancelacion(Reserva reserva, Usuarios vecino) {
        AccionesCancelacion acciones = evaluarAccionesCancelacion(reserva, vecino);

        if (!acciones.isPuedeSolicitarCancelacion()) {
            return new ResultadoValidacion(false, "No puedes solicitar cancelación para esta reserva");
        }

        return new ResultadoValidacion(true, "Puedes solicitar cancelación");
    }

    // Clases internas para los resultados
    @Setter
    @Getter
    public static class AccionesCancelacion {
        // Getters y setters
        private boolean puedeCancelarDirecto = false;
        private boolean puedeSolicitarCancelacion = false;
        private String tipoReembolso = "";
        private long horasRestantes = 0;
        private long minutosRestantes = 0;
        private String tipoPago = "";
        private String estado = "";

    }

    @Getter
    public static class ResultadoValidacion {
        private boolean valido;
        private String mensaje;

        public ResultadoValidacion(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

    }
}