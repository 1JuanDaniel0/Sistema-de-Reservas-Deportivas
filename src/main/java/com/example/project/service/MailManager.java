package com.example.project.service;

import com.example.project.entity.Reserva;
import com.example.project.entity.SolicitudCancelacion;
import com.example.project.entity.Usuarios;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MailManager {

    private final JavaMailSender javaMailSender;
    @Autowired private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String sender; // Esto es el "from" del correo

    public MailManager(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendMessage(String toEmail, String subject, String messageText) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(messageText, true); // true para HTML, false para texto plano
            helper.setFrom(sender);

            javaMailSender.send(message);
            System.out.println("Correo enviado a: " + toEmail); // Para depuración
        } catch (MessagingException e) {
            System.err.println("Error al enviar correo: " + e.getMessage()); // Para depuración
            throw new RuntimeException("Error al enviar el correo.", e);
        }
    }

    // Método general mejorado para notificaciones de reserva
    @Async
    public void enviarNotificacionReserva(Usuarios destinatario, String asunto, String titulo,
                                          String accion, String espacio, String fecha,
                                          String estadoReembolso, Reserva reserva, boolean esPagoBanco) {
        Context context = new Context();
        context.setVariable("nombre", destinatario.getNombres() + " " + destinatario.getApellidos());
        context.setVariable("titulo", titulo);
        context.setVariable("espacio", espacio);
        context.setVariable("fecha", fecha);
        context.setVariable("accion", accion);
        context.setVariable("reembolso", estadoReembolso);

        // Información adicional de la reserva
        if (reserva != null) {
            context.setVariable("costo", String.format("%.2f", reserva.getCosto()));
            context.setVariable("tipoPago", reserva.getTipoPago());
            context.setVariable("numeroReserva", reserva.getIdReserva().toString());

            // Datos para pago en banco
            context.setVariable("mostrarDatosPago", esPagoBanco);
            if (esPagoBanco) {
                String linkBoleta = "https://serviciosdeportivos-sanmiguel.lat/vecino/boleta/" + reserva.getIdReserva();
                context.setVariable("linkBoleta", linkBoleta);
            }
        } else {
            context.setVariable("mostrarDatosPago", false);
        }

        // Color según el estado
        String color = "#004ba0"; // azul por defecto
        if ("Rechazada".equalsIgnoreCase(accion) || "Rechazado".equalsIgnoreCase(estadoReembolso)) {
            color = "#e53935"; // rojo
        } else if ("Confirmada".equalsIgnoreCase(accion) || "Aprobado".equalsIgnoreCase(estadoReembolso)) {
            color = "#43a047"; // verde
        } else if ("registrada (pendiente de pago)".equalsIgnoreCase(accion)) {
            color = "#ff9800"; // naranja
        }
        context.setVariable("color", color);

        String contenido = templateEngine.process("email/plantilla-reserva", context);
        enviarCorreoHtml(destinatario.getCorreo(), asunto, contenido);
    }

    // Reserva confirmada - Pago en línea
    @Async
    public void enviarCorreoReservaConfirmada(Usuarios usuario, Reserva reserva) {
        if (usuario == null || reserva == null) return;

        String asunto = "✅ Reserva confirmada - Municipalidad de San Miguel";
        String titulo = "¡Tu reserva ha sido confirmada!";
        String accion = "confirmada";
        String espacio = reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar();
        String fecha = formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        String estadoReembolso = null;

        enviarNotificacionReserva(usuario, asunto, titulo, accion, espacio, fecha, estadoReembolso, reserva, false);
    }

    // Reserva pendiente - Pago en banco
    @Async
    public void enviarCorreoReservaPendiente(Usuarios usuario, Reserva reserva) {
        if (usuario == null || reserva == null) return;

        String asunto = "🕓 Reserva registrada - Complete su pago";
        String titulo = "Tu reserva fue registrada correctamente";
        String accion = "registrada (pendiente de pago)";
        String espacio = reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar();
        String fecha = formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        String estadoReembolso = null;

        enviarNotificacionReserva(usuario, asunto, titulo, accion, espacio, fecha, estadoReembolso, reserva, true);
    }

    // Método para reserva cancelada (opcional)
    @Async
    public void enviarCorreoReservaCancelada(Usuarios usuario, Reserva reserva, String motivoCancelacion) {
        if (usuario == null || reserva == null) return;

        String asunto = "❌ Reserva cancelada - Municipalidad de San Miguel";
        String titulo = "Su reserva ha sido cancelada";
        String accion = "cancelada";
        String espacio = reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar();
        String fecha = formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        String estadoReembolso = motivoCancelacion; // Usar el motivo como información adicional

        enviarNotificacionReserva(usuario, asunto, titulo, accion, espacio, fecha, estadoReembolso, reserva, false);
    }

    // Enviar correo de recordatorio el día de la reserva
    @Async
    public void enviarRecordatorioReserva(String correo, Reserva reserva) {
        Context context = new Context();
        context.setVariable("color", "#2196f3");
        context.setVariable("titulo", "📅 Recordatorio de tu reserva");
        context.setVariable("nombre", reserva.getVecino().getNombres());
        context.setVariable("espacio", reserva.getEspacio().getNombre());
        context.setVariable("fecha", reserva.getFecha().toString());
        context.setVariable("accion", "confirmada y está programada para mañana");
        context.setVariable("reembolso", null);

        String html = templateEngine.process("email/plantilla-reserva.html", context);
        enviarCorreoHtml(correo, "📌 Recordatorio de tu reserva deportiva", html);
    }

    // Enviar correo de consulta desde la landing
    @Async
    public void enviarCorreoConsulta(String nombre, String correo, String mensaje) {
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("correo", correo);
        context.setVariable("mensaje", mensaje);

        String html = templateEngine.process("email/correo-contacto.html", context);

        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo("juanulloavega3@gmail.com"); // Luego cambio para obtener el correo del administrador
            helper.setSubject("Nueva consulta desde la página principal");
            helper.setText(html, true);
            helper.setFrom(sender);            // Remitente oficial
            helper.setReplyTo(correo);         // Permite al admin responder al vecino

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo HTML.", e);
        }
    }

    // Enviar correo de confirmación de registro
    @Async
    public void enviarNotificacionRegistro(String emailDestino, String nombreUsuario) {
        Context context = new Context();
        context.setVariable("nombre", nombreUsuario);

        // Renderiza la plantilla HTML con Thymeleaf
        String html = templateEngine.process("email/notificacion-registro.html", context);

        // Asunto del correo
        String asunto = "Bienvenido a la Municipalidad de San Miguel";

        // Enviar el correo
        enviarCorreoHtml(emailDestino, asunto, html);
    }

    public void enviarOlvidoContrasena(String to, String resetLink) {
        Context context = new Context();
        context.setVariable("link", resetLink);
        String html = templateEngine.process("email/olvido-correo.html", context);
        enviarCorreoHtml(to, "Restablecimiento de contraseña", html);
    }
    public void enviarConfirmacionCambioContrasena(String correo, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String ubicacion = "Desconocida"; // si luego integras IP geolocation, puedes colocarla aquí

        Context context = new Context();
        context.setVariable("correo", correo);
        context.setVariable("ip", ip);
        context.setVariable("userAgent", userAgent);
        context.setVariable("fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        String html = templateEngine.process("email/confirmacion-cambio.html", context);
        enviarCorreoHtml(correo, "Tu contraseña ha sido cambiada", html);
    }

    // Método auxiliar para formatear fecha y hora
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


    // Método principal para notificar solicitud de reembolso al coordinador
    @Async
    public void enviarNotificacionSolicitudReembolso(SolicitudCancelacion solicitud) {
        if (solicitud == null || solicitud.getReserva() == null ||
                solicitud.getReserva().getCoordinador() == null) {
            System.err.println("❌ Error: Datos incompletos para enviar notificación de reembolso");
            return;
        }

        Reserva reserva = solicitud.getReserva();
        Usuarios coordinador = reserva.getCoordinador();
        Usuarios vecino = reserva.getVecino();

        // Calcular si es urgente (menos de 24 horas)
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaHoraReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        boolean esUrgente = ChronoUnit.HOURS.between(ahora, fechaHoraReserva) < 24;

        Context context = new Context();
        context.setVariable("coordinador", coordinador.getNombres() + " " + coordinador.getApellidos());
        context.setVariable("vecino", vecino.getNombres() + " " + vecino.getApellidos());
        context.setVariable("espacio", reserva.getEspacio().getNombre() + " - " +
                reserva.getEspacio().getIdLugar().getLugar());
        context.setVariable("fechaHora", reserva.getFecha() + " de " +
                reserva.getHoraInicio() + " a " + reserva.getHoraFin());
        context.setVariable("monto", String.format("%.2f", reserva.getCosto()));
        context.setVariable("codigoPago", solicitud.getCodigoPago());
        context.setVariable("motivo", solicitud.getMotivo());
        context.setVariable("esUrgente", esUrgente);
        context.setVariable("fechaSolicitud", solicitud.getFechaSolicitud()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        String linkPlataforma = "https://serviciosdeportivos-sanmiguel.lat/coordinador/ver-solicitudes-reembolso";
        context.setVariable("linkPlataforma", linkPlataforma);

        String html = templateEngine.process("email/solicitud-reembolso", context);

        String asunto = esUrgente ?
                "🔥 URGENTE - Nueva solicitud de reembolso" :
                "💰 Nueva solicitud de reembolso pendiente";

        enviarCorreoHtml(coordinador.getCorreo(), asunto, html);

        System.out.println("✅ Notificación de solicitud de reembolso enviada a: " + coordinador.getCorreo());
    }

    // Método para notificar al coordinador de que ha recibido una verificación de comprobante de pago

    /**
     * Envía una notificación por correo al coordinador cuando un vecino sube su comprobante de pago
     * @param reserva La reserva con el comprobante subido
     */
    public void enviarNotificacionVerificarComprobante(Reserva reserva) {
        try {
            // Obtener el coordinador de la reserva
            Usuarios coordinador = reserva.getCoordinador();
            if (coordinador == null || coordinador.getCorreo() == null) {
                System.err.println("❌ No se pudo enviar notificación: coordinador o correo no encontrado");
                return;
            }

            // Calcular horas hasta la reserva
            LocalDateTime fechaHoraReserva = LocalDateTime.of(
                    reserva.getFecha(),
                    reserva.getHoraInicio()
            );
            long horasHastaReserva = ChronoUnit.HOURS.between(LocalDateTime.now(), fechaHoraReserva);

            // Datos para la plantilla
            String nombreVecino = reserva.getVecino().getNombres() + " " + reserva.getVecino().getApellidos();
            String nombreEspacio = reserva.getEspacio().getNombre();
            String fechaReserva = reserva.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String horaReserva = reserva.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")) +
                    " - " + reserva.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));
            String estadoReserva = reserva.getEstado().getEstado();

            // Determinar la urgencia
            String claseUrgencia = horasHastaReserva <= 24 ? "urgente" : "normal";
            String textoUrgencia = horasHastaReserva <= 24 ?
                    "⚠️ URGENTE - Reserva en menos de 24 horas" :
                    "Reserva programada";

            Context context = new Context();
            context.setVariable("nombreVecino", nombreVecino);
            context.setVariable("nombreEspacio", nombreEspacio);
            context.setVariable("fechaReserva", fechaReserva);
            context.setVariable("horaReserva", horaReserva);
            context.setVariable("estadoReserva", estadoReserva);
            context.setVariable("claseUrgencia", claseUrgencia);
            context.setVariable("textoUrgencia", textoUrgencia);

            // Asunto del correo
            String asunto = String.format("🏃‍♂️ Comprobante subido - %s (%s)",
                    nombreEspacio,
                    horasHastaReserva <= 24 ? "URGENTE" : "Pendiente"
            );
            String linkPlataforma = "https://serviciosdeportivos-sanmiguel.lat/coordinador/verificar-reservas";
            context.setVariable("linkPlataforma", linkPlataforma);

            String html = templateEngine.process("email/verificar-comprobante", context);

            // Enviar el correo
            enviarCorreoHtml(coordinador.getCorreo(), asunto, html);

            System.out.println("✅ Notificación enviada al coordinador: " + coordinador.getCorreo());

        } catch (Exception e) {
            System.err.println("❌ Error al enviar notificación de comprobante: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método wrapper específico para respuestas de reembolso
    @Async
    public void enviarRespuestaSolicitudReembolso(SolicitudCancelacion solicitud, boolean aprobada, String observaciones) {
        if (solicitud == null || solicitud.getReserva() == null ||
                solicitud.getReserva().getVecino() == null) return;

        Reserva reserva = solicitud.getReserva();
        Usuarios vecino = reserva.getVecino();

        // Usar el método específico para reembolsos
        if (aprobada) {
            enviarCorreoReembolsoAprobado(vecino, reserva, observaciones);
        } else {
            enviarCorreoReembolsoRechazado(vecino, reserva, observaciones);
        }
    }

    // Métodos auxiliares más específicos
    @Async
    public void enviarCorreoReembolsoAprobado(Usuarios vecino, Reserva reserva, String observaciones) {
        String asunto = "✅ Reembolso Aprobado - Municipalidad de San Miguel";
        String titulo = "¡Tu solicitud de reembolso ha sido aprobada!";
        String accion = "aprobada";
        String espacio = reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar();
        String fecha = formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        String estadoReembolso = observaciones != null ? observaciones : "Reembolso será procesado en 24-48 horas";

        enviarNotificacionReserva(vecino, asunto, titulo, accion, espacio, fecha, estadoReembolso, reserva,
                "En banco".equals(reserva.getTipoPago()));
    }

    @Async
    public void enviarCorreoReembolsoRechazado(Usuarios vecino, Reserva reserva, String observaciones) {
        String asunto = "❌ Solicitud de Reembolso Rechazada - Municipalidad de San Miguel";
        String titulo = "Tu solicitud de reembolso ha sido rechazada";
        String accion = "rechazada";
        String espacio = reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar();
        String fecha = formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        String estadoReembolso = observaciones != null ? "Motivo: " + observaciones : "Sin motivo especificado";

        enviarNotificacionReserva(vecino, asunto, titulo, accion, espacio, fecha, estadoReembolso, reserva, false);
    }

    // En MailManager - Método específico para reembolsos automáticos
    @Async
    public void enviarCorreoReembolsoAutomatico(Usuarios usuario, Reserva reserva, boolean exitoso) {
        String asunto = exitoso ?
                "✅ Reserva cancelada con reembolso exitoso" :
                "⚠️ Reserva cancelada - Reembolso en proceso";

        String titulo = "Su reserva ha sido cancelada";
        String accion = exitoso ?
                "cancelada con reembolso automático" :
                "cancelada (reembolso en proceso manual)";

        String estadoReembolso = exitoso ?
                "Reembolso procesado - Disponible en 5-7 días hábiles" :
                "Reembolso será procesado manualmente";

        String espacio = reserva.getEspacio().getNombre() + " - " + reserva.getEspacio().getIdLugar().getLugar();
        String fecha = formatearFechaHora(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());

        enviarNotificacionReserva(usuario, asunto, titulo, accion, espacio, fecha, estadoReembolso, reserva, false);
    }

    /**
     * Envía correo de confirmación de reserva al vecino usando plantilla HTML
     */
    public void enviarConfirmacionReserva(Reserva reserva) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Configurar destinatario y asunto
        helper.setTo(reserva.getVecino().getCorreo());
        helper.setSubject("✅ Reserva Confirmada - Municipalidad de San Miguel");
        helper.setFrom("noreply@sanmiguel.gob.pe");

        // Crear contexto para la plantilla
        Context context = new Context();
        context.setVariable("reserva", reserva);

        // Procesar la plantilla HTML
        String contenidoHtml = templateEngine.process("email/email-confirmacion-reserva.html", context);

        // Establecer contenido HTML
        helper.setText(contenidoHtml, true);

        // Enviar correo
        javaMailSender.send(message);

        System.out.println("✅ Correo de confirmación enviado a: " + reserva.getVecino().getCorreo());
    }

    /**
     * Envía correo de rechazo de reserva al vecino usando plantilla HTML
     */
    public void enviarRechazoReserva(Reserva reserva, String motivo) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Configurar destinatario y asunto
        helper.setTo(reserva.getVecino().getCorreo());
        helper.setSubject("❌ Reserva No Confirmada - Municipalidad de San Miguel");
        helper.setFrom("noreply@sanmiguel.gob.pe");

        // Crear contexto para la plantilla
        Context context = new Context();
        context.setVariable("reserva", reserva);
        context.setVariable("motivo", motivo);

        // Procesar la plantilla HTML
        String contenidoHtml = templateEngine.process("email/email-rechazo-reserva.html", context);

        // Establecer contenido HTML
        helper.setText(contenidoHtml, true);

        // Enviar correo
        javaMailSender.send(message);

        System.out.println("✅ Correo de rechazo enviado a: " + reserva.getVecino().getCorreo());
    }

    // Método para enviar el correo
    public void enviarCorreoHtml(String toEmail, String subject, String htmlContent) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            helper.setFrom(sender);

            // Para evitar que Gmail los agrupe
            message.setHeader("X-Unique-Id", UUID.randomUUID().toString());

            javaMailSender.send(message);
            System.out.println("✅ Correo HTML enviado a: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ Error al enviar correo a " + toEmail + ": " + e.getMessage());
        }
    }

}