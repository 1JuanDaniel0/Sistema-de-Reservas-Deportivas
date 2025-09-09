package com.example.project.dto;

public class TendenciaReservaDTO {
    private Integer anio;
    private Integer mes;
    private Integer dia;
    private String tipoEspacio;
    private Long totalReservas;

    public TendenciaReservaDTO(Integer anio, Integer mes, Integer dia, String tipoEspacio, Long totalReservas) {
        this.anio = anio;
        this.mes = mes;
        this.dia = dia;
        this.tipoEspacio = tipoEspacio;
        this.totalReservas = totalReservas;
    }

    // Getters y setters
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public Integer getDia() { return dia; }
    public void setDia(Integer dia) { this.dia = dia; }
    public String getTipoEspacio() { return tipoEspacio; }
    public void setTipoEspacio(String tipoEspacio) { this.tipoEspacio = tipoEspacio; }
    public Long getTotalReservas() { return totalReservas; }
    public void setTotalReservas(Long totalReservas) { this.totalReservas = totalReservas; }
}
