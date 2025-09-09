package com.example.project.repository.admin;

public interface Cant_espaciosDto {
    String getTipo(); // Tipo de espacio (ej. "Piscinas", "Gimnasios")
    int getCantidad(); // Cantidad de espacios de este tipo
    int getReservationCount(); // Cantidad de reservas para este tipo
    double getReservationPercentage(); // Porcentaje de reservas
}