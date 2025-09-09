package com.example.project.service;

import com.example.project.controller.PagoMercadoPagoController;
import com.example.project.dto.ChatRequest;
import com.example.project.dto.ChatbotRespuesta;
import com.example.project.dto.DetallePagoDTO;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.vecino.EstadoReservaRepositoryVecino;
import com.example.project.repository.vecino.ReservaRepositoryVecino;
import com.mercadopago.MercadoPagoConfig;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private EstadoReservaRepositoryVecino estadoReservaRepository;
    @Autowired private ReservaRepositoryVecino reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private LugarRepository lugarRepository;
    @Autowired private MailManager mailManager;
    @Autowired private PagoMercadoPagoController pagoMercadoPagoController;

    public ResponseEntity<ChatbotRespuesta> flujoVerDisponibilidad(ChatRequest request, HttpSession session) {
        System.out.println("[FLUJO DEBUG] ═══ INICIANDO FLUJO VER DISPONIBILIDAD ═══");
        System.out.println("[FLUJO DEBUG] Mensaje: '" + request.getMensaje() + "'");

        // 🔧 FIX: Establecer flujo activo en sesión
        session.setAttribute("flujoActivo", "ver_disponibilidad");

        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        // 🔧 FIX: Obtener datos de sesión y combinar con parámetros nuevos
        Map<String, String> datos = new HashMap<>();

        // Primero cargar datos existentes de la sesión
        Map<String, Object> datosExistentes = (Map<String, Object>) session.getAttribute("datosTemporales");
        if (datosExistentes != null) {
            datosExistentes.forEach((k, v) -> {
                if (v != null) datos.put(k, v.toString());
            });
            System.out.println("[FLUJO DEBUG] Datos cargados de sesión: " + datos);
        }

        // Luego agregar/sobrescribir con nuevos parámetros
        if (request.getParametros() != null) {
            request.getParametros().forEach((k, v) -> {
                if (v != null) datos.put(k, v.toString());
            });
        }

        System.out.println("[FLUJO DEBUG] Parámetros finales: " + datos);
        String userInput = request.getMensaje().toLowerCase().trim();


        // Paso 1: Preguntar lugar si falta
        if (!datos.containsKey("lugar")) {
            List<Lugar> lugares = lugarRepository.findAll();

            System.out.println("[FLUJO DEBUG] Buscando lugar en mensaje: '" + userInput + "'");

            // 🔧 FIX: Buscar lugar en el mensaje del usuario
            Lugar lugarElegido = lugares.stream()
                    .filter(l -> {
                        String lugarNombre = l.getLugar().toLowerCase();
                        boolean contiene = userInput.contains(lugarNombre);
                        System.out.println("[FLUJO DEBUG] Comparando '" + lugarNombre + "' con mensaje: " + contiene);
                        return contiene;
                    })
                    .findFirst()
                    .orElse(null);

            if (lugarElegido != null) {
                datos.put("lugar", lugarElegido.getLugar());
                System.out.println("[FLUJO DEBUG] ✅ Lugar encontrado automáticamente: " + lugarElegido.getLugar());

                // 🔧 FIX: Usar transición automática en lugar de recursión
                session.setAttribute("datosTemporales", convertirParametros(datos));
                return continuarFlujoDisponibilidad(datos, "lugar", session);
            } else {
                System.out.println("[FLUJO DEBUG] ❌ Lugar no encontrado, mostrando opciones");
                Map<String, String> sugerencias = lugares.stream()
                        .collect(Collectors.toMap(Lugar::getLugar, Lugar::getLugar));

                ChatbotRespuesta r = new ChatbotRespuesta();
                r.setRespuesta("¿En qué lugar te gustaría consultar disponibilidad? 🏟️\n\n📍 Lugares disponibles:");
                r.setAccion("ver_disponibilidad");
                r.setParametros(Map.of("paso", "pedir_lugar"));
                r.setRelevante_para(new String[]{"reservas", "disponibilidad"});
                r.setNivel_confianza(0.9);
                r.setSugerencias(sugerencias);
                return ResponseEntity.ok(r);
            }
        }

        System.out.println("[FLUJO DEBUG] Lugar ya existe: " + datos.get("lugar"));

        // Paso 2: Preguntar idEspacio
        if (!datos.containsKey("idEspacio")) {
            String lugarSeleccionado = datos.get("lugar");
            List<Espacio> espacios;

            try {
                // 🔧 FIX: Corregir el filtro para usar el campo correcto
                espacios = espacioRepository.findAll().stream()
                        .filter(esp -> esp.getIdLugar().getLugar().equals(lugarSeleccionado))
                        .collect(Collectors.toList());

                System.out.println("[FLUJO DEBUG] Espacios encontrados para '" + lugarSeleccionado + "': " + espacios.size());
                espacios.forEach(e -> System.out.println("[FLUJO DEBUG] - " + e.getNombre()));
            } catch (Exception e) {
                System.err.println("[FLUJO DEBUG] Error buscando espacios: " + e.getMessage());
                espacios = new ArrayList<>();
            }

            if (espacios.isEmpty()) {
                session.removeAttribute("flujoActivo");
                session.removeAttribute("datosTemporales");
                return respuestaJson("No se encontraron espacios en " + lugarSeleccionado, "ninguna", Map.of());
            }

            // 🔧 FIX: Buscar espacio por nombre en el mensaje del usuario
            System.out.println("[FLUJO DEBUG] Buscando espacio en mensaje: '" + userInput + "'");

            Espacio espacioElegido = espacios.stream()
                    .filter(e -> {
                        String nombreEspacio = e.getNombre().toLowerCase();
                        boolean contiene = userInput.contains(nombreEspacio);
                        System.out.println("[FLUJO DEBUG] Comparando espacio '" + nombreEspacio + "' con mensaje: " + contiene);
                        return contiene;
                    })
                    .findFirst()
                    .orElse(null);

            if (espacioElegido != null) {
                datos.put("idEspacio", String.valueOf(espacioElegido.getIdEspacio()));
                System.out.println("[FLUJO DEBUG] ✅ Espacio encontrado automáticamente: " + espacioElegido.getNombre());

                // 🔧 FIX: Usar transición automática en lugar de recursión
                session.setAttribute("datosTemporales", convertirParametros(datos));
                return transicionAutomaticaDisponibilidad(datos, "idEspacio", session);
            } else {
                System.out.println("[FLUJO DEBUG] ❌ Espacio no encontrado, mostrando opciones");

                // 🔧 FIX: Las sugerencias deben mostrar el nombre pero internamente guardar el ID
                Map<String, String> sugerenciasEspacios = espacios.stream()
                        .collect(Collectors.toMap(
                                Espacio::getNombre, // Clave: nombre visible
                                e -> String.valueOf(e.getIdEspacio()) // Valor: ID interno
                        ));

                Map<String, String> parametros = new HashMap<>();
                parametros.put("paso", "pedir_espacio");
                parametros.put("lugar", lugarSeleccionado);

                // 🔧 FIX: Guardar datos en sesión
                session.setAttribute("datosTemporales", convertirParametros(datos));

                ChatbotRespuesta r = new ChatbotRespuesta();
                r.setRespuesta("¿Qué espacio deportivo dentro de " + lugarSeleccionado + " deseas consultar?");
                r.setAccion("ver_disponibilidad");
                r.setParametros(convertirParametros(parametros));
                r.setRelevante_para(new String[]{"reservas", "disponibilidad"});
                r.setNivel_confianza(0.9);
                r.setSugerencias(sugerenciasEspacios);
                return ResponseEntity.ok(r);
            }
        }

        // Paso 3: Preguntar fecha
        if (!datos.containsKey("fecha")) {
            String resultado = interpretarFecha(userInput, session);
            switch (resultado) {
                case "NEED_MONTH" -> {
                    session.setAttribute("datosTemporales", convertirParametros(datos));
                    return respuestaJson("¿De qué mes es? (en número)", "ver_disponibilidad", Map.of(
                            "paso", "pedir_fecha",
                            "lugar", datos.get("lugar"),
                            "idEspacio", datos.get("idEspacio")
                    ));
                }
                case "NEED_YEAR" -> {
                    session.setAttribute("datosTemporales", convertirParametros(datos));
                    return respuestaJson("¿De qué año?", "ver_disponibilidad", Map.of(
                            "paso", "pedir_fecha",
                            "lugar", datos.get("lugar"),
                            "idEspacio", datos.get("idEspacio")
                    ));
                }
                case "PAST" -> {
                    session.setAttribute("datosTemporales", convertirParametros(datos));
                    return respuestaJson("La fecha indicada no es válida o ya pasó. Intenta con otra fecha (YYYY-MM-DD)", "ver_disponibilidad", Map.of(
                            "paso", "pedir_fecha",
                            "lugar", datos.get("lugar"),
                            "idEspacio", datos.get("idEspacio")
                    ));
                }
                case "ASK_MONTH", "ASK_YEAR", "INVALID" -> {
                    session.setAttribute("datosTemporales", convertirParametros(datos));
                    return respuestaJson("No entendí la fecha. Por favor ingrésala en formato válido.", "ver_disponibilidad", Map.of(
                            "paso", "pedir_fecha",
                            "lugar", datos.get("lugar"),
                            "idEspacio", datos.get("idEspacio")
                    ));
                }
                default -> {
                    datos.put("fecha", resultado);
                    session.setAttribute("datosTemporales", convertirParametros(datos));
                    return continuarFlujoDisponibilidad(datos, "fecha", session);
                }
            }
        }

        // Paso 4: Preguntar horaInicio y horaFin
        if (!datos.containsKey("horaInicio") || !datos.containsKey("horaFin")) {
            Map<String, String> horarios = parseHorarios(userInput);

            if (horarios.containsKey("horaInicio") && horarios.containsKey("horaFin")) {
                int hi = Integer.parseInt(horarios.get("horaInicio"));
                int hf = Integer.parseInt(horarios.get("horaFin"));
                LocalDate fechaTmp = LocalDate.parse(datos.get("fecha"));
                LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
                int horaActual = LocalTime.now(ZoneId.of("America/Lima")).getHour();
                if (fechaTmp.equals(hoy) && hi <= horaActual) {
                    session.setAttribute("datosTemporales", convertirParametros(datos));
                    return respuestaJson("La hora de inicio ya pasó. Indica un nuevo horario válido.", "ver_disponibilidad", Map.of(
                            "paso", "pedir_horario",
                            "lugar", datos.get("lugar"),
                            "idEspacio", datos.get("idEspacio"),
                            "fecha", datos.get("fecha")
                    ));
                }
                datos.put("horaInicio", String.valueOf(hi));
                datos.put("horaFin", String.valueOf(hf));
                session.setAttribute("datosTemporales", convertirParametros(datos));
                return continuarFlujoDisponibilidad(datos, "horario", session);
            } else {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("paso", "pedir_horario");
                parametros.put("lugar", datos.get("lugar"));
                parametros.put("idEspacio", datos.get("idEspacio"));
                parametros.put("fecha", datos.get("fecha"));

                session.setAttribute("datosTemporales", convertirParametros(datos));
                Map<String, String> sugerenciasHorario = Map.of(
                        "De 9am a 11am", "De 9am a 11am",
                        "De 2pm a 4pm", "De 2pm a 4pm",
                        "De 6pm a 8pm", "De 6pm a 8pm"
                );

                ChatbotRespuesta r = new ChatbotRespuesta();
                r.setRespuesta("¿Qué horario deseas consultar? ⏰ Indica la hora de inicio y fin (Ej: de 8am a 9am, 14-16)");
                r.setAccion("ver_disponibilidad");
                r.setParametros(convertirParametros(parametros));
                r.setRelevante_para(new String[]{"reservas", "disponibilidad"});
                r.setNivel_confianza(0.9);
                r.setSugerencias(sugerenciasHorario);
                return ResponseEntity.ok(r);
            }
        }

        // Verificar disponibilidad
        try {
            int espacioId = Integer.parseInt(datos.get("idEspacio"));
            LocalDate fecha = LocalDate.parse(datos.get("fecha"));
            int horaInicio = Integer.parseInt(datos.get("horaInicio"));
            int horaFin = Integer.parseInt(datos.get("horaFin"));

            List<Reserva> conflictos = reservaRepository.findConflictosEnHorario(
                    espacioId,
                    fecha,
                    LocalTime.of(horaInicio, 0),
                    LocalTime.of(horaFin, 0)
            );

            if (!conflictos.isEmpty()) {
                session.removeAttribute("flujoActivo");
                session.removeAttribute("datosTemporales");
                return respuestaJson(
                        "❌ Lo siento, el espacio no está disponible en ese horario. Intenta con otro horario o fecha.",
                        "ninguna",
                        Map.of()
                );
            }

            // 🔧 FIX: Espacio disponible - preguntar si quiere reservar
            // ❌ NO limpiar flujo, cambiar a confirmar_reserva
            session.setAttribute("flujoActivo", "confirmar_reserva"); // 🔧 CAMBIAR A confirmar_reserva
            session.setAttribute("datosTemporales", convertirParametros(datos)); // 🔧 MANTENER DATOS

            Espacio esp = espacioRepository.findById(espacioId).orElse(null);
            int horas = horaFin - horaInicio;
            double costoEstimado = (esp != null ? esp.getCosto() * horas : 0);

            StringBuilder html = new StringBuilder();
            html.append("<div class='disponibilidad-resultado'>");
            html.append("<div>✅ <strong>¡Espacio disponible!</strong></div>");
            html.append("<div>📍 Lugar: ").append(datos.get("lugar")).append("</div>");
            html.append("<div>📅 Fecha: ").append(datos.get("fecha")).append("</div>");
            html.append("<div>🕒 Horario: ").append(horaInicio).append(":00 - ").append(horaFin).append(":00</div>");
            html.append("<div>💰 Costo estimado: S/").append(String.format("%.2f", costoEstimado)).append("</div>");
            html.append("</div>");
            html.append("<div style='margin-top:10px;'>¿Deseas crear la reserva con estos datos?</div>");

            Map<String, String> sugerenciasRespuesta = Map.of(
                    "Sí, reservar", "sí",
                    "No, cancelar", "no"
            );

            ChatbotRespuesta r = new ChatbotRespuesta();
            r.setRespuesta(html.toString());
            r.setAccion("confirmar_reserva");
            r.setParametros(convertirParametros(datos));
            r.setRelevante_para(new String[]{"reservas", "crear"});
            r.setNivel_confianza(0.95);
            r.setSugerencias(sugerenciasRespuesta);
            return ResponseEntity.ok(r);

        } catch (Exception e) {
            session.removeAttribute("flujoActivo");
            session.removeAttribute("datosTemporales");
            return respuestaJson(
                    "❌ Error al procesar los datos. Verifica el formato de fecha y horarios.",
                    "ninguna",
                    Map.of()
            );
        }
    }

    public ResponseEntity<ChatbotRespuesta> flujoCrearReserva(ChatRequest request, HttpSession session) {
        System.out.println("[FLUJO DEBUG] ═══ INICIANDO FLUJO CREAR RESERVA ═══");
        System.out.println("[FLUJO DEBUG] Mensaje: '" + request.getMensaje() + "'");

        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        // 🔧 FIX: Obtener datos de sesión primero
        Map<String, String> datos = new HashMap<>();

        // Cargar datos existentes de la sesión
        Map<String, Object> datosExistentes = (Map<String, Object>) session.getAttribute("datosTemporales");
        if (datosExistentes != null) {
            datosExistentes.forEach((k, v) -> {
                if (v != null) datos.put(k, v.toString());
            });
            System.out.println("[FLUJO DEBUG] Datos cargados de sesión: " + datos);
        }

        // Agregar nuevos parámetros si los hay
        if (request.getParametros() != null) {
            request.getParametros().forEach((k, v) -> {
                if (v != null) datos.put(k, v.toString());
            });
        }

        System.out.println("[FLUJO DEBUG] Parámetros finales: " + datos);

        String userInput = request.getMensaje().toLowerCase().trim();

        // 🔧 FIX: Verificar cancelación PRIMERO
        if (userInput.contains("no") || userInput.contains("cancelar")) {
            session.removeAttribute("flujoActivo");
            session.removeAttribute("datosTemporales");
            return respuestaJson("Reserva cancelada. ¿En qué más puedo ayudarte?", "ninguna", Map.of());
        }

        // Verificar que todos los datos básicos estén presentes
        String[] camposRequeridos = {"lugar", "idEspacio", "fecha", "horaInicio", "horaFin"};
        for (String campo : camposRequeridos) {
            if (!datos.containsKey(campo)) {
                System.err.println("[FLUJO DEBUG] ❌ Falta campo requerido: " + campo);
                session.removeAttribute("flujoActivo");
                session.removeAttribute("datosTemporales");
                return respuestaJson("Faltan datos para crear la reserva. Empecemos de nuevo.", "ver_disponibilidad", Map.of());
            }
        }

        // 🔧 FIX: MANEJO DEL TIPO DE PAGO
        if (!datos.containsKey("tipoPago")) {
            String tipoPago = null;

            // Detectar tipo de pago en el mensaje
            if (userInput.contains("banco") || userInput.contains("efectivo")) {
                tipoPago = "En banco";
            } else if (userInput.contains("línea") || userInput.contains("linea") || userInput.contains("online") || userInput.contains("tarjeta")) {
                tipoPago = "En línea";
            }

            if (tipoPago != null) {
                System.out.println("[FLUJO DEBUG] ✅ Tipo de pago detectado: " + tipoPago);
                datos.put("tipoPago", tipoPago);
                // 🔧 FIX: Actualizar datos en sesión INMEDIATAMENTE
                session.setAttribute("datosTemporales", convertirParametros(datos));

                Espacio esp = espacioRepository.findById(Integer.parseInt(datos.get("idEspacio"))).orElse(null);
                int horas = Integer.parseInt(datos.get("horaFin")) - Integer.parseInt(datos.get("horaInicio"));
                double costoEstimado = esp != null ? esp.getCosto() * horas : 0;

                StringBuilder html = new StringBuilder();
                html.append("<div class='reserva-resumen'>");
                html.append("<div style='font-weight:600;'>📋 Resumen de tu reserva:</div>");
                html.append("<div>📍 Lugar: ").append(datos.get("lugar")).append("</div>");
                html.append("<div>📅 Fecha: ").append(datos.get("fecha")).append("</div>");
                html.append("<div>🕒 Horario: ").append(datos.get("horaInicio")).append(":00 - ").append(datos.get("horaFin")).append(":00</div>");
                html.append("<div>💳 Pago: ").append(tipoPago).append("</div>");
                html.append("<div>💰 Costo estimado: S/").append(String.format("%.2f", costoEstimado)).append("</div>");
                html.append("</div>");
                html.append("<div style='margin-top:10px;'>¿Confirmas que deseas crear esta reserva?</div>");

                Map<String, String> sugerenciasConfirmacion = Map.of(
                        "Sí, confirmar", "sí",
                        "No, cancelar", "no"
                );

                ChatbotRespuesta r = new ChatbotRespuesta();
                r.setRespuesta(html.toString());
                r.setAccion("confirmar_reserva");
                r.setParametros(convertirParametros(datos));
                r.setRelevante_para(new String[]{"reservas", "confirmacion"});
                r.setNivel_confianza(0.95);
                r.setSugerencias(sugerenciasConfirmacion);
                return ResponseEntity.ok(r);

            } else {
                // Preguntar método de pago
                Map<String, String> sugerenciasPago = Map.of(
                        "En banco", "En banco",
                        "En línea", "En línea"
                );

                ChatbotRespuesta r = new ChatbotRespuesta();
                r.setRespuesta("¿Cómo deseas pagar? 💳 Elige tu método de pago:");
                r.setAccion("confirmar_reserva");
                r.setParametros(convertirParametros(datos));
                r.setRelevante_para(new String[]{"reservas", "pago"});
                r.setNivel_confianza(0.9);
                r.setSugerencias(sugerenciasPago);

                return ResponseEntity.ok(r);
            }
        }

        //  Si ya tenemos tipoPago, verificar confirmación final
        if (datos.containsKey("tipoPago")) {
            // Verificar confirmación
            if (!userInput.contains("sí") && !userInput.contains("si") && !userInput.contains("reservar") && !userInput.contains("confirmar")) {
                return respuestaJson("Por favor, confirma si deseas crear la reserva respondiendo 'sí' o 'no'.", "confirmar_reserva", datos);
            }

            // 🔧 AQUÍ ES DONDE DEBES HACER EL CAMBIO
            System.out.println("[FLUJO DEBUG] ✅ Confirmación recibida. Todos los datos presentes, ejecutando reserva...");

            String tipoPago = datos.get("tipoPago");
            if ("En línea".equals(tipoPago)) {
                // Crear preferencia de MercadoPago en lugar de ejecutar reserva
                return crearPreferenciaMercadoPago(datos, vecino, session);
            } else {
                // Para pagos en banco, mantener la lógica actual
                ResponseEntity<ChatbotRespuesta> resultado = ejecutarReservaDesdeChatbot(datos, vecino);
                // Limpiar sesión después de crear la reserva
                session.removeAttribute("flujoActivo");
                session.removeAttribute("datosTemporales");
                return resultado;
            }
        }

        // 🔧 FIX: Fallback - no debería llegar aquí
        System.err.println("[FLUJO DEBUG] ❌ Estado inesperado en flujoCrearReserva");
        return respuestaJson("Hubo un error inesperado. Empecemos de nuevo.", "ver_disponibilidad", Map.of());
    }

    private ResponseEntity<ChatbotRespuesta> crearPreferenciaMercadoPago(Map<String, String> datos, Usuarios vecino, HttpSession session) {
        try {
            // Crear DetallePagoDTO a partir de los datos del chatbot
            DetallePagoDTO detalle = new DetallePagoDTO();
            detalle.setIdEspacio(Integer.parseInt(datos.get("idEspacio")));
            detalle.setFecha(datos.get("fecha"));
            detalle.setHoraInicio(Integer.parseInt(datos.get("horaInicio")));
            detalle.setHoraFin(Integer.parseInt(datos.get("horaFin")));

            // Llamar al controlador de MercadoPago
            ResponseEntity<?> resultado = pagoMercadoPagoController.crearPreferencia(detalle, session);

            if (resultado.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = (Map<String, Object>) resultado.getBody();
                String urlPago = (String) body.get("url");

                // Limpiar sesión del chatbot pero mantener datos para después del pago
                session.removeAttribute("flujoActivo");
                session.removeAttribute("datosTemporales");

                ChatbotRespuesta respuesta = new ChatbotRespuesta();
                respuesta.setRespuesta("🔗 <a href='" + urlPago + "' target='_blank' class='btn btn-primary'>Ir a MercadoPago para completar el pago</a>");
                respuesta.setAccion("redirigir_pago");
                respuesta.setParametros(Map.of("url", urlPago));
                respuesta.setRelevante_para(new String[]{"pago"});
                respuesta.setNivel_confianza(1.0);

                return ResponseEntity.ok(respuesta);
            } else {
                return respuestaJson("❌ Error al procesar el pago. Intenta nuevamente.", "ninguna", Map.of());
            }

        } catch (Exception e) {
            System.err.println("Error creando preferencia desde chatbot: " + e.getMessage());
            return respuestaJson("❌ Error al procesar el pago. Intenta nuevamente.", "ninguna", Map.of());
        }
    }

    // 🆕 NUEVO FLUJO: Cancelar reservas del usuario
    public ResponseEntity<ChatbotRespuesta> flujoCancelarReserva(ChatRequest request, HttpSession session) {
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");
        if (vecino == null) {
            return respuestaSimple("❌ Error: No has iniciado sesión.", "ninguna");
        }

        // 🔧 FIX: Marcar flujo activo SIEMPRE
        session.setAttribute("flujoActivo", "cancelar_reserva");

        // Obtener datos del flujo actual
        @SuppressWarnings("unchecked")
        Map<String, String> datosCancelacion = (Map<String, String>) session.getAttribute("datosCancelacion");
        String pasoActual = (String) session.getAttribute("pasoCancelacion");

        if (pasoActual == null) {
            // PASO 1: Mostrar reservas cancelables
            return mostrarReservasCancelables(vecino, session);
        } else if ("seleccionando_reserva".equals(pasoActual)) {
            // PASO 2: Procesar selección de reserva
            return procesarSeleccionReserva(request.getMensaje(), vecino, session);
        } else if ("confirmando_cancelacion".equals(pasoActual)) {
            // PASO 3: Confirmar cancelación automática
            return confirmarCancelacionAutomatica(request.getMensaje(), datosCancelacion, vecino, session);
        } else if ("confirmando_motivo_ambiguo".equals(pasoActual)) {
            // PASO 4: Evitar ambiguedad
            return procesarConfirmacionMotivoAmbiguo(request.getMensaje(), datosCancelacion, vecino, session);
        } else if ("ingresando_motivo".equals(pasoActual)) {
            // PASO 5: Procesar motivo de solicitud
            return procesarMotivoSolicitud(request.getMensaje(), datosCancelacion, vecino, session);
        } else if ("ingresando_codigo".equals(pasoActual)) {
            // PASO 6: Procesar código de pago (opcional)
            return procesarCodigoPago(request.getMensaje(), datosCancelacion, vecino, session);
        }

        limpiarFlujoCancelacion(session);
        return respuestaSimple("❌ Error en el flujo de cancelación. Intenta nuevamente.", "ninguna");
    }

    /* 🆕 NUEVO FLUJO: Ver reservas del usuario (CON HTML MEJORADO) */
    public ResponseEntity<ChatbotRespuesta> flujoVerReservas(ChatRequest request, HttpSession session) {
        System.out.println("[FLUJO DEBUG] ═══ INICIANDO FLUJO VER RESERVAS ═══");
        System.out.println("[FLUJO DEBUG] Mensaje: '" + request.getMensaje() + "'");

        Usuarios vecino = (Usuarios) session.getAttribute("usuario");
        if (vecino == null) {
            return respuestaJson("❌ Debes iniciar sesión para ver tus reservas.", "ninguna", Map.of());
        }

        try {
            // 🔧 Usar el método que ya existe - buscar todas las reservas del usuario
            List<Reserva> todasLasReservas = reservaRepository
                    .findByVecino_IdUsuariosOrderByMomentoReservaDesc(vecino.getIdUsuarios());

            // 🔧 Filtrar solo estados "Confirmada" y "No confirmada"
            List<Reserva> reservasActivas = todasLasReservas.stream()
                    .filter(reserva -> {
                        String estado = reserva.getEstado().getEstado();
                        return "Confirmada".equals(estado) || "Pendiente de confirmación".equals(estado);
                    })
                    .collect(Collectors.toList());

            System.out.println("[FLUJO DEBUG] Total reservas del usuario: " + todasLasReservas.size());
            System.out.println("[FLUJO DEBUG] Reservas activas filtradas: " + reservasActivas.size());

            if (reservasActivas.isEmpty()) {
                return respuestaJson(
                        "<div style='text-align: center; padding: 20px;'>" +
                                "<div style='font-size: 2rem; margin-bottom: 10px;'>📋</div>" +
                                "<div style='font-weight: 600; color: #6c757d; margin-bottom: 10px;'>No tienes reservas activas</div>" +
                                "<div style='color: #6c757d;'>¿Te gustaría hacer una nueva reserva? 😊</div>" +
                                "</div>",
                        "ninguna",
                        Map.of()
                );
            }

            // 🔧 Construir lista de reservas con HTML estilizado
            StringBuilder respuesta = new StringBuilder();
            respuesta.append("<div class='reservas-lista'>");
            respuesta.append("<div style='font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;'>");
            respuesta.append("<span style='margin-right: 8px;'>📋</span>Tus reservas activas:");
            respuesta.append("</div>");

            for (int i = 0; i < reservasActivas.size(); i++) {
                Reserva reserva = reservasActivas.get(i);

                // 🔧 Determinar emoji y color del estado
                String estadoEmoji = "Confirmada".equals(reserva.getEstado().getEstado()) ? "✅" : "⏳";
                String estadoTexto = "Confirmada".equals(reserva.getEstado().getEstado()) ? "Confirmada" : "Pendiente";
                String estadoColor = "Confirmada".equals(reserva.getEstado().getEstado()) ? "#28a745" : "#ffc107";

                respuesta.append("<div style='");
                respuesta.append("background: #f8f9fa; ");
                respuesta.append("border-left: 4px solid ").append(estadoColor).append("; ");
                respuesta.append("border-radius: 8px; ");
                respuesta.append("padding: 12px; ");
                respuesta.append("margin-bottom: 12px; ");
                respuesta.append("box-shadow: 0 2px 4px rgba(0,0,0,0.1);");
                respuesta.append("'>");

                // Header con número y estado
                respuesta.append("<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;'>");
                respuesta.append("<span style='font-weight: 600; color: #495057;'>Reserva #").append(i + 1).append("</span>");
                respuesta.append("<span style='background: ").append(estadoColor).append("; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;'>");
                respuesta.append(estadoEmoji).append(" ").append(estadoTexto);
                respuesta.append("</span>");
                respuesta.append("</div>");

                // Detalles de la reserva
                respuesta.append("<div style='display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;'>");

                respuesta.append("<div><strong>🆔 ID:</strong> ").append(reserva.getIdReserva()).append("</div>");
                respuesta.append("<div><strong>📍 Lugar:</strong> ").append(reserva.getEspacio().getNombre()).append("</div>");
                respuesta.append("<div><strong>📅 Fecha:</strong> ").append(reserva.getFecha()).append("</div>");
                respuesta.append("<div><strong>🕒 Hora:</strong> ").append(reserva.getHoraInicio().toString().substring(0, 5))
                        .append(" - ").append(reserva.getHoraFin().toString().substring(0, 5)).append("</div>");
                respuesta.append("<div><strong>💰 Costo:</strong> S/").append(String.format("%.2f", reserva.getCosto())).append("</div>");
                respuesta.append("<div><strong>💳 Pago:</strong> ").append(reserva.getTipoPago()).append("</div>");

                respuesta.append("</div>");
                respuesta.append("</div>");
            }

            respuesta.append("</div>");
            respuesta.append("<div style='text-align: center; margin-top: 15px; color: #6c757d;'>");
            respuesta.append("¿En qué más puedo ayudarte? 😊");
            respuesta.append("</div>");

            return respuestaJson(respuesta.toString(), "ninguna", Map.of());

        } catch (Exception e) {
            System.err.println("[FLUJO DEBUG] ❌ Error al obtener reservas: " + e.getMessage());
            e.printStackTrace();
            return respuestaJson(
                    "<div style='color: #dc3545; text-align: center; padding: 15px;'>" +
                            "❌ Hubo un error al consultar tus reservas. Inténtalo nuevamente." +
                            "</div>",
                    "ninguna",
                    Map.of()
            );
        }
    }

    /* 🆕 NUEVO FLUJO: Listar lugares disponibles */
    public ResponseEntity<ChatbotRespuesta> flujoListarLugares(ChatRequest request, HttpSession session) {
        System.out.println("[FLUJO DEBUG] ═══ INICIANDO FLUJO LISTAR LUGARES ═══");
        limpiarTodasLasSesiones(session);

        List<Lugar> lugares = lugarRepository.findAll();

        StringBuilder html = new StringBuilder();
        html.append("<div class='lugares-lista'>");
        html.append("<div style='font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;'>");
        html.append("<span style='margin-right: 8px;'>📍</span>Lugares disponibles:");
        html.append("</div>");
        for (int i = 0; i < lugares.size(); i++) {
            Lugar l = lugares.get(i);
            html.append("<div style='background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;'>");
            html.append("<strong>").append(i + 1).append(". ").append(l.getLugar()).append("</strong>");
            html.append("</div>");
        }
        html.append("</div>");
        html.append("<div style='text-align: center; margin-top: 15px; color: #6c757d;'>¿En qué más puedo ayudarte? 😊</div>");

        return respuestaJson(html.toString(), "ninguna", Map.of());
    }

    /* 🆕 NUEVO FLUJO: Listar espacios deportivos */
    public ResponseEntity<ChatbotRespuesta> flujoListarEspacios(ChatRequest request, HttpSession session) {
        System.out.println("[FLUJO DEBUG] ═══ INICIANDO FLUJO LISTAR ESPACIOS ═══");
        limpiarTodasLasSesiones(session);

        List<Espacio> espacios = espacioRepository.findAll();

        StringBuilder html = new StringBuilder();
        html.append("<div class='espacios-lista'>");
        html.append("<div style='font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;'>");
        html.append("<span style='margin-right: 8px;'>🏟️</span>Espacios deportivos:");
        html.append("</div>");
        for (int i = 0; i < espacios.size(); i++) {
            Espacio e = espacios.get(i);
            html.append("<div style='background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;'>");
            html.append("<strong>").append(i + 1).append(". ").append(e.getNombre()).append("</strong>");
            html.append("<div style='color:#6c757d;font-size:0.9rem;'>Lugar: ").append(e.getIdLugar().getLugar()).append("</div>");
            String deportes = e.getDeportes().stream().map(Deporte::getNombre).collect(Collectors.joining(", "));
            if (!deportes.isEmpty()) {
                html.append("<div style='color:#6c757d;font-size:0.9rem;'>Deportes: ").append(deportes).append("</div>");
            }
            html.append("<div style='color:#6c757d;font-size:0.9rem;'>Costo por hora: S/")
                    .append(String.format("%.2f", e.getCosto())).append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        html.append("<div style='text-align: center; margin-top: 15px; color: #6c757d;'>¿En qué más puedo ayudarte? 😊</div>");

        return respuestaJson(html.toString(), "ninguna", Map.of());
    }

    private ResponseEntity<ChatbotRespuesta> mostrarReservasCancelables(Usuarios vecino, HttpSession session) {
        // Buscar reservas cancelables (estados 1=Confirmada, 2=Pendiente de confirmación)
        List<Reserva> reservasCancelables = reservaRepository.findByVecino_IdUsuariosAndEstado_IdEstadoReservaIn(
                vecino.getIdUsuarios(), List.of(1, 2));

        if (reservasCancelables.isEmpty()) {
            limpiarFlujoCancelacion(session);
            return respuestaSimple("😔 No tienes reservas que se puedan cancelar en este momento.", "ninguna");
        }

        // 🔧 FIX: Guardar solo datos primitivos, NO las entidades completas
        List<Map<String, String>> reservasData = new ArrayList<>();
        StringBuilder htmlReservas = new StringBuilder();
        htmlReservas.append("<div class='reservas-cancelables'>");
        htmlReservas.append("<h6>🗂️ Reservas que puedes cancelar:</h6>");

        for (int i = 0; i < reservasCancelables.size(); i++) {
            Reserva reserva = reservasCancelables.get(i);

            // 🔧 FIX: Crear mapa con datos primitivos solamente
            Map<String, String> reservaData = new HashMap<>();
            reservaData.put("idReserva", String.valueOf(reserva.getIdReserva()));
            reservaData.put("nombreEspacio", reserva.getEspacio().getNombre());
            reservaData.put("fecha", reserva.getFecha().toString());
            reservaData.put("horaInicio", reserva.getHoraInicio().toString());
            reservaData.put("horaFin", reserva.getHoraFin().toString());
            reservaData.put("estado", reserva.getEstado().getEstado());
            reservaData.put("indice", String.valueOf(i + 1));

            reservasData.add(reservaData);

            // Generar HTML para mostrar
            htmlReservas.append("<div class='reserva-item mb-2 p-2 border rounded'>");
            htmlReservas.append("<strong>").append(i + 1).append(". ✅ ").append(reserva.getEspacio().getNombre()).append("</strong><br>");
            htmlReservas.append("📅 ").append(reserva.getFecha()).append(" | ⏰ ").append(reserva.getHoraInicio()).append(" - ").append(reserva.getHoraFin()).append("<br>");
            htmlReservas.append("🏷️ Estado: ").append(reserva.getEstado().getEstado());
            htmlReservas.append("</div>");
        }

        htmlReservas.append("</div>");
        htmlReservas.append("<p><strong>Escribe el número de la reserva que deseas cancelar:</strong></p>");

        // 🔧 FIX: Guardar solo datos serializables en la sesión
        session.setAttribute("reservasCancelables", reservasData); // Lista de Maps, NO entidades
        session.setAttribute("pasoCancelacion", "seleccionando_reserva");

        return respuestaSimple(htmlReservas.toString(), "cancelar_reserva");
    }


    private ResponseEntity<ChatbotRespuesta> procesarSeleccionReserva(String mensaje, Usuarios vecino, HttpSession session) {
        try {
            int seleccion = Integer.parseInt(mensaje.trim()) - 1;

            // 🔧 FIX: Obtener datos serializables en lugar de entidades
            @SuppressWarnings("unchecked")
            List<Map<String, String>> reservasCancelables = (List<Map<String, String>>) session.getAttribute("reservasCancelables");

            if (reservasCancelables == null || seleccion < 0 || seleccion >= reservasCancelables.size()) {
                return respuestaSimple("❌ Número inválido. Por favor, escribe un número de la lista.", "cancelar_reserva");
            }

            // 🔧 FIX: Trabajar con datos primitivos
            Map<String, String> reservaSeleccionada = reservasCancelables.get(seleccion);

            // Buscar la reserva completa solo cuando sea necesario
            int idReserva = Integer.parseInt(reservaSeleccionada.get("idReserva"));
            Optional<Reserva> reservaOpt = reservaRepository.findById(idReserva);

            if (reservaOpt.isEmpty()) {
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Error: Reserva no encontrada.", "ninguna");
            }

            Reserva reserva = reservaOpt.get();

            // Crear datos para el flujo (solo primitivos)
            Map<String, String> datosCancelacion = new HashMap<>();
            datosCancelacion.put("idReserva", String.valueOf(reserva.getIdReserva()));
            datosCancelacion.put("nombreEspacio", reservaSeleccionada.get("nombreEspacio"));
            datosCancelacion.put("fecha", reservaSeleccionada.get("fecha"));
            datosCancelacion.put("horaInicio", reservaSeleccionada.get("horaInicio"));
            datosCancelacion.put("horaFin", reservaSeleccionada.get("horaFin"));
            datosCancelacion.put("estado", reservaSeleccionada.get("estado"));

            // 🔧 FIX: Usar zona horaria de Perú como en VecinoController
            LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
            LocalDateTime horaInicio = LocalDateTime.of(
                    reserva.getFecha(),       // LocalDate directamente
                    reserva.getHoraInicio()   // LocalTime directamente
            );

            // 🔧 FIX: Verificar si la reserva es elegible para cancelación automática (más de 24 horas)
            boolean puedeAutocancelar = ahora.isBefore(horaInicio.minusHours(24));

            System.out.println("[DEBUG CANCELACION] Ahora (Perú): " + ahora);
            System.out.println("[DEBUG CANCELACION] Hora inicio reserva: " + horaInicio);
            System.out.println("[DEBUG CANCELACION] 24 horas antes: " + horaInicio.minusHours(24));
            System.out.println("[DEBUG CANCELACION] ¿Puede autocancelar?: " + puedeAutocancelar);

            if (puedeAutocancelar) {
                return mostrarConfirmacionAutomatica(datosCancelacion, session);
            } else {
                return iniciarSolicitudCancelacion(datosCancelacion, session);
            }

        } catch (NumberFormatException e) {
            return respuestaSimple("❌ Por favor, escribe solo el número de la reserva.", "cancelar_reserva");
        } catch (Exception e) {
            System.err.println("[ERROR] Error al procesar selección de reserva: " + e.getMessage());
            e.printStackTrace(); // 🔧 FIX: Agregar stack trace para debug
            limpiarFlujoCancelacion(session);
            return respuestaSimple("❌ Error al procesar tu selección. Intenta nuevamente.", "ninguna");
        }
    }


    private ResponseEntity<ChatbotRespuesta> mostrarConfirmacionAutomatica(Map<String, String> datos, HttpSession session) {
        // 🔧 FIX: Guardar datos en la sesión ANTES de mostrar la confirmación
        session.setAttribute("datosCancelacion", datos);
        session.setAttribute("pasoCancelacion", "confirmando_cancelacion");

        StringBuilder html = new StringBuilder();
        html.append("<div class='confirmacion-cancelacion'>");
        html.append("<h6>✅ Cancelación Automática Disponible</h6>");
        html.append("<div class='alert alert-info'>");
        html.append("<strong>🏟️ Espacio:</strong> ").append(datos.get("nombreEspacio")).append("<br>");
        html.append("<strong>📅 Fecha:</strong> ").append(datos.get("fecha")).append("<br>");
        html.append("<strong>⏰ Horario:</strong> ").append(datos.get("horaInicio")).append(" - ").append(datos.get("horaFin")).append("<br>");
        html.append("</div>");

        // Determinar tipo de cancelación basado en el estado
        String estado = datos.get("estado");
        if ("Confirmada".equals(estado)) {
            html.append("<p>Como tu reserva es con más de 24 horas de anticipación, se cancelará automáticamente <strong>con reembolso</strong>.</p>");
        } else {
            html.append("<p>Como tu reserva es con más de 24 horas de anticipación, se cancelará automáticamente <strong>sin reembolso</strong>.</p>");
        }

        html.append("<p><strong>¿Confirmas la cancelación?</strong></p>");
        html.append("<p>Responde 'SÍ' para confirmar o 'NO' para cancelar.</p>");
        html.append("</div>");

        return respuestaSimple(html.toString(), "cancelar_reserva");
    }
    private ResponseEntity<ChatbotRespuesta> confirmarCancelacionAutomatica(String mensaje, Map<String, String> datos, Usuarios vecino, HttpSession session) {
        String respuesta = mensaje.trim().toLowerCase();

        if (respuesta.equals("sí") || respuesta.equals("si") || respuesta.equals("confirmar") || respuesta.equals("yes")) {
            return ejecutarCancelacionAutomatica(datos, vecino, session);
        } else if (respuesta.equals("no") || respuesta.equals("cancelar") || respuesta.equals("salir")) {
            limpiarFlujoCancelacion(session);
            return respuestaSimple("✅ Cancelación descartada. Tu reserva se mantiene activa.", "ninguna");
        } else {
            return respuestaSimple("❓ Por favor responde 'SÍ' para confirmar la cancelación o 'NO' para mantener tu reserva.", "cancelar_reserva");
        }
    }

    private ResponseEntity<ChatbotRespuesta> ejecutarCancelacionAutomatica(Map<String, String> datos, Usuarios vecino, HttpSession session) {
        try {
            int idReserva = Integer.parseInt(datos.get("idReserva"));
            Optional<Reserva> reservaOpt = reservaRepository.findById(idReserva);

            if (reservaOpt.isEmpty()) {
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Error: Reserva no encontrada.", "ninguna");
            }

            Reserva reserva = reservaOpt.get();

            // Verificar que la reserva pertenece al usuario
            if (!reserva.getVecino().getIdUsuarios().equals(vecino.getIdUsuarios())) {
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Error: No tienes autorización para cancelar esta reserva.", "ninguna");
            }

            // Verificar estado actual
            String estadoActual = reserva.getEstado().getEstado();
            if (estadoActual.equals("Cancelada") || estadoActual.equals("Cancelada con reembolso") ||
                    estadoActual.equals("Finalizada") || estadoActual.equals("Rechazada") || estadoActual.equals("Cancelada sin reembolso")) {
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Esta reserva ya no puede cancelarse.", "ninguna");
            }

            // 🔧 FIX: Usar zona horaria de Perú
            LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
            LocalDateTime horaInicio = LocalDateTime.of(
                    reserva.getFecha(),       // LocalDate directamente
                    reserva.getHoraInicio()   // LocalTime directamente
            );

            // Verificar que aún es cancelable automáticamente
            if (!ahora.isBefore(horaInicio.minusHours(24))) {
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Lo siento, ya no es posible cancelar automáticamente esta reserva (debe ser con más de 24 horas de anticipación).", "ninguna");
            }

            // Determinar el nuevo estado según el estado actual
            EstadoReserva nuevoEstado;
            String mensajeExito;

            if ("Confirmada".equals(estadoActual)) {
                // Cancelada con reembolso (ID 5)
                nuevoEstado = estadoReservaRepository.findById(5).orElse(null);
                mensajeExito = "✅ Reserva cancelada exitosamente con reembolso. El reembolso estará disponible en un plazo de 5 a 7 días hábiles.";
            } else {
                // Cancelada sin reembolso (ID 7)
                nuevoEstado = estadoReservaRepository.findById(7).orElse(null);
                mensajeExito = "✅ Reserva cancelada exitosamente sin reembolso.";
            }

            if (nuevoEstado == null) {
                System.err.println("[ERROR] No se pudo encontrar el estado de cancelación en la base de datos");
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Error interno: No se pudo procesar la cancelación.", "ninguna");
            }

            // Actualizar la reserva
            reserva.setEstado(nuevoEstado);
            reservaRepository.save(reserva);

            // 🔧 FIX: Limpiar la sesión ANTES de devolver la respuesta
            limpiarFlujoCancelacion(session);

            // 🆕 NUEVO: Agregar mensaje adicional para indicar que puede hacer otra acción
            String mensajeFinal = mensajeExito + "\n\n¿Te gustaría hacer alguna otra gestión? 😊";


            return respuestaSimple(mensajeFinal, "ninguna");

        } catch (Exception e) {
            System.err.println("[ERROR] Error al ejecutar cancelación automática: " + e.getMessage());
            e.printStackTrace();
            limpiarFlujoCancelacion(session);
            return respuestaSimple("❌ Error al procesar la cancelación. Intenta más tarde.", "ninguna");
        }
    }

    private ResponseEntity<ChatbotRespuesta> iniciarSolicitudCancelacion(Map<String, String> datos, HttpSession session) {
        // Guardar datos en la sesión ANTES de solicitar el motivo
        session.setAttribute("datosCancelacion", datos);
        session.setAttribute("pasoCancelacion", "ingresando_motivo");

        StringBuilder html = new StringBuilder();
        html.append("<div class='solicitud-cancelacion'>");
        html.append("<h6>📝 Solicitud de Cancelación</h6>");
        html.append("<div class='alert alert-warning'>");
        html.append("<strong>🏟️ Espacio:</strong> ").append(datos.get("nombreEspacio")).append("<br>");
        html.append("<strong>📅 Fecha:</strong> ").append(datos.get("fecha")).append("<br>");
        html.append("<strong>⏰ Horario:</strong> ").append(datos.get("horaInicio")).append(" - ").append(datos.get("horaFin")).append("<br>");
        html.append("</div>");
        html.append("<p>Como tu reserva es dentro de las próximas 24 horas, necesitas enviar una solicitud de cancelación al coordinador.</p>");
        html.append("<p><strong>¿Cuál es el motivo de tu cancelación?</strong></p>");
        html.append("<p><small>Ejemplo: Emergencia familiar, enfermedad, cambio de planes, etc.</small></p>");
        html.append("</div>");

        return respuestaSimple(html.toString(), "cancelar_reserva");
    }


    private ResponseEntity<ChatbotRespuesta> procesarMotivoSolicitud(String mensaje, Map<String, String> datos, Usuarios vecino, HttpSession session) {
        String motivo = mensaje.trim();

        // Detectar frases ambiguas antes de validar longitud
        List<String> frasesAmbiguas = Arrays.asList(
                "ya no quiero",
                "no quiero",
                "no me interesa",
                "cambié de opinión",
                "ya no",
                "no puedo",
                "no voy"
        );

        boolean esAmbigua = frasesAmbiguas.stream()
                .anyMatch(frase -> motivo.toLowerCase().contains(frase));

        if (esAmbigua && motivo.length() < 20) { // Solo si es corta Y ambigua
            // Pedir confirmación específica
            Map<String, String> soliConfirmacion = Map.of(
                    "Es el motivo de cancelación", "1",
                    "Quiero salir del proceso", "2"
            );

            ChatbotRespuesta r = new ChatbotRespuesta();
            r.setRespuesta("Para confirmar, tu respuesta \"" + motivo + "\" es:\n\n" +
                    "**1.** El **motivo** de cancelación de tu reserva\n" +
                    "**2.** Quieres **salir** del proceso de cancelación\n\n" +
                    "¿Cuál de las dos opciones?");
            r.setAccion("cancelar_reserva");
            r.setParametros(Map.of(
                    "paso", "confirmar_motivo_ambiguo"
            ));
            r.setRelevante_para(new String[]{"cancelacion"});
            r.setNivel_confianza(0.8);
            r.setSugerencias(soliConfirmacion);

            // Guardar temporalmente
            datos.put("motivo_temporal", motivo);
            session.setAttribute("datosCancelacion", datos);
            session.setAttribute("pasoCancelacion", "confirmando_motivo_ambiguo");

            return ResponseEntity.ok(r);
        }

        // Validación de longitud original
        if (motivo.length() < 10) {
            return respuestaSimple("❌ El motivo debe tener al menos 10 caracteres. Por favor, describe brevemente por qué necesitas cancelar tu reserva.", "cancelar_reserva");
        }

        datos.put("motivo", motivo);
        session.setAttribute("datosCancelacion", datos);

        boolean esConfirmada = "1".equals(datos.get("estadoReserva"));

        if (esConfirmada) {
            StringBuilder html = new StringBuilder();
            html.append("<div class='codigo-pago'>");
            html.append("<h6>💳 Código de Pago (Opcional)</h6>");
            html.append("<p>Si tienes el código de tu pago, ingrésalo para agilizar el proceso de reembolso.</p>");
            html.append("<p><strong>Ingresa tu código de pago o escribe 'OMITIR' para continuar sin él:</strong></p>");
            html.append("</div>");

            session.setAttribute("pasoCancelacion", "ingresando_codigo");
            return respuestaJson(html.toString(), "cancelar_reserva", Map.of());
        } else {
            // Si no es confirmada, enviar solicitud directamente
            return enviarSolicitudCancelacion(datos, vecino, session);
        }
    }

    private ResponseEntity<ChatbotRespuesta> procesarCodigoPago(String mensaje, Map<String, String> datos, Usuarios vecino, HttpSession session) {
        String codigo = mensaje.trim();

        if (!codigo.equalsIgnoreCase("omitir")) {
            datos.put("codigoPago", codigo);
            session.setAttribute("datosCancelacion", datos);
        }

        return enviarSolicitudCancelacion(datos, vecino, session);
    }

    private ResponseEntity<ChatbotRespuesta> enviarSolicitudCancelacion(Map<String, String> datos, Usuarios vecino, HttpSession session) {
        try {
            int idReserva = Integer.parseInt(datos.get("idReserva"));
            Reserva reserva = reservaRepository.findById(idReserva).orElse(null);

            if (reserva == null || !reserva.getVecino().getIdUsuarios().equals(vecino.getIdUsuarios())) {
                limpiarFlujoCancelacion(session);
                return respuestaSimple("❌ Error: Reserva no encontrada o no autorizada.", "ninguna");
            }

            // Crear solicitud de cancelación
            SolicitudCancelacion solicitud = new SolicitudCancelacion();
            solicitud.setReserva(reserva);
            solicitud.setMotivo(datos.get("motivo"));
            solicitud.setCodigoPago(datos.get("codigoPago"));
            solicitud.setEstado("Pendiente");
            solicitud.setFechaSolicitud(LocalDateTime.now());

            solicitudCancelacionRepository.save(solicitud);

            // Cambiar estado de la reserva a "Reembolso solicitado" (id=6)
            EstadoReserva estadoSolicitud = estadoReservaRepository.findById(6).orElse(null);
            if (estadoSolicitud != null) {
                reserva.setEstado(estadoSolicitud);
                reservaRepository.save(reserva);
            }

            limpiarFlujoCancelacion(session);

            // Mostrar resumen
            StringBuilder html = new StringBuilder();
            html.append("<div class='resumen-solicitud'>");
            html.append("<h6>✅ Solicitud Enviada Exitosamente</h6>");
            html.append("<div class='alert alert-success'>");
            html.append("<strong>📋 Resumen de tu solicitud:</strong><br><br>");
            html.append(String.format("<strong>🏟️ Espacio:</strong> %s<br>", datos.get("nombreEspacio")));
            html.append(String.format("<strong>📅 Fecha:</strong> %s<br>", datos.get("fecha")));
            html.append(String.format("<strong>⏰ Horario:</strong> %s - %s<br>", datos.get("horaInicio"), datos.get("horaFin")));
            html.append(String.format("<strong>📝 Motivo:</strong> %s<br>", datos.get("motivo")));
            if (datos.containsKey("codigoPago")) {
                html.append(String.format("<strong>💳 Código de Pago:</strong> %s<br>", datos.get("codigoPago")));
            }
            html.append("</div>");
            html.append("<p>🔔 Tu solicitud ha sido enviada al coordinador. Recibirás una notificación por correo cuando sea procesada.</p>");
            html.append("<p>⚠️ <strong>Nota:</strong> Tu reserva ha sido cancelada, pero el reembolso dependerá de la decisión del coordinador.</p>");
            html.append("</div>");

            return respuestaJson(html.toString(), "ninguna", Map.of());

        } catch (Exception e) {
            limpiarFlujoCancelacion(session);
            return respuestaSimple("❌ Error al procesar la solicitud. Intenta más tarde.", "ninguna");
        }
    }

    // 🔧 FIX: Actualizar limpiarFlujoCancelacion para usar contexto limpio
    private void limpiarFlujoCancelacion(HttpSession session) {
        session.removeAttribute("datosCancelacion");
        session.removeAttribute("pasoCancelacion");
        session.removeAttribute("reservasCancelables");
        session.removeAttribute("flujoActivo");

        // 🆕 NUEVO: Limpiar TODOS los flujos activos
        session.removeAttribute("datosDisponibilidad");
        session.removeAttribute("pasoDisponibilidad");
        session.removeAttribute("datosReserva");
        session.removeAttribute("pasoReserva");
        session.removeAttribute("datosTemporales");

        // 🆕 NUEVO: Marcar para limpiar contexto en próxima solicitud
        session.setAttribute("limpiarContexto", true);

        System.out.println("[DEBUG] ✅ Sesión completamente limpiada tras cancelación exitosa");
    }



    @Autowired
    private SolicitudCancelacionRepository solicitudCancelacionRepository;


    // 🔧 Método auxiliar actualizado
    private ResponseEntity<ChatbotRespuesta> continuarFlujoDisponibilidad(Map<String, String> datos, String pasoCompleto, HttpSession session) {
        ChatRequest nuevoRequest = new ChatRequest();
        nuevoRequest.setMensaje("continuar"); // Mensaje dummy
        nuevoRequest.setAccion("ver_disponibilidad");
        nuevoRequest.setParametros(convertirParametros(datos));

        // Llamar recursivamente con los datos actualizados
        return flujoVerDisponibilidad(nuevoRequest, session);
    }


    // 🆕 NUEVO: Método para transiciones automáticas sin recursión
    private ResponseEntity<ChatbotRespuesta> transicionAutomaticaDisponibilidad(Map<String, String> datos, String pasoCompletado, HttpSession session) {
        System.out.println("[FLUJO DEBUG] ═══ TRANSICIÓN AUTOMÁTICA - Paso completado: " + pasoCompletado + " ═══");

        // Paso siguiente después de encontrar idEspacio automáticamente: pedir fecha
        if ("idEspacio".equals(pasoCompletado)) {
            Map<String, String> parametros = new HashMap<>();
            parametros.put("paso", "pedir_fecha");
            parametros.put("lugar", datos.get("lugar"));
            parametros.put("idEspacio", datos.get("idEspacio"));

            session.setAttribute("datosTemporales", convertirParametros(datos));
            LocalDate pasadoManana = LocalDate.now().plusDays(2);
            String pasadoMananaString = pasadoManana.toString();

            Map<String, String> sugerenciasFecha = Map.of(
                    "Hoy", "hoy",
                    "Mañana", "mañana",
                    "Pasado mañana", pasadoMananaString
            );

            ChatbotRespuesta r = new ChatbotRespuesta();
            r.setRespuesta("¿Para qué fecha deseas consultar la disponibilidad? 📅 (Ej: 2024-12-25, hoy, mañana, etc.)");
            r.setAccion("ver_disponibilidad");
            r.setParametros(convertirParametros(parametros));
            r.setRelevante_para(new String[]{"reservas", "disponibilidad"});
            r.setNivel_confianza(0.9);
            r.setSugerencias(sugerenciasFecha);
            return ResponseEntity.ok(r);
        }

        // Paso siguiente después de encontrar lugar automáticamente: pedir espacio
        if ("lugar".equals(pasoCompletado)) {
            String lugarSeleccionado = datos.get("lugar");
            List<Espacio> espacios;

            try {
                espacios = espacioRepository.findAll().stream()
                        .filter(esp -> esp.getIdLugar().getLugar().equals(lugarSeleccionado))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("[FLUJO DEBUG] Error buscando espacios: " + e.getMessage());
                espacios = new ArrayList<>();
            }

            if (espacios.isEmpty()) {
                session.removeAttribute("flujoActivo");
                session.removeAttribute("datosTemporales");
                return respuestaJson("No se encontraron espacios en " + lugarSeleccionado, "ninguna", Map.of());
            }

            Map<String, String> sugerenciasEspacios = espacios.stream()
                    .collect(Collectors.toMap(
                            Espacio::getNombre,
                            e -> String.valueOf(e.getIdEspacio())
                    ));

            Map<String, String> parametros = new HashMap<>();
            parametros.put("paso", "pedir_espacio");
            parametros.put("lugar", lugarSeleccionado);

            session.setAttribute("datosTemporales", convertirParametros(datos));

            ChatbotRespuesta r = new ChatbotRespuesta();
            r.setRespuesta("¿Qué espacio deportivo dentro de " + lugarSeleccionado + " deseas consultar?");
            r.setAccion("ver_disponibilidad");
            r.setParametros(convertirParametros(parametros));
            r.setRelevante_para(new String[]{"reservas", "disponibilidad"});
            r.setNivel_confianza(0.9);
            r.setSugerencias(sugerenciasEspacios);
            return ResponseEntity.ok(r);
        }

        // Si llega aquí, error
        return respuestaJson("Error en transición automática", "ninguna", Map.of());
    }


    private ResponseEntity<ChatbotRespuesta> procesarConfirmacionMotivoAmbiguo(String mensaje, Map<String, String> datos, Usuarios vecino, HttpSession session) {
        String userInput = mensaje.trim();

        if ("1".equals(userInput) || userInput.toLowerCase().contains("motivo") || userInput.toLowerCase().contains("cancelación")) {
            // Usar como motivo de cancelación
            String motivoTemporal = datos.get("motivo_temporal");
            datos.put("motivo", motivoTemporal);
            datos.remove("motivo_temporal");
            session.setAttribute("datosCancelacion", datos);

            // Continuar con la lógica original
            boolean esConfirmada = "1".equals(datos.get("estadoReserva"));

            if (esConfirmada) {
                StringBuilder html = new StringBuilder();
                html.append("<div class='codigo-pago'>");
                html.append("<h6>💳 Código de Pago (Opcional)</h6>");
                html.append("<p>Si tienes el código de tu pago, ingrésalo para agilizar el proceso de reembolso.</p>");
                html.append("<p><strong>Ingresa tu código de pago o escribe 'OMITIR' para continuar sin él:</strong></p>");
                html.append("</div>");

                session.setAttribute("pasoCancelacion", "ingresando_codigo");
                return respuestaJson(html.toString(), "cancelar_reserva", Map.of());
            } else {
                return enviarSolicitudCancelacion(datos, vecino, session);
            }

        } else if ("2".equals(userInput) || userInput.toLowerCase().contains("salir") || userInput.toLowerCase().contains("proceso")) {
            // Cancelar flujo completamente
            limpiarFlujoCancelacion(session);
            return respuestaSimple("✅ Proceso de cancelación terminado. ¿En qué más puedo ayudarte?", "ninguna");
        }

        // Si no entendió la respuesta
        return respuestaJson(
                "No entendí tu respuesta. Por favor escribe:\n\n**1** = Es el motivo de cancelación\n**2** = Quiero salir del proceso",
                "cancelar_reserva",
                Map.of("paso", "confirmar_motivo_ambiguo")
        );
    }

    // Método auxiliar para convertir Map<String, String> a Map<String, Object>
    private Map<String, Object> convertirParametros(Map<String, String> datos) {
        Map<String, Object> parametros = new HashMap<>();
        datos.forEach(parametros::put);
        return parametros;
    }

    // 🔧 Método para interpretar fechas del usuario
    private String interpretarFecha(String entrada, HttpSession session) {
        ZoneId lima = ZoneId.of("America/Lima");
        LocalDate hoy = LocalDate.now(lima);

        String diaTemp = (String) session.getAttribute("diaTemporal");
        String mesTemp = (String) session.getAttribute("mesTemporal");

        String input = entrada.toLowerCase().trim();

        if (diaTemp != null && mesTemp == null) {
            if (input.matches("\\d{1,2}")) {
                session.setAttribute("mesTemporal", input);
                return "NEED_YEAR";
            }
            return "ASK_MONTH";
        }

        if (diaTemp != null && mesTemp != null) {
            if (input.matches("\\d{4}")) {
                try {
                    LocalDate fecha = LocalDate.of(Integer.parseInt(input), Integer.parseInt(mesTemp), Integer.parseInt(diaTemp));
                    session.removeAttribute("diaTemporal");
                    session.removeAttribute("mesTemporal");
                    if (fecha.isBefore(hoy)) return "PAST";
                    return fecha.toString();
                } catch (Exception e) {
                    return "INVALID";
                }
            }
            return "ASK_YEAR";
        }

        if ("hoy".equals(input)) {
            return hoy.toString();
        }
        if ("mañana".equals(input)) {
            return hoy.plusDays(1).toString();
        }

        if (input.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                LocalDate fecha = LocalDate.parse(input);
                if (fecha.isBefore(hoy)) return "PAST";
                return fecha.toString();
            } catch (Exception e) {
                return "INVALID";
            }
        }

        if (input.matches("\\d{1,2}")) {
            session.setAttribute("diaTemporal", input);
            return "NEED_MONTH";
        }

        if (input.matches("\\d{1,2}[-/]\\d{1,2}")) {
            String[] p = input.split("[-/]");
            session.setAttribute("diaTemporal", p[0]);
            session.setAttribute("mesTemporal", p[1]);
            return "NEED_YEAR";
        }

        return "INVALID";
    }

    // 🔧 Método para parsear horarios del mensaje del usuario
    private Map<String, String> parseHorarios(String input) {
        Map<String, String> horarios = new HashMap<>();

        String texto = input.toLowerCase().replace(".", "").trim();
        Pattern pattern = Pattern.compile("(?:de\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:a|-|hasta)?\\s*(\\d{1,2})?(?::(\\d{2}))?\\s*(am|pm)?");
        Matcher m = pattern.matcher(texto);
        if (m.find()) {
            String hInicio = m.group(1);
            String minInicio = m.group(2);
            String ampmInicio = m.group(3);
            String hFin = m.group(4);
            String minFin = m.group(5);
            String ampmFin = m.group(6);

            if ((minInicio != null && !"00".equals(minInicio)) || (minFin != null && !"00".equals(minFin))) {
                return horarios; // minutos distintos de 00 no permitidos
            }

            if (hFin == null || hFin.isEmpty()) {
                hFin = String.valueOf(Integer.parseInt(hInicio) + 1);
            }

            int inicio = Integer.parseInt(hInicio);
            int fin = Integer.parseInt(hFin);

            if (ampmInicio == null && ampmFin == null && inicio <= 12 && fin <= 12) {
                // Ambigüedad entre am o pm
                return horarios;
            }

            String ampm = ampmInicio != null ? ampmInicio : ampmFin;
            if (ampm != null) {
                inicio = convertirHoraA24(inicio, ampmInicio != null ? ampmInicio : ampm);
                fin = convertirHoraA24(fin, ampmFin != null ? ampmFin : ampm);
            }

            if (inicio >= 0 && inicio < 24 && fin > inicio && fin <= 24) {
                horarios.put("horaInicio", String.valueOf(inicio));
                horarios.put("horaFin", String.valueOf(fin));
            }
        }

        return horarios;
    }

    private int convertirHoraA24(int hora, String ampm) {
        if (ampm == null) return hora;
        if ("pm".equals(ampm) && hora < 12) return hora + 12;
        if ("am".equals(ampm) && hora == 12) return 0;
        return hora;
    }

    public ResponseEntity<ChatbotRespuesta> ejecutarReservaDesdeChatbot(Map<String, String> datos, Usuarios vecino) {
        System.out.println("[RESERVA DEBUG] ═══ EJECUTANDO RESERVA ═══");
        System.out.println("[RESERVA DEBUG] Datos recibidos: " + datos);
        System.out.println("[RESERVA DEBUG] Usuario: " + (vecino != null ? vecino.getNombres() : "null"));

        try {
            // 🔧 DEBUG: Verificar datos antes de buscar entidades
            System.out.println("[RESERVA DEBUG] ID Espacio a buscar: " + datos.get("idEspacio"));
            System.out.println("[RESERVA DEBUG] Fecha: " + datos.get("fecha"));
            System.out.println("[RESERVA DEBUG] Tipo de pago: " + datos.get("tipoPago"));

            Espacio espacio = espacioRepository.findById(Integer.parseInt(datos.get("idEspacio"))).orElse(null);

            // Solo maneja pagos en banco - siempre estado "Pendiente de confirmación"
            EstadoReserva estado = estadoReservaRepository.findById(2).orElse(null); // No confirmada

            // Asignar coordinador
            Usuarios coordinador = null;
            if (espacio != null && espacio.getIdLugar() != null) {
                List<Usuarios> coordinadores = espacio.getIdLugar().getCoordinadores();
                if (coordinadores != null && !coordinadores.isEmpty()) {
                    coordinador = coordinadores.get(new Random().nextInt(coordinadores.size()));
                }
            }

            System.out.println("[RESERVA DEBUG] Espacio encontrado: " + (espacio != null ? espacio.getNombre() : "null"));
            System.out.println("[RESERVA DEBUG] Estado encontrado: " + (estado != null ? estado.getEstado() : "null"));
            System.out.println("[RESERVA DEBUG] Coordinador encontrado: " + (coordinador != null ? coordinador.getNombres() : "null"));

            if (espacio == null || estado == null || coordinador == null || vecino == null) {
                System.err.println("[RESERVA DEBUG] ❌ Faltan entidades requeridas");
                System.err.println("[RESERVA DEBUG] - Espacio: " + (espacio != null));
                System.err.println("[RESERVA DEBUG] - Estado: " + (estado != null));
                System.err.println("[RESERVA DEBUG] - Coordinador: " + (coordinador != null));
                System.err.println("[RESERVA DEBUG] - Vecino: " + (vecino != null));
                return respuestaJson("Ocurrió un error al procesar la reserva. 🙁", "ninguna", Map.of());
            }

            Reserva reserva = new Reserva();
            reserva.setVecino(vecino);
            reserva.setEspacio(espacio);
            reserva.setEstado(estado);
            reserva.setCoordinador(coordinador);
            reserva.setFecha(LocalDate.parse(datos.get("fecha")));
            reserva.setHoraInicio(LocalTime.of(Integer.parseInt(datos.get("horaInicio")), 0));
            reserva.setHoraFin(LocalTime.of(Integer.parseInt(datos.get("horaFin")), 0));
            reserva.setTipoPago(datos.get("tipoPago"));
            reserva.setMomentoReserva(LocalDateTime.now());

            int horas = Integer.parseInt(datos.get("horaFin")) - Integer.parseInt(datos.get("horaInicio"));
            reserva.setCosto(espacio.getCosto() * horas);

            // Solo para pagos en banco - siempre pendiente
            reserva.setEstadoPago("Pendiente");

            System.out.println("[RESERVA DEBUG] Objeto Reserva creado:");
            System.out.println("[RESERVA DEBUG] - Vecino: " + reserva.getVecino().getNombres());
            System.out.println("[RESERVA DEBUG] - Espacio: " + reserva.getEspacio().getNombre());
            System.out.println("[RESERVA DEBUG] - Fecha: " + reserva.getFecha());
            System.out.println("[RESERVA DEBUG] - Hora inicio: " + reserva.getHoraInicio());
            System.out.println("[RESERVA DEBUG] - Hora fin: " + reserva.getHoraFin());
            System.out.println("[RESERVA DEBUG] - Tipo pago: " + reserva.getTipoPago());
            System.out.println("[RESERVA DEBUG] - Estado pago: " + reserva.getEstadoPago());
            System.out.println("[RESERVA DEBUG] - Costo: " + reserva.getCosto());

            System.out.println("[RESERVA DEBUG] Guardando reserva...");
            Reserva reservaGuardada = reservaRepository.save(reserva);
            System.out.println("[RESERVA DEBUG] ✅ Reserva guardada con ID: " + reservaGuardada.getIdReserva());

            // NO crear registro de pago para pagos en banco - se crea cuando se valide el comprobante
            System.out.println("[RESERVA DEBUG] ℹ️ Pago en banco - No se crea registro de pago hasta validación");

            // Enviar correo - solo para reservas no confirmadas (pago en banco)
            mailManager.enviarCorreoReservaPendiente(vecino, reservaGuardada);

            // Respuesta para pago en banco
            StringBuilder html = new StringBuilder();
            html.append("<div class='reserva-final'>");
            html.append("<div style='font-weight:600;'>⏳ ¡Reserva registrada exitosamente!</div>");
            html.append("<div style='color: #f39c12; font-weight: 500; margin: 10px 0;'>⚠️ Pendiente de pago en banco</div>");
            html.append("<div>🆔 ID Reserva: ").append(reservaGuardada.getIdReserva()).append("</div>");
            html.append("<div>📍 Espacio: ").append(espacio.getNombre()).append("</div>");
            html.append("<div>📅 Fecha: ").append(datos.get("fecha")).append("</div>");
            html.append("<div>🕒 Hora: ").append(datos.get("horaInicio")).append(":00 - ").append(datos.get("horaFin")).append(":00</div>");
            html.append("<div>💰 Total a pagar: S/").append(String.format("%.2f", reserva.getCosto())).append("</div>");
            html.append("<div>💳 Pago: ").append(datos.get("tipoPago")).append("</div>");
            html.append("</div>");
            html.append("<div style='margin: 15px 0; padding: 10px; background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px;'>");
            html.append("<div style='font-weight: 600; color: #856404;'>📋 Instrucciones de pago:</div>");
            html.append("<div style='color: #856404;'>1. Realiza el pago en banco por el monto indicado</div>");
            html.append("<div style='color: #856404;'>2. Sube el comprobante en tu boleta</div>");
            html.append("<div style='color: #856404;'>3. Espera la confirmación del administrador</div>");
            html.append("</div>");

            ChatbotRespuesta respuesta = new ChatbotRespuesta();
            respuesta.setRespuesta(html.toString());
            respuesta.setAccion("ninguna");
            respuesta.setParametros(Map.of());
            respuesta.setRelevante_para(new String[]{"reservas", "pago"});
            respuesta.setNivel_confianza(0.95);
            respuesta.setSugerencias(Map.of(
                    "📄 Ver mi boleta", "/vecino/boleta/" + reservaGuardada.getIdReserva(),
                    "🏠 Inicio", "inicio"
            ));

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            System.err.println("[RESERVA DEBUG] ❌ Error al crear reserva: " + e.getMessage());
            e.printStackTrace();
            return respuestaJson(
                    "❌ Error al procesar la reserva: " + e.getMessage() + "\nPor favor, intenta nuevamente.",
                    "ninguna",
                    Map.of()
            );
        }
    }

    public ResponseEntity<ChatbotRespuesta> respuestaJson(String texto, String accion, Map<String, String> parametros) {
        ChatbotRespuesta r = new ChatbotRespuesta();
        r.setRespuesta(texto);
        r.setAccion(accion);
        r.setParametros(convertirParametros(parametros));
        r.setRelevante_para(new String[]{"reservas"});
        r.setNivel_confianza(0.9);
        return ResponseEntity.ok(r);
    }

    public ResponseEntity<ChatbotRespuesta> respuestaSimple(String texto, String accion) {
        ChatbotRespuesta r = new ChatbotRespuesta();
        r.setRespuesta(texto);
        r.setAccion(accion);
        r.setParametros(Map.of());
        r.setRelevante_para(new String[]{});
        r.setNivel_confianza(0.7);
        return ResponseEntity.ok(r);
    }

    // 🔧 Métodos auxiliares para formatear estados
    private String getEmojiEstado(String estado) {
        return switch (estado) {
            case "Confirmada" -> "✅";
            case "Pendiente de confirmación" -> "⏳";
            case "Cancelada" -> "❌";
            case "Finalizada" -> "📅";
            default -> "❓";
        };
    }

    private String getTextoEstado(String estado) {
        return switch (estado) {
            case "Confirmada" -> "CONFIRMADA";
            case "Pendiente de confirmación" -> "PENDIENTE";
            case "Cancelada" -> "CANCELADA";
            case "Finalizada" -> "FINALIZADA";
            default -> estado.toUpperCase();
        };
    }

    // Método para limpiar completamente todas las sesiones con contexto limpio
    private void limpiarTodasLasSesiones(HttpSession session) {
        // Flujos
        session.removeAttribute("flujoActivo");

        // Cancelación
        session.removeAttribute("datosCancelacion");
        session.removeAttribute("pasoCancelacion");
        session.removeAttribute("reservasCancelables");

        // Disponibilidad/Reservas
        session.removeAttribute("datosDisponibilidad");
        session.removeAttribute("pasoDisponibilidad");
        session.removeAttribute("datosReserva");
        session.removeAttribute("pasoReserva");

        // Otros
        session.removeAttribute("datosTemporales");

        // Marcar para limpiar contexto
        session.setAttribute("limpiarContexto", true);

        System.out.println("[DEBUG] 🧹 Sesión completamente limpiada - Usuario puede realizar nueva acción");
    }
}