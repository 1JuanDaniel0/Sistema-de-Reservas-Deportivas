package com.example.project.controller;

import com.example.project.entity.*;

import com.example.project.repository.*;
import com.example.project.repository.coordinador.*;
import com.example.project.repository.superadmin.ReservasRepository;
import com.example.project.repository.vecino.EstadoReservaRepositoryVecino;
import com.example.project.repository.vecino.ReservaRepositoryVecino;

import com.example.project.service.MailManager;
import com.example.project.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.sql.Date;
import java.sql.Time;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/coordinador")
public class CoordinadorController {

    @Autowired private ActividadRepository actividadRepository;
    @Autowired private EspacioRepositoryCoord espacioRepositoryCoord;
    @Autowired private EstadoEspacioRepositoryCoord estadoEspacioRepositoryCoord;
    @Autowired private EstadoGeoRepository estadoGeoRepository;
    @Autowired private GeolocalizacionRepository geolocalizacionRepository;
    @Autowired private SolicitudCancelacionRepository solicitudCancelacionRepository;
    @Autowired private MailManager mailManager;
    @Autowired private EstadoReservaRepositoryVecino estadoReservaRepository;
    @Autowired private ReservaRepositoryVecino reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private LugarRepository lugarRepository;
    @Autowired private S3Service s3Service;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private ObservacionEspacioRepository observacionEspacioRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private ReembolsoRepository reembolsoRepository;
    private static final Logger logger = LoggerFactory.getLogger(CoordinadorController.class);

    @Value("${aws.bucket}")
    private String bucketName;
    @Value("${aws.region}")
    private String regionName;

    @GetMapping("/mi-perfil")
    public String mostrarPerfilCoordinador(Model model, HttpSession session) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario"); // Para la vista
        Usuarios usuario = (Usuarios) session.getAttribute("usuario"); // Para la navbar
        model.addAttribute("usuario", usuario);
        model.addAttribute("coordinador", coordinador);
        // 10 actividades más recientes:
        List<Actividad> recientes = actividadRepository
                .findByUsuarioOrderByFechaDesc(usuario)
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("actividades", recientes);

        // Encontrar reservas a su cargo
        // List<Reserva> reservas = reservaRepository.findReservasByCoordinador(coordinador);

        EstadoReserva reservasPasadas = estadoReservaRepository.findByEstado("Finalizada");
        int totalReservasACargo = reservaRepository.countReservasByCoordinadorAndEstado(coordinador, reservasPasadas);
        model.addAttribute("reservasACargo", totalReservasACargo);
        int cantidadLugares = lugarRepository.countLugaresByCoordinador(coordinador);
        model.addAttribute("lugaresAsignados", cantidadLugares);
        int cantidadEspacios = espacioRepositoryCoord.countEspaciosByCoordinador(coordinador);
        model.addAttribute("espaciosAsignados", cantidadEspacios);
        Geolocalizacion ultimaAsistencia = geolocalizacionRepository.findTopByCoordinadorOrderByFechaDesc(coordinador);
        model.addAttribute("ultimaAsistencia", ultimaAsistencia);
        int reembolsosMes = solicitudCancelacionRepository.countReembolsosProcesadosEsteMes(coordinador);
        model.addAttribute("reembolsosMes", reembolsosMes);
        int totalObservaciones = espacioRepositoryCoord.countObservacionesEspacios(coordinador);
        model.addAttribute("totalObservaciones", totalObservaciones);

        return "coordinador/coordinador-perfil-nuevo";
    }

    @PostMapping("/actualizar-perfil-completo")
    @ResponseBody
    public ResponseEntity<?> actualizarPerfilCompleto(@RequestParam String correo,
                                                      @RequestParam String telefono,
                                                      @RequestParam(value = "fotoPerfil", required = false) MultipartFile foto,
                                                      @RequestParam(value = "eliminarFoto", required = false) String eliminarFoto,
                                                      HttpSession session) {
        try {
            Usuarios coordinador = (Usuarios) session.getAttribute("usuario");

            if (coordinador == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Usuario no encontrado en la sesión"));
            }

            coordinador.setCorreo(correo);
            coordinador.setTelefono(telefono);

            String mensajeAccion = "Perfil actualizado correctamente";

            // Verificación de imagen
            if (foto != null && !foto.isEmpty()) {
                String contentType = foto.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "El archivo subido no es una imagen válida"));
                }

                String key = s3Service.subirArchivo(foto, "publica/fotos-perfil/" + coordinador.getIdUsuarios());
                String url = "https://" + bucketName + ".s3." + regionName + ".amazonaws.com/" + key;
                coordinador.setFotoPerfil(url);
                mensajeAccion = "Perfil y foto actualizados correctamente";

            } else if ("true".equals(eliminarFoto)) {
                coordinador.setFotoPerfil(null);
                mensajeAccion = "Perfil actualizado y foto eliminada correctamente";
            }

            usuariosRepository.save(coordinador);
            session.setAttribute("usuario", coordinador);

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", mensajeAccion);

            // Solo incluir nuevaFotoUrl si no es null
            if (coordinador.getFotoPerfil() != null) {
                response.put("nuevaFotoUrl", coordinador.getFotoPerfil());
            } else {
                response.put("nuevaFotoUrl", null); // Explícitamente null para indicar que se eliminó
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Log del error para debugging
            System.err.println("Error al actualizar perfil: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al actualizar el perfil: " +
                            (e.getMessage() != null ? e.getMessage() : "Error interno del servidor")));
        }
    }

    @GetMapping("/espacios")
    public String showEspacios(Model model, HttpSession session) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        List<Espacio> espacios = espacioRepositoryCoord.findAll();
        int cuentaEspaciosDisponibles = espacioRepositoryCoord.countEspaciosByIdEstadoEspacio_IdEstadoEspacio(1);
        int cuentaEspaciosMantenimiento = espacioRepositoryCoord.countEspaciosByIdEstadoEspacio_IdEstadoEspacio(2);
        int reservasHoy = reservaRepository.countReservasByEstado_IdEstadoReserva_AndFecha(1, LocalDate.now(ZoneId.of("America/Lima")));
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDateTime inicioDelDia = hoy.atStartOfDay(); // 00:00
        LocalDateTime finDelDia = hoy.atTime(23, 59, 59); // 23:59

        long totalObservacionesHoy = observacionEspacioRepository
                .countByUsuario_IdUsuariosAndFechaBetween(
                        coordinador.getIdUsuarios(),
                        inicioDelDia,
                        finDelDia
                );

        model.addAttribute("totalObservacionesHoy", totalObservacionesHoy);
        model.addAttribute("reservasHoy", reservasHoy);
        model.addAttribute("cuentaEspaciosDisponibles", cuentaEspaciosDisponibles);
        model.addAttribute("cuentaEspaciosMantenimiento", cuentaEspaciosMantenimiento);
        model.addAttribute("coordinador", coordinador);
        model.addAttribute("espacios", espacios);
        return "coordinador/coordinador-tabla-espacios2";
    }

    @GetMapping("/detalles")
    public String detallesEspacios(@RequestParam("id") int idEspacio, Model model, HttpSession session, HttpServletRequest request) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        Espacio espacio = espacioRepositoryCoord.findById(idEspacio).orElse(null);

        if (espacio != null) {
            // Obtener URL actual completa
            String urlActual = request.getRequestURL().toString();
            model.addAttribute("urlActual", urlActual);
            Double promedio = calificacionRepository.promedioPorEspacio(espacio.getIdEspacio());
            model.addAttribute("espacio", espacio);
            model.addAttribute("promedioCalificacion", promedio != null ? promedio : 0.0);
            List<ObservacionEspacio> observaciones = observacionEspacioRepository
                    .findByEspacio_IdEspacioOrderByFechaDesc(idEspacio);
            model.addAttribute("observaciones", observaciones);
            // Pasar lista completa de coordinadores del lugar
            List<Usuarios> coordinadores = espacio.getIdLugar().getCoordinadores();
            model.addAttribute("coordinadores", coordinadores);

            return "coordinador/coordinador-detalles-espacio";
        } else {
            return "redirect:/coordinador/mi-perfil";
        }
    }

    @GetMapping("/asistencia")
    public String mostrarAsistencia(Model model, HttpSession session) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        if (coordinador == null) {
            return "redirect:/login"; // o la ruta que corresponda
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        YearMonth mesActual = YearMonth.now();
        LocalDate primerDiaMes = mesActual.atDay(1);
        LocalDate ultimoDiaMes = mesActual.atEndOfMonth();

        // Todas las asistencias del mes actual
        List<Geolocalizacion> asistenciasDelMes = geolocalizacionRepository
                .findByCoordinadorAndFechaBetween(coordinador, primerDiaMes, ultimoDiaMes);

        // Total asistencias completadas (estado = "Asistió")
        long totalAsistenciasMes = asistenciasDelMes.stream()
                .filter(a -> a.getEstado().getEstado().equalsIgnoreCase("Asistió"))
                .count();

        // Días con doble asistencia (horaInicio y horaFin no nulos y estado = Asistió)
        long diasDobleAsistencia = asistenciasDelMes.stream()
                .filter(a ->
                        a.getEstado().getEstado().equalsIgnoreCase("Asistió") &&
                                a.getHoraInicio() != null &&
                                a.getHoraFin() != null
                ).map(Geolocalizacion::getFecha)
                .distinct()
                .count();

        // Porcentaje de días asistidos respecto a días hábiles (lunes a viernes)
        long diasHabiles = primerDiaMes.datesUntil(ultimoDiaMes.plusDays(1))
                .filter(d -> d.getDayOfWeek().getValue() <= 5)
                .count();
        long diasConAsistencia = asistenciasDelMes.stream()
                .filter(a -> a.getEstado().getEstado().equalsIgnoreCase("Asistió"))
                .map(Geolocalizacion::getFecha)
                .distinct()
                .count();

        long porcentaje = diasHabiles > 0 ? (diasConAsistencia * 100 / diasHabiles) : 0;

        // Última asistencia (por fecha)
        Geolocalizacion ultimaAsistencia = geolocalizacionRepository
                .findFirstByCoordinadorOrderByFechaDesc(coordinador)
                .orElse(null);

        model.addAttribute("totalAsistenciasMes", totalAsistenciasMes);
        model.addAttribute("diasDobleAsistencia", diasDobleAsistencia);
        model.addAttribute("asistenciasPorcentaje", porcentaje);
        model.addAttribute("ultimaAsistencia", ultimaAsistencia);

        if (session.getAttribute("mensajeError") != null) {
            session.removeAttribute("mensajeError");
        }

        return "coordinador/coordinador-asistencia-2";
    }

    @GetMapping("/asistencia/estado-actual")
    @ResponseBody
    public Map<String, Object> estadoActualAsistencia(HttpSession session) {
        // Añadir más logs
        Logger logger = LoggerFactory.getLogger(CoordinadorController.class);
        logger.info("Consultando estado actual de asistencia");

        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        Map<String, Object> response = new HashMap<>();

        if (coordinador == null) {
            logger.error("No hay coordinador en sesión al consultar estado");
            response.put("error", "No hay coordinador en sesión");
            response.put("enCurso", false);
            return response;
        }

        logger.info("Coordinador en sesión: {}", coordinador.getIdUsuarios());

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        EstadoGeo enCurso = estadoGeoRepository.findByEstado("En Curso").orElse(null);

        if (enCurso == null) {
            logger.error("Estado 'En Curso' no encontrado en la base de datos");
            response.put("error", "Estado 'En Curso' no encontrado");
            response.put("enCurso", false);
            return response;
        }

        logger.info("Consultando asistencia para fecha: {} y estado: {}", hoy, enCurso.getEstado());

        boolean asistenciaEnCurso = geolocalizacionRepository
                .findByCoordinadorAndFechaAndEstado(coordinador, hoy, enCurso)
                .isPresent();

        logger.info("¿Asistencia en curso?: {}", asistenciaEnCurso);

        response.put("enCurso", asistenciaEnCurso);
        return response;
    }

    @GetMapping("/asistencias-datatable")
    @ResponseBody
    public Map<String, Object> obtenerAsistenciasDatatable(
            @RequestParam("draw") int draw,
            @RequestParam("start") int start,
            @RequestParam("length") int length,
            @RequestParam(value = "search[value]", required = false) String search,
            HttpSession session
    ) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");

        int page = start / length;
        Pageable pageable = PageRequest.of(page, length, Sort.by("fecha").descending());

        Page<Geolocalizacion> asistencias;
        if (search != null && !search.isEmpty()) {
            asistencias = geolocalizacionRepository.findByCoordinadorAndLugarExactoContainingIgnoreCase(
                    coordinador, search, pageable);
        } else {
            asistencias = geolocalizacionRepository.findByCoordinador(coordinador, pageable);
        }

        List<Map<String, Object>> data = asistencias.getContent().stream().map(geo -> {
            Map<String, Object> row = new HashMap<>();
            row.put("fecha", geo.getFecha().toString());
            row.put("horaInicio", geo.getHoraInicio() != null ? geo.getHoraInicio().toString() : "-");
            row.put("horaFin", geo.getHoraFin() != null ? geo.getHoraFin().toString() : "-");
            row.put("lugarExacto", geo.getLugarExacto());
            row.put("estado", geo.getEstado().getEstado());
            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        response.put("recordsTotal", asistencias.getTotalElements());
        response.put("recordsFiltered", asistencias.getTotalElements()); // puedes ajustar si haces filtrado real
        response.put("data", data);

        return response;
    }

    @GetMapping("/ver-solicitudes-reembolso")
    public String verSolicitudesReembolso(Model model, HttpSession session) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        if (coordinador == null) {
            return "redirect:/login";
        }

        // Obtener todas las solicitudes del coordinador
        List<SolicitudCancelacion> solicitudes = solicitudCancelacionRepository
                .findByReserva_Coordinador(coordinador);

        // Calcular métricas para el dashboard
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        // 1. Solicitudes pendientes
        long solicitudesPendientes = solicitudes.stream()
                .filter(s -> "Pendiente".equals(s.getEstado()))
                .count();

        // 2. Solicitudes procesadas hoy
        long solicitudesProcesadasHoy = solicitudes.stream()
                .filter(s -> !s.getEstado().equals("Pendiente"))
                .filter(s -> s.getFechaSolicitud().toLocalDate().equals(hoy))
                .count();

        // 3. Monto total en solicitudes pendientes
        double montoTotalPendiente = solicitudes.stream()
                .filter(s -> "Pendiente".equals(s.getEstado()))
                .mapToDouble(s -> s.getReserva().getCosto())
                .sum();

        // 4. Solicitudes urgentes (reserva en menos de 24 horas)
        long solicitudesUrgentes = solicitudes.stream()
                .filter(s -> "Pendiente".equals(s.getEstado()))
                .filter(s -> {
                    LocalDateTime fechaHoraReserva = LocalDateTime.of(
                            s.getReserva().getFecha(),
                            s.getReserva().getHoraInicio()
                    );
                    return Duration.between(ahora, fechaHoraReserva).toHours() < 24 &&
                            Duration.between(ahora, fechaHoraReserva).toHours() > 0;
                })
                .count();

        // === ESTADÍSTICAS ADICIONALES PARA LA SECCIÓN ANTES DE LA TABLA ===
        double tiempoPromedioRespuesta = calcularTiempoPromedioRespuestaStreams(solicitudes);
        double tasaAprobacion = calcularTasaAprobacion(solicitudes);
        double totalReembolsadoMes = calcularTotalReembolsadoMes(solicitudes, inicioMes);

        // Agregar datos al modelo
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("solicitudesPendientes", solicitudesPendientes);
        model.addAttribute("solicitudesProcesadasHoy", solicitudesProcesadasHoy);
        model.addAttribute("montoTotalPendiente", montoTotalPendiente);
        model.addAttribute("solicitudesUrgentes", solicitudesUrgentes);

        // Nuevas estadísticas
        model.addAttribute("tiempoPromedioRespuesta", tiempoPromedioRespuesta);
        model.addAttribute("tasaAprobacion", tasaAprobacion);
        model.addAttribute("totalReembolsadoMes", totalReembolsadoMes);

        return "coordinador/coordinador-solicitudes-reembolso";
    }

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @PostMapping("/solicitud/{id}/aceptar")
    public String aceptarSolicitudConMotivo(@PathVariable int id,
                                            @RequestParam String motivoAceptacion,
                                            RedirectAttributes redirectAttrs,
                                            HttpSession session) {

        System.out.println("🟡 === INICIANDO APROBACIÓN DE SOLICITUD ID: " + id + " ===");

        // Obtener coordinador autenticado
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        if (coordinador == null) {
            redirectAttrs.addFlashAttribute("error", "Sesión expirada");
            return "redirect:/coordinador/login";
        }

        System.out.println("👤 Coordinador: " + coordinador.getNombres() + " " + coordinador.getApellidos());

        SolicitudCancelacion solicitud = solicitudCancelacionRepository.findById(id).orElse(null);
        if (solicitud == null || !solicitud.getEstado().equalsIgnoreCase("Pendiente")) {
            redirectAttrs.addFlashAttribute("msg", "La solicitud no existe o ya fue gestionada.");
            return "redirect:/coordinador/ver-solicitudes-reembolso";
        }

        // Validar que se haya proporcionado un motivo
        if (motivoAceptacion == null || motivoAceptacion.trim().isEmpty()) {
            redirectAttrs.addFlashAttribute("msg", "Debe proporcionar un motivo para la aceptación.");
            return "redirect:/coordinador/ver-solicitudes-reembolso";
        }

        Reserva reserva = solicitud.getReserva();
        System.out.println("📋 Reserva ID: " + reserva.getIdReserva());
        System.out.println("💳 Tipo de pago: " + reserva.getTipoPago());
        System.out.println("💰 Costo: " + reserva.getCosto());

        try {
            // CREAR FECHA/HORA CON ZONA HORARIA DE LIMA
            LocalDateTime fechaHoraLima = LocalDateTime.now(ZoneId.of("America/Lima"));

            // Crear registro de reembolso base
            Reembolso reembolso = new Reembolso();
            reembolso.setSolicitudCancelacion(solicitud);
            reembolso.setReserva(reserva);
            reembolso.setMontoReembolso(new BigDecimal(reserva.getCosto().toString()));
            reembolso.setTipoPagoOriginal(reserva.getTipoPago());
            reembolso.setAprobadoPorCoordinador(coordinador);
            reembolso.setFechaAprobacion(fechaHoraLima); // Usar fecha de Lima
            reembolso.setMotivoAprobacion(motivoAceptacion.trim());

            if (reserva.getTipoPago().equalsIgnoreCase("En línea")) {
                // === REEMBOLSO AUTOMÁTICO MERCADOPAGO ===
                System.out.println("🟡 Procesando reembolso automático para pago en línea");

                // BÚSQUEDA CORREGIDA: Buscar pago válido sin importar si cambió el estado
                Optional<Pago> pagoOpt = Optional.ofNullable(pagoRepository.findByReserva(reserva));

                // Si no existe, buscar por estados específicos como fallback
                if (pagoOpt.isEmpty()) {
                    pagoOpt = pagoRepository.findByReservaAndEstadoIn(
                            reserva,
                            Arrays.asList("Pagado", "REEMBOLSO_SOLICITADO", "Procesado")
                    );
                }

                if (pagoOpt.isEmpty()) {
                    System.err.println("❌ No se encontró pago válido para reembolso automático");
                    redirectAttrs.addFlashAttribute("error", "No se encontró un pago válido para realizar el reembolso automático.");
                    return "redirect:/coordinador/ver-solicitudes-reembolso";
                }

                Pago pago = pagoOpt.get();

                System.out.println("✅ Pago encontrado - ID: " + pago.getIdPago() + ", Estado: " + pago.getEstado());

                if (pago.getReferencia() == null || pago.getReferencia().trim().isEmpty()) {
                    System.err.println("❌ No se encontró referencia del pago para MercadoPago");
                    redirectAttrs.addFlashAttribute("error", "No se encontró la referencia del pago para procesar el reembolso automático.");
                    return "redirect:/coordinador/ver-solicitudes-reembolso";
                }

                reembolso.setEstadoReembolso(Reembolso.EstadoReembolso.PENDIENTE);

                String paymentId = pago.getReferencia();
                System.out.println("🔄 Intentando reembolso para payment ID: " + paymentId);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId + "/refunds"))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();
                String body = response.body();

                System.out.println("🟠 Código HTTP: " + code);
                System.out.println("🟠 Respuesta: " + body);

                if (code == 201) {
                    // ÉXITO: Reembolso automático exitoso
                    String idTransaccion = extraerIdTransaccionDeLaRespuesta(body);
                    reembolso.marcarComoAutomatico(idTransaccion, body);

                    // Actualizar estados
                    solicitud.setEstado("Aprobado");
                    solicitud.setTiempoRespuesta(fechaHoraLima); // Usar fecha de Lima
                    solicitud.setMotivoRespuesta(motivoAceptacion.trim());

                    EstadoReserva estadoReembolsada = estadoReservaRepository.findById(5).orElse(null);
                    if (estadoReembolsada != null) {
                        reserva.setEstado(estadoReembolsada);
                    }
                    reserva.setEstadoReembolso(Reserva.EstadoReembolso.APROBADO);

                    // IMPORTANTE: Solo cambiar estado del pago a "Reembolsado" cuando es exitoso
                    pago.setEstado("Reembolsado");

                    // Guardar todo
                    reembolsoRepository.save(reembolso);
                    solicitudCancelacionRepository.save(solicitud);
                    reservaRepository.save(reserva);
                    pagoRepository.save(pago);

                    System.out.println("✅ Reembolso automático procesado exitosamente");

                    // Enviar correo
                    try {
                        String mensajeCompleto = motivoAceptacion + "\n\nEl reembolso ha sido procesado exitosamente a través de MercadoPago. " +
                                "Los fondos aparecerán en tu cuenta en un plazo de 5-10 días hábiles.";
                        mailManager.enviarRespuestaSolicitudReembolso(solicitud, true, mensajeCompleto);
                    } catch (Exception emailError) {
                        System.err.println("❌ Error al enviar correo: " + emailError.getMessage());
                    }

                    redirectAttrs.addFlashAttribute("msg",
                            "✅ Solicitud aprobada y reembolso procesado automáticamente. " +
                                    "El vecino ha sido notificado por correo electrónico.");

                } else {
                    // ERROR en reembolso automático
                    System.err.println("❌ Error en reembolso automático. Código: " + code);

                    reembolso.marcarComoFallido("Error en MercadoPago: Código " + code + ". " + body);
                    reembolsoRepository.save(reembolso);

                    solicitud.setEstado("Aprobado");
                    solicitud.setTiempoRespuesta(fechaHoraLima); // Usar fecha de Lima
                    solicitud.setMotivoRespuesta(motivoAceptacion.trim() + " - Error en procesamiento automático");
                    reserva.setEstadoReembolso(Reserva.EstadoReembolso.PENDIENTE);

                    // NO cambiar estado del pago si falló el reembolso
                    solicitudCancelacionRepository.save(solicitud);
                    reservaRepository.save(reserva);

                    redirectAttrs.addFlashAttribute("error",
                            "❌ La solicitud fue aprobada, pero falló el reembolso automático. " +
                                    "El administrador procesará el reembolso manualmente. Código: " + code);
                }

            } else if (reserva.getTipoPago().equalsIgnoreCase("En banco")) {
                // === REEMBOLSO MANUAL (NO REQUIERE VALIDACIÓN DE PAGO) ===
                System.out.println("🟡 Procesando aprobación para reembolso manual (pago en banco)");

                reembolso.setEstadoReembolso(Reembolso.EstadoReembolso.PENDIENTE);

                // === ACTUALIZAR ESTADOS PARA REEMBOLSO MANUAL ===
                solicitud.setEstado("Aprobado");
                solicitud.setTiempoRespuesta(fechaHoraLima); // Usar fecha de Lima
                solicitud.setMotivoRespuesta(motivoAceptacion.trim());

                reserva.setEstadoReembolso(Reserva.EstadoReembolso.PENDIENTE);

                // Guardar todo
                reembolsoRepository.save(reembolso);
                solicitudCancelacionRepository.save(solicitud);
                reservaRepository.save(reserva);

                System.out.println("✅ Solicitud aprobada para reembolso manual");

                // Enviar correo informando que está en proceso
                try {
                    String mensajeCompleto = motivoAceptacion + "\n\nTu solicitud de reembolso ha sido aprobada por el coordinador. " +
                            "El reembolso será procesado manualmente por nuestro equipo administrativo en un plazo de 3-5 días hábiles. " +
                            "Te notificaremos cuando el depósito haya sido realizado.";

                    mailManager.enviarRespuestaSolicitudReembolso(solicitud, true, mensajeCompleto);
                    System.out.println("📧 Correo de notificación enviado");
                } catch (Exception emailError) {
                    System.err.println("❌ Error al enviar correo: " + emailError.getMessage());
                }

                redirectAttrs.addFlashAttribute("msg",
                        "✅ Solicitud aprobada correctamente. El reembolso ha sido enviado al administrador para procesamiento manual. " +
                                "El vecino ha sido notificado por correo electrónico.");

            } else {
                // Tipo de pago no reconocido
                System.err.println("❌ Tipo de pago no reconocido: " + reserva.getTipoPago());
                redirectAttrs.addFlashAttribute("error",
                        "❌ Tipo de pago no reconocido: " + reserva.getTipoPago());
                return "redirect:/coordinador/ver-solicitudes-reembolso";
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ Proceso interrumpido: " + e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Proceso interrumpido. Intente nuevamente.");

        } catch (IOException e) {
            System.err.println("❌ Error de conexión con MercadoPago: " + e.getMessage());
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error",
                    "❌ Error de conexión. La solicitud fue aprobada pero requiere procesamiento manual del administrador.");

        } catch (Exception e) {
            System.err.println("❌ Error general en aceptarSolicitudConMotivo: " + e.getMessage());
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "❌ Error interno: " + e.getMessage());
        }

        System.out.println("🟡 === FIN PROCESAMIENTO SOLICITUD ID: " + id + " ===");
        return "redirect:/coordinador/ver-solicitudes-reembolso";
    }

    /**
     * Método auxiliar para extraer ID de transacción de la respuesta de MercadoPago
     */
    private String extraerIdTransaccionDeLaRespuesta(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonResponse);
            String idTransaccion = node.path("id").asText();

            if (idTransaccion != null && !idTransaccion.isEmpty()) {
                System.out.println("✅ ID de transacción extraído: " + idTransaccion);
                return idTransaccion;
            } else {
                System.err.println("⚠️ No se pudo extraer ID de transacción del JSON");
                return "REEMBOLSO_" + UUID.randomUUID().toString().substring(0, 8);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al extraer ID de transacción: " + e.getMessage());
            return "ERROR_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    @PostMapping("/solicitud/{id}/rechazar")
    public String rechazarSolicitudConMotivo(@PathVariable int id,
                                             @RequestParam String motivoRechazo,
                                             RedirectAttributes redirectAttrs) {
        SolicitudCancelacion solicitud = solicitudCancelacionRepository.findById(id).orElse(null);
        if (solicitud == null || !solicitud.getEstado().equalsIgnoreCase("Pendiente")) {
            redirectAttrs.addFlashAttribute("msg", "La solicitud no existe o ya fue gestionada.");
            return "redirect:/coordinador/ver-solicitudes-reembolso";
        }

        // Validar que se haya proporcionado un motivo
        if (motivoRechazo == null || motivoRechazo.trim().isEmpty()) {
            redirectAttrs.addFlashAttribute("msg", "Debe proporcionar un motivo para el rechazo.");
            return "redirect:/coordinador/ver-solicitudes-reembolso";
        }

        Reserva reserva = solicitud.getReserva();

        try {
            // Marcar solicitud como rechazada CON motivo y tiempo de respuesta
            solicitud.setEstado("Rechazada");
            solicitud.marcarProcesada(motivoRechazo.trim());

            solicitudCancelacionRepository.save(solicitud);
            reserva.setEstado(estadoReservaRepository.findById(7).orElse(null));

            // Enviar correo de rechazo
            try {
                mailManager.enviarRespuestaSolicitudReembolso(
                        solicitud,
                        false, // aprobada = false
                        motivoRechazo.trim()
                );
            } catch (Exception emailError) {
                System.err.println("❌ Error al enviar correo de rechazo: " + emailError.getMessage());
            }

            redirectAttrs.addFlashAttribute("msg",
                    "✅ Solicitud rechazada correctamente. " +
                            "El vecino ha sido notificado del rechazo por correo electrónico.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error",
                    "❌ Error al procesar el rechazo: " + e.getMessage());
        }

        return "redirect:/coordinador/ver-solicitudes-reembolso";
    }

    @GetMapping("/verificar-ubicacion")
    @ResponseBody
    public Map<String, Object> verificarUbicacion(@RequestParam("latlon") String latlon, HttpSession session) {
        Logger logger = LoggerFactory.getLogger(CoordinadorController.class);
        logger.info("Verificando ubicación con latlon={}", latlon);

        Map<String, Object> response = new HashMap<>();
        response.put("ubicacionValida", false); // Valor predeterminado

        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        if (coordinador == null) {
            logger.error("No hay coordinador en sesión para verificar ubicación");
            return response;
        }

        try {
            String[] partes = latlon.split(",");
            if (partes.length != 2) {
                logger.error("Formato de coordenadas incorrecto: {}", latlon);
                return response;
            }

            double lat = Double.parseDouble(partes[0].trim());
            double lon = Double.parseDouble(partes[1].trim());

            logger.info("Verificando ubicación para coordenadas: lat={}, lon={}", lat, lon);

            // Buscar lugar más cercano dentro de 5 km
            Lugar lugarValido = buscarLugarValido(coordinador, lat, lon, 5.0);

            if (lugarValido != null) {
                logger.info("Se encontró lugar válido: {} (ID: {})", lugarValido.getLugar(), lugarValido.getIdLugar());
                response.put("ubicacionValida", true);
                response.put("lugarNombre", lugarValido.getLugar());
                response.put("lugarId", lugarValido.getIdLugar());
            } else {
                logger.warn("No se encontró un lugar válido dentro de 5 km");
            }

        } catch (Exception e) {
            logger.error("Error verificando ubicación:", e);
        }

        return response;
    }

    @GetMapping("/verificar-reservas")
    public String verificarReservas(Model model, HttpSession session) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");

        // Verificar que hay una sesión activa
        if (coordinador == null) {
            model.addAttribute("error", "Sesión no válida. Por favor, inicie sesión nuevamente.");
            return "redirect:/login";
        }

        // Obtener reservas pendientes que tienen comprobante subido
        List<Reserva> reservasPendientes = reservaRepository
                .findReservasPendientesConComprobante(coordinador);

        // Calcular estadísticas usando los métodos optimizados
        long totalPendientes = reservaRepository
                .countByCoordinadorAndEstado_Estado(coordinador, "Pendiente de confirmación");

        long reservasUrgentes = reservaRepository
                .findReservasUrgentesParaHoy(coordinador, LocalDate.now(ZoneId.of("America/Lima"))).size();

        long reservasConfirmadasHoy = reservaRepository
                .countByCoordinadorAndEstado_EstadoAndFechaPagoBetween(
                        coordinador,
                        "Confirmada",
                        LocalDate.now(ZoneId.of("America/Lima")).atStartOfDay(),
                        LocalDate.now(ZoneId.of("America/Lima")).atTime(LocalTime.MAX));

        Double montoTotalPendiente = reservaRepository
                .sumMontoPendientePorCoordinador(coordinador);
        // Agregar al modelo
        model.addAttribute("reservas", reservasPendientes);
        model.addAttribute("reservasPendientes", totalPendientes);
        model.addAttribute("reservasUrgentes", reservasUrgentes);
        model.addAttribute("reservasConfirmadasHoy", reservasConfirmadasHoy);
        model.addAttribute("montoTotalPendiente", montoTotalPendiente != null ? montoTotalPendiente : 0.0);
        return "coordinador/coordinador-verificar-reservas";
    }

    // Método para obtener URL prefirmada del comprobante
    @GetMapping("/comprobante/{idReserva}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerComprobanteReserva(@PathVariable Integer idReserva,
                                                                         HttpSession session) {
        try {
            // Obtener el coordinador directamente desde la sesión
            Usuarios coordinador = (Usuarios) session.getAttribute("usuario");

            // Verificar que hay una sesión activa
            if (coordinador == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Sesión no válida"));
            }

            // Verificar que la reserva existe
            Optional<Reserva> reservaOpt = reservaRepository.findById(idReserva);
            if (reservaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Reserva reserva = reservaOpt.get();

            // Verificar permisos del coordinador - comparación directa de IDs
            if (!reserva.getCoordinador().getIdUsuarios().equals(coordinador.getIdUsuarios())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permisos para ver este comprobante"));
            }

            // Verificar que la reserva tiene comprobante
            if (reserva.getCapturaKey() == null || reserva.getCapturaKey().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No hay comprobante disponible para esta reserva"));
            }

            // 🔧 MEJORA: Validar formato del key antes de generar URL
            String capturaKey = reserva.getCapturaKey().trim();

            // Verificar que el key no sea una URL completa (compatibilidad con datos antiguos)
            if (capturaKey.startsWith("http")) {
                // Si ya es una URL completa, extraer solo el key
                try {
                    // Extraer el key de una URL de S3:
                    // https://bucket.s3.region.amazonaws.com/carpeta/archivo.jpg -> carpeta/archivo.jpg
                    String[] urlParts = capturaKey.split("/");
                    if (urlParts.length >= 4) {
                        capturaKey = String.join("/", Arrays.copyOfRange(urlParts, 3, urlParts.length));
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error procesando URL existente: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Error al procesar el comprobante almacenado"));
                }
            }

            // 🔧 MEJORA: Logging para debugging
            System.out.println("🔍 Generando URL prefirmada para key: " + capturaKey);

            // Generar URL prefirmada válida por 15 minutos
            String urlPrefirmada = s3Service.generarUrlPreFirmada(capturaKey, 15);

            // 🔧 MEJORA: Validar que se generó correctamente
            if (urlPrefirmada == null || urlPrefirmada.trim().isEmpty()) {
                System.err.println("❌ Error: URL prefirmada vacía para key: " + capturaKey);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al generar enlace del comprobante"));
            }

            System.out.println("✅ URL prefirmada generada correctamente");

            Map<String, Object> response = new HashMap<>();
            response.put("url", urlPrefirmada);
            response.put("reservaId", idReserva);
            response.put("success", true);
            response.put("expiresIn", "15 minutos"); // Información útil para el frontend

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error en obtenerComprobanteReserva: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al obtener el comprobante"));
        }
    }

    @PostMapping("/reserva/{id}/confirmar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmarReserva(@PathVariable Integer id,
                                                                HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Obtener el coordinador directamente desde la sesión
            Usuarios coordinador = (Usuarios) session.getAttribute("usuario");

            // Verificar que hay una sesión activa
            if (coordinador == null) {
                response.put("success", false);
                response.put("message", "Sesión no válida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Verificar que la reserva existe y está pendiente
            Optional<Reserva> reservaOpt = reservaRepository.findById(id);
            if (reservaOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Reserva no encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            Reserva reserva = reservaOpt.get();

            // Verificar que el coordinador actual es el asignado
            if (!reserva.getCoordinador().getIdUsuarios().equals(coordinador.getIdUsuarios())) {
                response.put("success", false);
                response.put("message", "No tienes permisos para confirmar esta reserva");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Verificar que la reserva está en estado pendiente de confirmación
            if (!"Pendiente de confirmación".equals(reserva.getEstado().getEstado())) {
                response.put("success", false);
                response.put("message", "La reserva ya ha sido procesada");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar que tiene comprobante subido
            if (reserva.getCapturaKey() == null || reserva.getCapturaKey().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "La reserva no tiene comprobante de pago subido");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar que sea pago en banco
            if (!"En banco".equals(reserva.getTipoPago())) {
                response.put("success", false);
                response.put("message", "Solo se pueden confirmar reservas con pago en banco");
                return ResponseEntity.badRequest().body(response);
            }

            // Cambiar estado a "Confirmada"
            EstadoReserva estadoConfirmada = estadoReservaRepository.findByEstado("Confirmada");
            if (estadoConfirmada == null) {
                // Si no existe, buscar por ID como fallback
                estadoConfirmada = estadoReservaRepository.findById(1).orElse(null);
            }

            if (estadoConfirmada == null) {
                response.put("success", false);
                response.put("message", "Error al obtener estado confirmada");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // Verificar si ya existe un pago asociado
            Pago pago = pagoRepository.findByReserva(reserva);

            if (pago == null) {
                // Crear nuevo pago si no existe (caso normal para pagos en banco desde chatbot)
                pago = new Pago();
                pago.setReserva(reserva);
                pago.setMonto(BigDecimal.valueOf(reserva.getCosto()));
                pago.setTipoPago("En banco");
                pago.setFechaPago(LocalDateTime.now(ZoneId.of("America/Lima")));
                pago.setEstado("Pagado");
                pago.setReferencia("Confirmado por coordinador: " + coordinador.getNombres());
            } else {
                // Actualizar pago existente
                pago.setEstado("Pagado");
                pago.setFechaPago(LocalDateTime.now(ZoneId.of("America/Lima")));
                if (pago.getReferencia() == null) {
                    pago.setReferencia("Confirmado por coordinador: " + coordinador.getNombres());
                }
            }

            // Actualizar reserva
            reserva.setEstado(estadoConfirmada);
            reserva.setEstadoPago("Pagado");
            reserva.setFechaPago(LocalDateTime.now(ZoneId.of("America/Lima")));

            // Guardar cambios en orden correcto
            reservaRepository.save(reserva);
            pagoRepository.save(pago);

            System.out.println("[COORDINADOR] ✅ Reserva " + id + " confirmada exitosamente");
            System.out.println("[COORDINADOR] - Pago ID: " + pago.getIdPago());
            System.out.println("[COORDINADOR] - Estado final: " + reserva.getEstado().getEstado());

            // Enviar correo de confirmación al vecino
            try {
                mailManager.enviarConfirmacionReserva(reserva);
                System.out.println("[COORDINADOR] ✅ Correo de confirmación enviado");
            } catch (Exception emailError) {
                System.err.println("❌ Error al enviar correo de confirmación: " + emailError.getMessage());
                // No interrumpir el flujo principal por un error de correo
            }

            response.put("success", true);
            response.put("message", "Reserva confirmada exitosamente");
            response.put("idPago", pago.getIdPago());
            response.put("estadoFinal", reserva.getEstado().getEstado());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[COORDINADOR] ❌ Error al confirmar reserva " + id + ": " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reserva/{id}/rechazar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rechazarReserva(@PathVariable Integer id,
                                                               @RequestBody Map<String, String> requestBody,
                                                               HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            String motivo = requestBody.get("motivo");

            if (motivo == null || motivo.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Debe proporcionar un motivo para el rechazo");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtener el coordinador directamente desde la sesión
            Usuarios coordinador = (Usuarios) session.getAttribute("usuario");

            // Verificar que hay una sesión activa
            if (coordinador == null) {
                response.put("success", false);
                response.put("message", "Sesión no válida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Verificar que la reserva existe
            Optional<Reserva> reservaOpt = reservaRepository.findById(id);
            if (reservaOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Reserva no encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            Reserva reserva = reservaOpt.get();

            // Verificar permisos del coordinador - comparación directa de IDs
            if (!reserva.getCoordinador().getIdUsuarios().equals(coordinador.getIdUsuarios())) {
                response.put("success", false);
                response.put("message", "No tienes permisos para procesar esta reserva");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Cambiar estado a "Cancelada"
            EstadoReserva estadoRechazada = estadoReservaRepository.findByEstado("Cancelada");
            if (estadoRechazada == null) {
                estadoRechazada = new EstadoReserva();
                estadoRechazada.setEstado("Cancelada");
                estadoRechazada = estadoReservaRepository.save(estadoRechazada);
            }

            reserva.setEstado(estadoRechazada);
            reservaRepository.save(reserva);

            // Enviar correo de rechazo
            try {
                mailManager.enviarRechazoReserva(reserva, motivo);
            } catch (Exception emailError) {
                System.err.println("❌ Error al enviar correo de rechazo: " + emailError.getMessage());
            }

            response.put("success", true);
            response.put("message", "Reserva rechazada y vecino notificado");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/ver-recordatorios")
    public String verRecordatoriosCoordinador(Model model) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDate manana = hoy.plusDays(1);

        List<Reserva> recordatoriosHoy = reservaRepository.findByEstado_EstadoAndFecha("Confirmada", hoy);
        List<Reserva> recordatoriosManana = reservaRepository.findByEstado_EstadoAndFecha("Confirmada", manana);

        model.addAttribute("hoy", hoy);
        model.addAttribute("manana", manana);
        model.addAttribute("recordatoriosHoy", recordatoriosHoy);
        model.addAttribute("recordatoriosManana", recordatoriosManana);

        return "coordinador/coordinador-ver-recordatorios";
    }
    
    @PostMapping("/addObservacion")
    @ResponseBody
    public ResponseEntity<?> addObservacion(@RequestParam("idEspacio") int idEspacio,
                                            @RequestParam("observacion") String observacion,
                                            HttpSession session) {

        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        Optional<Espacio> optionalEspacio = espacioRepositoryCoord.findById(idEspacio);

        if (optionalEspacio.isPresent()) {
            Espacio espacio = optionalEspacio.get();

            ObservacionEspacio nuevaObservacion = new ObservacionEspacio();
            nuevaObservacion.setEspacio(espacio);
            nuevaObservacion.setUsuario(coordinador);
            nuevaObservacion.setContenido(observacion.trim());
            nuevaObservacion.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
            observacionEspacioRepository.save(nuevaObservacion);

            Actividad actividad = new Actividad();
            actividad.setUsuario(coordinador);
            actividad.setDescripcion("Agregó una observación");
            actividad.setDetalle("Se añadió una observación al espacio \"" + espacio.getNombre() + "\".");
            actividad.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
            actividadRepository.save(actividad);

            return ResponseEntity.ok().body(Map.of("success", true));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Espacio no encontrado"));
        }
    }

    @PostMapping("/marcarAsistencia")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarAsistencia(@RequestParam("latlon") String latlon,
                                                                @RequestParam(value = "observacion", required = false) String observacion,
                                                                HttpSession session) {
        System.out.println("=== MARCAR ASISTENCIA LLAMADO ===");
        System.out.println("Parámetros recibidos:");
        System.out.println("latlon: " + latlon);
        System.out.println("observacion: " + observacion);
        System.out.println("session: " + (session != null ? "OK" : "NULL"));

        Logger logger = LoggerFactory.getLogger(CoordinadorController.class);
        logger.info("Entrando a marcarAsistencia con latlon={}", latlon);

        Map<String, Object> response = new HashMap<>();

        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        if (coordinador == null) {
            System.out.println("ERROR: No hay coordinador en sesión");
            logger.error("No hay coordinador en sesión.");
            response.put("success", false);
            response.put("message", "No se encontró información de usuario en la sesión.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        System.out.println("Coordinador encontrado: " + coordinador.getNombres());

        ZoneId limaZone = ZoneId.of("America/Lima");
        LocalDate hoy = LocalDate.now(limaZone);
        EstadoGeo enCurso = estadoGeoRepository.findByEstado("En Curso").orElse(null);
        EstadoGeo asistio = estadoGeoRepository.findByEstado("Asistió").orElse(null);
        if (enCurso == null || asistio == null) {
            logger.error("Estados de asistencia no encontrados. enCurso={}, asistio={}",
                    (enCurso != null), (asistio != null));
            response.put("success", false);
            response.put("message", "Error en la configuración del sistema. Contacte al administrador.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            String[] partes = latlon.split(",");
            if (partes.length != 2) {
                logger.error("Formato de coordenadas incorrecto: {}", latlon);
                response.put("success", false);
                response.put("message", "Formato de coordenadas incorrecto.");
                return ResponseEntity.badRequest().body(response);
            }

            double lat = Double.parseDouble(partes[0].trim());
            double lon = Double.parseDouble(partes[1].trim());

            logger.info("Coordenadas parseadas: lat={}, lon={}", lat, lon);

            // Buscar lugar más cercano dentro de 5 km
            Lugar lugarValido = buscarLugarValido(coordinador, lat, lon, 5.0);

            if (lugarValido == null) {
                logger.warn("No se encontró un lugar válido dentro de 5 km.");
                response.put("success", false);
                response.put("message", "No estás cerca de ninguno de tus lugares asignados.");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Lugar válido encontrado: {} (ID: {})", lugarValido.getLugar(), lugarValido.getIdLugar());

            Optional<Geolocalizacion> geoOpt = geolocalizacionRepository
                    .findByCoordinadorAndFechaAndEstado(coordinador, hoy, enCurso);

            if (geoOpt.isPresent()) {
                // Marcar salida
                Geolocalizacion geo = geoOpt.get();
                geo.setHoraFin(LocalTime.now());
                geo.setEstado(asistio);
                geo.setObservacion(observacion); // por si desea añadir al cerrar
                geolocalizacionRepository.save(geo);

                logger.info("Asistencia cerrada ID: {} - Hora fin: {}", geo.getIdGeolocalizacion(), geo.getHoraFin());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String horaFormateada = geo.getHoraInicio().format(formatter);

                registrarActividad(coordinador, "Registró una Asistencia",
                        "Se registró la hora de SALIDA a las " + horaFormateada
                                + " en \"" + lugarValido.getLugar() + "\". Asistencia completada.");

                logger.info("Asistencia cerrada para usuario {}", coordinador.getIdUsuarios());

                response.put("success", true);
                response.put("message", "Hora de salida registrada exitosamente.");
                response.put("action", "salida");

            } else {
                // Marcar entrada
                Geolocalizacion geo = new Geolocalizacion();
                geo.setCoordinador(coordinador);
                geo.setFecha(hoy);
                geo.setHoraInicio(LocalTime.now());
                geo.setLugarExacto(latlon);
                geo.setEstado(enCurso);
                geo.setObservacion(observacion);
                geo.setLugar(lugarValido);
                Geolocalizacion savedGeo = geolocalizacionRepository.save(geo);

                logger.info("Nueva asistencia creada - ID: {} - Hora inicio: {}",
                        savedGeo.getIdGeolocalizacion(), savedGeo.getHoraInicio());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String horaFormateada = geo.getHoraInicio().format(formatter);

                registrarActividad(coordinador, "Inició una Asistencia",
                        "Se registró la hora de ENTRADA a las " + horaFormateada
                                + " en el lugar \"" + lugarValido.getLugar() + "\". Asistencia en curso.");

                logger.info("Nueva asistencia iniciada para usuario {}", coordinador.getIdUsuarios());

                response.put("success", true);
                response.put("message", "Hora de entrada registrada exitosamente.");
                response.put("action", "entrada");
            }

            // Solo elimina el error en caso de éxito
            session.removeAttribute("mensajeError");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error procesando asistencia:", e);
            response.put("success", false);
            response.put("message", "Ocurrió un error al registrar la asistencia: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/debug/lugares")
    @ResponseBody
    public Map<String, Object> debugLugares(HttpSession session) {
        Usuarios coordinador = (Usuarios) session.getAttribute("usuario");
        Map<String, Object> response = new HashMap<>();

        if (coordinador == null) {
            response.put("error", "No hay coordinador en sesión");
            return response;
        }

        List<Lugar> lugares = lugarRepository.findByCoordinadores_IdUsuarios(coordinador.getIdUsuarios());
        List<Map<String, Object>> lugaresInfo = new ArrayList<>();

        for (Lugar lugar : lugares) {
            Map<String, Object> info = new HashMap<>();
            info.put("nombre", lugar.getLugar());
            info.put("ubicacion", lugar.getUbicacion());

            try {
                String[] partes = lugar.getUbicacion().split(",");
                info.put("lat", Double.parseDouble(partes[0].trim()));
                info.put("lon", Double.parseDouble(partes[1].trim()));
                info.put("coordenadas_validas", true);
            } catch (Exception e) {
                info.put("coordenadas_validas", false);
                info.put("error", e.getMessage());
            }

            lugaresInfo.add(info);
        }

        response.put("lugares", lugaresInfo);
        response.put("total", lugares.size());

        return response;
    }

    @PostMapping("/impersonation-logout")
    public String impersonationLogout(HttpSession session) {
        Integer originalId = (Integer) session.getAttribute("originalSuperAdminId");
        Usuarios originalObj = (Usuarios) session.getAttribute("originalSuperAdminObj");
        if (originalId != null && originalObj != null) {
            UserDetails superAdminDetails = userDetailsService.loadUserByUsername(String.valueOf(originalObj.getDni()));
            Authentication superAdminAuth = new UsernamePasswordAuthenticationToken(
                    superAdminDetails,
                    superAdminDetails.getPassword(),
                    superAdminDetails.getAuthorities()
            );
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(superAdminAuth);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            session.setAttribute("usuario", originalObj);
            session.removeAttribute("originalSuperAdminId");
            session.removeAttribute("originalSuperAdminObj");
            return "redirect:/superadmin/home";
        }
        return "redirect:/logout";
    }

    private void registrarActividad(Usuarios usuario, String titulo, String detalle) {
        Actividad actividad = new Actividad();
        actividad.setUsuario(usuario);
        actividad.setDescripcion(titulo);
        actividad.setDetalle(detalle);
        actividad.setFecha(LocalDateTime.now());
        actividadRepository.save(actividad);
    }

    public double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public Lugar buscarLugarValido(Usuarios coordinador, double lat, double lon, double maxDistanciaKm) {
        Logger logger = LoggerFactory.getLogger(CoordinadorController.class);
        List<Lugar> lugares = lugarRepository.findByCoordinadores_IdUsuarios(coordinador.getIdUsuarios());

        logger.info("Buscando lugar válido para coordenadas: {}, {}. Lugares disponibles: {}",
                lat, lon, lugares.size());

        // Imprimir todos los lugares y sus coordenadas para depuración
        for (Lugar l : lugares) {
            logger.info("Lugar {} (ID: {}): nombre='{}', ubicación='{}'",
                    l.getIdLugar(), l.getLugar(), l.getUbicacion());
        }

        Lugar lugarValido = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (Lugar lugar : lugares) {
            try {
                String ubicacionCruda = lugar.getUbicacion();
                logger.info("Procesando ubicación: '{}'", ubicacionCruda);

                // Limpiar posibles caracteres especiales
                String ubicacionLimpia = ubicacionCruda.replaceAll("[()\\s]", "");
                String[] partes = ubicacionLimpia.split(",");

                if (partes.length != 2) {
                    logger.error("Formato inválido de ubicación para lugar {}: '{}'",
                            lugar.getLugar(), ubicacionCruda);
                    continue;
                }

                double lugarLat = Double.parseDouble(partes[0].trim());
                double lugarLon = Double.parseDouble(partes[1].trim());

                logger.info("Lugar {}: Lat={}, Lon={}", lugar.getLugar(), lugarLat, lugarLon);

                double distancia = calcularDistanciaKm(lat, lon, lugarLat, lugarLon);

                logger.info("Lugar: {} - Distancia: {} km (Max: {} km)",
                        lugar.getLugar(), String.format("%.2f", distancia), maxDistanciaKm);

                if (distancia <= maxDistanciaKm && distancia < distanciaMinima) {
                    lugarValido = lugar;
                    distanciaMinima = distancia;
                    logger.info("Nuevo lugar válido encontrado: {} a {} km",
                            lugar.getLugar(), String.format("%.2f", distancia));
                }
            } catch (Exception e) {
                logger.error("Error parseando coordenadas de lugar ID {}: {}",
                        lugar.getIdLugar(), e.getMessage(), e);
            }
        }

        if (lugarValido != null) {
            logger.info("Lugar válido seleccionado: {} a {} km",
                    lugarValido.getLugar(), String.format("%.2f", distanciaMinima));
        } else {
            logger.warn("No se encontró ningún lugar válido dentro de {} km", maxDistanciaKm);
        }

        return lugarValido;
    }

    // Métodos auxiliares para calcular estadísticas
    private double calcularTiempoPromedioRespuestaStreams(List<SolicitudCancelacion> solicitudes) {
        OptionalDouble promedio = solicitudes.stream()
                .filter(s -> s.getTiempoRespuesta() != null && s.getFechaSolicitud() != null)
                .filter(s -> !"Pendiente".equals(s.getEstado()))
                .mapToDouble(s -> {
                    Duration duracion = Duration.between(s.getFechaSolicitud(), s.getTiempoRespuesta());
                    return duracion.toMinutes() / 60.0; // Convertir a horas
                })
                .average();

        if (promedio.isPresent()) {
            double resultado = promedio.getAsDouble();
            System.out.println("🔍 Promedio calculado (streams): " + String.format("%.2f", resultado));
            return Math.round(resultado * 10.0) / 10.0;
        }

        return 0.0;
    }

    private double calcularTasaAprobacion(List<SolicitudCancelacion> solicitudes) {
        List<SolicitudCancelacion> procesadas = solicitudes.stream()
                .filter(s -> s.getEstado().equals("Aprobado") || s.getEstado().equals("Rechazado"))
                .collect(Collectors.toList());

        if (procesadas.isEmpty()) {
            return 0.0;
        }

        long aprobadas = procesadas.stream()
                .filter(s -> s.getEstado().equals("Aprobado"))
                .count();

        double tasa = (aprobadas * 100.0) / procesadas.size();
        return Math.round(tasa);
    }

    private double calcularTotalReembolsadoMes(List<SolicitudCancelacion> solicitudes, LocalDate inicioMes) {
        return solicitudes.stream()
                .filter(s -> s.getEstado().equals("Aprobado"))
                .filter(s -> s.getFechaSolicitud().toLocalDate().isAfter(inicioMes.minusDays(1)))
                .mapToDouble(s -> s.getReserva().getCosto())
                .sum();
    }

}