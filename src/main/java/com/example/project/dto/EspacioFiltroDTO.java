package com.example.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EspacioFiltroDTO {
    private String tipo;
    private List<String> deportes;
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private Integer precioMin;
    private Integer precioMax;
    private Integer estrellasMin;
    private Integer lugarId;
    private String nombre;
}
