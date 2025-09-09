package com.example.project.controller.api;

import com.example.project.dto.ReservaCalendarioDto;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.vecino.EspacioRepositoryVecino;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vecino/api/calendario")
public class VecinoCalendarioApiController {

    @Autowired private EspacioRepositoryVecino espacioRepositoryVecino;
    @Autowired private ReservaRepositoryVecino reservaRepositoryVecino;
    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private UsuariosRepository usuariosRepository;

    /**
     * Obtener lista de espacios disponibles para el vecino
     */
    @GetMapping("/espacios")
    public ResponseEntity<List<Map<String, Object>>> obtenerEspacios(HttpSession session) {
        try {
            System.out.println("🏢 [VECINO] Cargando lista de espacios...");

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                System.err.println("❌ [VECINO] Usuario no autenticado");
                return ResponseEntity.status(401).build();
            }

            // Obtener todos los espacios activos (estado 1 = activo)
            List<Espacio> espacios = espacioRepositoryVecino.findAllByEstadoWithDeportesAndTipoEspacio(1);

            List<Map<String, Object>> resultado = espacios.stream()
                    .map(espacio -> {
                        Map<String, Object> espacioMap = new HashMap<>();
                        espacioMap.put("idEspacio", espacio.getIdEspacio());
                        espacioMap.put("nombre", espacio.getNombre());
                        espacioMap.put("descripcion", espacio.getDescripcion());
                        espacioMap.put("costo", espacio.getCosto());
                        espacioMap.put("tipo", espacio.getTipoEspacio() != null ?
                                espacio.getTipoEspacio().getNombre() : "");
                        espacioMap.put("lugar", espacio.getIdLugar() != null ?
                                espacio.getIdLugar().getLugar() : "");
                        espacioMap.put("foto1Url", espacio.getFoto1Url());
                        return espacioMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ [VECINO] Espacios cargados: " + resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ [VECINO] Error al cargar espacios: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener eventos del calendario para un espacio específico
     */
    @GetMapping("/espacios/{idEspacio}/eventos")
    public ResponseEntity<Map<String, Object>> obtenerEventosEspacio(
            @PathVariable Integer idEspacio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            HttpSession session) {

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            System.out.println("🔍 [" + requestId + "] === OBTENIENDO EVENTOS PARA ESPACIO " + idEspacio + " ===");

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                System.err.println("❌ [" + requestId + "] Usuario no autenticado");
                return ResponseEntity.status(401).build();
            }

            // Buscar espacio
            Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);
            if (espacio == null) {
                System.err.println("❌ [" + requestId + "] Espacio no encontrado: " + idEspacio);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Espacio no encontrado"));
            }

            // Fechas por defecto si no se especifican
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now();
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now().plusMonths(6);
            }

            System.out.println("📅 [" + requestId + "] Rango de fechas: " + fechaInicio + " a " + fechaFin);

            // Obtener reservas del espacio
            List<String> estadosPermitidos = Arrays.asList("Confirmada", "Pendiente de confirmación");
            List<ReservaCalendarioDto> reservasDto = reservaRepositoryVecino.buscarReservasParaCalendario(
                    (long) idEspacio,
                    estadosPermitidos,
                    fechaInicio,
                    fechaFin
            );

            // Marcar cuáles son propias del usuario
            for (ReservaCalendarioDto reserva : reservasDto) {
                boolean esPropia = reserva.getIdVecino() != null &&
                        reserva.getIdVecino().equals(usuario.getIdUsuarios());
                reserva.setEsPropia(esPropia);
            }

            System.out.println("📊 [" + requestId + "] Reservas encontradas: " + reservasDto.size());

            // Obtener mantenimientos activos del espacio
            List<Mantenimiento> mantenimientos = mantenimientoRepository.findByEspacioAndFechaBetween(
                            espacio, fechaInicio, fechaFin
                    ).stream()
                    .filter(m -> m.getEstado() == Mantenimiento.EstadoMantenimiento.PROGRAMADO ||
                            m.getEstado() == Mantenimiento.EstadoMantenimiento.EN_PROCESO)
                    .collect(Collectors.toList());

            System.out.println("🔧 [" + requestId + "] Mantenimientos encontrados: " + mantenimientos.size());

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("espacio", Map.of(
                    "idEspacio", espacio.getIdEspacio(),
                    "nombre", espacio.getNombre(),
                    "descripcion", espacio.getDescripcion() != null ? espacio.getDescripcion() : "",
                    "costo", espacio.getCosto(),
                    "lugar", espacio.getIdLugar() != null ? espacio.getIdLugar().getLugar() : "",
                    "foto1Url", espacio.getFoto1Url() != null ? espacio.getFoto1Url() : ""
            ));
            response.put("reservas", reservasDto);
            response.put("mantenimientos", convertirMantenimientosParaJSON(mantenimientos));
            response.put("fechaInicio", fechaInicio.toString());
            response.put("fechaFin", fechaFin.toString());
            response.put("usuarioId", usuario.getIdUsuarios());

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ [" + requestId + "] Eventos obtenidos en " + duration + "ms");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("❌ [" + requestId + "] Error después de " + duration + "ms: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error obteniendo eventos: " + e.getMessage()));
        }
    }

    /**
     * Verificar disponibilidad de un horario específico
     */
    @PostMapping("/espacios/{idEspacio}/verificar-disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @PathVariable Integer idEspacio,
            @RequestBody Map<String, String> request,
            HttpSession session) {

        try {
            System.out.println("🔍 [VECINO] Verificando disponibilidad para espacio: " + idEspacio);

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Obtener parámetros
            String fecha = request.get("fecha");
            String horaInicio = request.get("horaInicio");
            String horaFin = request.get("horaFin");

            if (fecha == null || horaInicio == null || horaFin == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("disponible", false, "mensaje", "Parámetros incompletos"));
            }

            // Buscar espacio
            Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);
            if (espacio == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("disponible", false, "mensaje", "Espacio no encontrado"));
            }

            LocalDate fechaReserva = LocalDate.parse(fecha);

            // Validar que no sea fecha pasada
            if (fechaReserva.isBefore(LocalDate.now())) {
                return ResponseEntity.ok(Map.of(
                        "disponible", false,
                        "mensaje", "No se puede reservar en fechas pasadas"
                ));
            }

            // Convertir horas a LocalTime
            java.time.LocalTime horaInicioTime = java.time.LocalTime.of(Integer.parseInt(horaInicio), 0);
            java.time.LocalTime horaFinTime = java.time.LocalTime.of(Integer.parseInt(horaFin), 0);

            // Verificar conflictos con reservas existentes
            List<Reserva> conflictos = reservaRepositoryVecino.findConflictosEnHorario(
                    idEspacio, fechaReserva, horaInicioTime, horaFinTime);

            if (!conflictos.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "disponible", false,
                        "mensaje", "El horario ya está reservado",
                        "conflictos", conflictos.size()
                ));
            }

            // Verificar conflictos con mantenimientos
            List<Mantenimiento> mantenimientosConflicto = mantenimientoRepository.findByEspacioAndFechaBetween(
                            espacio, fechaReserva, fechaReserva
                    ).stream()
                    .filter(m -> m.estaActivo() && m.afectaFechaHora(fechaReserva, horaInicioTime))
                    .collect(Collectors.toList());

            if (!mantenimientosConflicto.isEmpty()) {
                Mantenimiento primerConflicto = mantenimientosConflicto.get(0);
                return ResponseEntity.ok(Map.of(
                        "disponible", false,
                        "mensaje", "El espacio no está disponible debido a mantenimiento: " +
                                primerConflicto.getTipoMantenimiento().getDescripcion(),
                        "tipoConflicto", "mantenimiento"
                ));
            }

            // Si llegamos aquí, está disponible
            return ResponseEntity.ok(Map.of(
                    "disponible", true,
                    "mensaje", "Horario disponible para reserva"
            ));

        } catch (Exception e) {
            System.err.println("❌ [VECINO] Error verificando disponibilidad: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("disponible", false, "mensaje", "Error interno del servidor"));
        }
    }

    /**
     * Obtener información detallada de un espacio
     */
    @GetMapping("/espacios/{idEspacio}/detalle")
    public ResponseEntity<Map<String, Object>> obtenerDetalleEspacio(
            @PathVariable Integer idEspacio,
            HttpSession session) {

        try {
            System.out.println("🔍 [VECINO] Obteniendo detalle del espacio: " + idEspacio);

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Buscar espacio
            Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);
            if (espacio == null) {
                return ResponseEntity.notFound().build();
            }

            // Preparar información detallada
            Map<String, Object> detalles = new HashMap<>();
            detalles.put("idEspacio", espacio.getIdEspacio());
            detalles.put("nombre", espacio.getNombre());
            detalles.put("descripcion", espacio.getDescripcion());
            detalles.put("costo", espacio.getCosto());
            detalles.put("observaciones", espacio.getObservaciones());

            // Información del tipo de espacio
            if (espacio.getTipoEspacio() != null) {
                detalles.put("tipoEspacio", Map.of(
                        "id", espacio.getTipoEspacio().getIdTipoEspacio(),
                        "nombre", espacio.getTipoEspacio().getNombre()
                ));
            }

            // Información del lugar
            if (espacio.getIdLugar() != null) {
                detalles.put("lugar", Map.of(
                        "id", espacio.getIdLugar().getIdLugar(),
                        "nombre", espacio.getIdLugar().getLugar(),
                        "direccion", espacio.getIdLugar().getDireccion() != null ?
                                espacio.getIdLugar().getDireccion() : "",
                        "ubicacion", espacio.getIdLugar().getUbicacion() != null ?
                                espacio.getIdLugar().getUbicacion() : ""
                ));
            }

            // Deportes disponibles
            if (espacio.getDeportes() != null && !espacio.getDeportes().isEmpty()) {
                List<Map<String, ? extends Serializable>> deportes = espacio.getDeportes().stream()
                        .map(deporte -> Map.of(
                                "id", deporte.getIdDeporte(),
                                "nombre", deporte.getNombre()
                        ))
                        .collect(Collectors.toList());
                detalles.put("deportes", deportes);
            }

            // URLs de fotos
            detalles.put("fotos", Arrays.asList(
                    espacio.getFoto1Url(),
                    espacio.getFoto2Url(),
                    espacio.getFoto3Url()
            ).stream().filter(Objects::nonNull).collect(Collectors.toList()));

            // Coordinadores del lugar
            if (espacio.getIdLugar() != null && espacio.getIdLugar().getCoordinadores() != null) {
                List<Map<String, ? extends Serializable>> coordinadores = espacio.getIdLugar().getCoordinadores().stream()
                        .map(coordinador -> Map.of(
                                "id", coordinador.getIdUsuarios(),
                                "nombres", coordinador.getNombres() != null ? coordinador.getNombres() : "",
                                "apellidos", coordinador.getApellidos() != null ? coordinador.getApellidos() : "",
                                "nombreCompleto", (coordinador.getNombres() != null ? coordinador.getNombres() : "") +
                                        " " + (coordinador.getApellidos() != null ? coordinador.getApellidos() : "")
                        ))
                        .collect(Collectors.toList());
                detalles.put("coordinadores", coordinadores);
            }

            System.out.println("✅ [VECINO] Detalle del espacio obtenido exitosamente");
            return ResponseEntity.ok(detalles);

        } catch (Exception e) {
            System.err.println("❌ [VECINO] Error obteniendo detalle del espacio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error obteniendo detalle del espacio"));
        }
    }

    /**
     * Obtener estadísticas del espacio para el usuario
     */
    @GetMapping("/espacios/{idEspacio}/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasEspacio(
            @PathVariable Integer idEspacio,
            HttpSession session) {

        try {
            System.out.println("📊 [VECINO] Obteniendo estadísticas del espacio: " + idEspacio);

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Buscar espacio
            Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);
            if (espacio == null) {
                return ResponseEntity.notFound().build();
            }

            // Rango de fechas para estadísticas (últimos 3 meses y próximos 3 meses)
            LocalDate fechaInicio = LocalDate.now().minusMonths(3);
            LocalDate fechaFin = LocalDate.now().plusMonths(3);

            // Obtener reservas del usuario en este espacio
            List<String> estadosReservas = Arrays.asList("Confirmada", "Pendiente de confirmación", "Cancelada");
            List<ReservaCalendarioDto> reservasUsuario = reservaRepositoryVecino.buscarReservasParaCalendario(
                            (long) idEspacio,
                            estadosReservas,
                            fechaInicio,
                            fechaFin
                    ).stream()
                    .filter(reserva -> reserva.getIdVecino() != null && reserva.getIdVecino().equals(usuario.getIdUsuarios()))
                    .collect(Collectors.toList());

            // Calcular estadísticas del usuario
            long reservasConfirmadas = reservasUsuario.stream()
                    .filter(r -> "Confirmada".equals(r.getEstado()))
                    .count();

            long reservasPendientes = reservasUsuario.stream()
                    .filter(r -> "Pendiente de confirmación".equals(r.getEstado()))
                    .count();

            long reservasCanceladas = reservasUsuario.stream()
                    .filter(r -> "Cancelada".equals(r.getEstado()))
                    .count();

            double totalGastado = reservasUsuario.stream()
                    .filter(r -> "Confirmada".equals(r.getEstado()))
                    .mapToDouble(r -> r.getCosto() != null ? r.getCosto() : 0.0)
                    .sum();

            // Obtener mantenimientos próximos (solo informativos)
            List<Mantenimiento> mantenimientosProximos = mantenimientoRepository.findByEspacioAndFechaBetween(
                            espacio, LocalDate.now(), LocalDate.now().plusMonths(1)
                    ).stream()
                    .filter(m -> m.getEstado() == Mantenimiento.EstadoMantenimiento.PROGRAMADO)
                    .limit(3)
                    .collect(Collectors.toList());

            // Preparar respuesta
            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("misReservas", Map.of(
                    "total", reservasUsuario.size(),
                    "confirmadas", reservasConfirmadas,
                    "pendientes", reservasPendientes,
                    "canceladas", reservasCanceladas,
                    "totalGastado", totalGastado
            ));

            estadisticas.put("mantenimientosProximos", mantenimientosProximos.size());

            // Información del espacio
            estadisticas.put("espacio", Map.of(
                    "nombre", espacio.getNombre(),
                    "costoPorHora", espacio.getCosto(),
                    "tipo", espacio.getTipoEspacio() != null ? espacio.getTipoEspacio().getNombre() : ""
            ));

            System.out.println("✅ [VECINO] Estadísticas obtenidas exitosamente");
            return ResponseEntity.ok(estadisticas);

        } catch (Exception e) {
            System.err.println("❌ [VECINO] Error obteniendo estadísticas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error obteniendo estadísticas"));
        }
    }

    /**
     * Método helper para convertir mantenimientos a formato JSON
     */
    private List<Map<String, Object>> convertirMantenimientosParaJSON(List<Mantenimiento> mantenimientos) {
        return mantenimientos.stream()
                .map(this::convertirMantenimientoAMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertirMantenimientoAMap(Mantenimiento mantenimiento) {
        Map<String, Object> map = new HashMap<>();

        map.put("idMantenimiento", mantenimiento.getIdMantenimiento());
        map.put("fechaInicio", mantenimiento.getFechaInicio().toString());
        map.put("fechaFin", mantenimiento.getFechaFin().toString());
        map.put("horaInicio", mantenimiento.getHoraInicio().toString());
        map.put("horaFin", mantenimiento.getHoraFin().toString());
        map.put("tipoMantenimiento", mantenimiento.getTipoMantenimiento().name());
        map.put("prioridad", mantenimiento.getPrioridad().name());
        map.put("estado", mantenimiento.getEstado().name());
        map.put("descripcion", mantenimiento.getDescripcion());
        map.put("costoEstimado", mantenimiento.getCostoEstimado());

        // Para vecinos, información limitada del responsable
        if (mantenimiento.getResponsable() != null) {
            map.put("responsable", "Coordinador asignado");
        } else {
            map.put("responsable", "Sin asignar");
        }

        return map;
    }
}