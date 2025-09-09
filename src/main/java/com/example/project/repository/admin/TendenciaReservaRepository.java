package com.example.project.repository.admin;
import com.example.project.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TendenciaReservaRepository extends JpaRepository<Reserva, Integer> {

    @Query(value = """
        SELECT\s
            anio,
            mes,
            dia,
            tipoEspacio,
            totalReservas
        FROM (
            SELECT\s
                YEAR(r.fecha) as anio,
                MONTH(r.fecha) as mes,
                DAY(r.fecha) as dia,
                te.nombre as tipoEspacio,
                COUNT(*) as totalReservas
            FROM reserva r
            JOIN espacio e ON r.espacio = e.idEspacio
            JOIN tipoespacio te ON e.idTipoEspacio = te.idTipoEspacio
            JOIN estadoreserva est ON r.estado = est.idEstadoReserva
            WHERE est.estado IN ('Finalizada', 'Confirmada')
            GROUP BY anio, mes, dia, tipoEspacio
            ORDER BY anio DESC, mes DESC, dia DESC, tipoEspacio DESC -- Ordena descendente para obtener los 'últimos'
            LIMIT 20 -- Limita a los 3 registros más recientes según este orden
        ) AS sub
        ORDER BY anio ASC, mes ASC, dia ASC, tipoEspacio ASC;
    """, nativeQuery = true)
    List<Object[]> obtenerTendenciaReservasRaw();
}
