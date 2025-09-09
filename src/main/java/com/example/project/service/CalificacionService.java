package com.example.project.service;

import com.example.project.entity.*;
import com.example.project.repository.CalificacionRepository;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CalificacionService {

    @Autowired
    private CalificacionRepository calificacionRepository;

    @Autowired
    private ReservaRepositoryVecino reservaRepository;

    @Transactional
    public String calificarReserva(Integer idReserva, Usuarios vecino, double puntaje, String comentario) {
        Reserva reserva = reservaRepository.findById(idReserva).orElse(null);

        if (reserva == null || !reserva.getVecino().getIdUsuarios().equals(vecino.getIdUsuarios())) {
            return "Reserva inválida o no pertenece al usuario.";
        }

        if (LocalDateTime.now().isBefore(reserva.getFecha().atTime(reserva.getHoraFin()))) {
            return "No se puede calificar una reserva que aún no ha concluido.";
        }

        if (calificacionRepository.existsByReserva(reserva)) {
            return "Ya se ha calificado esta reserva.";
        }

        Calificacion calificacion = new Calificacion();
        calificacion.setReserva(reserva);
        calificacion.setEspacio(reserva.getEspacio());
        calificacion.setVecino(vecino);
        calificacion.setPuntaje(puntaje);
        calificacion.setComentario(comentario);
        calificacion.setFechaCalificacion(LocalDateTime.now());

        calificacionRepository.save(calificacion);

        return "ok";
    }
}
