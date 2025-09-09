package com.example.project.repository;

import com.example.project.entity.Espacio;
import com.example.project.entity.ObservacionEspacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ObservacionEspacioRepository extends JpaRepository<ObservacionEspacio, Integer> {

    long countByFechaAndEspacioIn(LocalDateTime fecha, List<Espacio> espacios);
    List<ObservacionEspacio> findByFecha(LocalDateTime fecha);
    long countByUsuario_IdUsuariosAndFecha(Integer usuarioId, LocalDateTime fecha);
    List<ObservacionEspacio> findByEspacio_IdEspacioOrderByFechaDesc(Integer espacioId);
    long countByUsuario_IdUsuariosAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);
}
