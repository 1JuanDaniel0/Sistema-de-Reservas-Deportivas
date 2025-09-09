package com.example.project.controller.api;

import com.example.project.dto.MantenimientoDTO;
import com.example.project.dto.ReservaCalendarioDto;
import com.example.project.entity.*;
import com.example.project.repository.EspacioRepositoryGeneral;
import com.example.project.repository.MantenimientoRepository;
import com.example.project.repository.ReservaRepository;
import com.example.project.repository.UsuariosRepository;
import com.example.project.service.MantenimientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/mantenimiento")
public class AdminMantenimientoApiController {

    @Autowired private MantenimientoService mantenimientoService;
    @Autowired private EspacioRepositoryGeneral espacioRepositoryGeneral;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private ReservaRepository reservaRepository;

    // ENDPOINT: API para programar mantenimiento
    @PostMapping("/programar")
    @ResponseBody
    public ResponseEntity<?> programarMantenimiento(
            @RequestBody MantenimientoDTO mantenimientoDTO,
            HttpSession session) {

        try {
            System.out.println("=== RECIBIENDO SOLICITUD DE MANTENIMIENTO ===");
            System.out.println("DTO: " + mantenimientoDTO.getEspacioId() + " - " +
                    mantenimientoDTO.getFechaInicio() + " a " + mantenimientoDTO.getFechaFin());

            // Verificar usuario autenticado
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            // Programar mantenimiento
            Mantenimiento mantenimiento = mantenimientoService.programarMantenimiento(mantenimientoDTO, admin);

            // Respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mantenimiento programado exitosamente");
            response.put("mantenimientoId", mantenimiento.getIdMantenimiento());
            response.put("espacioNombre", mantenimiento.getEspacio().getNombre());
            response.put("fechaInicio", mantenimiento.getFechaInicio().toString());
            response.put("fechaFin", mantenimiento.getFechaFin().toString());
            response.put("tipo", mantenimiento.getTipoMantenimiento().getDescripcion());

            System.out.println("✅ Mantenimiento programado exitosamente: ID " + mantenimiento.getIdMantenimiento());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            System.err.println("❌ Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @PostMapping("/verificar-conflictos")
    @ResponseBody
    public ResponseEntity<?> verificarConflictos(
            @RequestBody MantenimientoDTO mantenimientoDTO,
            HttpSession session) {

        try {
            System.out.println("=== VERIFICANDO CONFLICTOS ===");
            System.out.println("DTO recibido: " + mantenimientoDTO.getEspacioId() + " - " +
                    mantenimientoDTO.getFechaInicio() + " a " + mantenimientoDTO.getFechaFin());

            // Verificar usuario autenticado
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            // Buscar espacio
            Espacio espacio = espacioRepositoryGeneral.findById(mantenimientoDTO.getEspacioId())
                    .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> conflictos = new ArrayList<>();

            try {
                // Convertir strings a LocalDate y LocalTime
                LocalDate fechaInicio = mantenimientoDTO.getFechaInicioAsLocalDate();
                LocalDate fechaFin = mantenimientoDTO.getFechaFinAsLocalDate();
                LocalTime horaInicio = mantenimientoDTO.getHoraInicioAsLocalTime();
                LocalTime horaFin = mantenimientoDTO.getHoraFinAsLocalTime();

                if (fechaInicio == null || fechaFin == null || horaInicio == null || horaFin == null) {
                    throw new IllegalArgumentException("Fechas y horas son obligatorias");
                }

                // Verificar conflictos con reservas usando los nuevos métodos
                mantenimientoService.verificarConflictosReservas(espacio, fechaInicio, fechaFin, horaInicio, horaFin);

                // Verificar conflictos con mantenimientos usando los nuevos métodos
                mantenimientoService.verificarConflictosMantenimientos(espacio, fechaInicio, fechaFin, horaInicio, horaFin, null);

                response.put("hayConflictos", false);
                response.put("message", "No se encontraron conflictos");

                System.out.println("✅ No se encontraron conflictos");

            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Conflictos encontrados: " + e.getMessage());

                response.put("hayConflictos", true);
                response.put("message", e.getMessage());

                // Agregar detalles específicos de los conflictos encontrados
                Map<String, Object> detalleConflicto = new HashMap<>();
                detalleConflicto.put("tipo", "validacion");
                detalleConflicto.put("descripcion", e.getMessage());
                detalleConflicto.put("espacio", espacio.getNombre());
                conflictos.add(detalleConflicto);
            }

            response.put("conflictos", conflictos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error verificando conflictos: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("hayConflictos", true);
            errorResponse.put("message", "Error al verificar conflictos");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/eventos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEventosMantenimiento(
            @RequestParam Integer espacioId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fechaFin,
            HttpSession session) {

        try {
            // Verificar autenticación
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Fechas por defecto si no se especifican
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().minusMonths(1);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now().plusMonths(2);
            }

            // Buscar espacio
            Espacio espacio = espacioRepositoryGeneral.findById(espacioId)
                    .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));

            // Obtener reservas
            List<ReservaCalendarioDto> reservas = reservaRepository.buscarReservasParaCalendario(
                    espacio.getIdEspacio().longValue(),
                    Arrays.asList("Confirmada", "Pendiente de confirmación", "Finalizada", "Cancelada", "Cancelada con reembolso", "Reembolso solicitado", "Cancelada sin reembolso"),
                    fechaInicio,
                    fechaFin
            );

            // Obtener mantenimientos
            List<Mantenimiento> mantenimientos = mantenimientoRepository.findByEspacioAndFechaBetween(
                    espacio, fechaInicio, fechaFin
            );

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("espacio", Map.of(
                    "idEspacio", espacio.getIdEspacio(),
                    "nombre", espacio.getNombre(),
                    "lugar", espacio.getIdLugar() != null ? espacio.getIdLugar().getLugar() : ""
            ));
            response.put("reservas", reservas);
            response.put("mantenimientos", convertirMantenimientosParaJSON(mantenimientos));
            response.put("fechaInicio", fechaInicio.toString());
            response.put("fechaFin", fechaFin.toString());

            System.out.println("📊 API: Eventos obtenidos para espacio " + espacio.getNombre() +
                    " - Reservas: " + reservas.size() + ", Mantenimientos: " + mantenimientos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo eventos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error obteniendo eventos: " + e.getMessage()));
        }
    }

    // Método helper para convertir mantenimientos a formato JSON
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
        map.put("fechaCreacion", mantenimiento.getFechaCreacion() != null ?
                mantenimiento.getFechaCreacion().toString() : null);

        // Información del responsable
        if (mantenimiento.getResponsable() != null) {
            Map<String, Object> responsable = new HashMap<>();
            responsable.put("nombres", mantenimiento.getResponsable().getNombres());
            responsable.put("apellidos", mantenimiento.getResponsable().getApellidos());
            map.put("responsable", responsable);
        }

        // Información de quien creó
        if (mantenimiento.getCreadoPor() != null) {
            Map<String, Object> creadoPor = new HashMap<>();
            creadoPor.put("nombres", mantenimiento.getCreadoPor().getNombres());
            creadoPor.put("apellidos", mantenimiento.getCreadoPor().getApellidos());
            map.put("creadoPor", creadoPor);
        }

        return map;
    }
}
