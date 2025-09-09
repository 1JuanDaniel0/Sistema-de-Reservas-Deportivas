package com.example.project.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMsg = "";

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            errorMsg = switch (statusCode) {
                case 403 -> "No tienes permiso para acceder a esta página";
                case 404 -> "Recurso no encontrado";
                case 500 -> "Error interno del servidor";
                default -> "Ha ocurrido un error inesperado";
            };

            model.addAttribute("errorCode", statusCode);
            model.addAttribute("errorMessage", errorMsg);
        }

        return "error/custom-error";
    }
}