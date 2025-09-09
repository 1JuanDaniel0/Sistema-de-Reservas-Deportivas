package com.example.project.util;

public class TelefonoUtils {

    /**
     * Normaliza un número de teléfono eliminando espacios, guiones, paréntesis, símbolos y prefijos como +51.
     * Ejemplos:
     * - "+51 999 123 456" → "999123456"
     * - "51-999123456" → "999123456"
     * - "999123456" → "999123456"
     */
    public static String normalizar(String telefono) {
        if (telefono == null) return null;

        // Quitar todo lo que no sea dígito
        String limpio = telefono.replaceAll("[^\\d]", "");

        // Quitar prefijo de país si comienza con 51 y tiene más de 9 dígitos
        if (limpio.startsWith("51") && limpio.length() > 9) {
            limpio = limpio.substring(2);
        }

        return limpio;
    }
}
