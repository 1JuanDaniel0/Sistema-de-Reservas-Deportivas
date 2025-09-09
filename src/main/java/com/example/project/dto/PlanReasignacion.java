package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PlanReasignacion {
    private Map<TipoEntidad, List<ReasignacionDetalle>> reasignacionesAutomaticas;
    private List<ReasignacionManual> reasignacionesManuales;
    private List<ConflictoReasignacion> conflictos;
    private Duration estimacionTiempo;
    private boolean requiereConfirmacionAdicional;
    private String resumenCambios;

    public enum TipoEntidad {
        RESERVA,
        MANTENIMIENTO,
        SOLICITUD_CANCELACION,
        REEMBOLSO
    }
}