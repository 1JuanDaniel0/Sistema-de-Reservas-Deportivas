package com.example.project.repository.coordinador;

import com.example.project.entity.EstadoEspacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoEspacioRepositoryCoord extends JpaRepository<EstadoEspacio, Integer> {
    EstadoEspacio findByEstado(String estado);
}