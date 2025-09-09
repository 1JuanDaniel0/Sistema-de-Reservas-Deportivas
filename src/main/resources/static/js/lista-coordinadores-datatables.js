let coordinadorSeleccionado = null;
let lugaresOriginales = [];
let analisisImpacto = null;
let planReasignacion = null;

// Función mejorada para editar lugares con análisis de impacto
function editarLugares(coordinadorId, coordinadorNombre) {
    coordinadorSeleccionado = coordinadorId;

    // Cargar datos del coordinador
    $.get(`/admin/coordinador/${coordinadorId}/lugares`)
        .done(function(data) {
            // Guardar lugares originales para comparar cambios
            lugaresOriginales = [...(data.coordinador.lugaresAsignados || [])];

            // Mostrar info del coordinador
            $('#coordinadorInfo').html(`
                <strong>Coordinador:</strong> ${data.coordinador.nombre}<br>
                <strong>ID:</strong> ${data.coordinador.id}
            `);

            // Limpiar y llenar select de lugares
            $('#lugaresSelect').empty();
            data.todosLugares.forEach(lugar => {
                $('#lugaresSelect').append(
                    `<option value="${lugar.id}">${lugar.nombre}</option>`
                );
            });

            // Seleccionar lugares actuales
            $('#lugaresSelect').val(data.coordinador.lugaresAsignados);
            $('#lugaresSelect').trigger('change');

            // Resetear checkbox y variables
            $('#reasignarReservas').prop('checked', false);
            analisisImpacto = null;
            planReasignacion = null;

            // Agregar listener para cambios en selección de lugares
            $('#lugaresSelect').off('change.impacto').on('change.impacto', function() {
                analizarImpactoTiempoReal();
            });

            // Mostrar modal
            $('#editarLugaresModal').modal('show');
        })
        .fail(function() {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Error al cargar los datos del coordinador'
            });
        });
}

// Nueva función para análisis de impacto en tiempo real
function analizarImpactoTiempoReal() {
    const lugaresSeleccionados = $('#lugaresSelect').val() || [];

    // Verificar si hubo cambios
    if (arraysIguales(lugaresOriginales, lugaresSeleccionados)) {
        ocultarInfoImpacto();
        return;
    }

    // Mostrar loading
    mostrarLoadingImpacto();

    // Realizar análisis
    const token = $('meta[name="_csrf"]').attr('content');
    const header = $('meta[name="_csrf_header"]').attr('content');

    $.ajax({
        url: `/admin/coordinador/${coordinadorSeleccionado}/lugares/analizar`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [header]: token
        },
        data: JSON.stringify({
            lugaresIds: lugaresSeleccionados
        }),
        success: function(response) {
            analisisImpacto = response;
            mostrarAnalisisImpacto(response);
        },
        error: function(xhr) {
            console.error('Error analizando impacto:', xhr.responseText);
            mostrarErrorImpacto();
        }
    });
}

// Función para mostrar el análisis de impacto
function mostrarAnalisisImpacto(analisis) {
    const infoDiv = $('#impactoInfo');

    if (!analisis.puedeRealizarse) {
        // Cambio bloqueado
        infoDiv.removeClass('alert-warning alert-info').addClass('alert-danger');
        infoDiv.html(`
            <h6><i class="bx bx-error me-2"></i>Cambio Bloqueado</h6>
            <p><strong>Motivo:</strong> ${analisis.motivoBloqueo}</p>
            <p>Este cambio no puede realizarse porque dejaría entidades críticas sin supervisión.</p>
            ${generarDetalleEstadisticas(analisis.estadisticas)}
        `);

        // Deshabilitar botón de guardar
        $('#guardarLugares').prop('disabled', true);

    } else if (analisis.estrategia === 'AUTOMATICA') {
        // Reasignación automática posible
        infoDiv.removeClass('alert-danger alert-info').addClass('alert-warning');
        infoDiv.html(`
            <h6><i class="bx bx-info-circle me-2"></i>Reasignación Automática Disponible</h6>
            <p>Se pueden reasignar automáticamente las entidades afectadas.</p>
            ${generarDetalleEstadisticas(analisis.estadisticas)}
            ${generarDetalleLugares(analisis.lugaresAfectados)}
            <div class="form-check mt-2">
                <input class="form-check-input" type="checkbox" id="confirmarReasignacion">
                <label class="form-check-label" for="confirmarReasignacion">
                    <strong>Confirmo que deseo realizar la reasignación automática</strong>
                </label>
            </div>
        `);

        // Habilitar botón de guardar
        $('#guardarLugares').prop('disabled', false);

    } else if (analisis.estrategia === 'ASISTIDA') {
        // Requiere selección manual
        mostrarSeleccionManual(analisis);

        // Habilitar botón de guardar
        $('#guardarLugares').prop('disabled', false);
    }

    infoDiv.show();
}

// Función para mostrar selección manual de coordinadores
function mostrarSeleccionManual(analisis) {
    const infoDiv = $('#impactoInfo');
    infoDiv.removeClass('alert-danger alert-warning').addClass('alert-info');

    let html = `
        <h6><i class="bx bx-user-check me-2"></i>Selección Manual Requerida</h6>
        <p>Algunos lugares requieren que selecciones un coordinador específico:</p>
        ${generarDetalleEstadisticas(analisis.estadisticas)}
    `;

    // Crear plan de reasignación para obtener las opciones manuales
    const token = $('meta[name="_csrf"]').attr('content');
    const header = $('meta[name="_csrf_header"]').attr('content');

    $.ajax({
        url: `/admin/coordinador/${coordinadorSeleccionado}/lugares/analizar`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [header]: token
        },
        data: JSON.stringify({
            lugaresIds: $('#lugaresSelect').val() || [],
            crearPlan: true
        }),
        success: function(response) {
            if (response.planReasignacion && response.planReasignacion.reasignacionesManuales) {
                html += generarSelectoresCoordinadores(response.planReasignacion.reasignacionesManuales);
            }
            infoDiv.html(html);
        }
    });
}

// Función para generar selectores de coordinadores
function generarSelectoresCoordinadores(reasignacionesManuales) {
    let html = '<div class="mt-3">';

    reasignacionesManuales.forEach(manual => {
        html += `
            <div class="mb-3">
                <label class="form-label"><strong>Lugar: ${manual.lugar.nombre}</strong></label>
                <small class="d-block text-muted mb-2">${manual.descripcionImpacto}</small>
                <select class="form-select coordinador-selector" data-lugar-id="${manual.lugar.id}">
                    <option value="">Seleccionar coordinador...</option>
        `;

        manual.coordinadoresCandidatos.forEach(coord => {
            html += `<option value="${coord.id}">${coord.nombre} (Carga: ${coord.cargaTrabajo})</option>`;
        });

        html += '</select></div>';
    });

    html += '</div>';
    return html;
}

// Función para generar detalle de estadísticas
function generarDetalleEstadisticas(estadisticas) {
    if (!estadisticas || estadisticas.totalEntidadesAfectadas === 0) {
        return '<p class="text-success"><i class="bx bx-check me-1"></i>No hay entidades afectadas.</p>';
    }

    return `
        <div class="small">
            <strong>Entidades afectadas:</strong>
            <ul class="mb-0">
                ${estadisticas.totalReservasAfectadas > 0 ? `<li>Reservas: ${estadisticas.totalReservasAfectadas}</li>` : ''}
                ${estadisticas.totalMantenimientosAfectados > 0 ? `<li>Mantenimientos: ${estadisticas.totalMantenimientosAfectados}</li>` : ''}
                ${estadisticas.totalSolicitudesAfectadas > 0 ? `<li>Solicitudes: ${estadisticas.totalSolicitudesAfectadas}</li>` : ''}
                ${estadisticas.totalReembolsosAfectados > 0 ? `<li>Reembolsos: ${estadisticas.totalReembolsosAfectados}</li>` : ''}
            </ul>
        </div>
    `;
}

// Función para generar detalle de lugares afectados
function generarDetalleLugares(lugaresAfectados) {
    if (!lugaresAfectados || lugaresAfectados.length === 0) {
        return '';
    }

    let html = '<div class="mt-2"><strong>Lugares afectados:</strong><ul class="small">';

    lugaresAfectados.forEach(lugar => {
        html += `<li>${lugar.lugar.nombre}: ${lugar.totalEntidadesAfectadas} entidades`;
        if (lugar.coordinadoresDisponibles && lugar.coordinadoresDisponibles.length > 0) {
            html += ` → ${lugar.coordinadoresDisponibles[0].nombre}`;
        }
        html += '</li>';
    });

    html += '</ul></div>';
    return html;
}

// Funciones auxiliares para manejo de UI
function mostrarLoadingImpacto() {
    const infoDiv = $('#impactoInfo');
    infoDiv.removeClass('alert-danger alert-warning').addClass('alert-info');
    infoDiv.html(`
        <div class="d-flex align-items-center">
            <div class="spinner-border spinner-border-sm me-2" role="status"></div>
            <span>Analizando impacto del cambio...</span>
        </div>
    `);
    infoDiv.show();
}

function ocultarInfoImpacto() {
    $('#impactoInfo').hide();
    $('#guardarLugares').prop('disabled', false);
}

function mostrarErrorImpacto() {
    const infoDiv = $('#impactoInfo');
    infoDiv.removeClass('alert-warning alert-info').addClass('alert-danger');
    infoDiv.html(`
        <h6><i class="bx bx-error me-2"></i>Error en Análisis</h6>
        <p>No se pudo analizar el impacto del cambio. Inténtalo nuevamente.</p>
    `);
    infoDiv.show();
}

// Función actualizada para guardar lugares con análisis de impacto
$('#guardarLugares').click(function() {
    if (!coordinadorSeleccionado) return;

    const lugaresSeleccionados = $('#lugaresSelect').val() || [];
    let reasignarReservas = $('#reasignarReservas').is(':checked');

    // Verificar si hubo cambios
    if (arraysIguales(lugaresOriginales, lugaresSeleccionados)) {
        Swal.fire({
            icon: 'info',
            title: 'Sin cambios',
            text: 'No se detectaron cambios en los lugares asignados.',
            timer: 3000,
            showConfirmButton: false
        });
        return;
    }

    // Si hay análisis de impacto y entidades afectadas
    if (analisisImpacto && analisisImpacto.estadisticas.totalEntidadesAfectadas > 0) {

        // Verificar confirmación de reasignación automática
        if (analisisImpacto.estrategia === 'AUTOMATICA') {
            const confirmado = $('#confirmarReasignacion').is(':checked');
            if (!confirmado) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Confirmación requerida',
                    text: 'Debes confirmar que deseas realizar la reasignación automática.'
                });
                return;
            }
            reasignarReservas = true;
        }

        // Recopilar selecciones manuales si es necesario
        let seleccionesManual = {};
        if (analisisImpacto.estrategia === 'ASISTIDA') {
            $('.coordinador-selector').each(function() {
                const lugarId = $(this).data('lugar-id');
                const coordinadorId = $(this).val();
                if (coordinadorId) {
                    seleccionesManual[lugarId] = parseInt(coordinadorId);
                }
            });

            // Verificar que todas las selecciones requeridas estén completas
            const selectoresRequeridos = $('.coordinador-selector').length;
            const seleccionesCompletas = Object.keys(seleccionesManual).length;

            if (selectoresRequeridos > seleccionesCompletas) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Selecciones incompletas',
                    text: 'Debes seleccionar coordinadores para todos los lugares requeridos.'
                });
                return;
            }

            reasignarReservas = true;
        }
    }

    // Mostrar confirmación final
    mostrarConfirmacionFinal(lugaresSeleccionados, reasignarReservas, seleccionesManual);
});

// Función para mostrar confirmación final mejorada
function mostrarConfirmacionFinal(lugaresSeleccionados, reasignarReservas, seleccionesManual = {}) {
    const lugaresOriginalesTexto = lugaresOriginales.length > 0 ?
        `${lugaresOriginales.length} lugares` : 'Ningún lugar';
    const lugaresNuevosTexto = lugaresSeleccionados.length > 0 ?
        `${lugaresSeleccionados.length} lugares` : 'Ningún lugar';

    let htmlConfirmacion = `
        <div class="text-start">
            <p><strong>Lugares actuales:</strong> ${lugaresOriginalesTexto}</p>
            <p><strong>Lugares nuevos:</strong> ${lugaresNuevosTexto}</p>
    `;

    if (reasignarReservas) {
        htmlConfirmacion += '<p class="text-warning"><i class="bx bx-warning me-1"></i><strong>Se realizará reasignación automática</strong></p>';

        if (analisisImpacto && analisisImpacto.estadisticas) {
            htmlConfirmacion += generarDetalleEstadisticas(analisisImpacto.estadisticas);
        }

        if (Object.keys(seleccionesManual).length > 0) {
            htmlConfirmacion += '<p><strong>Coordinadores seleccionados manualmente:</strong></p><ul>';
            for (const [lugarId, coordinadorId] of Object.entries(seleccionesManual)) {
                const selector = $(`.coordinador-selector[data-lugar-id="${lugarId}"]`);
                const coordinadorNombre = selector.find('option:selected').text();
                htmlConfirmacion += `<li>${coordinadorNombre}</li>`;
            }
            htmlConfirmacion += '</ul>';
        }
    } else {
        htmlConfirmacion += '<p class="text-info"><i class="bx bx-info-circle me-1"></i>Sin reasignación automática</p>';
    }

    htmlConfirmacion += '</div>';

    Swal.fire({
        title: '¿Confirmar cambios?',
        html: htmlConfirmacion,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#696cff',
        cancelButtonColor: '#8592a3',
        confirmButtonText: 'Sí, guardar cambios',
        cancelButtonText: 'Cancelar',
        customClass: {
            popup: 'swal-wide'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            ejecutarCambiosLugares(lugaresSeleccionados, reasignarReservas, seleccionesManual);
        }
    });
}

// Función para ejecutar los cambios finales
function ejecutarCambiosLugares(lugaresSeleccionados, reasignarReservas, seleccionesManual) {
    const formData = new FormData();

    if (lugaresSeleccionados && lugaresSeleccionados.length > 0) {
        lugaresSeleccionados.forEach(lugarId => {
            formData.append('lugaresIds', lugarId);
        });
    }

    formData.append('reasignarReservas', reasignarReservas);

    // Agregar selecciones manuales si las hay
    if (Object.keys(seleccionesManual).length > 0) {
        formData.append('planReasignacion', JSON.stringify({
            seleccionesManual: seleccionesManual
        }));
    }

    const token = $('meta[name="_csrf"]').attr('content');
    const header = $('meta[name="_csrf_header"]').attr('content');

    // Mostrar loading
    Swal.fire({
        title: 'Guardando cambios...',
        text: 'Por favor espere',
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    $.ajax({
        url: `/admin/coordinador/${coordinadorSeleccionado}/lugares`,
        method: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        },
        success: function(response) {
            handleSuccessResponse(response);
        },
        error: function(xhr) {
            handleErrorResponse(xhr);
        }
    });
}

// Función para manejar respuesta exitosa
function handleSuccessResponse(response) {
    $('#editarLugaresModal').modal('hide');

    let mensaje = 'Lugares actualizados correctamente';

    if (response.reasignacion) {
        mensaje += `\n\nReasignación realizada:`;
        for (const [tipo, cantidad] of Object.entries(response.reasignacion.entidadesReasignadas)) {
            mensaje += `\n- ${tipo}: ${cantidad}`;
        }
    }

    Swal.fire({
        icon: 'success',
        title: '¡Éxito!',
        text: mensaje,
        timer: 5000,
        showConfirmButton: true
    });

    // Recargar tabla
    $('.datatables-coordinadores').DataTable().ajax.reload();
}

// Función para manejar respuesta de error
function handleErrorResponse(xhr) {
    let mensaje = 'Error al actualizar los lugares';

    try {
        const response = JSON.parse(xhr.responseText);
        if (response.message) {
            mensaje = response.message;
        } else if (response.error) {
            mensaje = response.error;
        }

        // Si hay análisis de impacto en el error, mostrarlo
        if (response.analisis) {
            console.log('Análisis de impacto en error:', response.analisis);
        }

    } catch (e) {
        console.error('Error parseando respuesta:', e);
    }

    Swal.fire({
        icon: 'error',
        title: 'Error',
        text: mensaje
    });
}

// Función auxiliar que ya existía (mantener)
function arraysIguales(arr1, arr2) {
    if (arr1.length !== arr2.length) return false;
    const sorted1 = [...arr1].sort();
    const sorted2 = [...arr2].sort();
    return sorted1.every((val, i) => val === sorted2[i]);
}

// ===== FUNCIÓN PARA VER LUGARES ASIGNADOS (del script antiguo) =====
function verLugaresAsignados(coordinadorId, coordinadorNombre, lugaresTexto) {
    const modalHtml = `
        <div class="modal fade" id="verLugaresModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <i class="bx bx-map me-2"></i>Lugares Asignados
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="alert alert-info">
                            <strong>Coordinador:</strong> ${coordinadorNombre}<br>
                            <strong>ID:</strong> ${coordinadorId}
                        </div>
                        
                        <div id="lugaresListado">
                            <div class="d-flex justify-content-center">
                                <div class="spinner-border text-primary" role="status">
                                    <span class="visually-hidden">Cargando...</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                        <button type="button" class="btn btn-primary" 
                                onclick="editarLugares(${coordinadorId}, '${coordinadorNombre}'); $('#verLugaresModal').modal('hide');">
                            <i class="bx bx-edit me-1"></i>Editar Lugares
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Remover modal existente si existe
    $('#verLugaresModal').remove();

    // Agregar nuevo modal al body
    $('body').append(modalHtml);

    // Mostrar modal
    $('#verLugaresModal').modal('show');

    // Cargar datos detallados
    $.get(`/admin/coordinador/${coordinadorId}/lugares`)
        .done(function(data) {
            let contenido = '';

            if (data.coordinador.lugaresAsignados && data.coordinador.lugaresAsignados.length > 0) {
                contenido = '<h6 class="mb-3">Lugares asignados:</h6>';
                contenido += '<div class="list-group">';

                data.todosLugares.forEach(lugar => {
                    if (data.coordinador.lugaresAsignados.includes(lugar.id)) {
                        contenido += `
                            <div class="list-group-item d-flex justify-content-between align-items-center">
                                <div>
                                    <i class="bx bx-map-pin text-primary me-2"></i>
                                    <strong>${lugar.nombre}</strong>
                                </div>
                                <span class="badge bg-success">Asignado</span>
                            </div>
                        `;
                    }
                });

                contenido += '</div>';
            } else {
                contenido = `
                    <div class="text-center py-4">
                        <i class="bx bx-map-pin bx-lg text-muted mb-3"></i>
                        <p class="text-muted">No tiene lugares asignados</p>
                    </div>
                `;
            }

            $('#lugaresListado').html(contenido);
        })
        .fail(function() {
            $('#lugaresListado').html(`
                <div class="alert alert-danger">
                    <i class="bx bx-error me-2"></i>Error al cargar los lugares asignados
                </div>
            `);
        });

    // Limpiar modal cuando se cierre
    $('#verLugaresModal').on('hidden.bs.modal', function() {
        $(this).remove();
    });
}

$(document).ready(function() {
    console.log('🚀 Inicializando DataTable de coordinadores');

    // Inicializar DataTable
    $('#coordinadores-table').DataTable({
        processing: true,
        serverSide: true,
        ajax: {
            url: '/admin/coordinador/coordinadores-datatable',
            type: 'GET',
            error: function(xhr, error, code) {
                console.error('❌ Error en AJAX DataTable:', error, code);
                console.error('❌ Respuesta del servidor:', xhr.responseText);
                console.error('❌ Status:', xhr.status);
            }
        },
        columns: [
            {
                data: null,
                searchable: false,
                orderable: false,
                render: function (data, type, row, meta) {
                    return meta.row + 1; // numeración
                }
            },
            { data: 'nombres', name: 'nombres' },
            { data: 'apellidos', name: 'apellidos' },
            { data: 'dni', name: 'dni' },
            { data: 'correo', name: 'correo' },
            {
                data: 'estado',
                name: 'estado',
                render: function (data) {
                    let clase = data === 'Activo' ? 'bg-success' : 'bg-danger';
                    return `<span class="badge ${clase}">${data}</span>`;
                }
            },
            {
                data: 'lugaresAsignados',
                name: 'lugaresAsignados',
                orderable: false,
                render: function (data, type, row) {
                    if (!data || data === 'Sin lugares asignados') {
                        return `
                            <span class="text-muted">Sin lugares asignados</span>
                            <button type="button" class="btn btn-sm btn-outline-info ms-2" 
                                    onclick="verLugaresAsignados(${row.idUsuarios}, '${row.nombres} ${row.apellidos}', '')">
                                <i class="bx bx-show me-1"></i>Ver
                            </button>
                        `;
                    }

                    // Contar lugares y mostrar resumen
                    const lugares = data.split(', ');
                    const cantidadLugares = lugares.length;
                    const resumen = cantidadLugares === 1 ?
                        lugares[0] :
                        `${cantidadLugares} lugares asignados`;

                    return `
                        <span class="text-primary">${resumen}</span>
                        <button type="button" class="btn btn-sm btn-outline-info ms-2" 
                                onclick="verLugaresAsignados(${row.idUsuarios}, '${row.nombres} ${row.apellidos}', '${data.replace(/'/g, '\\\'').replace(/"/g, '&quot;')}')">
                            <i class="bx bx-show me-1"></i>Ver
                        </button>
                    `;
                }
            },
            {
                data: null,
                searchable: false,
                orderable: false,
                render: function (data, type, row) {
                    return `
                        <div class="dropdown">
                            <button type="button" class="btn btn-sm btn-outline-primary dropdown-toggle" 
                                    data-bs-toggle="dropdown" aria-expanded="false">
                                Acciones
                            </button>
                            <ul class="dropdown-menu">
                                <li>
                                    <a class="dropdown-item" href="javascript:void(0);" 
                                       onclick="editarLugares(${row.idUsuarios}, '${row.nombres} ${row.apellidos}')">
                                        <i class="bx bx-edit me-1"></i>Editar Lugares
                                    </a>
                                </li>
                                <li>
                                    <a class="dropdown-item" href="javascript:void(0);" 
                                       onclick="verImpacto(${row.idUsuarios})">
                                        <i class="bx bx-info-circle me-1"></i>Ver Impacto
                                    </a>
                                </li>
                            </ul>
                        </div>
                    `;
                }
            }
        ],
        language: {
            url: '//cdn.datatables.net/plug-ins/1.13.4/i18n/es-ES.json'
        },
        responsive: true,
        order: [[1, 'asc']], // Ordenar por nombres
        pageLength: 10,
        lengthMenu: [[10, 25, 50, 100], [10, 25, 50, 100]]
    });
});

// ===== FUNCIÓN PARA VER IMPACTO (mejorada) =====
function verImpacto(coordinadorId) {
    $.get(`/admin/coordinador/${coordinadorId}/impacto`)
        .done(function(data) {
            $('#impactoContent').html(`
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <div class="card bg-primary text-white">
                            <div class="card-body text-center">
                                <h3 style="color: white;">${data.reservasActivas}</h3>
                                <p class="mb-0">Reservas Activas</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6 mb-3">
                        <div class="card bg-warning text-white">
                            <div class="card-body text-center">
                                <h3 style="color: white;">${data.mantenimientosPendientes}</h3>
                                <p class="mb-0">Mantenimientos Pendientes</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6 mb-3">
                        <div class="card bg-info text-white">
                            <div class="card-body text-center">
                                <h3 style="color: white;">${data.solicitudesPendientes}</h3>
                                <p class="mb-0">Solicitudes Pendientes</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6 mb-3">
                        <div class="card bg-danger text-white">
                            <div class="card-body text-center">
                                <h3 style="color: white;">${data.reembolsosPendientes}</h3>
                                <p class="mb-0">Reembolsos Pendientes</p>
                            </div>
                        </div>
                    </div>
                </div>
            `);
            $('#impactoModal').modal('show');
        })
        .fail(function() {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Error al cargar el impacto del coordinador'
            });
        });
}