package com.example.project.controller;

import com.example.project.dto.ReservaCalendarioDto;
import com.example.project.dto.TendenciaReservaDTO;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.admin.*;
import com.example.project.repository.coordinador.ActividadRepository;
import com.example.project.service.ExportarServiceGeneral;
import com.example.project.service.MantenimientoService;
import com.example.project.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.example.project.entity.Usuarios;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private EstadoUsuRepository estadoUsuRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private ReservaRepositoryAdmin reservaRepositoryAdmin;
    @Autowired private ServiciosDeportivosDisponiblesRepository serviciosRepositoryDisponible;
    @Autowired private EspacioRepositoryAdmin espacioRepositoryAdmin;
    @Autowired private EspacioRepositoryGeneral espacioRepositoryGeneral;
    @Autowired private EstadoEspacioRepositoryAdmin estadoEspacioRepositoryAdmin;
    @Autowired private LugarRepositoryAdmin lugarRepositoryAdmin;
    @Autowired private DeporteRepository deporteRepository;
    @Autowired private TipoEspacioRepository tipoEspacioRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private S3Service s3Service;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private MantenimientoRepository mantenimientoRepository;

    @Value("${apisnet.token}")
    private String apisNetToken;

    private static final long MIN_IMAGE_SIZE = 300 * 1024; // 300 KB
    private static final long MAX_IMAGE_SIZE = 1024 * 1024; // 1 MB

    private boolean isImageSizeValid(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        long size = file.getSize();
        return size >= MIN_IMAGE_SIZE && size <= MAX_IMAGE_SIZE;
    }

    // Tabla para mostrar todas las reservas que existen
    @GetMapping("/reservas-deportivas")
    public String mostrarReservas(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        return "admin/reservas_deportivas";
    }

    // Mostrar servicios deportivos disponibles
    @GetMapping("/servicios_disponible")
    public String mostrarServiciosActivos(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        List<ServiciosDeportivosDisponiblesDto> servicios = serviciosRepositoryDisponible.listarServiciosActivos();
        model.addAttribute("servicios", servicios);
        return "admin/servicios_disponible";
    }

    // Mostrar datos del administrador
    @GetMapping("/mi-perfil")
    public String mostrarMiCuentaAdmin(Model model, HttpSession session) {

        Usuarios admin = (Usuarios) session.getAttribute("usuario"); // Para la vista
        Usuarios usuario = (Usuarios) session.getAttribute("usuario"); // Para la navbar
        model.addAttribute("admin", admin);
        model.addAttribute("usuario", usuario);


        List<Actividad> actividades = actividadRepository.findByUsuarioOrderByFechaDesc(usuario);
        model.addAttribute("actividades", actividades);

        // 5 actividades más recientes:
        List<Actividad> recientes = actividadRepository
                .findByUsuarioOrderByFechaDesc(usuario)
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("actividades", recientes);

        return "admin/mi_cuenta";
    }

    @GetMapping("/mantenimiento-espacios")
    public String mostrarCalendarioMantenimiento(
            @RequestParam(required = false) Integer idEspacio,
            HttpSession session,
            Model model) {

        try {
            // Verificar usuario autenticado
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            if (admin == null) {
                return "redirect:/login";
            }

            // Obtener todos los espacios disponibles
            List<Espacio> espacios = espacioRepositoryGeneral.findAllByEstadoWithDeportesAndTipoEspacio(1);
            model.addAttribute("espacios", espacios);

            // Seleccionar espacio por defecto o el especificado
            Espacio espacioSeleccionado = null;
            if (idEspacio != null) {
                espacioSeleccionado = espacios.stream()
                        .filter(e -> e.getIdEspacio().equals(idEspacio))
                        .findFirst()
                        .orElse(null);
            }

            if (espacioSeleccionado == null && !espacios.isEmpty()) {
                espacioSeleccionado = espacios.get(0);
            }

            model.addAttribute("espacioSeleccionado", espacioSeleccionado);

            // Obtener reservas del espacio seleccionado
            List<ReservaCalendarioDto> reservas = new ArrayList<>();
            if (espacioSeleccionado != null) {
                LocalDate fechaInicio = LocalDate.now().minusMonths(1);
                LocalDate fechaFin = LocalDate.now().plusMonths(2);

                reservas = reservaRepository.buscarReservasParaCalendario(
                        espacioSeleccionado.getIdEspacio().longValue(),
                        Arrays.asList("Confirmada", "Pendiente de confirmación", "Finalizada", "Cancelada", "Cancelada con reembolso", "Reembolso solicitado", "Cancelada sin reembolso"),
                        fechaInicio,
                        fechaFin
                );
            }
            model.addAttribute("reservas", reservas);

            // Obtener mantenimientos del espacio seleccionado
            List<Mantenimiento> mantenimientos = new ArrayList<>();
            if (espacioSeleccionado != null) {
                LocalDate fechaInicio = LocalDate.now().minusMonths(1);
                LocalDate fechaFin = LocalDate.now().plusMonths(2);

                mantenimientos = mantenimientoRepository.findByEspacioAndFechaBetween(
                        espacioSeleccionado, fechaInicio, fechaFin
                );
            }
            model.addAttribute("mantenimientos", mantenimientos);

            // Obtener coordinadores para asignar responsables
            List<Usuarios> coordinadores = usuariosRepository.findByRol_Rol("Coordinador");
            model.addAttribute("coordinadores", coordinadores);

            // Información adicional para el admin
            model.addAttribute("admin", admin);

            System.out.println("✅ Calendario de mantenimiento cargado:");
            System.out.println("  - Espacios: " + espacios.size());
            System.out.println("  - Espacio seleccionado: " + (espacioSeleccionado != null ? espacioSeleccionado.getNombre() : "Ninguno"));
            System.out.println("  - Reservas: " + reservas.size());
            System.out.println("  - Mantenimientos: " + mantenimientos.size());
            System.out.println("  - Coordinadores: " + coordinadores.size());

            return "admin/mantenimiento-espacios-calendario";

        } catch (Exception e) {
            System.err.println("❌ Error cargando calendario de mantenimiento: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error cargando el calendario de mantenimiento");
            return "error/custom-error";
        }
    }

    @GetMapping("/administrar-roles")
    public String mostrarAdministrarRoles(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        long totalAdmins = usuariosRepository.countByRol_Rol("Administrador");
        long totalCoordinadores = usuariosRepository.countByRol_Rol("Coordinador");
        long totalUsuariosFinales = usuariosRepository.countByRol_Rol("Usuario final");
        List<Usuarios> usuarios = usuariosRepository.findAll();
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("totalCoordinadores", totalCoordinadores);
        model.addAttribute("totalUsuariosFinales", totalUsuariosFinales);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("usuario", usuario);

        // NUEVO: Listas de usuarios por rol para mostrar iniciales en los cuadros
        List<Usuarios> admins = usuariosRepository.findByRolcito("Administrador");
        List<Usuarios> coordinadores = usuariosRepository.findByRolcito("Coordinador");
        List<Usuarios> usuariosFinales = usuariosRepository.findByRolcito("Usuario final");
        model.addAttribute("admins", admins);
        model.addAttribute("coordinadores", coordinadores);
        model.addAttribute("usuariosFinales", usuariosFinales);

        return "admin/admin-administrar-roles";
    }

    @GetMapping("/usuarios-datatable")
    @ResponseBody
    public Map<String, Object> obtenerUsuariosDatatable(HttpServletRequest request) {
        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        String searchValue = request.getParameter("search[value]");

        Pageable pageable = PageRequest.of(start / length, length);
        Page<Usuarios> page;

        if (searchValue != null && !searchValue.isEmpty()) {
            page = usuariosRepository.findByNombreOrCorreoContainingIgnoreCase(searchValue, pageable);
        } else {
            page = usuariosRepository.findAll(pageable);
        }

        List<Map<String, Object>> usuariosData = page.getContent().stream().map(usuario -> {
            Map<String, Object> u = new HashMap<>();
            u.put("idUsuarios", usuario.getIdUsuarios());
            u.put("nombreCompleto", usuario.getNombres() + " " + usuario.getApellidos());
            u.put("correo", usuario.getCorreo());
            u.put("rol", usuario.getRol().getRol());
            u.put("activo", usuario.getEstado().getEstado());
            return u;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        response.put("recordsTotal", page.getTotalElements());
        response.put("recordsFiltered", page.getTotalElements());
        response.put("data", usuariosData);

        return response;
    }

    @GetMapping("/crear-coordinador")
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        model.addAttribute("coordinador", new Usuarios());
        List<Lugar> lugares = lugarRepositoryAdmin.findAll();
        model.addAttribute("lugares", lugares);
        return "admin/crear_coordinador";
    }

    @PostMapping("/crear-coordinador")
    public String registrarNuevoCoordinador(
            @ModelAttribute("coordinador") Usuarios coordinador,
            @RequestParam(value = "lugaresIds", required = false) List<Integer> lugaresIds,
            HttpSession session, Model model) {

        Rol rolCoordinador = rolRepository.findById(2)
                .orElseThrow(() -> new RuntimeException("Rol con ID 2 no encontrado"));

        EstadoUsu estadoActivo = estadoUsuRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Estado con ID 1 no encontrado"));

        coordinador.setRol(rolCoordinador);
        coordinador.setEstado(estadoActivo);

        // Guardar el coordinador primero
        Usuarios coordinadorGuardado = usuariosRepository.save(coordinador);

        // Asignar lugares al coordinador si se seleccionaron
        if (lugaresIds != null && !lugaresIds.isEmpty()) {
            List<Lugar> lugaresAsignados = new ArrayList<>();
            for (Integer lugarId : lugaresIds) {
                Lugar lugar = lugarRepositoryAdmin.findById(lugarId)
                        .orElseThrow(() -> new RuntimeException("Lugar con ID " + lugarId + " no encontrado"));
                lugaresAsignados.add(lugar);
            }
            coordinadorGuardado.setLugaresAsignados(lugaresAsignados);
            usuariosRepository.save(coordinadorGuardado);
        }

        // Validar que se hayan seleccionado lugares
        if (lugaresIds == null || lugaresIds.isEmpty()) {
            model.addAttribute("error", "Debe seleccionar al menos un lugar para el coordinador");
            model.addAttribute("lugares", lugarRepositoryAdmin.findAll());
            return "admin/crear_coordinador";
        }

        // Registrar la actividad
        Actividad actividad = new Actividad();
        actividad.setUsuario(session.getAttribute("usuario") != null ?
                (Usuarios) session.getAttribute("usuario") : null);
        actividad.setDescripcion("Creación de Coordinador");
        actividad.setDetalle("Creó al coordinador \"" + coordinador.getNombres() + "\" a las " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        actividad.setFecha(LocalDateTime.now());
        actividadRepository.save(actividad);

        return "redirect:/admin/lista-coordinadores";
    }

    @GetMapping("/agregar-servicios")
    public String mostrarFormularioRegistroservicio(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        model.addAttribute("espacio", new Espacio());
        model.addAttribute("lugares", lugarRepositoryAdmin.findAll());
        model.addAttribute("estados", estadoEspacioRepositoryAdmin.findAll());
        model.addAttribute("tiposEspacio", tipoEspacioRepository.findAll());
        model.addAttribute("deportes", deporteRepository.findAll());
        return "admin/agregar_servicios";
    }

    @Value("${aws.bucket}")
    private String bucketName;
    @Value("${aws.region}")
    private String regionName;

    @PostMapping(value = "/agregar-servicios", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> guardarEspacio(
            @RequestParam String nombre,
            @RequestParam("lugar") int idLugar,
            @RequestParam("estado") int idEstado,
            @RequestParam("tipoEspacio") int idTipoEspacio,
            @RequestParam double costo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(value = "deportes", required = false) List<Integer> deportesIds,
            @RequestParam(value = "file", required = false) MultipartFile[] archivos,
            HttpSession session
    ) {
        try {
            System.out.println("📥 Iniciando creación de nuevo espacio...");
            Espacio espacio = new Espacio();
            espacio.setNombre(nombre);
            espacio.setCosto(costo);
            espacio.setDescripcion(descripcion);
            espacio.setIdLugar(lugarRepositoryAdmin.findById(idLugar).orElse(null));
            espacio.setIdEstadoEspacio(estadoEspacioRepositoryAdmin.findById(idEstado).orElse(null));
            espacio.setTipoEspacio(tipoEspacioRepository.findById(idTipoEspacio).orElse(null));
            espacio = espacioRepositoryAdmin.save(espacio);
            System.out.println("✅ Espacio guardado con ID: " + espacio.getIdEspacio());

            if (deportesIds != null) {
                List<Deporte> deportes = deporteRepository.findAllById(deportesIds);
                espacio.setDeportes(deportes);
            }

            // Simplificación: usar directamente el nuevo método para obtener URLs completas
            if (archivos != null && archivos.length > 0) {
                for (int i = 0; i < archivos.length && i < 3; i++) {
                    MultipartFile archivo = archivos[i];
                    if (!archivo.isEmpty()) {
                        try {
                            // El método devuelve directamente la URL completa
                            String urlCompleta = s3Service.subirArchivo(archivo, "publica/espacios/" + espacio.getIdEspacio());

                            // Almacenar la URL completa en el campo correspondiente
                            if (i == 0) espacio.setFoto1Url(urlCompleta);
                            if (i == 1) espacio.setFoto2Url(urlCompleta);
                            if (i == 2) espacio.setFoto3Url(urlCompleta);

                            System.out.println("✅ Archivo " + (i+1) + " subido: " + urlCompleta);
                        } catch (IOException e) {
                            System.out.println("❌ Error al subir archivo " + (i+1) + ": " + e.getMessage());
                        }
                    }
                }
            }

            espacioRepositoryAdmin.save(espacio);

            Actividad actividad = new Actividad();
            actividad.setUsuario(session.getAttribute("usuario") != null ? (Usuarios) session.getAttribute("usuario") : null);
            actividad.setDescripcion("Creación de espacio");
            actividad.setDetalle("Creó el espacio \"" + espacio.getNombre() + "\" a las " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            actividad.setFecha(LocalDateTime.now());
            actividadRepository.save(actividad);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Espacio creado correctamente",
                    "espacioId", espacio.getIdEspacio()
            ));

        } catch (Exception e) {
            System.out.println("❌ ERROR GENERAL en guardarEspacio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Ocurrió un error: " + e.getMessage()));
        }
    }

    @GetMapping("/espacios-deportivos")
    public String listarEspacios(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        List<EspacioDto> espacios = espacioRepositoryAdmin.findAllEspacioDtos();
        List<String> estados = List.of("Disponible", "Mantenimiento");

        model.addAttribute("espacios", espacios);
        model.addAttribute("estados", estados);

        return "admin/espacios_deportivos";
    }

    @PostMapping("/actualizar_estado")
    public String actualizarEstado(@RequestParam Integer idEspacio,
                                   @RequestParam String nuevoEstado,
                                   HttpSession session) {

        // Obtener el espacio antes de actualizarlo para tener acceso al nombre
        Espacio espacio = espacioRepositoryAdmin.findById(idEspacio).orElse(null);

        // Actualizar el estado
        espacioRepositoryAdmin.actualizarEstado(idEspacio, nuevoEstado);

        // Registrar la actividad
        Actividad actividad = new Actividad();
        actividad.setUsuario(session.getAttribute("usuario") != null ?
                (Usuarios) session.getAttribute("usuario") : null);
        actividad.setDescripcion("Cambio de estado");
        actividad.setDetalle("Cambió el estado de \"" + espacio.getNombre() + "\" a " + nuevoEstado);
        actividad.setFecha(LocalDateTime.now());
        actividadRepository.save(actividad);

        return "redirect:/admin/espacios-deportivos";
    }


    @Autowired
    private Cant_espacioRepository cantEspacioRepository;
    @Autowired
    private TendenciaReservaRepository tendenciaReservaRepository;

    @GetMapping("/dashboard-servicios")
    public String listarEspaciosPorTipo(Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        // Obtener resumen de reservas
        List<ReservaDto> resumen = reservaRepositoryAdmin.obtenerResumenReservas();

        // Mapeo de idTipoEspacio a nombre
        Map<Integer, String> tipoEspacioNombres = Map.of(
                1, "Cancha de fútbol - Grass Sintético",
                2, "Cancha de fútbol - Loza",
                3, "Piscina",
                4, "Pista de Atletismo"
        );

        // Inicializar resumenMap solo con los tipos válidos
        Map<String, ReservaDto> resumenMap = new LinkedHashMap<>();
        for (String nombre : tipoEspacioNombres.values()) {
            resumenMap.put(nombre, new ReservaDto() {
                @Override public String getTipo() { return nombre; }
                @Override public Integer getTotalReservas() { return 0; }
                @Override public Integer getTotalEspacios() { return 0; }
                @Override public Double getPorcentajeUso() { return 0.0; }
            });
        }
        for (ReservaDto dto : resumen) {
            if (resumenMap.containsKey(dto.getTipo())) {
                resumenMap.put(dto.getTipo(), dto);
            }
        }
        model.addAttribute("resumenMap", resumenMap);

        // Obtener tendencias de reservas con query nativo y mapear a DTO
        List<Object[]> tendenciasRaw = tendenciaReservaRepository.obtenerTendenciaReservasRaw();
        List<TendenciaReservaDTO> tendencias = tendenciasRaw.stream().map(obj -> new TendenciaReservaDTO(
                obj[0] != null ? ((Number) obj[0]).intValue() : null,
                obj[1] != null ? ((Number) obj[1]).intValue() : null,
                obj[2] != null ? ((Number) obj[2]).intValue() : null,
                obj[3] != null ? obj[3].toString() : null,
                obj[4] != null ? ((Number) obj[4]).longValue() : 0L
        )).toList();
        model.addAttribute("tendencias", tendencias);

        // Contar espacios por idTipoEspacio
        List<Object[]> resultados = cantEspacioRepository.contarPorIdTipoEspacio();
        Map<String, Integer> cantidadesPorTipo = new LinkedHashMap<>();
        for (Integer id : tipoEspacioNombres.keySet()) {
            cantidadesPorTipo.put(tipoEspacioNombres.get(id), 0);
        }
        for (Object[] fila : resultados) {
            Integer idTipo = ((Number) fila[0]).intValue();
            Long cantidad = ((Number) fila[1]).longValue();
            String nombre = tipoEspacioNombres.get(idTipo);
            if (nombre != null) {
                cantidadesPorTipo.put(nombre, cantidad.intValue());
            }
        }
        model.addAttribute("cantidadesPorTipo", cantidadesPorTipo);

        // Calcular el total de reservas por tipo de espacio usando tendencias
        Map<String, Long> totalReservasPorTipo = new LinkedHashMap<>();
        for (String nombre : tipoEspacioNombres.values()) {
            totalReservasPorTipo.put(nombre, 0L);
        }
        for (TendenciaReservaDTO t : tendencias) {
            if (totalReservasPorTipo.containsKey(t.getTipoEspacio())) {
                totalReservasPorTipo.put(t.getTipoEspacio(),
                        totalReservasPorTipo.get(t.getTipoEspacio()) + t.getTotalReservas());
            }
        }
        model.addAttribute("totalReservasPorTipo", totalReservasPorTipo);

        return "admin/dashboard_servicios";
    }

    @GetMapping("/detalles-espacio")
    public String detalles(@RequestParam("id") int id, Model model, HttpSession session, HttpServletRequest request) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        Espacio espacio = espacioRepositoryGeneral.findById(id).orElse(null);

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

            return "admin/detalles";
        } else {
            return "redirect:/vecino/home";
        }
    }

    @GetMapping("/editar-espacio/{id}")
    public String mostrarEditarEspacio(@PathVariable("id") Integer id, Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        Optional<Espacio> optEspacio = espacioRepositoryAdmin.findById(id);

        if (optEspacio.isPresent()) {
            Espacio espacio = optEspacio.get();

            model.addAttribute("espacio", espacio);
            model.addAttribute("lugares", lugarRepositoryAdmin.findAll());
            model.addAttribute("estados", estadoEspacioRepositoryAdmin.findAll());
            model.addAttribute("deportes", deporteRepository.findAll());
            model.addAttribute("tiposEspacio", tipoEspacioRepository.findAll());

            return "admin/editar_espacio";
        }

        return "redirect:/admin/espacios-deportivos";
    }

    @PostMapping(value = "/editar-espacio/{id}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> actualizarEspacio(
            @PathVariable("id") Integer id,
            @ModelAttribute Espacio espacioForm,
            @RequestParam(value = "deleteFile1", required = false) String deleteFile1,
            @RequestParam(value = "deleteFile2", required = false) String deleteFile2,
            @RequestParam(value = "deleteFile3", required = false) String deleteFile3,
            @RequestParam(value = "newFiles", required = false) MultipartFile[] newFiles,
            @RequestParam(value = "deportes", required = false) List<Integer> deportesIds,
            HttpSession session) {

        System.out.println("✏️ Iniciando edición del espacio ID: " + id);

        Optional<Espacio> optEspacio = espacioRepositoryAdmin.findById(id);
        if (optEspacio.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Espacio no encontrado."));
        }

        try {
            Espacio espacio = optEspacio.get();

            // Actualizar campos básicos
            espacio.setNombre(espacioForm.getNombre());
            espacio.setCosto(espacioForm.getCosto());
            espacio.setIdLugar(espacioForm.getIdLugar());
            espacio.setIdEstadoEspacio(espacioForm.getIdEstadoEspacio());
            espacio.setTipoEspacio(espacioForm.getTipoEspacio());
            espacio.setDescripcion(espacioForm.getDescripcion());

            // Actualizar deportes
            espacio.getDeportes().clear();
            if (deportesIds != null && !deportesIds.isEmpty()) {
                List<Deporte> deportes = deporteRepository.findAllById(deportesIds);
                espacio.setDeportes(deportes);
                System.out.println("Deportes actualizados: " + deportes.size());
            }

            // Gestión de eliminación de fotos existentes
            if ("true".equals(deleteFile1)) {
                System.out.println("🗑️ Eliminando foto 1");
                espacio.setFoto1Url(null);
            }
            if ("true".equals(deleteFile2)) {
                System.out.println("🗑️ Eliminando foto 2");
                espacio.setFoto2Url(null);
            }
            if ("true".equals(deleteFile3)) {
                System.out.println("🗑️ Eliminando foto 3");
                espacio.setFoto3Url(null);
            }

            // Procesar nuevas fotos usando el S3Service mejorado
            if (newFiles != null && newFiles.length > 0) {
                System.out.println("📸 Procesando " + newFiles.length + " nuevas fotos");

                for (MultipartFile archivo : newFiles) {
                    if (!archivo.isEmpty()) {
                        try {
                            System.out.println("📤 Subiendo nueva imagen: " + archivo.getOriginalFilename());

                            // Usar el método mejorado que retorna la URL completa y correcta
                            String urlCompleta = s3Service.subirArchivo(archivo, "publica/espacios/" + id);
                            System.out.println("✅ Foto subida: " + urlCompleta);

                            // Asignar URL a la primera posición disponible
                            if (espacio.getFoto1Url() == null) {
                                espacio.setFoto1Url(urlCompleta);
                                System.out.println("📷 Asignada como foto 1");
                            }
                            else if (espacio.getFoto2Url() == null) {
                                espacio.setFoto2Url(urlCompleta);
                                System.out.println("📷 Asignada como foto 2");
                            }
                            else if (espacio.getFoto3Url() == null) {
                                espacio.setFoto3Url(urlCompleta);
                                System.out.println("📷 Asignada como foto 3");
                            }
                            // Si todas las posiciones están ocupadas, ignorar el archivo
                            else {
                                System.out.println("⚠️ Ya hay 3 fotos, ignorando archivo adicional");
                            }
                        } catch (IOException e) {
                            System.err.println("❌ Error al subir archivo: " + e.getMessage());
                            // Continuar con los demás archivos si uno falla
                        }
                    }
                }
            }

            // Guardar los cambios
            espacio = espacioRepositoryAdmin.save(espacio);
            System.out.println("✅ Espacio actualizado exitosamente");

            // Registrar la actividad
            Actividad actividad = new Actividad();
            actividad.setDescripcion("Edición de espacio");
            actividad.setDetalle("Se editó el espacio: " + espacio.getNombre() + " a las " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            actividad.setFecha(LocalDateTime.now());
            actividad.setUsuario((Usuarios) session.getAttribute("usuario"));
            actividadRepository.save(actividad);

            System.out.println("Edición completada para espacio ID: " + espacio.getIdEspacio());

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Espacio actualizado exitosamente",
                    "espacioId", espacio.getIdEspacio(),  // ← Incluir el ID del espacio
                    "espacioNombre", espacio.getNombre()  // ← Información adicional
            ));

        } catch (Exception e) {
            System.err.println("❌ Error al actualizar espacio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al actualizar espacio: " + e.getMessage()));
        }
    }

    @Autowired
    private DetallesReservaRepository detallesReservaRepository;

    // Mostrar detalle de reserva por id
    @GetMapping("/detalles-reserva/{id}")
    public String verDetalleReserva(@PathVariable("id") Long id, Model model, HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        Optional<DetallesReservaDto> reservaOpt = detallesReservaRepository.obtenerDetallesReservas()
                .stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();

        if (reservaOpt.isEmpty()) {
            // Manejar error: reserva no encontrada
            return "redirect:/admin/detalles-reserva?error=notfound";
        }
        model.addAttribute("reserva", reservaOpt.get());

        Espacio espacio = espacioRepositoryAdmin.findById(reservaOpt.get().getIdespacio())
                .orElseThrow(() -> new IllegalArgumentException("ID de espacio no válido: " + id));
        model.addAttribute("espacio", espacio);
        return "admin/detalles_reservas"; // vista para el detalle individual
    }

    @GetMapping("/espacios-deportivos-datatable")
    @ResponseBody
    public Map<String, Object> obtenerEspaciosDatatable(HttpServletRequest request) {
        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        String searchValue = request.getParameter("search[value]");

        int page = start / length;

        Pageable pageable = PageRequest.of(page, length, Sort.by("nombre").ascending());

        Page<Espacio> espacios;

        if (searchValue != null && !searchValue.isEmpty()) {
            espacios = espacioRepositoryAdmin.buscarEspaciosConTodo(searchValue, pageable);
        } else {
            espacios = espacioRepositoryAdmin.findAll(pageable);
        }

        List<Map<String, Object>> data = espacios.getContent().stream().map(espacio -> {
            Map<String, Object> row = new HashMap<>();
            row.put("idEspacio", espacio.getIdEspacio());
            row.put("nombre", espacio.getNombre());
            row.put("nombreTipo", espacio.getTipoEspacio().getNombre());
            row.put("nombreLugar", espacio.getIdLugar().getLugar());
            row.put("costo", espacio.getCosto());
            row.put("estadoEspacio", espacio.getIdEstadoEspacio().getEstado());
            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        response.put("recordsTotal", espacios.getTotalElements());
        response.put("recordsFiltered", espacios.getTotalElements());
        response.put("data", data);

        return response;
    }

    @GetMapping("/usuarios/editar/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerUsuario(@PathVariable Integer id) {
        Optional<Usuarios> usuario = usuariosRepository.findById(id);
        if (usuario.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", usuario.get().getIdUsuarios());
            response.put("rol", usuario.get().getRol().getIdRol());
            response.put("nombreCompleto", usuario.get().getNombres() + " " + usuario.get().getApellidos());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/usuarios/editar/{id}")
    @ResponseBody
    public ResponseEntity<?> editarRolUsuario(@PathVariable Integer id, @RequestParam Integer rolId) {
        Optional<Usuarios> usuarioOpt = usuariosRepository.findById(id);
        Optional<Rol> rolOpt = rolRepository.findById(rolId);

        if (usuarioOpt.isPresent() && rolOpt.isPresent()) {
            Usuarios usuario = usuarioOpt.get();
            usuario.setRol(rolOpt.get());
            usuariosRepository.save(usuario);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/usuarios/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        if (usuariosRepository.existsById(id)) {
            usuariosRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/consultar-dni-api")
    @ResponseBody
    public ResponseEntity<Map<String, String>> consultarDniCoordinador(@RequestParam String dni, HttpSession session, HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            String url = "https://api.apis.net.pe/v2/reniec/dni?numero=" + dni;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apisNetToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (apiResponse.getStatusCode() == HttpStatus.OK) {
                JSONObject json = new JSONObject(apiResponse.getBody());
                String nombres = json.optString("nombres", "");
                String apellidoPaterno = json.optString("apellidoPaterno", "");
                String apellidoMaterno = json.optString("apellidoMaterno", "");
                String apellidos = (apellidoPaterno + " " + apellidoMaterno).trim();

                if (nombres.isEmpty() || apellidos.isEmpty()) {
                    response.put("status", "error");
                    return ResponseEntity.ok(response);
                }

                response.put("status", "ok");
                response.put("nombres", nombres);
                response.put("apellidos", apellidos);
            } else {
                response.put("status", "error");
            }
        } catch (Exception e) {
            response.put("status", "error");
        }
        return ResponseEntity.ok(response);
    }

    // Endpoint para cambiar credenciales del administrador
    @PostMapping("/cambiar-credenciales")
    public String cambiarCredenciales(
            @RequestParam("contrasenaActual") String contrasenaActual,
            @RequestParam(value = "nuevoCorreo", required = false) String nuevoCorreo,
            @RequestParam(value = "nuevoNumero", required = false) String nuevoNumero,
            @RequestParam(value = "nuevaContrasena", required = false) String nuevaContrasena,
            @RequestParam(value = "confirmarContrasena", required = false) String confirmarContrasena,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Usuarios admin = (Usuarios) session.getAttribute("usuario");
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "No se pudo identificar al usuario.");
            return "redirect:/admin/mi-perfil";
        }

        // Validar contraseña actual
        if (contrasenaActual == null || !passwordEncoder.matches(contrasenaActual, admin.getContrasena())) {
            redirectAttributes.addFlashAttribute("error", "Contraseña actual incorrecta.");
            return "redirect:/admin/mi-perfil";
        }

        // Validación de teléfono duplicado
        if (nuevoNumero != null && !nuevoNumero.isBlank()) {
            Optional<Usuarios> usuarioConTelefono = usuariosRepository.findByTelefono(nuevoNumero.trim());
            if (usuarioConTelefono.isPresent() && !usuarioConTelefono.get().getIdUsuarios().equals(admin.getIdUsuarios())) {
                redirectAttributes.addFlashAttribute("error", "El número ingresado ya está registrado en otro usuario.");
                return "redirect:/admin/mi-perfil";
            }
            admin.setTelefono(nuevoNumero.trim());
        }

        if (nuevoCorreo != null && !nuevoCorreo.isBlank()) {
            admin.setCorreo(nuevoCorreo.trim());
        }
        if (nuevaContrasena != null && !nuevaContrasena.isBlank() && nuevaContrasena.equals(confirmarContrasena)) {
            admin.setContrasena(passwordEncoder.encode(nuevaContrasena));
        }
        usuariosRepository.save(admin);
        redirectAttributes.addFlashAttribute("success", "Credenciales actualizadas correctamente.");
        return "redirect:/admin/mi-perfil";
    }

    // Endpoint para validar la contraseña actual vía AJAX
    @PostMapping("/validar-contrasena")
    @ResponseBody
    public Map<String, Boolean> validarContrasena(@RequestParam("contrasenaActual") String contrasenaActual,
                                                  HttpSession session) {
        Map<String, Boolean> response = new HashMap<>();
        try {
            Usuarios admin = (Usuarios) session.getAttribute("usuario");
            boolean isValid = admin != null &&
                    passwordEncoder.matches(contrasenaActual, admin.getContrasena());
            response.put("valida", isValid);
        } catch (Exception e) {
            response.put("valida", false);
        }
        return response;
    }

    // Endpoint para validar correo, teléfono o DNI por AJAX
    @PostMapping("/validar-campo")
    @ResponseBody
    public Map<String, Boolean> validarCampo(
            @RequestParam("tipo") String tipo,
            @RequestParam("valor") String valor,
            @RequestParam(value = "idUsuario", required = false) String idUsuarioStr
    ) {
        boolean existe = false;
        Integer idUsuario = null;
        try {
            idUsuario = idUsuarioStr != null && !idUsuarioStr.isBlank() ? Integer.parseInt(idUsuarioStr) : null;
        } catch (Exception ignored) {}

        switch (tipo) {
            case "correo" -> {
                Optional<Usuarios> usuario = usuariosRepository.findByCorreoIgnoreCase(valor);
                existe = usuario.isPresent() && (idUsuario == null || !usuario.get().getIdUsuarios().equals(idUsuario));
            }
            case "telefono" -> {
                Optional<Usuarios> usuario = usuariosRepository.findByTelefono(valor);
                existe = usuario.isPresent() && (idUsuario == null || !usuario.get().getIdUsuarios().equals(idUsuario));
            }
            case "dni" -> {
                Optional<Usuarios> usuario = usuariosRepository.findByDni(valor);
                existe = usuario.isPresent() && (idUsuario == null || !usuario.get().getIdUsuarios().equals(idUsuario));
            }
        }
        return Map.of("existe", existe);
    }

    @Autowired
    private UserDetailsService userDetailsService;

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
}
