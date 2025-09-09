package com.example.project.dto;

import java.util.Map;

public class ChatRequest {
    private String mensaje;
    private String accion;
    private Map<String, Object> parametros; // antes era Map<String, String>

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Map<String, Object> getParametros() {
        return parametros;
    }

    public void setParametros(Map<String, Object> parametros) {
        this.parametros = parametros;
    }
}