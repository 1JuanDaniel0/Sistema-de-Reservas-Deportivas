package com.example.project.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {

    /**
     * Obtiene la IP real del cliente incluso si está detrás de un proxy como NGINX o Cloudflare.
     * @param request HttpServletRequest actual.
     * @return IP pública real del cliente.
     */
    public static String obtenerIpReal(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "CF-Connecting-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim(); // toma la primera si hay múltiples
            }
        }

        return request.getRemoteAddr();
    }
}