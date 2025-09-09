package com.example.project.controller.api;

import com.example.project.entity.*;
import com.example.project.repository.ReservaRepository;
import com.example.project.repository.EspacioRepositoryGeneral;
import com.example.project.repository.EstadoReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api")
public class AdminReservasApiController {

    @Autowired private ReservaRepository reservaRepository;
    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private EstadoReservaRepository estadoReservaRepository;

    /**
     * Obtener lista de espacios para filtros
     */
    @GetMapping("/espacios")
    public ResponseEntity<List<Map<String, Object>>> obtenerEspacios() {
        try {
            System.out.println("🏢 Cargando lista de espacios...");

            List<Espacio> espacios = espacioRepository.findAll();
            List<Map<String, Object>> resultado = espacios.stream()
                    .map(espacio -> {
                        Map<String, Object> espacioMap = new HashMap<>();
                        espacioMap.put("idEspacio", espacio.getIdEspacio());
                        espacioMap.put("nombre", espacio.getNombre());
                        espacioMap.put("tipo", espacio.getTipoEspacio() != null ?
                                espacio.getTipoEspacio().getNombre() : "");
                        return espacioMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Espacios cargados: " + resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ Error al cargar espacios: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener la lista de Estados de Reserva para los filtros
     */
    @GetMapping("/estados-reserva")
    public ResponseEntity<List<Map<String, Object>>> obtenerEstadosReserva() {
        try {
            System.out.println("📋 Cargando estados de reserva...");

            List<EstadoReserva> estadosReserva = estadoReservaRepository.findAll();
            List<Map<String, Object>> resultado = estadosReserva.stream()
                    .map(estadoReserva -> {
                        Map<String, Object> estadoReservaMap = new HashMap<>();
                        estadoReservaMap.put("idEstadoReserva", estadoReserva.getIdEstadoReserva());
                        estadoReservaMap.put("estado", estadoReserva.getEstado());
                        return estadoReservaMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Estados de reserva cargados: " + resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ Error al cargar estados de reserva: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener reservas con filtros - VERSIÓN FINAL OPTIMIZADA
     */
    @GetMapping("/reservas")
    public ResponseEntity<Map<String, Object>> obtenerReservas(
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
            System.out.println("🔍 [" + requestId + "] === INICIO CONSULTA RESERVAS ===");
            System.out.println("🔍 [" + requestId + "] Filtros recibidos:");
            System.out.println("  - Fecha inicio: " + fechaInicio);
            System.out.println("  - Fecha fin: " + fechaFin);
            System.out.println("  - Estado reserva: " + estadoReserva);
            System.out.println("  - Espacio: " + espacio);
            System.out.println("  - Tipo pago: " + tipoPago);

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                System.err.println("❌ [" + requestId + "] Usuario no autenticado");
                return ResponseEntity.status(401).build();
            }

            // Obtener reservas con filtros aplicados
            List<Reserva> reservas = aplicarFiltros(fechaInicio, fechaFin, estadoReserva, espacio, tipoPago, requestId);
            System.out.println("✅ [" + requestId + "] Reservas encontradas: " + reservas.size());

            // Convertir a formato para DataTable
            List<Map<String, Object>> data = reservas.stream()
                    .map(reserva -> convertirReservaAMap(reserva, requestId))
                    .filter(Objects::nonNull) // Filtrar posibles nulos
                    .collect(Collectors.toList());

            // Log de validación
            if (!data.isEmpty()) {
                Map<String, Object> primeraReserva = data.get(0);
                System.out.println("🔍 [" + requestId + "] Primera reserva convertida:");
                System.out.println("  - ID: " + primeraReserva.get("idReserva"));
                System.out.println("  - Tipo Pago: " + primeraReserva.get("tipoPago"));
                System.out.println("  - Estado: " + primeraReserva.get("estado"));
                System.out.println("  - Vecino: " + primeraReserva.get("vecinoNombre"));

                // Validar que todos los registros tengan ID
                long registrosConId = data.stream()
                        .filter(item -> item.get("idReserva") != null)
                        .count();

                if (registrosConId != data.size()) {
                    System.err.println("⚠️ [" + requestId + "] Advertencia: " +
                            (data.size() - registrosConId) + " registros sin ID");
                }
            }

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
     * Obtener detalles de una reserva específica
     */
    @GetMapping("/reservas/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleReserva(
            @PathVariable Integer id,
            HttpSession session
    ) {
        try {
            System.out.println("🔍 Obteniendo detalles de reserva ID: " + id);

            // Verificar autenticación
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<Reserva> reservaOpt = reservaRepository.findById(id);
            if (!reservaOpt.isPresent()) {
                System.err.println("❌ Reserva no encontrada: " + id);
                return ResponseEntity.notFound().build();
            }

            Reserva reserva = reservaOpt.get();
            Map<String, Object> detalles = convertirReservaAMapDetallado(reserva);

            System.out.println("✅ Detalles de reserva obtenidos: " + id);
            return ResponseEntity.ok(detalles);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo detalles de reserva " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener estadísticas de reservas
     */
    @GetMapping("/reservas/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            @RequestParam(required = false) Integer estadoReserva,
            @RequestParam(required = false) Integer espacio,
            @RequestParam(required = false) String tipoPago,
            HttpSession session
    ) {
        try {
            System.out.println("📊 Calculando estadísticas...");

            // Verificar autenticación
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            List<Reserva> reservas = aplicarFiltros(fechaInicio, fechaFin, estadoReserva, espacio, tipoPago, "STATS");

            // Calcular estadísticas
            long total = reservas.size();
            long confirmadas = reservas.stream()
                    .filter(r -> r.getEstado() != null && "Confirmada".equals(r.getEstado().getEstado()))
                    .count();
            long pendientes = reservas.stream()
                    .filter(r -> r.getEstado() != null && "No confirmada".equals(r.getEstado().getEstado()))
                    .count();
            double ingresos = reservas.stream()
                    .filter(r -> r.getEstado() != null &&
                            ("Confirmada".equals(r.getEstado().getEstado()) || "Pasada".equals(r.getEstado().getEstado())))
                    .mapToDouble(r -> r.getCosto() != null ? r.getCosto() : 0.0)
                    .sum();

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("total", total);
            estadisticas.put("confirmadas", confirmadas);
            estadisticas.put("pendientes", pendientes);
            estadisticas.put("ingresos", ingresos);

            System.out.println("✅ Estadísticas calculadas: Total=" + total + ", Confirmadas=" + confirmadas +
                    ", Pendientes=" + pendientes + ", Ingresos=" + ingresos);

            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            System.err.println("❌ Error calculando estadísticas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancelar una reserva
     */
    @PostMapping("/reservas/{id}/cancelar")
    public ResponseEntity<Map<String, String>> cancelarReserva(
            @PathVariable Integer id,
            HttpSession session
    ) {
        try {
            System.out.println("🚫 Cancelando reserva ID: " + id);

            // Verificar autenticación
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<Reserva> reservaOpt = reservaRepository.findById(id);
            if (!reservaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Reserva reserva = reservaOpt.get();

            // Verificar que se pueda cancelar
            if (reserva.getEstado() != null && "Cancelada".equals(reserva.getEstado().getEstado())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La reserva ya está cancelada");
                return ResponseEntity.badRequest().body(error);
            }

            // Cambiar estado a cancelada
            EstadoReserva estadoCancelada = estadoReservaRepository.findByEstado("Cancelada");
            if (estadoCancelada == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Estado 'Cancelada' no encontrado");
                return ResponseEntity.internalServerError().body(error);
            }

            reserva.setEstado(estadoCancelada);
            reservaRepository.save(reserva);

            System.out.println("✅ Reserva " + id + " cancelada exitosamente");

            Map<String, String> response = new HashMap<>();
            response.put("message", "Reserva cancelada exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error cancelando reserva " + id + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Aplicar filtros a las reservas - OPTIMIZADO
     */
    private List<Reserva> aplicarFiltros(LocalDate fechaInicio, LocalDate fechaFin,
                                         Integer estadoReserva, Integer espacio, String tipoPago, String requestId) {

        System.out.println("🔍 [" + requestId + "] Aplicando filtros...");

        // Obtener todas las reservas de una vez
        List<Reserva> todasLasReservas = reservaRepository.findAll();
        System.out.println("📊 [" + requestId + "] Total de reservas en BD: " + todasLasReservas.size());

        List<Reserva> reservasFiltradas = todasLasReservas.stream()
                .filter(reserva -> {
                    try {
                        // Filtro por fecha
                        if (fechaInicio != null && (reserva.getFecha() == null || reserva.getFecha().isBefore(fechaInicio))) {
                            return false;
                        }
                        if (fechaFin != null && (reserva.getFecha() == null || reserva.getFecha().isAfter(fechaFin))) {
                            return false;
                        }

                        // Filtro por estado de la reserva
                        if (estadoReserva != null) {
                            if (reserva.getEstado() == null ||
                                    !estadoReserva.equals(reserva.getEstado().getIdEstadoReserva())) {
                                return false;
                            }
                        }

                        // Filtro por espacio
                        if (espacio != null) {
                            if (reserva.getEspacio() == null ||
                                    !espacio.equals(reserva.getEspacio().getIdEspacio())) {
                                return false;
                            }
                        }

                        // Filtro por tipo de pago
                        if (tipoPago != null && !tipoPago.isEmpty()) {
                            if (!tipoPago.equals(reserva.getTipoPago())) {
                                return false;
                            }
                        }

                        return true;
                    } catch (Exception e) {
                        System.err.println("⚠️ [" + requestId + "] Error filtrando reserva " +
                                (reserva.getIdReserva() != null ? reserva.getIdReserva() : "SIN_ID") + ": " + e.getMessage());
                        return false;
                    }
                })
                .sorted((r1, r2) -> {
                    try {
                        // Ordenar por fecha descendente
                        if (r1.getFecha() == null && r2.getFecha() == null) return 0;
                        if (r1.getFecha() == null) return 1;
                        if (r2.getFecha() == null) return -1;

                        int fechaCompare = r2.getFecha().compareTo(r1.getFecha());
                        if (fechaCompare != 0) return fechaCompare;

                        // Si las fechas son iguales, ordenar por hora descendente
                        if (r1.getHoraInicio() == null && r2.getHoraInicio() == null) return 0;
                        if (r1.getHoraInicio() == null) return 1;
                        if (r2.getHoraInicio() == null) return -1;

                        return r2.getHoraInicio().compareTo(r1.getHoraInicio());
                    } catch (Exception e) {
                        System.err.println("⚠️ [" + requestId + "] Error ordenando reservas: " + e.getMessage());
                        return 0;
                    }
                })
                .collect(Collectors.toList());

        System.out.println("✅ [" + requestId + "] Filtros aplicados. Resultado: " + reservasFiltradas.size() + " reservas");
        return reservasFiltradas;
    }

    /**
     * Convertir reserva a Map para DataTable - VERSIÓN SEGURA
     */
    private Map<String, Object> convertirReservaAMap(Reserva reserva, String requestId) {
        try {
            if (reserva == null) {
                System.err.println("⚠️ [" + requestId + "] Reserva nula encontrada");
                return null;
            }

            Map<String, Object> map = new HashMap<>();

            // ID de la reserva - CRÍTICO
            map.put("idReserva", reserva.getIdReserva());
            map.put("fecha", reserva.getFecha() != null ? reserva.getFecha().toString() : "");
            map.put("horaInicio", reserva.getHoraInicio() != null ? reserva.getHoraInicio().toString() : "");
            map.put("horaFin", reserva.getHoraFin() != null ? reserva.getHoraFin().toString() : "");

            // Información del vecino
            if (reserva.getVecino() != null) {
                String nombres = reserva.getVecino().getNombres() != null ? reserva.getVecino().getNombres() : "";
                String apellidos = reserva.getVecino().getApellidos() != null ? reserva.getVecino().getApellidos() : "";
                map.put("vecinoNombre", (nombres + " " + apellidos).trim());
                map.put("vecinoDni", reserva.getVecino().getDni() != null ? reserva.getVecino().getDni() : "N/A");
                map.put("vecinoEmail", reserva.getVecino().getCorreo() != null ? reserva.getVecino().getCorreo() : "N/A");
            } else {
                map.put("vecinoNombre", "N/A");
                map.put("vecinoDni", "N/A");
                map.put("vecinoEmail", "N/A");
            }

            // Información del espacio
            if (reserva.getEspacio() != null) {
                map.put("espacioNombre", reserva.getEspacio().getNombre() != null ? reserva.getEspacio().getNombre() : "N/A");
                map.put("espacioTipo", reserva.getEspacio().getTipoEspacio() != null ?
                        reserva.getEspacio().getTipoEspacio().getNombre() : "N/A");
            } else {
                map.put("espacioNombre", "N/A");
                map.put("espacioTipo", "N/A");
            }

            // Estado
            map.put("estado", reserva.getEstado() != null ? reserva.getEstado().getEstado() : "N/A");

            // Información financiera
            map.put("costo", reserva.getCosto() != null ? reserva.getCosto() : 0.0);
            map.put("tipoPago", reserva.getTipoPago() != null ? reserva.getTipoPago() : "N/A"); // CRÍTICO
            map.put("estadoPago", reserva.getEstadoPago() != null ? reserva.getEstadoPago() : "N/A");

            // Información del coordinador
            if (reserva.getCoordinador() != null) {
                String nombres = reserva.getCoordinador().getNombres() != null ? reserva.getCoordinador().getNombres() : "";
                String apellidos = reserva.getCoordinador().getApellidos() != null ? reserva.getCoordinador().getApellidos() : "";
                map.put("coordinadorNombre", (nombres + " " + apellidos).trim());
            } else {
                map.put("coordinadorNombre", null);
            }

            // Fechas adicionales
            map.put("momentoReserva", reserva.getMomentoReserva() != null ?
                    reserva.getMomentoReserva().toString() : null);
            map.put("fechaPago", reserva.getFechaPago() != null ?
                    reserva.getFechaPago().toString() : null);

            return map;
        } catch (Exception e) {
            System.err.println("❌ [" + requestId + "] Error convirtiendo reserva " +
                    (reserva != null && reserva.getIdReserva() != null ? reserva.getIdReserva() : "SIN_ID") +
                    ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convertir reserva a Map detallado para modal
     */
    private Map<String, Object> convertirReservaAMapDetallado(Reserva reserva) {
        Map<String, Object> map = convertirReservaAMap(reserva, "DETALLE");

        if (map == null) return new HashMap<>();

        // Agregar información adicional para el modal
        if (reserva.getEspacio() != null) {
            map.put("espacioDescripcion", reserva.getEspacio().getDescripcion());
            map.put("espacioUbicacion", reserva.getEspacio().getIdLugar() != null ?
                    reserva.getEspacio().getIdLugar().getLugar() : "N/A");
        }

        // Información de pago detallada
        map.put("idTransaccionPago", reserva.getIdTransaccionPago());

        // Observaciones del espacio si las hay
        if (reserva.getEspacio() != null && reserva.getEspacio().getObservaciones() != null) {
            map.put("observaciones", reserva.getEspacio().getObservaciones());
        }

        // URL de comprobante si existe
        if (reserva.getCapturaKey() != null) {
            map.put("comprobanteUrl", "https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/" +
                    reserva.getCapturaKey());
        }

        return map;
    }
}