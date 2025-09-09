package com.example.project.repository.coordinador;

import com.example.project.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeolocalizacionRepository extends JpaRepository<Geolocalizacion, Integer> {
    Optional<Geolocalizacion> findByCoordinadorAndFechaAndEstado(Usuarios coordinador, LocalDate fecha, EstadoGeo estado);
    List<Geolocalizacion> findByCoordinadorOrderByFechaDesc(Usuarios coordinador);
    Page<Geolocalizacion> findByCoordinador(Usuarios usuario, Pageable pageable);
    Page<Geolocalizacion> findByCoordinadorAndLugarExactoContainingIgnoreCase(Usuarios usuario, String lugarExacto, Pageable pageable);
    Geolocalizacion findTopByCoordinadorOrderByFechaDesc(Usuarios usuario);

    List<Geolocalizacion> findByCoordinadorAndFechaBetween(Usuarios coordinador, LocalDate desde, LocalDate hasta);

    Optional<Geolocalizacion> findFirstByCoordinadorOrderByFechaDesc(Usuarios coordinador);

}