package com.example.project.repository.admin;

public interface ServiciosDeportivosDto {
    Integer getId();            // r.idReserva AS ID
    String getTipo();           // e.tipo AS Tipo
    String getLugar();          // l.lugar AS Lugar
    Double getCosto();          // r.costo AS Costo
    String getEstadoEspacio();  // ee.estado AS EstadoEspacio
    String getFoto();
    String getObservaciones();
    String getNombre();
}
