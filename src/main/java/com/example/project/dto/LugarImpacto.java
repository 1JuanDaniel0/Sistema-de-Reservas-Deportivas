package com.example.project.dto;

import com.example.project.entity.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LugarImpacto {
    private Lugar lugar;
    private List<Reserva> reservasAfectadas;
    private List<Mantenimiento> mantenimientosAfectados;
    private List<SolicitudCancelacion> solicitudesAfectadas;
    private List<Reembolso> reembolsosAfectados;
    private List<Usuarios> coordinadoresDisponibles;
    private boolean requiereIntervencionManual;
    private int totalEntidadesAfectadas;
    private String razonProblema;
}