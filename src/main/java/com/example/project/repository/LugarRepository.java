package com.example.project.repository;

import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.project.entity.Lugar;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LugarRepository extends JpaRepository<Lugar, Integer> {
    Optional<Lugar> findByLugarIgnoreCase(String lugar);
    @Query("SELECT COUNT(l) FROM Lugar l JOIN l.coordinadores c WHERE c = :coordinador")
    int countLugaresByCoordinador(@Param("coordinador") Usuarios coordinador);
    List<Lugar> findByCoordinadores_IdUsuarios(Integer idUsuario);

    // metodo para depuración
    @Query("SELECT l FROM Lugar l JOIN l.coordinadores c WHERE c.idUsuarios = :idUsuario")
    List<Lugar> debugFindByCoordinadorId(@Param("idUsuario") Integer idUsuario);
}
