// Variables globales
let pagosDatatable;
let flatpickrInicio, flatpickrFin;

// Inicialización cuando el DOM está listo
$(document).ready(function() {
    console.log("🚀 Inicializando página de mis pagos...");

    // Inicializar componentes
    initializeFlatpickr();
    initializeDataTable();
    cargarEstadisticas();

    console.log("✅ Página inicializada correctamente");
});

/**
 * Inicializar Flatpickr para fechas
 */
function initializeFlatpickr() {
    console.log("📅 Inicializando date pickers...");

    flatpickrInicio = flatpickr("#fechaInicio", {
        locale: "es",
        dateFormat: "Y-m-d",
        maxDate: "today",
        onChange: function(selectedDates) {
            if (selectedDates.length > 0) {
                flatpickrFin.set('minDate', selectedDates[0]);
            }
        }
    });

    flatpickrFin = flatpickr("#fechaFin", {
        locale: "es",
        dateFormat: "Y-m-d",
        maxDate: "today"
    });
}

/**
 * Inicializar DataTable
 */
function initializeDataTable() {
    console.log("📊 Inicializando DataTable...");

    pagosDatatable = $('#pagosDatatable').DataTable({
        processing: true,
        serverSide: false, // Cambiamos a false para manejar filtros localmente
        ajax: {
            url: '/vecino/api/pagos',
            type: 'GET',
            data: function(d) {
                // Agregar parámetros de filtros
                const formData = new FormData(document.getElementById('filtrosForm'));

                d.fechaInicio = formData.get('fechaInicio');
                d.fechaFin = formData.get('fechaFin');
                d.tipoPago = formData.get('tipoPago');
                d.estado = formData.get('estado');
                d.espacio = formData.get('espacio');

                return d;
            },
            dataSrc: function(json) {
                console.log("📊 Datos recibidos:", json);
                return json.data || [];
            },
            error: function(xhr, error, thrown) {
                console.error("❌ Error cargando datos:", error);
                mostrarToast('Error cargando los pagos', 'error');
            }
        },
        columns: [
            {
                data: 'idPago',
                className: 'text-center fw-bold'
            },
            {
                data: 'fechaPago',
                render: function(data) {
                    if (!data) return '-';
                    const fecha = new Date(data);
                    return fecha.toLocaleDateString('es-PE', {
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                    });
                }
            },
            {
                data: 'espacioNombre',
                render: function(data, type, row) {
                    return data || 'N/A';
                }
            },
            {
                data: 'espacioLugar',
                render: function(data) {
                    return data || 'N/A';
                }
            },
            {
                data: 'fechaReserva',
                render: function(data) {
                    if (!data) return '-';
                    const fecha = new Date(data);
                    return fecha.toLocaleDateString('es-PE');
                }
            },
            {
                data: null,
                render: function(data, type, row) {
                    if (row.horaInicio && row.horaFin) {
                        return `${row.horaInicio} - ${row.horaFin}`;
                    }
                    return '-';
                }
            },
            {
                data: 'tipoPago',
                render: function(data) {
                    if (!data) return '-';

                    const badgeClass = data === 'En línea' ? 'bg-success' : 'bg-info';
                    const icon = data === 'En línea' ? 'bx-credit-card' : 'bx-building-house';

                    return `<span class="badge ${badgeClass} badge-custom">
                                <i class="bx ${icon} me-1"></i>${data}
                            </span>`;
                }
            },
            {
                data: 'monto',
                className: 'text-end',
                render: function(data) {
                    return data ? `S/. ${parseFloat(data).toFixed(2)}` : 'S/. 0.00';
                }
            },
            {
                data: 'estado',
                render: function(data) {
                    if (!data) return '-';

                    const badgeClass = data === 'Pagado' ? 'bg-success' : 'bg-warning';
                    return `<span class="badge ${badgeClass} badge-custom">${data}</span>`;
                }
            },
            {
                data: null,
                orderable: false,
                className: 'text-center',
                render: function(data, type, row) {
                    return `
                        <button type="button" class="btn btn-sm btn-outline-primary" 
                                onclick="verDetallePago(${row.idPago})" 
                                title="Ver detalles">
                            <i class="bx bx-show"></i>
                        </button>
                    `;
                }
            }
        ],
        order: [[1, 'desc']], // Ordenar por fecha de pago descendente
        pageLength: 10,
        lengthMenu: [[10, 25, 50, 100], [10, 25, 50, 100]],
        language: {
            url: '/vendor/libs/datatables/es.json'
        },
        responsive: true,
        dom: '<"row"<"col-sm-12 col-md-6"l><"col-sm-12 col-md-6"f>>rt<"row"<"col-sm-12 col-md-5"i><"col-sm-12 col-md-7"p>>',
        drawCallback: function(settings) {
            // Actualizar contador de registros
            const info = this.api().page.info();
            console.log(`📊 Mostrando ${info.recordsDisplay} de ${info.recordsTotal} pagos`);
        }
    });
}

/**
 * Aplicar filtros al DataTable
 */
function aplicarFiltros() {
    console.log("🔍 Aplicando filtros...");

    mostrarCargando(true);

    // Recargar DataTable con nuevos filtros
    pagosDatatable.ajax.reload(function() {
        mostrarCargando(false);
        cargarEstadisticas(); // Actualizar estadísticas con filtros
        mostrarToast('Filtros aplicados correctamente', 'success');
    });
}

/**
 * Limpiar todos los filtros
 */
function limpiarFiltros() {
    console.log("🧹 Limpiando filtros...");

    // Limpiar formulario
    document.getElementById('filtrosForm').reset();

    // Limpiar date pickers
    if (flatpickrInicio) flatpickrInicio.clear();
    if (flatpickrFin) flatpickrFin.clear();

    // Recargar datos
    aplicarFiltros();
}

/**
 * Cargar estadísticas
 */
function cargarEstadisticas() {
    console.log("📊 Cargando estadísticas...");

    const formData = new FormData(document.getElementById('filtrosForm'));
    const params = new URLSearchParams();

    // Agregar filtros de fecha si existen
    if (formData.get('fechaInicio')) {
        params.append('dateRange[start]', formData.get('fechaInicio'));
    }
    if (formData.get('fechaFin')) {
        params.append('dateRange[end]', formData.get('fechaFin'));
    }

    fetch(`/vecino/api/pagos/estadisticas?${params.toString()}`)
        .then(response => response.json())
        .then(data => {
            console.log("📊 Estadísticas recibidas:", data);
            actualizarTarjetasEstadisticas(data);
        })
        .catch(error => {
            console.error("❌ Error cargando estadísticas:", error);
            mostrarToast('Error cargando estadísticas', 'error');
        });
}

/**
 * Actualizar tarjetas de estadísticas
 */
function actualizarTarjetasEstadisticas(stats) {
    document.getElementById('totalPagosCount').textContent = stats.totalPagos || 0;
    document.getElementById('montoTotalAmount').textContent = `S/. ${(stats.montoTotal || 0).toFixed(2)}`;
    document.getElementById('pagosOnlineCount').textContent = stats.pagosOnline || 0;
    document.getElementById('pagosBancoCount').textContent = stats.pagosBanco || 0;
}

/**
 * Ver detalle de un pago
 */
function verDetallePago(idPago) {
    console.log("🔍 Cargando detalle del pago:", idPago);

    const modal = new bootstrap.Modal(document.getElementById('modalDetallePago'));
    const modalBody = document.getElementById('modalDetallePagoBody');

    // Mostrar loading en modal
    modalBody.innerHTML = `
        <div class="text-center py-4">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Cargando...</span>
            </div>
            <p class="mt-2 text-muted">Cargando detalles del pago...</p>
        </div>
    `;

    modal.show();

    // Cargar datos del pago
    fetch(`/vecino/api/pagos/${idPago}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error cargando detalles');
            }
            return response.json();
        })
        .then(data => {
            console.log("📋 Detalles del pago:", data);
            mostrarDetallePago(data);
        })
        .catch(error => {
            console.error("❌ Error cargando detalles:", error);
            modalBody.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bx bx-error me-2"></i>Error cargando los detalles del pago
                </div>
            `;
        });
}

/**
 * Mostrar detalles del pago en el modal
 */
function mostrarDetallePago(pago) {
    const modalBody = document.getElementById('modalDetallePagoBody');

    const fechaPago = pago.fechaPago ? new Date(pago.fechaPago).toLocaleDateString('es-PE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }) : 'N/A';

    const fechaReserva = pago.fechaReserva ? new Date(pago.fechaReserva).toLocaleDateString('es-PE') : 'N/A';

    const horario = (pago.horaInicio && pago.horaFin) ? `${pago.horaInicio} - ${pago.horaFin}` : 'N/A';

    modalBody.innerHTML = `
        <div class="row">
            <div class="col-md-6 mb-3">
                <h6 class="text-muted mb-2">Información del Pago</h6>
                <div class="card border-0 bg-light">
                    <div class="card-body">
                        <p class="mb-2"><strong>ID del Pago:</strong> ${pago.idPago}</p>
                        <p class="mb-2"><strong>Fecha de Pago:</strong> ${fechaPago}</p>
                        <p class="mb-2"><strong>Monto:</strong> S/. ${(pago.monto || 0).toFixed(2)}</p>
                        <p class="mb-2"><strong>Tipo de Pago:</strong> 
                            <span class="badge ${pago.tipoPago === 'En línea' ? 'bg-success' : 'bg-info'}">${pago.tipoPago || 'N/A'}</span>
                        </p>
                        <p class="mb-0"><strong>Estado:</strong> 
                            <span class="badge ${pago.estado === 'Pagado' ? 'bg-success' : 'bg-warning'}">${pago.estado || 'N/A'}</span>
                        </p>
                        ${pago.referencia ? `<p class="mb-0 mt-2"><strong>Referencia:</strong> ${pago.referencia}</p>` : ''}
                    </div>
                </div>
            </div>
            
            <div class="col-md-6 mb-3">
                <h6 class="text-muted mb-2">Información de la Reserva</h6>
                <div class="card border-0 bg-light">
                    <div class="card-body">
                        <p class="mb-2"><strong>ID Reserva:</strong> ${pago.idReserva || 'N/A'}</p>
                        <p class="mb-2"><strong>Espacio:</strong> ${pago.espacioNombre || 'N/A'}</p>
                        <p class="mb-2"><strong>Lugar:</strong> ${pago.espacioLugar || 'N/A'}</p>
                        <p class="mb-2"><strong>Fecha:</strong> ${fechaReserva}</p>
                        <p class="mb-0"><strong>Horario:</strong> ${horario}</p>
                        ${pago.espacioTipo ? `<p class="mb-0 mt-2"><strong>Tipo:</strong> ${pago.espacioTipo}</p>` : ''}
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Mostrar/ocultar overlay de carga
 */
function mostrarCargando(mostrar) {
    const overlay = document.getElementById('loadingOverlay');
    if (mostrar) {
        overlay.classList.remove('d-none');
    } else {
        overlay.classList.add('d-none');
    }
}

/**
 * Mostrar toast de notificación
 */
function mostrarToast(mensaje, tipo = 'info', duracion = 3000) {
    const toastId = 'toast-' + Date.now();
    const tipoClase = {
        'success': 'bg-success',
        'error': 'bg-danger',
        'warning': 'bg-warning',
        'info': 'bg-info'
    }[tipo] || 'bg-info';

    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white ${tipoClase} border-0 position-fixed" 
             style="top: 20px; right: 20px; z-index: 9999;" role="alert">
            <div class="d-flex">
                <div class="toast-body">${mensaje}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: duracion });
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', function() {
        this.remove();
    });
}