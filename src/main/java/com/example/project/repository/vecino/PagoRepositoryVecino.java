package com.example.project.repository.vecino;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.project.entity.Espacio;
import com.example.project.entity.Reserva;
import com.example.project.entity.Pago;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepositoryVecino extends JpaRepository<Pago, Integer>{
    List<Pago> findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(int idUsuario);

    Optional<Pago> findByReserva(Reserva reserva);
}
