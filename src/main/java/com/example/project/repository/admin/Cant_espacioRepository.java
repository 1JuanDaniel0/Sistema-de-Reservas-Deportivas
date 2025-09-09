package com.example.project.repository.admin;

import java.util.List;

import com.example.project.entity.Espacio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface Cant_espacioRepository extends CrudRepository<Espacio, Integer> {

    // Contar espacios por idTipoEspacio (1: Grass Sintético, 2: Loza, 3: Piscina, 4: Pista de Atletismo)
    @Query(value = """
    SELECT e.idTipoEspacio, COUNT(*) 
    FROM espacio e 
    WHERE e.idEstadoEspacio = 1
    GROUP BY e.idTipoEspacio
    """, nativeQuery = true)
    List<Object[]> contarPorIdTipoEspacio();

}
