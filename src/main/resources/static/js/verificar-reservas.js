// Variables globales
let tablaReservas;
let reservaSeleccionada = null;
let eventListenersInitialized = false;

$(document).ready(function() {
    // Verificar si ya se inicializó para evitar duplicados
    if (eventListenersInitialized) {
        console.warn('⚠️ Event listeners ya inicializados, evitando duplicados');
        return;
    }

    // Inicializar DataTable solo una vez
    initializeDataTable();

    // Inicializar event listeners una sola vez
    initializeEventListeners();

    // Inicializar contadores animados
    inicializarContadores();

    // Inicializar tooltips
    initializeTooltips();

    // Configurar notificaciones iniciales
    checkUrgentReservations();

    // Marcar como inicializado
    eventListenersInitialized = true;

    console.log('✅ Vista de verificación de reservas inicializada');
});

// Función separada para inicializar DataTable
function initializeDataTable() {
    // Destruir tabla existente si existe
    if ($.fn.DataTable.isDataTable('#tablaReservas')) {
        $('#tablaReservas').DataTable().destroy();
    }

    tablaReservas = $('#tablaReservas').DataTable({
        responsive: true,
        order: [[3, 'asc']], // Ordenar por fecha (columna 3)
        pageLength: 10,
        language: {
            url: '/lang/es-ES.json',
            // Fallback si no se encuentra el archivo de idioma
            emptyTable: "No hay reservas pendientes de verificación",
            info: "Mostrando _START_ a _END_ de _TOTAL_ reservas",
            infoEmpty: "Mostrando 0 a 0 de 0 reservas",
            infoFiltered: "(filtrado de _MAX_ reservas totales)",
            lengthMenu: "Mostrar _MENU_ reservas por página",
            loadingRecords: "Cargando...",
            processing: "Procesando...",
            search: "Buscar:",
            zeroRecords: "No se encontraron reservas que coincidan",
            paginate: {
                first: "Primero",
                last: "Último",
                next: "Siguiente",
                previous: "Anterior"
            }
        },
        columnDefs: [
            { targets: [7], orderable: false }, // Columna de acciones no ordenable
            { targets: [4], className: 'text-center' }, // Centrar columna monto
            { targets: [5], className: 'text-center' }, // Centrar columna estado
            { targets: [6], className: 'text-center' } // Centrar columna comprobante
        ],
        dom: '<"row"<"col-sm-12 col-md-6"l><"col-sm-12 col-md-6"f>>rtip',
        autoWidth: false
    });
}

// 🔧 NUEVA FUNCIÓN: Inicializar event listeners de forma controlada
function initializeEventListeners() {
    // Limpiar event listeners previos
    $(document).off('click.verificarReservas');

    // Event delegation para botones de confirmar
    $(document).on('click.verificarReservas', '[data-action="confirmar"]', function(e) {
        e.preventDefault();
        e.stopImmediatePropagation();

        // Verificar si ya hay un SweetAlert abierto
        if (Swal.isVisible()) {
            console.warn('⚠️ SweetAlert ya está visible, ignorando clic');
            return;
        }

        const reservaId = $(this).attr('data-reserva-id');
        const vecino = $(this).attr('data-vecino');

        if (!reservaId) {
            console.error('ID de reserva no encontrado');
            return;
        }

        mostrarConfirmacionReserva(reservaId, vecino);
    });

    // Event delegation para botones de rechazar
    $(document).on('click.verificarReservas', '[data-action="rechazar"]', function(e) {
        e.preventDefault();
        e.stopImmediatePropagation();

        // Verificar si ya hay un SweetAlert abierto
        if (Swal.isVisible()) {
            console.warn('⚠️ SweetAlert ya está visible, ignorando clic');
            return;
        }

        const reservaId = $(this).attr('data-reserva-id');
        const vecino = $(this).attr('data-vecino');

        if (!reservaId) {
            console.error('ID de reserva no encontrado');
            return;
        }

        mostrarRechazoReserva(reservaId, vecino);
    });

    // Event listeners para botones del modal
    $('#btnConfirmarDesdeModal').off('click.modal').on('click.modal', function() {
        const reservaId = $(this).attr('data-reserva-id');
        if (!reservaId) return;

        $('#modalComprobante').modal('hide');
        setTimeout(() => {
            const vecino = $('#infoVecino').text();
            mostrarConfirmacionReserva(reservaId, vecino);
        }, 500);
    });

    $('#btnRechazarDesdeModal').off('click.modal').on('click.modal', function() {
        const reservaId = $(this).attr('data-reserva-id');
        if (!reservaId) return;

        $('#modalComprobante').modal('hide');
        setTimeout(() => {
            const vecino = $('#infoVecino').text();
            mostrarRechazoReserva(reservaId, vecino);
        }, 500);
    });
}

// Función para ver comprobante (URLs prefirmadas con S3)
function verComprobante(btn) {
    const reservaId = btn.getAttribute('data-reserva-id');

    if (!reservaId) {
        console.error('ID de reserva no encontrado');
        return;
    }

    // Obtener datos de la fila
    const fila = $(btn).closest('tr');
    const vecino = fila.find('td:eq(1) h6').text().trim();
    const espacio = fila.find('td:eq(2) h6').text().trim();
    const fecha = fila.find('td:eq(3) strong').text().trim();
    const hora = fila.find('td:eq(3) small span').text().trim();
    const monto = fila.find('td:eq(4) span span').text().trim();

    // Configurar información del modal
    $('#reservaNumero').text(reservaId);
    $('#infoReservaId').text(reservaId);
    $('#infoVecino').text(vecino);
    $('#infoEspacio').text(espacio);
    $('#infoFecha').text(fecha);
    $('#infoHora').text(hora);
    $('#infoMonto').text(monto);

    // Configurar botones del modal
    reservaSeleccionada = reservaId;
    $('#btnConfirmarDesdeModal').attr('data-reserva-id', reservaId);
    $('#btnRechazarDesdeModal').attr('data-reserva-id', reservaId);

    // Mostrar loading en la imagen
    $('#imagenComprobante')
        .attr('src', '/img/loading.gif')
        .attr('alt', 'Cargando comprobante...')
        .removeClass('d-none');

    // Limpiar errores previos
    $('#errorComprobante').remove();

    // Mostrar el modal primero
    $('#modalComprobante').modal('show');

    // Obtener URL prefirmada del comprobante
    fetch(`/coordinador/comprobante/${reservaId}`, {
        method: 'GET',
        headers: {
            'X-Requested-With': 'XMLHttpRequest',
            [getCSRFHeader()]: getCSRFToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: No se pudo obtener el comprobante`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.url) {
                // Configurar la imagen con la URL prefirmada
                $('#imagenComprobante')
                    .attr('src', data.url)
                    .attr('alt', `Comprobante de pago - Reserva #${reservaId}`)
                    .on('load', function() {
                        console.log('✅ Comprobante cargado exitosamente');
                    });
            } else {
                throw new Error(data.error || 'URL del comprobante no disponible');
            }
        })
        .catch(error => {
            console.error('❌ Error al cargar comprobante:', error);
            handleComprobanteError(error.message);
        });
}

// Función para mostrar confirmación de reserva
function mostrarConfirmacionReserva(reservaId, vecino) {
    Swal.fire({
        title: '¿Confirmar reserva?',
        html: `
            <div class="text-start">
                <p><strong>Reserva:</strong> #${reservaId}</p>
                <p><strong>Vecino:</strong> ${vecino}</p>
                <hr>
                <p class="text-warning">
                    <i class="bx bx-info-circle me-1"></i>
                    ¿Has verificado que el comprobante de pago es válido?
                </p>
                <p class="text-muted small">
                    Esta acción enviará una confirmación por correo al vecino.
                </p>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '<i class="bx bx-check me-1"></i> Sí, confirmar',
        cancelButtonText: '<i class="bx bx-x me-1"></i> Cancelar',
        confirmButtonColor: '#28a745',
        cancelButtonColor: '#6c757d',
        focusConfirm: false,
        allowOutsideClick: true,
        allowEscapeKey: true,
        customClass: {
            confirmButton: 'btn btn-success',
            cancelButton: 'btn btn-secondary'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            procesarConfirmacion(reservaId);
        }
    });
}

// Función para mostrar rechazo de reserva
function mostrarRechazoReserva(reservaId, vecino) {
    Swal.fire({
        title: '¿Rechazar reserva?',
        html: `
            <div class="text-start mb-3">
                <p><strong>Reserva:</strong> #${reservaId}</p>
                <p><strong>Vecino:</strong> ${vecino}</p>
                <hr>
                <div class="mb-3">
                    <label for="motivoRechazo" class="form-label">
                        <i class="bx bx-edit me-1"></i>
                        Motivo del rechazo <span class="text-danger">*</span>
                    </label>
                    <textarea id="motivoRechazo" class="form-control" rows="4" 
                              placeholder="Ej: Comprobante no válido, monto incorrecto, imagen ilegible, etc." 
                              required maxlength="500"></textarea>
                    <div class="form-text">Máximo 500 caracteres</div>
                </div>
                <p class="text-muted small">
                    <i class="bx bx-info-circle me-1"></i>
                    Se enviará una notificación por correo al vecino con el motivo del rechazo.
                </p>
            </div>
        `,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '<i class="bx bx-x me-1"></i> Rechazar',
        cancelButtonText: '<i class="bx bx-arrow-back me-1"></i> Cancelar',
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        focusConfirm: false,
        allowOutsideClick: true,
        allowEscapeKey: true,
        customClass: {
            confirmButton: 'btn btn-danger',
            cancelButton: 'btn btn-secondary'
        },
        preConfirm: () => {
            const motivo = document.getElementById('motivoRechazo').value.trim();
            if (!motivo) {
                Swal.showValidationMessage('Debe proporcionar un motivo para el rechazo');
                return false;
            }
            if (motivo.length < 10) {
                Swal.showValidationMessage('El motivo debe tener al menos 10 caracteres');
                return false;
            }
            return motivo;
        }
    }).then((result) => {
        if (result.isConfirmed) {
            procesarRechazo(reservaId, result.value);
        }
    });
}

// Procesar confirmación de reserva
function procesarConfirmacion(reservaId) {
    Swal.fire({
        title: 'Confirmando reserva...',
        text: 'Por favor espere mientras se procesa la confirmación',
        allowOutsideClick: false,
        allowEscapeKey: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    fetch(`/coordinador/reserva/${reservaId}/confirmar`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            [getCSRFHeader()]: getCSRFToken()
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                Swal.fire({
                    icon: 'success',
                    title: '¡Reserva confirmada!',
                    text: 'La reserva ha sido confirmada exitosamente y se notificó al vecino',
                    timer: 3000,
                    showConfirmButton: false,
                    timerProgressBar: true
                }).then(() => {
                    location.reload();
                });
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Error al confirmar',
                    text: data.message || 'Ocurrió un error inesperado',
                    confirmButtonText: 'Entendido'
                });
            }
        })
        .catch(error => {
            console.error('❌ Error al confirmar reserva:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error de conexión',
                text: 'No se pudo conectar con el servidor. Verifique su conexión a internet.',
                confirmButtonText: 'Reintentar'
            });
        });
}

// Procesar rechazo de reserva
function procesarRechazo(reservaId, motivo) {
    Swal.fire({
        title: 'Rechazando reserva...',
        text: 'Por favor espere mientras se procesa el rechazo',
        allowOutsideClick: false,
        allowEscapeKey: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    fetch(`/coordinador/reserva/${reservaId}/rechazar`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            [getCSRFHeader()]: getCSRFToken()
        },
        body: JSON.stringify({ motivo: motivo })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                Swal.fire({
                    icon: 'success',
                    title: 'Reserva rechazada',
                    text: 'La reserva ha sido rechazada y se notificó al vecino',
                    timer: 3000,
                    showConfirmButton: false,
                    timerProgressBar: true
                }).then(() => {
                    location.reload();
                });
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Error al rechazar',
                    text: data.message || 'Ocurrió un error inesperado',
                    confirmButtonText: 'Entendido'
                });
            }
        })
        .catch(error => {
            console.error('❌ Error al rechazar reserva:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error de conexión',
                text: 'No se pudo conectar con el servidor. Verifique su conexión a internet.',
                confirmButtonText: 'Reintentar'
            });
        });
}

// Funciones de filtrado
function filtrarPor(tipo) {
    if (!tablaReservas) return;

    switch(tipo) {
        case 'todos':
            tablaReservas.search('').columns().search('').draw();
            break;
        case 'pendientes':
            tablaReservas.column(5).search('Pendiente de confirmación', true, false).draw();
            break;
        case 'urgentes':
            tablaReservas.search('HOY', true, false).draw();
            break;
        case 'semana':
            console.log('Filtrar por semana - Por implementar');
            break;
        default:
            console.warn('Tipo de filtro no reconocido:', tipo);
    }
}

// Función para refrescar página
function refrescarPagina() {
    location.reload();
}

// Inicializar contadores animados
function inicializarContadores() {
    const counters = document.querySelectorAll('.counter');
    const speed = 200;

    if (counters.length === 0) return;

    counters.forEach(counter => {
        const target = parseInt(counter.getAttribute('data-target')) || 0;
        let count = 0;

        const updateCount = () => {
            const increment = Math.max(1, Math.floor(target / speed));
            count += increment;

            if (count < target) {
                counter.innerText = count;
                setTimeout(updateCount, 20);
            } else {
                counter.innerText = target;
            }
        };

        updateCount();
    });
}

// Inicializar tooltips
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// Verificar reservas urgentes y mostrar notificación
function checkUrgentReservations() {
    const urgentes = $('.badge:contains("HOY")').length;
    const manana = $('.badge:contains("MAÑANA")').length;

    if (urgentes > 0) {
        if (typeof toastr !== 'undefined') {
            toastr.warning(
                `Tienes ${urgentes} reserva(s) para HOY que requieren verificación urgente`,
                'Reservas Urgentes',
                {
                    timeOut: 8000,
                    extendedTimeOut: 3000,
                    progressBar: true,
                    positionClass: 'toast-top-right'
                }
            );
        }
    }

    if (manana > 0) {
        if (typeof toastr !== 'undefined') {
            toastr.info(
                `Tienes ${manana} reserva(s) para MAÑANA pendientes de verificación`,
                'Reservas Próximas',
                {
                    timeOut: 6000,
                    progressBar: true,
                    positionClass: 'toast-top-right'
                }
            );
        }
    }
}

// Manejar errores de carga de comprobante
function handleComprobanteError(errorMessage) {
    $('#imagenComprobante')
        .attr('src', '/img/error-image.png')
        .attr('alt', 'Error al cargar comprobante');

    if ($('#errorComprobante').length === 0) {
        $('#imagenComprobante').after(`
            <div id="errorComprobante" class="alert alert-warning mt-3" role="alert">
                <i class="bx bx-exclamation-triangle me-2"></i>
                <strong>Error:</strong> ${errorMessage}
                <br><small class="text-muted">Verifica que el archivo existe y es accesible.</small>
            </div>
        `);
    }
}

// Limpiar modal cuando se cierre
$(document).on('hidden.bs.modal', '#modalComprobante', function () {
    $('#imagenComprobante').attr('src', '').attr('alt', '').off('load');
    $('#errorComprobante').remove();
    reservaSeleccionada = null;
});

// Manejo de errores de carga de imágenes
$(document).on('error', '#imagenComprobante', function() {
    console.log('❌ Error al cargar imagen del comprobante');
    $(this).attr('src', '/img/no-image-placeholder.png');

    if ($('#errorComprobante').length === 0) {
        handleComprobanteError('No se pudo cargar la imagen del comprobante');
    }
});

// Funciones de utilidad para CSRF
function getCSRFToken() {
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.getAttribute('content') : '';
}

function getCSRFHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]');
    return header ? header.getAttribute('content') : 'X-CSRF-TOKEN';
}

// Función para exportar reservas (si se implementa)
function exportarReservas() {
    window.location.href = '/coordinador/exportar-reservas-pendientes';
}

// Log de inicialización
console.log('📋 Script verificar-reservas.js cargado correctamente');