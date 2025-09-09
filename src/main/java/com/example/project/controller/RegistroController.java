package com.example.project.controller;

import com.example.project.repository.EstadoUsuRepository;
import com.example.project.repository.RolRepository;
import com.example.project.repository.UsuariosRepository;
import com.example.project.entity.EstadoUsu;
import com.example.project.entity.Rol;
import com.example.project.entity.Usuarios;
import com.example.project.service.IntentoIpService;
import com.example.project.service.MailManager;
import com.example.project.util.IpUtils;
import com.example.project.util.TelefonoUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;

@Controller
@RequestMapping("/registro")
public class RegistroController {

    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private EstadoUsuRepository estadoUsuRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MailManager mailManager;
    @Autowired private IntentoIpService intentoIpService;

//    @Value("${apisperu.token}")
//    private String apisPeruToken;

    @Value("${apisnet.token}")
    private String apisNetToken;

    @GetMapping
    public String mostrarFormularioRegistro(HttpSession session) {
        System.out.println("🟢 GET /registroNuevo iniciado");
        session.removeAttribute("nombres");
        session.removeAttribute("apellidos");
        session.removeAttribute("hash-autorizado");
        session.removeAttribute("intentosDni");
        return "registro/registro2";
    }

    @GetMapping("/verificar-dni")
    @ResponseBody
    public Map<String, Boolean> verificarDni(@RequestParam String dni) {
        boolean existe = usuariosRepository.existsByDni(dni);
        System.out.println("🔍 Verificando DNI (" + dni + "): existe = " + existe);
        return Map.of("existe", existe);
    }

    @PostMapping("/consultar-dni-api")
    @ResponseBody
    public ResponseEntity<Map<String, String>> consultarDni(@RequestParam String dni, HttpSession session, HttpServletRequest request) {

        Map<String, String> response = new HashMap<>();
        String clientIp = IpUtils.obtenerIpReal(request);

        System.out.println("🌐 Consultando DNI: " + dni + ", desde IP real: " + clientIp);

        boolean permitido = intentoIpService.verificaYActualizaIntento(clientIp, "CONSULTA_DNI", Duration.ofMinutes(5),3);
        if (!permitido) {
            System.out.println("⚠️ IP " + clientIp + " bloqueada por exceso de intentos en 5 minutos");
            return ResponseEntity.status(429).body(Map.of("status", "bloqueado"));
        }

        try {
            String url = "https://api.apis.net.pe/v2/reniec/dni?numero=" + dni;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apisNetToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            System.out.println("📡 Respuesta APIs.net.pe: " + apiResponse.getBody());

            if (apiResponse.getStatusCode() == HttpStatus.OK) {
                JSONObject json = new JSONObject(apiResponse.getBody());

                String nombres = json.optString("nombres", "");
                String apellidoPaterno = json.optString("apellidoPaterno", "");
                String apellidoMaterno = json.optString("apellidoMaterno", "");
                String apellidos = (apellidoPaterno + " " + apellidoMaterno).trim();

                if (nombres.isEmpty() || apellidos.isEmpty()) {
                    System.out.println("❌ Datos incompletos: nombres o apellidos vacíos");
                    response.put("status", "error");
                    return ResponseEntity.ok(response);
                }

                String hash = UUID.randomUUID().toString();
                session.setAttribute("nombres", nombres);
                session.setAttribute("apellidos", apellidos);
                session.setAttribute("hash-autorizado", hash);

                response.put("status", "ok");
                response.put("uuid", hash);
                System.out.println("✅ Consulta exitosa: " + nombres + " " + apellidos);
            } else {
                response.put("status", "error");
                System.out.println("❌ Error HTTP al consultar APIs.net.pe: " + apiResponse.getStatusCode());
            }

        } catch (Exception e) {
            System.out.println("🔥 Error en consulta APIs.net.pe: " + e.getMessage());
            response.put("status", "error");
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/datos-session")
    @ResponseBody
    public Map<String, String> obtenerDatosDesdeSesion(@RequestParam String uuid, HttpSession session) {
        Map<String, String> response = new HashMap<>();

        String hash = (String) session.getAttribute("hash-autorizado");
        String nombres = (String) session.getAttribute("nombres");
        String apellidos = (String) session.getAttribute("apellidos");

        System.out.println("📦 Consultando datos sesión: uuid=" + uuid + ", hash-sesion=" + hash);

        if (hash == null || !hash.equals(uuid) || nombres == null || apellidos == null) {
            System.out.println("⚠️ Datos inválidos o expirados en sesión");
            response.put("nombres", "");
            response.put("apellidos", "");
            return response;
        }

        response.put("nombres", nombres);
        response.put("apellidos", apellidos);
        return response;
    }

    @PostMapping
    public String procesarRegistro(@ModelAttribute Usuarios usuario, BindingResult result, Model model,
                                   HttpSession session, HttpServletRequest request) {

        System.out.println("📩 POST registro recibido: DNI=" + usuario.getDni() + ", correo=" + usuario.getCorreo());

        String hash = (String) session.getAttribute("hash-autorizado");
        String nombres = (String) session.getAttribute("nombres");
        String apellidos = (String) session.getAttribute("apellidos");

        if (hash == null || nombres == null || apellidos == null) {
            model.addAttribute("error", "Debes iniciar el registro correctamente desde el paso 1.");
            System.out.println("⛔ Error: sesión inválida o incompleta.");
            return "registro/registro2";
        }

        if (usuariosRepository.existsByDni(usuario.getDni())) {
            result.rejectValue("dni", "error.usuario", "El DNI ya está registrado.");
            System.out.println("⚠️ Error: DNI ya registrado.");
        }

        if (usuariosRepository.existsByCorreo(usuario.getCorreo())) {
            result.rejectValue("correo", "error.usuario", "El correo ya está registrado.");
            System.out.println("⚠️ Error: correo ya registrado.");
        }

        // 🧼 Normaliza el número ANTES de guardar o validar duplicados
        String telefonoLimpio = TelefonoUtils.normalizar(usuario.getTelefono());
        usuario.setTelefono(telefonoLimpio);

        if (telefonoLimpio != null && usuariosRepository.existsByTelefono(telefonoLimpio)) {
            result.rejectValue("telefono", "error.usuario", "El teléfono ya está registrado.");
            System.out.println("⚠️ Error: teléfono ya registrado.");
        }

        if (usuariosRepository.existsByTelefono(telefonoLimpio)) {
            result.rejectValue("telefono", "error.usuario", "El teléfono ya está registrado.");
            System.out.println("⚠️ Error: teléfono ya registrado.");
        }

        if (result.hasErrors()) {
            model.addAttribute("usuario", usuario);
            return "registro/registro2";
        }

        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setFechaCreacion(new Timestamp(System.currentTimeMillis()));
        EstadoUsu estadoUsu = estadoUsuRepository.findByEstado("Activo");
        Rol rol = rolRepository.findByRol("Usuario final");
        usuario.setEstado(estadoUsu);
        usuario.setRol(rol);
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuariosRepository.save(usuario);
        mailManager.enviarNotificacionRegistro(usuario.getCorreo(), nombres);

        System.out.println("✅ Usuario guardado exitosamente.");

        session.invalidate();

        return "redirect:/login?registroExitoso";
    }

    @GetMapping("/verificar-correo")
    @ResponseBody
    public Map<String, Boolean> verificarCorreo(@RequestParam String correo) {
        boolean existe = usuariosRepository.existsByCorreo(correo);
        return Map.of("existe", existe);
    }

    @GetMapping("/verificar-telefono")
    @ResponseBody
    public Map<String, Boolean> verificarTelefono(@RequestParam String telefono) {
        boolean existe = telefono != null && !telefono.isBlank()
                && usuariosRepository.existsByTelefono(telefono);
        return Map.of("existe", existe);
    }

}
