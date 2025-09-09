// CALENDARIO PRINCIPAL PARA VECINO - VERSIÓN ADAPTADA DEL ADMIN
"use strict";

let direction = "ltr";
if (isRtl) { direction = "rtl"; }

let calendar; // Variable global para el calendario
let filtersManager; // Gestor de filtros

class VecinoCalendarManager {
    constructor() {
        this.calendar = null;
        this.espacioActual = null;
        this.filtersManager = null;
        this.eventsProcessor = null;
        this.isInitialized = false;
        this.usuarioActual = null;
    }

    async initialize() {
        console.log("🚀 [VECINO] Inicializando calendario principal");

        try {
            // Esperar a que los datos estén listos
            await this.waitForData();

            // Inicializar componentes
            this.setupUserInfo();
            this.initializeFiltersManager();
            this.initializeCalendar();
            this.setupEventListeners();

            this.isInitialized = true;
            console.log("✅ [VECINO] Calendario inicializado correctamente");

        } catch (error) {
            console.error("❌ [VECINO] Error inicializando calendario:", error);
            this.showToast('Error inicializando el calendario', 'error');
        }
    }

    waitForData() {
        return new Promise((resolve) => {
            if (window.vecinoCalendarEventsProcessor && window.vecinoCalendarEventsProcessor.isDataReady) {
                console.log("📊 [VECINO] Datos ya están listos");
                this.eventsProcessor = window.vecinoCalendarEventsProcessor;
                resolve();
                return;
            }

            // Escuchar el evento de datos listos
            const handler = () => {
                console.log("📊 [VECINO] Datos recibidos via evento");
                this.eventsProcessor = window.vecinoCalendarEventsProcessor;
                window.removeEventListener('vecinoReservasDataReady', handler);
                resolve();
            };

            window.addEventListener('vecinoReservasDataReady', handler);

            // Timeout de fallback
            setTimeout(() => {
                if (!this.eventsProcessor) {
                    console.log("⏰ [VECINO] Timeout: Inicializando con datos vacíos");
                    this.eventsProcessor = window.vecinoCalendarEventsProcessor || { getAllEvents: () => [] };
                    window.removeEventListener('vecinoReservasDataReady', handler);
                    resolve();
                }
            }, 2000);
        });
    }

    setupUserInfo() {
        // Obtener información del usuario actual
        if (window.usuarioActualId) {
            this.usuarioActual = {
                id: window.usuarioActualId,
                nombre: window.usuarioSesionNombre || 'Usuario'
            };
        }

        // Actualizar información del espacio actual
        if (window.espacioSeleccionadoNombre) {
            this.espacioActual = {
                nombre: window.espacioSeleccionadoNombre,
                costo: window.espacioSeleccionadoCostoHora || 0
            };
        }
    }

    initializeFiltersManager() {
        console.log("🔧 [VECINO] Inicializando gestor de filtros");

        this.filtersManager = new VecinoCalendarFiltersManager();

        // Escuchar cambios en los filtros
        window.addEventListener('vecinoCalendarFiltersChanged', (event) => {
            this.handleFiltersChanged(event.detail);
        });

        // Mantener compatibilidad con el evento original
        window.addEventListener('calendarFiltersChanged', (event) => {
            this.handleFiltersChanged(event.detail);
        });

        // Actualizar contador de eventos inicial
        this.updateEventCounter();
    }

    handleFiltersChanged(filterData) {
        console.log("🔄 [VECINO] Filtros cambiados:", filterData.activeFilters);

        // Refrescar eventos del calendario
        if (this.calendar) {
            this.calendar.refetchEvents();
        }

        // Actualizar contador
        this.updateEventCounter();
    }

    updateEventCounter() {
        // Esta función se mantiene para compatibilidad, pero no es crítica para vecino
        if (!this.eventsProcessor) return;

        const allEvents = this.eventsProcessor.getAllEvents();
        const stats = this.eventsProcessor.getStats();

        console.log("📊 [VECINO] Estadísticas del calendario:", stats);
    }

    initializeCalendar() {
        console.log("📅 [VECINO] Inicializando FullCalendar");

        const calendarEl = document.getElementById("calendar");
        if (!calendarEl) {
            throw new Error("No se encontró el elemento del calendario");
        }

        // Colores para diferentes estados
        const estadoColors = {
            'confirmada': 'success',
            'no_confirmada': 'warning',
            'cancelada': 'secondary',
            'reservado': 'danger',
            'mantenimiento': 'info'
        };

        this.calendar = new Calendar(calendarEl, {
            initialView: "timeGridWeek", // Empezar en vista semanal como el original
            locale: "es",

            events: (fetchInfo, successCallback) => {
                console.log("📊 [VECINO] Cargando eventos para el calendario");

                try {
                    const allEvents = this.eventsProcessor?.getAllEvents() || [];
                    const filteredEvents = this.filtersManager?.filterEvents(allEvents) || allEvents;

                    console.log(`📊 [VECINO] Eventos cargados: ${filteredEvents.length} de ${allEvents.length}`);
                    successCallback(filteredEvents);
                } catch (error) {
                    console.error("❌ [VECINO] Error cargando eventos:", error);
                    successCallback([]);
                }
            },

            plugins: [dayGridPlugin, interactionPlugin, listPlugin, timegridPlugin],
            editable: false,
            dragScroll: true,
            dayMaxEvents: 3,

            // Configuración de horarios
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

            eventClassNames: ({event}) => {
                const estado = event._def.extendedProps.calendar;
                return ["fc-event-" + (estadoColors[estado] || 'info')];
            },

            eventClick: (info) => {
                this.handleEventClick(info);
            },

            dateClick: (info) => {
                this.handleDateClick(info);
            },

            select: (info) => {
                this.handleDateSelect(info);
            },

            selectAllow: (selectInfo) => {
                return this.isSelectionAllowed(selectInfo);
            },

            datesSet: () => {
                this.setupSidebarToggle();
            },

            viewDidMount: () => {
                this.setupSidebarToggle();
            },

            eventDidMount: (info) => {
                this.setupEventTooltip(info);
            },

            dayCellDidMount: (info) => {
                this.setupDayCell(info);
            }
        });

        console.log("📅 [VECINO] Renderizando calendario...");
        this.calendar.render();
        this.setupSidebarToggle();

        // Hacer accesible globalmente para compatibilidad
        window.calendar = this.calendar;
    }

    isSelectionAllowed(selectInfo) {
        const ahora = new Date();
        const hoy = ahora.toISOString().split("T")[0];
        const inicio = selectInfo.start;
        const esHoy = inicio.toISOString().startsWith(hoy);

        // Si es de hoy y la hora seleccionada es anterior a la actual
        if (esHoy && inicio < ahora) {
            console.log("Bloqueado: intervalo pasado");
            return false;
        }

        // Si es día pasado
        if (selectInfo.start < new Date().setHours(0, 0, 0, 0)) {
            console.log("Bloqueado: fecha pasada");
            return false;
        }

        return true;
    }

    handleEventClick(info) {
        const props = info.event.extendedProps;

        // Solo mostrar detalles si es reserva propia
        if (props.tipo === 'reserva' && !props.esPropia) {
            console.log("Reserva ajena clickeada. No se mostrará información detallada.");
            this.showToast('Esta reserva pertenece a otro usuario', 'info', 3000);
            return;
        }

        console.log("👆 [VECINO] Evento clickeado:", info.event);
        this.showEventInfo(info.event);
    }

    showEventInfo(event) {
        const props = event.extendedProps;
        let message = '';

        if (props.tipo === 'mantenimiento') {
            message = `
                <div class="event-info-modal">
                    <h6><i class="bx bx-wrench me-2"></i><strong>Mantenimiento Programado</strong></h6>
                    <p><strong>Tipo:</strong> ${props.tipoMantenimiento || 'No especificado'}</p>
                    <p><strong>Prioridad:</strong> <span class="badge bg-${this.getPriorityColor(props.prioridad)}">${props.prioridad || 'Media'}</span></p>
                    <p class="text-muted"><small>El espacio no estará disponible durante este período</small></p>
                </div>
            `;
        } else if (props.esPropia) {
            const inicio = moment(event.start).format('HH:mm');
            const fin = moment(event.end).format('HH:mm');
            message = `
                <div class="event-info-modal">
                    <h6><i class="bx bx-calendar-check me-2"></i><strong>Mi Reserva</strong></h6>
                    <p><strong>Estado:</strong> <span class="badge bg-${this.getEstadoColor(props.estado)}">${props.estado}</span></p>
                    <p><strong>Horario:</strong> ${inicio} - ${fin}</p>
                    <p><strong>Costo:</strong> S/. ${props.costo}</p>
                    <p><strong>Tipo de Pago:</strong> ${props.tipoPago}</p>
                    <p><strong>Espacio:</strong> ${props.espacioNombre}</p>
                </div>
            `;
        }

        this.showToast(message, 'info', 8000);
    }

    getPriorityColor(prioridad) {
        const colorMap = {
            'BAJA': 'info',
            'MEDIA': 'warning',
            'ALTA': 'danger',
            'URGENTE': 'dark'
        };
        return colorMap[prioridad] || 'secondary';
    }

    getEstadoColor(estado) {
        const colorMap = {
            'Confirmada': 'success',
            'Pendiente de confirmación': 'warning',
            'No confirmada': 'warning',
            'Cancelada': 'secondary'
        };
        return colorMap[estado] || 'info';
    }

    handleDateClick(info) {
        console.log("📅 [VECINO] Fecha clickeada:", info.dateStr);
        this.openReservationOffcanvas(info.date);
    }

    handleDateSelect(info) {
        console.log("📅 [VECINO] Rango seleccionado:", info);
        this.openReservationOffcanvas(info.start);
    }

    openReservationOffcanvas(fecha) {
        const fechaSeleccionada = new Date(fecha);
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        // Validar que no sea fecha pasada
        if (fechaSeleccionada < hoy) {
            this.showToast('No puedes reservar en fechas pasadas', 'warning');
            return;
        }

        // Verificar conflictos con mantenimiento antes de abrir el offcanvas
        const fechaStr = moment(fechaSeleccionada).format("YYYY-MM-DD");

        // Abrir el offcanvas de creación de reserva (código existente)
        const crearReservaOffcanvas = new bootstrap.Offcanvas(document.getElementById('crearReservaOffcanvas'));
        crearReservaOffcanvas.show();

        // Llamar a la función existente que maneja el llenado del offcanvas
        if (window.llenarOffcanvasReserva) {
            window.llenarOffcanvasReserva(fechaStr);
        } else {
            // Fallback manual
            document.getElementById("reservaFecha").value = fechaStr;
            this.showToast(`Fecha seleccionada: ${fechaStr}. Configure los detalles de su reserva.`, 'info');
        }
    }

    setupEventTooltip(info) {
        const props = info.event.extendedProps;
        let tooltipContent = '';

        if (props.tipo === 'mantenimiento') {
            tooltipContent = `
                🔧 ${props.tipoMantenimiento || 'Mantenimiento'}
                Prioridad: ${props.prioridad || 'Media'}
                El espacio no estará disponible
            `;
        } else if (props.esPropia) {
            const horaInicio = info.event.start.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true });
            const horaFin = info.event.end.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true });
            tooltipContent = `
                Mi Reserva - ${props.estado}
                ${horaInicio} a ${horaFin}
                Costo: S/. ${props.costo}
                Pago: ${props.tipoPago}
            `;
        } else {
            const horaInicio = info.event.start.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true });
            const horaFin = info.event.end.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true });
            tooltipContent = `
                Espacio Ocupado
                ${horaInicio} a ${horaFin}
                No disponible para reserva
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

    setupDayCell(info) {
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        const fechaCelda = new Date(info.date);
        fechaCelda.setHours(0, 0, 0, 0);

        if (fechaCelda < hoy) {
            info.el.classList.add("fc-day-disabled");
        }
    }

    setupSidebarToggle() {
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

    setupEventListeners() {
        // Configurar filtros existentes (compatibilidad con el código existente)
        this.setupExistingFilters();
    }

    setupExistingFilters() {
        const selectAllCheckbox = document.querySelector(".select-all");
        const filterInputs = document.querySelectorAll(".input-filter");

        // Marcar todos los filtros como activos por defecto si no está gestionado por el nuevo sistema
        if (selectAllCheckbox && !this.filtersManager) {
            selectAllCheckbox.checked = true;
            filterInputs.forEach(input => {
                input.checked = true;
            });

            selectAllCheckbox.addEventListener("click", () => {
                const isChecked = selectAllCheckbox.checked;
                filterInputs.forEach(input => {
                    input.checked = isChecked;
                });
                if (this.calendar) {
                    this.calendar.refetchEvents();
                }
            });

            filterInputs.forEach(input => {
                input.addEventListener("click", () => {
                    const checkedInputs = document.querySelectorAll(".input-filter:checked");
                    selectAllCheckbox.checked = checkedInputs.length === filterInputs.length;
                    if (this.calendar) {
                        this.calendar.refetchEvents();
                    }
                });
            });
        }
    }

    showToast(message, type = 'info', duration = 3000) {
        // Usar la función avanzada existente si está disponible
        if (window.mostrarToastAvanzado) {
            window.mostrarToastAvanzado(message, type, { duracion: duration });
            return;
        }

        // Fallback simple
        console.log(`Toast [${type}]: ${message}`);

        // Crear toast simple si no existe la función avanzada
        const typeMap = {
            'error': 'danger',
            'success': 'success',
            'warning': 'warning',
            'info': 'info'
        };

        const bootstrapType = typeMap[type] || 'info';
        const toastId = 'toast-' + Date.now();

        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${bootstrapType} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;

        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: duration });
        toast.show();

        toastElement.addEventListener('hidden.bs.toast', function() {
            this.remove();
        });
    }

    // Métodos públicos para acceso externo
    getCalendar() {
        return this.calendar;
    }

    getFiltersManager() {
        return this.filtersManager;
    }

    refreshCalendar() {
        if (this.calendar) {
            this.calendar.refetchEvents();
        }
    }

    destroy() {
        if (this.calendar) {
            this.calendar.destroy();
        }
        if (this.filtersManager) {
            this.filtersManager.destroy();
        }
    }
}

// Instancia global del gestor del calendario para vecino
let vecinoCalendarManager;

// Función de inicialización que mantiene compatibilidad con el código existente
function initializeCalendar() {
    console.log("📅 [VECINO] Función initializeCalendar llamada (compatibilidad)");

    if (!vecinoCalendarManager || !vecinoCalendarManager.isInitialized) {
        console.log("📅 [VECINO] Inicializando gestor principal...");
        vecinoCalendarManager = new VecinoCalendarManager();
        vecinoCalendarManager.initialize();
    } else {
        console.log("📅 [VECINO] Gestor ya inicializado, refrescando...");
        vecinoCalendarManager.refreshCalendar();
    }
}

// Función para obtener filtros activos (compatibilidad)
function getActiveFilters() {
    if (vecinoCalendarManager && vecinoCalendarManager.filtersManager) {
        return vecinoCalendarManager.filtersManager.getActiveFilters();
    }

    // Fallback para compatibilidad con código existente
    const activeFilters = [];
    const filterInputs = document.querySelectorAll(".input-filter:checked");
    filterInputs.forEach(input => {
        activeFilters.push(input.getAttribute("data-value"));
    });
    return activeFilters;
}

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("🚀 [VECINO] DOM cargado, inicializando gestor del calendario");

    // Esperar un poco para asegurar que todos los scripts estén cargados
    setTimeout(() => {
        // Escuchar el evento de datos listos
        window.addEventListener('vecinoReservasDataReady', function() {
            console.log("📊 [VECINO] Datos listos, inicializando calendario");
            initializeCalendar();
        });

        // Si los datos ya están disponibles, inicializar inmediatamente
        if (window.vecinoCalendarEventsProcessor && window.vecinoCalendarEventsProcessor.isDataReady) {
            console.log("📊 [VECINO] Datos ya disponibles, inicializando inmediatamente");
            initializeCalendar();
        }

        // Fallback para compatibilidad con el código existente
        window.addEventListener('reservasDataReady', function() {
            if (!vecinoCalendarManager || !vecinoCalendarManager.isInitialized) {
                console.log("📊 [VECINO] Fallback: inicializando desde evento reservasDataReady");
                initializeCalendar();
            }
        });
    }, 500);
});

// Exportar para uso global
window.vecinoCalendarManager = vecinoCalendarManager;
window.initializeCalendar = initializeCalendar;
window.getActiveFilters = getActiveFilters;