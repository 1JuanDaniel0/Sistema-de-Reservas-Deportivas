package com.example.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago", nullable = false)
    private Integer idPago;

    @OneToOne
    @JoinColumn(name = "idReserva")
    private Reserva reserva; // Llave de la tabla Reserva

    private BigDecimal monto;

    private LocalDateTime fechaPago;

    private String tipoPago; // 'En banco', 'En línea'

    private String estado; // Pagado, Anulado

    private String referencia; // (opcional)
}
