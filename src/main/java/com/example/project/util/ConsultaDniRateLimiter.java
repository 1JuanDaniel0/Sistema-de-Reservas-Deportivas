package com.example.project.util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConsultaDniRateLimiter {

    // CAMBIAR SI ES QUE SE QUIERE CAMBIAR EL INTERVALO O EL NÚMERO DE INTENTOS DENTRO DEL INTERVALO DE TIEMPO
    private final Map<String, List<Long>> intentosPorIp = new ConcurrentHashMap<>();
    private final long VENTANA_TIEMPO_MS = 5 * 60 * 1000; // 5 minutos
    private final int MAX_INTENTOS = 10;

    public synchronized boolean estaBloqueado(String ip) {
        limpiarIntentosAntiguos(ip);
        List<Long> intentos = intentosPorIp.getOrDefault(ip, new ArrayList<>());
        return intentos.size() >= MAX_INTENTOS;
    }

    public synchronized void registrarIntento(String ip) {
        limpiarIntentosAntiguos(ip);
        intentosPorIp.computeIfAbsent(ip, k -> new ArrayList<>()).add(System.currentTimeMillis());
    }

    private void limpiarIntentosAntiguos(String ip) {
        List<Long> intentos = intentosPorIp.get(ip);
        if (intentos != null) {
            long ahora = System.currentTimeMillis();
            intentos.removeIf(ts -> ahora - ts > VENTANA_TIEMPO_MS);
            if (intentos.isEmpty()) {
                intentosPorIp.remove(ip);
            }
        }
    }
}