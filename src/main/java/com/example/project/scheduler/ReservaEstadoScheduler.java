package com.example.project.scheduler;

import com.example.project.entity.EstadoReserva;
import com.example.project.entity.Reserva;
import com.example.project.repository.vecino.EstadoReservaRepositoryVecino;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class ReservaEstadoScheduler {

    @Autowired
    private ReservaRepositoryVecino reservaRepositoryVecino;

    @Autowired
    private EstadoReservaRepositoryVecino estadoReservaRepositoryVecino;

    @Scheduled(cron = "0 * * * * *") // cada minuto
    public void actualizarReservasPasadas() {
        ZoneId zonaPeru = ZoneId.of("America/Lima");
        LocalDateTime ahora = LocalDateTime.now(zonaPeru);

        // Buscar todas las reservas en estado 'Confirmada' (1) o 'No confirmada' (2)
        List<Reserva> reservasActivas = reservaRepositoryVecino.findByEstado_IdEstadoReservaIn(List.of(1, 2));

        for (Reserva reserva : reservasActivas) {
            LocalDateTime finReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraFin());
            if (finReserva.isBefore(ahora)) {
                EstadoReserva pasada = estadoReservaRepositoryVecino.findById(3).orElse(null);
                if (pasada != null) {
                    reserva.setEstado(pasada);
                    reservaRepositoryVecino.save(reserva);
                    System.out.println("Reserva ID " + reserva.getIdReserva() + " actualizada a estado 'Pasada'.");

                }
            }
        }
    }
}
