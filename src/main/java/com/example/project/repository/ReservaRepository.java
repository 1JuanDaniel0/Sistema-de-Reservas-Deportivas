package com.example.project.repository;

import com.example.project.dto.ReservaCalendarioDto;
import com.example.project.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    @Query("SELECT r FROM Reserva r WHERE r.espacio.idEspacio = :espacioId AND r.fecha = :fecha AND " +
            "((:horaInicio BETWEEN r.horaInicio AND r.horaFin) OR (:horaFin BETWEEN r.horaInicio AND r.horaFin) OR " +
            "(r.horaInicio BETWEEN :horaInicio AND :horaFin))")
    List<Reserva> findConflictosEnHorario(@Param("espacioId") int espacioId,
                                          @Param("fecha") LocalDate fecha,
                                          @Param("horaInicio") LocalTime horaInicio,
                                          @Param("horaFin") LocalTime horaFin);
    List<Reserva> findByEstado_EstadoAndFecha(String estado, LocalDate fecha);
    // Nuevo método que devuelve DTOs para evitar referencias circulares
    @Query("SELECT new com.example.project.dto.ReservaCalendarioDto(" +
            "r.idReserva, r.horaInicio, r.horaFin, r.fecha, r.costo, r.tipoPago, " +
            "r.momentoReserva, r.estado.estado, r.vecino.nombres, r.vecino.apellidos, r.espacio.nombre, r.vecino.idUsuarios) " +
            "FROM Reserva r " +
            "WHERE r.espacio.idEspacio = :idEspacio " +
            "AND r.estado.estado IN :estados " +
            "AND r.fecha BETWEEN :fechaInicio AND :fechaFin")
    List<ReservaCalendarioDto> buscarReservasParaCalendario(
            @Param("idEspacio") Long idEspacio,
            @Param("estados") List<String> estados,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    int countReservasByCoordinadorAndEstado(Usuarios coordinador, EstadoReserva estado);
    int countReservasByEstado_IdEstadoReserva_AndFecha(Integer estadoIdEstadoReserva, LocalDate fecha);
    /**
     * Busca reservas por coordinador y estado, ordenadas por fecha y hora
     */
    List<Reserva> findByCoordinadorAndEstado_EstadoOrderByFechaAscHoraInicioAsc(
            Usuarios coordinador, String estado);
    /**
     * Cuenta reservas confirmadas por coordinador en un rango de fechas
     */
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.estado.estado = :estado " +
            "AND r.momentoReserva BETWEEN :inicio AND :fin")
    long countByCoordinadorAndEstado_EstadoAndMomentoReservaBetween(
            @Param("coordinador") Usuarios coordinador,
            @Param("estado") String estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
    /**
     * Busca reservas por coordinador que tienen comprobante subido y están pendientes
     */
    @Query("SELECT r FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.estado.estado = 'No confirmada' " +
            "AND r.capturaKey IS NOT NULL " +
            "ORDER BY r.fecha ASC, r.horaInicio ASC")
    List<Reserva> findReservasPendientesConComprobante(@Param("coordinador") Usuarios coordinador);
    /**
     * Busca reservas urgentes (para hoy) por coordinador
     */
    @Query("SELECT r FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.estado.estado = 'No confirmada' " +
            "AND r.fecha = :fecha " +
            "ORDER BY r.horaInicio ASC")
    List<Reserva> findReservasUrgentesParaHoy(
            @Param("coordinador") Usuarios coordinador,
            @Param("fecha") LocalDate fecha);
    /**
     * Calcula el monto total pendiente de confirmación por coordinador
     */
    @Query("SELECT COALESCE(SUM(r.costo), 0) FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.estado.estado = 'No confirmada'")
    Double sumMontoPendientePorCoordinador(@Param("coordinador") Usuarios coordinador);
    /**
     * Busca reservas por coordinador con múltiples estados
     */
    @Query("SELECT r FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.estado.estado IN :estados " +
            "ORDER BY r.fecha ASC, r.horaInicio ASC")
    List<Reserva> findByCoordinadorAndEstadoIn(
            @Param("coordinador") Usuarios coordinador,
            @Param("estados") List<String> estados);
    /**
     * Cuenta reservas por coordinador y estado específico
     */
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.estado.estado = :estado")
    long countByCoordinadorAndEstado_Estado(
            @Param("coordinador") Usuarios coordinador,
            @Param("estado") String estado);
    /**
     * Busca reservas por coordinador en un rango de fechas
     */
    @Query("SELECT r FROM Reserva r WHERE r.coordinador = :coordinador " +
            "AND r.fecha BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY r.fecha ASC, r.horaInicio ASC")
    List<Reserva> findByCoordinadorAndFechaBetween(
            @Param("coordinador") Usuarios coordinador,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    /**
     * Buscar reservas por coordinador y lista de estados
     */
    List<Reserva> findByCoordinadorAndEstado_EstadoIn(Usuarios coordinador, List<String> estados);

    /**
     * Contar reservas por coordinador y lista de estados
     */
    long countByCoordinadorAndEstado_EstadoIn(Usuarios coordinador, List<String> estados);
}