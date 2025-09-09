package com.example.project.repository;
import com.example.project.entity.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.project.entity.Usuarios;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuariosRepository extends JpaRepository<Usuarios, Integer> {
    Optional<Usuarios> findByCorreo(String correo);
    Optional<Usuarios> findByDni(String dni);
    long countByRol_Rol(String rol);

    @Query("SELECT u FROM Usuarios u WHERE LOWER(u.nombres) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :term, '%'))" +
            "AND u.rol.rol NOT IN ('SuperAdmin', 'Administrador')")
    Page<Usuarios> findByNombreOrCorreoContainingIgnoreCase(@Param("term") String term, Pageable pageable);

    @Override
    @Query("SELECT u FROM Usuarios u WHERE u.rol.rol NOT IN ('SuperAdmin', 'Administrador')")
    Page<Usuarios> findAll(Pageable pageable);

    // Para el datatable (paginado)
    @Query("SELECT u FROM Usuarios u WHERE u.rol.rol = 'Coordinador'")
    Page<Usuarios> findByRol(@Param("rol") String rol, Pageable pageable);

    @Query("SELECT u FROM Usuarios u WHERE u.rol.rol = :rol") // Y :rol aquí
    List<Usuarios> findByRolcito(@Param("rol") String rol);

    @Query("""
        SELECT u FROM Usuarios u
        WHERE u.rol.rol = 'Coordinador'
          AND (
              LOWER(u.nombres) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%'))
            OR CAST(u.dni AS string) LIKE CONCAT('%', :search, '%')
           OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.estado.estado) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Usuarios> buscarCoordinadoresConTodo(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM Usuarios u WHERE u.rol.rol = 'Administrador'")
    Optional<Usuarios> findFirstAdministrador();

    boolean existsByDni(String dni);
    boolean existsByCorreo(String correo);
    Optional<Usuarios> findByTelefono(String telefono);
    boolean existsByTelefono(String telefono);
    Optional<Usuarios> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);

    /**
     * Buscar usuarios por rol (para coordinadores)
     */
    @Query("SELECT u FROM Usuarios u WHERE u.rol.rol = :rolNombre AND u.estado.estado = 'Activo'")
    List<Usuarios> findByRolNombreAndEstadoActivo(@Param("rolNombre") String rolNombre);

    /**
     * Buscar coordinadores activos
     */
    default List<Usuarios> findCoordinadoresActivos() {
        return findByRolNombreAndEstadoActivo("Coordinador");
    }

    Optional<Usuarios> findById(Integer id);

    List<Usuarios> findByRol_Rol(String rolRol);

    /**
     * Buscar coordinadores activos por lugar asignado
     */
    List<Usuarios> findByRol_RolAndEstado_EstadoAndLugaresAsignadosContaining(
            String rolNombre, String estado, Lugar lugar);

}
