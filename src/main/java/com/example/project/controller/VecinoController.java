package com.example.project.controller;
import com.example.project.dto.EspacioFiltroDTO;
import com.example.project.dto.ReservaCalendarioDto;
import com.example.project.dto.ReservaExtendidaDTO;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.admin.EspacioRepositoryAdmin;
import com.example.project.repository.admin.ReservaRepositoryAdmin;
import com.example.project.repository.vecino.PagoRepositoryVecino;
import com.example.project.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import com.example.project.repository.vecino.EspacioRepositoryVecino;
import com.example.project.repository.vecino.EstadoReservaRepositoryVecino;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Optional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/vecino")
public class VecinoController {
    final UsuariosRepository usuariosRepository;
    final RolRepository rolRepository;
    final EstadoUsuRepository estadoRepository;// Declaración correcta
    final EspacioRepositoryAdmin espacioRepositoryAdmin;
    final ReservaRepositoryAdmin reservaRepositoryAdmin;
    final ReservaRepositoryVecino reservaRepositoryVecino;
    final EspacioRepositoryVecino espacioRepositoryVecino;
    private final EstadoReservaRepositoryVecino estadoReservaRepositoryVecino;
    private final PasswordEncoder passwordEncoder;

    // Constructor con inyección de dependencia
    public VecinoController(UsuariosRepository usuariosRepository,
                            RolRepository rolRepository,
                            EstadoUsuRepository estadoRepository,
                            EspacioRepositoryAdmin espacioRepositoryAdmin,
                            EspacioRepositoryVecino espacioRepositoryVecino,
                            ReservaRepositoryAdmin reservaRepositoryAdmin,
                            ReservaRepositoryVecino reservaRepositoryVecino,
                            EstadoReservaRepositoryVecino estadoReservaRepositoryVecino,
                            PasswordEncoder passwordEncoder) {
        this.usuariosRepository = usuariosRepository;
        this.rolRepository      = rolRepository;
        this.estadoRepository   = estadoRepository;
        this.espacioRepositoryAdmin = espacioRepositoryAdmin;
        this.espacioRepositoryVecino = espacioRepositoryVecino;
        this.reservaRepositoryAdmin = reservaRepositoryAdmin;
        this.reservaRepositoryVecino = reservaRepositoryVecino;
        this.estadoReservaRepositoryVecino = estadoReservaRepositoryVecino;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired private TipoEspacioRepository tipoEspacioRepository;
    @Autowired private DeporteRepository deporteRepository;
    @Autowired private PagoRepositoryVecino pagoRepositoryVecino;
    @Autowired private PagoExportService pagoExportService;
    @Autowired private EspacioService espacioService;
    @Autowired private LugarRepository lugarRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private MailManager mailManager;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private SolicitudCancelacionRepository solicitudCancelacionRepository;
    @Autowired private MantenimientoRepository mantenimientoRepository;
    @Autowired private S3Service s3Service;
    @Autowired private MantenimientoService mantenimientoService;
    @Autowired private ReservaCancelacionService cancelacionService;

    // Método para ver disponibilidad de un espacio mediante Calendario
    @GetMapping("/disponibilidad-espacio")
    public String disponibilidadEspacio(@RequestParam int idEspacio, HttpSession session, Model model) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);

        // Obtener reservas del espacio en estados 'Confirmada' y 'Pendiente de confirmación'
        List<String> estadosPermitidos = Arrays.asList("Confirmada", "Pendiente de confirmación");

        // Buscar reservas solo desde hoy hacia adelante (6 meses)
        LocalDate fechaInicio = LocalDate.now();
        LocalDate fechaFin = LocalDate.now().plusMonths(6);

        // Usar el método que devuelve DTOs en lugar de entidades completas
        List<ReservaCalendarioDto> reservasDto = reservaRepositoryVecino.buscarReservasParaCalendario(
                (long) idEspacio,
                estadosPermitidos,
                fechaInicio,
                fechaFin
        );

        System.out.println("Reservas encontradas para espacio " + idEspacio + ": " + reservasDto.size());
        for (ReservaCalendarioDto reserva : reservasDto) {
            boolean esPropia = reserva.getIdVecino() != null &&
                    reserva.getIdVecino().equals(usuario.getIdUsuarios());
            reserva.setEsPropia(esPropia);

            System.out.println("Reserva ID: " + reserva.getIdReserva() +
                    ", Fecha: " + reserva.getFecha() +
                    ", Estado: " + reserva.getEstado() +
                    ", Vecino: " + reserva.getVecinoNombre() + " " + reserva.getVecinoApellido() +
                    ", ¿Es propia? " + esPropia);
        }

        // Obtener mantenimientos activos del espacio
        List<Mantenimiento> mantenimientos = mantenimientoRepository.findByEspacioAndFechaBetween(
                        espacio, fechaInicio, fechaFin
                ).stream()
                .filter(m -> m.getEstado() == Mantenimiento.EstadoMantenimiento.PROGRAMADO ||
                        m.getEstado() == Mantenimiento.EstadoMantenimiento.EN_PROCESO)
                .collect(Collectors.toList());

        System.out.println("Mantenimientos encontrados para espacio " + idEspacio + ": " + mantenimientos.size());
        for (Mantenimiento mantenimiento : mantenimientos) {
            System.out.println("Mantenimiento ID: " + mantenimiento.getIdMantenimiento() +
                    ", Fecha inicio: " + mantenimiento.getFechaInicio() +
                    ", Fecha fin: " + mantenimiento.getFechaFin() +
                    ", Tipo: " + mantenimiento.getTipoMantenimiento() +
                    ", Estado: " + mantenimiento.getEstado());
        }

        // Pasar lista completa de coordinadores del lugar
        List<Usuarios> coordinadores = espacio.getIdLugar().getCoordinadores();

        model.addAttribute("coordinadores", coordinadores);
        model.addAttribute("espacio", espacio);
        model.addAttribute("usuario", usuario);
        model.addAttribute("reservas", reservasDto);
        model.addAttribute("mantenimientos", mantenimientos);

        return "vecino/ver-disponibilidad-calendario";
    }

    @GetMapping("/url-comprobante/{idReserva}")
    @ResponseBody
    public ResponseEntity<?> obtenerUrlComprobante(@PathVariable Integer idReserva) {
        Reserva reserva = reservaRepositoryVecino.findById(idReserva).orElse(null);
        if (reserva == null || reserva.getCapturaKey() == null) {
            return ResponseEntity.notFound().build();
        }
        String capturaKey = reserva.getCapturaKey();
        String key;

        // Extraer solo la key si es una URL completa
        if (capturaKey.startsWith("http")) {
            // Formato: https://bucket.s3.region.amazonaws.com/path/to/file.ext
            int startIndex = capturaKey.indexOf(".com/") + 5;
            if (startIndex > 5) {
                key = capturaKey.substring(startIndex);
            } else {
                // Fallback si no encuentra el patrón esperado
                key = capturaKey;
            }
            System.out.println("🔄 URL convertida a key: " + key);
        } else {
            // Ya es solo la key
            key = capturaKey;
        }

        try {
            String url = s3Service.generarUrlPreFirmada(key, 10);
            System.out.println("🔗 URL prefirmada generada: " + url);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            System.out.println("❌ Error al generar URL: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar la URL del comprobante: " + e.getMessage()));
        }
    }

    @PostMapping("/subir-comprobante")
    @ResponseBody
    public ResponseEntity<?> subirComprobante(
            @RequestParam("comprobante") MultipartFile archivo,
            @RequestParam("idReserva") Integer idReserva
    ) {
        try {
            if (archivo.isEmpty() || !archivo.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo debe ser una imagen."));
            }
            if (archivo.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo supera los 5MB permitidos."));
            }

            Reserva reserva = reservaRepositoryVecino.findById(idReserva).orElse(null);
            if (reserva == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Reserva no encontrada."));
            }

            String key = s3Service.subirArchivo(archivo, "privada/comprobantes/" + idReserva);
            reserva.setCapturaKey(key);
            reserva.setEstado(estadoReservaRepositoryVecino.findByEstado("Pendiente de confirmación"));
            reservaRepositoryVecino.save(reserva);

            // Enviar correo al coordinador
            mailManager.enviarNotificacionVerificarComprobante(reserva);

            return ResponseEntity.ok().body(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir el comprobante."));
        }
    }

    @GetMapping("/detalles-espacio")
    public String detalles(@RequestParam("id") int id, Model model, HttpSession session, HttpServletRequest request) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        Espacio espacio = espacioRepository.findById(id).orElse(null);

        if (espacio != null) {
            // Obtener URL actual completa
            String urlActual = request.getRequestURL().toString();
            model.addAttribute("urlActual", urlActual);
            Double promedio = calificacionRepository.promedioPorEspacio(espacio.getIdEspacio());
            model.addAttribute("espacio", espacio);
            model.addAttribute("promedioCalificacion", promedio != null ? promedio : 0.0);

            // Pasar lista completa de coordinadores del lugar
            List<Usuarios> coordinadores = espacio.getIdLugar().getCoordinadores();
            model.addAttribute("coordinadores", coordinadores);

            return "vecino/vecino-ver-detalles-espacio2";
        } else {
            return "redirect:/vecino/home";
        }
    }

    @GetMapping("/espacios-disponibles")
    public String mostrarEspaciosDisponibles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String nombreEspacio,
            @RequestParam(required = false) Integer lugar,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) List<String> deporte,
            @RequestParam(required = false) Integer precioMin,
            @RequestParam(required = false) Integer precioMax,
            @RequestParam(required = false) Integer estrellasMin,
            @RequestParam(defaultValue = "false") boolean isMobile,
            Model model,
            HttpServletRequest request) {

        // Detectar si es móvil desde User-Agent si no se especifica
        if (!isMobile) {
            String userAgent = request.getHeader("User-Agent");
            isMobile = detectarDispositivo(userAgent);
        }

        // Determinar tamaño de página basado en dispositivo
        int pageSize = isMobile ? 1 : 3; // 1 card en móvil, 3 en desktop (1 fila)

        // Crear Pageable con el tamaño apropiado
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        EspacioFiltroDTO filtro = new EspacioFiltroDTO();
        filtro.setTipo(tipo);
        filtro.setDeportes(deporte);
        filtro.setPrecioMin(precioMin);
        filtro.setPrecioMax(precioMax);
        filtro.setEstrellasMin(estrellasMin);
        filtro.setLugarId(lugar);
        filtro.setNombre(nombreEspacio);

        // Obtener datos filtrados
        Page<Espacio> espaciosPage = espacioService.buscarEspaciosConFiltros(filtro, pageable);

        // Calcular calificaciones
        Map<Integer, Double> calificacionesMap = new HashMap<>();
        for (Espacio e : espaciosPage) {
            Double promedio = calificacionRepository.promedioPorEspacio(e.getIdEspacio());
            calificacionesMap.put(e.getIdEspacio(), promedio != null ? promedio : 0.0);
        }

        Double precioMaxGlobal = espacioRepository.findMaxPrecio();
        int maxPrecio = precioMaxGlobal != null ? precioMaxGlobal.intValue() : 50;

        // Calcular rango de páginas para paginación compacta con puntos suspensivos
        int totalPages = espaciosPage.getTotalPages();
        int startPage, endPage;

        if (totalPages <= 5) {
            // Si hay 5 páginas o menos, mostrar todas
            startPage = 1;
            endPage = totalPages;
        } else {
            // Lógica para mostrar 3 páginas centrales
            if (page <= 2) {
                // Al inicio: mostrar 1, 2, 3
                startPage = 1;
                endPage = 3;
            } else if (page >= totalPages - 1) {
                // Al final: mostrar las últimas 3
                startPage = totalPages - 2;
                endPage = totalPages;
            } else {
                // En el medio: mostrar página actual y sus vecinas
                startPage = page - 1;
                endPage = page + 1;
            }
        }

        // Agregar atributos al modelo
        model.addAttribute("espaciosPage", espaciosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("isMobile", isMobile);
        model.addAttribute("calificacionesMap", calificacionesMap);

        // Mantener parámetros de filtro
        model.addAttribute("nombreEspacio", nombreEspacio);
        model.addAttribute("lugar", lugar);
        model.addAttribute("tipo", tipo);
        model.addAttribute("deporte", deporte);
        model.addAttribute("precioMin", precioMin != null ? precioMin : 0);

        model.addAttribute("precioMax", precioMax != null ? precioMax : 50);
        model.addAttribute("estrellasMin", estrellasMin);

        // Datos para filtros
        model.addAttribute("lugares", lugarRepository.findAll());
        model.addAttribute("tiposEspacio", tipoEspacioRepository.findAll());
        model.addAttribute("deportes", deporteRepository.findAll());

        // Enviar el precio máximo de BD para que el slider sepa cuál es el límite real
        // pero sin afectar la inicialización por defecto
        model.addAttribute("maxPrecioDisponible", maxPrecio);

        return "vecino/vecino-ver-espacios2";
    }

    /**
     * Método auxiliar para detectar si el dispositivo es móvil
     */
    private boolean detectarDispositivo(String userAgent) {
        if (userAgent == null) return false;

        userAgent = userAgent.toLowerCase();
        return userAgent.contains("mobile") ||
                userAgent.contains("android") ||
                userAgent.contains("iphone") ||
                userAgent.contains("ipad") ||
                userAgent.contains("blackberry") ||
                userAgent.contains("windows phone");
    }

    /**
     *  Ver mis reservas con lógica de botones mejorada
     */
    @GetMapping("/mis-reservas")
    public String verMisReservas(@RequestParam(required = false) Integer estado,
                                 HttpSession session,
                                 Model model) {
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        // Obtener reservas filtradas
        List<Reserva> reservasFiltradas;
        if (estado != null) {
            reservasFiltradas = reservaRepositoryVecino
                    .findByVecino_IdUsuariosAndEstado_IdEstadoReservaOrderByMomentoReservaDesc(
                            vecino.getIdUsuarios(), estado);
        } else {
            reservasFiltradas = reservaRepositoryVecino
                    .findByVecino_IdUsuariosOrderByMomentoReservaDesc(vecino.getIdUsuarios());
        }

        // Obtener todas las reservas para contadores
        List<Reserva> todasLasReservas = reservaRepositoryVecino
                .findByVecino_IdUsuariosOrderByMomentoReservaDesc(vecino.getIdUsuarios());

        // Preparar DTOs
        List<ReservaExtendidaDTO> reservasDTO = new ArrayList<>();
        for (Reserva r : reservasFiltradas) {
            Pago pago = pagoRepositoryVecino.findByReserva(r).orElse(null);
            SolicitudCancelacion solicitud = solicitudCancelacionRepository.findByReserva(r).orElse(null);
            reservasDTO.add(new ReservaExtendidaDTO(r, pago, solicitud));
        }

        // Calcular contadores
        long totalReservas = todasLasReservas.size();
        long confirmadas = todasLasReservas.stream()
                .filter(r -> "Confirmada".equals(r.getEstado().getEstado())).count();
        long noConfirmadas = todasLasReservas.stream()
                .filter(r -> "Pendiente de confirmación".equals(r.getEstado().getEstado())).count();
        long finalizadas = todasLasReservas.stream()
                .filter(r -> "Finalizada".equals(r.getEstado().getEstado())).count();
        long canceladas = todasLasReservas.stream()
                .filter(r -> "Cancelada".equals(r.getEstado().getEstado())).count();
        long reembolsadas = todasLasReservas.stream()
                .filter(r -> "Cancelada con reembolso".equals(r.getEstado().getEstado())).count();
        long reembolsosSolicitados = todasLasReservas.stream()
                .filter(r -> "Reembolso solicitado".equals(r.getEstado().getEstado())).count();
        long noReembolsadas = todasLasReservas.stream()
                .filter(r -> "Cancelada sin reembolso".equals(r.getEstado().getEstado())).count();

        // NUEVA LÓGICA: Evaluar acciones de cancelación usando el servicio
        Map<Integer, Map<String, Object>> logicaBotones = new HashMap<>();
        Map<Integer, String> tiposReembolso = new HashMap<>();
        Map<Integer, Long> horasRestantesMap = new HashMap<>();
        Map<Integer, String> fechasFormateadasMap = new HashMap<>();
        Map<Integer, String> horasFormateadasMap = new HashMap<>();

        DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es", "ES"));
        DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("h:mm a", new Locale("es", "ES"));

        for (ReservaExtendidaDTO reservaDTO : reservasDTO) {
            Reserva reserva = reservaDTO.getReserva();

            // Evaluar acciones de cancelación
            ReservaCancelacionService.AccionesCancelacion acciones =
                    cancelacionService.evaluarAccionesCancelacion(reserva, vecino);

            // Preparar datos para la vista
            Map<String, Object> botones = new HashMap<>();
            botones.put("mostrarCancelar", acciones.isPuedeCancelarDirecto());
            botones.put("mostrarSolicitar", acciones.isPuedeSolicitarCancelacion());
            logicaBotones.put(reserva.getIdReserva(), botones);

            // Información adicional
            tiposReembolso.put(reserva.getIdReserva(), acciones.getTipoReembolso());
            horasRestantesMap.put(reserva.getIdReserva(), acciones.getHorasRestantes());
            fechasFormateadasMap.put(reserva.getIdReserva(), reserva.getFecha().format(fechaFormatter));
            horasFormateadasMap.put(reserva.getIdReserva(), reserva.getHoraInicio().format(horaFormatter));

            // Debug para reserva específica
            if (reserva.getIdReserva() == 52) {
                System.out.println("=== RESERVA 52 REFACTORIZADA ===");
                System.out.println("Estado: " + acciones.getEstado());
                System.out.println("Tipo pago: " + acciones.getTipoPago());
                System.out.println("Horas restantes: " + acciones.getHorasRestantes());
                System.out.println("Puede cancelar directo: " + acciones.isPuedeCancelarDirecto());
                System.out.println("Puede solicitar: " + acciones.isPuedeSolicitarCancelacion());
                System.out.println("Tipo reembolso: " + acciones.getTipoReembolso());
                System.out.println("=================================");
            }
        }

        // Mapas de textos y colores
        Map<String, String> textosEstado = Map.of(
                "Confirmada", "Confirmada",
                "Pendiente de confirmación", "Pendiente",
                "Cancelada", "Cancelada",
                "Cancelada con reembolso", "Reembolsada",
                "Reembolso solicitado", "Reembolso Solicitado",
                "Cancelada sin reembolso", "Sin Reembolso",
                "Finalizada", "Finalizada"
        );

        Map<String, String> coloresEstado = Map.of(
                "Confirmada", "bg-label-success",
                "Pendiente de confirmación", "bg-label-warning",
                "Cancelada", "bg-label-danger",
                "Cancelada con reembolso", "bg-label-info",
                "Reembolso solicitado", "bg-label-primary",
                "Cancelada sin reembolso", "bg-label-github",
                "Finalizada", "bg-label-secondary"
        );

        // Agregar todos los atributos al modelo
        model.addAttribute("reservasDTO", reservasDTO);
        model.addAttribute("logicaBotones", logicaBotones);
        model.addAttribute("tiposReembolso", tiposReembolso);
        model.addAttribute("horasRestantesMap", horasRestantesMap);
        model.addAttribute("fechasFormateadasMap", fechasFormateadasMap);
        model.addAttribute("horasFormateadasMap", horasFormateadasMap);
        model.addAttribute("textosEstado", textosEstado);
        model.addAttribute("coloresEstado", coloresEstado);
        model.addAttribute("estado", estado);

        // Contadores
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("confirmadas", confirmadas);
        model.addAttribute("noConfirmadas", noConfirmadas);
        model.addAttribute("finalizadas", finalizadas);
        model.addAttribute("canceladas", canceladas);
        model.addAttribute("reembolsadas", reembolsadas);
        model.addAttribute("reembolsosSolicitados", reembolsosSolicitados);
        model.addAttribute("noReembolsadas", noReembolsadas);

        return "vecino/vecino-ver-mis-reservas";
    }

    @Value("${mercadopago.access.token}")
    private String accessToken;

    /**
     * MÉTODO Cancelar reserva directamente
     */
    @PostMapping("/cancelarReserva")
    public String cancelarReserva(@RequestParam int id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        Reserva reserva = reservaRepositoryVecino.findById(id).orElse(null);

        if (reserva == null || !reserva.getVecino().getIdUsuarios().equals(usuario.getIdUsuarios())) {
            redirectAttrs.addFlashAttribute("msg", "Reserva no encontrada o no autorizada.");
            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
        }

        // Validar usando el servicio
        ReservaCancelacionService.ResultadoValidacion validacion =
                cancelacionService.validarCancelacionDirecta(reserva, usuario);

        if (!validacion.isValido()) {
            redirectAttrs.addFlashAttribute("msg", validacion.getMensaje());
            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
        }

        try {
            // Obtener información de la acción
            ReservaCancelacionService.AccionesCancelacion acciones =
                    cancelacionService.evaluarAccionesCancelacion(reserva, usuario);

            String estadoActual = reserva.getEstado().getEstado();
            String tipoPago = reserva.getTipoPago();

            if ("Pendiente de confirmación".equals(estadoActual)) {
                // Cancelación sin reembolso
                procesarCancelacionSinReembolso(reserva, usuario, redirectAttrs);

            } else if ("Confirmada".equals(estadoActual) && "En línea".equals(tipoPago) &&
                    acciones.getHorasRestantes() >= 24) {
                // Cancelación con reembolso automático MercadoPago
                procesarCancelacionConReembolsoAutomatico(reserva, usuario, redirectAttrs);

            } else if ("Confirmada".equals(estadoActual) && "En banco".equals(tipoPago)) {
                // Cancelación con reembolso manual
                procesarCancelacionConReembolsoManual(reserva, usuario, redirectAttrs);

            } else {
                redirectAttrs.addFlashAttribute("msg", "Esta reserva no puede cancelarse directamente.");
            }

            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";

        } catch (Exception e) {
            System.err.println("Error en cancelación: " + e.getMessage());
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("msg", "Error al procesar la cancelación: " + e.getMessage());
            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
        }
    }

    private String formatearFechaHora(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        // Formatear fecha en español
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        String fechaFormateada = fecha.getDayOfMonth() + " de " + meses[fecha.getMonthValue() - 1] + " de " + fecha.getYear();

        // Formatear horas
        String horaInicioStr = horaInicio.format(DateTimeFormatter.ofPattern("HH:mm"));
        String horaFinStr = horaFin.format(DateTimeFormatter.ofPattern("HH:mm"));

        return fechaFormateada + " de " + horaInicioStr + " a " + horaFinStr;
    }

    @GetMapping("/exportarPagos")
    public void exportarPagos(@RequestParam String tipo ,HttpSession session, HttpServletResponse response) throws IOException {
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        List<Pago> pagos = pagoRepositoryVecino.findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(vecino.getIdUsuarios());

        if ("excel".equalsIgnoreCase(tipo)) {
            pagoExportService.exportarPagosAExcel(pagos, response);
        } else {
            pagoExportService.exportarPagosAPdf(pagos, response);
        }
    }

    @GetMapping("/editar-perfil")
    public String editarPerfil(HttpSession session, Model model) {
        // Suponiendo que guardaste al usuario logueado en la sesión
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        if (vecino == null) {
            // Si no hay sesión activa, redirigir al login o a la página principal
            return "redirect:/login";
        }

        model.addAttribute("vecino", vecino);

        return "vecino/vecino-editar-perfil"; // nombre de tu HTML sin extensión
    }

    @GetMapping("/seguridad")
    public String editarSeguridad(HttpSession session, Model model){
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");
        return "vecino/vecino-editar-seguridad";
    }

    @PostMapping("/guardarPerfil")
    public String guardarPerfil(@ModelAttribute("vecino") Usuarios vecinoForm, HttpSession session, RedirectAttributes redirectAttributes) {

        Usuarios vecinoSesion = (Usuarios) session.getAttribute("usuario");

        if (vecinoSesion == null) {
            System.out.println("===> Sesión no encontrada, redirigiendo a login.");
            return "redirect:/login";
        }

        // Buscar si existe otro usuario con ese correo
        Optional<Usuarios> usuarioExistenteOptional = usuariosRepository.findByCorreo(vecinoForm.getCorreo());

        if (usuarioExistenteOptional.isPresent()) {
            Usuarios usuarioExistente = usuarioExistenteOptional.get();

            if (!usuarioExistente.getIdUsuarios().equals(vecinoSesion.getIdUsuarios())) {
                // El correo está siendo usado por otro usuario
                redirectAttributes.addFlashAttribute("error", "El correo ya está en uso por otro usuario.");
                return "redirect:/vecino/editar-perfil";
            }
        }

        // Solo actualizas los campos que permites editar
        vecinoSesion.setNombres(vecinoForm.getNombres());
        vecinoSesion.setApellidos(vecinoForm.getApellidos());
        vecinoSesion.setCorreo(vecinoForm.getCorreo());
        vecinoSesion.setTelefono(vecinoForm.getTelefono());

        // Aquí deberías tener tu repositorio para guardar
        usuariosRepository.save(vecinoSesion);
        System.out.println("===> Datos actualizados y guardados.");

        // Opcionalmente actualizar la sesión
        session.setAttribute("usuario", vecinoSesion);

        return "redirect:/vecino/editar-perfil?success"; // Puedes redirigir y mostrar un mensaje
    }

    // ESTE METODO SOLO SE USA PARA RESERVAS CON TIPO DE PAGO EN BANCO
    @Transactional
    @PostMapping("/crearReserva")
    public ResponseEntity<?> crearReserva(@RequestParam int idEspacio,
                                          @RequestParam String fecha,
                                          @RequestParam String horaInicio,
                                          @RequestParam String horaFin,
                                          @RequestParam String tipoPago,
                                          @RequestParam int idCoordinador,
                                          HttpSession session,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {

        // Convertir las horas manualmente
        LocalTime horaInicioTime;
        LocalTime horaFinTime;
        LocalDate fechaReserva;

        // Verificar si es una petición AJAX
        String acceptHeader = request.getHeader("Accept");
        boolean isAjax = acceptHeader != null && acceptHeader.contains("application/json");

        try {
            // Convertir fecha
            fechaReserva = LocalDate.parse(fecha);

            // Si viene solo la hora (ej: "15"), agregar minutos
            if (horaInicio.length() <= 2) {
                horaInicioTime = LocalTime.of(Integer.parseInt(horaInicio), 0);
            } else {
                horaInicioTime = LocalTime.parse(horaInicio);
            }

            if (horaFin.length() <= 2) {
                horaFinTime = LocalTime.of(Integer.parseInt(horaFin), 0);
            } else {
                horaFinTime = LocalTime.parse(horaFin);
            }

            System.out.println("Datos convertidos - Fecha: " + fechaReserva +
                    ", Horas: " + horaInicioTime + " - " + horaFinTime);

        } catch (Exception e) {
            System.err.println("Error convirtiendo horas: " + e.getMessage());
            String errorMsg = "Formato de fecha u hora inválido";

            if (isAjax) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", errorMsg, "error_type", "validation"));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/espacios-disponibles")
                    .build();
        }

        // Vecino de la sesión
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");
        // Espacio seleccionado que se reservará
        Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);

        if (vecino == null || espacio == null) {
            String errorMsg = "Error al procesar la reserva.";
            if (isAjax) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", errorMsg, "error_type", "system"));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/espacios-disponibles")
                    .build();
        }

        // Validar que el coordinador existe y pertenece al lugar del espacio
        Usuarios coordinador = usuariosRepository.findById(idCoordinador).orElse(null);
        if (coordinador == null ||
                espacio.getIdLugar() == null ||
                !espacio.getIdLugar().getCoordinadores().contains(coordinador)) {
            String errorMsg = "Coordinador no válido para este espacio.";
            if (isAjax) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", errorMsg, "error_type", "validation"));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/espacios-disponibles")
                    .build();
        }

        try {
            // 🔧 NUEVA VALIDACIÓN: Verificar conflictos con mantenimientos PRIMERO
            System.out.println("🔍 Verificando conflictos con mantenimientos...");
            mantenimientoService.verificarConflictosConMantenimientos(
                    espacio, fechaReserva, horaInicioTime, horaFinTime);

            // Validar que no existan conflictos de horario con otras reservas
            List<Reserva> conflictos = reservaRepositoryVecino.findConflictosEnHorario(
                    idEspacio,
                    fechaReserva,
                    horaInicioTime,
                    horaFinTime
            );

            if (!conflictos.isEmpty()) {
                String errorMsg = "Ya existe una reserva en el horario seleccionado.";
                if (isAjax) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", errorMsg, "error_type", "conflict"));
                }
                redirectAttributes.addFlashAttribute("error", errorMsg);
                return ResponseEntity.status(302)
                        .header("Location", "/vecino/disponibilidad-espacio?idEspacio=" + idEspacio)
                        .build();
            }

        } catch (IllegalArgumentException e) {
            // Error de validación de mantenimientos - MEJORAR LA RESPUESTA
            System.err.println("❌ Conflicto con mantenimiento: " + e.getMessage());

            // Crear mensaje más descriptivo
            String errorMsg = e.getMessage();
            if (!errorMsg.contains("mantenimiento") && !errorMsg.contains("Mantenimiento")) {
                errorMsg = "No se puede crear la reserva debido a mantenimientos programados: " + errorMsg;
            }

            if (isAjax) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", errorMsg,
                                "error_type", "maintenance_conflict",
                                "details", Map.of(
                                        "fecha", fechaReserva.toString(),
                                        "horaInicio", horaInicioTime.toString(),
                                        "horaFin", horaFinTime.toString(),
                                        "espacio", espacio.getNombre()
                                )
                        ));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/disponibilidad-espacio?idEspacio=" + idEspacio)
                    .build();

        } catch (Exception e) {
            // Cualquier otra excepción durante la validación
            System.err.println("❌ Error inesperado durante validación: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = "Error al validar la disponibilidad del espacio.";
            if (isAjax) {
                return ResponseEntity.status(500)
                        .body(Map.of(
                                "success", false,
                                "message", errorMsg,
                                "error_type", "system_error"
                        ));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/disponibilidad-espacio?idEspacio=" + idEspacio)
                    .build();
        }

        EstadoReserva estado;
        if (tipoPago.equalsIgnoreCase("En banco")) {
            estado = estadoReservaRepositoryVecino.findById(2).orElse(null); // PENDIENTE DE CONFIRMACION
        } else {
            estado = estadoReservaRepositoryVecino.findById(1).orElse(null); // CONFIRMADA
        }

        if (estado == null) {
            String errorMsg = "Error al determinar el estado de la reserva.";
            if (isAjax) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", errorMsg, "error_type", "system"));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/espacios-disponibles")
                    .build();
        }

        // SE CREA LA NUEVA RESERVA
        Reserva reserva = new Reserva();
        reserva.setVecino(vecino);
        reserva.setEspacio(espacio);
        reserva.setEstado(estado);
        reserva.setCoordinador(coordinador);
        reserva.setFecha(fechaReserva);
        reserva.setHoraInicio(horaInicioTime);
        reserva.setHoraFin(horaFinTime);
        reserva.setTipoPago(tipoPago);
        reserva.setMomentoReserva(LocalDateTime.now());
        reserva.setEstadoReembolso(Reserva.EstadoReembolso.NO_APLICA);

        // Calcular costo basado en la diferencia de horas
        long horas = java.time.Duration.between(horaInicioTime, horaFinTime).toHours();
        reserva.setCosto(espacio.getCosto() * horas);

        try {
            // Guarda primero la reserva para que tenga ID
            Reserva reservaGuardada = reservaRepositoryVecino.save(reserva);
            System.out.println("✅ Reserva guardada exitosamente con ID: " + reservaGuardada.getIdReserva());

            // Crear el pago relacionado si es pago en línea
            if (tipoPago.equalsIgnoreCase("En línea")) {
                Pago pago = new Pago();
                pago.setReserva(reservaGuardada);
                pago.setFechaPago(LocalDateTime.now());
                pago.setMonto(BigDecimal.valueOf(reservaGuardada.getCosto()));
                pago.setTipoPago(tipoPago);
                pago.setEstado("Pagado");
                pagoRepositoryVecino.save(pago);
            }

            // Enviar correo según el estado
            try {
                if (estado.getEstado().equalsIgnoreCase("Confirmada")) {
                    mailManager.enviarCorreoReservaConfirmada(vecino, reservaGuardada);
                } else if (estado.getIdEstadoReserva() == 2) {
                    mailManager.enviarCorreoReservaPendiente(vecino, reservaGuardada);
                }
            } catch (Exception emailError) {
                System.err.println("⚠️ Error enviando correo: " + emailError.getMessage());
                // No fallar la reserva por error de correo
            }

            // Respuesta para peticiones AJAX
            if (isAjax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Reserva generada correctamente.");
                response.put("idReserva", reservaGuardada.getIdReserva());
                response.put("redirectUrl", "/vecino/boleta/" + reservaGuardada.getIdReserva());

                // Información adicional de la reserva para el frontend
                response.put("reserva", Map.of(
                        "id", reservaGuardada.getIdReserva(),
                        "fecha", reservaGuardada.getFecha().toString(),
                        "horaInicio", reservaGuardada.getHoraInicio().toString(),
                        "horaFin", reservaGuardada.getHoraFin().toString(),
                        "costo", reservaGuardada.getCosto(),
                        "estado", reservaGuardada.getEstado().getEstado(),
                        "espacio", reservaGuardada.getEspacio().getNombre(),
                        "tipoPago", reservaGuardada.getTipoPago()
                ));

                return ResponseEntity.ok(response);
            }

            // Respuesta tradicional (redirección a la boleta)
            redirectAttributes.addFlashAttribute("msg", "Reserva generada correctamente.");
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/boleta/" + reservaGuardada.getIdReserva())
                    .build();

        } catch (DataAccessException e) {
            // Error específico de base de datos
            System.err.println("❌ Error de base de datos: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = "Error al guardar la reserva en la base de datos.";
            if (isAjax) {
                return ResponseEntity.status(500)
                        .body(Map.of(
                                "success", false,
                                "message", errorMsg,
                                "error_type", "database_error"
                        ));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/espacios-disponibles")
                    .build();

        } catch (Exception e) {
            // Error general
            e.printStackTrace();
            System.err.println("❌ Error guardando reserva: " + e.getMessage());

            String errorMsg = "No se pudo registrar la reserva. Intente nuevamente.";
            if (isAjax) {
                return ResponseEntity.status(500)
                        .body(Map.of(
                                "success", false,
                                "message", errorMsg,
                                "error_type", "general_error"
                        ));
            }
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return ResponseEntity.status(302)
                    .header("Location", "/vecino/espacios-disponibles")
                    .build();
        }
    }

    @GetMapping("/boleta/{idReserva}")
    public String verBoletaPago(@PathVariable Integer idReserva, Model model, HttpSession session) {
        // Obtiene al usuario vecino desde la sesión activa
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        // Busca la reserva en la base de datos
        Reserva reserva = reservaRepositoryVecino.findById(idReserva).orElse(null);

        // Si la reserva no existe o no pertenece al usuario actual, redirige a "espacios disponibles"
        if (reserva == null || !reserva.getVecino().getIdUsuarios().equals(vecino.getIdUsuarios())) {
            return "redirect:/vecino/espacios-disponibles";
        }

        // Si la reserva está pagada pero no confirmada, actualiza su estado en la base de datos
        if ("Confirmada".equals(reserva.getEstadoPago()) && !"Confirmada".equals(reserva.getEstado().getEstado())) {
            EstadoReserva estadoConfirmado = estadoReservaRepositoryVecino.findByEstado("Confirmada");
            if (estadoConfirmado != null) {
                reserva.setEstado(estadoConfirmado);
                reservaRepositoryVecino.save(reserva); // Guarda el estado actualizado
            }
        }

        // Añade la reserva actualizada al modelo para la vista
        model.addAttribute("reserva", reserva);

        return "vecino/boleta-pago"; // Retorna la vista de la boleta de pago
    }

    @GetMapping("/estado-reserva/{idReserva}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> obtenerEstadoReserva(@PathVariable Integer idReserva) {
        Reserva reserva = reservaRepositoryVecino.findById(idReserva).orElse(null);

        // Si la reserva no existe, retorna una respuesta 404 con mensaje apropiado
        if (reserva == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("estado", "No encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Devuelve el estado de la reserva encontrada
        Map<String, String> response = new HashMap<>();
        response.put("estado", reserva.getEstado().getEstado());
        return ResponseEntity.ok(response);
    }

    // MERCADO PAGO - PAGOS EN LÍNEA
    @Value("${mercadopago.access.token}")
    private String mercadoPagoAccessToken;

    @GetMapping("/pago-exitoso")
    public String pagoExitoso(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preference_id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        System.out.println("=== PROCESANDO PAGO EXITOSO ===");
        System.out.println("Payment ID: " + payment_id);
        System.out.println("Status: " + status);
        System.out.println("Preference ID: " + preference_id);

        try {
            // 1. Obtener datos de reserva de la sesión
            Map<String, Object> reservaData = (Map<String, Object>) session.getAttribute("reservaEnProceso");
            if (reservaData == null) {
                System.err.println("❌ No se encontraron datos de reserva en sesión");
                redirectAttributes.addFlashAttribute("error", "No se encontraron datos de la reserva. Por favor, intenta de nuevo.");
                return "redirect:/vecino/espacios-disponibles";
            }

            System.out.println("✅ Datos de reserva encontrados: " + reservaData);

            // 2. Verificar que el pago fue exitoso
            if (payment_id == null || (!status.equals("approved") && !status.equals("in_process"))) {
                System.err.println("❌ Estado de pago no válido: " + status);
                redirectAttributes.addFlashAttribute("error", "El pago no fue procesado correctamente.");
                return "redirect:/vecino/pago-fallido?status=" + status;
            }

            // 3. Verificar disponibilidad una vez más antes de crear la reserva
            Integer idEspacio = (Integer) reservaData.get("idEspacio");
            String fechaStr = (String) reservaData.get("fecha");
            String horaInicioStr = (String) reservaData.get("horaInicio");
            String horaFinStr = (String) reservaData.get("horaFin");

            LocalDate fecha = LocalDate.parse(fechaStr);
            LocalTime horaInicio = LocalTime.parse(horaInicioStr);
            LocalTime horaFin = LocalTime.parse(horaFinStr);

            // Verificar que el espacio sigue disponible
            List<Espacio> espaciosDisponibles = espacioRepository.findEspaciosDisponibles(fecha, horaInicio, horaFin);
            if (espaciosDisponibles.stream().noneMatch(e -> e.getIdEspacio().equals(idEspacio))) {
                System.err.println("❌ El espacio ya no está disponible - Iniciando reembolso automático");

                try {
                    // Iniciar proceso de reembolso automático
                    boolean reembolsoExitoso = procesarReembolsoAutomatico(payment_id, (Double) reservaData.get("costo"));

                    if (reembolsoExitoso) {
                        // Limpiar datos de sesión
                        session.removeAttribute("reservaEnProceso");

                        redirectAttributes.addFlashAttribute("error",
                                "❌ El espacio ya no está disponible. Se ha procesado automáticamente el reembolso. " +
                                        "Los fondos aparecerán en tu cuenta en un plazo de 5-10 días hábiles.");

                        System.out.println("✅ Reembolso automático procesado exitosamente para payment_id: " + payment_id);
                    } else {
                        // Si falla el reembolso automático, crear una solicitud manual
                        crearSolicitudReembolsoManual(payment_id, reservaData, "Espacio no disponible al momento del pago");

                        redirectAttributes.addFlashAttribute("error",
                                "❌ El espacio ya no está disponible. Hemos creado una solicitud de reembolso que será " +
                                        "procesada manualmente. Recibirás una respuesta en las próximas 24 horas.");

                        System.out.println("⚠️ Reembolso automático falló - Solicitud manual creada para payment_id: " + payment_id);
                    }

                } catch (Exception e) {
                    System.err.println("❌ Error en proceso de reembolso: " + e.getMessage());
                    e.printStackTrace();

                    // Como último recurso, crear solicitud manual
                    try {
                        crearSolicitudReembolsoManual(payment_id, reservaData, "Error técnico durante reembolso automático");
                        redirectAttributes.addFlashAttribute("error",
                                "❌ Ocurrió un error técnico. Hemos creado una solicitud de reembolso que será procesada manualmente.");
                    } catch (Exception ex) {
                        System.err.println("❌ Error crítico creando solicitud manual: " + ex.getMessage());
                        redirectAttributes.addFlashAttribute("error",
                                "❌ Error crítico. Por favor contacta al soporte técnico con el código: " + payment_id);
                    }
                }

                return "redirect:/vecino/espacios-disponibles";
            }

            // 4. Crear la reserva en la base de datos
            Reserva nuevaReserva = new Reserva();

            // Obtener entidades necesarias
            Usuarios vecino = usuariosRepository.findById((Integer) reservaData.get("idVecino")).orElse(null);
            Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);
            EstadoReserva estadoReserva = estadoReservaRepositoryVecino.findByEstado("Confirmada");

            // Obtener un coordinador por defecto
            Usuarios coordinadorDefecto = obtenerCoordinadorDefecto();

            if (vecino == null || espacio == null || estadoReserva == null) {
                System.err.println("❌ Error al obtener entidades necesarias");
                System.err.println("Vecino: " + (vecino != null ? "OK" : "NULL"));
                System.err.println("Espacio: " + (espacio != null ? "OK" : "NULL"));
                System.err.println("Estado: " + (estadoReserva != null ? "OK" : "NULL"));
                redirectAttributes.addFlashAttribute("error", "Error interno al procesar la reserva.");
                return "redirect:/vecino/espacios-disponibles";
            }

            if (coordinadorDefecto == null) {
                System.err.println("❌ No se encontró coordinador por defecto");
                redirectAttributes.addFlashAttribute("error", "Error: No hay coordinadores disponibles.");
                return "redirect:/vecino/espacios-disponibles";
            }

            // Configurar la reserva
            nuevaReserva.setVecino(vecino);
            nuevaReserva.setEspacio(espacio);
            nuevaReserva.setCoordinador(coordinadorDefecto); // ✅ ASIGNAR COORDINADOR
            nuevaReserva.setFecha(fecha);
            nuevaReserva.setHoraInicio(horaInicio);
            nuevaReserva.setHoraFin(horaFin);
            nuevaReserva.setCosto((Double) reservaData.get("costo"));
            nuevaReserva.setEstado(estadoReserva);
            nuevaReserva.setMomentoReserva(LocalDateTime.now());
            nuevaReserva.setTipoPago("En línea");
            nuevaReserva.setEstadoPago("Pagado");
            nuevaReserva.setIdTransaccionPago(payment_id);
            nuevaReserva.setFechaPago(LocalDateTime.now());

            // Guardar la reserva
            Reserva reservaGuardada = reservaRepositoryVecino.save(nuevaReserva);
            System.out.println("✅ Reserva creada con ID: " + reservaGuardada.getIdReserva());

            // 5. Crear el registro de pago
            Pago nuevoPago = new Pago();
            nuevoPago.setReserva(reservaGuardada);
            nuevoPago.setMonto(BigDecimal.valueOf(reservaGuardada.getCosto()));
            nuevoPago.setFechaPago(LocalDateTime.now());
            nuevoPago.setTipoPago("En línea");
            nuevoPago.setEstado("Pagado");
            nuevoPago.setReferencia(payment_id);

            pagoRepositoryVecino.save(nuevoPago);
            System.out.println("✅ Registro de pago creado");

            // 6.1. Enviar notificación por correo al vecino
            try {
                mailManager.enviarCorreoReservaConfirmada(vecino, reservaGuardada);
                System.out.println("📧 Correo de confirmación enviado a: " + vecino.getCorreo());
            } catch (Exception ex) {
                System.err.println("❌ Error enviando correo de confirmación: " + ex.getMessage());
            }

            // 6. Limpiar datos de sesión
            session.removeAttribute("reservaEnProceso");
            System.out.println("✅ Datos de sesión limpiados");

            // 7. Añadir datos al modelo para mostrar en la vista
            model.addAttribute("reserva", reservaGuardada);
            model.addAttribute("paymentId", payment_id);
            model.addAttribute("mensaje", "¡Tu reserva ha sido confirmada exitosamente!");

            System.out.println("=== PAGO PROCESADO EXITOSAMENTE ===");
            return "vecino/pago-exitoso";

        } catch (Exception e) {
            System.err.println("❌ Error procesando pago exitoso: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al procesar la reserva: " + e.getMessage());
            return "redirect:/vecino/espacios-disponibles";
        }
    }

    /**
     * Procesa un reembolso automático a través de MercadoPago
     */
    private boolean procesarReembolsoAutomatico(String paymentId, Double monto) {
        try {
            System.out.println("🔄 Iniciando reembolso automático para payment_id: " + paymentId + " - Monto: " + monto);

            // Configurar cliente HTTP
            HttpClient client = HttpClient.newHttpClient();

            // Crear request de reembolso
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId + "/refunds"))
                    .header("Authorization", "Bearer " + mercadoPagoAccessToken)
                    .header("Content-Type", "application/json")
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString("{}")) // Reembolso total
                    .build();

            // Enviar request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            System.out.println("🔄 Respuesta de MercadoPago - Código: " + statusCode);
            System.out.println("🔄 Respuesta de MercadoPago - Body: " + responseBody);

            if (statusCode == 201) {
                System.out.println("✅ Reembolso automático exitoso para payment_id: " + paymentId);
                return true;
            } else {
                System.err.println("❌ Error en reembolso - Código: " + statusCode + " - Respuesta: " + responseBody);
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Excepción durante reembolso automático: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Método auxiliar para procesar reembolso automático de MercadoPago
     */
    private boolean procesarReembolsoAutomaticoMercadoPago(Reserva reserva, Usuarios usuario, RedirectAttributes redirectAttrs) {
        try {
            // Buscar el pago para obtener el ID de transacción
            Optional<Pago> pagoOpt = pagoRepository.findByReservaAndEstadoIgnoreCase(reserva, "PAGADO");

            if (pagoOpt.isPresent() && pagoOpt.get().getReferencia() != null) {
                String paymentId = pagoOpt.get().getReferencia();
                Double monto = reserva.getCosto();

                // Usar tu método existente para procesar el reembolso
                boolean reembolsoExitoso = procesarReembolsoAutomatico(paymentId, monto);

                if (reembolsoExitoso) {
                    redirectAttrs.addFlashAttribute("msg",
                            "Reserva cancelada exitosamente. Reembolso automático procesado. " +
                                    "El dinero estará disponible en tu cuenta en 5-7 días hábiles.");

                    // En lugar de la llamada larga, usar:
                    mailManager.enviarCorreoReembolsoAutomatico(usuario, reserva, reembolsoExitoso);
                    return true;
                } else {
                    redirectAttrs.addFlashAttribute("msg",
                            "Reserva cancelada. El reembolso automático falló y será procesado manualmente. " +
                                    "Contacta al coordinador si no recibes el reembolso en 5 días.");
                    return false;
                }
            } else {
                redirectAttrs.addFlashAttribute("msg",
                        "Reserva cancelada, pero no se encontró información de pago válida para reembolso automático. " +
                                "El coordinador procesará el reembolso manualmente.");
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("msg",
                    "Reserva cancelada, pero ocurrió un error al procesar el reembolso automático. " +
                            "El coordinador gestionará el reembolso manualmente.");
            return false;
        }
    }

    /**
     * Crea una solicitud de reembolso manual cuando falla el automático
     */
    private void crearSolicitudReembolsoManual(String paymentId, Map<String, Object> reservaData, String motivo) {
        try {
            System.out.println("📝 Creando solicitud de reembolso manual para payment_id: " + paymentId);

            // Primero crear la reserva temporal para poder asociar la solicitud
            Reserva reservaTemporal = crearReservaTemporal(reservaData, paymentId);

            if (reservaTemporal != null) {
                // Crear solicitud de cancelación
                SolicitudCancelacion solicitud = new SolicitudCancelacion();
                solicitud.setReserva(reservaTemporal);
                solicitud.setMotivo("REEMBOLSO AUTOMÁTICO: " + motivo + " - Payment ID: " + paymentId);
                solicitud.setEstado("Pendiente");
                solicitud.setCodigoPago(paymentId);
                solicitud.setFechaSolicitud(LocalDateTime.now());

                // Guardar solicitud (necesitarás inyectar SolicitudCancelacionRepository)
                solicitudCancelacionRepository.save(solicitud);

                System.out.println("✅ Solicitud de reembolso manual creada con ID: " + solicitud.getId());
            } else {
                System.err.println("❌ No se pudo crear reserva temporal para solicitud manual");
            }

        } catch (Exception e) {
            System.err.println("❌ Error creando solicitud de reembolso manual: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error creando solicitud manual", e);
        }
    }

    /**
     * Crea una reserva temporal para poder asociar la solicitud de reembolso
     */
    private Reserva crearReservaTemporal(Map<String, Object> reservaData, String paymentId) {
        try {
            Reserva reservaTemporal = new Reserva();

            // Obtener entidades necesarias
            Integer idVecino = (Integer) reservaData.get("idVecino");
            Integer idEspacio = (Integer) reservaData.get("idEspacio");

            Usuarios vecino = usuariosRepository.findById(idVecino).orElse(null);
            Espacio espacio = espacioRepositoryVecino.findById(idEspacio).orElse(null);
            EstadoReserva estadoCancelada = estadoReservaRepositoryVecino.findByEstado("Cancelada");
            Usuarios coordinadorDefecto = obtenerCoordinadorDefecto();

            if (vecino == null || espacio == null || estadoCancelada == null || coordinadorDefecto == null) {
                System.err.println("❌ Error obteniendo entidades para reserva temporal");
                return null;
            }

            // Configurar reserva temporal
            reservaTemporal.setVecino(vecino);
            reservaTemporal.setEspacio(espacio);
            reservaTemporal.setCoordinador(coordinadorDefecto);
            reservaTemporal.setFecha(LocalDate.parse((String) reservaData.get("fecha")));
            reservaTemporal.setHoraInicio(LocalTime.parse((String) reservaData.get("horaInicio")));
            reservaTemporal.setHoraFin(LocalTime.parse((String) reservaData.get("horaFin")));
            reservaTemporal.setCosto((Double) reservaData.get("costo"));
            reservaTemporal.setEstado(estadoCancelada); // Estado cancelada porque no se pudo completar
            reservaTemporal.setMomentoReserva(LocalDateTime.now());
            reservaTemporal.setTipoPago("En línea");
            reservaTemporal.setEstadoPago("Reembolso pendiente");
            reservaTemporal.setIdTransaccionPago(paymentId);
            reservaTemporal.setFechaPago(LocalDateTime.now());

            // Guardar reserva temporal
            Reserva reservaGuardada = reservaRepositoryVecino.save(reservaTemporal);

            // También crear el registro de pago para tener trazabilidad
            Pago pagoTemporal = new Pago();
            pagoTemporal.setReserva(reservaGuardada);
            pagoTemporal.setMonto(BigDecimal.valueOf(reservaGuardada.getCosto()));
            pagoTemporal.setFechaPago(LocalDateTime.now());
            pagoTemporal.setTipoPago("En línea");
            pagoTemporal.setEstado("REEMBOLSO_PENDIENTE");
            pagoTemporal.setReferencia(paymentId);

            pagoRepositoryVecino.save(pagoTemporal);

            System.out.println("✅ Reserva temporal creada con ID: " + reservaGuardada.getIdReserva());
            return reservaGuardada;

        } catch (Exception e) {
            System.err.println("❌ Error creando reserva temporal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene un coordinador por defecto para reservas en línea (AHORA DEBE SER UN PARÁMETRO)
     */
    private Usuarios obtenerCoordinadorDefecto() {
        try {
            // Buscar un usuario con rol de coordinador
            List<Usuarios> coordinadores = usuariosRepository.findAll().stream()
                    .filter(u -> u.getRol() != null &&
                            "Coordinador".equalsIgnoreCase(u.getRol().getRol()))
                    .toList();

            if (!coordinadores.isEmpty()) {
                Usuarios coordinador = coordinadores.get(0); // Tomar el primero
                System.out.println("✅ Coordinador por defecto asignado: " + coordinador.getNombres());
                return coordinador;
            }

            System.err.println("❌ No se encontraron coordinadores en el sistema");
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error al buscar coordinador por defecto: " + e.getMessage());
            return null;
        }
    }

    @GetMapping("/pago-fallido")
    public String pagoFallido(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model) {

        Object reservaObj = session.getAttribute("reservaEnProceso");

        if (reservaObj instanceof Reserva reservaEnProceso) {
            model.addAttribute("espacio", reservaEnProceso.getEspacio());
            model.addAttribute("fecha", reservaEnProceso.getFecha());
            model.addAttribute("horaInicio", reservaEnProceso.getHoraInicio());
            model.addAttribute("horaFin", reservaEnProceso.getHoraFin());
        } else {
            model.addAttribute("mensaje", "No se pudo recuperar la información de la reserva.");
        }

        model.addAttribute("paymentId", payment_id);
        model.addAttribute("status", status);

        return "vecino/pago-fallido";
    }

    @GetMapping("/pago-pendiente")
    public String pagoPendiente(
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model) {

        // Similar al pago fallido, pero para pagos pendientes
        Reserva reservaEnProceso = (Reserva) session.getAttribute("reservaEnProceso");

        if (reservaEnProceso != null) {
            model.addAttribute("espacio", reservaEnProceso.getEspacio());
            model.addAttribute("fecha", reservaEnProceso.getFecha());
            model.addAttribute("horaInicio", reservaEnProceso.getHoraInicio());
            model.addAttribute("horaFin", reservaEnProceso.getHoraFin());
        }

        model.addAttribute("paymentId", payment_id);
        model.addAttribute("status", status);

        return "vecino/pago-pendiente";
    }

    /**
     * Método para verificar el estado del pago en MercadoPago (en un futuro cuando el usuario envíe su id de transacción para la cancelación)
     * y usarlo como un mini formulario haciendo la consulta para aceptar o no el reembolso
     */
    private boolean verificarPagoEnMercadoPago(String paymentId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(mercadoPagoAccessToken);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payments/" + paymentId,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                String status = (String) response.getBody().get("status");
                return "approved".equals(status);
            }

            return false;
        } catch (Exception e) {
            return false; // En caso de error, consideramos que no está verificado
        }
    }

    @PostMapping("/eliminarCuenta")
    public String eliminarCuenta(@RequestParam("confirmPassword") String confirmPassword, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // Aquí deberías verificar que la contraseña es correcta
        if (!passwordEncoder.matches(confirmPassword, usuario.getContrasena())) {
            // Contraseña incorrecta
            return "redirect:/vecino/editar-perfil?error=password";
        }

        // Cambiar estado a desactivado (idEstado = 2)
        EstadoUsu desactivado = estadoRepository.findByEstado("Desactivado"); // Asegúrate que tienes el estado "desactivado"
        usuario.setEstado(desactivado);

        usuariosRepository.save(usuario);

        // Invalidar sesión
        session.invalidate();

        return "redirect:/"; // O a donde quieras mandarlo después de eliminar
    }

    @PostMapping("/actualizarContrasena")
    public String actualizarContrasena(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            System.out.println("===> No hay usuario en sesión");
            return "redirect:/login";
        }

        System.out.println("===> Usuario en sesión: " + usuario.getCorreo());
        System.out.println("===> Contraseña actual ingresada: " + currentPassword);
        System.out.println("===> Nueva contraseña ingresada: " + newPassword);
        System.out.println("===> Confirmación de contraseña ingresada: " + confirmPassword);

        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, usuario.getContrasena())) {
            System.out.println("===> La contraseña actual no coincide con la almacenada.");
            redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta.");
            return "redirect:/vecino/seguridad";
        }

        // Verificar que nueva contraseña y confirmación coincidan
        if (!newPassword.equals(confirmPassword)) {
            System.out.println("===> Nueva contraseña y confirmación NO coinciden.");
            redirectAttributes.addFlashAttribute("error", "La nueva contraseña y su confirmación no coinciden.");
            return "redirect:/vecino/seguridad";
        }

        // Encriptar y guardar nueva contraseña
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        usuario.setContrasena(encodedNewPassword);

        usuariosRepository.save(usuario);
        session.setAttribute("usuario", usuario);

        System.out.println("===> Contraseña actualizada correctamente para: " + usuario.getCorreo());

        redirectAttributes.addFlashAttribute("success", "¡Contraseña actualizada exitosamente!");
        return "redirect:/vecino/seguridad";
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

    /**
     *  Solicitar cancelación
     */
    @PostMapping("/solicitarCancelacion")
    public String solicitarCancelacion(@RequestParam int idReserva,
                                       @RequestParam String motivo,
                                       @RequestParam(required = false) String codigoPago,
                                       HttpSession session,
                                       RedirectAttributes redirectAttrs) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        Reserva reserva = reservaRepositoryVecino.findById(idReserva).orElse(null);

        if (reserva == null || !reserva.getVecino().getIdUsuarios().equals(usuario.getIdUsuarios())) {
            redirectAttrs.addFlashAttribute("msg", "Reserva inválida o no autorizada.");
            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
        }

        // Validar usando el servicio
        ReservaCancelacionService.ResultadoValidacion validacion =
                cancelacionService.validarSolicitudCancelacion(reserva, usuario);

        if (!validacion.isValido()) {
            redirectAttrs.addFlashAttribute("msg", validacion.getMensaje());
            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
        }

        try {
            // Cambiar estado de la reserva
            EstadoReserva estadoSolicitud = estadoReservaRepositoryVecino.findById(6).orElse(null);
            if (estadoSolicitud == null) {
                redirectAttrs.addFlashAttribute("msg", "Error del sistema. Intenta nuevamente.");
                return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
            }

            reserva.setEstado(estadoSolicitud);
            reserva.setEstadoReembolso(Reserva.EstadoReembolso.PENDIENTE);
            reservaRepositoryVecino.save(reserva);

            // Actualizar pago si existe
            Pago pagoReserva = pagoRepository.findByReserva_IdReserva(reserva.getIdReserva());
            if (pagoReserva != null) {
                pagoReserva.setEstado("REEMBOLSO_SOLICITADO");
                pagoRepository.save(pagoReserva);
            }

            // Crear solicitud
            SolicitudCancelacion solicitud = new SolicitudCancelacion();
            solicitud.setReserva(reserva);
            solicitud.setMotivo(motivo);
            solicitud.setCodigoPago(codigoPago);
            solicitud.setFechaSolicitud(LocalDateTime.now(ZoneId.of("America/Lima")));
            solicitudCancelacionRepository.save(solicitud);

            // Enviar notificación
            try {
                mailManager.enviarNotificacionSolicitudReembolso(solicitud);
            } catch (Exception e) {
                System.err.println("Error al enviar notificación: " + e.getMessage());
            }

            String coordinadorNombre = reserva.getCoordinador() != null ?
                    reserva.getCoordinador().getNombres() : "coordinador";

            redirectAttrs.addFlashAttribute("msg",
                    "Tu solicitud de cancelación ha sido enviada al " + coordinadorNombre +
                            ". Recibirás una respuesta pronto.");

            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";

        } catch (Exception e) {
            System.err.println("Error en solicitud de cancelación: " + e.getMessage());
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("msg", "Error al procesar la solicitud: " + e.getMessage());
            return "redirect:https://serviciosdeportivos-sanmiguel.lat/vecino/mis-reservas";
        }
    }

    // Métodos auxiliares para diferentes tipos de cancelación
    private void procesarCancelacionSinReembolso(Reserva reserva, Usuarios usuario,
                                                 RedirectAttributes redirectAttrs) {
        EstadoReserva estadoCancelada = estadoReservaRepositoryVecino.findById(4).orElse(null);
        reserva.setEstado(estadoCancelada);
        reservaRepositoryVecino.save(reserva);

        redirectAttrs.addFlashAttribute("msg",
                "Reserva cancelada exitosamente. No aplica reembolso ya que no estaba confirmada.");

        // Enviar notificación
        try {
            mailManager.enviarNotificacionReserva(
                    usuario,
                    "❌ Reserva cancelada - Municipalidad de San Miguel",
                    "Su reserva ha sido cancelada",
                    "cancelada (no confirmada)",
                    reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar(),
                    formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin()),
                    "Sin reembolso - No estaba confirmada",
                    reserva,
                    false
            );
        } catch (Exception e) {
            System.err.println("Error enviando notificación: " + e.getMessage());
        }
    }

    private void procesarCancelacionConReembolsoAutomatico(Reserva reserva, Usuarios usuario,
                                                           RedirectAttributes redirectAttrs) {
        boolean reembolsoExitoso = procesarReembolsoAutomaticoMercadoPago(reserva, usuario, redirectAttrs);

        if (reembolsoExitoso) {
            EstadoReserva estadoReembolso = estadoReservaRepositoryVecino.findById(5).orElse(null);
            reserva.setEstado(estadoReembolso);

            redirectAttrs.addFlashAttribute("msg",
                    "Reserva cancelada exitosamente. El reembolso será procesado automáticamente por MercadoPago.");
        } else {
            EstadoReserva estadoCancelada = estadoReservaRepositoryVecino.findById(4).orElse(null);
            reserva.setEstado(estadoCancelada);

            redirectAttrs.addFlashAttribute("msg",
                    "Reserva cancelada pero hubo un error con el reembolso automático. Contacta al soporte.");
        }

        reservaRepositoryVecino.save(reserva);
    }

    private void procesarCancelacionConReembolsoManual(Reserva reserva, Usuarios usuario,
                                                       RedirectAttributes redirectAttrs) {
        EstadoReserva estadoReembolso = estadoReservaRepositoryVecino.findById(5).orElse(null);
        reserva.setEstado(estadoReembolso);
        reserva.setEstadoReembolso(Reserva.EstadoReembolso.APROBADO);
        reservaRepositoryVecino.save(reserva);

        // Crear solicitud de reembolso manual
        Map<String, Object> reservaData = new HashMap<>();
        reservaData.put("idReserva", reserva.getIdReserva());
        reservaData.put("costo", reserva.getCosto());
        reservaData.put("nombreEspacio", reserva.getEspacio().getNombre());

        crearSolicitudReembolsoManual(null, reservaData,
                "Cancelación con más de 24 horas de anticipación");

        redirectAttrs.addFlashAttribute("msg",
                "Reserva cancelada exitosamente. El reembolso será procesado manualmente en 3-5 días hábiles.");
    }
}
