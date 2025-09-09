package com.example.project.controller;

import com.example.project.entity.Espacio;
import com.example.project.repository.EspacioRepositoryGeneral;
import com.example.project.service.EspacioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/espacios")
public class EspacioEstadoController {

    @Autowired private EspacioService espacioService;
    @Autowired private EspacioRepositoryGeneral espacioRepository;

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora) {

        Espacio espacio = espacioRepository.findById(id).orElse(null);
        if (espacio == null) {
            return ResponseEntity.notFound().build();
        }

        boolean enMantenimiento = espacioService.estaEnMantenimiento(espacio, fecha, hora);

        Map<String, Object> response = new HashMap<>();
        response.put("disponible", !enMantenimiento);
        response.put("motivo", enMantenimiento ? "En mantenimiento programado" : "Disponible");
        response.put("estadoActual", espacio.getIdEstadoEspacio().getEstado());

        return ResponseEntity.ok(response);
    }
}