/**
 * JavaScript para la gestión de reservas deportivas - Administrador - VERSIÓN FINAL CORREGIDA
 */

$(document).ready(function() {
    'use strict';

    // Variables globales
    let reservasTable;
    let currentFilters = {
        dateRange: null,
        estadoReserva: '',
        espacio: '',
        tipoPago: ''
    };
    let isLoading = false; // Prevenir múltiples peticiones simultáneas

    // Configuración de idioma para DataTables
    const datatableLanguage = {
        "decimal": "",
        "emptyTable": "No hay reservas disponibles",
        "info": "Mostrando _START_ a _END_ de _TOTAL_ reservas",
        "infoEmpty": "Mostrando 0 a 0 de 0 reservas",
        "infoFiltered": "(filtrado de _MAX_ reservas totales)",
        "infoPostFix": "",
        "thousands": ",",
        "lengthMenu": "Mostrar _MENU_ reservas por página",
        "loadingRecords": "Cargando...",
        "processing": "Procesando...",
        "search": "Buscar:",
        "zeroRecords": "No se encontraron reservas",
        "paginate": {
            "first": "Primero",
            "last": "Último",
            "next": "Siguiente",
            "previous": "Anterior"
        },
        "aria": {
            "sortAscending": ": activar para ordenar la columna de forma ascendente",
            "sortDescending": ": activar para ordenar la columna de forma descendente"
        }
    };

    // Inicialización
    initializePage();

    function initializePage() {
        console.log('🚀 Inicializando página de reservas...');

        initializeDateRangePicker();
        initializeDataTable();
        initializeEventListeners();
        loadInitialData();
    }

    /**
     * Inicializar Date Range Picker
     */
    function initializeDateRangePicker() {
        $('#dateRange').daterangepicker({
            locale: {
                format: 'DD/MM/YYYY',
                separator: ' - ',
                applyLabel: 'Aplicar',
                cancelLabel: 'Cancelar',
                fromLabel: 'Desde',
                toLabel: 'Hasta',
                customRangeLabel: 'Personalizado',
                daysOfWeek: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa'],
                monthNames: ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
                firstDay: 1
            },
            ranges: {
                'Hoy': [moment(), moment()],
                'Ayer': [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
                'Últimos 7 días': [moment().subtract(6, 'days'), moment()],
                'Últimos 30 días': [moment().subtract(29, 'days'), moment()],
                'Este mes': [moment().startOf('month'), moment().endOf('month')],
                'Mes pasado': [moment().subtract(1, 'month').startOf('month'),
                    moment().subtract(1, 'month').endOf('month')]
            },
            opens: 'left',
            drops: 'down',
            autoUpdateInput: false
        });

        $('#dateRange').on('apply.daterangepicker', function(ev, picker) {
            $(this).val(picker.startDate.format('DD/MM/YYYY') + ' - ' + picker.endDate.format('DD/MM/YYYY'));
            currentFilters.dateRange = {
                start: picker.startDate.format('YYYY-MM-DD'),
                end: picker.endDate.format('YYYY-MM-DD')
            };
        });

        $('#dateRange').on('cancel.daterangepicker', function(ev, picker) {
            $(this).val('');
            currentFilters.dateRange = null;
        });
    }

    /**
     * Inicializar DataTable - CORREGIDO
     */
    function initializeDataTable() {
        console.log('📊 Inicializando DataTable...');

        reservasTable = $('#reservasTable').DataTable({
            responsive: true,
            processing: false, // Desactivar processing automático
            serverSide: false,
            language: datatableLanguage,
            pageLength: 25,
            lengthMenu: [[10, 25, 50, 100, -1], [10, 25, 50, 100, "Todos"]],
            order: [[1, 'desc']], // Ordenar por fecha/hora descendente
            data: [], // Inicializar con array vacío
            columns: [
                {
                    title: "ID",
                    data: "idReserva",
                    width: "60px",
                    className: "text-center"
                },
                {
                    title: "Fecha/Hora",
                    data: null,
                    width: "150px",
                    render: function(data, type, row) {
                        if (type === 'display') {
                            const fecha = moment(row.fecha).format('DD/MM/YYYY');
                            const horaInicio = row.horaInicio;
                            const horaFin = row.horaFin;
                            return `<div class="fw-semibold">${fecha}</div>
                                   <small class="text-muted">${horaInicio} - ${horaFin}</small>`;
                        }
                        return row.fecha;
                    }
                },
                {
                    title: "Vecino",
                    data: null,
                    width: "180px",
                    render: function(data, type, row) {
                        if (type === 'display') {
                            return `<div class="fw-semibold">${row.vecinoNombre}</div>
                                   <small class="text-muted">DNI: ${row.vecinoDni || 'N/A'}</small>`;
                        }
                        return row.vecinoNombre;
                    }
                },
                {
                    title: "Espacio",
                    data: null,
                    width: "150px",
                    render: function(data, type, row) {
                        if (type === 'display') {
                            return `<div class="fw-semibold">${row.espacioNombre}</div>
                                   <small class="text-muted">${row.espacioTipo || ''}</small>`;
                        }
                        return row.espacioNombre;
                    }
                },
                {
                    title: "Estado",
                    data: "estado",
                    width: "120px",
                    className: "text-center",
                    render: function(data, type, row) {
                        if (type === 'display') {
                            const estadoClass = getEstadoClass(row.estado);
                            return `<span class="badge ${estadoClass}">${row.estado}</span>`;
                        }
                        return data;
                    }
                },
                {
                    title: "Costo",
                    data: "costo",
                    width: "100px",
                    className: "text-end",
                    render: function(data, type, row) {
                        if (type === 'display') {
                            return `<span class="fw-semibold">S/ ${parseFloat(row.costo).toFixed(2)}</span>`;
                        }
                        return data;
                    }
                },
                {
                    title: "Tipo Pago",
                    data: "tipoPago",
                    width: "100px",
                    className: "text-center"
                },
                {
                    title: "Coordinador",
                    data: null,
                    width: "150px",
                    render: function(data, type, row) {
                        if (type === 'display' && row.coordinadorNombre) {
                            return `<small>${row.coordinadorNombre}</small>`;
                        }
                        return '<small class="text-muted">No asignado</small>';
                    }
                },
                {
                    title: "Acciones",
                    data: null,
                    width: "120px",
                    className: "text-center",
                    orderable: false,
                    render: function(data, type, row) {
                        if (type === 'display') {
                            return `
                                <button class="btn btn-sm btn-outline-primary btn-action" 
                                        onclick="verDetalles(${row.idReserva})" 
                                        title="Ver detalles">
                                    <i class="bx bx-show"></i>
                                </button>`;
                        }
                        return '';
                    }
                }
            ],
            dom: '<"row"<"col-sm-6"l><"col-sm-6"f>>rtip',
            buttons: [
                {
                    extend: 'excel',
                    text: '<i class="bx bx-file me-1"></i>Excel',
                    className: 'btn btn-success btn-sm',
                    filename: function() {
                        return 'reservas_deportivas_' + moment().format('YYYY-MM-DD_HH-mm');
                    },
                    title: 'Reporte de Reservas Deportivas',
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7] // Excluir columna de acciones
                    }
                },
                {
                    extend: 'pdf',
                    text: '<i class="bx bxs-file-pdf me-1"></i>PDF',
                    className: 'btn btn-danger btn-sm',
                    filename: function() {
                        return 'reservas_deportivas_' + moment().format('YYYY-MM-DD_HH-mm');
                    },
                    title: 'Reporte de Reservas Deportivas',
                    orientation: 'landscape',
                    pageSize: 'A4',
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7] // Excluir columna de acciones
                    }
                }
            ]
        });

        console.log('✅ DataTable inicializado correctamente');
    }

    /**
     * Inicializar eventos
     */
    function initializeEventListeners() {
        // Filtros de fecha rápidos
        $('.filter-date-btn').on('click', function() {
            if (isLoading) return; // Prevenir clicks durante carga

            $('.filter-date-btn').removeClass('active');
            $(this).addClass('active');

            const filter = $(this).data('filter');
            applyQuickDateFilter(filter);
        });

        // Aplicar filtros
        $('#applyFilters').on('click', function() {
            if (isLoading) return;
            applyFilters();
        });

        // Limpiar filtros
        $('#clearFilters').on('click', function() {
            if (isLoading) return;
            clearAllFilters();
        });

        // Cambios en los filtros
        $('#filterEstado, #filterEspacio, #filterTipoPago').on('change', function() {
            updateCurrentFilters();
        });

        // Exportar
        $('#exportExcel').on('click', function() {
            reservasTable.button(0).trigger();
        });

        $('#exportPdf').on('click', function() {
            reservasTable.button(1).trigger();
        });
    }

    /**
     * Aplicar filtro de fecha rápido
     */
    function applyQuickDateFilter(filter) {
        if (isLoading) return;

        let startDate, endDate;

        switch(filter) {
            case 'today':
                startDate = endDate = moment().format('YYYY-MM-DD');
                $('#dateRange').val(moment().format('DD/MM/YYYY'));
                break;
            case 'week':
                startDate = moment().startOf('isoWeek').format('YYYY-MM-DD');
                endDate = moment().endOf('isoWeek').format('YYYY-MM-DD');
                $('#dateRange').val(moment().startOf('isoWeek').format('DD/MM/YYYY') + ' - ' +
                    moment().endOf('isoWeek').format('DD/MM/YYYY'));
                break;
            case 'month':
                startDate = moment().startOf('month').format('YYYY-MM-DD');
                endDate = moment().endOf('month').format('YYYY-MM-DD');
                $('#dateRange').val(moment().startOf('month').format('DD/MM/YYYY') + ' - ' +
                    moment().endOf('month').format('DD/MM/YYYY'));
                break;
            case 'year':
                startDate = moment().startOf('year').format('YYYY-MM-DD');
                endDate = moment().endOf('year').format('YYYY-MM-DD');
                $('#dateRange').val(moment().startOf('year').format('DD/MM/YYYY') + ' - ' +
                    moment().endOf('year').format('DD/MM/YYYY'));
                break;
            case 'all':
                startDate = endDate = null;
                $('#dateRange').val('');
                break;
        }

        currentFilters.dateRange = startDate ? { start: startDate, end: endDate } : null;

        // Pequeño delay para evitar múltiples peticiones
        setTimeout(() => {
            applyFilters();
        }, 100);
    }

    /**
     * Actualizar filtros actuales
     */
    function updateCurrentFilters() {
        currentFilters.estadoReserva = $('#filterEstado').val();
        currentFilters.espacio = $('#filterEspacio').val();
        currentFilters.tipoPago = $('#filterTipoPago').val();
    }

    /**
     * Aplicar todos los filtros
     */
    function applyFilters() {
        if (isLoading) {
            console.log('⏳ Ya hay una petición en curso, ignorando...');
            return;
        }

        updateCurrentFilters();
        loadReservasData();
        updateStatistics();
    }

    /**
     * Limpiar todos los filtros
     */
    function clearAllFilters() {
        if (isLoading) return;

        $('#dateRange').val('');
        $('#filterEstado').val('');
        $('#filterEspacio').val('');
        $('#filterTipoPago').val('');
        $('.filter-date-btn').removeClass('active');

        currentFilters = {
            dateRange: null,
            estadoReserva: '',
            espacio: '',
            tipoPago: ''
        };

        loadReservasData();
        updateStatistics();
    }

    /**
     * Cargar datos iniciales - SIMPLIFICADO
     */
    function loadInitialData() {
        console.log('📋 Cargando datos iniciales...');

        // Cargar filtros primero
        Promise.all([
            loadEspaciosFilter(),
            loadEstadosReservaFilter()
        ]).then(() => {
            console.log('✅ Filtros cargados, aplicando filtro "Hoy"...');
            // Solo después de cargar los filtros, aplicar "Hoy"
            setTodayFilter();
        }).catch(error => {
            console.error('❌ Error cargando filtros:', error);
            // Si hay error, cargar todas las reservas
            loadReservasData();
        });
    }

    /**
     * Establecer filtro "Hoy" por defecto
     */
    function setTodayFilter() {
        $('.filter-date-btn[data-filter="today"]').addClass('active');
        applyQuickDateFilter('today');
    }

    /**
     * Cargar espacios para el filtro
     */
    function loadEspaciosFilter() {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/admin/api/espacios',
                method: 'GET',
                success: function(espacios) {
                    const select = $('#filterEspacio');
                    select.empty().append('<option value="">Todos los espacios</option>');

                    espacios.forEach(function(espacio) {
                        select.append(`<option value="${espacio.idEspacio}">${espacio.nombre}</option>`);
                    });

                    console.log('✅ Espacios cargados:', espacios.length);
                    resolve();
                },
                error: function(error) {
                    console.error('❌ Error al cargar los espacios:', error);
                    reject(error);
                }
            });
        });
    }

    /**
     * Carga opciones para el filtro de Estado de la Reserva
     */
    function loadEstadosReservaFilter() {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/admin/api/estados-reserva',
                method: 'GET',
                success: function(estadosReserva) {
                    const select = $('#filterEstado');
                    select.empty().append('<option value="">Todos los estados</option>');

                    estadosReserva.forEach(function(estadoReserva) {
                        select.append(`<option value="${estadoReserva.idEstadoReserva}">${estadoReserva.estado}</option>`);
                    });

                    console.log('✅ Estados de reserva cargados:', estadosReserva.length);
                    resolve();
                },
                error: function(error) {
                    console.error('❌ Error al cargar los estados de reserva:', error);
                    reject(error);
                }
            });
        });
    }

    /**
     * Cargar datos de reservas
     */
    function loadReservasData() {
        if (isLoading) {
            console.log('⏳ Ya hay una petición de reservas en curso...');
            return;
        }

        isLoading = true;
        console.log('🔄 Cargando reservas...');

        // Preparar parámetros para la petición AJAX
        let requestData = {};

        if (currentFilters.dateRange) {
            requestData['dateRange[start]'] = currentFilters.dateRange.start;
            requestData['dateRange[end]'] = currentFilters.dateRange.end;
        }

        if (currentFilters.estadoReserva) {
            requestData.estadoReserva = currentFilters.estadoReserva;
        }

        if (currentFilters.espacio) {
            requestData.espacio = currentFilters.espacio;
        }

        if (currentFilters.tipoPago) {
            requestData.tipoPago = currentFilters.tipoPago;
        }

        console.log('📤 Enviando parámetros:', requestData);

        $.ajax({
            url: '/admin/api/reservas',
            method: 'GET',
            data: requestData,
            timeout: 30000, // 30 segundos timeout
            success: function(response) {
                console.log('📥 Respuesta recibida:', response);

                try {
                    // Limpiar y cargar nuevos datos
                    reservasTable.clear();

                    if (response.data && response.data.length > 0) {
                        console.log(`✅ Cargando ${response.data.length} reservas`);
                        console.log('🔍 Primera reserva:', response.data[0]);

                        // Validar que todos los registros tengan los campos necesarios
                        const validData = response.data.filter(row => {
                            return row.idReserva !== undefined && row.idReserva !== null;
                        });

                        if (validData.length !== response.data.length) {
                            console.warn(`⚠️ ${response.data.length - validData.length} registros inválidos filtrados`);
                        }

                        reservasTable.rows.add(validData);
                    } else {
                        console.log('ℹ️ No se encontraron reservas');
                    }

                    reservasTable.draw();
                    console.log('✅ Tabla actualizada correctamente');

                } catch (error) {
                    console.error('❌ Error procesando datos:', error);
                    Swal.fire('Error', 'Error al procesar los datos de reservas', 'error');
                }
            },
            error: function(xhr, status, error) {
                console.error('❌ Error AJAX:', { status: xhr.status, error, responseText: xhr.responseText });

                if (xhr.status === 401) {
                    Swal.fire({
                        title: 'Sesión expirada',
                        text: 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.',
                        icon: 'warning',
                        confirmButtonText: 'Ir al login'
                    }).then(() => {
                        window.location.href = '/login';
                    });
                } else {
                    Swal.fire('Error', 'Error al cargar las reservas. Por favor, recarga la página.', 'error');
                }
            },
            complete: function() {
                isLoading = false;
                console.log('🏁 Petición de reservas completada');
            }
        });
    }

    /**
     * Actualizar estadísticas
     */
    function updateStatistics() {
        let requestData = {};

        if (currentFilters.dateRange) {
            requestData['dateRange[start]'] = currentFilters.dateRange.start;
            requestData['dateRange[end]'] = currentFilters.dateRange.end;
        }

        if (currentFilters.estadoReserva) {
            requestData.estadoReserva = currentFilters.estadoReserva;
        }

        if (currentFilters.espacio) {
            requestData.espacio = currentFilters.espacio;
        }

        if (currentFilters.tipoPago) {
            requestData.tipoPago = currentFilters.tipoPago;
        }

        $.ajax({
            url: '/admin/api/reservas/estadisticas',
            method: 'GET',
            data: requestData,
            success: function(stats) {
                $('#totalReservas').text(stats.total || 0);
                $('#reservasConfirmadas').text(stats.confirmadas || 0);
                $('#reservasPendientes').text(stats.pendientes || 0);
                $('#totalIngresos').text('S/ ' + (stats.ingresos || 0).toFixed(2));
            },
            error: function() {
                console.error('❌ Error al cargar estadísticas');
            }
        });
    }

    /**
     * Obtener clase CSS para el estado
     */
    function getEstadoClass(estado) {
        switch(estado) {
            case 'Confirmada':
                return 'badge-estado estado-confirmada bg-success';
            case 'No confirmada':
                return 'badge-estado estado-pendiente bg-warning';
            case 'Cancelada':
                return 'badge-estado estado-cancelada bg-danger';
            case 'Cancelada reembolso':
                return 'badge-estado estado-cancelada bg-danger';
            case 'Pasada':
            case 'Finalizada':
                return 'badge-estado estado-finalizada bg-secondary';
            case 'Solicitud cancelar':
                return 'badge-estado estado-solicitud bg-info';
            default:
                return 'badge-estado bg-light text-dark';
        }
    }

    /**
     * Funciones globales para acciones de tabla
     */
    window.verDetalles = function(idReserva) {
        $.ajax({
            url: `/admin/api/reservas/${idReserva}`,
            method: 'GET',
            success: function(reserva) {
                mostrarDetallesModal(reserva);
            },
            error: function() {
                Swal.fire('Error', 'No se pudieron cargar los detalles de la reserva', 'error');
            }
        });
    };

    window.cancelarReserva = function(idReserva) {
        Swal.fire({
            title: '¿Cancelar reserva?',
            text: 'Esta acción no se puede deshacer',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, cancelar',
            cancelButtonText: 'No'
        }).then((result) => {
            if (result.isConfirmed) {
                $.ajax({
                    url: `/admin/api/reservas/${idReserva}/cancelar`,
                    method: 'POST',
                    success: function() {
                        Swal.fire('Cancelada', 'La reserva ha sido cancelada', 'success');
                        loadReservasData();
                        updateStatistics();
                    },
                    error: function() {
                        Swal.fire('Error', 'No se pudo cancelar la reserva', 'error');
                    }
                });
            }
        });
    };

    /**
     * Mostrar modal con detalles de la reserva
     */
    function mostrarDetallesModal(reserva) {
        const modalContent = `
            <div class="row">
                <div class="col-md-6">
                    <div class="info-card">
                        <div class="label">ID de Reserva</div>
                        <div class="value">#${reserva.idReserva}</div>
                    </div>
                    <div class="info-card">
                        <div class="label">Fecha y Hora</div>
                        <div class="value">${moment(reserva.fecha).format('DD/MM/YYYY')}<br>
                                          ${reserva.horaInicio} - ${reserva.horaFin}</div>
                    </div>
                    <div class="info-card">
                        <div class="label">Estado</div>
                        <div class="value">
                            <span class="badge ${getEstadoClass(reserva.estado)}">${reserva.estado}</span>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="info-card">
                        <div class="label">Vecino</div>
                        <div class="value">${reserva.vecinoNombre}<br>
                                          <small>DNI: ${reserva.vecinoDni || 'N/A'}</small><br>
                                          <small>Email: ${reserva.vecinoEmail || 'N/A'}</small></div>
                    </div>
                    <div class="info-card">
                        <div class="label">Espacio Deportivo</div>
                        <div class="value">${reserva.espacioNombre}<br>
                                          <small>${reserva.espacioTipo || ''}</small></div>
                    </div>
                    <div class="info-card">
                        <div class="label">Costo</div>
                        <div class="value">S/ ${parseFloat(reserva.costo).toFixed(2)}</div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-12">
                    <div class="info-card">
                        <div class="label">Información de Pago</div>
                        <div class="value">
                            Tipo: ${reserva.tipoPago || 'N/A'}<br>
                            Estado: ${reserva.estadoPago || 'N/A'}<br>
                            ${reserva.fechaPago ? `Fecha: ${moment(reserva.fechaPago).format('DD/MM/YYYY HH:mm')}` : ''}
                        </div>
                    </div>
                </div>
            </div>
            ${reserva.observaciones ? `
                <div class="row">
                    <div class="col-12">
                        <div class="info-card">
                            <div class="label">Observaciones</div>
                            <div class="value">${reserva.observaciones}</div>
                        </div>
                    </div>
                </div>
            ` : ''}
        `;

        $('#reservaDetalleContent').html(modalContent);
        $('#reservaDetalleModal').modal('show');
    }
});