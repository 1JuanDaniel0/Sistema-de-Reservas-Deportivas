// SCRIPT CORREGIDO PARA MANTENIMIENTO ADMIN - SIN REFRESH INFINITO
"use strict";

let direction = "ltr";
if (isRtl) { direction = "rtl"; }

let calendar; // Variable global para el calendario
let espacioActual = null;
let flatpickrInicio, flatpickrFin; // Variables para los date pickers

function initializeMaintenanceCalendar() {
    console.log("Inicializando calendario de mantenimiento...");
    console.log("Eventos disponibles:", window.reservasEvents);

    const calendarEl = document.getElementById("calendar");
    const sidebar = document.querySelector(".app-calendar-sidebar");
    const overlay = document.querySelector(".app-overlay");

    // Colores para diferentes estados
    const estadoColors = {
        'confirmada': 'success',
        'no_confirmada': 'warning',
        'cancelada': 'danger',
        'mantenimiento': 'info'
    };

    // Función para obtener filtros activos
    function getActiveFilters() {
        const activeFilters = [];
        const filterInputs = document.querySelectorAll(".input-filter:checked");
        filterInputs.forEach(input => {
            activeFilters.push(input.getAttribute("data-value"));
        });
        return activeFilters;
    }

    // Función para configurar el botón toggle del sidebar
    function setupSidebarToggle() {
        const toggleBtn = document.querySelector(".fc-sidebarToggle-button");
        if (toggleBtn) {
            toggleBtn.classList.remove("fc-button-primary");
            toggleBtn.classList.add("d-lg-none", "d-inline-block", "ps-0");

            // Limpiar contenido existente
            while (toggleBtn.firstChild) {
                toggleBtn.firstChild.remove();
            }

            toggleBtn.setAttribute("data-bs-toggle", "sidebar");
            toggleBtn.setAttribute("data-overlay", "");
            toggleBtn.setAttribute("data-target", "#app-calendar-sidebar");
            toggleBtn.insertAdjacentHTML("beforeend", '<i class="bx bx-menu bx-sm text-heading"></i>');
        }
    }

    // Inicializar el calendario
    calendar = new Calendar(calendarEl, {
        initialView: "dayGridMonth",
        locale: "es",

        events: function(fetchInfo, successCallback) {
            console.log("Función events llamada");

            // Asegurarse de que la variable existe
            if (!window.reservasEvents) {
                console.log("No hay eventos disponibles, inicializando array vacío");
                window.reservasEvents = [];
            }

            console.log("Eventos disponibles:", window.reservasEvents);

            // Si no hay eventos, devolver array vacío
            if (window.reservasEvents.length === 0) {
                console.log("No hay eventos para mostrar");
                successCallback([]);
                return;
            }

            // Filtrar eventos según los filtros seleccionados
            const activeFilters = getActiveFilters();
            console.log("Filtros activos:", activeFilters);

            // Si no hay filtros activos, mostrar todos los eventos
            if (activeFilters.length === 0) {
                console.log("No hay filtros activos, mostrando todos los eventos");
                successCallback(window.reservasEvents);
                return;
            }

            const filteredEvents = window.reservasEvents.filter(function(event) {
                const isIncluded = activeFilters.includes(event.extendedProps.calendar);
                console.log(`Evento ${event.title} - calendario: ${event.extendedProps.calendar} - incluido: ${isIncluded}`);
                return isIncluded;
            });

            console.log("Eventos filtrados:", filteredEvents);
            successCallback(filteredEvents);
        },

        plugins: [dayGridPlugin, interactionPlugin, listPlugin, timegridPlugin],
        editable: false,
        dragScroll: true,
        dayMaxEvents: 3,

        customButtons: {
            sidebarToggle: {
                text: "Sidebar"
            }
        },

        headerToolbar: {
            start: "sidebarToggle, prev,next, title",
            end: "dayGridMonth,timeGridWeek,timeGridDay,listMonth"
        },

        direction: direction,
        initialDate: new Date(),
        navLinks: true,
        selectable: true,
        selectMirror: true,

        eventClassNames: function({event}) {
            const estado = event._def.extendedProps.calendar;
            return ["fc-event-" + (estadoColors[estado] || 'info')];
        },

        eventClick: function(info) {
            console.log("Evento clickeado:", info.event);

            // Solo mostrar información, no permitir edición
            const props = info.event.extendedProps;

            // Mostrar información en un toast o modal simple
            showEventInfo(info.event);
        },

        dateClick: function(info) {
            console.log("Fecha clickeada:", info.dateStr);
            const fechaSeleccionada = info.date;
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            // No permitir seleccionar días pasados
            if (fechaSeleccionada < hoy) {
                console.log("Fecha pasada, ignorando.");
                showToast('No puedes programar mantenimiento en fechas pasadas', 'warning');
                return;
            }

            // Abrir modal de mantenimiento
            openMaintenanceModal(fechaSeleccionada);
        },

        select: function(info) {
            console.log("Rango seleccionado:", info);
            const fechaInicio = info.start;
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            // No permitir seleccionar días pasados
            if (fechaInicio < hoy) {
                console.log("Rango incluye fechas pasadas, ignorando.");
                showToast('No puedes programar mantenimiento en fechas pasadas', 'warning');
                calendar.unselect();
                return;
            }

            // Abrir modal con rango preseleccionado
            openMaintenanceModal(fechaInicio, info.end);
        },

        datesSet: function() {
            setupSidebarToggle();
        },

        viewDidMount: function() {
            setupSidebarToggle();
        },

        eventDidMount: function(info) {
            const props = info.event.extendedProps;

            // Crear tooltip con información del evento
            let tooltipContent = '';
            if (props.calendar === 'mantenimiento') {
                tooltipContent = `
                    Mantenimiento Programado
                    Tipo: ${props.tipo || 'No especificado'}
                    Responsable: ${props.responsable || 'No asignado'}
                    Prioridad: ${props.prioridad || 'Media'}
                `;
            } else {
                const horaInicio = info.event.start.toLocaleTimeString('es-PE', { hour: 'numeric', hour12: true });
                const horaFin = info.event.end.toLocaleTimeString('es-PE', { hour: 'numeric', hour12: true });
                tooltipContent = `
                    ${props.estado} | ${props.vecino}
                    ${horaInicio} a ${horaFin}
                    Costo: S/. ${props.costo}
                    Pago: ${props.tipoPago}
                `;
            }

            new bootstrap.Tooltip(info.el, {
                title: tooltipContent.trim(),
                placement: "top",
                trigger: "hover",
                container: "body",
                animation: true,
                customClass: "tooltip-fade-reserva"
            });
        }
    });

    console.log("Renderizando calendario...");
    calendar.render();
    setupSidebarToggle();

    // Configurar filtros
    setupFilters();

    // Configurar selector de espacios
    setupSpaceSelector();

    // Configurar modal de mantenimiento
    setupMaintenanceModal();
}

function setupFilters() {
    const selectAllCheckbox = document.querySelector(".select-all");
    const filterInputs = document.querySelectorAll(".input-filter");

    // Marcar todos los filtros como activos por defecto
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = true;
    }
    filterInputs.forEach(input => {
        input.checked = true;
    });

    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener("click", function() {
            const isChecked = this.checked;
            filterInputs.forEach(input => {
                input.checked = isChecked;
            });
            if (calendar) {
                calendar.refetchEvents();
            }
        });
    }

    filterInputs.forEach(input => {
        input.addEventListener("click", function() {
            const checkedInputs = document.querySelectorAll(".input-filter:checked");
            if (selectAllCheckbox) {
                selectAllCheckbox.checked = checkedInputs.length === filterInputs.length;
            }
            if (calendar) {
                calendar.refetchEvents();
            }
        });
    });
}

function setupSpaceSelector() {
    const espacioSelector = document.getElementById('espacioSelector');

    if (espacioSelector && window.espaciosData) {
        // Inicializar Select2
        $(espacioSelector).select2({
            placeholder: 'Selecciona un espacio...',
            allowClear: false
        });

        // Solo configurar el cambio si no hay un espacio ya seleccionado en la URL
        const urlParams = new URLSearchParams(window.location.search);
        const espacioEnUrl = urlParams.get('idEspacio');

        if (espacioEnUrl) {
            // Ya hay un espacio en la URL, solo actualizar la selección
            $(espacioSelector).val(espacioEnUrl);
            const espacioSeleccionado = window.espaciosData.find(e => e.idEspacio === espacioEnUrl);
            if (espacioSeleccionado) {
                updateSpaceInfo(espacioSeleccionado);
            }
        } else {
            // No hay espacio en URL, seleccionar el primero
            if (window.espaciosData.length > 0) {
                const primerEspacio = window.espaciosData[0];
                $(espacioSelector).val(primerEspacio.idEspacio);
                updateSpaceInfo(primerEspacio);
                // Actualizar URL sin refrescar
                const newUrl = new URL(window.location);
                newUrl.searchParams.set('idEspacio', primerEspacio.idEspacio);
                window.history.replaceState({}, '', newUrl);
            }
        }

        // Configurar el evento change DESPUÉS de la inicialización
        $(espacioSelector).off('change.mantenimiento').on('change.mantenimiento', function() {
            const espacioId = this.value;
            const espacioActualEnUrl = urlParams.get('idEspacio');

            // Solo cambiar si es diferente al actual
            if (espacioId && espacioId !== espacioActualEnUrl) {
                changeSpace(espacioId);
            }
        });
    }
}

function changeSpace(espacioId) {
    console.log("Cambiando a espacio:", espacioId);

    // Mostrar loading
    showToast('Cargando reservas del espacio...', 'info');

    // Redirigir con el nuevo espacio
    const url = new URL(window.location);
    url.searchParams.set('idEspacio', espacioId);
    window.location.href = url.toString();
}

function updateSpaceInfo(espacio) {
    // Actualizar información del espacio en el sidebar
    document.getElementById('espacioNombre').textContent = espacio.nombre || '-';
    document.getElementById('espacioLugar').textContent = espacio.idLugar?.lugar || '-';
    document.getElementById('espacioCosto').textContent = espacio.costo ? espacio.costo.toFixed(2) : '-';

    espacioActual = espacio;
}

function setupMaintenanceModal() {
    // Inicializar Flatpickr para las fechas
    flatpickrInicio = flatpickr("#fechaInicioMantenimiento", {
        locale: "es",
        dateFormat: "Y-m-d",
        minDate: "today",
        onChange: function(selectedDates) {
            if (selectedDates.length > 0) {
                // Actualizar fecha mínima del fin
                flatpickrFin.set('minDate', selectedDates[0]);
            }
        }
    });

    flatpickrFin = flatpickr("#fechaFinMantenimiento", {
        locale: "es",
        dateFormat: "Y-m-d",
        minDate: "today"
    });

    // Configurar validación de horas
    document.getElementById('horaInicioMantenimiento').addEventListener('change', validateHours);
    document.getElementById('horaFinMantenimiento').addEventListener('change', validateHours);

    // Configurar botón de guardar
    document.getElementById('btnGuardarMantenimiento').addEventListener('click', saveMaintenancePlan);
}

function openMaintenanceModal(fechaInicio, fechaFin = null) {
    const modal = new bootstrap.Modal(document.getElementById('modalMantenimiento'));

    // Limpiar formulario
    document.getElementById('formMantenimiento').reset();

    // Configurar fechas
    const fechaInicioStr = moment(fechaInicio).format('YYYY-MM-DD');
    flatpickrInicio.setDate(fechaInicioStr);

    if (fechaFin) {
        const fechaFinStr = moment(fechaFin).subtract(1, 'day').format('YYYY-MM-DD');
        flatpickrFin.setDate(fechaFinStr);
    } else {
        flatpickrFin.setDate(fechaInicioStr);
    }

    // Actualizar información del espacio en el modal
    if (espacioActual) {
        document.getElementById('modalEspacioNombre').textContent = espacioActual.nombre;
        document.getElementById('modalEspacioLugar').textContent = espacioActual.idLugar?.lugar || '-';
    }

    modal.show();
}

// ACTUALIZAR LA FUNCIÓN DE VALIDACIÓN DE HORAS
function validateHours() {
    const horaInicio = document.getElementById('horaInicioMantenimiento').value;
    const horaFin = document.getElementById('horaFinMantenimiento').value;

    if (horaInicio && horaFin) {
        if (horaInicio >= horaFin) {
            showToast('La hora de fin debe ser posterior a la hora de inicio', 'warning');
            document.getElementById('horaFinMantenimiento').value = '';
            return false;
        }

        // Validar horario de operación (8 AM - 10 PM)
        const horaInicioNum = parseInt(horaInicio.split(':')[0]);
        const horaFinNum = parseInt(horaFin.split(':')[0]);

        if (horaInicioNum < 8 || horaInicioNum > 22) {
            showToast('El horario de operación es de 8:00 AM a 10:00 PM', 'warning');
            document.getElementById('horaInicioMantenimiento').value = '';
            return false;
        }

        if (horaFinNum < 8 || horaFinNum > 22) {
            showToast('El horario de operación es de 8:00 AM a 10:00 PM', 'warning');
            document.getElementById('horaFinMantenimiento').value = '';
            return false;
        }
    }

    return true;
}

function saveMaintenancePlan() {
    const form = document.getElementById('formMantenimiento');

    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    // Mostrar loading en el botón
    const btnGuardar = document.getElementById('btnGuardarMantenimiento');
    const textoOriginal = btnGuardar.innerHTML;
    btnGuardar.disabled = true;
    btnGuardar.innerHTML = '<i class="bx bx-loader-alt bx-spin me-1"></i>Programando...';

    // Recopilar datos del formulario con formato correcto
    const horaInicioValue = document.getElementById('horaInicioMantenimiento').value;
    const horaFinValue = document.getElementById('horaFinMantenimiento').value;

    // Asegurar formato HH:mm (agregar segundos si es necesario)
    const formatTime = (timeString) => {
        if (timeString && timeString.length === 5) {
            return timeString; // Ya está en formato HH:mm
        }
        return timeString;
    };

    const data = {
        espacioId: espacioActual?.idEspacio,
        fechaInicio: document.getElementById('fechaInicioMantenimiento').value, // yyyy-MM-dd
        fechaFin: document.getElementById('fechaFinMantenimiento').value,       // yyyy-MM-dd
        horaInicio: formatTime(horaInicioValue),                               // HH:mm
        horaFin: formatTime(horaFinValue),                                     // HH:mm
        tipoMantenimiento: document.getElementById('tipoMantenimiento').value,
        prioridad: document.getElementById('prioridadMantenimiento').value,
        descripcion: document.getElementById('descripcionMantenimiento').value,
        responsableId: document.getElementById('responsableMantenimiento').value || null,
        costoEstimado: parseFloat(document.getElementById('costoMantenimiento').value) || null
    };

    console.log("Datos del mantenimiento a enviar:", data);

    // Validaciones adicionales en el frontend
    if (!data.espacioId) {
        showToast('Debe seleccionar un espacio', 'error');
        resetButton();
        return;
    }

    if (!data.fechaInicio || !data.fechaFin) {
        showToast('Las fechas de inicio y fin son obligatorias', 'error');
        resetButton();
        return;
    }

    if (!data.horaInicio || !data.horaFin) {
        showToast('Las horas de inicio y fin son obligatorias', 'error');
        resetButton();
        return;
    }

    // Validar que la hora fin sea posterior a la hora inicio
    if (data.horaInicio >= data.horaFin) {
        showToast('La hora de fin debe ser posterior a la hora de inicio', 'error');
        resetButton();
        return;
    }

    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // Enviar datos al servidor
    fetch('/admin/api/mantenimiento/programar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            console.log('Respuesta del servidor:', response.status);

            // Verificar si la respuesta es JSON válida
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return response.json();
        })
        .then(result => {
            console.log('Resultado:', result);

            if (result.success) {
                // Éxito
                showToast(`✅ ${result.message}`, 'success', 4000);

                // Cerrar modal
                bootstrap.Modal.getInstance(document.getElementById('modalMantenimiento')).hide();

                // Refrescar calendario para mostrar el nuevo mantenimiento
                setTimeout(() => {
                    const url = new URL(window.location);
                    window.location.href = url.toString();
                }, 1500);

            } else {
                // Error del servidor
                showToast(`❌ ${result.error || 'Error desconocido'}`, 'error', 6000);
            }
        })
        .catch(error => {
            console.error('Error en la petición:', error);

            // Mensajes de error más específicos
            if (error.message.includes('500')) {
                showToast('❌ Error interno del servidor. Revisa la consola para más detalles.', 'error', 7000);
            } else if (error.message.includes('400')) {
                showToast('❌ Datos de entrada inválidos. Verifica el formulario.', 'error', 6000);
            } else {
                showToast('❌ Error de conexión con el servidor', 'error', 5000);
            }
        })
        .finally(() => {
            resetButton();
        });

    function resetButton() {
        btnGuardar.disabled = false;
        btnGuardar.innerHTML = textoOriginal;
    }
}

// Verificar conflictos antes de guardar (opcional)
function verificarConflictosMantenimiento(data) {
    return new Promise((resolve, reject) => {
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        fetch('/admin/api/mantenimiento/verificar-conflictos', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(data)
        })
            .then(response => response.json())
            .then(result => {
                if (result.hayConflictos) {
                    reject(new Error(result.message));
                } else {
                    resolve(result);
                }
            })
            .catch(error => {
                reject(error);
            });
    });
}

function showEventInfo(event) {
    const props = event.extendedProps;
    let message = '';

    if (props.calendar === 'mantenimiento') {
        message = `
            <strong>Mantenimiento Programado</strong><br>
            Tipo: ${props.tipo || 'No especificado'}<br>
            Responsable: ${props.responsable || 'No asignado'}<br>
            Prioridad: ${props.prioridad || 'Media'}<br>
            Descripción: ${props.descripcion || 'Sin descripción'}
        `;
    } else {
        const inicio = moment(event.start).format('HH:mm');
        const fin = moment(event.end).format('HH:mm');
        message = `
            <strong>Reserva: ${props.estado}</strong><br>
            Vecino: ${props.vecino}<br>
            Horario: ${inicio} - ${fin}<br>
            Costo: S/. ${props.costo}<br>
            Tipo de Pago: ${props.tipoPago}
        `;
    }

    showToast(message, 'info', 5000);
}

// ACTUALIZAR LA FUNCIÓN showToast para manejar diferentes tipos
function showToast(message, type = 'info', duration = 3000) {
    // Mapear tipos para Bootstrap
    const typeMap = {
        'error': 'danger',
        'success': 'success',
        'warning': 'warning',
        'info': 'info'
    };

    const bootstrapType = typeMap[type] || 'info';

    // Crear toast dinámicamente
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-${bootstrapType} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    // Crear contenedor si no existe
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '9999';
        document.body.appendChild(toastContainer);
    }

    // Agregar toast
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    // Mostrar toast
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: duration });
    toast.show();

    // Limpiar después de mostrar
    toastElement.addEventListener('hidden.bs.toast', function() {
        this.remove();
    });
}

// 🔧 CORREGIDO: Función mejorada para buscar el espacio actual
function findCurrentSpace() {
    if (window.espaciosData && window.espacioActualId) {
        const espacio = window.espaciosData.find(e => e.idEspacio == window.espacioActualId);
        if (espacio) {
            updateSpaceInfo(espacio);
            return;
        }
    }

    // Fallback: obtener de la URL
    const urlParams = new URLSearchParams(window.location.search);
    const espacioEnUrl = urlParams.get('idEspacio');
    if (espacioEnUrl && window.espaciosData) {
        const espacio = window.espaciosData.find(e => e.idEspacio == espacioEnUrl);
        if (espacio) {
            updateSpaceInfo(espacio);
        }
    }
}

// Inicializar cuando los datos estén listos
document.addEventListener("DOMContentLoaded", function() {
    console.log("DOM cargado para calendario de mantenimiento");

    // Buscar espacio actual
    findCurrentSpace();

    // Escuchar el evento de datos listos
    window.addEventListener('reservasDataReady', function() {
        console.log("Datos de reservas listos, inicializando calendario");
        initializeMaintenanceCalendar();
    });

    // Fallback: si los datos ya están disponibles
    setTimeout(function() {
        if (typeof window.reservasEvents !== 'undefined') {
            console.log("Datos ya disponibles, inicializando calendario inmediatamente");
            initializeMaintenanceCalendar();
        } else {
            console.log("No hay datos disponibles, inicializando calendario con array vacío");
            window.reservasEvents = [];
            initializeMaintenanceCalendar();
        }
    }, 1000);
});