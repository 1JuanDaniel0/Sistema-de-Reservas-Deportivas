package com.example.project.controller.api;

import com.example.project.entity.*;
import com.example.project.repository.ReservaRepository;
import com.example.project.repository.EspacioRepositoryGeneral;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/pagos")
public class AdminPagosApiController {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private EspacioRepositoryGeneral espacioRepository;

    /**
     * Obtener todos los pagos con filtros
     */
    @GetMapping("/registro")
    public ResponseEntity<Map<String, Object>> obtenerRegistroPagos(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            @RequestParam(required = false) Integer estadoReserva,
            @RequestParam(required = false) Integer espacio,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String estadoPago,
            HttpSession session
    ) {
        try {
            System.out.println("🔍 === CONSULTA REGISTRO DE PAGOS ===");

            // Verificar usuario autenticado
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            // Obtener todas las reservas con pagos
            List<Reserva> reservas = aplicarFiltrosPagos(fechaInicio, fechaFin, estadoReserva, espacio, tipoPago, estadoPago);

            // Convertir a formato para DataTable
            List<Map<String, Object>> data = reservas.stream()
                    .map(this::convertirReservaAPagoMap)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("recordsTotal", data.size());
            response.put("recordsFiltered", data.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo registro de pagos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener estadísticas de pagos
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String estadoPago,
            HttpSession session
    ) {
        try {
            System.out.println("📊 Calculando estadísticas de pagos...");

            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(401).build();
            }

            List<Reserva> reservas = aplicarFiltrosPagos(fechaInicio, fechaFin, null, null, tipoPago, estadoPago);

            // Calcular estadísticas
            long total = reservas.size();
            long pagados = reservas.stream()
                    .filter(r -> "Pagado".equals(r.getEstadoPago()))
                    .count();
            long pendientes = reservas.stream()
                    .filter(r -> "Pendiente".equals(r.getEstadoPago()))
                    .count();
            long anulados = reservas.stream()
                    .filter(r -> "Anulado".equals(r.getEstadoPago()))
                    .count();

            double montoTotal = reservas.stream()
                    .filter(r -> "Pagado".equals(r.getEstadoPago()))
                    .mapToDouble(r -> r.getCosto() != null ? r.getCosto() : 0.0)
                    .sum();

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("total", total);
            estadisticas.put("pagados", pagados);
            estadisticas.put("pendientes", pendientes);
            estadisticas.put("anulados", anulados);
            estadisticas.put("montoTotal", montoTotal);

            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            System.err.println("❌ Error calculando estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener lista de espacios para filtros
     */
    @GetMapping("/espacios")
    public ResponseEntity<List<Map<String, Object>>> obtenerEspacios() {
        try {
            List<Espacio> espacios = espacioRepository.findAll();
            List<Map<String, Object>> resultado = espacios.stream()
                    .map(espacio -> {
                        Map<String, Object> espacioMap = new HashMap<>();
                        espacioMap.put("idEspacio", espacio.getIdEspacio());
                        espacioMap.put("nombre", espacio.getNombre());
                        return espacioMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ Error al cargar espacios: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Aplicar filtros a las reservas con pagos
     */
    private List<Reserva> aplicarFiltrosPagos(LocalDate fechaInicio, LocalDate fechaFin,
                                              Integer estadoReserva, Integer espacio,
                                              String tipoPago, String estadoPago) {

        List<Reserva> todasReservas = reservaRepository.findAll();

        return todasReservas.stream()
                .filter(reserva -> {
                    try {
                        // Solo reservas que tienen información de pago
                        if (reserva.getTipoPago() == null && reserva.getEstadoPago() == null) {
                            return false;
                        }

                        // Filtro por fecha de reserva
                        if (fechaInicio != null && (reserva.getFecha() == null || reserva.getFecha().isBefore(fechaInicio))) {
                            return false;
                        }
                        if (fechaFin != null && (reserva.getFecha() == null || reserva.getFecha().isAfter(fechaFin))) {
                            return false;
                        }

                        // Filtro por estado de reserva
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

                        // Filtro por estado de pago
                        if (estadoPago != null && !estadoPago.isEmpty()) {
                            if (!estadoPago.equals(reserva.getEstadoPago())) {
                                return false;
                            }
                        }

                        return true;
                    } catch (Exception e) {
                        System.err.println("⚠️ Error filtrando reserva: " + e.getMessage());
                        return false;
                    }
                })
                .sorted((r1, r2) -> {
                    try {
                        // Ordenar por fecha de pago descendente, luego por fecha de reserva
                        if (r1.getFechaPago() != null && r2.getFechaPago() != null) {
                            return r2.getFechaPago().compareTo(r1.getFechaPago());
                        }
                        if (r1.getFechaPago() != null) return -1;
                        if (r2.getFechaPago() != null) return 1;

                        if (r1.getFecha() != null && r2.getFecha() != null) {
                            return r2.getFecha().compareTo(r1.getFecha());
                        }
                        return 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Convertir reserva a Map para tabla de pagos
     */
    private Map<String, Object> convertirReservaAPagoMap(Reserva reserva) {
        try {
            Map<String, Object> map = new HashMap<>();

            // IDs
            map.put("idReserva", reserva.getIdReserva());
            map.put("idTransaccion", reserva.getIdTransaccionPago());

            // Fechas
            map.put("fecha", reserva.getFecha() != null ? reserva.getFecha().toString() : "");
            map.put("fechaPago", reserva.getFechaPago() != null ?
                    reserva.getFechaPago().toString() : "");
            map.put("momentoReserva", reserva.getMomentoReserva() != null ?
                    reserva.getMomentoReserva().toString() : "");

            // Información del vecino
            if (reserva.getVecino() != null) {
                String nombres = reserva.getVecino().getNombres() != null ? reserva.getVecino().getNombres() : "";
                String apellidos = reserva.getVecino().getApellidos() != null ? reserva.getVecino().getApellidos() : "";
                map.put("vecinoNombre", (nombres + " " + apellidos).trim());
                map.put("vecinoDni", reserva.getVecino().getDni());
            } else {
                map.put("vecinoNombre", "N/A");
                map.put("vecinoDni", "N/A");
            }

            // Información del espacio
            if (reserva.getEspacio() != null) {
                map.put("espacioNombre", reserva.getEspacio().getNombre());
            } else {
                map.put("espacioNombre", "N/A");
            }

            // Información del pago
            map.put("costo", reserva.getCosto() != null ? reserva.getCosto() : 0.0);
            map.put("tipoPago", reserva.getTipoPago() != null ? reserva.getTipoPago() : "N/A");
            map.put("estadoPago", reserva.getEstadoPago() != null ? reserva.getEstadoPago() : "N/A");
            map.put("estadoReserva", reserva.getEstado() != null ? reserva.getEstado().getEstado() : "N/A");

            return map;
        } catch (Exception e) {
            System.err.println("❌ Error convirtiendo reserva a pago: " + e.getMessage());
            return null;
        }
    }
}