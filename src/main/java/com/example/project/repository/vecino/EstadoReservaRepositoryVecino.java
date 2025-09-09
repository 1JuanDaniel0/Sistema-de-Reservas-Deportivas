package com.example.project.repository.vecino;
import com.example.project.entity.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoReservaRepositoryVecino extends JpaRepository<EstadoReserva, Integer> {
    EstadoReserva findByEstado(String estado);
}
