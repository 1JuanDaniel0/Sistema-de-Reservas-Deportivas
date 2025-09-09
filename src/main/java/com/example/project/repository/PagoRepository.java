package com.example.project.repository;

import com.example.project.entity.Pago;
import com.example.project.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.tipoPago = :tipo AND p.estado = :estado")
    BigDecimal sumaPagosPorTipoYEstado(@Param("tipo") String tipo, @Param("estado") String estado);

    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.estado = :estado")
    BigDecimal sumaPagosPorEstado(@Param("estado") String estado);

    @Query("SELECT SUM(p.monto) FROM Pago p WHERE FUNCTION('YEAR', p.fechaPago) = :anio AND FUNCTION('MONTH', p.fechaPago) = :mes AND p.estado = :estado")
    BigDecimal sumaPagosPorMes(@Param("anio") int anio, @Param("mes") int mes, @Param("estado") String estado);

    @Query("SELECT SUM(p.monto) FROM Pago p WHERE FUNCTION('DATE', p.fechaPago) = :fecha AND p.tipoPago = :tipo AND p.estado = :estado")
    BigDecimal sumaPagosPorDiaYTipo(@Param("fecha") LocalDate fecha, @Param("tipo") String tipo, @Param("estado") String estado);

    Optional<Pago> findByReservaAndEstadoIgnoreCase(Reserva reserva, String estado);

    Optional<Pago> findByReservaAndEstadoIn(Reserva reserva, List<String> estados);

    @Query("SELECT p FROM Pago p WHERE p.reserva.idReserva = :reservaId")
    Pago findByReserva(@Param("reservaId") Integer reservaId);
    // O si usas el objeto Reserva directamente:
    Pago findByReserva(Reserva reserva);
    Pago findByReserva_IdReserva(Integer reservaId);
    Optional<Pago> findOptionalByReserva(Reserva reserva);

    /**
     * Buscar todos los pagos de un vecino ordenados por fecha descendente
     */
    List<Pago> findByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(Integer idVecino);

    /**
     * Buscar pagos de un vecino por tipo de pago
     */
    List<Pago> findByReserva_Vecino_IdUsuariosAndTipoPagoOrderByFechaPagoDesc(
            Integer idVecino, String tipoPago);

    /**
     * Buscar pagos de un vecino por estado
     */
    List<Pago> findByReserva_Vecino_IdUsuariosAndEstadoOrderByFechaPagoDesc(
            Integer idVecino, String estado);

    /**
     * Buscar pagos de un vecino en un rango de fechas
     */
    @Query("SELECT p FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino " +
            "AND p.fechaPago BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY p.fechaPago DESC")
    List<Pago> findByVecinoAndFechaBetween(
            @Param("idVecino") Integer idVecino,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Contar total de pagos de un vecino
     */
    long countByReserva_Vecino_IdUsuarios(Integer idVecino);

    /**
     * Calcular monto total pagado por un vecino
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino")
    Double sumMontoTotalByVecino(@Param("idVecino") Integer idVecino);

    /**
     * Calcular monto total pagado por un vecino en un rango de fechas
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino " +
            "AND p.fechaPago BETWEEN :fechaInicio AND :fechaFin")
    Double sumMontoByVecinoAndFechaBetween(
            @Param("idVecino") Integer idVecino,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Contar pagos por tipo de pago de un vecino
     */
    long countByReserva_Vecino_IdUsuariosAndTipoPago(Integer idVecino, String tipoPago);

    /**
     * Contar pagos por estado de un vecino
     */
    long countByReserva_Vecino_IdUsuariosAndEstado(Integer idVecino, String estado);

    /**
     * Buscar último pago de un vecino
     */
    Optional<Pago> findFirstByReserva_Vecino_IdUsuariosOrderByFechaPagoDesc(Integer idVecino);

    /**
     * Buscar pagos de un vecino por espacio específico
     */
    @Query("SELECT p FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino " +
            "AND p.reserva.espacio.idEspacio = :idEspacio " +
            "ORDER BY p.fechaPago DESC")
    List<Pago> findByVecinoAndEspacio(
            @Param("idVecino") Integer idVecino,
            @Param("idEspacio") Integer idEspacio);

    /**
     * Obtener estadísticas de pagos por mes para un vecino
     */
    @Query("SELECT MONTH(p.fechaPago) as mes, YEAR(p.fechaPago) as anio, COUNT(p) as cantidad, SUM(p.monto) as total " +
            "FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino " +
            "AND p.fechaPago >= :fechaDesde " +
            "GROUP BY YEAR(p.fechaPago), MONTH(p.fechaPago) " +
            "ORDER BY YEAR(p.fechaPago) DESC, MONTH(p.fechaPago) DESC")
    List<Object[]> findEstadisticasPorMes(
            @Param("idVecino") Integer idVecino,
            @Param("fechaDesde") LocalDateTime fechaDesde);

    /**
     * Buscar espacios más utilizados por un vecino
     */
    @Query("SELECT p.reserva.espacio.nombre, COUNT(p) as cantidad " +
            "FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino " +
            "GROUP BY p.reserva.espacio.idEspacio, p.reserva.espacio.nombre " +
            "ORDER BY COUNT(p) DESC")
    List<Object[]> findEspaciosMasUtilizados(@Param("idVecino") Integer idVecino);

    /**
     * Verificar si un vecino tiene pagos pendientes
     */
    boolean existsByReserva_Vecino_IdUsuariosAndEstado(Integer idVecino, String estado);

    /**
     * Buscar pagos con filtros múltiples
     */
    @Query("SELECT p FROM Pago p WHERE p.reserva.vecino.idUsuarios = :idVecino " +
            "AND (:tipoPago IS NULL OR p.tipoPago = :tipoPago) " +
            "AND (:estado IS NULL OR p.estado = :estado) " +
            "AND (:idEspacio IS NULL OR p.reserva.espacio.idEspacio = :idEspacio) " +
            "AND (:fechaInicio IS NULL OR p.fechaPago >= :fechaInicio) " +
            "AND (:fechaFin IS NULL OR p.fechaPago <= :fechaFin) " +
            "ORDER BY p.fechaPago DESC")
    List<Pago> findByVecinoWithFilters(
            @Param("idVecino") Integer idVecino,
            @Param("tipoPago") String tipoPago,
            @Param("estado") String estado,
            @Param("idEspacio") Integer idEspacio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);
}
