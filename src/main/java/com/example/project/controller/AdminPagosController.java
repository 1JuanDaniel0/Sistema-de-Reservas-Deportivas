package com.example.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/pagos")
public class AdminPagosController {

    @GetMapping("/registro")
    public String verRegistroPagos() {
        return "admin/registro-pagos";
    }
}
