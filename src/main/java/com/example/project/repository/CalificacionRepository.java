package com.example.project.repository;

import com.example.project.entity.Calificacion;
import com.example.project.entity.Espacio;
import com.example.project.entity.Reserva;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, Integer> {

    Optional<Calificacion> findByReserva(Reserva reserva);

    List<Calificacion> findByEspacio(Espacio espacio);

    boolean existsByReserva(Reserva reserva);

    @Query("SELECT AVG(c.puntaje) FROM Calificacion c WHERE c.reserva.espacio.idEspacio = :idEspacio")
    Double promedioPorEspacio(@Param("idEspacio") Integer idEspacio);

    @Query("SELECT AVG(c.puntaje) FROM Calificacion c")
    Double promedioGeneral();
}
