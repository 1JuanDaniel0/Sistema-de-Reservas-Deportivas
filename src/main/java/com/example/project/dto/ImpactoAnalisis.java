package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ImpactoAnalisis {
    private List<LugarImpacto> lugaresAfectados;
    private EstrategiaReasignacion estrategiaRecomendada;
    private List<AccionRequerida> accionesRequeridas;
    private boolean puedeRealizarseCambio;
    private String motivoBloqueo;
    private Map<String, Object> estadisticas;

    public enum EstrategiaReasignacion {
        AUTOMATICA,
        ASISTIDA,
        BLOQUEADA
    }
}