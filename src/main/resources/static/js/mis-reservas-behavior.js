// mis-reservas-behavior-refactored.js - VERSIÓN MEJORADA

document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.querySelector('.filter-form');
    const filterSelect = document.getElementById('filtroEstado');
    const filterTabs = document.querySelectorAll('.filter-tab');

    const filterTexts = {
        '1': 'Reservas Confirmadas',
        '2': 'Reservas Pendientes',
        '3': 'Reservas Finalizadas',
        '4': 'Reservas Canceladas',
        '5': 'Reservas Reembolsadas',
        '6': 'Reembolsos Pendientes',
        '7': 'Reservas NO Reembolsadas'
    };

    // ===========================================
    // MANEJO DE FILTROS (sin cambios)
    // ===========================================
    filterTabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            e.preventDefault();
            filterTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            const filterValue = tab.dataset.value;
            filterSelect.value = filterValue;
            filterForm.submit();
        });
    });

    // ===========================================
    // BOTONES DE CANCELAR DIRECTO - REFACTORIZADO
    // ===========================================
    document.querySelectorAll(".btn-cancelar").forEach(btn => {
        btn.addEventListener("click", e => {
            e.preventDefault();

            const reservaData = extraerDatosReserva(btn);
            const infoReembolso = determinarTipoReembolso(reservaData);

            mostrarConfirmacionCancelacion(reservaData, infoReembolso);
        });
    });

    // ===========================================
    // BOTONES DE SOLICITAR CANCELACIÓN - REFACTORIZADO
    // ===========================================
    document.querySelectorAll(".btn-solicitar").forEach(btn => {
        btn.addEventListener("click", e => {
            e.preventDefault();

            const reservaData = extraerDatosReserva(btn);
            mostrarConfirmacionSolicitud(reservaData);
        });
    });

    // ===========================================
    // FUNCIONES AUXILIARES
    // ===========================================

    /**
     * Extrae todos los datos relevantes de una reserva desde el botón
     */
    function extraerDatosReserva(btn) {
        return {
            id: btn.dataset.id,
            estado: btn.dataset.estado?.trim() || '',
            tipoPago: btn.dataset.tipoPago?.trim() || '',
            horasRestantes: parseInt(btn.dataset.horasRestantes) || 0,
            fechaReserva: btn.dataset.fechaReserva || '',
            horaInicio: btn.dataset.horaInicio || '',
            tipoReembolso: btn.dataset.tipoReembolso || ''
        };
    }

    /**
     * Determina el tipo de reembolso y mensaje según los datos de la reserva
     */
    function determinarTipoReembolso(data) {
        let mensaje = "";
        let tipoReembolso = "";
        let tiempoEstimado = "";
        let icono = "warning";

        const { estado, tipoPago, horasRestantes } = data;

        if (estado === "Pendiente de confirmación") {
            mensaje = "Esta reserva no estaba confirmada, por lo que no se había procesado el pago.";
            tipoReembolso = "Sin reembolso";
            tiempoEstimado = "Inmediato";
            icono = "info";

        } else if (estado === "Confirmada") {

            if (tipoPago === "En línea") {
                if (horasRestantes >= 24) {
                    mensaje = "Como tu reserva tiene más de 24 horas de anticipación, tienes derecho a reembolso automático.";
                    tipoReembolso = "Reembolso automático vía MercadoPago";
                    tiempoEstimado = "3-5 días hábiles";
                    icono = "success";
                } else {
                    // Este caso no debería ocurrir con el botón cancelar directo
                    mensaje = "Reserva muy próxima. Debes usar 'Solicitar cancelación'.";
                    tipoReembolso = "No aplicable";
                    tiempoEstimado = "N/A";
                    icono = "error";
                }

            } else if (tipoPago === "En banco") {
                mensaje = "Tu pago fue realizado en banco, por lo que el reembolso será procesado manualmente.";
                tipoReembolso = "Reembolso manual por coordinador";
                tiempoEstimado = "3-5 días hábiles";
                icono = "warning";
            }
        }

        return { mensaje, tipoReembolso, tiempoEstimado, icono };
    }

    /**
     * Muestra la confirmación para cancelación directa
     */
    function mostrarConfirmacionCancelacion(reservaData, infoReembolso) {
        const { id, estado, tipoPago, horasRestantes, fechaReserva, horaInicio } = reservaData;
        const { mensaje, tipoReembolso, tiempoEstimado, icono } = infoReembolso;

        Swal.fire({
            title: '¿Confirmar cancelación?',
            html: `
                <div class="cancelacion-info">
                    <div class="reserva-detalles">
                        <h6><i class="bx bx-calendar me-2"></i>Detalles de la Reserva</h6>
                        <div class="row text-start">
                            <div class="col-6"><strong>Fecha:</strong></div>
                            <div class="col-6">${fechaReserva}</div>
                            <div class="col-6"><strong>Hora:</strong></div>
                            <div class="col-6">${horaInicio}</div>
                            <div class="col-6"><strong>Estado:</strong></div>
                            <div class="col-6">${estado}</div>
                            <div class="col-6"><strong>Pago:</strong></div>
                            <div class="col-6">${tipoPago}</div>
                            <div class="col-6"><strong>Tiempo restante:</strong></div>
                            <div class="col-6">${horasRestantes} horas</div>
                        </div>
                    </div>
                    
                    <hr class="my-3">
                    
                    <div class="reembolso-info">
                        <h6><i class="bx bx-money me-2"></i>Información de Reembolso</h6>
                        <p class="mb-2">${mensaje}</p>
                        <div class="alert alert-info p-2 mb-2">
                            <small>
                                <strong>Tipo:</strong> ${tipoReembolso}<br>
                                <strong>Tiempo estimado:</strong> ${tiempoEstimado}
                            </small>
                        </div>
                    </div>
                </div>
            `,
            icon: icono,
            showCancelButton: true,
            confirmButtonText: 'Sí, cancelar reserva',
            cancelButtonText: 'No, mantener reserva',
            reverseButtons: true,
            customClass: {
                confirmButton: 'btn btn-danger me-2',
                cancelButton: 'btn btn-secondary'
            },
            buttonsStyling: false,
            width: 650,
            showLoaderOnConfirm: true,
            preConfirm: () => {
                return ejecutarCancelacionDirecta(id);
            },
            allowOutsideClick: () => !Swal.isLoading()
        }).then((result) => {
            if (result.isConfirmed && result.value) {
                mostrarResultadoCancelacion(result.value);
            }
        });
    }

    /**
     * Muestra la confirmación para solicitud de cancelación
     */
    function mostrarConfirmacionSolicitud(reservaData) {
        const { id, estado, tipoPago, horasRestantes, fechaReserva, horaInicio } = reservaData;

        let mensajeMotivo = "";
        if (horasRestantes < 24) {
            mensajeMotivo = "Tu reserva está muy próxima (menos de 24 horas). El coordinador evaluará tu solicitud considerando el motivo.";
        } else if (tipoPago === "En banco") {
            mensajeMotivo = "Como tu pago fue en banco, todas las cancelaciones requieren aprobación del coordinador.";
        }

        Swal.fire({
            title: 'Solicitar cancelación',
            html: `
                <div class="solicitud-info">
                    <div class="reserva-detalles mb-3">
                        <h6><i class="bx bx-calendar me-2"></i>Detalles de la Reserva</h6>
                        <div class="row text-start">
                            <div class="col-6"><strong>Fecha:</strong></div>
                            <div class="col-6">${fechaReserva}</div>
                            <div class="col-6"><strong>Hora:</strong></div>
                            <div class="col-6">${horaInicio}</div>
                            <div class="col-6"><strong>Estado:</strong></div>
                            <div class="col-6">${estado}</div>
                            <div class="col-6"><strong>Pago:</strong></div>
                            <div class="col-6">${tipoPago}</div>
                            <div class="col-6"><strong>Tiempo restante:</strong></div>
                            <div class="col-6">${horasRestantes} horas</div>
                        </div>
                    </div>
                    
                    <div class="alert alert-warning p-2 mb-3">
                        <small><i class="bx bx-info-circle me-1"></i>${mensajeMotivo}</small>
                    </div>
                    
                    <p class="text-muted">
                        <strong>Proceso:</strong> 
                        ${tipoPago === 'En banco' ?
                'Reembolso manual en 3-5 días hábiles si es aprobado' :
                'Reembolso según evaluación del coordinador'}
                    </p>
                </div>
            `,
            icon: 'info',
            showCancelButton: true,
            confirmButtonText: 'Continuar',
            cancelButtonText: 'Cancelar',
            customClass: {
                confirmButton: 'btn btn-warning me-2',
                cancelButton: 'btn btn-secondary'
            },
            buttonsStyling: false,
            width: 600
        }).then(result => {
            if (result.isConfirmed) {
                abrirModalSolicitud(id);
            }
        });
    }

    /**
     * Abre el modal de solicitud de cancelación
     */
    function abrirModalSolicitud(reservaId) {
        document.getElementById("inputIdReserva").value = reservaId;

        // Limpiar el formulario
        document.getElementById("motivo").value = "";
        document.getElementById("codigoPago").value = "";

        const modal = new bootstrap.Modal(document.getElementById("modalSolicitudCancelacion"));
        modal.show();
    }

    /**
     * Ejecuta la cancelación directa vía AJAX
     */
    async function ejecutarCancelacionDirecta(reservaId) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            const formData = new FormData();
            formData.append('id', reservaId);

            const response = await fetch('/vecino/cancelarReserva', {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken
                },
                body: formData
            });

            if (response.ok) {
                return {
                    success: true,
                    message: 'Reserva cancelada exitosamente'
                };
            } else {
                let errorMessage = 'Error al cancelar la reserva';
                try {
                    const errorText = await response.text();
                    if (errorText && errorText.length > 0 && errorText.length < 200) {
                        errorMessage = errorText;
                    }
                } catch (e) {
                    console.warn('No se pudo obtener mensaje de error del servidor');
                }

                return {
                    success: false,
                    message: errorMessage,
                    status: response.status
                };
            }

        } catch (error) {
            console.error('Error en cancelación:', error);
            return {
                success: false,
                message: 'Error de conexión. Verifica tu internet.',
                isConnectionError: true
            };
        }
    }

    /**
     * Muestra el resultado de la cancelación
     */
    function mostrarResultadoCancelacion(resultado) {
        if (resultado.success) {
            Swal.fire({
                icon: 'success',
                title: '¡Reserva cancelada!',
                text: resultado.message,
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#28a745',
                timer: 3000,
                timerProgressBar: true
            }).then(() => {
                window.location.reload();
            });
        } else {
            let footerMessage = '';

            if (resultado.isConnectionError) {
                footerMessage = '<small>Verifica tu conexión a internet e intenta de nuevo.</small>';
            } else if (resultado.status === 403) {
                footerMessage = '<small>Problema de permisos. Intenta recargar la página.</small>';
            } else if (resultado.status >= 500) {
                footerMessage = '<small>Error del servidor. Intenta más tarde.</small>';
            } else {
                footerMessage = '<small>Verifica que la reserva aún puede cancelarse.</small>';
            }

            Swal.fire({
                icon: 'error',
                title: 'Error al cancelar',
                text: resultado.message,
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#dc3545',
                footer: footerMessage
            });
        }
    }

    // ===========================================
    // VALIDACIÓN DEL FORMULARIO DE SOLICITUD
    // ===========================================
    const modalSolicitud = document.getElementById('modalSolicitudCancelacion');
    if (modalSolicitud) {
        const formSolicitud = modalSolicitud.querySelector('form');
        const motivoTextarea = document.getElementById('motivo');
        const submitBtn = formSolicitud.querySelector('button[type="submit"]');

        // Validación en tiempo real del motivo
        if (motivoTextarea) {
            motivoTextarea.addEventListener('input', function() {
                const longitud = this.value.trim().length;

                if (longitud < 10) {
                    this.classList.add('is-invalid');
                    if (submitBtn) submitBtn.disabled = true;

                    // Mostrar mensaje de ayuda
                    let feedback = this.parentElement.querySelector('.invalid-feedback');
                    if (!feedback) {
                        feedback = document.createElement('div');
                        feedback.className = 'invalid-feedback';
                        this.parentElement.appendChild(feedback);
                    }
                    feedback.textContent = `Mínimo 10 caracteres. Tienes ${longitud}.`;

                } else {
                    this.classList.remove('is-invalid');
                    if (submitBtn) submitBtn.disabled = false;

                    const feedback = this.parentElement.querySelector('.invalid-feedback');
                    if (feedback) feedback.remove();
                }
            });
        }

        // Mejorar la experiencia de envío
        if (formSolicitud) {
            formSolicitud.addEventListener('submit', function(e) {
                const motivo = motivoTextarea.value.trim();

                if (motivo.length < 10) {
                    e.preventDefault();
                    motivoTextarea.focus();
                    return false;
                }

                // Mostrar loading
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<i class="bx bx-loader-alt bx-spin me-1"></i>Enviando...';
                }
            });
        }
    }

    // ===========================================
    // ANIMACIONES Y EFECTOS VISUALES
    // ===========================================

    // Animaciones de entrada para las cards
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);

    // Aplicar animación a las cards
    document.querySelectorAll('.reserva-card').forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        card.style.transition = `opacity 0.6s ease ${index * 0.1}s, transform 0.6s ease ${index * 0.1}s`;
        observer.observe(card);
    });

    // Efecto hover mejorado para botones
    document.querySelectorAll('.btn-cancelar, .btn-solicitar').forEach(btn => {
        btn.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
            this.style.boxShadow = '0 4px 8px rgba(0,0,0,0.2)';
        });

        btn.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
            this.style.boxShadow = 'none';
        });
    });

    // ===========================================
    // UTILIDADES ADICIONALES
    // ===========================================

    /**
     * Función para formatear fechas en español
     */
    function formatearFecha(fecha) {
        const opciones = {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            weekday: 'long'
        };
        return new Date(fecha).toLocaleDateString('es-ES', opciones);
    }

    /**
     * Función para calcular tiempo restante más preciso
     */
    function calcularTiempoRestante(fechaReserva, horaReserva) {
        const ahora = new Date();
        const reserva = new Date(`${fechaReserva}T${horaReserva}`);
        const diferencia = reserva - ahora;

        if (diferencia <= 0) return "Ya empezó";

        const dias = Math.floor(diferencia / (1000 * 60 * 60 * 24));
        const horas = Math.floor((diferencia % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutos = Math.floor((diferencia % (1000 * 60 * 60)) / (1000 * 60));

        if (dias > 0) return `${dias}d ${horas}h`;
        if (horas > 0) return `${horas}h ${minutos}m`;
        return `${minutos}m`;
    }

    console.log("✅ Script de mis-reservas-behavior refactorizado cargado correctamente");
});