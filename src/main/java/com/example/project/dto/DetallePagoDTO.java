package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetallePagoDTO {
    private Integer idEspacio; // Corregido: debe ser idEspacio, no idReserva
    private String fecha;
    private Integer horaInicio;
    private Integer horaFin;
}