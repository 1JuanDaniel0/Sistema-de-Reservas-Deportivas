package com.example.project.dto;

import com.example.project.entity.Usuarios;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReasignacionDetalle {
    private Integer entidadId;
    private String entidadTipo;
    private String entidadDescripcion;
    private Usuarios coordinadorOrigen;
    private Usuarios coordinadorDestino;
    private String razonReasignacion;
    private boolean esAutomatica;
}