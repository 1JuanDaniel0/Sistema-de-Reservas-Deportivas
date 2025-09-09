package com.example.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "reembolso")
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reembolso", nullable = false)
    private Integer idReembolso;

    @OneToOne(optional = false)
    @JoinColumn(name = "solicitud_cancelacion_id")
    private SolicitudCancelacion solicitudCancelacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(name = "monto_reembolso", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoReembolso;

    @Column(name = "tipo_pago_original", length = 20, nullable = false)
    private String tipoPagoOriginal; // "En línea", "En banco"

    @Column(name = "estado_reembolso", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReembolso estadoReembolso;

    @Column(name = "metodo_reembolso", length = 30)
    @Enumerated(EnumType.STRING)
    private MetodoReembolso metodoReembolso;

    // Datos del coordinador que aprobó
    @ManyToOne(optional = false)
    @JoinColumn(name = "aprobado_por_coordinador")
    private Usuarios aprobadoPorCoordinador;

    @Column(name = "fecha_aprobacion", nullable = false)
    private LocalDateTime fechaAprobacion;

    @Column(name = "motivo_aprobacion", columnDefinition = "TEXT")
    private String motivoAprobacion;

    // Datos del administrador (solo para reembolsos manuales)
    @ManyToOne
    @JoinColumn(name = "procesado_por_admin")
    private Usuarios procesadoPorAdmin;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "observaciones_admin", columnDefinition = "TEXT")
    private String observacionesAdmin;

    // Para reembolsos automáticos de MercadoPago
    @Column(name = "id_transaccion_reembolso")
    private String idTransaccionReembolso;

    @Column(name = "respuesta_mercadopago", columnDefinition = "TEXT")
    private String respuestaMercadoPago;

    // Para reembolsos manuales
    @Column(name = "numero_operacion")
    private String numeroOperacion; // Número de depósito/transferencia

    @Column(name = "entidad_bancaria")
    private String entidadBancaria;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Métodos de utilidad
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public void marcarComoAutomatico(String idTransaccion, String respuestaMercadoPago) {
        this.estadoReembolso = EstadoReembolso.COMPLETADO;
        this.metodoReembolso = MetodoReembolso.MERCADOPAGO_AUTOMATICO;
        this.idTransaccionReembolso = idTransaccion;
        this.respuestaMercadoPago = respuestaMercadoPago;
        this.fechaProcesamiento = LocalDateTime.now();
    }

    public void marcarComoManual(Usuarios admin, String numeroOperacion,
                                 String entidadBancaria, String observaciones) {
        this.estadoReembolso = EstadoReembolso.COMPLETADO;
        this.metodoReembolso = MetodoReembolso.DEPOSITO_MANUAL;
        this.procesadoPorAdmin = admin;
        this.numeroOperacion = numeroOperacion;
        this.entidadBancaria = entidadBancaria;
        this.observacionesAdmin = observaciones;
        this.fechaProcesamiento = LocalDateTime.now();
    }

    public void marcarComoFallido(String motivoFallo) {
        this.estadoReembolso = EstadoReembolso.FALLIDO;
        this.observacionesAdmin = motivoFallo;
        this.fechaProcesamiento = LocalDateTime.now();
    }

    // Verificar si requiere intervención manual
    public boolean requiereIntervencionManual() {
        return "En banco".equalsIgnoreCase(this.tipoPagoOriginal) &&
                this.estadoReembolso == EstadoReembolso.PENDIENTE;
    }

    // Enums
    @Getter
    public enum EstadoReembolso {
        PENDIENTE("Pendiente de procesamiento"),
        COMPLETADO("Reembolso completado"),
        FALLIDO("Falló el procesamiento");

        private final String descripcion;

        EstadoReembolso(String descripcion) {
            this.descripcion = descripcion;
        }

    }

    @Getter
    public enum MetodoReembolso {
        MERCADOPAGO_AUTOMATICO("Reembolso automático MercadoPago"),
        DEPOSITO_MANUAL("Depósito manual por administrador"),
        TRANSFERENCIA_MANUAL("Transferencia manual por administrador");

        private final String descripcion;

        MetodoReembolso(String descripcion) {
            this.descripcion = descripcion;
        }

    }
}