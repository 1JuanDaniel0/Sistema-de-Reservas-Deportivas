package com.example.project.controller;

import com.example.project.entity.*;
import com.example.project.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vecino")
public class VecinoPagosController {

    @Autowired private PagoRepository pagoRepositoryVecino;

    /**
     * Mostrar página de mis pagos con datos iniciales
     */
    @GetMapping("/mis-pagos")
    public String verPagos(HttpSession session, Model model) {
        try {
            System.out.println("💰 Cargando página de mis pagos...");

            Usuarios vecino = (Usuarios) session.getAttribute("usuario");
            if (vecino == null) {
                System.err.println("❌ Usuario no autenticado");
                return "redirect:/login";
            }

            // Obtener datos básicos para la página
            List<Pago> todosPagos = pagoRepositoryVecino.findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(
                    vecino.getIdUsuarios()
            );

            // Obtener lista de espacios únicos para el filtro
            Set<Espacio> espaciosUtilizados = todosPagos.stream()
                    .filter(p -> p.getReserva() != null && p.getReserva().getEspacio() != null)
                    .map(p -> p.getReserva().getEspacio())
                    .collect(Collectors.toSet());

            // Obtener tipos de pago únicos
            Set<String> tiposPago = todosPagos.stream()
                    .map(Pago::getTipoPago)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Obtener estados únicos
            Set<String> estados = todosPagos.stream()
                    .map(Pago::getEstado)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Calcular estadísticas básicas
            long totalPagos = todosPagos.size();
            double montoTotal = todosPagos.stream()
                    .filter(p -> p.getMonto() != null)
                    .mapToDouble(p -> p.getMonto().doubleValue())
                    .sum();

            model.addAttribute("vecino", vecino);
            model.addAttribute("totalPagos", totalPagos);
            model.addAttribute("montoTotal", montoTotal);
            model.addAttribute("espaciosUtilizados", espaciosUtilizados);
            model.addAttribute("tiposPago", tiposPago);
            model.addAttribute("estados", estados);

            System.out.println("✅ Página de pagos cargada exitosamente");
            System.out.println("📊 Total de pagos: " + totalPagos);
            System.out.println("💰 Monto total: S/. " + montoTotal);

            return "vecino/vecino-ver-pagos";

        } catch (Exception e) {
            System.err.println("❌ Error cargando página de pagos: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar los pagos");
            return "error/500";
        }
    }

    /**
     * Obtener pagos con filtros aplicados
     */
    private List<Pago> obtenerPagosConFiltros(Usuarios vecino, String fechaInicio, String fechaFin,
                                              String tipoPago, String estado, Integer espacio) {

        List<Pago> pagos = pagoRepositoryVecino.findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(
                vecino.getIdUsuarios()
        );

        return pagos.stream()
                .filter(pago -> {
                    // Filtro por fecha
                    if (fechaInicio != null && !fechaInicio.isEmpty()) {
                        LocalDate fechaInicioDate = LocalDate.parse(fechaInicio);
                        if (pago.getFechaPago().toLocalDate().isBefore(fechaInicioDate)) {
                            return false;
                        }
                    }

                    if (fechaFin != null && !fechaFin.isEmpty()) {
                        LocalDate fechaFinDate = LocalDate.parse(fechaFin);
                        if (pago.getFechaPago().toLocalDate().isAfter(fechaFinDate)) {
                            return false;
                        }
                    }

                    // Filtro por tipo de pago
                    if (tipoPago != null && !tipoPago.isEmpty() && !tipoPago.equals(pago.getTipoPago())) {
                        return false;
                    }

                    // Filtro por estado
                    if (estado != null && !estado.isEmpty() && !estado.equals(pago.getEstado())) {
                        return false;
                    }

                    // Filtro por espacio
                    if (espacio != null && pago.getReserva() != null && pago.getReserva().getEspacio() != null) {
                        if (!espacio.equals(pago.getReserva().getEspacio().getIdEspacio())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }
}