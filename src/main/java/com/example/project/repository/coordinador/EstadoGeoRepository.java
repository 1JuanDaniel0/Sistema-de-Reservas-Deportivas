package com.example.project.repository.coordinador;

import com.example.project.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoGeoRepository extends JpaRepository<EstadoGeo, Integer> {
    Optional<EstadoGeo> findByEstado(String estado);
}