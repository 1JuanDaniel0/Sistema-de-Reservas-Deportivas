package com.example.project.repository.admin;

import com.example.project.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiciosDeportivosDisponiblesRepository extends JpaRepository<Espacio, Integer> {

    @Query(value = """
    SELECT 
        r.idReserva AS id,
        e.nombre as nombre,
        te.nombre AS tipo,  -- cambiado
        l.nombre AS lugar,
        r.costo AS costo,
        ee.estado AS estadoEspacio
    FROM 
        reserva r
    JOIN espacio e ON r.espacio = e.idEspacio
    JOIN tipoespacio te ON e.idTipoEspacio = te.idTipoEspacio
    JOIN lugar l ON e.idLugar = l.idLugar
    JOIN estadoespacio ee ON e.idEstadoEspacio = ee.idEstadoEspacio
    WHERE ee.estado = 'Activo'
""", nativeQuery = true)
    List<ServiciosDeportivosDisponiblesDto> listarServiciosActivos();
}
