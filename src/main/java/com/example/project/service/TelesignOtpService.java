package com.example.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TelesignOtpService {

    @Value("${telesign.customer_id}")
    private String customerId;

    @Value("${telesign.api_key}")
    private String apiKey;

    private static final String ENDPOINT = "https://rest-api.telesign.com/v1/verify/sms";

    public boolean enviarOtp(String numero, String mensaje) {
        try {
            String phone = numero.replaceAll("\\D+", ""); // Elimina cualquier símbolo
            String template = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);

            String url = ENDPOINT + "?phone_number=" + phone + "&template=" + template;

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(customerId, apiKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            System.out.println("📤 Enviando OTP a: " + phone);
            System.out.println("🌐 Endpoint completo: " + url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);

            System.out.println("✅ Respuesta Telesign: " + response.getStatusCode());
            System.out.println("📨 Body: " + response.getBody());

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("🚨 Error al enviar OTP a " + numero);
            e.printStackTrace();
            return false;
        }
    }
}

