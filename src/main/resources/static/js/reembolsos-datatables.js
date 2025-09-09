$(document).ready(function () {
    // Inicializar DataTable con configuración avanzada
    var tabla = $('#tablaReembolsos').DataTable({
        responsive: true,
        autoWidth: false,
        pageLength: 10,
        lengthMenu: [[5, 10, 25, 50, -1], [5, 10, 25, 50, "Todos"]],
        language: {
            url: '/lang/es-ES.json'
        },
        dom: '<"row"<"col-sm-12 col-md-6"l><"col-sm-12 col-md-6"f>>' +
            '<"row"<"col-sm-12"tr>>' +
            '<"row"<"col-sm-12 col-md-5"i><"col-sm-12 col-md-7"p>>',
        columnDefs: [
            { targets: [0], className: 'text-start', width: '15%' }, // Vecino
            { targets: [1], className: 'text-start', width: '15%' }, // Espacio
            { targets: [2], className: 'text-center', width: '10%' }, // Fecha reserva
            { targets: [3], className: 'text-center', width: '8%' }, // Monto
            { targets: [4], className: 'text-start', width: '20%' }, // Motivo solicitud
            { targets: [5], className: 'text-center', width: '10%' }, // Fecha solicitud
            { targets: [6], className: 'text-center', width: '8%' }, // Estado
            { targets: [7], className: 'text-center', width: '10%' }, // Tiempo respuesta
            { targets: [8], className: 'text-start', width: '15%' }, // Motivo respuesta
            { targets: [9], className: 'text-center', orderable: false, width: '10%' } // Acciones
        ],
        order: [[5, 'desc']], // Ordenar por fecha de solicitud (más recientes primero)
        buttons: [
            {
                extend: 'excel',
                text: '<i class="bx bx-download"></i> Excel',
                className: 'btn btn-success btn-sm',
                title: 'Solicitudes_Reembolso_' + new Date().toISOString().slice(0,10),
                exportOptions: {
                    columns: [0, 1, 2, 3, 4, 5, 6, 7, 8],
                    format: {
                        body: function (data, row, column, node) {
                            var $node = $(node);
                            switch(column) {
                                case 0: // Vecino
                                    var nombre = $node.find('h6').text().trim();
                                    var dni = $node.find('small').text().trim();
                                    return nombre + ' (' + dni + ')';
                                case 1: // Espacio
                                    var espacio = $node.find('h6').text().trim();
                                    var lugar = $node.find('small').text().trim();
                                    return espacio + ', ' + lugar;
                                case 2: // Reserva (fecha y hora)
                                    var fecha = $node.find('strong').text().trim();
                                    var hora = $node.find('small span').text().trim();
                                    return fecha + ' ' + hora;
                                case 3: // Monto
                                    return $node.find('span:last').text().trim();
                                case 4: // Motivo solicitud (sin el botón "Ver completo")
                                    return $node.contents().first().text().trim();
                                case 5: // Fecha solicitud
                                    var fechaSol = $node.find('small').first().text().trim();
                                    var horaSol = $node.find('small').last().text().trim();
                                    return fechaSol + ' ' + horaSol;
                                case 6: // Estado
                                    return $node.find('span').text().trim();
                                case 7: // Tiempo respuesta
                                    return $node.text().trim().replace('-', 'Sin procesar');
                                case 8: // Motivo respuesta
                                    var motivoRespuesta = $node.contents().first().text().trim();
                                    return motivoRespuesta || 'Sin motivo registrado';
                                default:
                                    return data;
                            }
                        }
                    }
                }
            }
        ],
        initComplete: function () {
            $('.dataTables_filter input').attr('placeholder', 'Buscar por vecino, espacio, motivo...');
            $('.dataTables_filter input').addClass('form-control');
            $('.dataTables_filter input').removeClass('form-control-sm');
            $('[title]').tooltip();
            console.log('✅ DataTable de reembolsos iniciada correctamente');
        },
        drawCallback: function() {
            $('[title]').tooltip('dispose').tooltip();
            $('#tablaReembolsos tbody tr').each(function() {
                var urgenteBadge = $(this).find('.badge:contains("URGENTE")');
                if (urgenteBadge.length > 0) {
                    $(this).addClass('table-warning');
                }
            });
        }
    });

    // ========== EVENT LISTENERS CORREGIDOS ==========

    // Filtros de tiempo
    $('input[name="filtroTiempo"]').on('change', function() {
        var filtro = $(this).val();
        $('input[name="filtroTiempo"]').next('.btn').removeClass('active');
        $(this).next('.btn').addClass('active');

        var hoy = new Date();
        var filtroFecha = '';

        switch(filtro) {
            case 'hoy':
                var fechaHoy = hoy.getDate().toString().padStart(2, '0') + '/' +
                    (hoy.getMonth() + 1).toString().padStart(2, '0') + '/' +
                    hoy.getFullYear();
                filtroFecha = fechaHoy;
                break;
            case 'semana':
                var inicioSemana = new Date(hoy);
                var dia = hoy.getDay();
                var diferencia = dia === 0 ? -6 : 1 - dia;
                inicioSemana.setDate(hoy.getDate() + diferencia);
                var mesInicio = (inicioSemana.getMonth() + 1).toString().padStart(2, '0');
                filtroFecha = mesInicio + '/' + inicioSemana.getFullYear();
                break;
            case 'mes':
                var mesActual = (hoy.getMonth() + 1).toString().padStart(2, '0');
                var añoActual = hoy.getFullYear();
                filtroFecha = mesActual + '/' + añoActual;
                break;
            case 'todos':
            default:
                filtroFecha = '';
                break;
        }

        tabla.column(5).search(filtroFecha, false, false).draw();
        console.log(`📅 Filtro de tiempo aplicado: ${filtro} - Buscando: ${filtroFecha}`);
    });

    // Filtros rápidos por estado
    $('input[name="filtroEstado"]').on('change', function() {
        var filtro = $(this).val();
        $('input[name="filtroEstado"]').next('.btn').removeClass('active');
        $(this).next('.btn').addClass('active');

        if (filtro === 'todos') {
            tabla.column(6).search('').draw();
            tabla.search('').draw();
        } else if (filtro === 'urgentes') {
            tabla.search('URGENTE').draw();
        } else if (filtro === 'procesadas') {
            tabla.column(6).search('Aprobado|Rechazado', true, false).draw();
        } else {
            tabla.column(6).search(filtro).draw();
        }

        console.log(`🔍 Filtro de estado aplicado: ${filtro}`);
    });

    // Inicializar estados de los filtros
    $('input[name="filtroTiempo"][value="todos"]').prop('checked', true);
    $('input[name="filtroTiempo"][value="todos"]').next('.btn').addClass('active');
    $('input[name="filtroEstado"][value="todos"]').prop('checked', true);
    $('input[name="filtroEstado"][value="todos"]').next('.btn').addClass('active');

    // Botón de exportar
    $('#exportarExcel').on('click', function() {
        tabla.button(0).trigger();
    });

    // Función para ver motivo completo (solicitud)
    $(document).on('click', '.ver-motivo-completo', function() {
        var motivo = $(this).data('motivo');
        $('#motivoCompletoTexto').text(motivo);
        $('#modalMotivoCompleto').modal('show');
    });

    // Función para ver motivo de respuesta completo
    $(document).on('click', '.ver-motivo-respuesta', function() {
        var motivoRespuesta = $(this).data('motivo-respuesta');
        $('#motivoRespuestaTexto').text(motivoRespuesta);
        $('#modalMotivoRespuesta').modal('show');
    });

    // ========== CORRECCION PRINCIPAL: Event listeners para aprobar/rechazar ==========

    $(document).on('click', '.btn-aprobar', function(e) {
        e.preventDefault();
        e.stopPropagation();

        var id = $(this).data('id');
        var tipoPago = $(this).data('tipo-pago') || 'En banco'; // fallback

        console.log('🟢 Botón aprobar clickeado, ID:', id, 'Tipo pago:', tipoPago);

        mostrarModalAprobacion(id, tipoPago);
    });

    // Event listener para botón RECHAZAR
    $(document).on('click', '.btn-rechazar', function(e) {
        e.preventDefault();
        e.stopPropagation();

        var id = $(this).data('id');
        console.log('🔴 Botón rechazar clickeado, ID:', id);

        mostrarModalRechazo(id);
    });

    // ========== FUNCIONES MODALES CORREGIDAS ==========

    function obtenerTipoPagoDeReserva(row) {
        // Intentar obtener tipo de pago desde la reserva
        // Como no está directamente en la tabla, asumimos según el contexto
        // En un caso real, podrías agregarlo como data-attribute en el botón
        return 'En banco'; // Por defecto, ya que es más común en solicitudes de reembolso
    }

    function mostrarModalAprobacion(id, tipoPago) {
        console.log('📝 Mostrando modal de aprobación para ID:', id, 'Tipo pago:', tipoPago);

        var tituloModal = '¿Aprobar solicitud de reembolso?';
        var contenidoAdicional = '';

        if (tipoPago === 'En línea') {
            contenidoAdicional = `
                <div class="alert alert-info d-flex align-items-center mb-3">
                    <i class="bx bx-credit-card me-2"></i>
                    <div>
                        <strong>Reembolso automático:</strong>
                        <ul class="list-unstyled mt-2 mb-0">
                            <li>✅ Se procesará automáticamente via MercadoPago</li>
                            <li>📧 Se notificará al vecino por correo electrónico</li>
                            <li>💰 Los fondos aparecerán en 5-10 días hábiles</li>
                        </ul>
                    </div>
                </div>
            `;
        } else {
            contenidoAdicional = `
                <div class="alert alert-warning d-flex align-items-center mb-3">
                    <i class="bx bx-bank me-2"></i>
                    <div>
                        <strong>Reembolso manual:</strong>
                        <ul class="list-unstyled mt-2 mb-0">
                            <li>✅ Tu aprobación será enviada al administrador</li>
                            <li>📧 Se notificará al vecino por correo electrónico</li>
                            <li>💰 El administrador gestionará el reembolso en 3-5 días hábiles</li>
                        </ul>
                    </div>
                </div>
            `;
        }

        Swal.fire({
            title: tituloModal,
            html: `
                <div class="text-start mb-3">
                    ${contenidoAdicional}
                    <label for="motivoAprobacion" class="form-label fw-bold">
                        Motivo de la aprobación: <span class="text-danger">*</span>
                    </label>
                    <textarea id="motivoAprobacion" class="form-control" rows="3" 
                              placeholder="Ej: Solicitud válida según políticas de cancelación. El vecino cumple con los requisitos establecidos..." required></textarea>
                    <small class="text-muted">Este motivo será enviado al vecino por correo electrónico.</small>
                </div>
            `,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: '<i class="bx bx-check"></i> Confirmar Aprobación',
            cancelButtonText: '<i class="bx bx-x"></i> Cancelar',
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            width: '600px',
            focusConfirm: false,
            preConfirm: () => {
                const motivo = document.getElementById('motivoAprobacion').value.trim();
                if (!motivo) {
                    Swal.showValidationMessage('Debe proporcionar un motivo para la aprobación');
                    return false;
                }
                if (motivo.length < 10) {
                    Swal.showValidationMessage('El motivo debe tener al menos 10 caracteres');
                    return false;
                }
                return motivo;
            }
        }).then(result => {
            if (result.isConfirmed) {
                const motivo = result.value;

                // SweetAlert de procesamiento
                Swal.fire({
                    title: 'Procesando aprobación...',
                    html: `
                        <div class="text-center">
                            <div class="spinner-border text-primary mb-3" role="status">
                                <span class="visually-hidden">Cargando...</span>
                            </div>
                            <p class="mb-2">${tipoPago === 'En línea' ? 'Procesando reembolso automático...' : 'Enviando aprobación al administrador...'}</p>
                            <small class="text-muted">Este proceso puede tardar unos segundos</small>
                        </div>
                    `,
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    showConfirmButton: false
                });

                // Enviar formulario
                enviarFormularioAprobacion(id, motivo);
            }
        });

        // Enfocar el textarea cuando se abra el modal
        setTimeout(() => {
            const textarea = document.getElementById('motivoAprobacion');
            if (textarea) {
                textarea.focus();
            }
        }, 100);
    }

    function mostrarModalRechazo(id) {
        Swal.fire({
            title: '¿Rechazar solicitud de reembolso?',
            html: `
                <div class="text-start mb-3">
                    <div class="alert alert-warning d-flex align-items-center mb-3">
                        <i class="bx bx-error-circle me-2"></i>
                        <div>
                            <strong>Importante:</strong> El vecino será notificado del rechazo por correo electrónico.
                        </div>
                    </div>
                    <label for="motivoRechazo" class="form-label fw-bold">
                        Motivo del rechazo: <span class="text-danger">*</span>
                    </label>
                    <textarea id="motivoRechazo" class="form-control" rows="4" 
                              placeholder="Ej: La solicitud no cumple con el plazo mínimo de 48 horas establecido en nuestras políticas de cancelación..." required></textarea>
                    <small class="text-muted">Explique claramente el motivo del rechazo para que el vecino comprenda la decisión.</small>
                </div>
            `,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: '<i class="bx bx-x"></i> Confirmar Rechazo',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            width: '600px',
            focusConfirm: false,
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
        }).then(result => {
            if (result.isConfirmed) {
                const motivo = result.value;

                // SweetAlert de procesamiento para rechazo
                Swal.fire({
                    title: 'Procesando rechazo...',
                    html: `
                        <div class="text-center">
                            <div class="spinner-border text-warning mb-3" role="status">
                                <span class="visually-hidden">Cargando...</span>
                            </div>
                            <p class="mb-2">Registrando la decisión...</p>
                            <small class="text-muted">Enviando notificación al vecino</small>
                        </div>
                    `,
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    showConfirmButton: false
                });

                enviarFormularioRechazo(id, motivo);
            }
        });

        // Enfocar el textarea cuando se abra el modal
        setTimeout(() => {
            const textarea = document.getElementById('motivoRechazo');
            if (textarea) {
                textarea.focus();
            }
        }, 100);
    }

    // ========== FUNCIONES DE ENVÍO CORREGIDAS ==========

    function enviarFormularioAprobacion(id, motivo) {
        console.log('📤 Enviando formulario de aprobación, ID:', id, 'Motivo:', motivo);

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/coordinador/solicitud/${id}/aceptar`;

        // Agregar el motivo
        const inputMotivo = document.createElement('input');
        inputMotivo.type = 'hidden';
        inputMotivo.name = 'motivoAceptacion';
        inputMotivo.value = motivo;
        form.appendChild(inputMotivo);

        // Agregar token CSRF
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        if (csrfToken) {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = csrfToken.getAttribute('content');
            form.appendChild(csrfInput);
            console.log('🔐 Token CSRF agregado:', csrfToken.getAttribute('content'));
        } else {
            console.warn('⚠️ No se encontró token CSRF');
        }

        document.body.appendChild(form);
        form.submit();
    }

    function enviarFormularioRechazo(id, motivo) {
        console.log('📤 Enviando formulario de rechazo, ID:', id, 'Motivo:', motivo);

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/coordinador/solicitud/${id}/rechazar`;

        // Agregar el motivo
        const inputMotivo = document.createElement('input');
        inputMotivo.type = 'hidden';
        inputMotivo.name = 'motivoRechazo';
        inputMotivo.value = motivo;
        form.appendChild(inputMotivo);

        // Agregar token CSRF
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        if (csrfToken) {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = csrfToken.getAttribute('content');
            form.appendChild(csrfInput);
            console.log('🔐 Token CSRF agregado para rechazo');
        }

        document.body.appendChild(form);
        form.submit();
    }

    // Función global para ver detalles de solicitud
    window.verDetallesSolicitud = function(btn) {
        var row = $(btn).closest('tr');
        var solicitudId = $(btn).data('id');

        // Extraer datos de la fila
        $('#detalleNombreVecino').text(row.find('td:eq(0) h6').text());
        $('#detalleDniVecino').text(row.find('td:eq(0) small').text());
        $('#detalleEspacio').text(row.find('td:eq(1) h6').text() + ' - ' + row.find('td:eq(1) small').text());
        $('#detalleFechaHora').text(row.find('td:eq(2)').text().replace(/\s+/g, ' ').trim());
        $('#detalleMonto').text(row.find('td:eq(3)').text());
        $('#detalleMotivoCompleto').text(row.find('.ver-motivo-completo').data('motivo'));

        // Configurar botones del modal
        $('#aprobarDesdeModal').data('id', solicitudId);
        $('#rechazarDesdeModal').data('id', solicitudId);

        $('#modalDetallesSolicitud').modal('show');
    };

    // Manejar aprobación/rechazo desde modal de detalles
    $('#aprobarDesdeModal').on('click', function() {
        var id = $(this).data('id');
        $('#modalDetallesSolicitud').modal('hide');
        setTimeout(() => mostrarModalAprobacion(id, 'En banco'), 500); // Asumir tipo de pago por defecto
    });

    $('#rechazarDesdeModal').on('click', function() {
        var id = $(this).data('id');
        $('#modalDetallesSolicitud').modal('hide');
        setTimeout(() => mostrarModalRechazo(id), 500);
    });

    console.log('🎉 Event listeners de aprobación/rechazo configurados correctamente');
});