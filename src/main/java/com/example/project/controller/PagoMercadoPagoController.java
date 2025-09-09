package com.example.project.controller;

import com.example.project.dto.DetallePagoDTO;
import com.example.project.entity.Espacio;
import com.example.project.entity.Reserva;
import com.example.project.entity.Usuarios;
import com.example.project.repository.EspacioRepositoryGeneral;

import com.example.project.repository.vecino.ReservaRepositoryVecino;
import com.example.project.service.MantenimientoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/pago/mercadopago")
public class PagoMercadoPagoController {

    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private ReservaRepositoryVecino reservaRepositoryVecino;
    @Autowired private MantenimientoService mantenimientoService;
    @Value("${mercadopago.access.token}")
    private String mercadoPagoAccessToken;

    @Value("${mercadopago.public.key}")
    private String mercadoPagoPublicKey;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> obtenerConfiguracion() {
        Map<String, String> config = new HashMap<>();
        config.put("publicKey", mercadoPagoPublicKey);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/preferencia")
    public ResponseEntity<?> crearPreferencia(@RequestBody DetallePagoDTO detalle, HttpSession session) {
        System.out.println("=== DEBUG: Iniciando creación de preferencia ===");

        try {
            // VERIFICAR CONFIGURACIÓN ANTES DE PROCEDER
            if (mercadoPagoAccessToken == null || mercadoPagoAccessToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Token de MercadoPago no configurado"));
            }

            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "URL base no configurada"));
            }

            System.out.println("Token MercadoPago: " + mercadoPagoAccessToken.substring(0, 10) + "...");
            System.out.println("Base URL: " + baseUrl);

            // Configurar MercadoPago ANTES de usarlo
            MercadoPagoConfig.setAccessToken(mercadoPagoAccessToken);
            System.out.println("MercadoPago configurado exitosamente");

            // 1. Validaciones de datos de entrada
            if (detalle == null || detalle.getIdEspacio() == null || detalle.getFecha() == null ||
                    detalle.getHoraInicio() == null || detalle.getHoraFin() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Datos de reserva incompletos"));
            }

            if (detalle.getHoraInicio() >= detalle.getHoraFin()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La hora de inicio debe ser menor a la hora de fin"));
            }

            // 2. Verificar usuario autenticado
            Usuarios vecino = (Usuarios) session.getAttribute("usuario");
            if (vecino == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            System.out.println("Usuario autenticado: " + vecino.getNombres());

            // 3. Buscar espacio en la base de datos
            Espacio espacio = espacioRepository.findById(detalle.getIdEspacio()).orElse(null);
            if (espacio == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Espacio no encontrado"));
            }
            System.out.println("Espacio encontrado: " + espacio.getNombre());

            // 4. Verificar disponibilidad del espacio
            LocalDate fecha = LocalDate.parse(detalle.getFecha());
            LocalTime horaInicio = LocalTime.of(detalle.getHoraInicio(), 0);
            LocalTime horaFin = LocalTime.of(detalle.getHoraFin(), 0);

            // 🔧 NUEVA VALIDACIÓN: Verificar conflictos con mantenimientos PRIMERO
            try {
                System.out.println("🔍 Verificando conflictos con mantenimientos en pago...");
                mantenimientoService.verificarConflictosConMantenimientos(
                        espacio, fecha, horaInicio, horaFin);
            } catch (IllegalArgumentException e) {
                System.err.println("❌ Conflicto con mantenimiento en pago: " + e.getMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Espacio no disponible: " + e.getMessage()));
            }

            // Verificar conflictos de horario con otras reservas
            List<Reserva> conflictos = reservaRepositoryVecino.findConflictosEnHorario(
                    espacio.getIdEspacio(), fecha, horaInicio, horaFin);

            if (!conflictos.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El espacio no está disponible en el horario solicitado"));
            }

            // 5. Calcular costo total
            int horas = detalle.getHoraFin() - detalle.getHoraInicio();
            double costoTotal = espacio.getCosto() * horas;
            System.out.println("Costo calculado: " + costoTotal + " PEN (" + horas + " horas)");

            // 6. Guardar datos de reserva en sesión
            Map<String, Object> reservaData = new HashMap<>();
            reservaData.put("idVecino", vecino.getIdUsuarios());
            reservaData.put("idEspacio", espacio.getIdEspacio());
            reservaData.put("fecha", fecha.toString());
            reservaData.put("horaInicio", horaInicio.toString());
            reservaData.put("horaFin", horaFin.toString());
            reservaData.put("costo", costoTotal);
            reservaData.put("timestamp", LocalDateTime.now());
            session.setAttribute("reservaEnProceso", reservaData);
            System.out.println("Datos de reserva guardados en sesión");

            // 7. Crear preferencia de MercadoPago
            String initPoint = crearPreferenciaMercadoPago(espacio, costoTotal);
            System.out.println("Preferencia creada exitosamente, URL: " + initPoint);

            // IMPORTANTE: Devolver JSON con la URL
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", initPoint,
                    "message", "Preferencia creada exitosamente"
            ));

        } catch (Exception e) {
            System.err.println("Error en crearPreferencia: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la reserva: " + e.getMessage()));
        }
    }

    /**
     * Método privado para crear la preferencia en MercadoPago
     */
    private String crearPreferenciaMercadoPago(Espacio espacio, double costo) {
        try {
            System.out.println("=== CREANDO PREFERENCIA MERCADOPAGO ===");
            System.out.println("Access Token: " + (mercadoPagoAccessToken != null ? "Configurado" : "NULO"));
            System.out.println("Base URL: " + baseUrl);
            System.out.println("Espacio: " + espacio.getNombre());
            System.out.println("Costo: " + costo);

            // Limpiar URL base para evitar dobles barras
            String cleanedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

            // Crear el item de la preferencia
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id("espacio-" + espacio.getIdEspacio())
                    .title("Reserva: " + espacio.getNombre())
                    .description("Reserva del espacio deportivo " + espacio.getNombre() + 
                               (espacio.getDescripcion() != null ? " - " + espacio.getDescripcion() : ""))
                    .quantity(1)
                    .unitPrice(new BigDecimal(String.valueOf(costo)))
                    .currencyId("PEN")
                    .categoryId("services")
                    .build();

            System.out.println("Item creado: " + itemRequest.getTitle() + " - " + itemRequest.getUnitPrice() + " PEN");

            // Configurar URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(cleanedBaseUrl + "/vecino/pago-exitoso")
                    .failure(cleanedBaseUrl + "/vecino/pago-fallido")
                    .pending(cleanedBaseUrl + "/vecino/pago-pendiente")
                    .build();

            System.out.println("URLs de retorno configuradas:");
            System.out.println("- Éxito: " + cleanedBaseUrl + "/vecino/pago-exitoso");
            System.out.println("- Fallo: " + cleanedBaseUrl + "/vecino/pago-fallido");
            System.out.println("- Pendiente: " + cleanedBaseUrl + "/vecino/pago-pendiente");

            // Crear la solicitud de preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference("reserva-" + UUID.randomUUID())
                    .notificationUrl(cleanedBaseUrl + "/pago/mercadopago/webhooks/mercadopago")
                    .expirationDateTo(OffsetDateTime.now().plusHours(1)) // ⭐ AGREGAR ESTA LÍNEA
                    .build();

            // Crear la preferencia usando el cliente de MercadoPago
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            System.out.println("Preferencia creada exitosamente:");
            System.out.println("- ID: " + preference.getId());
            System.out.println("- Init Point: " + preference.getInitPoint());
            System.out.println("- External Reference: " + preference.getExternalReference());

            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException e) {
            System.err.println("=== ERROR DE MERCADOPAGO API ===");
            System.err.println("Código de estado: " + e.getStatusCode());
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Respuesta API: " + e.getApiResponse());
            throw new RuntimeException("Error en la API de MercadoPago: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("=== ERROR GENERAL EN CREACIÓN DE PREFERENCIA ===");
            System.err.println("Tipo de error: " + e.getClass().getSimpleName());
            System.err.println("Mensaje: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error general al crear preferencia: " + e.getMessage());
        }
    }

    @PostMapping("/webhooks/mercadopago")
    public ResponseEntity<String> recibirWebhook(@RequestBody String body) {
        System.out.println("📥 Webhook de MercadoPago recibido:");
        System.out.println("Contenido: " + body);

        try {
            // Parsear el JSON del webhook
            ObjectMapper mapper = new ObjectMapper();
            JsonNode webhook = mapper.readTree(body);

            String action = webhook.path("action").asText();
            String type = webhook.path("type").asText();

            System.out.println("Tipo: " + type + ", Acción: " + action);

            // Procesar solo notificaciones de pago
            if ("payment".equals(type) && "payment.updated".equals(action)) {
                Long paymentId = webhook.path("data").path("id").asLong();
                System.out.println("ID de pago a verificar: " + paymentId);

                // TODO: Verificar el estado del pago en MercadoPago
                // TODO: Actualizar el estado de la reserva si es necesario
                // TODO: Enviar notificaciones al usuario
            }

            System.out.println("✅ Webhook procesado correctamente");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            System.err.println("❌ Error procesando webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }
}