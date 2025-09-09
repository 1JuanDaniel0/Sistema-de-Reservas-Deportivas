package com.example.project.service;

import com.example.project.dto.MantenimientoDTO;
import com.example.project.entity.*;
import com.example.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MantenimientoService {

    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private ReservaRepository reservaRepository;

    /**
     * Verificar si existe conflicto entre una reserva propuesta y mantenimientos activos
     */
    public void verificarConflictosConMantenimientos(Espacio espacio, LocalDate fecha,
                                                     LocalTime horaInicio, LocalTime horaFin)
            throws IllegalArgumentException {

        System.out.println("🔍 Verificando conflictos con mantenimientos para reserva...");
        System.out.println("Espacio: " + espacio.getNombre());
        System.out.println("Fecha: " + fecha);
        System.out.println("Horario: " + horaInicio + " - " + horaFin);

        // Buscar mantenimientos activos que afecten el espacio en la fecha/hora específica
        List<Mantenimiento> mantenimientosConflictivos =
                mantenimientoRepository.findConflictosMantenimientoEnFecha(
                        espacio, fecha, horaInicio, horaFin);

        if (!mantenimientosConflictivos.isEmpty()) {
            System.out.println("⚠️ Conflictos encontrados con mantenimientos:");

            String detallesConflictos = mantenimientosConflictivos.stream()
                    .map(m -> String.format(
                            "• %s (%s) programado del %s al %s de %s a %s - Prioridad: %s",
                            m.getTipoMantenimiento().getDescripcion(),
                            m.getEstado().getDescripcion(),
                            m.getFechaInicio(),
                            m.getFechaFin(),
                            m.getHoraInicio(),
                            m.getHoraFin(),
                            m.getPrioridad().getDescripcion()
                    ))
                    .collect(Collectors.joining("\n"));

            throw new IllegalArgumentException(
                    "No se puede crear la reserva debido a mantenimientos programados:\n" +
                            detallesConflictos
            );
        }

        System.out.println("✅ No hay conflictos con mantenimientos");
    }

    /**
     * Programar un nuevo mantenimiento con validaciones
     */
    public Mantenimiento programarMantenimiento(MantenimientoDTO dto, Usuarios administrador)
            throws IllegalArgumentException {

        System.out.println("=== PROGRAMANDO MANTENIMIENTO ===");
        System.out.println("DTO recibido: " + dto.getEspacioId() + " - " + dto.getFechaInicio() + " a " + dto.getFechaFin());

        // 1. Validaciones básicas
        validarDatos(dto);

        // 2. Buscar espacio
        Espacio espacio = espacioRepository.findById(dto.getEspacioId())
                .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));

        // 3. Convertir strings a LocalDate y LocalTime
        LocalDate fechaInicio = dto.getFechaInicioAsLocalDate();
        LocalDate fechaFin = dto.getFechaFinAsLocalDate();
        LocalTime horaInicio = dto.getHoraInicioAsLocalTime();
        LocalTime horaFin = dto.getHoraFinAsLocalTime();

        // 4. Validar fechas y horas convertidas
        validarFechasYHoras(fechaInicio, fechaFin, horaInicio, horaFin);

        // 5. Crear DTO temporal para validaciones (usando los métodos originales)
        MantenimientoDTO dtoParaValidacion = new MantenimientoDTO();
        dtoParaValidacion.setEspacioId(dto.getEspacioId());
        dtoParaValidacion.setFechaInicio(dto.getFechaInicio());
        dtoParaValidacion.setFechaFin(dto.getFechaFin());
        dtoParaValidacion.setHoraInicio(dto.getHoraInicio());
        dtoParaValidacion.setHoraFin(dto.getHoraFin());

        // 6. Verificar conflictos con reservas existentes
        verificarConflictosReservas(espacio, fechaInicio, fechaFin, horaInicio, horaFin);

        // 7. Verificar conflictos con otros mantenimientos
        verificarConflictosMantenimientos(espacio, fechaInicio, fechaFin, horaInicio, horaFin, null);

        // 8. Buscar responsable si se especifica
        Usuarios responsable = null;
        if (dto.getResponsableId() != null) {
            responsable = usuariosRepository.findById(dto.getResponsableId())
                    .orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado"));
        }

        // 9. Crear entidad de mantenimiento
        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setEspacio(espacio);
        mantenimiento.setFechaInicio(fechaInicio);
        mantenimiento.setFechaFin(fechaFin);
        mantenimiento.setHoraInicio(horaInicio);
        mantenimiento.setHoraFin(horaFin);

        // Convertir tipo de mantenimiento
        mantenimiento.setTipoMantenimiento(convertirTipoMantenimiento(dto.getTipoMantenimiento()));

        // Convertir prioridad
        mantenimiento.setPrioridad(convertirPrioridad(dto.getPrioridad()));

        mantenimiento.setDescripcion(dto.getDescripcion());
        mantenimiento.setResponsable(responsable);
        mantenimiento.setCreadoPor(administrador);
        mantenimiento.setCostoEstimado(dto.getCostoEstimado());
        mantenimiento.setEstado(Mantenimiento.EstadoMantenimiento.PROGRAMADO);

        // 10. Guardar
        Mantenimiento saved = mantenimientoRepository.save(mantenimiento);
        System.out.println("✅ Mantenimiento programado exitosamente: ID " + saved.getIdMantenimiento());

        return saved;
    }

    public void verificarConflictosReservas(Espacio espacio, LocalDate fechaInicio, LocalDate fechaFin,
                                            LocalTime horaInicio, LocalTime horaFin) {
        System.out.println("🔍 Verificando conflictos con reservas...");

        LocalDate fechaActual = fechaInicio;
        while (!fechaActual.isAfter(fechaFin)) {
            // Buscar reservas confirmadas en cada fecha
            List<Reserva> reservasEnFecha = reservaRepository.findConflictosEnHorario(
                    espacio.getIdEspacio(),
                    fechaActual,
                    horaInicio,
                    horaFin
            );

            if (!reservasEnFecha.isEmpty()) {
                String conflictos = reservasEnFecha.stream()
                        .map(r -> String.format("Reserva #%d del %s de %s a %s",
                                r.getIdReserva(), r.getFecha(), r.getHoraInicio(), r.getHoraFin()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                throw new IllegalArgumentException(
                        "Existe conflicto con reservas confirmadas en " + fechaActual + ": " + conflictos
                );
            }

            fechaActual = fechaActual.plusDays(1);
        }

        System.out.println("✅ No se encontraron conflictos con reservas");
    }

    public void verificarConflictosMantenimientos(Espacio espacio, LocalDate fechaInicio, LocalDate fechaFin,
                                                  LocalTime horaInicio, LocalTime horaFin, Integer mantenimientoIdActual) {
        System.out.println("🔍 Verificando conflictos con otros mantenimientos...");

        List<Mantenimiento> conflictos = mantenimientoRepository.findConflictosMantenimiento(
                espacio, fechaInicio, fechaFin, horaInicio, horaFin
        );

        // Filtrar el mantenimiento actual si estamos editando
        if (mantenimientoIdActual != null) {
            conflictos = conflictos.stream()
                    .filter(m -> !m.getIdMantenimiento().equals(mantenimientoIdActual))
                    .toList();
        }

        if (!conflictos.isEmpty()) {
            String mensajeConflictos = conflictos.stream()
                    .map(m -> String.format("Mantenimiento #%d (%s) del %s al %s",
                            m.getIdMantenimiento(), m.getTipoMantenimiento(),
                            m.getFechaInicio(), m.getFechaFin()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            throw new IllegalArgumentException(
                    "Existe conflicto con otros mantenimientos programados: " + mensajeConflictos
            );
        }

        System.out.println("✅ No se encontraron conflictos con otros mantenimientos");
    }

    private void validarFechasYHoras(LocalDate fechaInicio, LocalDate fechaFin, LocalTime horaInicio, LocalTime horaFin) {
        // No permitir fechas pasadas
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede programar mantenimiento en fechas pasadas");
        }

        // Fecha fin debe ser igual o posterior a fecha inicio
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser igual o posterior a la fecha de inicio");
        }

        // Hora fin debe ser posterior a hora inicio
        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        // Validar horario de operación (8 AM - 10 PM)
        LocalTime horaMinima = LocalTime.of(8, 0);
        LocalTime horaMaxima = LocalTime.of(22, 0);

        if (horaInicio.isBefore(horaMinima) || horaInicio.isAfter(horaMaxima)) {
            throw new IllegalArgumentException("La hora de inicio debe estar entre 8:00 AM y 10:00 PM");
        }

        if (horaFin.isBefore(horaMinima) || horaFin.isAfter(horaMaxima)) {
            throw new IllegalArgumentException("La hora de fin debe estar entre 8:00 AM y 10:00 PM");
        }
    }

    /**
     * Obtener mantenimientos de un espacio para mostrar en el calendario
     */
    public List<Mantenimiento> obtenerMantenimientosParaCalendario(Integer espacioId,
                                                                   LocalDate fechaInicio,
                                                                   LocalDate fechaFin) {
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));

        return mantenimientoRepository.findByEspacioAndFechaBetween(espacio, fechaInicio, fechaFin);
    }

    /**
     * Obtener mantenimiento por ID
     */
    public Optional<Mantenimiento> obtenerPorId(Integer id) {
        return mantenimientoRepository.findById(id);
    }

    /**
     * Validaciones básicas de datos
     */
    private void validarDatos(MantenimientoDTO dto) {
        if (dto.getEspacioId() == null) {
            throw new IllegalArgumentException("El espacio es obligatorio");
        }
        if (dto.getFechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (dto.getFechaFin() == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria");
        }
        if (dto.getHoraInicio() == null) {
            throw new IllegalArgumentException("La hora de inicio es obligatoria");
        }
        if (dto.getHoraFin() == null) {
            throw new IllegalArgumentException("La hora de fin es obligatoria");
        }
        if (dto.getTipoMantenimiento() == null || dto.getTipoMantenimiento().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de mantenimiento es obligatorio");
        }
        if (dto.getPrioridad() == null || dto.getPrioridad().trim().isEmpty()) {
            throw new IllegalArgumentException("La prioridad es obligatoria");
        }
        if (dto.getDescripcion() == null || dto.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
    }

    /**
     * Convertir string a enum TipoMantenimiento
     */
    private Mantenimiento.TipoMantenimiento convertirTipoMantenimiento(String tipo) {
        try {
            return Mantenimiento.TipoMantenimiento.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de mantenimiento inválido: " + tipo);
        }
    }

    /**
     * Convertir string a enum PrioridadMantenimiento
     */
    private Mantenimiento.PrioridadMantenimiento convertirPrioridad(String prioridad) {
        try {
            return Mantenimiento.PrioridadMantenimiento.valueOf(prioridad.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Prioridad inválida: " + prioridad);
        }
    }
}