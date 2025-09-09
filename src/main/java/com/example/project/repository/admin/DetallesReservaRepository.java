package com.example.project.repository.admin;

import com.example.project.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DetallesReservaRepository extends JpaRepository<Reserva, Long> {

    @Query(value = """
    SELECT 
        r.idReserva AS id,
        r.fecha AS fecha,
        r.horaInicio AS horaInicio,
        r.horaFin AS horaFin,
        te.nombre AS tipoEspacio,
        e.nombre AS nombreEspacio,
        e.foto1_url AS fotoEspacio,
        CONCAT(u1.nombres, ' ', u1.apellidos) AS coordinadorNombre,
        CONCAT(u2.nombres, ' ', u2.apellidos) AS vecinoNombre,
        er.estado AS estado,
        r.tipoPago AS tipoPago,
        r.costo AS costo
    FROM reserva r
    INNER JOIN espacio e ON r.espacio = e.idEspacio
    INNER JOIN tipoespacio te ON e.idTipoEspacio = te.idTipoEspacio
    INNER JOIN usuarios u1 ON r.coordinador = u1.idUsuarios
    INNER JOIN usuarios u2 ON r.vecino = u2.idUsuarios
    INNER JOIN estadoreserva er ON r.estado = er.idEstadoReserva
""", nativeQuery = true)
    List<DetallesReservaDto> obtenerDetallesReservas();
}
