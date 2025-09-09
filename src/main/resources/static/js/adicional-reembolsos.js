// adicional-reembolsos.js - Script complementario para funcionalidades avanzadas

$(document).ready(function() {

    // ========== VALIDACIONES Y UTILIDADES ==========

    // Función para validar motivos
    function validarMotivo(motivo, tipoAccion) {
        if (!motivo || motivo.trim().length === 0) {
            return {
                valido: false,
                mensaje: `Debe proporcionar un motivo para la ${tipoAccion}.`
            };
        }

        if (motivo.trim().length < 10) {
            return {
                valido: false,
                mensaje: `El motivo debe tener al menos 10 caracteres.`
            };
        }

        if (motivo.trim().length > 1000) {
            return {
                valido: false,
                mensaje: `El motivo no puede exceder los 1000 caracteres.`
            };
        }

        return {
            valido: true,
            mensaje: 'Motivo válido'
        };
    }

    // ========== MEJORAS DE UX ==========

    // Auto-resize de textareas en modales
    $(document).on('input', 'textarea', function() {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });

    // Contador de caracteres en tiempo real
    $(document).on('input', '#motivoAprobacion, #motivoRechazo', function() {
        const maxLength = 1000;
        const currentLength = $(this).val().length;
        const remaining = maxLength - currentLength;

        // Crear o actualizar contador
        let counterId = $(this).attr('id') + 'Counter';
        let counter = $('#' + counterId);

        if (counter.length === 0) {
            $(this).after(`<small id="${counterId}" class="text-muted d-block mt-1"></small>`);
            counter = $('#' + counterId);
        }

        counter.text(`${currentLength}/${maxLength} caracteres`);

        if (remaining < 50) {
            counter.removeClass('text-muted').addClass('text-warning');
        } else if (remaining < 20) {
            counter.removeClass('text-warning').addClass('text-danger');
        } else {
            counter.removeClass('text-warning text-danger').addClass('text-muted');
        }
    });

    // ========== ESTADÍSTICAS Y MÉTRICAS ==========

    // Calcular tiempo promedio de respuesta visible
    function calcularTiempoPromedioRespuesta() {
        let tiemposTotales = 0;
        let solicitudesProcesadas = 0;

        $('#tablaReembolsos tbody tr').each(function() {
            const fechaSolicitud = $(this).find('td:eq(5) small:first').text();
            const tiempoRespuesta = $(this).find('td:eq(7) small:first').text();

            if (fechaSolicitud && tiempoRespuesta && tiempoRespuesta !== '-') {
                // Aquí podrías implementar el cálculo real si tienes las fechas en formato procesable
                solicitudesProcesadas++;
            }
        });

        return solicitudesProcesadas > 0 ? Math.round(tiemposTotales / solicitudesProcesadas) : 0;
    }

    // ========== NOTIFICACIONES Y ALERTAS ==========

    // Mostrar notificación de solicitudes urgentes
    function verificarSolicitudesUrgentes() {
        const urgentes = $('#tablaReembolsos tbody .badge:contains("URGENTE")').length;

        if (urgentes > 0) {
            // Crear notificación flotante
            if ($('#notificacionUrgente').length === 0) {
                $('body').append(`
                    <div id="notificacionUrgente" class="position-fixed top-0 end-0 p-3" style="z-index: 1055;">
                        <div class="toast show" role="alert">
                            <div class="toast-header bg-danger text-white">
                                <i class="bx bx-alarm-exclamation me-2"></i>
                                <strong class="me-auto">Solicitudes Urgentes</strong>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
                            </div>
                            <div class="toast-body">
                                <strong>${urgentes}</strong> solicitud(es) requieren atención inmediata (reserva en menos de 24h).
                                <div class="mt-2">
                                    <button class="btn btn-sm btn-outline-danger" onclick="filtrarUrgentes()">
                                        Ver urgentes
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                `);
            }
        } else {
            $('#notificacionUrgente').remove();
        }
    }

    // Función para filtrar solo solicitudes urgentes
    window.filtrarUrgentes = function() {
        $('input[name="filtroEstado"][value="urgentes"]').prop('checked', true).trigger('change');
        $('#notificacionUrgente').remove();
    };

    // ========== EXPORTACIÓN MEJORADA ==========

    // Función para exportar con filtros aplicados
    window.exportarConFiltros = function() {
        const filtroEstado = $('input[name="filtroEstado"]:checked').val();
        const filtroTiempo = $('input[name="filtroTiempo"]:checked').val();

        let nombreArchivo = 'Solicitudes_Reembolso';

        if (filtroEstado !== 'todos') {
            nombreArchivo += '_' + filtroEstado.charAt(0).toUpperCase() + filtroEstado.slice(1);
        }

        if (filtroTiempo !== 'todos') {
            nombreArchivo += '_' + filtroTiempo.charAt(0).toUpperCase() + filtroTiempo.slice(1);
        }

        nombreArchivo += '_' + new Date().toISOString().slice(0,10);

        // Actualizar el título del botón de exportación
        const tabla = $('#tablaReembolsos').DataTable();
        tabla.button(0).title(nombreArchivo);
        tabla.button(0).trigger();
    };

    // ========== ATAJOS DE TECLADO ==========

    // Atajos de teclado para acciones comunes
    $(document).on('keydown', function(e) {
        // Solo si no estamos en un input o textarea
        if (!$(e.target).is('input, textarea, select')) {
            switch(e.key.toLowerCase()) {
                case 'f': // Filtrar
                    e.preventDefault();
                    $('#tablaReembolsos_filter input').focus();
                    break;
                case 'e': // Exportar
                    if (e.ctrlKey) {
                        e.preventDefault();
                        exportarConFiltros();
                    }
                    break;
                case 'r': // Refrescar (Ctrl+R)
                    if (e.ctrlKey) {
                        e.preventDefault();
                        location.reload();
                    }
                    break;
            }
        }
    });

    // ========== MEJORAR MODALES CON ANIMACIONES ==========

    // Añadir animaciones suaves a los modales
    $('.modal').on('show.bs.modal', function() {
        $(this).find('.modal-dialog').addClass('animate__animated animate__fadeInDown');
    });

    $('.modal').on('hide.bs.modal', function() {
        $(this).find('.modal-dialog').removeClass('animate__fadeInDown');
    });

    // ========== AUTO-ACTUALIZACIÓN (OPCIONAL) ==========

    // Función para auto-actualizar la página cada X minutos (opcional)
    function configurarAutoActualizacion() {
        const INTERVALO_ACTUALIZACION = 5 * 60 * 1000; // 5 minutos

        // Solo si hay solicitudes pendientes
        const solicitudesPendientes = $('#tablaReembolsos tbody .badge:contains("Pendiente")').length;

        if (solicitudesPendientes > 0) {
            setTimeout(() => {
                if (confirm('Han pasado 5 minutos. ¿Desea actualizar la página para ver nuevas solicitudes?')) {
                    location.reload();
                }
            }, INTERVALO_ACTUALIZACION);
        }
    }

    // ========== INICIALIZACIÓN ==========

    // Verificar solicitudes urgentes al cargar
    verificarSolicitudesUrgentes();

    // Configurar auto-actualización si hay pendientes
    configurarAutoActualizacion();

    // Configurar tooltips mejorados
    $('[title]').tooltip({
        placement: 'top',
        trigger: 'hover',
        delay: { show: 500, hide: 100 }
    });

    // ========== FUNCIONES DE BÚSQUEDA AVANZADA ==========

    // Búsqueda por DNI específico
    window.buscarPorDNI = function(dni) {
        const tabla = $('#tablaReembolsos').DataTable();
        tabla.search(dni).draw();
    };

    // Búsqueda por rango de fechas
    window.buscarPorFecha = function(fechaInicio, fechaFin) {
        const tabla = $('#tablaReembolsos').DataTable();
        // Implementar lógica de filtrado por rango de fechas
        console.log(`Buscando entre ${fechaInicio} y ${fechaFin}`);
    };

    // ========== INTEGRACIÓN CON SISTEMA DE NOTIFICACIONES ==========

    // Marcar solicitudes como "vistas"
    $(document).on('click', '.btn-aprobar, .btn-rechazar', function() {
        $(this).closest('tr').addClass('table-active');
    });

    // ========== MÉTRICAS EN TIEMPO REAL ==========

    function actualizarEstadisticasEnVivo() {
        const tabla = $('#tablaReembolsos').DataTable();
        const filas = tabla.rows({ search: 'applied' }).data();

        let totalPendientes = 0;
        let totalProcesadas = 0;
        let montoTotal = 0;

        filas.each(function(fila, index) {
            const estado = $(tabla.row(index).node()).find('td:eq(6) .badge').text().trim();
            const monto = parseFloat($(tabla.row(index).node()).find('td:eq(3)').text().replace('S/.', '').trim()) || 0;

            if (estado === 'Pendiente') {
                totalPendientes++;
                montoTotal += monto;
            } else {
                totalProcesadas++;
            }
        });

        // Actualizar contadores en la interfaz si existen
        $('.contador-pendientes').text(totalPendientes);
        $('.contador-procesadas').text(totalProcesadas);
        $('.monto-total').text('S/. ' + montoTotal.toFixed(2));
    }

    // Actualizar estadísticas cuando se apliquen filtros
    $('#tablaReembolsos').on('draw.dt', function() {
        actualizarEstadisticasEnVivo();
    });

    console.log('✅ Script adicional de reembolsos cargado correctamente');
});

// ========== FUNCIONES GLOBALES ==========

// Función para mostrar estadísticas rápidas
window.mostrarEstadisticas = function() {
    const tabla = $('#tablaReembolsos').DataTable();
    const totalFilas = tabla.rows().count();
    const filasVisibles = tabla.rows({ search: 'applied' }).count();

    Swal.fire({
        title: 'Estadísticas Rápidas',
        html: `
            <div class="text-start">
                <ul class="list-unstyled">
                    <li><strong>Total solicitudes:</strong> ${totalFilas}</li>
                    <li><strong>Mostrando:</strong> ${filasVisibles}</li>
                    <li><strong>Filtros aplicados:</strong> ${totalFilas !== filasVisibles ? 'Sí' : 'No'}</li>
                </ul>
            </div>
        `,
        icon: 'info'
    });
};

// Función para limpiar todos los filtros
window.limpiarFiltros = function() {
    const tabla = $('#tablaReembolsos').DataTable();

    // Limpiar filtros de radio buttons
    $('input[name="filtroEstado"][value="todos"]').prop('checked', true).trigger('change');
    $('input[name="filtroTiempo"][value="todos"]').prop('checked', true).trigger('change');

    // Limpiar búsqueda
    tabla.search('').draw();

    Swal.fire({
        title: 'Filtros limpiados',
        text: 'Se han restablecido todos los filtros',
        icon: 'success',
        timer: 2000,
        showConfirmButton: false
    });
};