package com.example.project.repository.coordinador;

import com.example.project.entity.Actividad;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Integer> {
    // Encontrar todas las actividades del usuario
    List<Actividad> findByUsuarioOrderByFechaDesc(Usuarios usuario);

    // Metodo para obtener todas las actividades por nombre de rpl
    List<Actividad> findByUsuario_Rol_RolOrderByFechaDesc(String rol);
}