package com.example.project.repository;

import com.example.project.entity.Reserva;
import com.example.project.entity.SolicitudCancelacion;
import com.example.project.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudCancelacionRepository extends JpaRepository<SolicitudCancelacion, Integer> {
    List<SolicitudCancelacion> findByReserva_Vecino_IdUsuarios(int idUsuario);
    boolean existsByReserva_IdReserva(int idReserva);
    List<SolicitudCancelacion> findByReserva_Coordinador(Usuarios coordinador);
    List<SolicitudCancelacion> findByReserva_CoordinadorAndEstado(Usuarios coordinador, String estado);
    Optional<SolicitudCancelacion> findByReserva(Reserva reserva);
    @Query("SELECT COUNT(s) FROM SolicitudCancelacion s " +
            "WHERE s.reserva.coordinador = :coordinador " +
            "AND s.estado IN ('Reembolsada', 'Rechazada') " +
            "AND FUNCTION('MONTH', s.fechaSolicitud) = FUNCTION('MONTH', CURRENT_DATE) " +
            "AND FUNCTION('YEAR', s.fechaSolicitud) = FUNCTION('YEAR', CURRENT_DATE)")
    int countReembolsosProcesadosEsteMes(@Param("coordinador") Usuarios coordinador);

    /**
     * Contar solicitudes por coordinador y estado
     */
    Long countByReserva_CoordinadorAndEstado(Usuarios coordinador, String estado);

}
