package com.example.project.repository.admin;

import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ListaCoordinadoresRepository extends JpaRepository<Usuarios ,Integer> {


    @Query("""
    SELECT u.idUsuarios AS idUsuarios, u.nombres AS nombres, u.apellidos AS apellidos,
           u.correo AS correo, u.dni AS dni, u.contrasena AS contrasena, e.estado AS estado
    FROM Usuarios u
    JOIN u.estado e
    WHERE u.rol.idRol = 2
    """)
    List<ListaCoordinadoresDto> listarCoordinadores();

}
