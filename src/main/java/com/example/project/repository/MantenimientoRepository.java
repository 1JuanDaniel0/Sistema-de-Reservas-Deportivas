package com.example.project.repository;

import com.example.project.entity.Mantenimiento;
import com.example.project.entity.Espacio;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Integer> {

    /**
     * Buscar mantenimientos activos (PROGRAMADO o EN_PROCESO) que afecten un espacio en una fecha/hora específica
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.espacio = :espacio " +
            "AND m.estado IN ('PROGRAMADO', 'EN_PROCESO') " +
            "AND :fecha BETWEEN m.fechaInicio AND m.fechaFin " +
            "AND (" +
            "(:hora BETWEEN m.horaInicio AND m.horaFin) " +
            "OR (m.horaInicio <= :hora AND m.horaFin > :hora)" +
            ")")
    List<Mantenimiento> findMantenimientosActivosEnFechaHora(
            @Param("espacio") Espacio espacio,
            @Param("fecha") LocalDate fecha,
            @Param("hora") LocalTime hora);

    /**
     * Verificar conflictos de mantenimiento en un rango de fechas y horas - VERSIÓN MEJORADA
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.espacio = :espacio AND " +
            "m.estado IN ('PROGRAMADO', 'EN_PROCESO') AND " +
            // Verificar solapamiento de fechas
            "NOT (m.fechaFin < :fechaInicio OR m.fechaInicio > :fechaFin) AND " +
            // Verificar solapamiento de horas (solo si las fechas se solapan)
            "NOT (m.horaFin <= :horaInicio OR m.horaInicio >= :horaFin)")
    List<Mantenimiento> findConflictosMantenimiento(
            @Param("espacio") Espacio espacio,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );

    /**
     * Buscar mantenimientos por espacio en un rango de fechas
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.espacio = :espacio " +
            "AND m.fechaInicio <= :fechaFin AND m.fechaFin >= :fechaInicio " +
            "ORDER BY m.fechaInicio ASC, m.horaInicio ASC")
    List<Mantenimiento> findByEspacioAndFechaBetween(
            @Param("espacio") Espacio espacio,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    /**
     * Buscar mantenimientos por responsable
     */
    List<Mantenimiento> findByResponsableOrderByFechaInicioAscHoraInicioAsc(Usuarios responsable);

    /**
     * Buscar mantenimientos por estado
     */
    List<Mantenimiento> findByEstadoOrderByFechaInicioAscHoraInicioAsc(Mantenimiento.EstadoMantenimiento estado);

    /**
     * Buscar mantenimientos programados para hoy
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.fechaInicio = :fecha " +
            "AND m.estado = 'PROGRAMADO' " +
            "ORDER BY m.horaInicio ASC")
    List<Mantenimiento> findMantenimientosParaHoy(@Param("fecha") LocalDate fecha);

    /**
     * Contar mantenimientos activos por espacio
     */
    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.espacio = :espacio " +
            "AND m.estado IN ('PROGRAMADO', 'EN_PROCESO')")
    long countMantenimientosActivosPorEspacio(@Param("espacio") Espacio espacio);

    /**
     * Buscar mantenimientos urgentes (alta prioridad y próximos)
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.prioridad = 'URGENTE' " +
            "AND m.estado = 'PROGRAMADO' " +
            "AND m.fechaInicio BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY m.fechaInicio ASC, m.horaInicio ASC")
    List<Mantenimiento> findMantenimientosUrgentes(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    /**
     * Verificar si existe solapamiento con otros mantenimientos del mismo espacio
     */
    @Query("SELECT COUNT(m) > 0 FROM Mantenimiento m WHERE m.espacio = :espacio " +
            "AND m.idMantenimiento != :mantenimientoId " +
            "AND m.estado IN ('PROGRAMADO', 'EN_PROCESO') " +
            "AND NOT (m.fechaFin < :fechaInicio OR m.fechaInicio > :fechaFin) " +
            "AND NOT (m.horaFin <= :horaInicio OR m.horaInicio >= :horaFin)")
    boolean existsSolapamientoMantenimiento(
            @Param("espacio") Espacio espacio,
            @Param("mantenimientoId") Integer mantenimientoId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    // Mantenimientos que deberían iniciarse ahora
    @Query("SELECT m FROM Mantenimiento m WHERE m.estado = 'PROGRAMADO' " +
            "AND m.fechaInicio = :fecha " +
            "AND m.horaInicio <= :hora " +
            "AND m.horaFin > :hora")
    List<Mantenimiento> findMantenimientosParaIniciar(
            @Param("fecha") LocalDate fecha,
            @Param("hora") LocalTime hora);

    // Mantenimientos que deberían finalizarse ahora
    @Query("SELECT m FROM Mantenimiento m WHERE m.estado = 'EN_PROCESO' " +
            "AND (m.fechaFin < :fecha OR " +
            "(m.fechaFin = :fecha AND m.horaFin <= :hora))")
    List<Mantenimiento> findMantenimientosParaFinalizar(
            @Param("fecha") LocalDate fecha,
            @Param("hora") LocalTime hora);

    /**
     * Contar mantenimientos por responsable y estado específico
     */
    long countByResponsableAndEstado(Usuarios responsable, Mantenimiento.EstadoMantenimiento estado);

    /**
     * Contar mantenimientos por responsable y lista de estados
     */
    long countByResponsableAndEstadoIn(Usuarios responsable, List<Mantenimiento.EstadoMantenimiento> estados);

    /**
     * Buscar mantenimientos por responsable y lista de estados
     */
    List<Mantenimiento> findByResponsableAndEstadoIn(Usuarios responsable, List<Mantenimiento.EstadoMantenimiento> estados);

    /**
     * Buscar mantenimientos por responsable, estado y rango de fechas
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.responsable = :responsable AND " +
            "m.estado IN :estados AND " +
            "m.fechaInicio BETWEEN :fechaInicio AND :fechaFin")
    List<Mantenimiento> findByResponsableAndEstadoInAndFechaBetween(
            @Param("responsable") Usuarios responsable,
            @Param("estados") List<Mantenimiento.EstadoMantenimiento> estados,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    /**
     * Buscar mantenimientos activos por lugar (a través del espacio)
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.responsable = :responsable AND " +
            "m.estado IN :estados AND " +
            "m.espacio.idLugar.idLugar = :lugarId")
    List<Mantenimiento> findByResponsableAndEstadoInAndEspacioLugar(
            @Param("responsable") Usuarios responsable,
            @Param("estados") List<Mantenimiento.EstadoMantenimiento> estados,
            @Param("lugarId") Integer lugarId
    );


    /**
     * Método específico para verificar conflictos en una fecha específica (más eficiente para reservas)
     */
    @Query("SELECT m FROM Mantenimiento m WHERE m.espacio = :espacio AND " +
            "m.estado IN ('PROGRAMADO', 'EN_PROCESO') AND " +
            ":fecha BETWEEN m.fechaInicio AND m.fechaFin AND " +
            "NOT (m.horaFin <= :horaInicio OR m.horaInicio >= :horaFin)")
    List<Mantenimiento> findConflictosMantenimientoEnFecha(
            @Param("espacio") Espacio espacio,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );
}