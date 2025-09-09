package com.example.project.controller;

import com.example.project.dto.ExportDataDTO;
import com.example.project.entity.*;
import com.example.project.repository.*;
import com.example.project.repository.coordinador.ActividadRepository;
import com.example.project.repository.coordinador.GeolocalizacionRepository;
import com.example.project.service.ExportarServiceGeneral;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/export")
public class ExportController {

    @Autowired private ExportarServiceGeneral exportarService;
    @Autowired private EspacioRepositoryGeneral espacioRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private GeolocalizacionRepository geolocalizacionRepository;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private PagoRepository pagoRepositoryVecino;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Exportar espacios - Solo Admin y SuperAdmin
     */
    @GetMapping("/espacios")
    @PreAuthorize("hasAnyAuthority('ROLE_Administrador', 'ROLE_SuperAdmin')")
    public void exportarEspacios(@RequestParam String formato, HttpServletResponse response) throws IOException {
        List<Espacio> espacios = espacioRepository.findAll();

        ExportDataDTO<Espacio> config = ExportDataDTO.<Espacio>builder()
                .title("Lista de Espacios Deportivos")
                .fileName("espacios_deportivos")
                .sheetName("Espacios")
                .headers(new String[]{"Nombre", "Tipo", "Lugar", "Costo", "Estado"})
                .columnWidths(new float[]{3, 3, 3, 2, 2})
                .landscape(true)
                .dataExtractors(List.of(
                        esp -> esp.getNombre(),
                        esp -> esp.getTipoEspacio().getNombre(),
                        esp -> esp.getIdLugar().getLugar(),
                        esp -> "S/ " + String.format("%.2f", esp.getCosto()),
                        esp -> esp.getIdEstadoEspacio().getEstado()
                ))
                .build();

        exportarPorFormato(espacios, config, formato, response);
    }

    /**
     * Exportar reservas - Solo Admin y SuperAdmin
     */
    @GetMapping("/reservas")
    @PreAuthorize("hasAnyAuthority('ROLE_Administrador', 'ROLE_SuperAdmin')")
    public void exportarReservas(@RequestParam String formato, HttpServletResponse response) throws IOException {
        // List<Reserva> reservas = reservaRepository.findAll();

        ExportDataDTO<Reserva> config = ExportDataDTO.<Reserva>builder()
                .title("Lista de Reservas")
                .fileName("reservas")
                .sheetName("Reservas")
                .headers(new String[]{"Espacio", "Vecino", "Fecha", "Hora Inicio", "Hora Fin", "Estado", "Costo"})
                .columnWidths(new float[]{3, 3, 2, 2, 2, 2, 2})
                .landscape(true)
                .dataExtractors(List.of(
                        r -> r.getEspacio().getNombre(),
                        r -> r.getVecino().getNombres() + " " + r.getVecino().getApellidos(),
                        r -> String.valueOf(r.getFecha()),
                        r -> r.getHoraInicio().toString(),
                        r -> r.getHoraFin().toString(),
                        r -> r.getEstado().getEstado(),
                        r -> "S/ " + String.format("%.2f", r.getCosto())
                ))
                .build();

        // exportarPorFormato(reservas, config, formato, response);
    }

    /**
     * Exportar mi actividad (admin)
     */
    @GetMapping("/mi-actividad")
    @PreAuthorize("hasAnyAuthority('ROLE_Administrador')")
    public void exportarMiActividad(@RequestParam String formato, HttpServletResponse response, HttpSession session) throws IOException {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        if (usuario.getRol().getRol().equals("Administrador")){
            List<Actividad> actividad = actividadRepository.findByUsuarioOrderByFechaDesc(usuario);
            String titulo = "REGISTRO DE ACTIVIDAD DE " + usuario.getNombres().toUpperCase() + " " + usuario.getApellidos().toUpperCase() + "\n";
            String nombreArchivo = "actividad_" + usuario.getDni();

            ExportDataDTO<Actividad> config = ExportDataDTO.<Actividad>builder()
                    .title(titulo)
                    .fileName(nombreArchivo)
                    .sheetName("Actividad")
                    .headers(new String[]{"Fecha", "Descripcion", "Detalle"})
                    .columnWidths(new float[]{3, 4, 6})
                    .landscape(true)
                    .dataExtractors(List.of(
                            act -> act.getFecha().format(LocalDateTimeFormatter),
                            act -> act.getDescripcion(),
                            act -> act.getDetalle()
                    ))
                    .build();

            exportarPorFormato(actividad, config, formato, response);
        }
    }

    /**
     * Exportar lista de coordinadores
     */
    @GetMapping("/lista-coordinadores")
    @PreAuthorize("hasAnyAuthority('ROLE_Administrador', 'ROLE_SuperAdmin')")
    public void exportarListaCoordinadores(@RequestParam String formato,
                                           HttpServletResponse response,
                                           HttpSession session) throws IOException {

        List<Usuarios> coordinadores = usuariosRepository.findByRol_Rol("Coordinador");
        String titulo = "Lista de Coordinadores\n\n";
        String nombreArchivo = "lista_coordinadores";

        ExportDataDTO<Usuarios> config = ExportDataDTO.<Usuarios>builder()
                .title(titulo)
                .fileName(nombreArchivo)
                .sheetName("coordinadores")
                .headers(new String[]{"#", "Nombres", "Apellidos", "DNI", "Correo", "Estado", "Lugares Asignados"})
                .columnWidths(new float[]{0.3F, 1.5F, 1.5F, 1, 2, 0.75F, 4})
                .landscape(true)
                .dataExtractors(List.of(
                        coord -> String.valueOf(coord.getIdUsuarios()),
                        coord -> coord.getNombres(),
                        coord -> coord.getApellidos(),
                        coord -> coord.getDni(),
                        coord -> coord.getCorreo(),
                        coord -> coord.getEstado().getEstado(),
                        coord -> formatearLugares(coord.getLugaresAsignados())
                ))
                .build();
        exportarPorFormato(coordinadores, config, formato, response);
    }

    private String formatearLugares(List<Lugar> lugares) {
        if (lugares == null || lugares.isEmpty()) {
            return "Sin lugares asignados";
        }

        return lugares.stream()
                .map(Lugar::getLugar)
                .collect(Collectors.joining(", "));
    }

    /**
     * Exportar actividad de coordinadores - Admin y Coordinador
     */
    @GetMapping("/actividad-coordinadores")
    @PreAuthorize("hasAnyAuthority('ROLE_Administrador', 'ROLE_Coordinador', 'ROLE_SuperAdmin')")
    public void exportarActividadCoordinadores(@RequestParam String formato,
                                               HttpServletResponse response,
                                               HttpSession session) throws IOException {

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        // Si es coordinador, solo puede ver y exportar su propia actividad
        if (usuario.getRol().getRol().equals("Coordinador")) {
            List<Actividad> actividad = actividadRepository.findByUsuarioOrderByFechaDesc(usuario);
            String coordinador = usuario.getNombres().toUpperCase() + " " + usuario.getApellidos().toUpperCase() + " (" + usuario.getDni() + ")";
            String titulo = "REGISTRO DE ACTIVIDAD DE " + coordinador + "\n";
            String nombreArchivo = "actividad_" + usuario.getDni();

            ExportDataDTO<Actividad> config = ExportDataDTO.<Actividad>builder()
                    .title(titulo)
                    .fileName(nombreArchivo)
                    .sheetName("Actividad")
                    .headers(new String[]{"Fecha", "Descripcion", "Detalle"})
                    .columnWidths(new float[]{3, 4, 6})
                    .landscape(true)
                    .dataExtractors(List.of(
                            act -> act.getFecha().format(LocalDateTimeFormatter),
                            act -> act.getDescripcion(),
                            act -> act.getDetalle()
                    ))
                    .build();

            exportarPorFormato(actividad, config, formato, response);

        } else if (usuario.getRol().getRol().equals("Administrador") || usuario.getRol().getRol().equals("SuperAdmin")) {
            // Admin puede ver toda la actividad
            List<Actividad> actividad = actividadRepository.findByUsuario_Rol_RolOrderByFechaDesc("Coordinador");
            String titulo = "Registro de Actividad de Todos los Coordinadores\n";

            ExportDataDTO<Actividad> config = ExportDataDTO.<Actividad>builder()
                    .title(titulo)
                    .fileName("actividad_coordinadores")
                    .sheetName("Actividad")
                    .headers(new String[]{"Coordinador", "Descripcion", "Detalle", "Fecha"})
                    .columnWidths(new float[]{4, 5, 6, 3})
                    .landscape(true)
                    .dataExtractors(List.of(
                            act -> act.getUsuario().getNombres() + " " + act.getUsuario().getApellidos(),
                            act -> act.getDescripcion(),
                            act -> act.getDetalle(),
                            act -> act.getFecha().format(LocalDateTimeFormatter)
                    ))
                    .build();

            exportarPorFormato(actividad, config, formato, response);
        }
    }

    /**
     * Asistencia Coordinadores - Reporte del coordinador y Admin
     */
    @GetMapping("/asistencia-coordinadores")
    @PreAuthorize("hasAnyAuthority('ROLE_Administrador', 'ROLE_Coordinador', 'ROLE_SuperAdmin')")
    public void exportarAsistenciaCoordinadores(@RequestParam String formato,
                                                HttpServletResponse response,
                                                HttpSession session) throws IOException {
        Usuarios usuario = (Usuarios) session.getAttribute("usuario");

        // Si es coordinador, solo puede ver y exportar su propia lista de asistencia
        if (usuario.getRol().getRol().equals("Coordinador")) {

            List<Geolocalizacion> geolocalizacion = geolocalizacionRepository.findByCoordinadorOrderByFechaDesc(usuario);
            String coordinador = usuario.getNombres().toUpperCase() + " " + usuario.getApellidos().toUpperCase() + " (" + usuario.getDni() + ")";
            String titulo = "REGISTRO DE ASISTENCIAS DE " + coordinador + "\n";
            String nombreArchivo = "asistencias_" + usuario.getDni();

            ExportDataDTO<Geolocalizacion> config = ExportDataDTO.<Geolocalizacion>builder()
                    .title(titulo)
                    .fileName(nombreArchivo)
                    .sheetName("Asistencias")
                    .headers(new String[]{"Fecha", "Hora Entrada", "Hora Salida", "Lugar", "Observaciones", "Estado"})
                    .columnWidths(new float[]{2, 2, 2, 3, 4, 2})
                    .landscape(true)
                    .dataExtractors(List.of(
                            geo -> geo.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), // CORREGIDO
                            geo -> String.valueOf(geo.getHoraInicio()),
                            geo -> String.valueOf(geo.getHoraFin()),
                            geo -> geo.getLugar().getLugar(),
                            geo -> geo.getObservacion(),
                            geo -> geo.getEstado().getEstado()
                    ))
                    .build();
            exportarPorFormato(geolocalizacion, config, formato, response);
        } else if (usuario.getRol().getRol().equals("Administrador") || usuario.getRol().getRol().equals("SuperAdmin")) {
            // SI LO EXPORTA UN ADMIN
            List<Geolocalizacion> geolocalizacion = geolocalizacionRepository.findAll();
            String titulo = "REGISTRO DE ASISTENCIAS DE TODOS LOS COORDINADORES\n";
            String nombreArchivo = "asistencias_todos_coordinadores";

            ExportDataDTO<Geolocalizacion> config = ExportDataDTO.<Geolocalizacion>builder()
                    .title(titulo)
                    .fileName(nombreArchivo)
                    .sheetName("Asistencias Coordinadores")
                    .headers(new String[]{"Coordinador", "Fecha", "Hora Entrada", "Hora Salida", "Lugar", "Observaciones", "Estado"})
                    .columnWidths(new float[]{3, 1.5F, 1.5F, 1.5F, 3, 4, 1.5F})
                    .landscape(true)
                    .dataExtractors(List.of(
                            geo -> geo.getCoordinador().getNombres() + " " + geo.getCoordinador().getApellidos(),
                            geo -> geo.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), // CORREGIDO
                            geo -> String.valueOf(geo.getHoraInicio()),
                            geo -> String.valueOf(geo.getHoraFin()),
                            geo -> geo.getLugar().getLugar(),
                            geo -> geo.getObservacion(),
                            geo -> geo.getEstado().getEstado()
                    ))
                    .build();
            exportarPorFormato(geolocalizacion, config, formato, response);
        }
    }

    /**
     * Exportar usuarios - Solo SuperAdmin
     */
    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('ROLE_SuperAdmin')")
    public void exportarUsuarios(@RequestParam String formato, HttpServletResponse response) throws IOException {
        // Implementar según tu repositorio de usuarios
        // List<Usuarios> usuarios = usuarioRepository.findAll();

        ExportDataDTO<Usuarios> config = ExportDataDTO.<Usuarios>builder()
                .title("Lista de Usuarios")
                .fileName("usuarios")
                .sheetName("Usuarios")
                .headers(new String[]{"Nombres", "Apellidos", "DNI", "Correo", "Rol", "Estado"})
                .columnWidths(new float[]{2, 2, 2, 3, 2, 2})
                .landscape(true)
                .dataExtractors(List.of(
                        u -> u.getNombres(),
                        u -> u.getApellidos(),
                        u -> u.getDni(),
                        u -> u.getCorreo(),
                        u -> u.getRol().getRol(),
                        u -> u.getEstado().getEstado()
                ))
                .build();

        // exportarPorFormato(usuarios, config, formato, response);
    }

    /**
     * Exportar mis pagos
     */
    @GetMapping("/mis-pagos")
    @PreAuthorize("hasAnyAuthority('ROLE_Usuario final', 'ROLE_SuperAdmin')")
    public void exportarMisPagos(
            @RequestParam String formato,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer espacio,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        Usuarios vecino = (Usuarios) session.getAttribute("usuario");
        if (vecino == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Obtener pagos con filtros
        List<Pago> pagos = obtenerPagosConFiltros(vecino, fechaInicio, fechaFin, tipoPago, estado, espacio);

        ExportDataDTO<Pago> config = ExportDataDTO.<Pago>builder()
                .title("Historial de Pagos - " + vecino.getNombres() + " " + vecino.getApellidos())
                .fileName("mis_pagos_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .sheetName("Mis Pagos")
                .headers(new String[]{"ID", "Fecha Pago", "Espacio", "Lugar", "Fecha Reserva", "Horario", "Tipo Pago", "Monto", "Estado"})
                .columnWidths(new float[]{1f, 2.5f, 2.5f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f})
                .landscape(true)
                .dataExtractors(List.of(
                        pago -> pago.getIdPago().toString(),
                        pago -> pago.getFechaPago() != null ?
                                pago.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "",
                        pago -> pago.getReserva() != null && pago.getReserva().getEspacio() != null ?
                                pago.getReserva().getEspacio().getNombre() : "N/A",
                        pago -> pago.getReserva() != null && pago.getReserva().getEspacio() != null &&
                                pago.getReserva().getEspacio().getIdLugar() != null ?
                                pago.getReserva().getEspacio().getIdLugar().getLugar() : "N/A",
                        pago -> pago.getReserva() != null && pago.getReserva().getFecha() != null ?
                                pago.getReserva().getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                        pago -> {
                            if (pago.getReserva() != null && pago.getReserva().getHoraInicio() != null &&
                                    pago.getReserva().getHoraFin() != null) {
                                return pago.getReserva().getHoraInicio() + " - " + pago.getReserva().getHoraFin();
                            }
                            return "";
                        },
                        pago -> pago.getTipoPago() != null ? pago.getTipoPago() : "",
                        pago -> pago.getMonto() != null ? "S/. " + String.format("%.2f", pago.getMonto().doubleValue()) : "S/. 0.00",
                        pago -> pago.getEstado() != null ? pago.getEstado() : ""
                ))
                .build();

        exportarPorFormato(pagos, config, formato, response);
    }

    /**
     * Método helper para obtener pagos con filtros
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

    /**
     * Método privado para manejar diferentes formatos
     */
    private <T> void exportarPorFormato(List<T> datos, ExportDataDTO<T> config,
                                        String formato, HttpServletResponse response) throws IOException {
        switch (formato.toLowerCase()) {
            case "pdf":
                exportarService.exportarAPdf(datos, config, response);
                break;
            case "xlsx":
            case "excel":
                exportarService.exportarAXlsx(datos, config, response);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no soportado: " + formato);
        }
    }


}