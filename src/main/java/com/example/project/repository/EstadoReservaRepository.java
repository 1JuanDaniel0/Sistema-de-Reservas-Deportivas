package com.example.project.repository;

import com.example.project.entity.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoReservaRepository extends JpaRepository<EstadoReserva, Integer> {

    /**
     * Buscar estado de reserva por nombre del estado
     * @param estado Nombre del estado (ej: "Confirmada", "Cancelada", etc.)
     * @return EstadoReserva encontrado o null si no existe
     */
    EstadoReserva findByEstado(String estado);

    /**
     * Verificar si existe un estado con el nombre dado
     * @param estado Nombre del estado
     * @return true si existe, false si no
     */
    boolean existsByEstado(String estado);
}