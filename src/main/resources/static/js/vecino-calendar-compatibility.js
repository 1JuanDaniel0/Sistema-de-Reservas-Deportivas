// SCRIPT DE COMPATIBILIDAD PARA MANTENER LA FUNCIONALIDAD EXISTENTE
// Este script reemplaza reservas-calendar-events.js y reservas-calendar.js
"use strict";

// ============================================================================
// PARTE 1: Compatibilidad con reservas-calendar-events.js
// ============================================================================

// Variables globales para mantener compatibilidad
let reservasData = [];
let mantenimientosData = [];

// Función de inicialización de reservas (MANTENIDA PARA COMPATIBILIDAD)
function inicializarReservas(reservas) {
    console.log("📊 [COMPAT] inicializarReservas llamada con:", reservas.length, "reservas");

    // Delegar al nuevo procesador
    if (window.vecinoCalendarEventsProcessor) {
        const usuarioId = window.usuarioActualId || null;
        const mantenimientos = window.mantenimientosDelServidor || [];
        window.vecinoCalendarEventsProcessor.initialize(reservas, mantenimientos, usuarioId);
        reservasData = window.vecinoCalendarEventsProcessor.getReservas();
    } else {
        console.warn("⚠️ [COMPAT] vecinoCalendarEventsProcessor no disponible");
        reservasData = reservas || [];
    }
}

// Función de inicialización de mantenimientos (NUEVA COMPATIBILIDAD)
function inicializarMantenimientos(mantenimientos) {
    console.log("🔧 [COMPAT] inicializarMantenimientos llamada con:", mantenimientos.length, "mantenimientos");

    // Delegar al nuevo procesador
    if (window.vecinoCalendarEventsProcessor) {
        mantenimientosData = window.vecinoCalendarEventsProcessor.getMantenimientos();
    } else {
        console.warn("⚠️ [COMPAT] vecinoCalendarEventsProcessor no disponible");
        mantenimientosData = mantenimientos || [];
    }
}

// Función para combinar eventos (MANTENIDA PARA COMPATIBILIDAD)
function combinarEventos() {
    console.log("🔄 [COMPAT] combinarEventos llamada");

    if (window.vecinoCalendarEventsProcessor) {
        window.reservasEvents = window.vecinoCalendarEventsProcessor.getAllEvents();
    } else {
        window.reservasEvents = [...reservasData, ...mantenimientosData];
    }

    console.log("🔄 [COMPAT] Eventos combinados:", window.reservasEvents.length);

    // Notificar que los datos están listos
    window.dispatchEvent(new CustomEvent('reservasDataReady'));
}

// Funciones de validación de conflictos (MANTENIDAS PARA COMPATIBILIDAD)
function verificarConflictoMantenimiento(fechaHoraInicio, fechaHoraFin) {
    if (window.vecinoCalendarEventsProcessor) {
        return window.vecinoCalendarEventsProcessor.verificarConflictoMantenimiento(fechaHoraInicio, fechaHoraFin);
    }

    // Fallback
    return mantenimientosData.some(mantenimiento => {
        const mantenimientoInicio = new Date(mantenimiento.start);
        const mantenimientoFin = new Date(mantenimiento.end);
        return (fechaHoraInicio < mantenimientoFin && fechaHoraFin > mantenimientoInicio);
    });
}

function validarReservaContraMantenimiento(fecha, horaInicio, horaFin) {
    if (window.vecinoCalendarEventsProcessor) {
        return window.vecinoCalendarEventsProcessor.validarReservaContraMantenimiento(fecha, horaInicio, horaFin);
    }

    // Fallback
    const fechaInicioReserva = new Date(fecha + 'T' + horaInicio);
    const fechaFinReserva = new Date(fecha + 'T' + horaFin);

    const hayConflicto = verificarConflictoMantenimiento(fechaInicioReserva, fechaFinReserva);

    if (hayConflicto) {
        alert('No se puede reservar en este horario debido a mantenimiento programado.');
        return false;
    }

    return true;
}

// Exportar funciones para uso global (COMPATIBILIDAD)
window.verificarConflictoMantenimiento = verificarConflictoMantenimiento;
window.validarReservaContraMantenimiento = validarReservaContraMantenimiento;

// ============================================================================
// PARTE 2: Compatibilidad con reservas-calendar.js
// ============================================================================

let direction = "ltr";
if (isRtl) { direction = "rtl"; }

let calendar; // Variable global para el calendario (MANTENIDA)

// Función principal de inicialización del calendario (ADAPTADA)
function initializeCalendar() {
    console.log("📅 [COMPAT] initializeCalendar llamada");

    // Si el nuevo sistema está disponible, usarlo
    if (window.vecinoCalendarManager) {
        console.log("📅 [COMPAT] Delegando al nuevo sistema vecinoCalendarManager");
        if (!window.vecinoCalendarManager.isInitialized) {
            window.vecinoCalendarManager.initialize();
        }
        return;
    }

    // Fallback: sistema original simplificado
    console.log("📅 [COMPAT] Usando sistema de fallback");
    initializeCalendarFallback();
}

// Sistema de fallback simplificado
function initializeCalendarFallback() {
    console.log("📅 [COMPAT] Inicializando calendario con sistema de fallback");

    const calendarEl = document.getElementById("calendar");
    if (!calendarEl) {
        console.error("❌ [COMPAT] No se encontró el elemento del calendario");
        return;
    }

    const estadoColors = {
        'confirmada': 'success',
        'no_confirmada': 'warning',
        'cancelada': 'secondary',
        'reservado': 'danger',
        'mantenimiento': 'info'
    };

    calendar = new Calendar(calendarEl, {
        initialView: "timeGridWeek",
        locale: "es",

        events: function(fetchInfo, successCallback) {
            console.log("📊 [COMPAT] Cargando eventos (fallback)");
            const eventos = window.reservasEvents || [];

            // Aplicar filtros si están disponibles
            const activeFilters = getActiveFilters();
            if (activeFilters.length > 0) {
                const filteredEvents = eventos.filter(event => {
                    const calendar = event.extendedProps?.calendar || 'unknown';
                    return activeFilters.includes(calendar);
                });
                successCallback(filteredEvents);
            } else {
                successCallback(eventos);
            }
        },

        plugins: [dayGridPlugin, interactionPlugin, listPlugin, timegridPlugin],
        editable: false,
        dragScroll: true,
        dayMaxEvents: 3,

        slotMinTime: "08:00:00",
        slotMaxTime: "23:00:00",
        slotDuration: "00:30:00",
        slotLabelInterval: "01:00:00",
        slotLabelFormat: {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        },

        customButtons: {
            sidebarToggle: { text: "Sidebar" }
        },

        headerToolbar: {
            start: "sidebarToggle, prev,next, title",
            end: "dayGridMonth,timeGridWeek,timeGridDay,listMonth"
        },

        direction: direction,
        initialDate: new Date(),
        navLinks: true,
        selectable: true,

        eventClassNames: ({event}) => {
            const estado = event._def.extendedProps.calendar;
            return ["fc-event-" + (estadoColors[estado] || 'info')];
        },

        eventClick: function(info) {
            handleEventClickFallback(info);
        },

        dateClick: function(info) {
            handleDateClickFallback(info);
        },

        datesSet: function() {
            setupSidebarToggle();
        },

        viewDidMount: function() {
            setupSidebarToggle();
        }
    });

    calendar.render();
    setupSidebarToggle();
    setupFiltersFallback();
}

// Manejo de eventos (FALLBACK)
function handleEventClickFallback(info) {
    const props = info.event.extendedProps;

    if (props.tipo === 'reserva' && !props.esPropia) {
        console.log("Reserva ajena clickeada (fallback)");
        return;
    }

    console.log("👆 [COMPAT] Evento clickeado (fallback):", info.event);
    // Mostrar información básica
    alert(`Evento: ${info.event.title}\nTipo: ${props.tipo || 'Desconocido'}`);
}

function handleDateClickFallback(info) {
    console.log("📅 [COMPAT] Fecha clickeada (fallback):", info.dateStr);

    const fechaSeleccionada = info.date;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    if (fechaSeleccionada < hoy) {
        alert('No puedes reservar en fechas pasadas');
        return;
    }

    // Abrir offcanvas de reserva
    const crearReservaOffcanvas = new bootstrap.Offcanvas(document.getElementById('crearReservaOffcanvas'));
    crearReservaOffcanvas.show();

    const fechaStr = moment(fechaSeleccionada).format("YYYY-MM-DD");
    document.getElementById("reservaFecha").value = fechaStr;
}

// Configuración del toggle del sidebar (MANTENIDA)
function setupSidebarToggle() {
    const toggleBtn = document.querySelector(".fc-sidebarToggle-button");
    if (toggleBtn) {
        toggleBtn.classList.remove("fc-button-primary");
        toggleBtn.classList.add("d-lg-none", "d-inline-block", "ps-0");

        while (toggleBtn.firstChild) {
            toggleBtn.firstChild.remove();
        }

        toggleBtn.setAttribute("data-bs-toggle", "sidebar");
        toggleBtn.setAttribute("data-overlay", "");
        toggleBtn.setAttribute("data-target", "#app-calendar-sidebar");
        toggleBtn.insertAdjacentHTML("beforeend", '<i class="bx bx-menu bx-sm text-heading"></i>');
    }
}

// Configuración de filtros (FALLBACK)
function setupFiltersFallback() {
    const selectAllCheckbox = document.querySelector(".select-all");
    const filterInputs = document.querySelectorAll(".input-filter");

    if (selectAllCheckbox) {
        selectAllCheckbox.checked = true;
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
        input.checked = true;
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

// Función para obtener filtros activos (MANTENIDA PARA COMPATIBILIDAD)
function getActiveFilters() {
    // Intentar usar el nuevo sistema primero
    if (window.vecinoCalendarManager && window.vecinoCalendarManager.filtersManager) {
        return window.vecinoCalendarManager.filtersManager.getActiveFilters();
    }

    // Fallback: leer directamente de los checkboxes
    const activeFilters = [];
    const filterInputs = document.querySelectorAll(".input-filter:checked");
    filterInputs.forEach(input => {
        activeFilters.push(input.getAttribute("data-value"));
    });
    return activeFilters;
}

// ============================================================================
// PARTE 3: Inicialización y listeners de eventos
// ============================================================================

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("📊 [COMPAT] DOM cargado, verificando datos del servidor...");

    // Verificar si hay datos de reservas del servidor
    if (typeof window.reservasDelServidor !== 'undefined') {
        console.log("📊 [COMPAT] Datos de reservas del servidor encontrados:", window.reservasDelServidor.length);
        inicializarReservas(window.reservasDelServidor);
    } else {
        console.log("📊 [COMPAT] No se encontraron datos de reservas del servidor, inicializando con array vacío");
        inicializarReservas([]);
    }

    // Verificar si hay datos de mantenimientos del servidor
    if (typeof window.mantenimientosDelServidor !== 'undefined') {
        console.log("🔧 [COMPAT] Datos de mantenimientos del servidor encontrados:", window.mantenimientosDelServidor.length);
        inicializarMantenimientos(window.mantenimientosDelServidor);
    } else {
        console.log("🔧 [COMPAT] No se encontraron datos de mantenimientos del servidor, inicializando con array vacío");
        inicializarMantenimientos([]);
    }

    // Combinar eventos
    combinarEventos();

    // Listener para cuando los datos estén listos
    window.addEventListener('reservasDataReady', function() {
        console.log("📊 [COMPAT] Datos de reservas listos, inicializando calendario");

        // Delay para asegurar que todos los scripts estén cargados
        setTimeout(() => {
            initializeCalendar();
        }, 500);
    });

    // Listener para el nuevo evento específico de vecino
    window.addEventListener('vecinoReservasDataReady', function() {
        console.log("📊 [COMPAT] Datos específicos de vecino listos");

        setTimeout(() => {
            initializeCalendar();
        }, 500);
    });

    // Fallback: si no hay datos o eventos, inicializar de todas formas
    setTimeout(() => {
        if (!window.reservasEvents) {
            console.log("⏰ [COMPAT] Timeout: Forzando inicialización con arrays vacíos");
            window.reservasEvents = [];
            initializeCalendar();
        }
    }, 2000);
});

// ============================================================================
// PARTE 4: Funciones globales para compatibilidad
// ============================================================================

// Exportar funciones para uso global
window.inicializarReservas = inicializarReservas;
window.inicializarMantenimientos = inicializarMantenimientos;
window.combinarEventos = combinarEventos;
window.initializeCalendar = initializeCalendar;
window.getActiveFilters = getActiveFilters;

// Funciones para llenar el offcanvas (COMPATIBILIDAD CON calendario-logica.js)
function llenarOffcanvasReserva(fecha) {
    document.getElementById('reservaFecha').value = fecha;

    // Llamar a la función de cálculo de costo si existe
    if (window.calcularCostoTotal) {
        window.calcularCostoTotal();
    }

    // Resetear el botón
    const btnPagar = document.getElementById('btnPagarReservar');
    if (btnPagar) {
        btnPagar.textContent = 'Seleccione tipo de pago';
        btnPagar.className = 'btn btn-secondary w-100';
        btnPagar.disabled = false;
    }

    // Toast informativo si la función existe
    if (window.mostrarToastAvanzado) {
        window.mostrarToastAvanzado(`Fecha seleccionada: ${fecha}. Configure los detalles de su reserva.`, 'info', {
            duracion: 3000
        });
    }
}

// Exponer la función globalmente para compatibilidad
window.llenarOffcanvasReserva = llenarOffcanvasReserva;

// Función de limpieza del offcanvas cuando se cierra
document.addEventListener('DOMContentLoaded', function() {
    const eventSidebar = document.getElementById('addEventSidebar');
    if (eventSidebar) {
        eventSidebar.addEventListener('hidden.bs.offcanvas', function() {
            // Limpiar campos del sidebar
            const eventTitle = document.getElementById('eventTitle');
            const eventStartDate = document.getElementById('eventStartDate');
            const eventEndDate = document.getElementById('eventEndDate');
            const eventLocation = document.getElementById('eventLocation');
            const eventDescription = document.getElementById('eventDescription');

            if (eventTitle) {
                eventTitle.disabled = false;
                eventTitle.value = '';
            }
            if (eventStartDate) {
                eventStartDate.disabled = false;
                eventStartDate.value = '';
            }
            if (eventEndDate) {
                eventEndDate.disabled = false;
                eventEndDate.value = '';
            }
            if (eventLocation) {
                eventLocation.disabled = false;
                eventLocation.value = '';
            }
            if (eventDescription) {
                eventDescription.disabled = false;
                eventDescription.value = '';
            }
        });
    }
});

console.log("✅ [COMPAT] Script de compatibilidad cargado completamente");