package com.example.project.controller;

import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.admin.AdminRepository;
import com.example.project.repository.superadmin.ReservasRepository;
import com.example.project.service.MailManager;
import com.example.project.service.SmsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.annotation.Validated;
import com.example.project.validation.OnCreate;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

@Controller
@RequestMapping("/")
public class HomeController {

    final UsuariosRepository usuariosRepository;
    final RolRepository rolRepository;
    final EstadoUsuRepository estadoRepository;
    final MailManager mailManager;
    final PasswordEncoder passwordEncoder;
    final EspacioRepositoryGeneral espacioRepositoryGeneral;
    @Autowired private ReservasRepository reservasRepository;
    @Autowired private CalificacionRepository calificacionRepository;

    // @Value("${app.base-url}")
    // private String appBaseUrl;

    // Constructor con las inyecciones necesarias
    public HomeController(UsuariosRepository usuariosRepository,
                          RolRepository rolRepository,
                          EstadoUsuRepository estadoRepository,
                          MailManager mailManager,
                          PasswordEncoder passwordEncoder,
                          EspacioRepositoryGeneral espacioRepositoryGeneral) {
        this.usuariosRepository = usuariosRepository;
        this.rolRepository = rolRepository;
        this.estadoRepository = estadoRepository;
        this.mailManager = mailManager;
        this.passwordEncoder = passwordEncoder;
        this.espacioRepositoryGeneral = espacioRepositoryGeneral;
    }

    @Autowired ChatMensajeRepository chatMensajeRepository;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        if (session.getAttribute("usuario") != null) {
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            model.addAttribute("usuario", usuario);
            String idConversacion = (String) session.getAttribute("idConversacionActual");
            if (idConversacion != null) {
                List<ChatMensaje> historial = chatMensajeRepository
                        .findTop15ByUsuarioAndIdConversacionOrderByFechaDesc(usuario, idConversacion)
                        .stream()
                        .sorted(Comparator.comparing(ChatMensaje::getFecha))
                        .toList();
                model.addAttribute("historial", historial);
            }
        }

        Optional<Usuarios> admin = usuariosRepository.findFirstAdministrador();
        List<Espacio> espacios = espacioRepositoryGeneral.findTop6ByMejorCalificacion();

        // Crear un mapa con los promedios de calificación para cada espacio
        Map<Integer, Double> promediosCalificacion = new HashMap<>();
        for (Espacio espacio : espacios) {
            Double promedio = calificacionRepository.promedioPorEspacio(espacio.getIdEspacio());
            promediosCalificacion.put(espacio.getIdEspacio(), promedio != null ? promedio : 0.0);
        }

        Integer reservasLinea = reservasRepository.countReservasByTipoPagoIgnoreCase("En línea");
        Long usuariosRegistrados = usuariosRepository.count();
        Double promedioCalificacion = calificacionRepository.promedioGeneral();

        model.addAttribute("promedioCalificacion", promedioCalificacion);
        model.addAttribute("usuariosRegistrados", usuariosRegistrados);
        model.addAttribute("reservasLinea", reservasLinea);
        model.addAttribute("espacios", espacios);
        model.addAttribute("promediosCalificacion", promediosCalificacion);
        model.addAttribute("admin", admin.orElse(null));

        return "registro/principal";
    }

    @GetMapping("/login")
    public String login() {
        return "registro/login";
    }

    // API para consultar DNI y devolver nombres y apellidos ---
    @PostMapping(value = "/api/dni", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> consultarDni(@RequestParam("dni") String dni) {
        try {
            String url = "https://api.consultasperu.com/api/v1/query";
            JSONObject body = new JSONObject();
            body.put("token", "8a244a83b388ee9bb124a3a3f8e4d1269b44386bef1b519f3683a6a796285499");
            body.put("type_document", "dni");
            body.put("document_number", dni);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject json = new JSONObject(response.getBody());
                if (json.optBoolean("success")) {
                    JSONObject data = json.optJSONObject("data");
                    String nombres = data.optString("name", "");
                    String apellidos = data.optString("surname", "");
                    return ResponseEntity.ok(new JSONObject()
                            .put("nombres", nombres)
                            .put("apellidos", apellidos)
                            .toString());
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"No encontrado\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Error en la consulta\"}");
        }
    }

    @PostMapping("/enviar-consulta")
    public String enviarConsulta(
            @RequestParam("nombre") String nombre,
            @RequestParam("correo") String correo,
            @RequestParam("mensaje") String mensaje,
            RedirectAttributes redirectAttributes) {

        try {
            mailManager.enviarCorreoConsulta(nombre, correo, mensaje);
            redirectAttributes.addFlashAttribute("exito", "Tu consulta ha sido enviada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hubo un problema al enviar tu consulta.");
        }

        return "redirect:/#contacto";
    }


}
