

$(document).ready(function() {
    console.log('📊 Inicializando sistema de historial de reembolsos...');

    // Variables globales
    let historialTable;
    let filtrosActivos = {
        dateRange: null,
        estadoReembolso: null,
        espacio: null,
        tipoPago: null
    };

    // Inicialización
    inicializarComponentes();
    cargarDatosIniciales();
    configurarEventos();

    /**
     * Inicializar todos los componentes
     */
    function inicializarComponentes() {
        console.log('⚙️ Inicializando componentes...');

        // Inicializar DataTable
        inicializarDataTable();

        // Inicializar DateRangePicker
        inicializarDateRangePicker();

        // Aplicar filtro inicial por defecto (todo el tiempo)
        aplicarFiltroFecha('all');

        console.log('✅ Componentes inicializados');
    }

    /**
     * Cargar datos iniciales
     */
    function cargarDatosIniciales() {
        console.log('📊 Cargando datos iniciales...');

        // Cargar filtros
        cargarEspacios();

        // Cargar estadísticas
        cargarEstadisticas();

        console.log('✅ Datos iniciales cargados');
    }

    /**
     * Configurar eventos
     */
    function configurarEventos() {
        console.log('🔗 Configurando eventos...');

        // Filtros de fecha rápidos
        $('.filter-date-btn').click(function() {
            const filtro = $(this).data('filter');
            aplicarFiltroFecha(filtro);

            // Actualizar botones activos
            $('.filter-date-btn').removeClass('active');
            $(this).addClass('active');
        });

        // Botón aplicar filtros
        $('#applyFilters').click(function() {
            aplicarFiltros();
        });

        // Botón limpiar filtros
        $('#clearFilters').click(function() {
            limpiarFiltros();
        });

        // Exportar datos
        $('#exportPdf').click(function() {
            exportarDatos('pdf');
        });

        $('#exportExcel').click(function() {
            exportarDatos('excel');
        });

        console.log('✅ Eventos configurados');
    }

    /**
     * Inicializar DataTable
     */
    function inicializarDataTable() {
        console.log('📋 Inicializando DataTable...');

        historialTable = $('#historialReembolsosTable').DataTable({
            processing: true,
            serverSide: false,
            responsive: true,
            pageLength: 25,
            order: [[1, 'desc']], // Ordenar por fecha de solicitud descendente
            language: {
                url: 'https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json'
            },
            dom: '<"row"<"col-sm-6"l><"col-sm-6"f>>rtip',
            columns: [
                {
                    data: 'idSolicitud',
                    title: 'ID',
                    width: '60px',
                    render: function(data) {
                        return `#${data}`;
                    }
                },
                {
                    data: 'fechaSolicitud',
                    title: 'Fecha Solicitud',
                    render: function(data) {
                        const fecha = new Date(data);
                        return fecha.toLocaleDateString('es-PE') + '<br>' +
                            '<small class="text-muted">' + fecha.toLocaleTimeString('es-PE', {
                                hour: '2-digit',
                                minute: '2-digit'
                            }) + '</small>';
                    }
                },
                {
                    data: 'vecinoNombre',
                    title: 'Vecino',
                    render: function(data, type, row) {
                        return `
                            <div>
                                <strong>${data}</strong><br>
                                <small class="text-muted">DNI: ${row.vecinoDni}</small>
                            </div>
                        `;
                    }
                },
                {
                    data: 'espacioNombre',
                    title: 'Espacio',
                    render: function(data, type, row) {
                        return `<strong>${data}</strong>`;
                    }
                },
                {
                    data: 'fecha',
                    title: 'Reserva',
                    render: function(data, type, row) {
                        const fecha = new Date(data).toLocaleDateString('es-PE');
                        return `
                            <div>
                                <strong>${fecha}</strong><br>
                                <small class="text-muted">${row.horaInicio} - ${row.horaFin}</small>
                            </div>
                        `;
                    }
                },
                {
                    data: 'estado',
                    title: 'Estado',
                    render: function(data, type, row) {
                        const badgeClass = {
                            'PENDIENTE': 'badge-pendiente',
                            'COMPLETADO': 'badge-completado',
                            'FALLIDO': 'badge-rechazado'
                        };
                        const descripcion = row.estadoDescripcion || data;
                        return `<span class="badge ${badgeClass[data] || 'badge-normal'}">${descripcion}</span>`;
                    }
                },
                {
                    data: 'metodoReembolso',
                    title: 'Método',
                    render: function(data, type, row) {
                        if (!data || data === 'null') return '<span class="text-muted">-</span>';

                        const iconos = {
                            'MERCADOPAGO_AUTOMATICO': '<i class="bx bx-credit-card text-success me-1"></i>',
                            'DEPOSITO_MANUAL': '<i class="bx bx-bank text-primary me-1"></i>',
                            'TRANSFERENCIA_MANUAL': '<i class="bx bx-transfer text-info me-1"></i>'
                        };

                        const descripcion = row.metodoDescripcion || data;
                        const icono = iconos[data] || '<i class="bx bx-help-circle text-muted me-1"></i>';

                        return `${icono}<small>${descripcion}</small>`;
                    }
                },
                {
                    data: 'montoReembolso',
                    title: 'Monto',
                    render: function(data) {
                        return `S/ ${parseFloat(data || 0).toFixed(2)}`;
                    }
                },
                {
                    data: 'fechaProcesamiento',
                    title: 'Fecha Procesamiento',
                    render: function(data, type, row) {
                        if (!data) {
                            // Si no hay fecha de procesamiento, mostrar estado
                            if (row.estado === 'PENDIENTE') {
                                return '<span class="text-warning"><i class="bx bx-time me-1"></i>Pendiente</span>';
                            }
                            return '<span class="text-muted">-</span>';
                        }
                        const fecha = new Date(data);
                        return fecha.toLocaleDateString('es-PE') + '<br>' +
                            '<small class="text-muted">' + fecha.toLocaleTimeString('es-PE', {
                                hour: '2-digit',
                                minute: '2-digit'
                            }) + '</small>';
                    }
                },
                {
                    data: null,
                    title: 'Acciones',
                    orderable: false,
                    searchable: false,
                    render: function(data, type, row) {
                        return `
                            <button class="btn btn-accion btn-ver-detalle" 
                                    onclick="verDetalleSolicitud(${row.idSolicitud})"
                                    title="Ver detalles históricos">
                                <i class="bx bx-show"></i>
                            </button>
                        `;
                    }
                }
            ],
            ajax: {
                url: '/admin/api/reembolsos/historial',
                type: 'GET',
                data: function(d) {
                    // Añadir filtros a la petición
                    if (filtrosActivos.dateRange) {
                        d['dateRange[start]'] = filtrosActivos.dateRange.start;
                        d['dateRange[end]'] = filtrosActivos.dateRange.end;
                    }
                    if (filtrosActivos.estadoReembolso) {
                        d.estadoReembolso = filtrosActivos.estadoReembolso;
                    }
                    if (filtrosActivos.espacio) {
                        d.espacio = filtrosActivos.espacio;
                    }
                    if (filtrosActivos.tipoPago) {
                        d.tipoPago = filtrosActivos.tipoPago;
                    }

                    console.log('📡 Enviando petición con filtros:', d);
                    return d;
                },
                dataSrc: function(json) {
                    console.log('📥 Datos recibidos:', json.data.length, 'reembolsos históricos');
                    return json.data;
                }
            },
            drawCallback: function() {
                // Configurar tooltips después de dibujar la tabla
                $('[data-bs-toggle="tooltip"]').tooltip();
            }
        });

        console.log('✅ DataTable inicializado');
    }

    /**
     * Inicializar DateRangePicker
     */
    function inicializarDateRangePicker() {
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
            autoUpdateInput: false
        });

        $('#dateRange').on('apply.daterangepicker', function(ev, picker) {
            $(this).val(picker.startDate.format('DD/MM/YYYY') + ' - ' + picker.endDate.format('DD/MM/YYYY'));
            filtrosActivos.dateRange = {
                start: picker.startDate.format('YYYY-MM-DD'),
                end: picker.endDate.format('YYYY-MM-DD')
            };
        });

        $('#dateRange').on('cancel.daterangepicker', function() {
            $(this).val('');
            filtrosActivos.dateRange = null;
        });
    }

    /**
     * Aplicar filtro de fecha rápido
     */
    function aplicarFiltroFecha(filtro) {
        const hoy = moment();
        let inicio, fin;

        switch(filtro) {
            case 'today':
                inicio = fin = hoy.clone();
                break;
            case 'week':
                inicio = hoy.clone().startOf('week');
                fin = hoy.clone().endOf('week');
                break;
            case 'month':
                inicio = hoy.clone().startOf('month');
                fin = hoy.clone().endOf('month');
                break;
            case 'year':
                inicio = hoy.clone().startOf('year');
                fin = hoy.clone().endOf('year');
                break;
            case 'all':
                inicio = fin = null;
                break;
        }

        if (inicio && fin) {
            filtrosActivos.dateRange = {
                start: inicio.format('YYYY-MM-DD'),
                end: fin.format('YYYY-MM-DD')
            };
            $('#dateRange').val(inicio.format('DD/MM/YYYY') + ' - ' + fin.format('DD/MM/YYYY'));
        } else {
            filtrosActivos.dateRange = null;
            $('#dateRange').val('');
        }

        // Recargar tabla
        historialTable.ajax.reload();

        console.log('📅 Filtro de fecha aplicado:', filtro, filtrosActivos.dateRange);
    }

    /**
     * Cargar espacios para filtro
     */
    function cargarEspacios() {
        $.get('/admin/api/espacios')
            .done(function(data) {
                const select = $('#filterEspacio');
                select.empty().append('<option value="">Todos los espacios</option>');

                data.forEach(function(espacio) {
                    select.append(
                        `<option value="${espacio.idEspacio}">${espacio.nombre} (${espacio.tipo})</option>`
                    );
                });

                console.log('✅ Espacios cargados:', data.length);
            })
            .fail(function() {
                console.error('❌ Error cargando espacios');
                mostrarError('Error cargando lista de espacios');
            });
    }

    /**
     * Cargar estadísticas del historial (SIMPLIFICADO)
     */
    function cargarEstadisticas() {
        const params = new URLSearchParams();

        if (filtrosActivos.dateRange) {
            params.append('dateRange[start]', filtrosActivos.dateRange.start);
            params.append('dateRange[end]', filtrosActivos.dateRange.end);
        }

        $.get('/admin/api/reembolsos/historial/estadisticas?' + params.toString())
            .done(function(data) {
                $('#totalReservas').text(data.total || 0);
                $('#reservasConfirmadas').text(data.completados || 0);
                $('#reservasPendientes').text(data.fallidos || 0); // Cambiamos a fallidos en lugar de rechazados
                $('#totalIngresos').text('S/ ' + (data.montoTotal || 0).toFixed(2));

                console.log('📊 Estadísticas del historial actualizadas:', data);

                // Log adicional para debugging
                if (data.estadisticasPorMetodo) {
                    console.log('📈 Estadísticas por método:', data.estadisticasPorMetodo);
                }
            })
            .fail(function() {
                console.error('❌ Error cargando estadísticas del historial');
                mostrarError('Error cargando estadísticas');
            });
    }

    /**
     * Aplicar filtros
     */
    function aplicarFiltros() {
        // Actualizar filtros activos
        filtrosActivos.estadoReembolso = $('#filterEstadoReembolso').val() || null;
        filtrosActivos.espacio = $('#filterEspacio').val() || null;
        filtrosActivos.tipoPago = $('#filterTipoPago').val() || null;

        // Recargar tabla y estadísticas
        historialTable.ajax.reload();
        cargarEstadisticas();

        console.log('🔍 Filtros aplicados:', filtrosActivos);
        mostrarExito('Filtros aplicados correctamente');
    }

    /**
     * Limpiar filtros
     */
    function limpiarFiltros() {
        // Limpiar controles
        $('#dateRange').val('');
        $('#filterEstadoReembolso').val('');
        $('#filterEspacio').val('');
        $('#filterTipoPago').val('');

        // Limpiar filtros activos
        filtrosActivos = {
            dateRange: null,
            estadoReembolso: null,
            espacio: null,
            tipoPago: null
        };

        // Reactivar filtro "todo el tiempo"
        $('.filter-date-btn').removeClass('active');
        $('.filter-date-btn[data-filter="all"]').addClass('active');
        aplicarFiltroFecha('all');

        console.log('🧹 Filtros limpiados');
        mostrarInfo('Filtros restablecidos');
    }

    /**
     * Ver detalle de solicitud histórica
     */
    window.verDetalleSolicitud = function(idSolicitud) {
        console.log('👁️ Viendo detalle histórico de solicitud:', idSolicitud);

        $.get(`/admin/api/reembolsos/historial/${idSolicitud}`)
            .done(function(data) {
                mostrarModalDetalle(data);
            })
            .fail(function() {
                console.error('❌ Error cargando detalle de solicitud histórica');
                mostrarError('Error cargando detalles de la solicitud');
            });
    };

    /**
     * Mostrar modal con detalles históricos
     */
    function mostrarModalDetalle(data) {
        const modal = $('#reservaDetalleModal');
        const contenido = $('#reservaDetalleContent');

        let estadoBadge = '';
        const badgeClass = {
            'Pendiente': 'badge-pendiente',
            'Aprobado': 'badge-aprobado',
            'Completado': 'badge-completado',
            'Rechazado': 'badge-rechazado'
        };
        estadoBadge = `<span class="badge ${badgeClass[data.estado] || 'badge-normal'}">${data.estado}</span>`;

        let comprobanteHtml = '';
        if (data.comprobanteUrlPreFirmada) {
            comprobanteHtml = `
                <div class="comprobante-container">
                    <h6>Comprobante de Pago</h6>
                    <img src="${data.comprobanteUrlPreFirmada}" 
                         class="comprobante-image" 
                         alt="Comprobante de pago"
                         onclick="window.open('${data.comprobanteUrlPreFirmada}', '_blank')">
                    <br><small class="text-muted">Haga clic para ver en tamaño completo</small>
                </div>
            `;
        } else {
            comprobanteHtml = '<div class="no-comprobante">No hay comprobante disponible</div>';
        }

        let respuestaHtml = '';
        if (data.tiempoRespuesta) {
            const iconoEstado = data.estado === 'Completado' ? 'bx-check-circle text-success' :
                data.estado === 'Rechazado' ? 'bx-x-circle text-danger' :
                    'bx-time text-warning';

            respuestaHtml = `
                <div class="info-section border-start-info">
                    <h6><i class="bx ${iconoEstado} me-2"></i>Respuesta del Administrador</h6>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-label">Fecha de Procesamiento:</div>
                            <div class="info-value">${new Date(data.tiempoRespuesta).toLocaleString('es-PE')}</div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-label">Estado Final:</div>
                            <div class="info-value">${estadoBadge}</div>
                        </div>
                        <div class="col-12 mt-2">
                            <div class="info-label">Observaciones:</div>
                            <div class="info-value">${data.motivoRespuesta || 'Sin observaciones registradas'}</div>
                        </div>
                    </div>
                </div>
            `;
        }

        // Información de procesamiento adicional para reembolsos completados
        let procesamientoHtml = '';
        if (data.estado === 'Completado' && data.tipoPago === 'En banco') {
            procesamientoHtml = `
                <div class="info-section border-start-success">
                    <h6><i class="bx bx-money me-2"></i>Información de Reembolso</h6>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-label">Método de Reembolso:</div>
                            <div class="info-value">Depósito Bancario Manual</div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-label">Monto Reembolsado:</div>
                            <div class="info-value text-success fw-bold">S/ ${parseFloat(data.costo).toFixed(2)}</div>
                        </div>
                    </div>
                </div>
            `;
        } else if (data.estado === 'Completado' && data.tipoPago === 'En línea') {
            procesamientoHtml = `
                <div class="info-section border-start-success">
                    <h6><i class="bx bx-credit-card me-2"></i>Información de Reembolso</h6>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-label">Método de Reembolso:</div>
                            <div class="info-value">Reembolso Automático MercadoPago</div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-label">Monto Reembolsado:</div>
                            <div class="info-value text-success fw-bold">S/ ${parseFloat(data.costo).toFixed(2)}</div>
                        </div>
                    </div>
                </div>
            `;
        }

        contenido.html(`
            <div class="row">
                <div class="col-md-6">
                    <div class="info-section border-start-primary">
                        <h6><i class="bx bx-info-circle me-2"></i>Información de la Solicitud</h6>
                        <div class="row">
                            <div class="col-6">
                                <div class="info-label">ID Solicitud:</div>
                                <div class="info-value">#${data.idSolicitud}</div>
                            </div>
                            <div class="col-6">
                                <div class="info-label">ID Reembolso:</div>
                                <div class="info-value">#${data.idReembolso}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Estado Final:</div>
                                <div class="info-value">${estadoBadge}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Tipo de Pago:</div>
                                <div class="info-value">${data.tipoPago}</div>
                            </div>
                            <div class="col-12 mt-2">
                                <div class="info-label">Fecha de Solicitud:</div>
                                <div class="info-value">${new Date(data.fechaSolicitud).toLocaleString('es-PE')}</div>
                            </div>
                            ${data.fechaProcesamiento ? `
                                <div class="col-12 mt-2">
                                    <div class="info-label">Fecha de Procesamiento:</div>
                                    <div class="info-value">${new Date(data.fechaProcesamiento).toLocaleString('es-PE')}</div>
                                </div>
                            ` : ''}
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="info-section border-start-success">
                        <h6><i class="bx bx-user me-2"></i>Información del Vecino</h6>
                        <div class="row">
                            <div class="col-12">
                                <div class="info-label">Nombre Completo:</div>
                                <div class="info-value">${data.vecinoNombre}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">DNI:</div>
                                <div class="info-value">${data.vecinoDni}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Email:</div>
                                <div class="info-value">${data.vecinoEmail}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6">
                    <div class="info-section border-start-warning">
                        <h6><i class="bx bx-calendar me-2"></i>Detalles de la Reserva</h6>
                        <div class="row">
                            <div class="col-6">
                                <div class="info-label">ID Reserva:</div>
                                <div class="info-value">#${data.idReserva}</div>
                            </div>
                            <div class="col-6">
                                <div class="info-label">Fecha:</div>
                                <div class="info-value">${new Date(data.fecha).toLocaleDateString('es-PE')}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Hora Inicio:</div>
                                <div class="info-value">${data.horaInicio}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Hora Fin:</div>
                                <div class="info-value">${data.horaFin}</div>
                            </div>
                            <div class="col-12 mt-2">
                                <div class="info-label">Espacio:</div>
                                <div class="info-value">${data.espacioNombre}</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Monto Reembolsado:</div>
                                <div class="info-value text-success fw-bold">S/ ${parseFloat(data.montoReembolso || 0).toFixed(2)}</div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="info-section border-start-danger">
                        <h6><i class="bx bx-message-alt-detail me-2"></i>Motivo de Cancelación</h6>
                        <div class="info-value">${data.motivoSolicitud}</div>
                        ${data.codigoPago ? `
                            <div class="mt-2">
                                <div class="info-label">Código de Pago:</div>
                                <div class="info-value">${data.codigoPago}</div>
                            </div>
                        ` : ''}
                    </div>
                </div>
            </div>
            
            ${comprobanteHtml}
            
            ${processingInfo}
            
            ${respuestaHtml}
        `);

        // Actualizar título del modal
        modal.find('.modal-title').text(`Historial de Reembolso #${data.idSolicitud}`);

        // Agregar clase específica para styling
        modal.addClass('modal-reembolso-historial');

        // Mostrar modal
        modal.modal('show');
    }

    // Actualizar título del modal
    modal.find('.modal-title').text(`Historial de Reembolso #${data.idSolicitud}`);

    // Agregar clase específica para styling
    modal.addClass('modal-reembolso-historial');

    // Mostrar modal
    modal.modal('show');

    /**
     * Exportar datos
     */
    function exportarDatos(formato) {
        console.log('📄 Exportando historial en formato:', formato);

        if (formato === 'pdf') {
            historialTable.button(0).trigger(); // Trigger PDF export
        } else if (formato === 'excel') {
            historialTable.button(1).trigger(); // Trigger Excel export
        }

        mostrarInfo(`Exportando historial en formato ${formato.toUpperCase()}...`);
    }

    /**
     * Utilidades para notificaciones
     */
    function mostrarExito(mensaje) {
        Swal.fire({
            icon: 'success',
            title: '¡Éxito!',
            text: mensaje,
            timer: 3000,
            showConfirmButton: false,
            toast: true,
            position: 'bottom-end'
        });
    }

    function mostrarError(mensaje) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: mensaje,
            confirmButtonText: 'Entendido'
        });
    }

    function mostrarInfo(mensaje) {
        Swal.fire({
            icon: 'info',
            title: 'Información',
            text: mensaje,
            timer: 2000,
            showConfirmButton: false,
            toast: true,
            position: 'bottom-end'
        });
    }

    /**
     * Configurar botones de exportación de DataTable (ACTUALIZADO PARA DTO)
     */
    function configurarBotonesExportacion() {
        new $.fn.dataTable.Buttons(historialTable, {
            buttons: [
                {
                    extend: 'pdfHtml5',
                    title: 'Historial de Reembolsos',
                    filename: 'historial_reembolsos_' + moment().format('YYYY-MM-DD'),
                    orientation: 'landscape',
                    pageSize: 'A4',
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7, 8] // Excluir columna de acciones
                    },
                    customize: function(doc) {
                        doc.content[1].table.widths = ['8%', '12%', '18%', '15%', '12%', '10%', '12%', '8%', '12%'];
                        doc.styles.tableHeader.fontSize = 9;
                        doc.defaultStyle.fontSize = 7;

                        // Agregar información del filtro aplicado
                        let filtroInfo = 'Filtros aplicados: ';
                        if (filtrosActivos.dateRange) {
                            filtroInfo += `Fechas: ${filtrosActivos.dateRange.start} - ${filtrosActivos.dateRange.end}; `;
                        }
                        if (filtrosActivos.estadoReembolso) {
                            filtroInfo += `Estado: ${filtrosActivos.estadoReembolso}; `;
                        }
                        if (filtrosActivos.tipoPago) {
                            filtroInfo += `Tipo Pago: ${filtrosActivos.tipoPago}; `;
                        }

                        doc.content.splice(1, 0, {
                            text: filtroInfo,
                            style: 'subheader',
                            margin: [0, 0, 0, 10]
                        });
                    }
                },
                {
                    extend: 'excelHtml5',
                    title: 'Historial de Reembolsos',
                    filename: 'historial_reembolsos_' + moment().format('YYYY-MM-DD'),
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7, 8] // Excluir columna de acciones
                    }
                }
            ]
        });
    }

    // Configurar botones de exportación una vez que la tabla esté lista
    historialTable.on('init', function() {
        configurarBotonesExportacion();
    });

    /**
     * Auto-refresh periódico (menos frecuente que en pendientes)
     */
    setInterval(function() {
        // Solo auto-refresh si estamos en la pestaña activa
        if (!document.hidden) {
            console.log('🔄 Auto-refresh del historial...');
            historialTable.ajax.reload(null, false); // false para mantener paginación
            cargarEstadisticas();
        }
    }, 300000); // Cada 5 minutos (historial cambia menos frecuentemente)

    /**
     * Notificación cuando la página pierde/gana foco
     */
    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            console.log('👁️ Página visible, refrescando historial...');
            historialTable.ajax.reload(null, false);
            cargarEstadisticas();
        }
    });

    console.log('✅ Sistema de historial de reembolsos inicializado completamente');
});