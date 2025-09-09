// CoordinadorImpactoService.java
package com.example.project.service;

import com.example.project.dto.*;
import com.example.project.entity.*;
import com.example.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoordinadorImpactoService {

    @Autowired private ReservaRepository reservaRepository;
    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private SolicitudCancelacionRepository solicitudCancelacionRepository;
    @Autowired private ReembolsoRepository reembolsoRepository;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private LugarRepository lugarRepository;

    /**
     * Analiza el impacto de cambiar los lugares asignados a un coordinador
     */
    public ImpactoAnalisis analizarCambioLugares(Usuarios coordinador, List<Lugar> nuevosLugares) {
        System.out.println("🔍 Analizando impacto para coordinador: " + coordinador.getNombres());

        ImpactoAnalisis analisis = new ImpactoAnalisis();
        List<LugarImpacto> lugaresAfectados = new ArrayList<>();

        // Obtener lugares actuales del coordinador
        List<Lugar> lugaresActuales = coordinador.getLugaresAsignados();

        // Encontrar lugares que se van a quitar
        List<Lugar> lugaresAQuitar = lugaresActuales.stream()
                .filter(lugar -> !nuevosLugares.contains(lugar))
                .collect(Collectors.toList());

        System.out.println("📍 Lugares a quitar: " + lugaresAQuitar.size());

        // Analizar impacto por cada lugar que se quita
        for (Lugar lugar : lugaresAQuitar) {
            LugarImpacto impactoLugar = analizarImpactoLugar(coordinador, lugar);
            if (impactoLugar.getTotalEntidadesAfectadas() > 0) {
                lugaresAfectados.add(impactoLugar);
            }
        }

        analisis.setLugaresAfectados(lugaresAfectados);

        // Determinar estrategia basada en el análisis
        determinarEstrategia(analisis);

        // Generar acciones requeridas
        generarAccionesRequeridas(analisis);

        // Calcular estadísticas
        calcularEstadisticas(analisis);

        System.out.println("✅ Análisis completado. Estrategia: " + analisis.getEstrategiaRecomendada());

        return analisis;
    }

    /**
     * Analiza el impacto en un lugar específico
     */
    private LugarImpacto analizarImpactoLugar(Usuarios coordinador, Lugar lugar) {
        LugarImpacto impacto = new LugarImpacto();
        impacto.setLugar(lugar);

        // Buscar reservas activas que requieren reasignación (solo estados específicos)
        List<String> estadosReasignables = Arrays.asList("Confirmada", "Pendiente de confirmación", "Reembolso solicitado");

        List<Reserva> reservasAfectadas = reservaRepository.findByCoordinadorAndEstado_EstadoIn(coordinador, estadosReasignables)
                .stream()
                .filter(reserva -> reserva.getEspacio() != null &&
                        lugar.equals(reserva.getEspacio().getIdLugar()))
                .collect(Collectors.toList());

        // Buscar mantenimientos activos
        List<Mantenimiento> mantenimientosAfectados = mantenimientoRepository.findByResponsableAndEstadoIn(
                        coordinador, Arrays.asList(Mantenimiento.EstadoMantenimiento.PROGRAMADO, Mantenimiento.EstadoMantenimiento.EN_PROCESO))
                .stream()
                .filter(mantenimiento -> mantenimiento.getEspacio() != null &&
                        lugar.equals(mantenimiento.getEspacio().getIdLugar()))
                .collect(Collectors.toList());

        // Buscar solicitudes de cancelación pendientes
        List<SolicitudCancelacion> solicitudesAfectadas = solicitudCancelacionRepository.findByReserva_CoordinadorAndEstado(coordinador, "Pendiente")
                .stream()
                .filter(solicitud -> solicitud.getReserva() != null &&
                        solicitud.getReserva().getEspacio() != null &&
                        lugar.equals(solicitud.getReserva().getEspacio().getIdLugar()))
                .collect(Collectors.toList());

        // Buscar reembolsos pendientes
        List<Reembolso> reembolsosAfectados = reembolsoRepository.findByAprobadoPorCoordinadorAndEstadoReembolso(
                        coordinador, Reembolso.EstadoReembolso.PENDIENTE)
                .stream()
                .filter(reembolso -> reembolso.getReserva() != null &&
                        reembolso.getReserva().getEspacio() != null &&
                        lugar.equals(reembolso.getReserva().getEspacio().getIdLugar()))
                .collect(Collectors.toList());

        impacto.setReservasAfectadas(reservasAfectadas);
        impacto.setMantenimientosAfectados(mantenimientosAfectados);
        impacto.setSolicitudesAfectadas(solicitudesAfectadas);
        impacto.setReembolsosAfectados(reembolsosAfectados);

        // Calcular total de entidades afectadas
        int total = reservasAfectadas.size() + mantenimientosAfectados.size() +
                solicitudesAfectadas.size() + reembolsosAfectados.size();
        impacto.setTotalEntidadesAfectadas(total);

        // Buscar coordinadores disponibles para este lugar
        List<Usuarios> coordinadoresDisponibles = buscarCoordinadoresDisponibles(lugar, coordinador);
        impacto.setCoordinadoresDisponibles(coordinadoresDisponibles);

        // Determinar si requiere intervención manual
        boolean requiereIntervencion = coordinadoresDisponibles.isEmpty() && total > 0;
        impacto.setRequiereIntervencionManual(requiereIntervencion);

        if (requiereIntervencion) {
            impacto.setRazonProblema("No hay coordinadores disponibles para el lugar " + lugar.getLugar() +
                    " y tiene " + total + " entidades que requieren supervisión");
        }

        System.out.println("📊 Lugar " + lugar.getLugar() + ": " + total + " entidades afectadas, " +
                coordinadoresDisponibles.size() + " coordinadores disponibles");

        return impacto;
    }

    /**
     * Busca coordinadores disponibles para un lugar específico (excluyendo al coordinador actual)
     */
    private List<Usuarios> buscarCoordinadoresDisponibles(Lugar lugar, Usuarios coordinadorExcluir) {
        // Buscar todos los coordinadores activos que tienen asignado este lugar
        return usuariosRepository.findByRol_RolAndEstado_EstadoAndLugaresAsignadosContaining(
                        "Coordinador", "Activo", lugar)
                .stream()
                .filter(coordinador -> !coordinador.getIdUsuarios().equals(coordinadorExcluir.getIdUsuarios()))
                .collect(Collectors.toList());
    }

    /**
     * Determina la estrategia de reasignación basada en el análisis
     */
    private void determinarEstrategia(ImpactoAnalisis analisis) {
        List<LugarImpacto> lugaresProblematicos = analisis.getLugaresAfectados().stream()
                .filter(LugarImpacto::isRequiereIntervencionManual)
                .collect(Collectors.toList());

        if (lugaresProblematicos.isEmpty()) {
            // Todos los lugares tienen coordinadores disponibles
            boolean hayEntidadesAfectadas = analisis.getLugaresAfectados().stream()
                    .anyMatch(lugar -> lugar.getTotalEntidadesAfectadas() > 0);

            if (hayEntidadesAfectadas) {
                analisis.setEstrategiaRecomendada(ImpactoAnalisis.EstrategiaReasignacion.AUTOMATICA);
                analisis.setPuedeRealizarseCambio(true);
            } else {
                analisis.setEstrategiaRecomendada(ImpactoAnalisis.EstrategiaReasignacion.AUTOMATICA);
                analisis.setPuedeRealizarseCambio(true);
            }
        } else {
            // Hay lugares sin coordinadores disponibles
            boolean tieneEntidadesCriticas = lugaresProblematicos.stream()
                    .anyMatch(lugar -> lugar.getTotalEntidadesAfectadas() > 0);

            if (tieneEntidadesCriticas) {
                analisis.setEstrategiaRecomendada(ImpactoAnalisis.EstrategiaReasignacion.BLOQUEADA);
                analisis.setPuedeRealizarseCambio(false);
                analisis.setMotivoBloqueo("Existen lugares con entidades activas que quedarían sin coordinador responsable");
            } else {
                analisis.setEstrategiaRecomendada(ImpactoAnalisis.EstrategiaReasignacion.ASISTIDA);
                analisis.setPuedeRealizarseCambio(true);
            }
        }
    }

    /**
     * Genera las acciones requeridas basadas en el análisis
     */
    private void generarAccionesRequeridas(ImpactoAnalisis analisis) {
        List<AccionRequerida> acciones = new ArrayList<>();

        for (LugarImpacto lugarImpacto : analisis.getLugaresAfectados()) {
            if (lugarImpacto.isRequiereIntervencionManual()) {
                AccionRequerida accion = new AccionRequerida();
                accion.setTipo(AccionRequerida.TipoAccion.ASIGNAR_NUEVO_COORDINADOR);
                accion.setDescripcion("Asignar un coordinador al lugar " + lugarImpacto.getLugar().getLugar());
                accion.setEntidadAfectada("Lugar: " + lugarImpacto.getLugar().getLugar());
                accion.setCantidadAfectada(lugarImpacto.getTotalEntidadesAfectadas());
                accion.setEsOpcional(false);
                accion.setSolucionSugerida("Crear un nuevo coordinador o reasignar uno existente a este lugar");
                acciones.add(accion);
            } else if (!lugarImpacto.getCoordinadoresDisponibles().isEmpty()) {
                AccionRequerida accion = new AccionRequerida();
                accion.setTipo(AccionRequerida.TipoAccion.REASIGNAR_AUTOMATICO);
                accion.setDescripcion("Reasignar automáticamente " + lugarImpacto.getTotalEntidadesAfectadas() +
                        " entidades del lugar " + lugarImpacto.getLugar().getLugar());
                accion.setEntidadAfectada("Lugar: " + lugarImpacto.getLugar().getLugar());
                accion.setCantidadAfectada(lugarImpacto.getTotalEntidadesAfectadas());
                accion.setEsOpcional(true);
                accion.setSolucionSugerida("Distribuir entre coordinadores: " +
                        lugarImpacto.getCoordinadoresDisponibles().stream()
                                .map(c -> c.getNombres() + " " + c.getApellidos())
                                .collect(Collectors.joining(", ")));
                acciones.add(accion);
            }
        }

        analisis.setAccionesRequeridas(acciones);
    }

    /**
     * Calcula estadísticas del impacto
     */
    private void calcularEstadisticas(ImpactoAnalisis analisis) {
        Map<String, Object> estadisticas = new HashMap<>();

        int totalReservas = analisis.getLugaresAfectados().stream()
                .mapToInt(lugar -> lugar.getReservasAfectadas().size()).sum();
        int totalMantenimientos = analisis.getLugaresAfectados().stream()
                .mapToInt(lugar -> lugar.getMantenimientosAfectados().size()).sum();
        int totalSolicitudes = analisis.getLugaresAfectados().stream()
                .mapToInt(lugar -> lugar.getSolicitudesAfectadas().size()).sum();
        int totalReembolsos = analisis.getLugaresAfectados().stream()
                .mapToInt(lugar -> lugar.getReembolsosAfectados().size()).sum();

        estadisticas.put("totalReservasAfectadas", totalReservas);
        estadisticas.put("totalMantenimientosAfectados", totalMantenimientos);
        estadisticas.put("totalSolicitudesAfectadas", totalSolicitudes);
        estadisticas.put("totalReembolsosAfectados", totalReembolsos);
        estadisticas.put("totalEntidadesAfectadas", totalReservas + totalMantenimientos + totalSolicitudes + totalReembolsos);
        estadisticas.put("lugaresAfectados", analisis.getLugaresAfectados().size());
        estadisticas.put("lugaresProblematicos", analisis.getLugaresAfectados().stream()
                .mapToInt(lugar -> lugar.isRequiereIntervencionManual() ? 1 : 0).sum());

        analisis.setEstadisticas(estadisticas);
    }

    /**
     * Crea un plan de reasignación detallado
     */
    public PlanReasignacion crearPlanReasignacion(ImpactoAnalisis impacto, Map<Integer, Integer> seleccionesManual) {
        System.out.println("📋 Creando plan de reasignación...");

        PlanReasignacion plan = new PlanReasignacion();
        Map<PlanReasignacion.TipoEntidad, List<ReasignacionDetalle>> reasignacionesAutomaticas = new HashMap<>();
        List<ReasignacionManual> reasignacionesManuales = new ArrayList<>();
        List<ConflictoReasignacion> conflictos = new ArrayList<>();

        for (LugarImpacto lugarImpacto : impacto.getLugaresAfectados()) {
            if (lugarImpacto.isRequiereIntervencionManual()) {
                // Verificar si se proporcionó una selección manual
                Integer coordinadorSeleccionado = seleccionesManual.get(lugarImpacto.getLugar().getIdLugar());
                if (coordinadorSeleccionado != null) {
                    // Crear reasignaciones con el coordinador seleccionado
                    crearReasignacionesConCoordinador(lugarImpacto, coordinadorSeleccionado, reasignacionesAutomaticas);
                } else {
                    // Marcar como manual pendiente
                    ReasignacionManual manual = new ReasignacionManual();
                    manual.setLugar(lugarImpacto.getLugar());
                    manual.setCoordinadoresCandidatos(lugarImpacto.getCoordinadoresDisponibles());
                    manual.setEsObligatoria(true);
                    manual.setDescripcionImpacto("Lugar con " + lugarImpacto.getTotalEntidadesAfectadas() + " entidades sin coordinador");
                    reasignacionesManuales.add(manual);
                }
            } else {
                // Reasignación automática
                crearReasignacionesAutomaticas(lugarImpacto, reasignacionesAutomaticas);
            }
        }

        plan.setReasignacionesAutomaticas(reasignacionesAutomaticas);
        plan.setReasignacionesManuales(reasignacionesManuales);
        plan.setConflictos(conflictos);
        plan.setRequiereConfirmacionAdicional(!reasignacionesManuales.isEmpty());

        // Generar resumen
        generarResumenPlan(plan);

        System.out.println("✅ Plan de reasignación creado");
        return plan;
    }

    /**
     * Crea reasignaciones automáticas para un lugar con coordinadores disponibles
     */
    private void crearReasignacionesAutomaticas(LugarImpacto lugarImpacto,
                                                Map<PlanReasignacion.TipoEntidad, List<ReasignacionDetalle>> reasignaciones) {

        if (lugarImpacto.getCoordinadoresDisponibles().isEmpty()) {
            return;
        }

        // Seleccionar el mejor coordinador basado en carga de trabajo
        Usuarios mejorCoordinador = seleccionarMejorCoordinador(lugarImpacto.getCoordinadoresDisponibles());

        // Crear reasignaciones para reservas
        List<ReasignacionDetalle> reservasReasignadas = lugarImpacto.getReservasAfectadas().stream()
                .map(reserva -> crearReasignacionDetalle(reserva, mejorCoordinador, "RESERVA"))
                .collect(Collectors.toList());

        // Crear reasignaciones para mantenimientos
        List<ReasignacionDetalle> mantenimientosReasignados = lugarImpacto.getMantenimientosAfectados().stream()
                .map(mantenimiento -> crearReasignacionDetalle(mantenimiento, mejorCoordinador, "MANTENIMIENTO"))
                .collect(Collectors.toList());

        // Crear reasignaciones para solicitudes de cancelación
        List<ReasignacionDetalle> solicitudesReasignadas = lugarImpacto.getSolicitudesAfectadas().stream()
                .map(solicitud -> crearReasignacionDetalle(solicitud, mejorCoordinador, "SOLICITUD_CANCELACION"))
                .collect(Collectors.toList());

        // Crear reasignaciones para reembolsos
        List<ReasignacionDetalle> reembolsosReasignados = lugarImpacto.getReembolsosAfectados().stream()
                .map(reembolso -> crearReasignacionDetalle(reembolso, mejorCoordinador, "REEMBOLSO"))
                .collect(Collectors.toList());

        // Agregar al plan
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.RESERVA, k -> new ArrayList<>())
                .addAll(reservasReasignadas);
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.MANTENIMIENTO, k -> new ArrayList<>())
                .addAll(mantenimientosReasignados);
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.SOLICITUD_CANCELACION, k -> new ArrayList<>())
                .addAll(solicitudesReasignadas);
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.REEMBOLSO, k -> new ArrayList<>())
                .addAll(reembolsosReasignados);
    }

    /**
     * Crea reasignaciones con un coordinador específico seleccionado manualmente
     */
    private void crearReasignacionesConCoordinador(LugarImpacto lugarImpacto, Integer coordinadorId,
                                                   Map<PlanReasignacion.TipoEntidad, List<ReasignacionDetalle>> reasignaciones) {

        Optional<Usuarios> coordinadorOpt = usuariosRepository.findById(coordinadorId);
        if (!coordinadorOpt.isPresent()) {
            System.err.println("❌ Coordinador seleccionado no encontrado: " + coordinadorId);
            return;
        }

        Usuarios coordinadorSeleccionado = coordinadorOpt.get();

        // Crear reasignaciones similares a las automáticas pero con coordinador específico
        List<ReasignacionDetalle> reservasReasignadas = lugarImpacto.getReservasAfectadas().stream()
                .map(reserva -> crearReasignacionDetalle(reserva, coordinadorSeleccionado, "RESERVA"))
                .collect(Collectors.toList());

        List<ReasignacionDetalle> mantenimientosReasignados = lugarImpacto.getMantenimientosAfectados().stream()
                .map(mantenimiento -> crearReasignacionDetalle(mantenimiento, coordinadorSeleccionado, "MANTENIMIENTO"))
                .collect(Collectors.toList());

        List<ReasignacionDetalle> solicitudesReasignadas = lugarImpacto.getSolicitudesAfectadas().stream()
                .map(solicitud -> crearReasignacionDetalle(solicitud, coordinadorSeleccionado, "SOLICITUD_CANCELACION"))
                .collect(Collectors.toList());

        List<ReasignacionDetalle> reembolsosReasignados = lugarImpacto.getReembolsosAfectados().stream()
                .map(reembolso -> crearReasignacionDetalle(reembolso, coordinadorSeleccionado, "REEMBOLSO"))
                .collect(Collectors.toList());

        // Agregar al plan
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.RESERVA, k -> new ArrayList<>())
                .addAll(reservasReasignadas);
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.MANTENIMIENTO, k -> new ArrayList<>())
                .addAll(mantenimientosReasignados);
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.SOLICITUD_CANCELACION, k -> new ArrayList<>())
                .addAll(solicitudesReasignadas);
        reasignaciones.computeIfAbsent(PlanReasignacion.TipoEntidad.REEMBOLSO, k -> new ArrayList<>())
                .addAll(reembolsosReasignados);
    }

    /**
     * Crea un detalle de reasignación para cualquier tipo de entidad
     */
    private ReasignacionDetalle crearReasignacionDetalle(Object entidad, Usuarios coordinadorDestino, String tipo) {
        ReasignacionDetalle detalle = new ReasignacionDetalle();
        detalle.setCoordinadorDestino(coordinadorDestino);
        detalle.setEntidadTipo(tipo);
        detalle.setEsAutomatica(true);
        detalle.setRazonReasignacion("Cambio de lugares asignados del coordinador original");

        switch (tipo) {
            case "RESERVA":
                Reserva reserva = (Reserva) entidad;
                detalle.setEntidadId(reserva.getIdReserva());
                detalle.setCoordinadorOrigen(reserva.getCoordinador());
                detalle.setEntidadDescripcion("Reserva #" + reserva.getIdReserva() + " - " +
                        reserva.getEspacio().getNombre() + " (" + reserva.getFecha() + ")");
                break;

            case "MANTENIMIENTO":
                Mantenimiento mantenimiento = (Mantenimiento) entidad;
                detalle.setEntidadId(mantenimiento.getIdMantenimiento());
                detalle.setCoordinadorOrigen(mantenimiento.getResponsable());
                detalle.setEntidadDescripcion("Mantenimiento #" + mantenimiento.getIdMantenimiento() + " - " +
                        mantenimiento.getTipoMantenimiento() + " (" + mantenimiento.getFechaInicio() + ")");
                break;

            case "SOLICITUD_CANCELACION":
                SolicitudCancelacion solicitud = (SolicitudCancelacion) entidad;
                detalle.setEntidadId(solicitud.getId());
                detalle.setCoordinadorOrigen(solicitud.getReserva().getCoordinador());
                detalle.setEntidadDescripcion("Solicitud cancelación #" + solicitud.getId() + " - " +
                        "Reserva #" + solicitud.getReserva().getIdReserva());
                break;

            case "REEMBOLSO":
                Reembolso reembolso = (Reembolso) entidad;
                detalle.setEntidadId(reembolso.getIdReembolso());
                detalle.setCoordinadorOrigen(reembolso.getAprobadoPorCoordinador());
                detalle.setEntidadDescripcion("Reembolso #" + reembolso.getIdReembolso() + " - " +
                        "S/. " + reembolso.getMontoReembolso());
                break;
        }

        return detalle;
    }

    /**
     * Selecciona el mejor coordinador basado en carga de trabajo
     */
    private Usuarios seleccionarMejorCoordinador(List<Usuarios> coordinadoresDisponibles) {
        return coordinadoresDisponibles.stream()
                .min(Comparator.comparing(this::calcularCargaTrabajo))
                .orElse(coordinadoresDisponibles.get(0));
    }

    /**
     * Método público para calcular carga de trabajo de un coordinador
     * (Para uso desde el controlador)
     */
    public int calcularCargaTrabajo(Usuarios coordinador) {
        // Contar reservas activas
        long reservasActivas = reservaRepository.countByCoordinadorAndEstado_EstadoIn(
                coordinador, Arrays.asList("Confirmada", "Pendiente de confirmación"));

        // Contar mantenimientos activos
        long mantenimientosActivos = mantenimientoRepository.countByResponsableAndEstadoIn(
                coordinador, Arrays.asList(Mantenimiento.EstadoMantenimiento.PROGRAMADO,
                        Mantenimiento.EstadoMantenimiento.EN_PROCESO));

        // Contar solicitudes pendientes
        long solicitudesPendientes = solicitudCancelacionRepository.countByReserva_CoordinadorAndEstado(
                coordinador, "Pendiente");

        // Contar reembolsos pendientes
        long reembolsosPendientes = reembolsoRepository.countByAprobadoPorCoordinadorAndEstadoReembolso(
                coordinador, Reembolso.EstadoReembolso.PENDIENTE);

        return (int) (reservasActivas + mantenimientosActivos + solicitudesPendientes + reembolsosPendientes);
    }

    /**
     * Genera un resumen del plan de reasignación
     */
    private void generarResumenPlan(PlanReasignacion plan) {
        StringBuilder resumen = new StringBuilder();

        int totalReasignaciones = plan.getReasignacionesAutomaticas().values().stream()
                .mapToInt(List::size).sum();

        resumen.append("Plan de reasignación creado:\n");
        resumen.append("- Total de reasignaciones automáticas: ").append(totalReasignaciones).append("\n");
        resumen.append("- Reasignaciones manuales pendientes: ").append(plan.getReasignacionesManuales().size()).append("\n");

        for (Map.Entry<PlanReasignacion.TipoEntidad, List<ReasignacionDetalle>> entry :
                plan.getReasignacionesAutomaticas().entrySet()) {
            resumen.append("  * ").append(entry.getKey()).append(": ").append(entry.getValue().size()).append("\n");
        }

        plan.setResumenCambios(resumen.toString());
    }

    /**
     * Ejecuta el plan de reasignación de forma transaccional
     */
    @Transactional
    public ResultadoReasignacion ejecutarPlan(PlanReasignacion plan, Usuarios coordinadorOriginal, List<Lugar> nuevosLugares) {
        System.out.println("🚀 Ejecutando plan de reasignación...");

        ResultadoReasignacion resultado = new ResultadoReasignacion();
        resultado.setFechaEjecucion(LocalDateTime.now());
        resultado.setEntidadesReasignadas(new HashMap<>());
        resultado.setErrores(new ArrayList<>());
        resultado.setAdvertencias(new ArrayList<>());

        try {
            // Ejecutar reasignaciones automáticas
            ejecutarReasignacionesAutomaticas(plan, resultado);

            // Actualizar lugares del coordinador original
            coordinadorOriginal.setLugaresAsignados(nuevosLugares);
            usuariosRepository.save(coordinadorOriginal);

            resultado.setExitoso(true);
            resultado.setMensaje("Reasignación ejecutada exitosamente");

            // Generar resumen completo
            generarResumenResultado(resultado, plan);

            System.out.println("✅ Plan ejecutado exitosamente");

        } catch (Exception e) {
            System.err.println("❌ Error ejecutando plan: " + e.getMessage());
            e.printStackTrace();

            resultado.setExitoso(false);
            resultado.setMensaje("Error durante la ejecución: " + e.getMessage());
            resultado.getErrores().add("Error crítico: " + e.getMessage());

            throw e; // Re-lanzar para rollback de transacción
        }

        return resultado;
    }

    /**
     * Ejecuta las reasignaciones automáticas
     */
    private void ejecutarReasignacionesAutomaticas(PlanReasignacion plan, ResultadoReasignacion resultado) {
        Map<String, Integer> contadores = resultado.getEntidadesReasignadas();

        // Ejecutar reasignaciones de reservas
        List<ReasignacionDetalle> reservas = plan.getReasignacionesAutomaticas()
                .getOrDefault(PlanReasignacion.TipoEntidad.RESERVA, new ArrayList<>());

        for (ReasignacionDetalle detalle : reservas) {
            try {
                Optional<Reserva> reservaOpt = reservaRepository.findById(detalle.getEntidadId());
                if (reservaOpt.isPresent()) {
                    Reserva reserva = reservaOpt.get();
                    reserva.setCoordinador(detalle.getCoordinadorDestino());
                    reservaRepository.save(reserva);
                    contadores.put("reservas", contadores.getOrDefault("reservas", 0) + 1);
                }
            } catch (Exception e) {
                resultado.getErrores().add("Error reasignando reserva " + detalle.getEntidadId() + ": " + e.getMessage());
            }
        }

        // Ejecutar reasignaciones de mantenimientos
        List<ReasignacionDetalle> mantenimientos = plan.getReasignacionesAutomaticas()
                .getOrDefault(PlanReasignacion.TipoEntidad.MANTENIMIENTO, new ArrayList<>());

        for (ReasignacionDetalle detalle : mantenimientos) {
            try {
                Optional<Mantenimiento> mantenimientoOpt = mantenimientoRepository.findById(detalle.getEntidadId());
                if (mantenimientoOpt.isPresent()) {
                    Mantenimiento mantenimiento = mantenimientoOpt.get();
                    mantenimiento.setResponsable(detalle.getCoordinadorDestino());
                    mantenimientoRepository.save(mantenimiento);
                    contadores.put("mantenimientos", contadores.getOrDefault("mantenimientos", 0) + 1);
                }
            } catch (Exception e) {
                resultado.getErrores().add("Error reasignando mantenimiento " + detalle.getEntidadId() + ": " + e.getMessage());
            }
        }

        // Ejecutar reasignaciones de solicitudes de cancelación
        List<ReasignacionDetalle> solicitudes = plan.getReasignacionesAutomaticas()
                .getOrDefault(PlanReasignacion.TipoEntidad.SOLICITUD_CANCELACION, new ArrayList<>());

        for (ReasignacionDetalle detalle : solicitudes) {
            try {
                Optional<SolicitudCancelacion> solicitudOpt = solicitudCancelacionRepository.findById(detalle.getEntidadId());
                if (solicitudOpt.isPresent()) {
                    SolicitudCancelacion solicitud = solicitudOpt.get();
                    // Reasignar la reserva asociada
                    solicitud.getReserva().setCoordinador(detalle.getCoordinadorDestino());
                    reservaRepository.save(solicitud.getReserva());
                    contadores.put("solicitudes", contadores.getOrDefault("solicitudes", 0) + 1);
                }
            } catch (Exception e) {
                resultado.getErrores().add("Error reasignando solicitud " + detalle.getEntidadId() + ": " + e.getMessage());
            }
        }

        // Ejecutar reasignaciones de reembolsos
        List<ReasignacionDetalle> reembolsos = plan.getReasignacionesAutomaticas()
                .getOrDefault(PlanReasignacion.TipoEntidad.REEMBOLSO, new ArrayList<>());

        for (ReasignacionDetalle detalle : reembolsos) {
            try {
                Optional<Reembolso> reembolsoOpt = reembolsoRepository.findById(detalle.getEntidadId());
                if (reembolsoOpt.isPresent()) {
                    Reembolso reembolso = reembolsoOpt.get();
                    reembolso.setAprobadoPorCoordinador(detalle.getCoordinadorDestino());
                    reembolsoRepository.save(reembolso);
                    contadores.put("reembolsos", contadores.getOrDefault("reembolsos", 0) + 1);
                }
            } catch (Exception e) {
                resultado.getErrores().add("Error reasignando reembolso " + detalle.getEntidadId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Genera un resumen completo del resultado
     */
    private void generarResumenResultado(ResultadoReasignacion resultado, PlanReasignacion plan) {
        StringBuilder resumen = new StringBuilder();

        resumen.append("=== RESUMEN DE REASIGNACIÓN ===\n");
        resumen.append("Fecha de ejecución: ").append(resultado.getFechaEjecucion()).append("\n");
        resumen.append("Estado: ").append(resultado.isExitoso() ? "EXITOSO" : "FALLIDO").append("\n\n");

        resumen.append("Entidades reasignadas:\n");
        for (Map.Entry<String, Integer> entry : resultado.getEntidadesReasignadas().entrySet()) {
            resumen.append("- ").append(entry.getKey().toUpperCase()).append(": ").append(entry.getValue()).append("\n");
        }

        if (!resultado.getErrores().isEmpty()) {
            resumen.append("\nErrores encontrados:\n");
            for (String error : resultado.getErrores()) {
                resumen.append("- ").append(error).append("\n");
            }
        }

        if (!resultado.getAdvertencias().isEmpty()) {
            resumen.append("\nAdvertencias:\n");
            for (String advertencia : resultado.getAdvertencias()) {
                resumen.append("- ").append(advertencia).append("\n");
            }
        }

        resultado.setResumenCompleto(resumen.toString());
    }

}