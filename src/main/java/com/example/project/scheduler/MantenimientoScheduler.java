package com.example.project.scheduler;

import com.example.project.entity.Espacio;
import com.example.project.entity.EstadoEspacio;
import com.example.project.entity.Mantenimiento;
import com.example.project.repository.EspacioRepositoryGeneral;
import com.example.project.repository.EstadoEspacioRepository;
import com.example.project.repository.MantenimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@EnableScheduling
public class MantenimientoScheduler {

    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private EstadoEspacioRepository estadoEspacioRepository;

    // Se ejecuta cada 5 minutos
    @Scheduled(fixedRate = 300000) // 5 minutos = 300,000 ms
    public void actualizarEstadosEspacios() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = LocalDate.now();
        LocalTime horaActual = LocalTime.now();

        System.out.println("🔄 Verificando estados de mantenimiento: " + ahora);

        try {
            List<Mantenimiento> mantenimientosParaIniciar = mantenimientoRepository
                    .findMantenimientosParaIniciar(hoy, horaActual);

            for (Mantenimiento mantenimiento : mantenimientosParaIniciar) {
                mantenimiento.iniciarMantenimiento();
                mantenimientoRepository.save(mantenimiento);

                // Cambiar estado del espacio a "Mantenimiento" (ID = 2)
                Espacio espacio = mantenimiento.getEspacio();
                EstadoEspacio estadoMantenimiento = estadoEspacioRepository.findById(2).orElse(null);
                if (estadoMantenimiento != null) {
                    espacio.setIdEstadoEspacio(estadoMantenimiento);
                    espacioRepository.save(espacio);
                    System.out.println("🔧 Espacio " + espacio.getNombre() + " cambiado a MANTENIMIENTO");
                }
            }

            // 2. FINALIZAR mantenimientos que ya terminaron
            List<Mantenimiento> mantenimientosParaFinalizar = mantenimientoRepository
                    .findMantenimientosParaFinalizar(hoy, horaActual);

            for (Mantenimiento mantenimiento : mantenimientosParaFinalizar) {
                mantenimiento.setEstado(Mantenimiento.EstadoMantenimiento.COMPLETADO);
                mantenimientoRepository.save(mantenimiento);

                // Cambiar estado del espacio a "Disponible" (ID = 1)
                Espacio espacio = mantenimiento.getEspacio();
                EstadoEspacio estadoDisponible = estadoEspacioRepository.findById(1).orElse(null);
                if (estadoDisponible != null) {
                    espacio.setIdEstadoEspacio(estadoDisponible);
                    espacioRepository.save(espacio);
                    System.out.println("✅ Espacio " + espacio.getNombre() + " cambiado a DISPONIBLE");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error en scheduler de mantenimiento: " + e.getMessage());
            e.printStackTrace();
        }
    }
}