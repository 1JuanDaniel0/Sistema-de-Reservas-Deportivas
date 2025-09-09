package com.example.project.repository.admin;

import com.example.project.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;


import java.util.List;

@Repository
public interface ReservaRepositoryAdmin extends JpaRepository<Reserva, Integer> {
    @Query(value = """
    WITH 
    ReservasPorTipo AS (
        SELECT 
            te.nombre AS tipo,
            COUNT(r.idReserva) AS totalReservas
        FROM espacio e
        JOIN tipoespacio te ON e.idTipoEspacio = te.idTipoEspacio
        LEFT JOIN reserva r ON e.idEspacio = r.espacio
        WHERE e.idEstadoEspacio = 1
        GROUP BY te.nombre
    ),
    EspaciosActivosPorTipo AS (
        SELECT 
            te.nombre AS tipo,
            COUNT(*) AS totalEspacios
        FROM espacio e
        JOIN tipoespacio te ON e.idTipoEspacio = te.idTipoEspacio
        WHERE e.idEstadoEspacio = 1
        GROUP BY te.nombre
    )
    SELECT 
        e.tipo AS tipo,
        COALESCE(r.totalReservas, 0) AS totalReservas,
        e.totalEspacios AS totalEspacios,
        ROUND((COALESCE(r.totalReservas, 0) * 100.0) / e.totalEspacios, 2) AS porcentajeUso
    FROM EspaciosActivosPorTipo e
    LEFT JOIN ReservasPorTipo r ON e.tipo = r.tipo
    ORDER BY porcentajeUso DESC
""", nativeQuery = true)
    List<ReservaDto> obtenerResumenReservas();

    @Query(value = "select * from reserva where vecino=?1 and fecha>curdate(); ", nativeQuery = true)
    List<Reserva> findByIDUsuario(int idUsuario);


}
