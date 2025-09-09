package com.example.project.repository.superadmin;

import com.example.project.entity.Rol;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<Usuarios, Integer> {
    @Query(value="select count(*) from espacio where idEstadoEspacio= ?1",nativeQuery=true)
    int cantidadDeEspaciosPorIdEstado(int idEstadoEspacio);
    @Transactional
    @Modifying
    @Query(value="update usuarios set estado=?2 where idUsuarios=?1;",nativeQuery = true)
    void actualizarEstadoUsuario(int idUsuario,int estado);
    List<Usuarios> findByRolNot(Rol rol);
}
