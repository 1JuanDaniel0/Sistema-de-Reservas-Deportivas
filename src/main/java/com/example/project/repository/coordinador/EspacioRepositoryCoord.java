package com.example.project.repository.coordinador;

import com.example.project.entity.Espacio;
import com.example.project.entity.EstadoEspacio;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EspacioRepositoryCoord extends JpaRepository<Espacio, Integer> {
    @Query("SELECT COUNT(e) FROM Espacio e JOIN e.idLugar l JOIN l.coordinadores c WHERE c = :coordinador")
    int countEspaciosByCoordinador(@Param("coordinador") Usuarios coordinador);
    @Query("SELECT COUNT(e) FROM Espacio e JOIN e.idLugar l JOIN l.coordinadores c WHERE c = :coordinador AND e.observaciones IS NOT NULL AND e.observaciones <> ''")
    int countObservacionesEspacios(@Param("coordinador") Usuarios coordinador);
    int countEspaciosByIdEstadoEspacio_IdEstadoEspacio(int idEstadoEspacio);
}