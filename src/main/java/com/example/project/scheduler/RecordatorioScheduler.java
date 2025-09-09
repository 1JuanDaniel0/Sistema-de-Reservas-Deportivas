package com.example.project.scheduler;

import com.example.project.entity.Reserva;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import com.example.project.service.MailManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class RecordatorioScheduler {

    @Autowired
    private ReservaRepositoryVecino reservaRepository;

    @Autowired
    private MailManager mailManager;

    @Scheduled(cron = "0 0 7 * * *", zone = "America/Lima") // todos los días a las 7:00 AM hora de Perú
    @Transactional
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now(ZoneId.of("America/Lima")).plusDays(1);
        List<Reserva> reservas = reservaRepository.findByEstado_EstadoAndFecha("Confirmada", manana);

        System.out.println("[SCHEDULER] Enviando recordatorios para " + reservas.size() + " reservas de " + manana);

        for (Reserva reserva : reservas) {
            try {
                String correo = reserva.getVecino().getCorreo();
                mailManager.enviarRecordatorioReserva(correo, reserva);
                System.out.println("[SCHEDULER] 📧 Recordatorio enviado a: " + correo);
            } catch (Exception e) {
                System.err.println("[SCHEDULER] ❌ Error al enviar recordatorio a " +
                        reserva.getVecino().getCorreo() + ": " + e.getMessage());
            }
        }
    }
}
