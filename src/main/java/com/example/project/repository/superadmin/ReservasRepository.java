package com.example.project.repository.superadmin;


import com.example.project.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservasRepository extends JpaRepository<Reserva, Integer> {
    @Query(value="select sum(costo) as total from reserva where estado=1;",nativeQuery=true)
    int ingresoTotal();
    @Query(value="select ifnull(sum(costo),0) as total from reserva where estado=1 and extract(MONTH FROM date_sub(curdate(), interval ?1 month))=extract(MONTH FROM fecha) and extract(YEAR FROM date_sub(curdate(), interval ?1 month))=extract(YEAR FROM fecha);",nativeQuery=true)
    int ingresoMensual(int mes_intervalo);
    @Query(value="select month(date_sub(curdate(),interval ?1 month)) as intofmonth;",nativeQuery=true)
    int intofmonth(int mes_intervalo);
    @Query(value="select sum(costo) as total from reserva where estado=1 and tipoPago='En línea';",nativeQuery=true)
    int ingresoTotalEnLinea();
    @Query(value="select sum(costo) as total from reserva where estado=1 and tipoPago='En banco';",nativeQuery=true)
    int ingresoTotalEnBanco();
    @Query(value="select count(*) from reserva where tipoPago='En banco' and estado=1 and fecha=date_sub(curdate(), interval ?1 day );",nativeQuery=true)
    int ingresoDiarioEnBanco(int dia_intervalo);
    @Query(value="select count(*) from reserva where tipoPago='En línea' and estado=1 and fecha=date_sub(curdate(), interval ?1 day );",nativeQuery=true)
    int ingresoDiarioEnLinea(int dia_intervalo);
    @Query(value = "select dayofweek(date_sub(curdate(),interval ?1 day)) as intofweek;",nativeQuery = true)
    int intofday(int dia_intervalo);

    int countReservasByTipoPagoIgnoreCase(String tipoPago);
    List<Reserva> findByFechaAfterOrderByIdReservaDesc(LocalDate fecha);
}
