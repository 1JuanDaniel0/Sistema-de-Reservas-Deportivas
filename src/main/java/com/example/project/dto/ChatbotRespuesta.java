package com.example.project.dto;

import java.util.Map;

public class ChatbotRespuesta {
    private String respuesta;
    private String accion;
    private Map<String, Object> parametros;
    private String[] relevante_para;
    private double nivel_confianza;

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
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
    public String[] getRelevante_para() {
        return relevante_para;
    }

    public void setRelevante_para(String[] relevante_para) {
        this.relevante_para = relevante_para;
    }

    public double getNivel_confianza() {
        return nivel_confianza;
    }

    public void setNivel_confianza(double nivel_confianza) {
        this.nivel_confianza = nivel_confianza;
    }

    private Map<String, String> sugerencias;

    public Map<String, String> getSugerencias() {
        return sugerencias;
    }

    public void setSugerencias(Map<String, String> sugerencias) {
        this.sugerencias = sugerencias;
    }

}
