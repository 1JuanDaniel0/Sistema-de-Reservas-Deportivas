package com.example.project.service;
import com.example.project.dto.EspacioFiltroDTO;
import com.example.project.entity.Espacio;
import com.example.project.entity.Mantenimiento;
import com.example.project.entity.Reserva;
import com.example.project.repository.CalificacionRepository;
import com.example.project.repository.EspacioRepositoryGeneral;
import com.example.project.repository.MantenimientoRepository;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EspacioService {
    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private ReservaRepositoryVecino reservaRepositoryVecino;
    @Autowired private MantenimientoRepository mantenimientoRepository;
    public List<String> consultarEspaciosDisponibles(String fecha, String horaInicio, String horaFin) {
        List<String> disponibles = new ArrayList<>();
        try {
            LocalDate f = LocalDate.parse(fecha);
            LocalTime hIni = LocalTime.parse(horaInicio);
            LocalTime hFin = LocalTime.parse(horaFin);
            LocalDateTime inicio = LocalDateTime.of(f, hIni);
            LocalDateTime fin = LocalDateTime.of(f, hFin);
            List<Espacio> espacios = espacioRepository.findAll();
            for (Espacio espacio : espacios) {
                if (!"Disponible".equalsIgnoreCase(espacio.getIdEstadoEspacio().getEstado())) {
                    continue; // solo espacios marcados como disponibles
                }

                List<Reserva> reservas = reservaRepositoryVecino.findByEspacio_IdEspacioAndFecha(espacio.getIdEspacio(), f);
                boolean ocupado = false;
                for (Reserva r : reservas) {
                    LocalDateTime rInicio = LocalDateTime.of(r.getFecha(), r.getHoraInicio());
                    LocalDateTime rFin = LocalDateTime.of(r.getFecha(), r.getHoraFin());

                    if (inicio.isBefore(rFin) && fin.isAfter(rInicio)) {
                        ocupado = true;
                        break;
                    }
                }
                if (!ocupado) { disponibles.add(espacio.getNombre()); }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return disponibles;
    }

    public List<Espacio> buscarEspaciosDisponibles(String fecha, String horaInicio, String horaFin, int idLugar) {
        List<Espacio> disponibles = new ArrayList<>();
        try {
            LocalDate f = LocalDate.parse(fecha);
            LocalTime hIni = LocalTime.parse(horaInicio);
            LocalTime hFin = LocalTime.parse(horaFin);
            LocalDateTime inicio = LocalDateTime.of(f, hIni);
            LocalDateTime fin = LocalDateTime.of(f, hFin);
            // Obtener espacios del lugar específico
            List<Espacio> espacios = espacioRepository.findAll().stream()
                    .filter(e -> e.getIdLugar().getIdLugar() == idLugar)
                    .toList();
            for (Espacio espacio : espacios) {
                // Verificar si el espacio está marcado como disponible
                if (!"Disponible".equalsIgnoreCase(espacio.getIdEstadoEspacio().getEstado())) {
                    continue;
                }
                // Buscar reservas para este espacio en la fecha específica
                List<Reserva> reservas = reservaRepositoryVecino.findByEspacio_IdEspacioAndFecha(
                        espacio.getIdEspacio(),
                        f
                );
                // Verificar si hay solapamiento con alguna reserva existente
                boolean ocupado = false;
                for (Reserva r : reservas) {
                    LocalDateTime rInicio = LocalDateTime.of(r.getFecha(), r.getHoraInicio());
                    LocalDateTime rFin = LocalDateTime.of(r.getFecha(), r.getHoraFin());

                    if (inicio.isBefore(rFin) && fin.isAfter(rInicio)) {
                        ocupado = true;
                        break;
                    }
                }
                // Si no está ocupado, agregarlo a la lista de disponibles
                if (!ocupado) { disponibles.add(espacio); }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return disponibles;
    }

    @Autowired private CalificacionRepository calificacionRepository;

    public Page<Espacio> buscarEspaciosConFiltros(EspacioFiltroDTO filtro, Pageable pageable) {
        List<Espacio> espacios = espacioRepository.findAllByEstadoWithDeportesAndTipoEspacio(1);

        // Filtro por nombre de espacio
        if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
            String nombre = filtro.getNombre().toLowerCase();
            espacios = espacios.stream()
                    .filter(e -> e.getNombre() != null &&
                            e.getNombre().toLowerCase().contains(nombre))
                    .collect(Collectors.toList());
        }

        // filtro por tipo
        if (filtro.getTipo() != null && !filtro.getTipo().isEmpty()) {
            espacios = espacios.stream()
                    .filter(e -> e.getTipoEspacio() != null &&
                            e.getTipoEspacio().getNombre().toLowerCase().contains(filtro.getTipo().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // filtro por deportes
        if (filtro.getDeportes() != null && !filtro.getDeportes().isEmpty()) {
            espacios = espacios.stream()
                    .filter(e -> e.getDeportes() != null &&
                            e.getDeportes().stream().anyMatch(d -> filtro.getDeportes().contains(d.getNombre())))
                    .collect(Collectors.toList());
        }

        // filtro de costo por hora
        if (filtro.getPrecioMin() != null && filtro.getPrecioMax() != null) {
            espacios = espacios.stream()
                    .filter(e -> e.getCosto() != null &&
                            e.getCosto() >= filtro.getPrecioMin() &&
                            e.getCosto() <= filtro.getPrecioMax())
                    .collect(Collectors.toList());
        }

        // Filtro estrellas mínimas
        if (filtro.getEstrellasMin() != null) {
            espacios = espacios.stream()
                    .filter(e -> {
                        Double promedio = calificacionRepository.promedioPorEspacio(e.getIdEspacio());
                        return promedio != null && promedio >= filtro.getEstrellasMin();
                    })
                    .collect(Collectors.toList());
        }

        if (filtro.getLugarId() != null) {
            espacios = espacios.stream()
                    .filter(e -> e.getIdLugar() != null &&
                            e.getIdLugar().getIdLugar().equals(filtro.getLugarId()))
                    .collect(Collectors.toList());
        }

        // paginar manualmente (opcional mientras no se use JPA dynamic query)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), espacios.size());
        List<Espacio> paginados = espacios.subList(start, end);
        return new PageImpl<>(paginados, pageable, espacios.size());
    }

    public boolean estaEnMantenimiento(Espacio espacio, LocalDate fecha, LocalTime hora) {
        List<Mantenimiento> mantenimientosActivos = mantenimientoRepository
                .findMantenimientosActivosEnFechaHora(espacio, fecha, hora);

        return !mantenimientosActivos.isEmpty();
    }

    public List<Espacio> obtenerEspaciosDisponibles(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        List<Espacio> todosLosEspacios = espacioRepository.findAllByEstadoWithDeportesAndTipoEspacio(1);

        return todosLosEspacios.stream()
                .filter(espacio -> !estaEnMantenimiento(espacio, fecha, horaInicio))
                .collect(Collectors.toList());
    }

}