package com.example.project.dto;

import com.example.project.entity.Reserva;
import com.example.project.entity.Pago;
import com.example.project.entity.SolicitudCancelacion;
import lombok.Getter;

@Getter
public class ReservaExtendidaDTO {
    // Getters
    private Reserva reserva;
    private Pago pago;
    private SolicitudCancelacion solicitud;

    // Constructor
    public ReservaExtendidaDTO(Reserva reserva, Pago pago, SolicitudCancelacion solicitud) {
        this.reserva = reserva;
        this.pago = pago;
        this.solicitud = solicitud;
    }

}
