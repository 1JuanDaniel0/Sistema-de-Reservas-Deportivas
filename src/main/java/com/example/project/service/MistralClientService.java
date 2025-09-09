package com.example.project.service;

import com.example.project.dto.ChatbotRespuesta;
import com.example.project.entity.ChatMensaje;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MistralClientService {

    @Value("${mistral.api.url:https://api.mistral.ai/v1/chat/completions}")
    private String apiUrl;

    @Value("${mistral.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
Eres "Asistente Deportivo", el bot oficial del sistema de reservas deportivas de la Municipalidad de San Miguel.

🎯 Tu objetivo es ayudar a los vecinos a:
- Consultar disponibilidad de espacios deportivos
- Crear reservas (SIEMPRE después de verificar disponibilidad)
- Ver reservas existentes
- Cancelar reservas

🚨🚨🚨 FORMATO DE RESPUESTA OBLIGATORIO 🚨🚨🚨
DEBES responder ÚNICAMENTE con un JSON válido. 
PROHIBIDO escribir texto antes o después del JSON.
PROHIBIDO usar markdown, explicaciones o comentarios.
TU RESPUESTA COMPLETA DEBE SER SOLO UN JSON QUE EMPIECE CON { Y TERMINE CON }.

ESTRUCTURA JSON OBLIGATORIA:
{
  "respuesta": "tu mensaje aquí",
  "accion": "una_accion_valida",
  "parametros": {},
  "relevante_para": ["categoria"],
  "nivel_confianza": 0.9
}

Acciones válidas ÚNICAMENTE:
- "ver_disponibilidad" (para consultar y crear reservas)
- "ver_reservas" (para mostrar reservas del usuario)
- "cancelar_reserva" (para eliminar reservas)
- "confirmar_reserva" (para confirmar reservas)
- "listar_lugares" (para mostrar lugares disponibles)
- "listar_espacios" (para mostrar espacios deportivos)
- "cancelar_flujo" (para salir de procesos)
- "ninguna" (para respuestas generales)

EJEMPLOS EXACTOS QUE DEBES SEGUIR:

Si usuario dice "ver mis reservas", "mis reservas", "qué reservas tengo":
{
  "respuesta": "Te muestro tus reservas actuales.",
  "accion": "ver_reservas",
  "parametros": {},
  "relevante_para": ["reservas"],
  "nivel_confianza": 0.95
}

Si usuario dice "quiero hacer reserva", "reservar":
{
  "respuesta": "¡Perfecto! Te ayudo a verificar la disponibilidad. ¿En qué lugar deportivo te gustaría reservar?",
  "accion": "ver_disponibilidad",
  "parametros": {},
  "relevante_para": ["reservas"],
  "nivel_confianza": 0.9
}

Si usuario dice "cancelar", "salir", "no quiero":
{
  "respuesta": "✅ Proceso cancelado. ¿En qué más puedo ayudarte?",
  "accion": "cancelar_flujo",
  "parametros": {},
  "relevante_para": ["general"],
  "nivel_confianza": 1.0
}

Si usuario dice "cancelar reserva":
{
  "respuesta": "Te ayudo a cancelar una de tus reservas. ¡Veamos cuáles puedes cancelar!",
  "accion": "cancelar_reserva",
  "parametros": {},
  "relevante_para": ["cancelacion"],
  "nivel_confianza": 0.9
}

Si usuario dice "lugares disponibles" o "ver lugares":
{
  "respuesta": "Estos son los lugares disponibles.",
  "accion": "listar_lugares",
  "parametros": {},
  "relevante_para": ["reservas"],
  "nivel_confianza": 0.9
}

Si usuario dice "ver espacios" o "qué espacios hay":
{
  "respuesta": "Estos son los espacios deportivos registrados.",
  "accion": "listar_espacios",
  "parametros": {},
  "relevante_para": ["reservas"],
  "nivel_confianza": 0.9
}

DETECCIÓN DE CANCELACIÓN:
Si usuario dice: "cancelar", "salir", "ya no quiero, "no quiero", "ya no", "terminar", "parar", "no gracias", "mejor no", "olvídalo", "atrás", "volver", "no", "nop" → SIEMPRE usar acción "cancelar_flujo"

Temas NO permitidos:
Si pregunta algo no relacionado con reservas deportivas, responde:
{
  "respuesta": "Lo siento, solo puedo ayudarte con temas relacionados con reservas deportivas 🏟️.",
  "accion": "ninguna",
  "parametros": {},
  "relevante_para": ["general"],
  "nivel_confianza": 1.0
}

REGLA FINAL: Tu respuesta DEBE ser SOLO el JSON. Sin texto extra, sin explicaciones, sin markdown.
""";

    // 🆕 NUEVO: Método con soporte para contexto limpio
    public ChatbotRespuesta responderConContexto(String prompt, List<ChatMensaje> historial, HttpSession session) throws Exception {
        try {
            // 🚨 NUEVO: Verificar si se debe limpiar el contexto (con validación de sesión)
            Boolean limpiarContexto = null;
            if (session != null) {
                limpiarContexto = (Boolean) session.getAttribute("limpiarContexto");
            }

            if (Boolean.TRUE.equals(limpiarContexto)) {
                System.out.println("[Mistral DEBUG] 🧹 CONTEXTO LIMPIO - Enviando solo mensaje actual");
                session.removeAttribute("limpiarContexto"); // Limpiar bandera después de usar
                return responderSinContexto(prompt);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 🔧 FIX: Construir mensajes con contexto limitado y limpio
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

            // 🔧 FIX: Filtrar historial y limpiar contenido HTML (máximo 4 mensajes)
            int maxHistorial = Math.min(historial.size(), 4);
            for (int i = Math.max(0, historial.size() - maxHistorial); i < historial.size(); i++) {
                ChatMensaje msg = historial.get(i);
                String role = msg.getRol() == ChatMensaje.RolMensaje.USUARIO ? "user" : "assistant";

                // 🚨 NUEVO: Limpiar contenido HTML del contexto
                String contenidoLimpio = limpiarContenidoParaContexto(msg.getContenido());

                // Solo agregar si el contenido limpio es relevante
                if (!contenidoLimpio.trim().isEmpty() && contenidoLimpio.length() < 300) {
                    messages.add(Map.of("role", role, "content", contenidoLimpio));
                }
            }

            // Agregar mensaje actual
            messages.add(Map.of("role", "user", "content", prompt));

            System.out.println("[Mistral DEBUG] Enviando " + messages.size() + " mensajes (incluyendo system prompt)");
            System.out.println("[Mistral DEBUG] Último contexto limpio: " +
                    (messages.size() > 2 ? messages.get(messages.size() - 2).get("content") : "Sin historial"));

            return enviarSolicitudAMistral(messages);

        } catch (HttpClientErrorException httpEx) {
            // 🆕 NUEVO: Manejo específico del error 429 (Too Many Requests)
            if (httpEx.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                System.err.println("[Mistral ERROR 429] Demasiadas solicitudes - Límite excedido");
                return crearRespuestaError429();
            }

            System.err.println("[Mistral HTTP ERROR] Código: " + httpEx.getStatusCode());
            System.err.println("[Mistral HTTP ERROR] Cuerpo: " + httpEx.getResponseBodyAsString());
            throw httpEx;

        } catch (HttpServerErrorException httpEx) {
            System.err.println("[Mistral SERVER ERROR] Código: " + httpEx.getStatusCode());
            System.err.println("[Mistral SERVER ERROR] Cuerpo: " + httpEx.getResponseBodyAsString());
            throw httpEx;

        } catch (ResourceAccessException netEx) {
            System.err.println("[Mistral NETWORK ERROR] No se pudo acceder a la API de Mistral: " + netEx.getMessage());
            throw netEx;

        } catch (Exception e) {
            System.err.println("[Mistral EXCEPTION] Error inesperado: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // 🆕 NUEVO: Método para responder sin contexto (contexto limpio)
    private ChatbotRespuesta responderSinContexto(String prompt) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", prompt));

        System.out.println("[Mistral DEBUG] 🧹 SOLICITUD LIMPIA - Solo 2 mensajes (system + user)");
        return enviarSolicitudAMistral(messages);
    }

    // 🔧 FIX: Método centralizado para enviar solicitudes a Mistral
    private ChatbotRespuesta enviarSolicitudAMistral(List<Map<String, String>> messages) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "mistral-small",
                "temperature", 0.7,
                "max_tokens", 500, // 🔧 FIX: Limitar tokens para evitar respuestas largas
                "messages", messages
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

        System.out.println("[Mistral DEBUG] Código de respuesta: " + response.getStatusCode());
        System.out.println("[Mistral DEBUG] Respuesta bruta: " + response.getBody());

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode contentNode = root.path("choices").get(0).path("message").path("content");

        if (contentNode == null || contentNode.isMissingNode()) {
            System.err.println("[Mistral ERROR] Nodo 'content' no encontrado en respuesta.");
            throw new RuntimeException("La respuesta de Mistral no contiene contenido");
        }

        String content = contentNode.asText();
        System.out.println("[Mistral DEBUG] Contenido procesado: " + content);

        // 🔧 FIX: Extraer solo el JSON válido del contenido
        String jsonContent = extractJson(content);
        System.out.println("[Mistral DEBUG] JSON extraído: " + jsonContent);

        return mapper.readValue(jsonContent, ChatbotRespuesta.class);
    }

    // 🆕 NUEVO: Crear respuesta específica para error 429
    private ChatbotRespuesta crearRespuestaError429() {
        ChatbotRespuesta respuesta = new ChatbotRespuesta();
        respuesta.setRespuesta("⚠️ Hemos recibido muchas solicitudes. Intenta nuevamente en unos segundos.");
        respuesta.setAccion("error_temporal");
        respuesta.setParametros(Map.of());
        return respuesta;
    }

    // 🚨 NUEVO: Método para limpiar contenido HTML del contexto
    private String limpiarContenidoParaContexto(String contenido) {
        if (contenido == null) return "";

        // Si contiene HTML de reservas cancelables, resumir en texto plano
        if (contenido.contains("<div class='reservas-cancelables'>")) {
            return "📋 Se mostraron las reservas cancelables del usuario.";
        }

        // Si contiene HTML de reservas, resumir en texto plano
        if (contenido.contains("<div class='reservas-lista'>")) {
            return "📋 El usuario consultó sus reservas activas.";
        }

        // Si contiene HTML de disponibilidad, resumir
        if (contenido.contains("disponibilidad-resultado")) {
            return "📅 Se mostró disponibilidad de espacios deportivos.";
        }

        // Si contiene HTML de confirmación de cancelación
        if (contenido.contains("<div class='confirmacion-cancelacion'>")) {
            return "❓ Se mostró confirmación de cancelación al usuario.";
        }

        // Si contiene HTML de solicitud de cancelación
        if (contenido.contains("<div class='solicitud-cancelacion'>")) {
            return "📝 Se solicitó motivo de cancelación al usuario.";
        }

        // Limpiar otros tags HTML generales
        String limpio = contenido.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // Limitar longitud para evitar sobrecargar el contexto
        if (limpio.length() > 150) {
            limpio = limpio.substring(0, 150) + "...";
        }

        return limpio;
    }

    // 🔧 FIX: Método para extraer JSON válido del contenido de MistralAI
    private String extractJson(String content) {
        try {
            // Buscar el primer { y el último }
            int startIndex = content.indexOf('{');
            int lastIndex = content.lastIndexOf('}');

            if (startIndex != -1 && lastIndex != -1 && startIndex < lastIndex) {
                String jsonPart = content.substring(startIndex, lastIndex + 1);
                System.out.println("[Mistral DEBUG] JSON encontrado entre posiciones " + startIndex + " y " + lastIndex);

                // Validar que sea JSON válido intentando parsearlo
                try {
                    mapper.readTree(jsonPart);
                    return jsonPart;
                } catch (Exception e) {
                    System.err.println("[Mistral WARN] JSON extraído no es válido, intentando fallback...");
                }
            }

            // 🔧 NUEVO: Fallback inteligente basado en contenido
            System.err.println("[Mistral ERROR] No se pudo extraer JSON válido del contenido: " + content);
            return createIntelligentFallbackResponse(content);

        } catch (Exception e) {
            System.err.println("[Mistral ERROR] Error al extraer JSON: " + e.getMessage());
            return createIntelligentFallbackResponse(content);
        }
    }

    // 🆕 NUEVO: Fallback inteligente que detecta patrones en la respuesta de texto
    private String createIntelligentFallbackResponse(String originalContent) {
        String lowerContent = originalContent.toLowerCase();

        // 🔍 Detectar si habla de reservas del usuario
        if (lowerContent.contains("reservas actuales") ||
                lowerContent.contains("tus reservas") ||
                lowerContent.contains("mostrar reservas") ||
                lowerContent.contains("te muestro")) {

            return """
        {
            "respuesta": "Te muestro tus reservas actuales.",
            "accion": "ver_reservas",
            "parametros": {},
            "relevante_para": ["reservas"],
            "nivel_confianza": 0.8
        }
        """;
        }

        // 🔍 Detectar si habla de disponibilidad
        if (lowerContent.contains("disponibilidad") ||
                lowerContent.contains("verificar") ||
                lowerContent.contains("lugar deportivo")) {

            return """
        {
            "respuesta": "¡Perfecto! Te ayudo a verificar la disponibilidad. ¿En qué lugar deportivo te gustaría reservar?",
            "accion": "ver_disponibilidad",
            "parametros": {},
            "relevante_para": ["reservas"],
            "nivel_confianza": 0.8
        }
        """;
        }

        // 🔍 Detectar si habla de cancelación
        if (lowerContent.contains("cancelar") ||
                lowerContent.contains("proceso cancelado") ||
                lowerContent.contains("salir")) {

            return """
        {
            "respuesta": "✅ Proceso cancelado. ¿En qué más puedo ayudarte?",
            "accion": "cancelar_flujo",
            "parametros": {},
            "relevante_para": ["general"],
            "nivel_confianza": 0.9
        }
        """;
        }

        // 🔧 Fallback genérico mejorado
        String cleanText = originalContent
                .replaceAll("[{}\\[\\]\"\\n\\r\\t]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleanText.length() > 150) {
            cleanText = cleanText.substring(0, 150) + "...";
        }
        if (cleanText.isEmpty()) {
            cleanText = "Lo siento, hubo un problema al procesar tu solicitud.";
        }

        cleanText = cleanText.replace("\"", "\\\"");

        return String.format("""
    {
        "respuesta": "%s",
        "accion": "ninguna",
        "parametros": {},
        "relevante_para": ["general"],
        "nivel_confianza": 0.3
    }
    """, cleanText);
    }

    // 🔧 FIX: Crear respuesta de emergencia cuando MistralAI no devuelve JSON válido
    private String createFallbackResponse(String originalContent) {
        // Extraer solo el texto limpio sin caracteres problemáticos
        String cleanText = originalContent
                .replaceAll("[{}\\[\\]\"\\n\\r\\t]", " ")  // 🔧 FIX: Remover más caracteres problemáticos
                .replaceAll("\\s+", " ")  // Múltiples espacios a uno solo
                .trim();

        if (cleanText.length() > 150) {
            cleanText = cleanText.substring(0, 150) + "...";
        }
        if (cleanText.isEmpty()) {
            cleanText = "Lo siento, hubo un problema al procesar tu solicitud.";
        }

        // 🔧 FIX: Escapar correctamente las comillas
        cleanText = cleanText.replace("\"", "\\\"");

        return String.format("""
        {
            "respuesta": "%s",
            "accion": "ninguna",
            "parametros": {},
            "relevante_para": ["general"],
            "nivel_confianza": 0.5
        }
        """, cleanText);
    }

    // 🔧 UPDATED: Actualizar método de compatibilidad para evitar sesión nula
    public ChatbotRespuesta responderConContexto(String prompt, List<ChatMensaje> historial) throws Exception {
        // Llamar al método principal sin funcionalidad de sesión
        return responderConContexto(prompt, historial, null);
    }
}