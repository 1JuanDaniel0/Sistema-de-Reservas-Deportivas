/**
 * REEMBOLSOS PENDIENTES - JAVASCRIPT
 * Sistema de gestión de solicitudes de reembolso para reservas pagadas en banco
 */

$(document).ready(function() {
    console.log('🏦 Inicializando sistema de reembolsos pendientes...');

    // Variables globales
    let reembolsosTable;
    let filtrosActivos = {
        dateRange: null,
        estado: null,
        espacio: null,
        tipoPago: 'En banco' // Siempre filtrar por pago en banco
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

        // Aplicar filtro inicial por defecto (hoy)
        aplicarFiltroFecha('today');

        console.log('✅ Componentes inicializados');
    }

    /**
     * Cargar datos iniciales
     */
    function cargarDatosIniciales() {
        console.log('📊 Cargando datos iniciales...');

        // Cargar filtros
        cargarEspacios();
        cargarEstadosReserva();

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

        reembolsosTable = $('#reembolsosTable').DataTable({
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
                    className: 'text-center',
                    render: function(data, type, row) {
                        let urgenteBadge = row.urgente ?
                            '<span class="badge badge-urgente ms-1">URGENTE</span>' : '';
                        return `#${data}${urgenteBadge}`;
                    }
                },
                {
                    data: 'fechaSolicitud',
                    title: 'Fecha Solicitud',
                    width: '120px',
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
                        return `
                        <div>
                            <strong>${data}</strong><br>
                            <small class="text-muted">${row.espacioTipo}</small>
                        </div>
                    `;
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
                    render: function(data) {
                        const badgeClass = {
                            'Pendiente': 'badge-pendiente',
                            'Aprobado': 'badge-aprobado',
                            'Completado': 'badge-completado', // NUEVO estado
                            'Rechazado': 'badge-rechazado'
                        };
                        return `<span class="badge ${badgeClass[data] || 'badge-normal'}">${data}</span>`;
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
                    data: 'horasRestantes',
                    title: 'Tiempo',
                    render: function(data, type, row) {
                        if (row.urgente) {
                            return `<span class="text-danger fw-bold">${data}h restantes</span>`;
                        }
                        return `${data}h restantes`;
                    }
                },
                {
                    data: null,
                    title: 'Acciones',
                    width: '180px', // Aumentar ancho para más botones
                    orderable: false,
                    searchable: false,
                    render: function(data, type, row) {
                        let botones = `
                        <div class="btn-group" role="group" aria-label="Acciones">
                            <button class="btn btn-accion btn-ver-detalle" 
                                    onclick="verDetalleSolicitud(${row.idSolicitud})"
                                    title="Ver detalles">
                                <i class="bx bx-show"></i>
                            </button>
                    `;

                        // Botones según el estado
                        if (row.estado === 'Pendiente') {
                            botones += `
                            <button class="btn btn-accion btn-aprobar" 
                                    onclick="aprobarReembolso(${row.idSolicitud})"
                                    title="Aprobar reembolso">
                                <i class="bx bx-check"></i>
                            </button>
                            <button class="btn btn-accion btn-rechazar" 
                                    onclick="rechazarReembolso(${row.idSolicitud})"
                                    title="Rechazar reembolso">
                                <i class="bx bx-x"></i>
                            </button>
                        `;
                        }
                        else if (row.estado === 'Aprobado') {
                            // NUEVO: Botón para marcar como gestionado
                            botones += `
                            <button class="btn btn-accion btn-gestionar" 
                                    onclick="marcarComoGestionado(${row.idSolicitud})"
                                    title="Marcar como gestionado">
                                <i class="bx bx-check-double"></i> Gestionar
                            </button>
                        `;
                        }
                        else if (row.estado === 'Completado') {
                            // NUEVO: Mostrar que ya fue gestionado
                            botones += `
                            <span class="badge bg-success ms-2">
                                <i class="bx bx-check-double me-1"></i>Gestionado
                            </span>
                        `;
                        }

                        botones += `</div>`; // Cerrar btn-group

                        return botones;
                    }
                }
            ],
            ajax: {
                url: '/admin/api/reembolsos/pendientes',
                type: 'GET',
                data: function(d) {
                    // Añadir filtros a la petición
                    if (filtrosActivos.dateRange) {
                        d['dateRange[start]'] = filtrosActivos.dateRange.start;
                        d['dateRange[end]'] = filtrosActivos.dateRange.end;
                    }
                    if (filtrosActivos.estado) {
                        d.estadoReserva = filtrosActivos.estado;
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
                    console.log('📥 Datos recibidos:', json.data.length, 'reembolsos');

                    // NUEVO: Actualizar estadísticas automáticamente
                    actualizarEstadisticasLocalesToTable(json.data);

                    return json.data;
                }
            },
            drawCallback: function() {
                // Configurar tooltips después de dibujar la tabla
                $('[data-bs-toggle="tooltip"]').tooltip();

                // NUEVO: Destacar filas urgentes
                destacarFilasUrgentes();
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
        reembolsosTable.ajax.reload();

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
     * Cargar estados de reserva para filtro
     */
    function cargarEstadosReserva() {
        $.get('/admin/api/estados-reserva')
            .done(function(data) {
                const select = $('#filterEstado');
                select.empty().append('<option value="">Todos los estados</option>');

                data.forEach(function(estado) {
                    select.append(
                        `<option value="${estado.idEstadoReserva}">${estado.estado}</option>`
                    );
                });

                console.log('✅ Estados de reserva cargados:', data.length);
            })
            .fail(function() {
                console.error('❌ Error cargando estados de reserva');
                mostrarError('Error cargando estados de reserva');
            });
    }

    /**
     * Cargar estadísticas
     */
    function cargarEstadisticas() {
        const params = new URLSearchParams();

        if (filtrosActivos.dateRange) {
            params.append('dateRange[start]', filtrosActivos.dateRange.start);
            params.append('dateRange[end]', filtrosActivos.dateRange.end);
        }

        $.get('/admin/api/reembolsos/estadisticas?' + params.toString())
            .done(function(data) {
                $('#totalReservas').text(data.total || 0);
                $('#reservasConfirmadas').text(data.aprobadas || 0);
                $('#reservasPendientes').text(data.pendientes || 0);
                $('#totalIngresos').text('S/ ' + (data.montoTotal || 0).toFixed(2));

                console.log('📊 Estadísticas actualizadas:', data);
            })
            .fail(function() {
                console.error('❌ Error cargando estadísticas');
                mostrarError('Error cargando estadísticas');
            });
    }

    /**
     * Aplicar filtros
     */
    function aplicarFiltros() {
        // Actualizar filtros activos
        filtrosActivos.estado = $('#filterEstado').val() || null;
        filtrosActivos.espacio = $('#filterEspacio').val() || null;
        filtrosActivos.tipoPago = $('#filterTipoPago').val() || 'En banco';

        // Recargar tabla y estadísticas
        reembolsosTable.ajax.reload();
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
        $('#filterEstado').val('');
        $('#filterEspacio').val('');
        $('#filterTipoPago').val('En banco');

        // Limpiar filtros activos
        filtrosActivos = {
            dateRange: null,
            estado: null,
            espacio: null,
            tipoPago: 'En banco'
        };

        // Reactivar filtro "hoy"
        $('.filter-date-btn').removeClass('active');
        $('.filter-date-btn[data-filter="today"]').addClass('active');
        aplicarFiltroFecha('today');

        console.log('🧹 Filtros limpiados');
        mostrarInfo('Filtros restablecidos');
    }

    /**
     * Ver detalle de solicitud
     */
    window.verDetalleSolicitud = function(idSolicitud) {
        console.log('👁️ Viendo detalle de solicitud:', idSolicitud);

        $.get(`/admin/api/reembolsos/pendientes/${idSolicitud}`)
            .done(function(data) {
                mostrarModalDetalle(data);
            })
            .fail(function() {
                console.error('❌ Error cargando detalle de solicitud');
                mostrarError('Error cargando detalles de la solicitud');
            });
    };

    /**
     * Aprobar reembolso
     */
    window.aprobarReembolso = function(idSolicitud) {
        console.log('✅ Aprobando reembolso:', idSolicitud);

        Swal.fire({
            title: '¿Aprobar reembolso?',
            text: 'Esta acción aprobará el reembolso y cancelará la reserva',
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Sí, aprobar',
            cancelButtonText: 'Cancelar',
            input: 'textarea',
            inputLabel: 'Motivo de aprobación (opcional)',
            inputPlaceholder: 'Ingrese el motivo de la aprobación...'
        }).then((result) => {
            if (result.isConfirmed) {
                const motivoRespuesta = result.value || 'Reembolso aprobado por administrador';

                $.ajax({
                    url: `/admin/api/reembolsos/pendientes/${idSolicitud}/aprobar`,
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify({ motivoRespuesta }),
                    success: function() {
                        mostrarExito('Reembolso aprobado exitosamente');
                        reembolsosTable.ajax.reload();
                        cargarEstadisticas();
                    },
                    error: function() {
                        mostrarError('Error al aprobar el reembolso');
                    }
                });
            }
        });
    };

    /**
     * Rechazar reembolso
     */
    window.rechazarReembolso = function(idSolicitud) {
        console.log('❌ Rechazando reembolso:', idSolicitud);

        Swal.fire({
            title: '¿Rechazar reembolso?',
            text: 'Esta acción rechazará la solicitud de reembolso',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, rechazar',
            cancelButtonText: 'Cancelar',
            input: 'textarea',
            inputLabel: 'Motivo del rechazo (requerido)',
            inputPlaceholder: 'Ingrese el motivo del rechazo...',
            inputValidator: (value) => {
                if (!value || value.trim().length === 0) {
                    return 'Debe proporcionar un motivo para el rechazo';
                }
            }
        }).then((result) => {
            if (result.isConfirmed) {
                const motivoRespuesta = result.value;

                $.ajax({
                    url: `/admin/api/reembolsos/pendientes/${idSolicitud}/rechazar`,
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify({ motivoRespuesta }),
                    success: function() {
                        mostrarExito('Reembolso rechazado exitosamente');
                        reembolsosTable.ajax.reload();
                        cargarEstadisticas();
                    },
                    error: function() {
                        mostrarError('Error al rechazar el reembolso');
                    }
                });
            }
        });
    };

    /**
     * Mostrar modal con detalles
     */
    function mostrarModalDetalle(data) {
        const modal = $('#reservaDetalleModal');
        const contenido = $('#reservaDetalleContent');

        let urgenteBadge = data.urgente ?
            '<div class="urgente-indicator"><i class="bx bx-time text-warning"></i> <strong class="text-warning">SOLICITUD URGENTE</strong> - La reserva es en menos de 24 horas</div>' : '';

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

        let estadoBadge = '';
        const badgeClass = {
            'Pendiente': 'badge-pendiente',
            'Aprobado': 'badge-aprobado',
            'Rechazado': 'badge-rechazado'
        };
        estadoBadge = `<span class="badge ${badgeClass[data.estado] || 'badge-normal'}">${data.estado}</span>`;

        let respuestaHtml = '';
        if (data.tiempoRespuesta) {
            respuestaHtml = `
                <div class="info-section border-start-info">
                    <h6><i class="bx bx-message-dots me-2"></i>Respuesta del Administrador</h6>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-label">Fecha de Respuesta:</div>
                            <div class="info-value">${new Date(data.tiempoRespuesta).toLocaleString('es-PE')}</div>
                        </div>
                        <div class="col-12 mt-2">
                            <div class="info-label">Motivo:</div>
                            <div class="info-value">${data.motivoRespuesta || 'Sin motivo especificado'}</div>
                        </div>
                    </div>
                </div>
            `;
        }

        contenido.html(`
            ${urgenteBadge}
            
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
                                <div class="info-label">Estado:</div>
                                <div class="info-value">${estadoBadge}</div>
                            </div>
                            <div class="col-12 mt-2">
                                <div class="info-label">Fecha de Solicitud:</div>
                                <div class="info-value">${new Date(data.fechaSolicitud).toLocaleString('es-PE')}</div>
                            </div>
                            <div class="col-12 mt-2">
                                <div class="info-label">Tiempo Restante:</div>
                                <div class="info-value ${data.urgente ? 'text-danger fw-bold' : ''}">${data.horasRestantes} horas</div>
                            </div>
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
                                <div class="info-value">${data.espacioNombre} (${data.espacioTipo})</div>
                            </div>
                            <div class="col-6 mt-2">
                                <div class="info-label">Costo:</div>
                                <div class="info-value text-success fw-bold">S/ ${parseFloat(data.costo).toFixed(2)}</div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="info-section border-start-danger">
                        <h6><i class="bx bx-message-alt-detail me-2"></i>Motivo de Cancelación</h6>
                        <div class="info-value">${data.motivo}</div>
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
            
            ${respuestaHtml}
        `);

        // Actualizar título del modal
        modal.find('.modal-title').text(`Solicitud de Reembolso #${data.idSolicitud}`);

        // Agregar clase específica para styling
        modal.addClass('modal-reembolso');

        // Mostrar modal
        modal.modal('show');
    }

    /**
     * Exportar datos
     */
    function exportarDatos(formato) {
        console.log('📄 Exportando datos en formato:', formato);

        if (formato === 'pdf') {
            reembolsosTable.button(0).trigger(); // Trigger PDF export
        } else if (formato === 'excel') {
            reembolsosTable.button(1).trigger(); // Trigger Excel export
        }

        mostrarInfo(`Exportando datos en formato ${formato.toUpperCase()}...`);
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
            position: 'bottom-end',
            zIndex: 999
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
            position: 'bottom-end',
        });
    }

    function mostrarAdvertencia(mensaje) {
        Swal.fire({
            icon: 'warning',
            title: 'Advertencia',
            text: mensaje,
            confirmButtonText: 'Entendido'
        });
    }

    /**
     * Configurar botones de exportación de DataTable
     */
    function configurarBotonesExportacion() {
        new $.fn.dataTable.Buttons(reembolsosTable, {
            buttons: [
                {
                    extend: 'pdfHtml5',
                    title: 'Reembolsos Pendientes',
                    filename: 'reembolsos_pendientes_' + moment().format('YYYY-MM-DD'),
                    orientation: 'landscape',
                    pageSize: 'A4',
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7] // Excluir columna de acciones
                    },
                    customize: function(doc) {
                        doc.content[1].table.widths = ['10%', '15%', '20%', '15%', '15%', '10%', '10%', '5%'];
                        doc.styles.tableHeader.fontSize = 10;
                        doc.defaultStyle.fontSize = 8;
                    }
                },
                {
                    extend: 'excelHtml5',
                    title: 'Reembolsos Pendientes',
                    filename: 'reembolsos_pendientes_' + moment().format('YYYY-MM-DD'),
                    exportOptions: {
                        columns: [0, 1, 2, 3, 4, 5, 6, 7] // Excluir columna de acciones
                    }
                }
            ]
        });
    }

    // Configurar botones de exportación una vez que la tabla esté lista
    reembolsosTable.on('init', function() {
        configurarBotonesExportacion();
    });

    /**
     * Notificación cuando la página pierde/gana foco
     */
    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            reembolsosTable.ajax.reload(null, false);
            cargarEstadisticas();
        }
    });

    console.log('✅ Sistema de reembolsos pendientes inicializado completamente');
});

/**
 * Marcar reembolso como gestionado (función global)
 */
window.marcarComoGestionado = function(idSolicitud) {
    console.log('🏦 Marcando reembolso como gestionado:', idSolicitud);

    Swal.fire({
        title: '¿Marcar como gestionado?',
        html: `
            <div class="text-start mb-3">
                <div class="alert alert-info d-flex align-items-center mb-3">
                    <i class="bx bx-info-circle me-2"></i>
                    <div>
                        <strong>Confirme que ya realizó el reembolso:</strong>
                        <ul class="list-unstyled mt-2 mb-0">
                            <li>✅ Transferencia bancaria realizada</li>
                            <li>📧 Vecino notificado del reembolso</li>
                            <li>📋 Comprobante archivado</li>
                        </ul>
                    </div>
                </div>
                <label for="notasGestion" class="form-label fw-bold">
                    Notas adicionales: <span class="text-muted">(opcional)</span>
                </label>
                <textarea id="notasGestion" class="form-control" rows="3" 
                          placeholder="Ej: Transferencia realizada el 31/07/2025. Comprobante #123456"></textarea>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '<i class="bx bx-check me-1"></i>Marcar como Gestionado',
        cancelButtonText: '<i class="bx bx-x me-1"></i>Cancelar',
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        width: '600px',
        preConfirm: () => {
            const notas = document.getElementById('notasGestion').value.trim();
            return { notas: notas };
        }
    }).then((result) => {
        if (result.isConfirmed) {
            // Mostrar loading
            Swal.fire({
                title: 'Procesando...',
                text: 'Marcando reembolso como gestionado',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Realizar petición
            $.ajax({
                url: `/admin/api/reembolsos/pendientes/${idSolicitud}/marcar-gestionado`,
                method: 'POST',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    [getCSRFHeader()]: getCSRFToken()
                },
                contentType: 'application/json',
                data: JSON.stringify({
                    notas: result.value.notas
                })
            })
                .done(function(response) {
                    console.log('✅ Reembolso marcado como gestionado exitosamente');

                    Swal.fire({
                        icon: 'success',
                        title: '¡Reembolso gestionado!',
                        text: 'El reembolso ha sido marcado como completado correctamente',
                        timer: 3000,
                        showConfirmButton: false
                    });

                    // Recargar tabla y estadísticas
                    if (typeof reembolsosTable !== 'undefined') {
                        reembolsosTable.ajax.reload(null, false);
                    }
                    if (typeof cargarEstadisticas === 'function') {
                        cargarEstadisticas();
                    }
                })
                .fail(function(xhr) {
                    console.error('❌ Error marcando reembolso como gestionado:', xhr.responseJSON);

                    let errorMessage = 'Error interno del servidor';
                    if (xhr.responseJSON && xhr.responseJSON.error) {
                        errorMessage = xhr.responseJSON.error;
                    }

                    Swal.fire({
                        icon: 'error',
                        title: 'Error',
                        text: errorMessage,
                        confirmButtonColor: '#dc3545'
                    });
                });
        }
    });
};

/**
 * Actualizar estadísticas basadas en los datos de la tabla
 */
function actualizarEstadisticasLocalesToTable(datos) {
    let pendientes = 0;
    let aprobados = 0;
    let completados = 0;
    let montoTotal = 0;
    let montoGestionado = 0;

    datos.forEach(function(fila) {
        const monto = parseFloat(fila.costo) || 0;
        montoTotal += monto;

        switch(fila.estado) {
            case 'Pendiente':
                pendientes++;
                break;
            case 'Aprobado':
                aprobados++;
                break;
            case 'Completado':
                completados++;
                montoGestionado += monto;
                break;
        }
    });

    // Actualizar contadores en la UI (si existen los elementos)
    $('#totalReservas').text(datos.length);
    $('#reservasPendientes').text(pendientes);
    $('#reservasAprobadas').text(aprobados);
    $('#reservasCompletadas').text(completados);
    $('#totalIngresos').text('S/ ' + montoTotal.toFixed(2));
    $('#montoGestionado').text('S/ ' + montoGestionado.toFixed(2));
}

/**
 * Destacar filas urgentes visualmente
 */
function destacarFilasUrgentes() {
    $('#reembolsosTable tbody tr').each(function() {
        const $row = $(this);
        const urgenteBadge = $row.find('.badge-urgente');

        if (urgenteBadge.length > 0) {
            $row.addClass('table-warning'); // Destacar fila completa
            $row.find('td').css('border-left', '4px solid #ff6b6b'); // Borde izquierdo rojo
        }
    });
}

function getCSRFToken() {
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.getAttribute('content') : '';
}

function getCSRFHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]');
    return header ? header.getAttribute('content') : 'X-CSRF-TOKEN';
}