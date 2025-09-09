package com.example.project.controller;

import com.example.project.dto.*;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.admin.ListaCoordinadoresDto;
import com.example.project.repository.admin.ListaCoordinadoresRepository;
import com.example.project.repository.admin.LugarRepositoryAdmin;
import com.example.project.repository.coordinador.ActividadRepository;
import com.example.project.service.CoordinadorImpactoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/coordinador")
public class AdminCoordinadoresController {

    @Autowired private CoordinadorImpactoService coordinadorImpactoService;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private LugarRepositoryAdmin lugarRepositoryAdmin;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private SolicitudCancelacionRepository solicitudCancelacionRepository;
    @Autowired private ReembolsoRepository reembolsoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private ListaCoordinadoresRepository listaCoordinadoresRepository;

    @GetMapping("/lista-coordinadores")
    public String listarCoordinadores(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        List<ListaCoordinadoresDto> coordinadores = listaCoordinadoresRepository.listarCoordinadores();
        model.addAttribute("coordinadores", coordinadores);
        model.addAttribute("lugares", lugarRepositoryAdmin.findAll());
        return "admin/lista_coordinadores";
    }

    @GetMapping("/coordinadores-datatable")
    @ResponseBody
    public Map<String, Object> obtenerCoordinadoresDatatable(HttpServletRequest request) {
        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        String searchValue = request.getParameter("search[value]");

        int page = start / length;
        Pageable pageable = PageRequest.of(page, length, Sort.by("nombres").ascending());

        Page<Usuarios> pageResult;
        if (searchValue != null && !searchValue.isBlank()) {
            pageResult = usuariosRepository.buscarCoordinadoresConTodo(searchValue, pageable);
        } else {
            pageResult = usuariosRepository.findByRol("Coordinador", pageable);
        }

        List<Map<String, Object>> data = pageResult.getContent().stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("idUsuarios", u.getIdUsuarios());
            row.put("nombres", u.getNombres());
            row.put("apellidos", u.getApellidos());
            row.put("dni", u.getDni());
            row.put("correo", u.getCorreo());
            row.put("estado", u.getEstado().getEstado());

            // Formatear lugares asignados
            String lugaresTexto = formatearLugares(u.getLugaresAsignados());
            row.put("lugaresAsignados", lugaresTexto);

            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        response.put("recordsTotal", pageResult.getTotalElements());
        response.put("recordsFiltered", pageResult.getTotalElements());
        response.put("data", data);
        return response;
    }

    /**
     * Obtener datos del coordinador para editar
     */
    @GetMapping("/{id}/lugares")
    @ResponseBody
    public Map<String, Object> obtenerDatosCoordinador(@PathVariable Integer id) {
        Usuarios coordinador = usuariosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));

        List<Lugar> todosLugares = lugarRepositoryAdmin.findAll();
        List<Integer> lugaresAsignados = coordinador.getLugaresAsignados().stream()
                .map(Lugar::getIdLugar)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("coordinador", Map.of(
                "id", coordinador.getIdUsuarios(),
                "nombre", coordinador.getNombres() + " " + coordinador.getApellidos(),
                "lugaresAsignados", lugaresAsignados
        ));
        response.put("todosLugares", todosLugares.stream().map(lugar -> Map.of(
                "id", lugar.getIdLugar(),
                "nombre", lugar.getLugar()
        )).collect(Collectors.toList()));

        return response;
    }

    /**
     * Verificar impacto antes de cambiar (método original - mantener por compatibilidad)
     */
    @GetMapping("/{id}/impacto")
    @ResponseBody
    public Map<String, Object> verificarImpacto(@PathVariable Integer id) {
        Usuarios coordinador = usuariosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));

        Map<String, Object> impacto = new HashMap<>();

        // Contar reservas activas
        long reservasActivas = reservaRepository.countByCoordinadorAndEstado_Estado(coordinador, "Confirmada");
        impacto.put("reservasActivas", reservasActivas);

        // Contar mantenimientos pendientes
        long mantenimientosPendientes = mantenimientoRepository.countByResponsableAndEstado(
                coordinador, Mantenimiento.EstadoMantenimiento.PROGRAMADO);
        impacto.put("mantenimientosPendientes", mantenimientosPendientes);

        // Contar solicitudes de cancelación pendientes
        long solicitudesPendientes = solicitudCancelacionRepository.countByReserva_CoordinadorAndEstado(
                coordinador, "Pendiente");
        impacto.put("solicitudesPendientes", solicitudesPendientes);

        // Contar reembolsos pendientes
        long reembolsosPendientes = reembolsoRepository.countByAprobadoPorCoordinadorAndEstadoReembolso(
                coordinador, Reembolso.EstadoReembolso.PENDIENTE);
        impacto.put("reembolsosPendientes", reembolsosPendientes);

        return impacto;
    }

    /**
     * Endpoint para analizar el impacto antes de cambiar lugares
     */
    @PostMapping("/{id}/lugares/analizar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analizarImpactoCambioLugares(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        try {
            System.out.println("🔍 Analizando impacto para coordinador ID: " + id);
            System.out.println("🔍 Request recibido: " + request);

            // Verificar autenticación
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            // Obtener coordinador
            Usuarios coordinador = usuariosRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));

            // 🔧 FIX: Manejar la conversión de String a Integer correctamente
            @SuppressWarnings("unchecked")
            List<Object> nuevosLugaresIdsRaw = (List<Object>) request.get("lugaresIds");

            List<Lugar> nuevosLugares = new ArrayList<>();
            if (nuevosLugaresIdsRaw != null && !nuevosLugaresIdsRaw.isEmpty()) {
                for (Object lugarIdObj : nuevosLugaresIdsRaw) {
                    // Convertir a Integer sin importar si viene como String o Integer
                    Integer lugarId;
                    if (lugarIdObj instanceof String) {
                        lugarId = Integer.parseInt((String) lugarIdObj);
                    } else if (lugarIdObj instanceof Integer) {
                        lugarId = (Integer) lugarIdObj;
                    } else {
                        throw new RuntimeException("Formato de ID de lugar inválido: " + lugarIdObj);
                    }

                    System.out.println("🔍 Procesando lugar ID: " + lugarId + " (tipo: " + lugarIdObj.getClass().getSimpleName() + ")");

                    Lugar lugar = lugarRepositoryAdmin.findById(lugarId)
                            .orElseThrow(() -> new RuntimeException("Lugar con ID " + lugarId + " no encontrado"));
                    nuevosLugares.add(lugar);
                }
            }

            System.out.println("🔍 Total lugares procesados: " + nuevosLugares.size());

            // Realizar análisis de impacto
            ImpactoAnalisis analisis = coordinadorImpactoService.analizarCambioLugares(coordinador, nuevosLugares);

            // Convertir a formato JSON amigable
            Map<String, Object> response = convertirAnalisisAMap(analisis);
            response.put("coordinador", Map.of(
                    "id", coordinador.getIdUsuarios(),
                    "nombre", coordinador.getNombres() + " " + coordinador.getApellidos()
            ));

            System.out.println("✅ Análisis completado. Estrategia: " + analisis.getEstrategiaRecomendada());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error analizando impacto: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error analizando impacto: " + e.getMessage()));
        }
    }

    /**
     * Endpoint mejorado para actualizar lugares con manejo de impacto
     */
    @PostMapping("/{id}/lugares")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarLugaresCoordinador(
            @PathVariable Integer id,
            @RequestParam(value = "lugaresIds", required = false) List<Integer> lugaresIds,
            @RequestParam(value = "reasignarReservas", defaultValue = "false") boolean reasignarReservas,
            @RequestBody(required = false) Map<String, Object> planReasignacion,
            HttpSession session) {

        try {
            System.out.println("🔄 Actualizando lugares para coordinador ID: " + id);

            // Verificar autenticación
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            Usuarios coordinador = usuariosRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));

            // Guardar lugares anteriores para el log
            List<String> lugaresAnteriores = coordinador.getLugaresAsignados().stream()
                    .map(Lugar::getLugar)
                    .collect(Collectors.toList());

            // Preparar nuevos lugares
            List<Lugar> nuevosLugares = new ArrayList<>();
            if (lugaresIds != null && !lugaresIds.isEmpty()) {
                for (Integer lugarId : lugaresIds) {
                    Lugar lugar = lugarRepositoryAdmin.findById(lugarId)
                            .orElseThrow(() -> new RuntimeException("Lugar con ID " + lugarId + " no encontrado"));
                    nuevosLugares.add(lugar);
                }
            }

            // Realizar análisis de impacto
            ImpactoAnalisis analisis = coordinadorImpactoService.analizarCambioLugares(coordinador, nuevosLugares);

            // Verificar si el cambio es posible
            if (!analisis.isPuedeRealizarseCambio()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "No se puede realizar el cambio",
                                "motivo", analisis.getMotivoBloqueo(),
                                "analisis", convertirAnalisisAMap(analisis)
                        ));
            }

            ResultadoReasignacion resultado = null;

            // Si hay impacto y se solicita reasignación automática
            if (!analisis.getLugaresAfectados().isEmpty() && reasignarReservas) {

                // Extraer selecciones manuales si las hay
                Map<Integer, Integer> seleccionesManual = new HashMap<>();
                if (planReasignacion != null && planReasignacion.containsKey("seleccionesManual")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> selecciones = (Map<String, Integer>) planReasignacion.get("seleccionesManual");
                    for (Map.Entry<String, Integer> entry : selecciones.entrySet()) {
                        seleccionesManual.put(Integer.parseInt(entry.getKey()), entry.getValue());
                    }
                }

                // Crear y ejecutar plan de reasignación
                PlanReasignacion plan = coordinadorImpactoService.crearPlanReasignacion(analisis, seleccionesManual);

                // Verificar si hay reasignaciones manuales pendientes
                if (!plan.getReasignacionesManuales().isEmpty() && seleccionesManual.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error", "Se requieren selecciones manuales",
                                    "planReasignacion", convertirPlanAMap(plan),
                                    "analisis", convertirAnalisisAMap(analisis)
                            ));
                }

                // Ejecutar plan
                resultado = coordinadorImpactoService.ejecutarPlan(plan, coordinador, nuevosLugares);

            } else {
                // Cambio simple sin reasignación
                coordinador.setLugaresAsignados(nuevosLugares);
                usuariosRepository.save(coordinador);
            }

            // Registrar actividad
            registrarActividadCambioLugares(coordinador, lugaresAnteriores, nuevosLugares, session);

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Lugares actualizados correctamente");

            if (resultado != null) {
                response.put("reasignacion", Map.of(
                        "exitoso", resultado.isExitoso(),
                        "mensaje", resultado.getMensaje(),
                        "entidadesReasignadas", resultado.getEntidadesReasignadas(),
                        "resumen", resultado.getResumenCompleto()
                ));
            }

            System.out.println("✅ Lugares actualizados exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error actualizando lugares: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para obtener coordinadores disponibles para un lugar específico
     */
    @GetMapping("/lugar/{lugarId}/coordinadores-disponibles")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerCoordinadoresDisponibles(
            @PathVariable Integer lugarId,
            @RequestParam(required = false) Integer excluirCoordinadorId,
            HttpSession session) {

        try {
            // Verificar autenticación
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Lugar lugar = lugarRepositoryAdmin.findById(lugarId)
                    .orElseThrow(() -> new RuntimeException("Lugar no encontrado"));

            // Buscar coordinadores activos asignados a este lugar
            List<Usuarios> coordinadores = usuariosRepository.findByRol_RolAndEstado_EstadoAndLugaresAsignadosContaining(
                    "Coordinador", "Activo", lugar);

            // Excluir coordinador específico si se solicita
            if (excluirCoordinadorId != null) {
                coordinadores = coordinadores.stream()
                        .filter(c -> !c.getIdUsuarios().equals(excluirCoordinadorId))
                        .collect(Collectors.toList());
            }

            // Convertir a formato JSON con información de carga de trabajo
            List<Map<String, Object>> resultado = coordinadores.stream()
                    .map(coordinador -> {
                        Map<String, Object> coordMap = new HashMap<>();
                        coordMap.put("id", coordinador.getIdUsuarios());
                        coordMap.put("nombre", coordinador.getNombres() + " " + coordinador.getApellidos());
                        coordMap.put("correo", coordinador.getCorreo());

                        // Calcular carga de trabajo actual
                        int cargaTrabajo = calcularCargaTrabajoCoordinador(coordinador);
                        coordMap.put("cargaTrabajo", cargaTrabajo);
                        coordMap.put("descripcionCarga", generarDescripcionCarga(cargaTrabajo));

                        return coordMap;
                    })
                    .sorted((c1, c2) -> Integer.compare((Integer) c1.get("cargaTrabajo"), (Integer) c2.get("cargaTrabajo")))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo coordinadores disponibles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Método original para manejar impacto (mantenido para compatibilidad)
     */
    private void manejarImpactoReservas(Usuarios coordinador, List<Lugar> nuevosLugares) {
        // Lógica original mantenida
        List<Reserva> reservasAfectadas = reservaRepository.findByCoordinadorAndEstado_EstadoIn(
                coordinador, Arrays.asList("Confirmada", "Pendiente de confirmación"));

        for (Reserva reserva : reservasAfectadas) {
            // Verificar si el espacio de la reserva está en los nuevos lugares
            boolean espacioEnNuevosLugares = nuevosLugares.stream()
                    .anyMatch(lugar -> lugar.equals(reserva.getEspacio().getIdLugar()));

            if (!espacioEnNuevosLugares) {
                // Buscar otro coordinador para este lugar
                // O marcar para revisión manual
                // Por ahora, solo agregar una observación o cambiar estado
            }
        }
    }

    /**
     * Registrar actividad de cambio de lugares
     */
    private void registrarActividadCambioLugares(Usuarios coordinador, List<String> lugaresAnteriores,
                                                 List<Lugar> nuevosLugares, HttpSession session) {
        try {
            String nuevosLugaresTexto = nuevosLugares.stream()
                    .map(Lugar::getLugar)
                    .collect(Collectors.joining(", "));

            Actividad actividad = new Actividad();
            actividad.setUsuario((Usuarios) session.getAttribute("usuario"));
            actividad.setDescripcion("Actualización de Lugares de Coordinador");
            actividad.setDetalle(String.format("Cambió los lugares del coordinador %s %s. Anteriores: [%s]. Nuevos: [%s]",
                    coordinador.getNombres(), coordinador.getApellidos(),
                    String.join(", ", lugaresAnteriores), nuevosLugaresTexto));
            actividad.setFecha(LocalDateTime.now());
            actividadRepository.save(actividad);
        } catch (Exception e) {
            System.err.println("⚠️ Error registrando actividad: " + e.getMessage());
        }
    }

    // Métodos auxiliares para conversión de datos
    private Map<String, Object> convertirAnalisisAMap(ImpactoAnalisis analisis) {
        Map<String, Object> map = new HashMap<>();

        map.put("estrategia", analisis.getEstrategiaRecomendada().toString());
        map.put("puedeRealizarse", analisis.isPuedeRealizarseCambio());
        map.put("motivoBloqueo", analisis.getMotivoBloqueo());
        map.put("estadisticas", analisis.getEstadisticas());

        // Convertir lugares afectados
        List<Map<String, Object>> lugaresAfectados = analisis.getLugaresAfectados().stream()
                .map(this::convertirLugarImpactoAMap)
                .collect(Collectors.toList());
        map.put("lugaresAfectados", lugaresAfectados);

        // Convertir acciones requeridas
        List<Map<String, Object>> acciones = analisis.getAccionesRequeridas().stream()
                .map(this::convertirAccionAMap)
                .collect(Collectors.toList());
        map.put("accionesRequeridas", acciones);

        return map;
    }

    private Map<String, Object> convertirLugarImpactoAMap(LugarImpacto lugarImpacto) {
        Map<String, Object> map = new HashMap<>();

        map.put("lugar", Map.of(
                "id", lugarImpacto.getLugar().getIdLugar(),
                "nombre", lugarImpacto.getLugar().getLugar()
        ));

        map.put("totalEntidadesAfectadas", lugarImpacto.getTotalEntidadesAfectadas());
        map.put("requiereIntervencionManual", lugarImpacto.isRequiereIntervencionManual());
        map.put("razonProblema", lugarImpacto.getRazonProblema());

        // Detalles de entidades afectadas
        Map<String, Integer> detalleEntidades = new HashMap<>();
        detalleEntidades.put("reservas", lugarImpacto.getReservasAfectadas().size());
        detalleEntidades.put("mantenimientos", lugarImpacto.getMantenimientosAfectados().size());
        detalleEntidades.put("solicitudes", lugarImpacto.getSolicitudesAfectadas().size());
        detalleEntidades.put("reembolsos", lugarImpacto.getReembolsosAfectados().size());
        map.put("detalleEntidades", detalleEntidades);

        // Coordinadores disponibles
        List<Map<String, ? extends Serializable>> coordinadores = lugarImpacto.getCoordinadoresDisponibles().stream()
                .map(coord -> Map.of(
                        "id", coord.getIdUsuarios(),
                        "nombre", coord.getNombres() + " " + coord.getApellidos(),
                        "cargaTrabajo", calcularCargaTrabajoCoordinador(coord)
                ))
                .collect(Collectors.toList());
        map.put("coordinadoresDisponibles", coordinadores);

        return map;
    }

    private Map<String, Object> convertirAccionAMap(AccionRequerida accion) {
        Map<String, Object> map = new HashMap<>();
        map.put("tipo", accion.getTipo().toString());
        map.put("descripcion", accion.getDescripcion());
        map.put("entidadAfectada", accion.getEntidadAfectada());
        map.put("cantidadAfectada", accion.getCantidadAfectada());
        map.put("esOpcional", accion.isEsOpcional());
        map.put("solucionSugerida", accion.getSolucionSugerida());
        return map;
    }

    private Map<String, Object> convertirPlanAMap(PlanReasignacion plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("resumenCambios", plan.getResumenCambios());
        map.put("requiereConfirmacionAdicional", plan.isRequiereConfirmacionAdicional());

        // Convertir reasignaciones manuales
        List<Map<String, Object>> manuales = plan.getReasignacionesManuales().stream()
                .map(manual -> Map.of(
                        "lugar", Map.of(
                                "id", manual.getLugar().getIdLugar(),
                                "nombre", manual.getLugar().getLugar()
                        ),
                        "entidadesAfectadas", manual.getEntidadesAfectadas(),
                        "coordinadoresCandidatos", manual.getCoordinadoresCandidatos().stream()
                                .map(coord -> Map.of(
                                        "id", coord.getIdUsuarios(),
                                        "nombre", coord.getNombres() + " " + coord.getApellidos(),
                                        "cargaTrabajo", calcularCargaTrabajoCoordinador(coord)
                                ))
                                .collect(Collectors.toList()),
                        "esObligatoria", manual.isEsObligatoria(),
                        "descripcionImpacto", manual.getDescripcionImpacto()
                ))
                .collect(Collectors.toList());
        map.put("reasignacionesManuales", manuales);

        return map;
    }

    private int calcularCargaTrabajoCoordinador(Usuarios coordinador) {
        // Reutilizar la lógica del servicio
        return coordinadorImpactoService.calcularCargaTrabajo(coordinador);
    }

    private String generarDescripcionCarga(int carga) {
        if (carga == 0) return "Sin carga de trabajo";
        if (carga <= 5) return "Carga ligera";
        if (carga <= 15) return "Carga moderada";
        if (carga <= 25) return "Carga alta";
        return "Carga muy alta";
    }

    private String formatearLugares(List<Lugar> lugares) {
        if (lugares == null || lugares.isEmpty()) {
            return "Sin lugares asignados";
        }
        return lugares.stream()
                .map(Lugar::getLugar)
                .collect(Collectors.joining(", "));
    }
}