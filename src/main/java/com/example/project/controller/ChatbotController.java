package com.example.project.controller;

import com.example.project.dto.ChatRequest;
import com.example.project.dto.ChatbotRespuesta;
import com.example.project.entity.ChatMensaje;
import com.example.project.entity.Usuarios;
import com.example.project.repository.ChatMensajeRepository;
import com.example.project.service.ActionDispatcher;
import com.example.project.service.MistralClientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/vecino/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatMensajeRepository chatMensajeRepository;

    @Autowired
    private ActionDispatcher actionDispatcher;

    @Autowired
    private MistralClientService mistralClientService;

    @PostMapping
    public ResponseEntity<ChatbotRespuesta> procesarMensaje(@RequestBody Map<String, String> payload,
                                                            HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Verificar reserva pendiente expirada
        Map<String, Object> reservaPendiente = (Map<String, Object>) session.getAttribute("reservaEnProceso");
        if (reservaPendiente != null) {
            LocalDateTime creacion = (LocalDateTime) reservaPendiente.get("timestamp");
            if (creacion != null && creacion.isBefore(LocalDateTime.now().minusHours(1))) {
                // Limpiar reserva expirada
                session.removeAttribute("reservaEnProceso");
                System.out.println("[CHATBOT DEBUG] Reserva pendiente expirada eliminada");

                // Opcional: notificar al usuario sobre la expiración
                ChatbotRespuesta respuestaExpiracion = new ChatbotRespuesta();
                respuestaExpiracion.setRespuesta("⚠️ Tu proceso de pago anterior expiró. ¿En qué puedo ayudarte?");
                respuestaExpiracion.setAccion("ninguna");
                respuestaExpiracion.setParametros(Map.of());
                respuestaExpiracion.setRelevante_para(new String[]{"general"});
                respuestaExpiracion.setNivel_confianza(1.0);

                // Guardar mensaje del bot
                ChatMensaje msgBot = new ChatMensaje();
                msgBot.setUsuario(usuario);
                msgBot.setRol(ChatMensaje.RolMensaje.BOT);
                msgBot.setContenido(respuestaExpiracion.getRespuesta());
                msgBot.setIdConversacion((String) session.getAttribute("idConversacionActual"));
                msgBot.setFecha(LocalDateTime.now());
                chatMensajeRepository.save(msgBot);

                return ResponseEntity.ok(respuestaExpiracion);
            }
        }
        // FIN DE LA VERIFICACIÓN

        String mensaje = payload.get("mensaje");

        // 🔧 DEBUG: Log del mensaje del usuario
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("[CHATBOT DEBUG] Usuario escribió: '" + mensaje + "'");
        System.out.println("[CHATBOT DEBUG] Usuario ID: " + usuario.getIdUsuarios());

        String idConversacion = (String) session.getAttribute("idConversacionActual");
        if (idConversacion == null) {
            idConversacion = UUID.randomUUID().toString();
            session.setAttribute("idConversacionActual", idConversacion);
            System.out.println("[CHATBOT DEBUG] Nueva conversación iniciada: " + idConversacion);
        } else {
            System.out.println("[CHATBOT DEBUG] Conversación existente: " + idConversacion);
        }

        // 🔧 FIX: Obtener contexto de conversación para MistralAI
        List<ChatMensaje> historialReciente = chatMensajeRepository
                .findTop10ByUsuarioAndIdConversacionOrderByFechaDesc(usuario, idConversacion)
                .stream()
                .sorted(Comparator.comparing(ChatMensaje::getFecha))
                .toList();

        System.out.println("[CHATBOT DEBUG] Mensajes previos en conversación: " + historialReciente.size());

        // Guardar mensaje del usuario
        ChatMensaje msgUsuario = new ChatMensaje();
        msgUsuario.setUsuario(usuario);
        msgUsuario.setRol(ChatMensaje.RolMensaje.USUARIO);
        msgUsuario.setContenido(mensaje);
        msgUsuario.setIdConversacion(idConversacion);
        msgUsuario.setFecha(LocalDateTime.now());
        chatMensajeRepository.save(msgUsuario);

        try {
            // 🔧 FIX: PRIORITAR BYPASS ANTES QUE MISTRAL
            String flujoActivo = (String) session.getAttribute("flujoActivo");
            Map<String, Object> datosTemporales = (Map<String, Object>) session.getAttribute("datosTemporales");

            System.out.println("[CHATBOT DEBUG] Flujo activo: " + flujoActivo);
            System.out.println("[CHATBOT DEBUG] Datos temporales: " + datosTemporales);

            if (flujoActivo != null) {

                // 🚨 NUEVO: Verificar intención de cancelación PRIMERO
                if (esIntencionDeCancelar(mensaje, session)) {
                    System.out.println("[CHATBOT DEBUG] ❌ Cancelación detectada - Saliendo del flujo: " + flujoActivo);

                    // Limpiar flujo activo y datos temporales
                    session.removeAttribute("flujoActivo");
                    session.removeAttribute("datosTemporales");

                    ChatbotRespuesta respuestaCancelacion = new ChatbotRespuesta();
                    respuestaCancelacion.setRespuesta("✅ Proceso cancelado. ¿En qué más puedo ayudarte?");
                    respuestaCancelacion.setAccion("ninguna");
                    respuestaCancelacion.setParametros(Map.of());
                    respuestaCancelacion.setRelevante_para(new String[]{"general"});
                    respuestaCancelacion.setNivel_confianza(1.0);

                    // Guardar mensaje del bot
                    ChatMensaje msgBot = new ChatMensaje();
                    msgBot.setUsuario(usuario);
                    msgBot.setRol(ChatMensaje.RolMensaje.BOT);
                    msgBot.setContenido(respuestaCancelacion.getRespuesta());
                    msgBot.setIdConversacion(idConversacion);
                    msgBot.setFecha(LocalDateTime.now());
                    chatMensajeRepository.save(msgBot);

                    System.out.println("═══════════════════════════════════════════════");
                    return ResponseEntity.ok(respuestaCancelacion);
                }

                System.out.println("[CHATBOT DEBUG] 🔄 BYPASS DIRECTO - Enviando al flujo activo: " + flujoActivo);

                ChatRequest chatRequest = new ChatRequest();
                chatRequest.setMensaje(mensaje);
                chatRequest.setAccion(flujoActivo);

                // 🔧 FIX: Mantener datos temporales del flujo
                if (datosTemporales != null) {
                    chatRequest.setParametros(datosTemporales);
                } else {
                    chatRequest.setParametros(Map.of());
                }

                // Enviar directamente al ActionDispatcher sin consultar Mistral
                ChatbotRespuesta respuestaFinal = (ChatbotRespuesta) actionDispatcher.dispatch(chatRequest, session).getBody();

                System.out.println("[CHATBOT DEBUG] Respuesta del flujo: " + respuestaFinal.getRespuesta());

                // Guardar mensaje del bot en base de datos
                ChatMensaje msgBot = new ChatMensaje();
                msgBot.setUsuario(usuario);
                msgBot.setRol(ChatMensaje.RolMensaje.BOT);
                msgBot.setContenido(respuestaFinal.getRespuesta());
                msgBot.setIdConversacion(idConversacion);
                msgBot.setFecha(LocalDateTime.now());
                chatMensajeRepository.save(msgBot);

                System.out.println("═══════════════════════════════════════════════");
                return ResponseEntity.ok(respuestaFinal);
            }

            // 🔧 Solo usar Mistral si NO hay flujo activo
            System.out.println("[CHATBOT DEBUG] 🤖 Consultando Mistral...");

            // Pedir respuesta a Mistral CON CONTEXTO Y SESIÓN
            ChatbotRespuesta respuestaIA = mistralClientService.responderConContexto(mensaje, historialReciente, session);

            System.out.println("[CHATBOT DEBUG] MistralAI respondió - Acción: '" + respuestaIA.getAccion() + "'");
            System.out.println("[CHATBOT DEBUG] MistralAI respondió - Texto: '" + respuestaIA.getRespuesta() + "'");


            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMensaje(mensaje);
            chatRequest.setAccion(respuestaIA.getAccion());
            chatRequest.setParametros(respuestaIA.getParametros());

            System.out.println("[CHATBOT DEBUG] Enviando al ActionDispatcher - Acción: '" + chatRequest.getAccion() + "'");

            // Enviar la acción al ActionDispatcher
            ChatbotRespuesta respuestaFinal = (ChatbotRespuesta) actionDispatcher.dispatch(chatRequest, session).getBody();

            System.out.println("[CHATBOT DEBUG] ActionDispatcher respondió: '" + respuestaFinal.getRespuesta() + "'");

            // Guardar mensaje del bot en base de datos
            ChatMensaje msgBot = new ChatMensaje();
            msgBot.setUsuario(usuario);
            msgBot.setRol(ChatMensaje.RolMensaje.BOT);
            msgBot.setContenido(respuestaFinal.getRespuesta());
            msgBot.setIdConversacion(idConversacion);
            msgBot.setFecha(LocalDateTime.now());
            chatMensajeRepository.save(msgBot);

            System.out.println("═══════════════════════════════════════════════");

            return ResponseEntity.ok(respuestaFinal);

        } catch (Exception e) {
            System.err.println("[CHATBOT ERROR] Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();

            ChatbotRespuesta error = new ChatbotRespuesta();
            error.setRespuesta("❌ Hubo un error al procesar tu mensaje.");
            error.setAccion("ninguna");
            error.setParametros(Map.of());
            error.setRelevante_para(new String[]{});
            error.setNivel_confianza(0.0);
            return ResponseEntity.ok(error);
        }
    }

    // 🚨 NUEVO: Método para detectar intención de cancelación
    private boolean esIntencionDeCancelar(String mensaje, HttpSession session) {
        String msg = mensaje.toLowerCase().trim();

        // 🔧 FIX: Obtener contexto del flujo actual
        String flujoActivo = (String) session.getAttribute("flujoActivo");
        String pasoCancelacion = (String) session.getAttribute("pasoCancelacion");

        // 🔧 FIX: En flujo de cancelación, ser más específico sobre qué constituye cancelación
        if ("cancelar_reserva".equals(flujoActivo)) {
            // En estos pasos específicos, NO tratar como cancelación general
            if ("ingresando_motivo".equals(pasoCancelacion) ||
                    "confirmando_motivo_ambiguo".equals(pasoCancelacion)) {

                // Solo cancelar si usa frases MUY específicas para salir del proceso
                String[] cancelacionesEspecificas = {
                        "cancelar proceso", "salir del proceso", "terminar proceso",
                        "no quiero continuar", "abandonar proceso", "detener proceso"
                };

                for (String frase : cancelacionesEspecificas) {
                    if (msg.contains(frase)) {
                        return true;
                    }
                }

                // En estos pasos, "ya no quiero", "no quiero" etc. NO son cancelación
                return false;
            }
        }

        // 🔧 Para otros flujos o pasos, usar la lógica original
        String[] palabrasCancelacion = {
                "cancelar", "cancel", "salir", "exit", "no quiero continuar",
                "no gracias", "terminar", "parar", "stop", "atrás", "volver",
                "abandonar", "dejar", "mejor no", "olvídalo",
                "olvidalo", "mejor otro día", "después", "luego", "más tarde",
                "ya no quiero", "ya no", "mentira"
        };

        for (String palabra : palabrasCancelacion) {
            if (msg.contains(palabra)) {
                return true;
            }
        }

        // Verificar respuestas negativas simples solo en contextos apropiados
        if ((msg.equals("no") || msg.equals("nop") || msg.equals("nope")) &&
                !"ingresando_motivo".equals(pasoCancelacion)) {
            return true;
        }

        return false;
    }


    @GetMapping("/historial")
    @ResponseBody
    public ResponseEntity<?> obtenerHistorial(HttpSession session) {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado"));
        }

        String idConversacion = (String) session.getAttribute("idConversacionActual");
        List<ChatMensaje> historial = chatMensajeRepository
                .findTop15ByUsuarioAndIdConversacionOrderByFechaDesc(usuario, idConversacion)
                .stream()
                .sorted(Comparator.comparing(ChatMensaje::getFecha))
                .toList();

        List<Map<String, String>> mensajes = historial.stream().map(m -> Map.of(
                "rol", m.getRol().toString(),
                "contenido", m.getContenido(),
                "fecha", m.getFecha().toString()
        )).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("mensajes", mensajes);
        response.put("nombre", usuario.getNombres());

        return ResponseEntity.ok(response);
    }
}