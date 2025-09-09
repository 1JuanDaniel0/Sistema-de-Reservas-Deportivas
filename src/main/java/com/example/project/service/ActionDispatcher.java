package com.example.project.service;

import com.example.project.dto.ChatRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ActionDispatcher {

    @Autowired
    private ChatbotService chatbotService;

    public ResponseEntity<?> dispatch(ChatRequest chatRequest, HttpSession session) {
        String accion = chatRequest.getAccion();

        // Manejar cancelación de flujo
        if ("cancelar_flujo".equalsIgnoreCase(accion)) {
            // Limpiar cualquier flujo activo
            session.removeAttribute("flujoActivo");
            session.removeAttribute("datosTemporales");

            // Marcar para limpiar contexto en próxima solicitud
            session.setAttribute("limpiarContexto", true);

            return chatbotService.respuestaSimple(
                    "✅ Proceso cancelado. ¿En qué más puedo ayudarte?",
                    "ninguna"
            );
        }

        // Siempre redirigir crear_reserva a ver_disponibilidad primero
        if ("crear_reserva".equalsIgnoreCase(accion)) {
            // Cambiar la acción para que pase por verificar disponibilidad
            chatRequest.setAccion("ver_disponibilidad");
            return chatbotService.flujoVerDisponibilidad(chatRequest, session);
        }

        if ("ver_disponibilidad".equalsIgnoreCase(accion)) {
            return chatbotService.flujoVerDisponibilidad(chatRequest, session);
        }

        if ("confirmar_reserva".equalsIgnoreCase(accion)) {
            // Limpiar contexto después de confirmar reserva
            ResponseEntity<?> respuesta = chatbotService.flujoCrearReserva(chatRequest, session);
            session.setAttribute("limpiarContexto", true);
            return respuesta;
        }

        if ("listar_lugares".equalsIgnoreCase(accion)) {
            session.setAttribute("limpiarContexto", true);
            return chatbotService.flujoListarLugares(chatRequest, session);
        }

        if ("listar_espacios".equalsIgnoreCase(accion)) {
            session.setAttribute("limpiarContexto", true);
            return chatbotService.flujoListarEspacios(chatRequest, session);
        }

        if ("ver_reservas".equalsIgnoreCase(accion)) {
            // Limpiar contexto antes de ver reservas
            session.setAttribute("limpiarContexto", true);
            return chatbotService.flujoVerReservas(chatRequest, session);
        }

        if ("cancelar_reserva".equalsIgnoreCase(accion)) {
            return chatbotService.flujoCancelarReserva(chatRequest, session);
        }

        // Manejar error temporal (429)
        if ("error_temporal".equalsIgnoreCase(accion)) {
            session.setAttribute("limpiarContexto", true);
            return chatbotService.respuestaSimple(
                    "⚠️ Hemos recibido muchas solicitudes. Intenta nuevamente en unos segundos.",
                    "ninguna"
            );
        }

        // Acción desconocida o no soportada
        return chatbotService.respuestaSimple("Lo siento, no entendí tu solicitud 🙁", "ninguna");
    }
}