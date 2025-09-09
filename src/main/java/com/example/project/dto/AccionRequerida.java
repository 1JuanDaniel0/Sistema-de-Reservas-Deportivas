package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccionRequerida {
    private TipoAccion tipo;
    private String descripcion;
    private String entidadAfectada;
    private Integer cantidadAfectada;
    private boolean esOpcional;
    private String solucionSugerida;

    public enum TipoAccion {
        REASIGNAR_AUTOMATICO,
        SELECCIONAR_COORDINADOR,
        ASIGNAR_NUEVO_COORDINADOR,
        CANCELAR_ENTIDADES,
        BLOQUEAR_CAMBIO
    }
}