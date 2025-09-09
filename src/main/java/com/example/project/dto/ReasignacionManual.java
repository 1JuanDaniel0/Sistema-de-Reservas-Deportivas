package com.example.project.dto;

import com.example.project.entity.Lugar;
import com.example.project.entity.Usuarios;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReasignacionManual {
    private Lugar lugar;
    private List<String> entidadesAfectadas;
    private List<Usuarios> coordinadoresCandidatos;
    private Integer coordinadorSeleccionadoId;
    private boolean esObligatoria;
    private String descripcionImpacto;
}