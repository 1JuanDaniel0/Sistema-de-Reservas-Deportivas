package com.example.project.controller;
import com.example.project.entity.EstadoUsu;
import com.example.project.entity.Rol;
import com.example.project.entity.Usuarios;
import com.example.project.repository.RolRepository;
import com.example.project.repository.superadmin.ReservasRepository;
import com.example.project.repository.superadmin.SuperAdminRepository;
import com.example.project.repository.UsuariosRepository; // <--- Usa el mismo que en WebSecurityConfig
import com.example.project.repository.PagoRepository;
import com.example.project.entity.Pago;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    SuperAdminRepository superAdminRepository;

    @Autowired
    ReservasRepository reservasRepository;

    @Autowired
    UsuariosRepository usuariosRepository;
    @Autowired
    RolRepository rolRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping(value = {"", "/home"})
    public String home(Model model, HttpSession session) {
        System.out.println("--- DEBUG: Accediendo a /superadmin/home ---"); // DEBUG

        model.addAttribute("cantidadActivos", superAdminRepository.cantidadDeEspaciosPorIdEstado(1));
        model.addAttribute("cantidadEnMantenimiento", superAdminRepository.cantidadDeEspaciosPorIdEstado(2));
        model.addAttribute("cantidadPrestados", superAdminRepository.cantidadDeEspaciosPorIdEstado(3));
        model.addAttribute("cantidadCerrados", superAdminRepository.cantidadDeEspaciosPorIdEstado(4));
        model.addAttribute("cantidadTotal", superAdminRepository.count());
        Rol rol = rolRepository.findById(4).get();
        model.addAttribute("listaUsuarios", superAdminRepository.findByRolNot(rol));
        // Solo reservas del último año
        LocalDate haceUnAno = LocalDate.now().minusYears(1);
        model.addAttribute("listaReservas", reservasRepository.findByFechaAfterOrderByIdReservaDesc(haceUnAno));
        model.addAttribute("totalReservas", reservasRepository.count());

        // --- NUEVO: Datos para Balance Total y Actividad de Pagos desde la tabla Pago ---

        // Ingreso total en línea y en banco
        BigDecimal ingresoTotalEnLinea = pagoRepository.sumaPagosPorTipoYEstado("En Línea", "PAGADO");
        BigDecimal ingresoTotalEnBanco = pagoRepository.sumaPagosPorTipoYEstado("En Banco", "PAGADO");
        BigDecimal ingresoTotal = pagoRepository.sumaPagosPorEstado("PAGADO");

        model.addAttribute("ingresoTotal", ingresoTotal != null ? ingresoTotal : BigDecimal.ZERO);
        model.addAttribute("ingresoTotalEnLinea", ingresoTotalEnLinea != null ? ingresoTotalEnLinea : BigDecimal.ZERO);
        model.addAttribute("ingresoTotalEnBanco", ingresoTotalEnBanco != null ? ingresoTotalEnBanco : BigDecimal.ZERO);

        // Serie de ingresos por mes para el gráfico de Balance Total
        List<String> listaMeses = new ArrayList<>();
        List<BigDecimal> listCantidadesMes = new ArrayList<>();
        YearMonth ahora = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = ahora.minusMonths(i);
            BigDecimal montoMes = pagoRepository.sumaPagosPorMes(ym.getYear(), ym.getMonthValue(), "PAGADO");
            listCantidadesMes.add(montoMes != null ? montoMes : BigDecimal.ZERO);
            listaMeses.add(ym.getMonth().toString().substring(0, 3));
        }
        model.addAttribute("meses", listaMeses);
        model.addAttribute("cantidadesMes", listCantidadesMes);

        // Serie de ingresos diarios para el gráfico de Actividad de Pagos (últimos 7 días)
        List<String> listaDias = new ArrayList<>();
        List<BigDecimal> listaCantidadesBanco = new ArrayList<>();
        List<BigDecimal> listaCantidadesEnLinea = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            BigDecimal montoBanco = pagoRepository.sumaPagosPorDiaYTipo(dia, "En Banco", "PAGADO");
            BigDecimal montoEnLinea = pagoRepository.sumaPagosPorDiaYTipo(dia, "En Línea", "PAGADO");
            listaCantidadesBanco.add(montoBanco != null ? montoBanco : BigDecimal.ZERO);
            listaCantidadesEnLinea.add(montoEnLinea != null ? montoEnLinea : BigDecimal.ZERO);
            listaDias.add(dia.getDayOfWeek().toString().substring(0, 3));
        }
        model.addAttribute("dias", listaDias);
        model.addAttribute("cantidadesBanco", listaCantidadesBanco);
        model.addAttribute("cantidadesEnLinea", listaCantidadesEnLinea);

        // --- Agregar usuario actual al modelo para el formulario de credenciales ---
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("usuario", usuario);
        }

        // Asegúrate de que el archivo sea casa.html
        return "superadmin/casa";
    }

    @GetMapping(value = "/en")
    public String habilita(@RequestParam("id") int id){
        superAdminRepository.actualizarEstadoUsuario(id, 1);
        return "redirect:/superadmin/home";
    }

    @GetMapping(value = "/de")
    public String deshabilita(@RequestParam("id") int id) {
        Optional<Usuarios> usuario = usuariosRepository.findById(id);
        if (usuario.isPresent() && usuario.get().getRol().getIdRol() == 4) {
            // Si el usuario tiene rol SuperAdmin (id=4), redirigir sin deshabilitar
            return "redirect:/superadmin/home";
        }
        // Si no es SuperAdmin, proceder con la deshabilitación
        superAdminRepository.actualizarEstadoUsuario(id, 2);
        return "redirect:/superadmin/home";
    }
    @Autowired
    private UserDetailsService userDetailsService; // <--- Debe ser el mismo bean que en WebSecurityConfig
    // Clave para almacenar el ID y el objeto del SuperAdmin original en la sesión
    private static final String ORIGINAL_SUPERADMIN_ID_SESSION_KEY = "originalSuperAdminId";
    private static final String ORIGINAL_SUPERADMIN_OBJ_SESSION_KEY = "originalSuperAdminObj";

// ... (otros métodos del controlador, como home, habilita, deshabilita) ...

    // Nuevo endpoint para manejar la impersonación
    @PostMapping("/impersonate")

    public String impersonateUser(@RequestParam("userId") Integer userId, HttpSession session, RedirectAttributes redirectAttributes) {
        System.out.println("--- DEBUG: [SuperAdminController] Recibida solicitud de impersonación para userId: " + userId + " ---"); // DEBUG

        // Obtener la autenticación actual del SuperAdmin
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("--- DEBUG: [SuperAdminController] Autenticación actual (SuperAdmin): " + currentAuthentication.getName() + ", Authorities: " + currentAuthentication.getAuthorities() + " ---"); // DEBUG

        // Guardar el ID y el objeto del superadmin original SOLO si aún no se está impersonando
        if (session.getAttribute(ORIGINAL_SUPERADMIN_ID_SESSION_KEY) == null) {
            Optional<Usuarios> originalSuperAdminOpt = Optional.empty();
            String superAdminUsername = currentAuthentication.getName();
            try {
                String dni = superAdminUsername;
                originalSuperAdminOpt = usuariosRepository.findByDni(dni);
            } catch (NumberFormatException e) {
                originalSuperAdminOpt = usuariosRepository.findByCorreo(superAdminUsername);
            }
            if(originalSuperAdminOpt.isPresent()){
                session.setAttribute(ORIGINAL_SUPERADMIN_ID_SESSION_KEY, originalSuperAdminOpt.get().getIdUsuarios());
                session.setAttribute(ORIGINAL_SUPERADMIN_OBJ_SESSION_KEY, originalSuperAdminOpt.get());
                System.out.println("--- DEBUG: [SuperAdminController] ID del SuperAdmin original guardado en sesión: " + originalSuperAdminOpt.get().getIdUsuarios() + " ---"); // DEBUG
            } else {
                System.out.println("--- DEBUG: [SuperAdminController] ERROR: No se pudo encontrar la entidad del SuperAdmin original por username: " + superAdminUsername + " ---"); // DEBUG
                // Manejar error: No se encontró la entidad del usuario SuperAdmin original
                redirectAttributes.addFlashAttribute("error", "Error al iniciar impersonación: No se encontró el usuario SuperAdmin original.");
                return "redirect:/superadmin/home"; // Redirigir de vuelta a la página de inicio del SuperAdmin
            }
        } else {
            System.out.println("--- DEBUG: [SuperAdminController] Ya se está impersonando. No se sobrescribe el ID del SuperAdmin original. ---"); // DEBUG
        }


        // Encontrar la entidad del usuario objetivo por ID
        Optional<Usuarios> targetUserOpt = usuariosRepository.findById(userId);

        if (targetUserOpt.isPresent()) {
            Usuarios targetUser = targetUserOpt.get();
            System.out.println("--- DEBUG: [SuperAdminController] Entidad del usuario objetivo encontrada. ID: " + targetUser.getIdUsuarios() + ", Correo: " + targetUser.getCorreo() + ", Rol: " + (targetUser.getRol() != null ? targetUser.getRol().getRol() : "N/A") + ", Estado ID: " + (targetUser.getEstado() != null ? targetUser.getEstado().getIdEstado() : "N/A") + " ---"); // DEBUG

            // Cargar UserDetails para el usuario objetivo usando el UserDetailsService
            // Usar DNI o Correo como nombre de usuario, dependiendo de cuál esté disponible/se use para login
            UserDetails targetUserDetails = null;
            String targetUsername = null;
            if (targetUser.getDni() != null) {
                targetUsername = String.valueOf(targetUser.getDni());
                System.out.println("--- DEBUG: [SuperAdminController] Intentando cargar UserDetails por DNI: " + targetUsername + " ---"); // DEBUG
            } else if (targetUser.getCorreo() != null) {
                targetUsername = targetUser.getCorreo();
                System.out.println("--- DEBUG: [SuperAdminController] Intentando cargar UserDetails por Correo: " + targetUsername + " ---"); // DEBUG
            }

            if (targetUsername != null) {
                try {
                    targetUserDetails = userDetailsService.loadUserByUsername(targetUsername);
                    System.out.println("--- DEBUG: [SuperAdminController] UserDetails del usuario objetivo cargado. Username: " + targetUserDetails.getUsername() + ", Authorities: " + targetUserDetails.getAuthorities() + ", Enabled: " + targetUserDetails.isEnabled() + " ---"); // DEBUG

                    // Verificar si el usuario objetivo está habilitado
                    if (!targetUserDetails.isEnabled()) {
                        System.out.println("--- DEBUG: [SuperAdminController] Usuario objetivo deshabilitado. No se puede impersonar. ---"); // DEBUG
                        redirectAttributes.addFlashAttribute("error", "No se puede entrar en la sesión de un usuario deshabilitado.");
                        return "redirect:/superadmin/home"; // Redirigir de vuelta a la página de inicio del SuperAdmin
                    }

                    // Crear un nuevo objeto Authentication para el usuario objetivo
                    // Usar las autoridades obtenidas del UserDetails
                    Authentication targetAuthentication = new UsernamePasswordAuthenticationToken(
                            targetUserDetails,
                            targetUserDetails.getPassword(), // La contraseña no se usa para autorización después de establecer el contexto
                            targetUserDetails.getAuthorities() // Usar autoridades de UserDetails (con prefijo ROLE_)
                    );
                    System.out.println("--- DEBUG: [SuperAdminController] Nueva autenticación creada para el usuario objetivo. Authorities: " + targetAuthentication.getAuthorities() + " ---"); // DEBUG


                    // Cambiar el contexto de seguridad y la sesión
                    SecurityContext context = SecurityContextHolder.getContext();
                    context.setAuthentication(targetAuthentication);
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

                    // --- CAMBIO CLAVE: Actualizar el objeto "usuario" en la sesión ---
                    session.setAttribute("usuario", targetUser);

                    // Redirigir a la página de inicio del usuario objetivo según su rol
                    String targetRolName = targetUser.getRol().getRol(); // Obtener el nombre del rol desde la entidad (sin prefijo ROLE_)
                    System.out.println("--- DEBUG: [SuperAdminController] Redirigiendo basado en el rol objetivo (sin prefijo ROLE_): " + targetRolName + " ---"); // DEBUG

                    if (targetRolName.equals("SuperAdmin")) {
                        // No deberías impersonar a otro SuperAdmin con este flujo, pero lo manejamos
                        return "redirect:/superadmin/home";
                    } else if (targetRolName.equals("Administrador")) {
                        return "redirect:/admin/mi_cuenta"; // Asumiendo que esta es la URL de inicio del Admin
                    } else if (targetRolName.equals("Coordinador")) {
                        return "redirect:/coordinador/mi-perfil"; // Asumiendo que esta es la URL de inicio del Coordinador
                    } else if (targetRolName.equals("Usuario final")) {
                        return "redirect:/"; // Asumiendo que esta es la URL de inicio del Usuario final
                    } else {
                        // Redirección por defecto si el rol no es reconocido
                        System.out.println("--- DEBUG: [SuperAdminController] Rol objetivo no reconocido: " + targetRolName + ". Redirigiendo a la página principal. ---"); // DEBUG
                        redirectAttributes.addFlashAttribute("warning", "Usuario impersonado con rol no reconocido. Redirigido a la página principal.");
                        return "redirect:/";
                    }

                } catch (UsernameNotFoundException e) {
                    System.out.println("--- DEBUG: [SuperAdminController] ERROR: No se pudo cargar UserDetails para el usuario objetivo: " + targetUsername + " ---"); // DEBUG
                    // Manejar error: No se pudieron cargar los detalles del usuario objetivo
                    redirectAttributes.addFlashAttribute("error", "Error al iniciar impersonación: No se pudieron cargar los detalles del usuario objetivo.");
                    return "redirect:/superadmin/home"; // Redirigir de vuelta a la página de inicio del SuperAdmin
                }
            } else {
                System.out.println("--- DEBUG: [SuperAdminController] ERROR: Usuario objetivo sin DNI ni Correo. No se puede cargar UserDetails. ---"); // DEBUG
                // Manejar error: El usuario objetivo no tiene DNI ni Correo
                redirectAttributes.addFlashAttribute("error", "Error al iniciar impersonación: El usuario objetivo no tiene DNI ni correo.");
                return "redirect:/superadmin/home"; // Redirigir de vuelta a la página de inicio del SuperAdmin
            }

        } else {
            System.out.println("--- DEBUG: [SuperAdminController] ERROR: Usuario objetivo no encontrado por ID: " + userId + " ---"); // DEBUG
            // Manejar error: Usuario objetivo no encontrado
            redirectAttributes.addFlashAttribute("error", "Error al iniciar impersonación: Usuario objetivo no encontrado.");
            return "redirect:/superadmin/home"; // Redirigir de vuelta a la página de inicio del SuperAdmin
        }
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
        // Si no hay impersonación, cerrar sesión normal
        return "redirect:/logout";
    }

    @GetMapping("/exportar-reservas-excel")
    @ResponseBody
    public void exportarReservasExcel(HttpServletResponse response) throws Exception {
        // Obtener la lista de reservas recientes (igual que en el método home)
        List<com.example.project.entity.Reserva> reservas = reservasRepository.findAll(Sort.by("idReserva").descending());

        // Crear el archivo Excel
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reservas Recientes");

        // Cabecera
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Usuario");
        header.createCell(1).setCellValue("Monto");
        header.createCell(2).setCellValue("Estado");
        header.createCell(3).setCellValue("Forma de pago");

        int rowIdx = 1;
        for (com.example.project.entity.Reserva reserva : reservas) {
            Row row = sheet.createRow(rowIdx++);
            String usuario = reserva.getVecino() != null ? reserva.getVecino().getNombres() + " " + reserva.getVecino().getApellidos() : "";
            row.createCell(0).setCellValue(usuario);
            row.createCell(1).setCellValue(reserva.getCosto() != null ? reserva.getCosto() : 0);
            String estado = reserva.getEstado() != null && reserva.getEstado().getIdEstadoReserva() == 1 ? "PAGADO" : "PENDIENTE";
            row.createCell(2).setCellValue(estado);
            String formaPago = "Pago en línea";
            if ("En banco".equals(reserva.getTipoPago())) {
                formaPago = "Pago en banco";
            }
            row.createCell(3).setCellValue(formaPago);
        }

        // Configurar la respuesta HTTP
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reservas_recientes.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @PostMapping("/exportar-reservas-excel")
    @ResponseBody
    public void exportarReservasExcelFiltradas(HttpServletResponse response, @RequestParam("reservas") String reservasJson) throws Exception {
        System.out.println("Iniciando exportación de reservas filtradas..."); // Log para debug

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> reservas = mapper.readValue(reservasJson, new TypeReference<List<Map<String, Object>>>(){});

            System.out.println("Número de reservas a exportar: " + reservas.size()); // Log para debug

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Reservas Filtradas");

            // Crear estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Crear encabezado con estilo
            Row header = sheet.createRow(0);
            String[] columns = {"Usuario", "Monto", "Estado", "Forma de pago"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowIdx = 1;
            for (Map<String, Object> reserva : reservas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue((String) reserva.get("usuario"));
                row.createCell(1).setCellValue(Double.parseDouble(reserva.get("monto").toString()));
                row.createCell(2).setCellValue((String) reserva.get("estado"));
                row.createCell(3).setCellValue((String) reserva.get("formaPago"));
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=reservas_filtradas.xlsx");

            // Escribir el archivo
            workbook.write(response.getOutputStream());
            workbook.close();

            System.out.println("Exportación completada exitosamente"); // Log para debug

        } catch (Exception e) {
            System.err.println("Error durante la exportación: " + e.getMessage()); // Log para debug
            e.printStackTrace();
            throw e; // Re-lanzar la excepción para que el cliente sepa que hubo un error
        }
    }


    // Método auxiliar para verificar si una cadena es numérica (útil si el username puede ser DNI)
    private boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Nuevo endpoint para cambiar credenciales solo con contraseña actual
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
        Usuarios superadmin = (Usuarios) session.getAttribute("usuario");
        if (superadmin == null) {
            redirectAttributes.addFlashAttribute("error", "No se pudo identificar al usuario.");
            return "redirect:/superadmin/home";
        }

        // Validar contraseña actual
        if (contrasenaActual == null || !passwordEncoder.matches(contrasenaActual, superadmin.getContrasena())) {
            redirectAttributes.addFlashAttribute("error", "Contraseña actual incorrecta.");
            return "redirect:/superadmin/home";
        }

        // Validación de teléfono duplicado
        if (nuevoNumero != null && !nuevoNumero.isBlank()) {
            Optional<Usuarios> usuarioConTelefono = usuariosRepository.findByTelefono(nuevoNumero.trim());
            if (usuarioConTelefono.isPresent() && !usuarioConTelefono.get().getIdUsuarios().equals(superadmin.getIdUsuarios())) {
                redirectAttributes.addFlashAttribute("error", "El número ingresado ya está registrado en otro usuario.");
                return "redirect:/superadmin/home";
            }
            try {
                superadmin.setTelefono(nuevoNumero.trim());
            } catch (Exception ignored) {}
        }

        if (nuevoCorreo != null && !nuevoCorreo.isBlank()) {
            superadmin.setCorreo(nuevoCorreo.trim());
        }
        if (nuevaContrasena != null && !nuevaContrasena.isBlank() && nuevaContrasena.equals(confirmarContrasena)) {
            superadmin.setContrasena(passwordEncoder.encode(nuevaContrasena));
        }
        usuariosRepository.save(superadmin);
        redirectAttributes.addFlashAttribute("success", "Credenciales actualizadas correctamente.");
        return "redirect:/superadmin/home";
    }

    // Endpoint para validar la contraseña actual vía AJAX
    @PostMapping("/validar-contrasena")
    @ResponseBody
    public Map<String, Boolean> validarContrasena(@RequestParam("contrasenaActual") String contrasenaActual,
                                                HttpSession session) {
        Map<String, Boolean> response = new HashMap<>();
        try {
            Usuarios superadmin = (Usuarios) session.getAttribute("usuario");
            boolean isValid = superadmin != null &&
                            passwordEncoder.matches(contrasenaActual, superadmin.getContrasena());
            response.put("valida", isValid);
        } catch (Exception e) {
            response.put("valida", false);
        }
        return response;
    }

    @PostMapping("/crear-admin")
    public String crearAdmin(@ModelAttribute("admin") Usuarios admin,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {
        try {
            // Validaciones básicas
            if (admin.getDni() == null || admin.getDni().trim().isEmpty() ||
                admin.getNombres() == null || admin.getNombres().trim().isEmpty() ||
                admin.getApellidos() == null || admin.getApellidos().trim().isEmpty() ||
                admin.getCorreo() == null || admin.getCorreo().trim().isEmpty() ||
                admin.getContrasena() == null || admin.getContrasena().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Todos los campos son obligatorios");
                return "redirect:/superadmin/home";
            }

            // Verificar si ya existe un usuario con ese DNI
            if (usuariosRepository.findByDni(admin.getDni()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un usuario con ese DNI");
                return "redirect:/superadmin/home";
            }

            // Verificar si ya existe un usuario con ese correo
            if (usuariosRepository.findByCorreo(admin.getCorreo()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un usuario con ese correo");
                return "redirect:/superadmin/home";
            }

            // Configurar el rol de Administrador (ID 3 según tu base de datos)
            Rol rolAdmin = rolRepository.findById(3)
                .orElseThrow(() -> new RuntimeException("Rol de Administrador no encontrado"));
            admin.setRol(rolAdmin);

            // Configurar estado activo (ID 1 según tu base de datos)
            EstadoUsu estadoActivo = new EstadoUsu();
            estadoActivo.setIdEstado(1);
            admin.setEstado(estadoActivo);

            // Encriptar la contraseña
            admin.setContrasena(passwordEncoder.encode(admin.getContrasena()));

            // Guardar el nuevo administrador
            usuariosRepository.save(admin);

            redirectAttributes.addFlashAttribute("success", "Administrador creado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear el administrador: " + e.getMessage());
        }

        return "redirect:/superadmin/home";
    }
}
