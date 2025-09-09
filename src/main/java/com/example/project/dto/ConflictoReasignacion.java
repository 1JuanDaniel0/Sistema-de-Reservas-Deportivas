package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConflictoReasignacion {
    private String tipoConflicto;
    private String descripcion;
    private String entidadAfectada;
    private String posibleSolucion;
    private boolean esBloquente;
}