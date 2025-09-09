package com.example.project.repository.admin;

import java.time.LocalDate;
import java.time.LocalTime;

public interface DetallesReservaDto {
    Long getId();
    LocalDate getFecha();
    LocalTime getHoraInicio();
    LocalTime getHoraFin();
    String getTipoEspacio();
    String getNombreEspacio();
    String getFotoEspacio();
    String getCoordinadorNombre();
    String getVecinoNombre();
    String getEstado();
    String getTipoPago();
    Double getCosto();
    Integer getIdespacio();
}
