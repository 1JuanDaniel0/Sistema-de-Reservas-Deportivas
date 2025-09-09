package com.example.project.controller;

import com.example.project.entity.Usuarios;
import com.example.project.service.CalificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/calificacion")
public class CalificacionController {

    @Autowired
    private CalificacionService calificacionService;

    @PostMapping("/guardar")
    public String guardarCalificacion(
            @RequestParam("idReserva") Integer idReserva,
            @RequestParam("puntaje") Double puntaje,
            @RequestParam(value = "comentario", required = false) String comentario,
            HttpSession session
    ) {
        Usuarios vecino = (Usuarios) session.getAttribute("usuario");

        if (vecino == null) {
            return "No autorizado";
        }

        return calificacionService.calificarReserva(idReserva, vecino, puntaje, comentario);
    }
}
