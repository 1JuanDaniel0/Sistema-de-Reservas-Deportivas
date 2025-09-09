package com.example.project.repository.admin;

public interface EspacioDto {
    Integer getIdEspacio();
    String getNombre();
    String getNombreLugar();     // nombre del lugar (alias en el SQL)
    String getEstadoEspacio();   // nombre del estado del espacio (alias en el SQL)
    String getTipo();
    Double getCosto();
    String getNombreTipo();
}
