package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResultadoReasignacion {
    private boolean exitoso;
    private String mensaje;
    private Map<String, Integer> entidadesReasignadas;
    private List<String> errores;
    private List<String> advertencias;
    private LocalDateTime fechaEjecucion;
    private String resumenCompleto;
}