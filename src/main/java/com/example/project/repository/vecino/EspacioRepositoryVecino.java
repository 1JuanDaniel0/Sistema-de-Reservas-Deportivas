package com.example.project.repository.vecino;

import com.example.project.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspacioRepositoryVecino extends JpaRepository<Espacio, Integer> {

    @Query("SELECT DISTINCT e FROM Espacio e " +
            "JOIN FETCH e.deportes d " +
            "JOIN FETCH e.tipoEspacio te " +
            "WHERE e.idEstadoEspacio.idEstadoEspacio = :idEstadoEspacio")
    List<Espacio> findAllByEstadoWithDeportesAndTipoEspacio(@Param("idEstadoEspacio") int idEstadoEspacio);

}
