package com.example.project.repository;
import com.example.project.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EspacioRepositoryGeneral extends JpaRepository<Espacio, Integer>{

    List<Espacio> findTop6ByOrderByIdEspacioAsc();
    @Query("""
    SELECT e FROM Espacio e
    WHERE e.idEspacio NOT IN (
        SELECT r.espacio.idEspacio FROM Reserva r
        WHERE r.fecha = :fecha
        AND (
            (:horaInicio BETWEEN r.horaInicio AND r.horaFin OR
             :horaFin BETWEEN r.horaInicio AND r.horaFin OR
             r.horaInicio BETWEEN :horaInicio AND :horaFin)
        )
    )
""")
    List<Espacio> findEspaciosDisponibles(@Param("fecha") LocalDate fecha,
                                          @Param("horaInicio") LocalTime horaInicio,
                                          @Param("horaFin") LocalTime horaFin);

    @Query("""
    SELECT DISTINCT e FROM Espacio e
    LEFT JOIN FETCH e.deportes
    JOIN FETCH e.tipoEspacio
    WHERE e.idEstadoEspacio.idEstadoEspacio = :estado
""")
    List<Espacio> findAllByEstadoWithDeportesAndTipoEspacio(@Param("estado") int estado);

    @Query("SELECT MAX(e.costo) FROM Espacio e")
    Double findMaxPrecio();

    @Query(value = """
    SELECT e.* FROM espacio e
    LEFT JOIN calificacion c ON c.idEspacio = e.idEspacio
    WHERE e.idEstadoEspacio = 1
    GROUP BY e.idEspacio
    ORDER BY AVG(COALESCE(c.puntaje, 0)) DESC
    LIMIT 6
""", nativeQuery = true)
    List<Espacio> findTop6ByMejorCalificacion();
}
