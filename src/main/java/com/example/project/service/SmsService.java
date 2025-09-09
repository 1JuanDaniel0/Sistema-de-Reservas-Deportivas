package com.example.project.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Random;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        System.out.println("📲 Twilio inicializado correctamente");
    }

    public String sendOtpSms(String toPhoneNumber, String otpCode) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),        // Destino
                    new PhoneNumber(twilioPhoneNumber),    // Número de Twilio
                    "Tu código de verificación de la Municipalidad de San Miguel es: " + otpCode
            ).create();

            System.out.println("✅ SMS enviado con SID: " + message.getSid());
            return message.getSid();
        } catch (Exception e) {
            System.err.println("❌ Error al enviar SMS: " + e.getMessage());
            return null;
        }
    }

    public String generateOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }
}
