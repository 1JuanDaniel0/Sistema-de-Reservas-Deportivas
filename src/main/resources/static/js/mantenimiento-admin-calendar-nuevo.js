// CALENDARIO DE MANTENIMIENTO ADMIN - VERSIÓN REFACTORIZADA
"use strict";

let direction = "ltr";
if (isRtl) { direction = "rtl"; }

let calendar; // Variable global para el calendario
let espacioActual = null;
let flatpickrInicio, flatpickrFin; // Variables para los date pickers
let filtersManager; // Gestor de filtros

class MaintenanceCalendarManager {
    constructor() {
        this.calendar = null;
        this.espacioActual = null;
        this.filtersManager = null;
        this.eventsProcessor = null;
        this.isInitialized = false;
    }

    async initialize() {
        console.log("🚀 Inicializando calendario de mantenimiento refactorizado");

        try {
            // Esperar a que los datos estén listos
            await this.waitForData();

            // Inicializar componentes
            this.initializeFiltersManager();
            this.initializeCalendar();
            this.setupSpaceSelector();
            this.setupMaintenanceModal();
            this.setupQuickActions();

            this.isInitialized = true;
            console.log("✅ Calendario de mantenimiento inicializado correctamente");

        } catch (error) {
            console.error("❌ Error inicializando calendario:", error);
            this.showToast('Error inicializando el calendario', 'error');
        }
    }

    waitForData() {
        return new Promise((resolve) => {
            if (window.calendarEventsProcessor && window.calendarEventsProcessor.isDataReady) {
                console.log("📊 Datos ya están listos");
                this.eventsProcessor = window.calendarEventsProcessor;
                resolve();
                return;
            }

            // Escuchar el evento de datos listos
            const handler = () => {
                console.log("📊 Datos recibidos via evento");
                this.eventsProcessor = window.calendarEventsProcessor;
                window.removeEventListener('reservasDataReady', handler);
                resolve();
            };

            window.addEventListener('reservasDataReady', handler);

            // Timeout de fallback
            setTimeout(() => {
                if (!this.eventsProcessor) {
                    console.log("⏰ Timeout: Inicializando con datos vacíos");
                    this.eventsProcessor = window.calendarEventsProcessor || { getAllEvents: () => [] };
                    window.removeEventListener('reservasDataReady', handler);
                    resolve();
                }
            }, 2000);
        });
    }

    initializeFiltersManager() {
        console.log("🔧 Inicializando gestor de filtros");

        this.filtersManager = new CalendarFiltersManager();

        // Escuchar cambios en los filtros
        window.addEventListener('calendarFiltersChanged', (event) => {
            this.handleFiltersChanged(event.detail);
        });

        // Configurar botones de acción rápida
        this.setupFilterQuickActions();

        // Actualizar contador de eventos
        this.updateEventCounter();
    }

    setupFilterQuickActions() {
        const btnShowOnlyReservas = document.getElementById('btnShowOnlyReservas');
        const btnShowOnlyMantenimiento = document.getElementById('btnShowOnlyMantenimiento');

        if (btnShowOnlyReservas) {
            btnShowOnlyReservas.addEventListener('click', () => {
                this.filtersManager.updateFilters(['confirmada', 'no_confirmada', 'cancelada']);
            });
        }

        if (btnShowOnlyMantenimiento) {
            btnShowOnlyMantenimiento.addEventListener('click', () => {
                this.filtersManager.updateFilters([
                    'mantenimiento', 'mantenimiento_preventivo',
                    'mantenimiento_correctivo', 'mantenimiento_limpieza',
                    'mantenimiento_urgente'
                ]);
            });
        }
    }

    handleFiltersChanged(filterData) {
        console.log("🔄 Filtros cambiados:", filterData.activeFilters);

        // Refrescar eventos del calendario
        if (this.calendar) {
            this.calendar.refetchEvents();
        }

        // Actualizar contador
        this.updateEventCounter();
    }

    updateEventCounter() {
        const counterElement = document.getElementById('eventCounter');
        if (!counterElement || !this.eventsProcessor) return;

        const allEvents = this.eventsProcessor.getAllEvents();
        const stats = this.eventsProcessor.getStats();

        if (this.filtersManager) {
            const filteredEvents = this.filtersManager.filterEvents(allEvents);
            counterElement.innerHTML = `
                Mostrando ${filteredEvents.length} de ${allEvents.length} eventos
                <br><small>(${stats.reservas.total} reservas, ${stats.mantenimientos.total} mantenimientos)</small>
            `;
        } else {
            counterElement.textContent = `${allEvents.length} eventos totales`;
        }
    }

    initializeCalendar() {
        console.log("📅 Inicializando FullCalendar");

        const calendarEl = document.getElementById("calendar");
        if (!calendarEl) {
            throw new Error("No se encontró el elemento del calendario");
        }

        // Colores para diferentes estados
        const estadoColors = {
            'confirmada': 'success',
            'no_confirmada': 'warning',
            'cancelada': 'danger',
            'mantenimiento': 'info'
        };

        this.calendar = new Calendar(calendarEl, {
            initialView: "dayGridMonth",
            locale: "es",

            events: (fetchInfo, successCallback) => {
                console.log("📊 Cargando eventos para el calendario");

                try {
                    const allEvents = this.eventsProcessor?.getAllEvents() || [];
                    const filteredEvents = this.filtersManager?.filterEvents(allEvents) || allEvents;

                    console.log(`📊 Eventos cargados: ${filteredEvents.length} de ${allEvents.length}`);
                    successCallback(filteredEvents);
                } catch (error) {
                    console.error("❌ Error cargando eventos:", error);
                    successCallback([]);
                }
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

            datesSet: () => {
                this.setupSidebarToggle();
            },

            viewDidMount: () => {
                this.setupSidebarToggle();
            },

            eventDidMount: (info) => {
                this.setupEventTooltip(info);
            }
        });

        console.log("📅 Renderizando calendario...");
        this.calendar.render();
        this.setupSidebarToggle();
    }

    handleEventClick(info) {
        console.log("👆 Evento clickeado:", info.event);
        this.showEventInfo(info.event);
    }

    handleDateClick(info) {
        console.log("📅 Fecha clickeada:", info.dateStr);

        const fechaSeleccionada = info.date;
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        if (fechaSeleccionada < hoy) {
            this.showToast('No puedes programar mantenimiento en fechas pasadas', 'warning');
            return;
        }

        this.openMaintenanceModal(fechaSeleccionada);
    }

    handleDateSelect(info) {
        console.log("📅 Rango seleccionado:", info);

        const fechaInicio = info.start;
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        if (fechaInicio < hoy) {
            this.showToast('No puedes programar mantenimiento en fechas pasadas', 'warning');
            this.calendar.unselect();
            return;
        }

        this.openMaintenanceModal(fechaInicio, info.end);
    }

    showEventInfo(event) {
        const props = event.extendedProps;
        let message = '';

        if (props.tipo === 'mantenimiento') {
            message = `
                <div class="event-info-modal">
                    <h6><i class="bx bx-wrench me-2"></i><strong>Mantenimiento Programado</strong></h6>
                    <p><strong>Tipo:</strong> ${props.tipoMantenimiento || 'No especificado'}</p>
                    <p><strong>Responsable:</strong> ${props.responsable || 'No asignado'}</p>
                    <p><strong>Prioridad:</strong> <span class="badge bg-${this.getPriorityColor(props.prioridad)}">${props.prioridad || 'Media'}</span></p>
                    <p><strong>Estado:</strong> ${props.estado || 'Programado'}</p>
                    <p><strong>Descripción:</strong> ${props.descripcion || 'Sin descripción'}</p>
                    ${props.costoEstimado ? `<p><strong>Costo estimado:</strong> S/. ${props.costoEstimado}</p>` : ''}
                </div>
            `;
        } else {
            const inicio = moment(event.start).format('HH:mm');
            const fin = moment(event.end).format('HH:mm');
            message = `
                <div class="event-info-modal">
                    <h6><i class="bx bx-calendar me-2"></i><strong>Reserva: ${props.estado}</strong></h6>
                    <p><strong>Vecino:</strong> ${props.vecino}</p>
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

    setupEventTooltip(info) {
        const props = info.event.extendedProps;
        let tooltipContent = '';

        if (props.tipo === 'mantenimiento') {
            tooltipContent = `
                ${props.icon || '🔧'} Mantenimiento Programado
                Tipo: ${props.tipoMantenimiento || 'No especificado'}
                Responsable: ${props.responsable || 'No asignado'}
                Prioridad: ${props.prioridad || 'Media'}
            `;
        } else {
            const horaInicio = info.event.start.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true });
            const horaFin = info.event.end.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true });
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

    setupSpaceSelector() {
        const espacioSelector = document.getElementById('espacioSelector');
        if (!espacioSelector || !window.espaciosData) return;

        // Inicializar Select2
        $(espacioSelector).select2({
            placeholder: 'Selecciona un espacio...',
            allowClear: false
        });

        // Configurar espacio inicial
        const urlParams = new URLSearchParams(window.location.search);
        const espacioEnUrl = urlParams.get('idEspacio');

        if (espacioEnUrl) {
            $(espacioSelector).val(espacioEnUrl);
            const espacioSeleccionado = window.espaciosData.find(e => e.idEspacio == espacioEnUrl);
            if (espacioSeleccionado) {
                this.updateSpaceInfo(espacioSeleccionado);
            }
        } else if (window.espaciosData.length > 0) {
            const primerEspacio = window.espaciosData[0];
            $(espacioSelector).val(primerEspacio.idEspacio);
            this.updateSpaceInfo(primerEspacio);

            const newUrl = new URL(window.location);
            newUrl.searchParams.set('idEspacio', primerEspacio.idEspacio);
            window.history.replaceState({}, '', newUrl);
        }

        // Configurar evento change
        $(espacioSelector).off('change.maintenance').on('change.maintenance', (e) => {
            const espacioId = e.target.value;
            if (espacioId) {
                this.changeSpace(espacioId);
            }
        });
    }

    updateSpaceInfo(espacio) {
        document.getElementById('espacioNombre').textContent = espacio.nombre || '-';
        document.getElementById('espacioLugar').textContent = espacio.idLugar?.lugar || '-';
        document.getElementById('espacioCosto').textContent = espacio.costo ? espacio.costo.toFixed(2) : '-';
        this.espacioActual = espacio;
    }

    changeSpace(espacioId) {
        console.log("🔄 Cambiando a espacio:", espacioId);
        this.showToast('Cargando reservas del espacio...', 'info');

        const url = new URL(window.location);
        url.searchParams.set('idEspacio', espacioId);
        window.location.href = url.toString();
    }

    setupMaintenanceModal() {
        // Inicializar Flatpickr
        flatpickrInicio = flatpickr("#fechaInicioMantenimiento", {
            locale: "es",
            dateFormat: "Y-m-d",
            minDate: "today",
            onChange: (selectedDates) => {
                if (selectedDates.length > 0) {
                    flatpickrFin.set('minDate', selectedDates[0]);
                }
            }
        });

        flatpickrFin = flatpickr("#fechaFinMantenimiento", {
            locale: "es",
            dateFormat: "Y-m-d",
            minDate: "today"
        });

        // Configurar validaciones
        document.getElementById('horaInicioMantenimiento').addEventListener('change', () => this.validateHours());
        document.getElementById('horaFinMantenimiento').addEventListener('change', () => this.validateHours());

        // Configurar botón de guardar
        document.getElementById('btnGuardarMantenimiento').addEventListener('click', () => this.saveMaintenancePlan());
    }

    setupQuickActions() {
        // Configurar acciones rápidas del sidebar
        const btnToday = document.querySelector('[onclick="calendar.today()"]');
        if (btnToday) {
            btnToday.onclick = () => {
                if (this.calendar) this.calendar.today();
            };
        }

        const btnRefresh = document.querySelector('[onclick="calendar.refetchEvents()"]');
        if (btnRefresh) {
            btnRefresh.onclick = () => {
                if (this.calendar) this.calendar.refetchEvents();
                this.updateEventCounter();
            };
        }
    }

    openMaintenanceModal(fechaInicio, fechaFin = null) {
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

        // Actualizar información del espacio
        if (this.espacioActual) {
            document.getElementById('modalEspacioNombre').textContent = this.espacioActual.nombre;
            document.getElementById('modalEspacioLugar').textContent = this.espacioActual.idLugar?.lugar || '-';
        }

        modal.show();
    }

    validateHours() {
        const horaInicio = document.getElementById('horaInicioMantenimiento').value;
        const horaFin = document.getElementById('horaFinMantenimiento').value;

        if (horaInicio && horaFin) {
            if (horaInicio >= horaFin) {
                this.showToast('La hora de fin debe ser posterior a la hora de inicio', 'warning');
                document.getElementById('horaFinMantenimiento').value = '';
                return false;
            }

            const horaInicioNum = parseInt(horaInicio.split(':')[0]);
            const horaFinNum = parseInt(horaFin.split(':')[0]);

            if (horaInicioNum < 8 || horaInicioNum > 22) {
                this.showToast('El horario de operación es de 8:00 AM a 10:00 PM', 'warning');
                document.getElementById('horaInicioMantenimiento').value = '';
                return false;
            }

            if (horaFinNum < 8 || horaFinNum > 22) {
                this.showToast('El horario de operación es de 8:00 AM a 10:00 PM', 'warning');
                document.getElementById('horaFinMantenimiento').value = '';
                return false;
            }
        }

        return true;
    }

    async saveMaintenancePlan() {
        const form = document.getElementById('formMantenimiento');

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const btnGuardar = document.getElementById('btnGuardarMantenimiento');
        const textoOriginal = btnGuardar.innerHTML;
        btnGuardar.disabled = true;
        btnGuardar.innerHTML = '<i class="bx bx-loader-alt bx-spin me-1"></i>Programando...';

        try {
            const data = this.collectFormData();
            await this.submitMaintenanceData(data);

            this.showToast('✅ Mantenimiento programado exitosamente', 'success', 4000);
            bootstrap.Modal.getInstance(document.getElementById('modalMantenimiento')).hide();

            setTimeout(() => {
                window.location.reload();
            }, 1500);

        } catch (error) {
            console.error('❌ Error guardando mantenimiento:', error);
            this.showToast(`❌ ${error.message}`, 'error', 6000);
        } finally {
            btnGuardar.disabled = false;
            btnGuardar.innerHTML = textoOriginal;
        }
    }

    collectFormData() {
        const horaInicioValue = document.getElementById('horaInicioMantenimiento').value;
        const horaFinValue = document.getElementById('horaFinMantenimiento').value;

        const data = {
            espacioId: this.espacioActual?.idEspacio,
            fechaInicio: document.getElementById('fechaInicioMantenimiento').value,
            fechaFin: document.getElementById('fechaFinMantenimiento').value,
            horaInicio: horaInicioValue,
            horaFin: horaFinValue,
            tipoMantenimiento: document.getElementById('tipoMantenimiento').value,
            prioridad: document.getElementById('prioridadMantenimiento').value,
            descripcion: document.getElementById('descripcionMantenimiento').value,
            responsableId: document.getElementById('responsableMantenimiento').value || null,
            costoEstimado: parseFloat(document.getElementById('costoMantenimiento').value) || null
        };

        // Validaciones
        if (!data.espacioId) throw new Error('Debe seleccionar un espacio');
        if (!data.fechaInicio || !data.fechaFin) throw new Error('Las fechas son obligatorias');
        if (!data.horaInicio || !data.horaFin) throw new Error('Las horas son obligatorias');
        if (data.horaInicio >= data.horaFin) throw new Error('La hora de fin debe ser posterior a la de inicio');

        return data;
    }

    async submitMaintenanceData(data) {
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        const response = await fetch('/admin/api/mantenimiento/programar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            throw new Error(`Error HTTP ${response.status}`);
        }

        const result = await response.json();
        if (!result.success) {
            throw new Error(result.error || 'Error desconocido');
        }

        return result;
    }

    showToast(message, type = 'info', duration = 3000) {
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
            this.updateEventCounter();
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

// Instancia global del gestor del calendario
let maintenanceCalendarManager;

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("🚀 DOM cargado, inicializando gestor del calendario");

    maintenanceCalendarManager = new MaintenanceCalendarManager();
    maintenanceCalendarManager.initialize();

    // Hacer accesible globalmente para compatibilidad
    window.maintenanceCalendarManager = maintenanceCalendarManager;
    window.calendar = null; // Se asignará cuando el calendario esté listo

    // Asignar referencias globales cuando esté listo
    setTimeout(() => {
        if (maintenanceCalendarManager.calendar) {
            window.calendar = maintenanceCalendarManager.calendar;
        }
    }, 1000);
});