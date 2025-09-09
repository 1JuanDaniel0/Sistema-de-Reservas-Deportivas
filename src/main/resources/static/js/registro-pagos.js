$(document).ready(function() {
    console.log('🔄 Inicializando Registro de Pagos...');

    // Variables globales
    let pagosTable;
    let espaciosData = [];

    // Inicializar componentes
    initializeDateRangePicker();
    loadEspacios();
    initializeTable();
    bindEventHandlers();
    loadStatistics();

    /**
     * Inicializar DateRangePicker
     */
    function initializeDateRangePicker() {
        $('#dateRange').daterangepicker({
            opens: 'left',
            autoUpdateInput: false,
            locale: {
                cancelLabel: 'Limpiar',
                applyLabel: 'Aplicar',
                fromLabel: 'Desde',
                toLabel: 'Hasta',
                customRangeLabel: 'Personalizado',
                weekLabel: 'S',
                daysOfWeek: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa'],
                monthNames: ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'],
                firstDay: 1
            }
        });

        $('#dateRange').on('apply.daterangepicker', function(ev, picker) {
            $(this).val(picker.startDate.format('DD/MM/YYYY') + ' - ' + picker.endDate.format('DD/MM/YYYY'));
        });

        $('#dateRange').on('cancel.daterangepicker', function(ev, picker) {
            $(this).val('');
        });
    }

    /**
     * Cargar espacios para filtros
     */
    function loadEspacios() {
        $.get('/admin/api/pagos/espacios')
            .done(function(data) {
                espaciosData = data;
                const select = $('#filterEspacio');
                select.empty().append('<option value="">Todos los espacios</option>');

                data.forEach(espacio => {
                    select.append(`<option value="${espacio.idEspacio}">${espacio.nombre}</option>`);
                });
                console.log('✅ Espacios cargados:', data.length);
            })
            .fail(function() {
                console.error('❌ Error cargando espacios');
                showToast('Error cargando espacios', 'error');
            });
    }

    /**
     * Inicializar tabla DataTable
     */
    function initializeTable() {
        pagosTable = $('#registroPagosTable').DataTable({
            ajax: {
                url: '/admin/api/pagos/registro',
                type: 'GET',
                data: function(d) {
                    return getFilterParams();
                },
                error: function(xhr, error, thrown) {
                    console.error('Error cargando datos:', error);
                    showToast('Error cargando datos de pagos', 'error');
                }
            },
            columns: [
                {
                    data: 'idReserva',
                    title: 'ID Reserva',
                    width: '80px'
                },
                {
                    data: 'fechaPago',
                    title: 'Fecha Pago',
                    render: function(data) {
                        if (!data) return '<span class="text-muted">Sin fecha</span>';
                        const fecha = new Date(data);
                        return fecha.toLocaleDateString('es-PE');
                    }
                },
                {
                    data: 'vecinoNombre',
                    title: 'Vecino',
                    render: function(data, type, row) {
                        return `
                            <div>
                                <div class="fw-medium">${data}</div>
                                <small class="text-muted">DNI: ${row.vecinoDni}</small>
                            </div>
                        `;
                    }
                },
                {
                    data: 'espacioNombre',
                    title: 'Espacio'
                },
                {
                    data: 'fecha',
                    title: 'Fecha Reserva',
                    render: function(data) {
                        const fecha = new Date(data);
                        return fecha.toLocaleDateString('es-PE');
                    }
                },
                {
                    data: 'tipoPago',
                    title: 'Tipo Pago',
                    render: function(data) {
                        const colorClass = data === 'En línea' ? 'success' : 'info';
                        return `<span class="badge bg-${colorClass}">${data}</span>`;
                    }
                },
                {
                    data: 'estadoPago',
                    title: 'Estado Pago',
                    render: function(data) {
                        let colorClass = 'secondary';
                        if (data === 'Pagado') colorClass = 'success';
                        else if (data === 'Pendiente') colorClass = 'warning';
                        else if (data === 'Anulado') colorClass = 'danger';

                        return `<span class="badge bg-${colorClass}">${data}</span>`;
                    }
                },
                {
                    data: 'costo',
                    title: 'Monto',
                    render: function(data) {
                        return `S/ ${parseFloat(data).toFixed(2)}`;
                    }
                },
                {
                    data: 'idTransaccion',
                    title: 'ID Transacción',
                    render: function(data) {
                        if (!data) return '<span class="text-muted">-</span>';
                        return `<small class="font-monospace">${data}</small>`;
                    }
                }
            ],
            order: [[1, 'desc']], // Ordenar por fecha de pago descendente
            pageLength: 25,
            responsive: true,
            language: {
                url: '//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json'
            },
            dom: '<"row"<"col-sm-12 col-md-6"l><"col-sm-12 col-md-6"f>>' +
                '<"row"<"col-sm-12"tr>>' +
                '<"row"<"col-sm-12 col-md-5"i><"col-sm-12 col-md-7"p>>',
            buttons: [
                {
                    extend: 'excel',
                    text: '<i class="bx bx-file me-1"></i>Excel',
                    className: 'btn btn-success btn-sm',
                    title: 'Registro_Pagos_' + new Date().toISOString().split('T')[0],
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7]
                    }
                },
                {
                    extend: 'pdf',
                    text: '<i class="bx bxs-file-pdf me-1"></i>PDF',
                    className: 'btn btn-danger btn-sm',
                    title: 'Registro de Pagos',
                    orientation: 'landscape',
                    pageSize: 'A4',
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7]
                    }
                }
            ]
        });

        console.log('✅ Tabla inicializada');
    }

    /**
     * Obtener parámetros de filtros
     */
    function getFilterParams() {
        const dateRange = $('#dateRange').val();
        let params = {
            estadoPago: $('#filterEstadoPago').val(),
            espacio: $('#filterEspacio').val(),
            tipoPago: $('#filterTipoPago').val()
        };

        // Parsear rango de fechas
        if (dateRange) {
            const dates = dateRange.split(' - ');
            if (dates.length === 2) {
                const startDate = moment(dates[0], 'DD/MM/YYYY').format('YYYY-MM-DD');
                const endDate = moment(dates[1], 'DD/MM/YYYY').format('YYYY-MM-DD');
                params['dateRange[start]'] = startDate;
                params['dateRange[end]'] = endDate;
            }
        }

        return params;
    }

    /**
     * Cargar estadísticas
     */
    function loadStatistics() {
        const params = getFilterParams();

        $.get('/admin/api/pagos/estadisticas', params)
            .done(function(data) {
                $('#totalPagos').text(data.total || 0);
                $('#pagosPagados').text(data.pagados || 0);
                $('#pagosPendientes').text(data.pendientes || 0);
                $('#pagosAnulados').text(data.anulados || 0);
                $('#montoTotal').text(`S/ ${(data.montoTotal || 0).toFixed(2)}`);
                console.log('✅ Estadísticas actualizadas');
            })
            .fail(function() {
                console.error('❌ Error cargando estadísticas');
            });
    }

    /**
     * Vincular eventos
     */
    function bindEventHandlers() {
        // Aplicar filtros
        $('#applyFilters').click(function() {
            console.log('🔍 Aplicando filtros...');
            pagosTable.ajax.reload();
            loadStatistics();
        });

        // Limpiar filtros
        $('#clearFilters').click(function() {
            console.log('🔄 Limpiando filtros...');
            $('#dateRange').val('');
            $('#filterEstadoPago').val('');
            $('#filterEspacio').val('');
            $('#filterTipoPago').val('');
            $('.filter-date-btn').removeClass('active');
            $('#filterAll').addClass('active');

            pagosTable.ajax.reload();
            loadStatistics();
        });

        // Botones de filtro rápido de fecha
        $('.filter-date-btn').click(function() {
            $('.filter-date-btn').removeClass('active');
            $(this).addClass('active');

            const filter = $(this).data('filter');
            let startDate, endDate;

            switch(filter) {
                case 'today':
                    startDate = moment();
                    endDate = moment();
                    break;
                case 'week':
                    startDate = moment().startOf('week');
                    endDate = moment().endOf('week');
                    break;
                case 'month':
                    startDate = moment().startOf('month');
                    endDate = moment().endOf('month');
                    break;
                case 'year':
                    startDate = moment().startOf('year');
                    endDate = moment().endOf('year');
                    break;
                case 'all':
                default:
                    $('#dateRange').val('');
                    pagosTable.ajax.reload();
                    loadStatistics();
                    return;
            }

            if (startDate && endDate) {
                $('#dateRange').val(startDate.format('DD/MM/YYYY') + ' - ' + endDate.format('DD/MM/YYYY'));
                pagosTable.ajax.reload();
                loadStatistics();
            }
        });

        // Exportar Excel
        $('#exportExcel').click(function() {
            console.log('📊 Exportando a Excel...');
            pagosTable.button('.buttons-excel').trigger();
        });

        // Exportar PDF
        $('#exportPdf').click(function() {
            console.log('📄 Exportando a PDF...');
            pagosTable.button('.buttons-pdf').trigger();
        });
    }

    /**
     * Mostrar toast de notificación
     */
    function showToast(message, type = 'info') {
        // Usar SweetAlert2 si está disponible
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                title: message,
                icon: type,
                zIndex: 999
            });
        } else {
            console.log(`${type.toUpperCase()}: ${message}`);
        }
    }

    console.log('✅ Registro de Pagos inicializado correctamente');
});