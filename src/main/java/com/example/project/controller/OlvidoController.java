package com.example.project.controller;

import com.example.project.entity.OtpRequest;
import com.example.project.entity.OtpVerification;
import com.example.project.entity.PasswordResetToken;
import com.example.project.entity.Usuarios;
import com.example.project.repository.OtpVerificationRepository;
import com.example.project.repository.PasswordResetTokenRepository;
import com.example.project.repository.UsuariosRepository;
import com.example.project.service.IntentoIpService;
import com.example.project.service.MailManager;
import com.example.project.service.SmsService;
import com.example.project.service.TelesignOtpService;
import com.example.project.util.IpUtils;
import com.example.project.util.TelefonoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/olvido")
@RequiredArgsConstructor
public class OlvidoController {

    private final UsuariosRepository usuariosRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final MailManager mailManager;
    private final TelesignOtpService telesignOtpService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SmsService smsService;

    @Autowired
    private IntentoIpService intentoIpService;

    @Value("${app.url:https://localhost:8080}")
    private String appUrl;

    @GetMapping
    public String mostrarFormularioOlvido() {
        return "registro/olvidoContrasena";
    }

    @PostMapping
    public ResponseEntity<String> procesarOlvido(@RequestParam("identificador") String identificador,
                                                 HttpServletRequest request) {
        String limpio = identificador.trim().replaceAll("\\s+", "");
        String ip = IpUtils.obtenerIpReal(request);

        System.out.println("📩 Solicitud de olvido recibida.");
        System.out.println("🌐 IP solicitante: " + ip);
        System.out.println("🧾 Identificador recibido: '" + identificador + "'");
        System.out.println("🧹 Identificador limpio: '" + limpio + "'");

        // 🔒 Verificar si la IP ya ha hecho una solicitud recientemente
        boolean permitido = intentoIpService.verificaYActualizaIntento(ip, "OLVIDO", Duration.ofMinutes(10), 3);
        System.out.println("🛡 ¿IP permitida?: " + permitido);
        if (!permitido) {
            return ResponseEntity.status(429).body("limite_ip");
        }

        // Lógica normal de procesamiento
        boolean esTelefono = limpio.matches("\\d{9}");
        System.out.println("📱 ¿Es número de teléfono válido de 9 dígitos?: " + esTelefono);

        if (esTelefono) {
            System.out.println("➡️ Redirigiendo a flujo de recuperación por teléfono...");
            return procesarTelefonoAjax(limpio);
        } else {
            System.out.println("➡️ Redirigiendo a flujo de recuperación por correo...");
            return procesarEmailAjax(limpio, request);
        }
    }

    @ResponseBody
    private ResponseEntity<String> procesarEmailAjax(String correo, HttpServletRequest request) {
        Optional<Usuarios> usuario = usuariosRepository.findByCorreoIgnoreCase(correo);
        if (usuario.isEmpty()) {
            return ResponseEntity.ok("no_existe");
        }

        passwordResetTokenRepository.deleteByCorreo(correo);

        String token = UUID.randomUUID().toString();
        LocalDateTime ahora = LocalDateTime.now();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setCorreo(correo);
        prt.setToken(token);
        prt.setFechaCreacion(ahora);
        prt.setFechaExpiracion(ahora.plusMinutes(30));
        prt.setUsado(false);
        passwordResetTokenRepository.save(prt);

        String link = construirResetLink(request, token, correo);
        mailManager.enviarOlvidoContrasena(correo, link);

        return ResponseEntity.ok("ok");
    }

    @ResponseBody
    private ResponseEntity<String> procesarTelefonoAjax(String telefono) {
        String telefonoLimpio = TelefonoUtils.normalizar(telefono); // ej: 996113248
        System.out.println("📩 Solicitud de recuperación por teléfono: " + telefonoLimpio);

        Optional<Usuarios> usuario = usuariosRepository.findByTelefono(telefonoLimpio);
        if (usuario.isEmpty()) {
            System.out.println("❌ No se encontró un usuario con ese número");
            return ResponseEntity.badRequest().body("no_existe");
        }

        // Eliminar OTP anterior si existe
        otpVerificationRepository.findByIdentificador(telefonoLimpio).ifPresent(otp -> {
            otpVerificationRepository.delete(otp);
            System.out.println("🗑 Eliminando OTP anterior para: " + telefonoLimpio);
        });

        // Generar y guardar nuevo OTP
        String otp = generarOtp();
        LocalDateTime ahora = LocalDateTime.now();

        OtpVerification verif = new OtpVerification();
        verif.setIdentificador(telefonoLimpio);
        verif.setOtpCode(otp);
        verif.setFechaCreacion(ahora);
        verif.setFechaExpiracion(ahora.plusMinutes(5)); // Vigencia de 5 minutos
        otpVerificationRepository.save(verif);

        System.out.println("✅ Nuevo OTP generado: " + otp);

        // Enviar SMS usando SmsService
        String destino = "+51" + telefonoLimpio;
        System.out.println("📲 Enviando OTP por SMS a: " + destino);
        String sid = smsService.sendOtpSms(destino, otp);

        if (sid == null) {
            System.err.println("🚨 Error: No se pudo enviar el SMS a " + destino);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error_envio_sms");
        }

        System.out.println("📤 OTP enviado correctamente a: " + destino);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/validar-identificador")
    @ResponseBody
    public ResponseEntity<String> validarIdentificador(@RequestParam("valor") String valor) {
        String limpio = valor.trim().replaceAll("\\s+", "");

        System.out.println("🔍 Validando identificador recibido: '" + valor + "'");
        System.out.println("🔧 Limpio como: '" + limpio + "'");

        boolean existeCorreo = usuariosRepository.existsByCorreoIgnoreCase(limpio);
        boolean existeTelefono = usuariosRepository.existsByTelefono(limpio);

        System.out.println("📧 ¿Existe como correo?: " + existeCorreo);
        System.out.println("📱 ¿Existe como teléfono?: " + existeTelefono);

        boolean existe = existeCorreo || existeTelefono;
        return ResponseEntity.ok(existe ? "existe" : "no_existe");
    }

    @GetMapping("/reestablecer-contrasena")
    public String mostrarFormularioNuevaContrasena(@RequestParam("token") String token,
                                                   Model model,
                                                   RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> opt = passwordResetTokenRepository.findByToken(token);

        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este enlace no es válido.");
            return "redirect:/login";
        }

        PasswordResetToken prt = opt.get();

        if (prt.isUsado() || prt.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este enlace ya fue usado o ha expirado. Solicita uno nuevo.");
            return "redirect:/login";
        }

        String identificador = prt.getCorreo();
        // Si es teléfono, normalízalo
        if (identificador != null && identificador.matches("\\d{9,}")) {
            identificador = com.example.project.util.TelefonoUtils.normalizar(identificador);
        }

        model.addAttribute("token", token);
        model.addAttribute("identificador", identificador);
        return "registro/nuevaContrasena";
    }

    @PostMapping("/confirmoContrasena")
    @Transactional
    public String procesarNuevaContrasena(
            @RequestParam String identificador,
            @RequestParam String contrasena,
            @RequestParam String confirmContrasena,
            @RequestParam String token,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        System.out.println("🔑 Intentando cambiar contraseña para: " + identificador);
        System.out.println("🔗 Token recibido: " + token);

        // 1. Validar que coincidan
        if (!contrasena.equals(confirmContrasena)) {
            System.out.println("⚠️ Las contraseñas no coinciden.");
            redirectAttributes.addFlashAttribute("errorMessage", "Las contraseñas no coinciden.");
            redirectAttributes.addFlashAttribute("token", token);
            redirectAttributes.addFlashAttribute("identificadorRecuperado", identificador);
            return "redirect:/olvido/reestablecer-contraseña";
        }

        // 2. Validar requisitos mínimos
        if (contrasena.length() < 8 ||
                !contrasena.matches(".*[a-z].*") ||
                !(contrasena.matches(".*\\d.*") || contrasena.matches(".*[\\W_].*"))) {

            System.out.println("⚠️ La contraseña no cumple requisitos.");
            redirectAttributes.addFlashAttribute("errorMessage", "La contraseña no cumple los requisitos de seguridad.");
            redirectAttributes.addFlashAttribute("token", token);
            redirectAttributes.addFlashAttribute("identificadorRecuperado", identificador);
            return "redirect:/olvido/reestablecer-contraseña";
        }

        // 3. Verificar token válido y no expirado
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(token);
        if (optionalToken.isEmpty()) {
            System.out.println("⚠️ Token inexistente.");
            redirectAttributes.addFlashAttribute("errorMessage", "Token inválido o inexistente.");
            return "redirect:/olvido";
        }

        PasswordResetToken prt = optionalToken.get();
        if (prt.isUsado()) {
            System.out.println("⚠️ Token ya usado previamente.");
            redirectAttributes.addFlashAttribute("errorMessage", "Este enlace ya fue utilizado.");
            return "redirect:/olvido";
        }
        if (prt.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            System.out.println("⚠️ Token expirado: " + prt.getFechaExpiracion());
            redirectAttributes.addFlashAttribute("errorMessage", "Este enlace ha expirado. Solicita uno nuevo.");
            return "redirect:/olvido";
        }

        // 4. Hashear y actualizar contraseña
        String hashed = passwordEncoder.encode(contrasena);
        boolean esTelefono = identificador.matches("\\d+");
        String identificadorFinal = identificador;

        if (esTelefono) {
            identificadorFinal = com.example.project.util.TelefonoUtils.normalizar(identificador);
            System.out.println("📱 Identificador normalizado para teléfono: " + identificadorFinal);
        }

        String sql = esTelefono
                ? "UPDATE Usuarios SET contrasena = ? WHERE telefono = ?"
                : "UPDATE Usuarios SET contrasena = ? WHERE correo = ?";

        Object[] params = esTelefono
                ? new Object[]{hashed, identificadorFinal}
                : new Object[]{hashed, identificadorFinal};

        int filas = jdbcTemplate.update(sql, params);
        System.out.println("📦 Filas afectadas en BD: " + filas);

        // Verifica el valor guardado en BD
        if (filas > 0) {
            String consulta = esTelefono
                ? "SELECT contrasena FROM Usuarios WHERE telefono = ?"
                : "SELECT contrasena FROM Usuarios WHERE correo = ?";
            String hashGuardado = jdbcTemplate.queryForObject(consulta, new Object[]{identificadorFinal}, String.class);
            System.out.println("🔎 Hash guardado en BD: " + hashGuardado);

            prt.setUsado(true);
            passwordResetTokenRepository.save(prt);
            System.out.println("✅ Contraseña actualizada y token marcado como usado.");

            // Solo enviar correo si es email
            if (!esTelefono) {
                mailManager.enviarConfirmacionCambioContrasena(identificadorFinal, request);
            }

            redirectAttributes.addFlashAttribute("successMessage", "¡Tu contraseña fue restablecida con éxito!");
            return "redirect:/login?cambioContraOk";
        } else {
            System.out.println("⚠️ Ninguna fila actualizada (usuario no encontrado o identificador incorrecto).");
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo actualizar la contraseña. Intenta nuevamente.");
            redirectAttributes.addFlashAttribute("token", token);
            redirectAttributes.addFlashAttribute("identificadorRecuperado", identificador);
            return "redirect:/olvido/reestablecer-contraseña";
        }
    }

    private String construirResetLink(HttpServletRequest request, String token, String correo) {
        String baseUrl;
        if (request != null) {
            baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        } else {
            baseUrl = appUrl;
        }
        return baseUrl + "/olvido/reestablecer-contrasena?token=" + token;
    }

    @PostMapping("/verificar-otp")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verificarOtp(@RequestBody OtpRequest request) {
        String telefonoLimpio = TelefonoUtils.normalizar(request.getTelefono());
        System.out.println("🔍 [OTP] Teléfono recibido: " + request.getTelefono());
        System.out.println("🔍 [OTP] Teléfono limpio: " + telefonoLimpio);
        System.out.println("🔍 [OTP] Código recibido: " + request.getOtp());

        Optional<OtpVerification> otp = otpVerificationRepository.findByIdentificador(telefonoLimpio);

        if (otp.isEmpty()) {
            System.out.println("❌ [OTP] No existe registro de OTP para: " + telefonoLimpio);
            return ResponseEntity.ok(Map.of("status", "fail", "motivo", "no_otp"));
        }
        if (otp.get().getFechaExpiracion().isBefore(LocalDateTime.now())) {
            System.out.println("❌ [OTP] OTP expirado para: " + telefonoLimpio + " (expiró en " + otp.get().getFechaExpiracion() + ")");
            return ResponseEntity.ok(Map.of("status", "fail", "motivo", "expirado"));
        }
        if (!otp.get().getOtpCode().equals(request.getOtp())) {
            System.out.println("❌ [OTP] Código incorrecto. Esperado: " + otp.get().getOtpCode() + ", recibido: " + request.getOtp());
            return ResponseEntity.ok(Map.of("status", "fail", "motivo", "incorrecto"));
        }

        // Marca como usado o lo borra
        otpVerificationRepository.delete(otp.get());
        System.out.println("✅ [OTP] Verificación exitosa para: " + telefonoLimpio + " con código: " + request.getOtp());

        // Genera token de recuperación como en correo
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setCorreo(telefonoLimpio);
        prt.setToken(token);
        prt.setFechaCreacion(LocalDateTime.now());
        prt.setFechaExpiracion(LocalDateTime.now().plusMinutes(30));
        prt.setUsado(false);
        passwordResetTokenRepository.save(prt);

        return ResponseEntity.ok(Map.of("status", "ok", "token", token));
    }

    private String generarOtp() {
        // Ahora puedes delegar a smsService si lo prefieres:
        return smsService.generateOtp();
    }

}