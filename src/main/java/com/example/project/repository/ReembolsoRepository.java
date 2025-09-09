package com.example.project.repository;

import com.example.project.dto.ReembolsoDTO;
import com.example.project.entity.Reembolso;
import com.example.project.entity.SolicitudCancelacion;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, Integer> {

    // Buscar reembolso por solicitud de cancelación
    Optional<Reembolso> findBySolicitudCancelacion(SolicitudCancelacion solicitud);

    /**
     * Buscar todos los reembolsos con información completa para historial
     */
    @Query("SELECT r FROM Reembolso r " +
            "JOIN FETCH r.solicitudCancelacion sc " +
            "JOIN FETCH r.reserva res " +
            "JOIN FETCH res.vecino v " +
            "JOIN FETCH res.espacio e " +
            "JOIN FETCH e.tipoEspacio te " +
            "ORDER BY r.fechaCreacion DESC")
    List<Reembolso> findAllWithCompleteInfo();

    // Verificar si existe reembolso para una solicitud
    boolean existsBySolicitudCancelacion(SolicitudCancelacion solicitud);

    // Buscar reembolsos pendientes de procesamiento manual
    @Query("SELECT r FROM Reembolso r WHERE r.estadoReembolso = 'PENDIENTE' " +
            "AND r.tipoPagoOriginal = 'En banco' " +
            "ORDER BY r.fechaCreacion ASC")
    List<Reembolso> findReembolsosPendientesManuales();

    @Query("SELECT DISTINCT r FROM Reembolso r " +
            "JOIN FETCH r.solicitudCancelacion sc " +
            "JOIN FETCH r.reserva res " +
            "JOIN FETCH res.vecino v " +
            "JOIN FETCH res.espacio e " +
            "JOIN FETCH e.tipoEspacio te " +
            "WHERE r.estadoReembolso = 'PENDIENTE' " +
            "AND r.tipoPagoOriginal = 'En banco' " +
            "AND sc.estado = 'Aprobado' " +
            "ORDER BY r.fechaCreacion ASC")
    List<Reembolso> findReembolsosPendientesUnicos();

    // Buscar reembolsos por estado
    List<Reembolso> findByEstadoReembolsoOrderByFechaCreacionDesc(Reembolso.EstadoReembolso estado);

    // Buscar reembolsos procesados por un administrador específico
    List<Reembolso> findByProcesadoPorAdminOrderByFechaProcesamientoDesc(Usuarios admin);

    // Buscar reembolsos aprobados por un coordinador específico
    List<Reembolso> findByAprobadoPorCoordinadorOrderByFechaAprobacionDesc(Usuarios coordinador);

    // Contar reembolsos pendientes de procesamiento manual
    @Query("SELECT COUNT(r) FROM Reembolso r WHERE r.estadoReembolso = 'PENDIENTE' " +
            "AND r.tipoPagoOriginal = 'En banco'")
    long countReembolsosPendientesManuales();

    // Buscar reembolsos en un rango de fechas
    @Query("SELECT r FROM Reembolso r WHERE r.fechaCreacion BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY r.fechaCreacion DESC")
    List<Reembolso> findByFechaCreacionBetween(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // Buscar reembolsos por método de reembolso
    List<Reembolso> findByMetodoReembolsoOrderByFechaCreacionDesc(Reembolso.MetodoReembolso metodo);

    // Calcular monto total de reembolsos en un período
    @Query("SELECT COALESCE(SUM(r.montoReembolso), 0) FROM Reembolso r " +
            "WHERE r.estadoReembolso = 'COMPLETADO' " +
            "AND r.fechaProcesamiento BETWEEN :fechaInicio AND :fechaFin")
    Double sumMontoReembolsosCompletados(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // Buscar reembolsos por ID de transacción de MercadoPago
    Optional<Reembolso> findByIdTransaccionReembolso(String idTransaccion);

    // Buscar reembolsos urgentes (pendientes hace más de X días)
    @Query("SELECT r FROM Reembolso r WHERE r.estadoReembolso = 'PENDIENTE' " +
            "AND r.tipoPagoOriginal = 'En banco' " +
            "AND r.fechaCreacion < :fechaLimite " +
            "ORDER BY r.fechaCreacion ASC")
    List<Reembolso> findReembolsosUrgentes(@Param("fechaLimite") LocalDateTime fechaLimite);

    /**
     * Obtener historial como DTOs con filtros
     */
    @Query("SELECT new com.example.project.dto.ReembolsoDTO(" +
            "r.idReembolso, " +
            "sc.id, " +
            "res.idReserva, " +
            "res.vecino.nombres, " +
            "res.vecino.apellidos, " +
            "res.vecino.dni, " +
            "res.vecino.correo, " +
            "res.espacio.nombre, " +
            "res.fecha, " +
            "res.horaInicio, " +
            "res.horaFin, " +
            "r.montoReembolso, " +
            "r.tipoPagoOriginal, " +
            "CAST(r.estadoReembolso AS string), " +
            "CAST(r.metodoReembolso AS string), " +
            "r.fechaCreacion, " +
            "r.fechaProcesamiento, " +
            "r.aprobadoPorCoordinador.nombres, " +
            "r.aprobadoPorCoordinador.apellidos, " +
            "r.motivoAprobacion, " +
            "r.fechaAprobacion, " +
            "r.procesadoPorAdmin.nombres, " +
            "r.procesadoPorAdmin.apellidos, " +
            "r.numeroOperacion, " +
            "r.entidadBancaria, " +
            "r.observacionesAdmin, " +
            "sc.motivo, " +
            "sc.fechaSolicitud, " +
            "sc.codigoPago, " +
            "sc.comprobanteUrl, " +
            "r.idTransaccionReembolso" +
            ") " +
            "FROM Reembolso r " +
            "JOIN r.solicitudCancelacion sc " +
            "JOIN r.reserva res " +
            "WHERE (:estadoReembolso IS NULL OR r.estadoReembolso = :estadoReembolso) AND " +
            "(:tipoPago IS NULL OR res.tipoPago = :tipoPago) AND " +
            "(:idEspacio IS NULL OR res.espacio.idEspacio = :idEspacio) AND " +
            "(:fechaInicio IS NULL OR r.fechaCreacion >= :fechaInicio) AND " +
            "(:fechaFin IS NULL OR r.fechaCreacion <= :fechaFin) " +
            "ORDER BY r.fechaCreacion DESC")
    List<ReembolsoDTO> findHistorialDTOWithFilters(
            @Param("estadoReembolso") Reembolso.EstadoReembolso estadoReembolso,
            @Param("tipoPago") String tipoPago,
            @Param("idEspacio") Integer idEspacio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Obtener DTO específico por ID
     */
    @Query("SELECT new com.example.project.dto.ReembolsoDTO(" +
            "r.idReembolso, " +
            "sc.id, " +
            "res.idReserva, " +
            "res.vecino.nombres, " +
            "res.vecino.apellidos, " +
            "res.vecino.dni, " +
            "res.vecino.correo, " +
            "res.espacio.nombre, " +
            "res.fecha, " +
            "res.horaInicio, " +
            "res.horaFin, " +
            "r.montoReembolso, " +
            "r.tipoPagoOriginal, " +
            "CAST(r.estadoReembolso AS string), " +
            "CAST(r.metodoReembolso AS string), " +
            "r.fechaCreacion, " +
            "r.fechaProcesamiento, " +
            "r.aprobadoPorCoordinador.nombres, " +
            "r.aprobadoPorCoordinador.apellidos, " +
            "r.motivoAprobacion, " +
            "r.fechaAprobacion, " +
            "r.procesadoPorAdmin.nombres, " +
            "r.procesadoPorAdmin.apellidos, " +
            "r.numeroOperacion, " +
            "r.entidadBancaria, " +
            "r.observacionesAdmin, " +
            "sc.motivo, " +
            "sc.fechaSolicitud, " +
            "sc.codigoPago, " +
            "sc.comprobanteUrl, " +
            "r.idTransaccionReembolso" +
            ") " +
            "FROM Reembolso r " +
            "JOIN r.solicitudCancelacion sc " +
            "JOIN r.reserva res " +
            "WHERE sc.id = :idSolicitud")
    Optional<ReembolsoDTO> findDTOBySolicitudId(@Param("idSolicitud") Integer idSolicitud);

    /**
     * Contar reembolsos por coordinador aprobador y estado
     */
    long countByAprobadoPorCoordinadorAndEstadoReembolso(Usuarios coordinador, Reembolso.EstadoReembolso estado);

    /**
     * Buscar reembolsos por coordinador aprobador y estado
     */
    List<Reembolso> findByAprobadoPorCoordinadorAndEstadoReembolso(Usuarios coordinador, Reembolso.EstadoReembolso estado);
}