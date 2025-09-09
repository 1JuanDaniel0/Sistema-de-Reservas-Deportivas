package com.example.project.controller;

import com.example.project.entity.Usuarios;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminReembolsosController {

    /**
     * Vista principal de reembolsos pendientes
     */
    @GetMapping("/reembolsos/pendientes")
    public String reembolsosPendientes(Model model, HttpSession session) {
        System.out.println("🏦 Accediendo a vista de reembolsos pendientes");

        // Verificar autenticación
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario == null) {
            System.err.println("❌ Usuario no autenticado");
            return "redirect:/login";
        }

        // Verificar rol de administrador
        if (usuario.getRol() == null || !"Administrador".equals(usuario.getRol().getRol())) {
            System.err.println("❌ Usuario sin permisos de administrador");
            return "redirect:/acceso-denegado";
        }

        // Agregar información del usuario al modelo
        model.addAttribute("usuario", usuario);

        System.out.println("✅ Cargando vista de reembolsos pendientes para: " + usuario.getNombres());
        return "admin/reembolsos-pendientes";
    }

    /**
     * Vista de historial de reembolsos
     */
    @GetMapping("/reembolsos/historial")
    public String reembolsosHistorial(Model model, HttpSession session) {
        System.out.println("📋 Accediendo a historial de reembolsos");

        // Verificar autenticación
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Verificar rol de administrador
        if (usuario.getRol() == null || !"Administrador".equals(usuario.getRol().getRol())) {
            return "redirect:/acceso-denegado";
        }

        model.addAttribute("usuario", usuario);

        return "admin/reembolsos-historial"; // Nueva vista
    }

    /**
     * Vista de estadísticas de reembolsos (para implementar después)
     */
    @GetMapping("/reembolsos/estadisticas-dashboard")
    public String reembolsosEstadisticas(Model model, HttpSession session) {
        System.out.println("📊 Accediendo a estadísticas de reembolsos");

        // Verificar autenticación
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Verificar rol de administrador
        if (usuario.getRol() == null || !"Administrador".equals(usuario.getRol().getRol())) {
            return "redirect:/acceso-denegado";
        }

        model.addAttribute("usuario", usuario);

        // Por ahora redirigir a pendientes, después implementaremos las estadísticas
        return "redirect:/admin/reembolsos/pendientes";
    }
}