package com.example.project.repository.admin;

import com.example.project.entity.Deporte;
import com.example.project.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;


import jakarta.transaction.Transactional;  // o javax.transaction.Transactional según tu versión
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspacioRepositoryAdmin extends JpaRepository<Espacio, Integer> {

    @Query(value ="""
    SELECT
        e.idEspacio AS idEspacio,
        e.nombre AS nombre,
        l.nombre AS nombreLugar,
        es.estado AS estadoEspacio,
        te.nombre AS tipo,  -- usado para EspacioDto.getTipo()
        e.costo AS costo,
        te.nombre AS nombreTipo
    FROM
        espacio e
    LEFT JOIN lugar l ON e.idLugar = l.idLugar
    LEFT JOIN estadoespacio es ON e.idEstadoEspacio = es.idEstadoEspacio
    LEFT JOIN tipoespacio te ON e.idTipoEspacio = te.idTipoEspacio
""", nativeQuery = true)
    List<EspacioDto> findAllEspacioDtos();

    @Query("""
    SELECT e FROM Espacio e
    WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(e.tipoEspacio.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(e.idLugar.lugar) LIKE LOWER(CONCAT('%', :search, '%'))
       OR STR(e.costo) LIKE CONCAT('%', :search, '%')
       OR LOWER(e.idEstadoEspacio.estado) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<Espacio> buscarEspaciosConTodo(@Param("search") String search, Pageable pageable);


    @Modifying
    @Transactional
    @Query(value = """
    UPDATE espacio 
    SET idEstadoEspacio = (
        SELECT idEstadoEspacio 
        FROM estadoespacio 
        WHERE estado = :nuevoEstado
    )
    WHERE idEspacio = :idEspacio
""", nativeQuery = true)
    void actualizarEstado(@Param("idEspacio") Integer idEspacio, @Param("nuevoEstado") String nuevoEstado);

    @Query("SELECT DISTINCT d FROM Espacio e JOIN e.deportes d WHERE e.idEspacio = :idEspacio")
    List<Deporte> findDeportesByEspacioId(@Param("idEspacio") Integer idEspacio);

    Page<Espacio> findByNombreContainingIgnoreCaseOrTipoEspacio_NombreContainingIgnoreCaseOrIdLugar_LugarContainingIgnoreCase(
            String nombre, String tipo, String lugar, Pageable pageable);

}
