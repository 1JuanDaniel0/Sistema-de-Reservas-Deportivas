package com.example.project.controller.api;

import com.example.project.dto.ReembolsoDTO;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/reembolsos")
public class AdminReembolsosApiController {

    @Autowired
    private SolicitudCancelacionRepository solicitudCancelacionRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private EspacioRepositoryGeneral espacioRepository;
    @Autowired
    private EstadoReservaRepository estadoReservaRepository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ReembolsoRepository reembolsoRepository;

    /**
     * Obtener solicitudes de reembolso pendientes con filtros
     */
    @GetMapping("/pendientes")
    public ResponseEntity<Map<String, Object>> obtenerReembolsosPendientes(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            @RequestParam(required = false) Integer estadoReserva,
            @RequestParam(required = false) Integer espacio,
            @RequestParam(required = false) String tipoPago,
            HttpSession session
    ) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            System.out.println("🔍 [" + requestId + "] === INICIO CONSULTA REEMBOLSOS PENDIENTES ===");

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                System.err.println("❌ [" + requestId + "] Usuario no autenticado");
                return ResponseEntity.status(401).build();
            }

            // Obtener solicitudes de cancelación pendientes con reservas que pagaron en banco
            List<SolicitudCancelacion> solicitudes = aplicarFiltrosReembolsos(
                    fechaInicio, fechaFin, estadoReserva, espacio, tipoPago, requestId);

            System.out.println("✅ [" + requestId + "] Solicitudes encontradas: " + solicitudes.size());

            // Convertir a formato para DataTable
            List<Map<String, Object>> data = solicitudes.stream()
                    .map(solicitud -> convertirSolicitudAMap(solicitud, requestId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("recordsTotal", data.size());
            response.put("recordsFiltered", data.size());
            response.put("requestId", requestId);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ [" + requestId + "] Consulta completada en " + duration + "ms");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("❌ [" + requestId + "] Error después de " + duration + "ms: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener historial usando DTOs
     */
    @GetMapping("/historial")
    public ResponseEntity<Map<String, Object>> obtenerHistorialReembolsos(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            @RequestParam(required = false) String estadoReembolso,
            @RequestParam(required = false) Integer espacio,
            @RequestParam(required = false) String tipoPago,
            HttpSession session
    ) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            System.out.println("📊 [" + requestId + "] === CONSULTA HISTORIAL CON DTOs ===");
            System.out.println("📊 [" + requestId + "] Parámetros recibidos:");
            System.out.println("  - fechaInicio: " + fechaInicio);
            System.out.println("  - fechaFin: " + fechaFin);
            System.out.println("  - estadoReembolso: " + estadoReembolso);
            System.out.println("  - espacio: " + espacio);
            System.out.println("  - tipoPago: " + tipoPago);

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Convertir parámetros
            LocalDateTime fechaInicioDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
            LocalDateTime fechaFinDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

            Reembolso.EstadoReembolso estadoEnum = null;
            if (estadoReembolso != null && !estadoReembolso.isEmpty()) {
                try {
                    estadoEnum = Reembolso.EstadoReembolso.valueOf(estadoReembolso.toUpperCase());
                    System.out.println("📊 [" + requestId + "] Estado enum convertido: " + estadoEnum);
                } catch (IllegalArgumentException e) {
                    System.err.println("⚠️ Estado inválido: " + estadoReembolso);
                }
            }

            // FALLBACK: Si el repositorio DTO no funciona, usar método tradicional
            List<ReembolsoDTO> reembolsosDTO = List.of();
            try {
                System.out.println("📊 [" + requestId + "] Intentando obtener DTOs del repositorio...");
                reembolsosDTO = reembolsoRepository.findHistorialDTOWithFilters(
                        estadoEnum, tipoPago, espacio, fechaInicioDateTime, fechaFinDateTime);
                System.out.println("✅ [" + requestId + "] DTOs obtenidos del repositorio: " + reembolsosDTO.size());
            } catch (Exception e) {
                System.err.println("⚠️ [" + requestId + "] Error con DTOs del repositorio, usando método alternativo: " + e.getMessage());
                e.printStackTrace();

                // Método alternativo: obtener todos los reembolsos y filtrar manualmente
                try {
                    System.out.println("🔄 [" + requestId + "] Intentando método alternativo...");
                    List<Reembolso> reembolsos = reembolsoRepository.findAllWithCompleteInfo();
                    System.out.println("📊 [" + requestId + "] Reembolsos encontrados (método alternativo): " + reembolsos.size());

                    // Verificar si hay reembolsos en la base de datos
                    if (reembolsos.isEmpty()) {
                        System.out.println("⚠️ [" + requestId + "] No hay reembolsos en la base de datos");

                        // Verificar si hay solicitudes de cancelación
                        List<SolicitudCancelacion> solicitudes = solicitudCancelacionRepository.findAll();
                        System.out.println("📊 [" + requestId + "] Solicitudes de cancelación encontradas: " + solicitudes.size());

                        // Verificar si hay reservas
                        List<Reserva> reservas = reservaRepository.findAll();
                        System.out.println("📊 [" + requestId + "] Reservas encontradas: " + reservas.size());
                    }

                    Reembolso.EstadoReembolso finalEstadoEnum = estadoEnum;
                    reembolsosDTO = reembolsos.stream()
                            .filter(r -> filtrarReembolso(r, finalEstadoEnum, tipoPago, espacio, fechaInicioDateTime, fechaFinDateTime))
                            .map(this::convertirReembolsoADTO)
                            .collect(Collectors.toList());

                    System.out.println("✅ [" + requestId + "] DTOs convertidos manualmente: " + reembolsosDTO.size());
                } catch (Exception e2) {
                    System.err.println("❌ [" + requestId + "] Error en método alternativo: " + e2.getMessage());
                    e2.printStackTrace();
                }
            }

            // Convertir DTOs a Maps para el frontend
            List<Map<String, Object>> data = reembolsosDTO.stream()
                    .map(this::convertirDTOAMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("recordsTotal", data.size());
            response.put("recordsFiltered", data.size());

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ [" + requestId + "] Completado en " + duration + "ms");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("❌ [" + requestId + "] Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NUEVO ENDPOINT: Marcar reembolso como gestionado
     * Se agrega al AdminReembolsosApiController existente
     */
    @PostMapping("/pendientes/{id}/marcar-gestionado")
    public ResponseEntity<Map<String, String>> marcarComoGestionado(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {
        try {
            System.out.println("✅ Marcando reembolso como gestionado - ID: " + id);

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Verificar que sea administrador
            if (!"Administrador".equals(usuario.getRol().getRol())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Sin permisos para esta operación");
                return ResponseEntity.status(403).body(error);
            }

            // Buscar la solicitud de cancelación
            Optional<SolicitudCancelacion> solicitudOpt = solicitudCancelacionRepository.findById(id);
            if (!solicitudOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Solicitud no encontrada");
                return ResponseEntity.notFound().build();
            }

            SolicitudCancelacion solicitud = solicitudOpt.get();
            Reserva reserva = solicitud.getReserva();

            // Validar que sea un reembolso "En banco" y esté aprobado por coordinador
            if (!"En banco".equals(reserva.getTipoPago()) || !"Aprobado".equals(solicitud.getEstado())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Solo se pueden gestionar reembolsos aprobados de tipo 'En banco'");
                return ResponseEntity.badRequest().body(error);
            }

            // Buscar o crear el registro de reembolso
            Optional<Reembolso> reembolsoOpt = reembolsoRepository.findBySolicitudCancelacion(solicitud);
            Reembolso reembolso;

            if (reembolsoOpt.isPresent()) {
                reembolso = reembolsoOpt.get();
            } else {
                // Crear nuevo registro de reembolso si no existe
                reembolso = new Reembolso();
                reembolso.setSolicitudCancelacion(solicitud);
                reembolso.setReserva(reserva);
                reembolso.setMontoReembolso(BigDecimal.valueOf(reserva.getCosto()));
                reembolso.setTipoPagoOriginal(reserva.getTipoPago());
                reembolso.setMetodoReembolso(Reembolso.MetodoReembolso.DEPOSITO_MANUAL);
                reembolso.setFechaCreacion(LocalDateTime.now());
                reembolso.setAprobadoPorCoordinador(solicitud.getReserva().getCoordinador()); // Si tienes este campo
            }

            // Actualizar el estado del reembolso
            reembolso.setEstadoReembolso(Reembolso.EstadoReembolso.COMPLETADO);
            reembolso.setFechaProcesamiento(LocalDateTime.now());
            reembolso.setProcesadoPorAdmin(usuario);
            reembolso.setObservacionesAdmin(request.get("notas")); // Notas del administrador

            // Actualizar la solicitud de cancelación
            solicitud.setEstado("Completado");
            solicitud.marcarProcesada("Reembolso gestionado manualmente por el administrador: " +
                    (request.get("notas") != null ? request.get("notas") : "Sin observaciones"));

            // Actualizar estado de la reserva
            reserva.setEstadoReembolso(Reserva.EstadoReembolso.APROBADO);

            // Cambiar estado de la reserva a cancelada si no lo está ya
            EstadoReserva estadoCancelada = estadoReservaRepository.findByEstado("Cancelada");
            if (estadoCancelada != null && !estadoCancelada.equals(reserva.getEstado())) {
                reserva.setEstado(estadoCancelada);
            }

            // Guardar cambios
            reembolsoRepository.save(reembolso);
            solicitudCancelacionRepository.save(solicitud);
            reservaRepository.save(reserva);

            System.out.println("✅ Reembolso " + id + " marcado como gestionado exitosamente por " + usuario.getNombres());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Reembolso marcado como gestionado correctamente");
            response.put("fechaProcesamiento", reembolso.getFechaProcesamiento().toString());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error marcando reembolso como gestionado " + id + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * MÉTODO HELPER: Verificar si un reembolso puede ser gestionado
     */
    private boolean puedeSerGestionado(SolicitudCancelacion solicitud) {
        return solicitud != null &&
                "Aprobado".equals(solicitud.getEstado()) &&
                solicitud.getReserva() != null &&
                "En banco".equals(solicitud.getReserva().getTipoPago());
    }

    /**
     * NUEVO: Endpoint específico para estadísticas del historial
     * IMPORTANTE: Este endpoint debe ir ANTES que /historial/{id} para evitar conflictos
     */
    @GetMapping("/historial/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasHistorial(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            HttpSession session
    ) {
        try {
            System.out.println("📊 Calculando estadísticas del historial de reembolsos...");

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Obtener todos los reembolsos para calcular estadísticas
            List<Reembolso> todosReembolsos;
            try {
                todosReembolsos = reembolsoRepository.findAllWithCompleteInfo();
                System.out.println("📊 Total de reembolsos en BD: " + todosReembolsos.size());
            } catch (Exception e) {
                System.err.println("⚠️ Error obteniendo reembolsos, verificando repositorio básico: " + e.getMessage());

                // Fallback: usar repositorio básico
                todosReembolsos = reembolsoRepository.findAll();
                System.out.println("📊 Total de reembolsos (findAll): " + todosReembolsos.size());

                if (todosReembolsos.isEmpty()) {
                    // Verificar otras entidades relacionadas
                    long totalSolicitudes = solicitudCancelacionRepository.count();
                    long totalReservas = reservaRepository.count();

                    System.out.println("📊 Diagnóstico de datos:");
                    System.out.println("  - Solicitudes de cancelación: " + totalSolicitudes);
                    System.out.println("  - Reservas: " + totalReservas);
                    System.out.println("  - Reembolsos: " + todosReembolsos.size());

                    // Retornar estadísticas vacías pero válidas
                    Map<String, Object> estadisticasVacias = new HashMap<>();
                    estadisticasVacias.put("total", 0);
                    estadisticasVacias.put("completados", 0);
                    estadisticasVacias.put("pendientes", 0);
                    estadisticasVacias.put("fallidos", 0);
                    estadisticasVacias.put("montoTotal", 0.0);
                    estadisticasVacias.put("mensaje", "No hay datos de reembolsos disponibles");

                    return ResponseEntity.ok(estadisticasVacias);
                }
            }

            // Aplicar filtros de fecha si existen
            if (fechaInicio != null || fechaFin != null) {
                LocalDateTime fechaInicioDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
                LocalDateTime fechaFinDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

                todosReembolsos = todosReembolsos.stream()
                        .filter(r -> {
                            LocalDateTime fechaCreacion = r.getFechaCreacion();
                            if (fechaInicioDateTime != null && fechaCreacion.isBefore(fechaInicioDateTime)) {
                                return false;
                            }
                            if (fechaFinDateTime != null && fechaCreacion.isAfter(fechaFinDateTime)) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }

            // Calcular estadísticas
            long total = todosReembolsos.size();
            long completados = todosReembolsos.stream()
                    .filter(r -> r.getEstadoReembolso() == Reembolso.EstadoReembolso.COMPLETADO)
                    .count();
            long pendientes = todosReembolsos.stream()
                    .filter(r -> r.getEstadoReembolso() == Reembolso.EstadoReembolso.PENDIENTE)
                    .count();
            long fallidos = todosReembolsos.stream()
                    .filter(r -> r.getEstadoReembolso() == Reembolso.EstadoReembolso.FALLIDO)
                    .count();

            double montoTotal = todosReembolsos.stream()
                    .filter(r -> r.getEstadoReembolso() == Reembolso.EstadoReembolso.COMPLETADO)
                    .mapToDouble(r -> r.getMontoReembolso() != null ? r.getMontoReembolso().doubleValue() : 0.0)
                    .sum();

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("total", total);
            estadisticas.put("completados", completados);
            estadisticas.put("pendientes", pendientes);
            estadisticas.put("fallidos", fallidos);
            estadisticas.put("montoTotal", montoTotal);

            System.out.println("✅ Estadísticas del historial calculadas: Total=" + total +
                    ", Completados=" + completados + ", Pendientes=" + pendientes +
                    ", Fallidos=" + fallidos + ", MontoTotal=" + montoTotal);

            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            System.err.println("❌ Error calculando estadísticas del historial: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener detalles usando DTO
     * IMPORTANTE: Este endpoint debe ir DESPUÉS que /historial/estadisticas
     */
    @GetMapping("/historial/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleReembolso(
            @PathVariable Integer id,
            HttpSession session
    ) {
        try {
            System.out.println("🔍 Obteniendo detalle con DTO: " + id);

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Intentar obtener DTO directamente
            Optional<ReembolsoDTO> reembolsoDTO = Optional.empty();
            try {
                reembolsoDTO = reembolsoRepository.findDTOBySolicitudId(id);
            } catch (Exception e) {
                System.err.println("⚠️ Error obteniendo DTO, intentando método alternativo: " + e.getMessage());
            }

            // Método alternativo si el DTO falla
            if (!reembolsoDTO.isPresent()) {
                Optional<Reembolso> reembolsoOpt = reembolsoRepository.findById(id);
                if (reembolsoOpt.isPresent()) {
                    ReembolsoDTO dto = convertirReembolsoADTO(reembolsoOpt.get());
                    reembolsoDTO = Optional.of(dto);
                }
            }

            if (!reembolsoDTO.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Convertir DTO a Map detallado
            Map<String, Object> detalles = convertirDTOAMapDetallado(reembolsoDTO.get());

            return ResponseEntity.ok(detalles);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo detalle: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener detalles de una solicitud de reembolso específica
     */
    @GetMapping("/pendientes/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleSolicitud(
            @PathVariable Integer id,
            HttpSession session
    ) {
        try {
            System.out.println("🔍 Obteniendo detalles de solicitud ID: " + id);

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<SolicitudCancelacion> solicitudOpt = solicitudCancelacionRepository.findById(id);
            if (!solicitudOpt.isPresent()) {
                System.err.println("❌ Solicitud no encontrada: " + id);
                return ResponseEntity.notFound().build();
            }

            SolicitudCancelacion solicitud = solicitudOpt.get();
            Map<String, Object> detalles = convertirSolicitudAMapDetallado(solicitud);

            System.out.println("✅ Detalles de solicitud obtenidos: " + id);
            return ResponseEntity.ok(detalles);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo detalles de solicitud " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Aprobar una solicitud de reembolso
     */
    @PostMapping("/pendientes/{id}/aprobar")
    public ResponseEntity<Map<String, String>> aprobarReembolso(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {
        try {
            System.out.println("✅ Aprobando reembolso ID: " + id);

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<SolicitudCancelacion> solicitudOpt = solicitudCancelacionRepository.findById(id);
            if (!solicitudOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            SolicitudCancelacion solicitud = solicitudOpt.get();
            Reserva reserva = solicitud.getReserva();

            // Validar que se pueda aprobar
            if (!"Pendiente".equals(solicitud.getEstado())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La solicitud ya fue procesada");
                return ResponseEntity.badRequest().body(error);
            }

            // Aprobar solicitud
            solicitud.setEstado("Completado");
            solicitud.marcarProcesada(request.get("motivoRespuesta"));

            // Cambiar estado de reembolso en la reserva
            reserva.setEstadoReembolso(Reserva.EstadoReembolso.APROBADO);

            // Cambiar estado de la reserva a cancelada
            EstadoReserva estadoCancelada = estadoReservaRepository.findByEstado("Cancelada");
            if (estadoCancelada != null) {
                reserva.setEstado(estadoCancelada);
            }

            solicitudCancelacionRepository.save(solicitud);
            reservaRepository.save(reserva);

            System.out.println("✅ Reembolso " + id + " aprobado exitosamente");

            Map<String, String> response = new HashMap<>();
            response.put("message", "Reembolso aprobado exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error aprobando reembolso " + id + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Rechazar una solicitud de reembolso
     */
    @PostMapping("/pendientes/{id}/rechazar")
    public ResponseEntity<Map<String, String>> rechazarReembolso(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {
        try {
            System.out.println("❌ Rechazando reembolso ID: " + id);

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<SolicitudCancelacion> solicitudOpt = solicitudCancelacionRepository.findById(id);
            if (!solicitudOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            SolicitudCancelacion solicitud = solicitudOpt.get();
            Reserva reserva = solicitud.getReserva();

            // Validar que se pueda rechazar
            if (!"Pendiente".equals(solicitud.getEstado())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La solicitud ya fue procesada");
                return ResponseEntity.badRequest().body(error);
            }

            // Rechazar solicitud
            solicitud.setEstado("Rechazado");
            solicitud.marcarProcesada(request.get("motivoRespuesta"));

            // Cambiar estado de reembolso en la reserva
            reserva.setEstadoReembolso(Reserva.EstadoReembolso.RECHAZADO);

            solicitudCancelacionRepository.save(solicitud);
            reservaRepository.save(reserva);

            System.out.println("✅ Reembolso " + id + " rechazado exitosamente");

            Map<String, String> response = new HashMap<>();
            response.put("message", "Reembolso rechazado exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error rechazando reembolso " + id + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Obtener estadísticas de reembolsos pendientes
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            HttpSession session
    ) {
        try {
            System.out.println("📊 Calculando estadísticas de reembolsos...");

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            List<SolicitudCancelacion> todasSolicitudes = aplicarFiltrosReembolsos(
                    fechaInicio, fechaFin, null, null, "En banco", "STATS");

            long total = todasSolicitudes.size();
            long pendientes = todasSolicitudes.stream()
                    .filter(s -> "Aprobado".equals(s.getEstado()))
                    .count();

            long aprobadas = todasSolicitudes.stream()
                    .filter(s -> "Completado".equals(s.getEstado()))
                    .count();

            double montoTotal = todasSolicitudes.stream()
                    .filter(s -> s.getReserva() != null && s.getReserva().getCosto() != null)
                    .mapToDouble(s -> s.getReserva().getCosto())
                    .sum();

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("total", total);
            estadisticas.put("pendientes", pendientes);
            estadisticas.put("aprobadas", aprobadas);
            estadisticas.put("montoTotal", montoTotal);

            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            System.err.println("❌ Error calculando estadísticas de reembolsos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint de debugging para verificar datos
     */
    @GetMapping("/debug/datos")
    public ResponseEntity<Map<String, Object>> debugDatos(HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario == null) return ResponseEntity.status(401).build();

        Map<String, Object> debug = new HashMap<>();
        debug.put("reembolsos", reembolsoRepository.count());
        debug.put("solicitudes", solicitudCancelacionRepository.count());
        debug.put("reservas", reservaRepository.count());

        return ResponseEntity.ok(debug);
    }

    /**
     * Aplicar filtros a las solicitudes de reembolso
     */
    private List<SolicitudCancelacion> aplicarFiltrosReembolsos(
            LocalDate fechaInicio, LocalDate fechaFin, Integer estadoReserva,
            Integer espacio, String tipoPago, String requestId) {

        System.out.println("🔍 [" + requestId + "] Aplicando filtros de reembolsos...");

        List<SolicitudCancelacion> todasSolicitudes = solicitudCancelacionRepository.findAll();

        List<SolicitudCancelacion> solicitudesFiltradas = todasSolicitudes.stream()
                .filter(solicitud -> {
                    try {
                        // Solo solicitudes pagadas en banco Y aprobadas por coordinador
                        if (solicitud.getReserva() == null ||
                                !"En banco".equals(solicitud.getReserva().getTipoPago()) ||
                                !"Aprobado".equals(solicitud.getEstado())) {
                            return false;
                        }

                        // Filtro por fecha de solicitud
                        if (fechaInicio != null && solicitud.getFechaSolicitud().toLocalDate().isBefore(fechaInicio)) {
                            return false;
                        }
                        if (fechaFin != null && solicitud.getFechaSolicitud().toLocalDate().isAfter(fechaFin)) {
                            return false;
                        }

                        // Filtro por estado de reserva
                        if (estadoReserva != null) {
                            if (solicitud.getReserva().getEstado() == null ||
                                    !estadoReserva.equals(solicitud.getReserva().getEstado().getIdEstadoReserva())) {
                                return false;
                            }
                        }

                        // Filtro por espacio
                        if (espacio != null) {
                            if (solicitud.getReserva().getEspacio() == null ||
                                    !espacio.equals(solicitud.getReserva().getEspacio().getIdEspacio())) {
                                return false;
                            }
                        }

                        return true;
                    } catch (Exception e) {
                        System.err.println("⚠️ [" + requestId + "] Error filtrando solicitud: " + e.getMessage());
                        return false;
                    }
                })
                .sorted((s1, s2) -> {
                    try {
                        boolean s1Urgente = s1.esUrgente();
                        boolean s2Urgente = s2.esUrgente();

                        if (s1Urgente && !s2Urgente) return -1;
                        if (!s1Urgente && s2Urgente) return 1;

                        return s2.getFechaSolicitud().compareTo(s1.getFechaSolicitud());
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());

        System.out.println("✅ [" + requestId + "] Filtros aplicados. Resultado: " + solicitudesFiltradas.size() + " solicitudes");
        return solicitudesFiltradas;
    }

    /**
     * Método helper para filtrar reembolsos manualmente
     */
    private boolean filtrarReembolso(Reembolso reembolso, Reembolso.EstadoReembolso estadoEnum,
                                     String tipoPago, Integer espacio,
                                     LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        try {
            // Filtro por estado
            if (estadoEnum != null && reembolso.getEstadoReembolso() != estadoEnum) {
                return false;
            }

            // Filtro por tipo de pago
            if (tipoPago != null && !tipoPago.isEmpty()) {
                if (!tipoPago.equals(reembolso.getTipoPagoOriginal())) {
                    return false;
                }
            }

            // Filtro por espacio
            if (espacio != null) {
                if (reembolso.getReserva() == null ||
                        reembolso.getReserva().getEspacio() == null ||
                        !espacio.equals(reembolso.getReserva().getEspacio().getIdEspacio())) {
                    return false;
                }
            }

            // Filtro por fecha
            if (fechaInicio != null && reembolso.getFechaCreacion().isBefore(fechaInicio)) {
                return false;
            }
            if (fechaFin != null && reembolso.getFechaCreacion().isAfter(fechaFin)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Error filtrando reembolso: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convertir entidad Reembolso a DTO manualmente
     */
    private ReembolsoDTO convertirReembolsoADTO(Reembolso reembolso) {
        try {
            ReembolsoDTO dto = new ReembolsoDTO();

            // IDs básicos
            dto.setIdReembolso(reembolso.getIdReembolso());
            if (reembolso.getSolicitudCancelacion() != null) {
                dto.setIdSolicitud(reembolso.getSolicitudCancelacion().getId());
                dto.setMotivoSolicitud(reembolso.getSolicitudCancelacion().getMotivo());
                dto.setFechaSolicitud(reembolso.getSolicitudCancelacion().getFechaSolicitud());
                dto.setCodigoPago(reembolso.getSolicitudCancelacion().getCodigoPago());
                dto.setComprobanteUrl(reembolso.getSolicitudCancelacion().getComprobanteUrl());
            }

            // Información de la reserva
            if (reembolso.getReserva() != null) {
                Reserva reserva = reembolso.getReserva();
                dto.setIdReserva(reserva.getIdReserva());
                dto.setFechaReserva(reserva.getFecha());
                dto.setHoraInicio(reserva.getHoraInicio());
                dto.setHoraFin(reserva.getHoraFin());

                // Información del vecino
                if (reserva.getVecino() != null) {
                    dto.setVecinoNombre(reserva.getVecino().getNombres());
                    dto.setVecinoApellido(reserva.getVecino().getApellidos());
                    dto.setVecinoDni(reserva.getVecino().getDni());
                    dto.setVecinoCorreo(reserva.getVecino().getCorreo());
                }

                // Información del espacio
                if (reserva.getEspacio() != null) {
                    dto.setEspacioNombre(reserva.getEspacio().getNombre());
                }
            }

            // Información del reembolso
            dto.setMontoReembolso(reembolso.getMontoReembolso());
            dto.setTipoPagoOriginal(reembolso.getTipoPagoOriginal());
            dto.setEstadoReembolso(reembolso.getEstadoReembolso().name());
            if (reembolso.getMetodoReembolso() != null) {
                dto.setMetodoReembolso(reembolso.getMetodoReembolso().name());
            }
            dto.setFechaCreacion(reembolso.getFechaCreacion());
            dto.setFechaProcesamiento(reembolso.getFechaProcesamiento());

            // Información del coordinador
            if (reembolso.getAprobadoPorCoordinador() != null) {
                dto.setCoordinadorNombre(reembolso.getAprobadoPorCoordinador().getNombres() + " " + reembolso.getAprobadoPorCoordinador().getApellidos());
            }
            dto.setMotivoAprobacion(reembolso.getMotivoAprobacion());
            dto.setFechaAprobacion(reembolso.getFechaAprobacion());

            // Información del admin
            if (reembolso.getProcesadoPorAdmin() != null) {
                dto.setAdminNombre(reembolso.getProcesadoPorAdmin().getNombres() + " " + reembolso.getProcesadoPorAdmin().getApellidos());
            }
            dto.setNumeroOperacion(reembolso.getNumeroOperacion());
            dto.setEntidadBancaria(reembolso.getEntidadBancaria());
            dto.setObservacionesAdmin(reembolso.getObservacionesAdmin());
            dto.setIdTransaccionReembolso(reembolso.getIdTransaccionReembolso());

            return dto;
        } catch (Exception e) {
            System.err.println("❌ Error convirtiendo reembolso a DTO: " + e.getMessage());
            return new ReembolsoDTO();
        }
    }

    /**
     * Convertir solicitud a Map para DataTable
     */
    private Map<String, Object> convertirSolicitudAMap(SolicitudCancelacion solicitud, String requestId) {
        try {
            if (solicitud == null || solicitud.getReserva() == null) {
                return null;
            }

            Reserva reserva = solicitud.getReserva();
            Map<String, Object> map = new HashMap<>();

            // Información de la solicitud
            map.put("idSolicitud", solicitud.getId());
            map.put("fechaSolicitud", solicitud.getFechaSolicitud().toString());
            map.put("estado", solicitud.getEstado());
            map.put("motivo", solicitud.getMotivo());
            map.put("urgente", solicitud.esUrgente());
            map.put("horasRestantes", solicitud.getHorasHastaReserva());

            // Información de la reserva
            map.put("idReserva", reserva.getIdReserva());
            map.put("fecha", reserva.getFecha() != null ? reserva.getFecha().toString() : "");
            map.put("horaInicio", reserva.getHoraInicio() != null ? reserva.getHoraInicio().toString() : "");
            map.put("horaFin", reserva.getHoraFin() != null ? reserva.getHoraFin().toString() : "");
            map.put("costo", reserva.getCosto() != null ? reserva.getCosto() : 0.0);

            // Información del vecino
            if (reserva.getVecino() != null) {
                String nombres = reserva.getVecino().getNombres() != null ? reserva.getVecino().getNombres() : "";
                String apellidos = reserva.getVecino().getApellidos() != null ? reserva.getVecino().getApellidos() : "";
                map.put("vecinoNombre", (nombres + " " + apellidos).trim());
                map.put("vecinoDni", reserva.getVecino().getDni());
                map.put("vecinoEmail", reserva.getVecino().getCorreo());
            } else {
                map.put("vecinoNombre", "N/A");
                map.put("vecinoDni", "N/A");
                map.put("vecinoEmail", "N/A");
            }

            // Información del espacio
            if (reserva.getEspacio() != null) {
                map.put("espacioNombre", reserva.getEspacio().getNombre());
                map.put("espacioTipo", reserva.getEspacio().getTipoEspacio() != null ?
                        reserva.getEspacio().getTipoEspacio().getNombre() : "N/A");
            } else {
                map.put("espacioNombre", "N/A");
                map.put("espacioTipo", "N/A");
            }

            // Información de pago
            map.put("codigoPago", solicitud.getCodigoPago());
            map.put("comprobanteUrl", solicitud.getComprobanteUrl());

            return map;
        } catch (Exception e) {
            System.err.println("❌ [" + requestId + "] Error convirtiendo solicitud: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convertir solicitud a Map detallado para modal
     */
    private Map<String, Object> convertirSolicitudAMapDetallado(SolicitudCancelacion solicitud) {
        Map<String, Object> map = convertirSolicitudAMap(solicitud, "DETALLE");

        if (map == null) return new HashMap<>();

        // Agregar información adicional
        if (solicitud.getComprobanteUrl() != null) {
            try {
                String urlPreFirmada = s3Service.generarUrlPreFirmada(solicitud.getComprobanteUrl(), 60);
                map.put("comprobanteUrlPreFirmada", urlPreFirmada);
            } catch (Exception e) {
                System.err.println("Error generando URL pre-firmada: " + e.getMessage());
            }
        }

        // Información de respuesta si existe
        if (solicitud.getTiempoRespuesta() != null) {
            map.put("tiempoRespuesta", solicitud.getTiempoRespuesta().toString());
            map.put("motivoRespuesta", solicitud.getMotivoRespuesta());
        }

        return map;
    }

    /**
     * Convertir DTO a Map para DataTable
     */
    private Map<String, Object> convertirDTOAMap(ReembolsoDTO dto) {
        Map<String, Object> map = new HashMap<>();

        // IDs
        map.put("idReembolso", dto.getIdReembolso());
        map.put("idSolicitud", dto.getIdSolicitud());
        map.put("idReserva", dto.getIdReserva());

        // Fechas
        map.put("fechaSolicitud", dto.getFechaSolicitud() != null ? dto.getFechaSolicitud().toString() : null);
        map.put("fechaProcesamiento", dto.getFechaProcesamiento() != null ?
                dto.getFechaProcesamiento().toString() : null);

        // Vecino
        map.put("vecinoNombre", dto.getVecinoNombreCompleto());
        map.put("vecinoDni", dto.getVecinoDni());
        map.put("vecinoEmail", dto.getVecinoCorreo());

        // Espacio y reserva
        map.put("espacioNombre", dto.getEspacioNombre());
        map.put("fecha", dto.getFechaReserva() != null ? dto.getFechaReserva().toString() : null);
        map.put("horaInicio", dto.getHoraInicio() != null ? dto.getHoraInicio().toString() : null);
        map.put("horaFin", dto.getHoraFin() != null ? dto.getHoraFin().toString() : null);

        // Estados y método
        map.put("estado", dto.getEstadoReembolso());
        map.put("estadoDescripcion", dto.getEstadoDescripcion());
        map.put("metodoReembolso", dto.getMetodoReembolso());
        map.put("metodoDescripcion", dto.getMetodoDescripcion());

        // Montos
        map.put("montoReembolso", dto.getMontoReembolso() != null ?
                dto.getMontoReembolso().doubleValue() : 0.0);
        map.put("tipoPago", dto.getTipoPagoOriginal());

        return map;
    }

    /**
     * Convertir DTO a Map detallado para modal
     */
    private Map<String, Object> convertirDTOAMapDetallado(ReembolsoDTO dto) {
        Map<String, Object> map = convertirDTOAMap(dto);

        // Información adicional para el modal
        map.put("motivoSolicitud", dto.getMotivoSolicitud());
        map.put("motivoAprobacion", dto.getMotivoAprobacion());
        map.put("observacionesAdmin", dto.getObservacionesAdmin());
        map.put("coordinadorNombre", dto.getCoordinadorNombre());
        map.put("adminNombre", dto.getAdminNombre());
        map.put("numeroOperacion", dto.getNumeroOperacion());
        map.put("entidadBancaria", dto.getEntidadBancaria());
        map.put("idTransaccionReembolso", dto.getIdTransaccionReembolso());
        map.put("codigoPago", dto.getCodigoPago());
        map.put("fechaAprobacion", dto.getFechaAprobacion() != null ?
                dto.getFechaAprobacion().toString() : null);
        map.put("fechaCreacion", dto.getFechaCreacion() != null ?
                dto.getFechaCreacion().toString() : null);

        // Generar URL pre-firmada para comprobante si existe
        if (dto.getComprobanteUrl() != null) {
            try {
                String urlPreFirmada = s3Service.generarUrlPreFirmada(dto.getComprobanteUrl(), 60);
                map.put("comprobanteUrlPreFirmada", urlPreFirmada);
            } catch (Exception e) {
                System.err.println("Error generando URL pre-firmada: " + e.getMessage());
            }
        }

        return map;
    }
}