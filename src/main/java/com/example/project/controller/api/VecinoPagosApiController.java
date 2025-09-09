package com.example.project.controller.api;

import com.example.project.entity.*;
import com.example.project.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vecino/api/pagos")
public class VecinoPagosApiController {

    @Autowired private PagoRepository pagoRepositoryVecino;

    /**
     * Obtener pagos del vecino con filtros
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerPagos(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer espacio,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        try {
            System.out.println("💰 [" + requestId + "] === CONSULTA PAGOS VECINO ===");
            System.out.println("💰 [" + requestId + "] Filtros:");
            System.out.println("  - Fecha inicio: " + fechaInicio);
            System.out.println("  - Fecha fin: " + fechaFin);
            System.out.println("  - Tipo pago: " + tipoPago);
            System.out.println("  - Estado: " + estado);
            System.out.println("  - Espacio: " + espacio);

            // Verificar usuario autenticado
            Usuarios vecino = (Usuarios) session.getAttribute("usuario");
            if (vecino == null) {
                System.err.println("❌ [" + requestId + "] Usuario no autenticado");
                return ResponseEntity.status(401).build();
            }

            // Obtener todos los pagos del vecino
            List<Pago> todosPagos = pagoRepositoryVecino.findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(
                    vecino.getIdUsuarios()
            );

            // Aplicar filtros
            List<Pago> pagosFiltrados = aplicarFiltros(todosPagos, fechaInicio, fechaFin,
                    tipoPago, estado, espacio, requestId);

            // Convertir a DTOs
            List<Map<String, Object>> data = pagosFiltrados.stream()
                    .map(pago -> convertirPagoAMap(pago, requestId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Aplicar paginación manual si es necesario
            int totalElements = data.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<Map<String, Object>> paginatedData = data.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedData);
            response.put("recordsTotal", totalElements);
            response.put("recordsFiltered", totalElements);
            response.put("currentPage", page);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("requestId", requestId);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ [" + requestId + "] Consulta completada en " + duration + "ms");
            System.out.println("💰 [" + requestId + "] Pagos encontrados: " + totalElements);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("❌ [" + requestId + "] Error después de " + duration + "ms: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener estadísticas de pagos del vecino
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @RequestParam(required = false, name = "dateRange[start]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false, name = "dateRange[end]") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            HttpSession session
    ) {
        try {
            System.out.println("📊 Calculando estadísticas de pagos...");

            Usuarios vecino = (Usuarios) session.getAttribute("usuario");
            if (vecino == null) {
                return ResponseEntity.status(401).build();
            }

            List<Pago> todosPagos = pagoRepositoryVecino.findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(
                    vecino.getIdUsuarios()
            );

            // Aplicar filtros de fecha si existen
            if (fechaInicio != null || fechaFin != null) {
                LocalDateTime fechaInicioDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
                LocalDateTime fechaFinDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

                todosPagos = todosPagos.stream()
                        .filter(pago -> {
                            LocalDateTime fechaPago = pago.getFechaPago();
                            if (fechaInicioDateTime != null && fechaPago.isBefore(fechaInicioDateTime)) {
                                return false;
                            }
                            if (fechaFinDateTime != null && fechaPago.isAfter(fechaFinDateTime)) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }

            // Calcular estadísticas
            long totalPagos = todosPagos.size();

            long pagosOnline = todosPagos.stream()
                    .filter(p -> "En línea".equals(p.getTipoPago()))
                    .count();

            long pagosBanco = todosPagos.stream()
                    .filter(p -> "En banco".equals(p.getTipoPago()))
                    .count();

            double montoTotal = todosPagos.stream()
                    .filter(p -> p.getMonto() != null)
                    .mapToDouble(p -> p.getMonto().doubleValue())
                    .sum();

            double montoPromedio = totalPagos > 0 ? montoTotal / totalPagos : 0.0;

            // Pagos por mes (últimos 6 meses)
            Map<String, Double> pagosPorMes = calcularPagosPorMes(todosPagos);

            // Espacios más utilizados
            Map<String, Long> espaciosMasUtilizados = todosPagos.stream()
                    .filter(p -> p.getReserva() != null && p.getReserva().getEspacio() != null)
                    .collect(Collectors.groupingBy(
                            p -> p.getReserva().getEspacio().getNombre(),
                            Collectors.counting()
                    ));

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("totalPagos", totalPagos);
            estadisticas.put("pagosOnline", pagosOnline);
            estadisticas.put("pagosBanco", pagosBanco);
            estadisticas.put("montoTotal", montoTotal);
            estadisticas.put("montoPromedio", montoPromedio);
            estadisticas.put("pagosPorMes", pagosPorMes);
            estadisticas.put("espaciosFavoritos", espaciosMasUtilizados);

            System.out.println("✅ Estadísticas calculadas: Total=" + totalPagos +
                    ", Online=" + pagosOnline + ", Banco=" + pagosBanco +
                    ", MontoTotal=" + montoTotal);

            return ResponseEntity.ok(estadisticas);

        } catch (Exception e) {
            System.err.println("❌ Error calculando estadísticas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener detalles de un pago específico
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetallePago(
            @PathVariable Integer id,
            HttpSession session
    ) {
        try {
            System.out.println("🔍 Obteniendo detalles de pago ID: " + id);

            Usuarios vecino = (Usuarios) session.getAttribute("usuario");
            if (vecino == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<Pago> pagoOpt = pagoRepositoryVecino.findById(id);
            if (!pagoOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Pago pago = pagoOpt.get();

            // Verificar que el pago pertenece al vecino
            if (!pago.getReserva().getVecino().getIdUsuarios().equals(vecino.getIdUsuarios())) {
                return ResponseEntity.status(403).build();
            }

            Map<String, Object> detalles = convertirPagoAMapDetallado(pago);
            return ResponseEntity.ok(detalles);

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo detalles de pago: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Aplicar filtros a la lista de pagos
     */
    private List<Pago> aplicarFiltros(List<Pago> pagos, LocalDate fechaInicio, LocalDate fechaFin,
                                      String tipoPago, String estado, Integer espacio, String requestId) {

        System.out.println("🔍 [" + requestId + "] Aplicando filtros...");

        return pagos.stream()
                .filter(pago -> {
                    try {
                        // Filtro por fecha
                        if (fechaInicio != null && pago.getFechaPago().toLocalDate().isBefore(fechaInicio)) {
                            return false;
                        }
                        if (fechaFin != null && pago.getFechaPago().toLocalDate().isAfter(fechaFin)) {
                            return false;
                        }

                        // Filtro por tipo de pago
                        if (tipoPago != null && !tipoPago.isEmpty() && !tipoPago.equals(pago.getTipoPago())) {
                            return false;
                        }

                        // Filtro por estado
                        if (estado != null && !estado.isEmpty() && !estado.equals(pago.getEstado())) {
                            return false;
                        }

                        // Filtro por espacio
                        if (espacio != null) {
                            if (pago.getReserva() == null || pago.getReserva().getEspacio() == null ||
                                    !espacio.equals(pago.getReserva().getEspacio().getIdEspacio())) {
                                return false;
                            }
                        }

                        return true;
                    } catch (Exception e) {
                        System.err.println("⚠️ [" + requestId + "] Error filtrando pago: " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Convertir pago a Map para DataTable
     */
    private Map<String, Object> convertirPagoAMap(Pago pago, String requestId) {
        try {
            if (pago == null) {
                return null;
            }

            Map<String, Object> map = new HashMap<>();

            // Información básica del pago
            map.put("idPago", pago.getIdPago());
            map.put("fechaPago", pago.getFechaPago() != null ? pago.getFechaPago().toString() : "");
            map.put("monto", pago.getMonto() != null ? pago.getMonto().doubleValue() : 0.0);
            map.put("tipoPago", pago.getTipoPago() != null ? pago.getTipoPago() : "");
            map.put("estado", pago.getEstado() != null ? pago.getEstado() : "");
            map.put("referencia", pago.getReferencia() != null ? pago.getReferencia() : "");

            // Información de la reserva
            if (pago.getReserva() != null) {
                Reserva reserva = pago.getReserva();
                map.put("idReserva", reserva.getIdReserva());
                map.put("fechaReserva", reserva.getFecha() != null ? reserva.getFecha().toString() : "");
                map.put("horaInicio", reserva.getHoraInicio() != null ? reserva.getHoraInicio().toString() : "");
                map.put("horaFin", reserva.getHoraFin() != null ? reserva.getHoraFin().toString() : "");

                // Información del espacio
                if (reserva.getEspacio() != null) {
                    map.put("espacioNombre", reserva.getEspacio().getNombre());
                    map.put("espacioTipo", reserva.getEspacio().getTipoEspacio() != null ?
                            reserva.getEspacio().getTipoEspacio().getNombre() : "");
                    map.put("espacioLugar", reserva.getEspacio().getIdLugar() != null ?
                            reserva.getEspacio().getIdLugar().getLugar() : "");
                } else {
                    map.put("espacioNombre", "N/A");
                    map.put("espacioTipo", "N/A");
                    map.put("espacioLugar", "N/A");
                }
            } else {
                map.put("idReserva", null);
                map.put("fechaReserva", "");
                map.put("horaInicio", "");
                map.put("horaFin", "");
                map.put("espacioNombre", "N/A");
                map.put("espacioTipo", "N/A");
                map.put("espacioLugar", "N/A");
            }

            return map;

        } catch (Exception e) {
            System.err.println("❌ [" + requestId + "] Error convirtiendo pago: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convertir pago a Map detallado para modal
     */
    private Map<String, Object> convertirPagoAMapDetallado(Pago pago) {
        Map<String, Object> map = convertirPagoAMap(pago, "DETALLE");

        if (map == null) return new HashMap<>();

        // Agregar información adicional
        if (pago.getReserva() != null) {
            map.put("estadoReserva", pago.getReserva().getEstado() != null ?
                    pago.getReserva().getEstado().getEstado() : "");
            map.put("momentoReserva", pago.getReserva().getMomentoReserva() != null ?
                    pago.getReserva().getMomentoReserva().toString() : "");
        }

        return map;
    }

    /**
     * Calcular pagos por mes (últimos 6 meses)
     */
    private Map<String, Double> calcularPagosPorMes(List<Pago> pagos) {
        Map<String, Double> pagosPorMes = new LinkedHashMap<>();

        // Generar últimos 6 meses
        LocalDate fechaActual = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate fecha = fechaActual.minusMonths(i);
            String nombreMes = fecha.getMonth().name() + " " + fecha.getYear();
            pagosPorMes.put(nombreMes, 0.0);
        }

        // Sumar pagos por mes
        pagos.forEach(pago -> {
            if (pago.getFechaPago() != null && pago.getMonto() != null) {
                LocalDate fechaPago = pago.getFechaPago().toLocalDate();
                String nombreMes = fechaPago.getMonth().name() + " " + fechaPago.getYear();

                if (pagosPorMes.containsKey(nombreMes)) {
                    pagosPorMes.put(nombreMes,
                            pagosPorMes.get(nombreMes) + pago.getMonto().doubleValue());
                }
            }
        });

        return pagosPorMes;
    }
}