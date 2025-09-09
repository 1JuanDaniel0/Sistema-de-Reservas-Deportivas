package com.example.project.service;

import com.example.project.entity.IntentoIp;
import com.example.project.repository.IntentoIpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IntentoIpService {

    @Autowired
    private IntentoIpRepository intentoIpRepository;

    public boolean verificaYActualizaIntento(String ip, String tipo, Duration ventanaTiempo, int maxIntentos) {
        LocalDateTime ahora = LocalDateTime.now();

        Optional<IntentoIp> intentoOpt = intentoIpRepository.findByIpAndTipo(ip, tipo);
        if (intentoOpt.isPresent()) {
            IntentoIp intento = intentoOpt.get();
            LocalDateTime ultima = intento.getUltimaSolicitud();

            if (Duration.between(ultima, ahora).compareTo(ventanaTiempo) <= 0) {
                // Si aún estamos dentro de la ventana
                if (intento.getContador() >= maxIntentos) {
                    System.out.println("⛔ IP " + ip + " bloqueada por exceder " + maxIntentos + " intentos en " + ventanaTiempo.toMinutes() + " min.");
                    return false;
                } else {
                    intento.setContador(intento.getContador() + 1);
                }
            } else {
                // Ha pasado la ventana, reiniciamos contador
                intento.setContador(1);
            }

            intento.setUltimaSolicitud(ahora);
            intentoIpRepository.save(intento);

        } else {
            IntentoIp nuevo = new IntentoIp();
            nuevo.setIp(ip);
            nuevo.setTipo(tipo);
            nuevo.setUltimaSolicitud(ahora);
            nuevo.setContador(1);
            intentoIpRepository.save(nuevo);
        }

        return true;
    }

}
