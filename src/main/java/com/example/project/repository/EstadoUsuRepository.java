package com.example.project.repository;

import com.example.project.entity.EstadoUsu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoUsuRepository extends JpaRepository<EstadoUsu, Integer> {
    EstadoUsu findByEstado(String estado);
}
