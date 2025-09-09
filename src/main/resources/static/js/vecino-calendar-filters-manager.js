// GESTOR DE FILTROS PARA CALENDARIO DE VECINO - VERSIÓN ADAPTADA
"use strict";

class VecinoCalendarFiltersManager {
    constructor() {
        this.activeFilters = new Set();
        this.filterConfig = {
            // Filtros para reservas propias
            'confirmada': {
                label: 'Mis Reservas Confirmadas',
                icon: 'bx-check-circle',
                color: 'success',
                category: 'mis_reservas'
            },
            'no_confirmada': {
                label: 'Mis Reservas Pendientes',
                icon: 'bx-time-five',
                color: 'warning',
                category: 'mis_reservas'
            },
            'cancelada': {
                label: 'Mis Reservas Canceladas',
                icon: 'bx-x-circle',
                color: 'secondary',
                category: 'mis_reservas'
            },
            // Filtros para reservas de otros
            'reservado': {
                label: 'Espacios Ocupados',
                icon: 'bx-block',
                color: 'danger',
                category: 'otros'
            },
            // Filtros para mantenimientos
            'mantenimiento': {
                label: 'Mantenimientos',
                icon: 'bx-wrench',
                color: 'info',
                category: 'mantenimiento'
            }
        };

        this.init();
    }

    init() {
        console.log("🔧 [VECINO] Inicializando gestor de filtros del calendario");
        this.initializeDefaultFilters();
        this.setupFilterEventListeners();
        this.renderFilterUI();
    }

    initializeDefaultFilters() {
        // Por defecto, mostrar todos los filtros activos
        Object.keys(this.filterConfig).forEach(filterKey => {
            this.activeFilters.add(filterKey);
        });
    }

    renderFilterUI() {
        const filterContainer = document.querySelector('.app-calendar-events-filter');
        if (!filterContainer) {
            console.error("No se encontró el contenedor de filtros");
            return;
        }

        // Limpiar contenido existente pero mantener el checkbox "Ver Todas"
        const existingCheckboxes = filterContainer.querySelectorAll('.form-check:not(:first-child)');
        existingCheckboxes.forEach(checkbox => checkbox.remove());

        // Agrupar por categorías
        const misReservasFilters = Object.entries(this.filterConfig)
            .filter(([key, config]) => config.category === 'mis_reservas');

        const otrosFilters = Object.entries(this.filterConfig)
            .filter(([key, config]) => config.category === 'otros');

        const mantenimientoFilters = Object.entries(this.filterConfig)
            .filter(([key, config]) => config.category === 'mantenimiento');

        // Renderizar secciones
        if (misReservasFilters.length > 0) {
            this.appendFiltersToContainer(filterContainer, misReservasFilters);
        }

        if (otrosFilters.length > 0) {
            this.appendFiltersToContainer(filterContainer, otrosFilters);
        }

        if (mantenimientoFilters.length > 0) {
            this.appendFiltersToContainer(filterContainer, mantenimientoFilters);
        }
    }

    appendFiltersToContainer(container, filters) {
        filters.forEach(([key, config]) => {
            const filterItem = this.createFilterItem(key, config);
            container.appendChild(filterItem);
        });
    }

    createFilterItem(key, config) {
        const div = document.createElement('div');
        div.className = `form-check form-check-${config.color} mb-2`;

        const checkbox = document.createElement('input');
        checkbox.className = `form-check-input input-filter`;
        checkbox.type = 'checkbox';
        checkbox.id = `select-${key}`;
        checkbox.setAttribute('data-value', key);
        checkbox.checked = this.activeFilters.has(key);

        const label = document.createElement('label');
        label.className = 'form-check-label';
        label.setAttribute('for', checkbox.id);
        label.innerHTML = `<i class="bx ${config.icon} me-1"></i>${config.label}`;

        div.appendChild(checkbox);
        div.appendChild(label);

        return div;
    }

    setupFilterEventListeners() {
        // Listener para el checkbox "Ver Todas" (selectAll)
        const selectAllCheckbox = document.querySelector('.select-all');
        if (selectAllCheckbox) {
            selectAllCheckbox.addEventListener('change', (e) => {
                this.handleSelectAll(e.target.checked);
            });
        }

        // Listener para filtros individuales (delegado)
        document.addEventListener('change', (e) => {
            if (e.target.classList.contains('input-filter')) {
                this.handleFilterChange(e.target);
            }
        });
    }

    handleSelectAll(isChecked) {
        console.log(`🔧 [VECINO] Seleccionar todo: ${isChecked}`);

        const filterInputs = document.querySelectorAll('.input-filter');
        filterInputs.forEach(input => {
            input.checked = isChecked;

            if (isChecked) {
                this.activeFilters.add(input.getAttribute('data-value'));
            } else {
                this.activeFilters.delete(input.getAttribute('data-value'));
            }
        });

        this.updateSelectAllState();
        this.notifyFiltersChanged();
    }

    handleFilterChange(filterInput) {
        const filterValue = filterInput.getAttribute('data-value');

        if (filterInput.checked) {
            this.activeFilters.add(filterValue);
            console.log(`✅ [VECINO] Filtro activado: ${filterValue}`);
        } else {
            this.activeFilters.delete(filterValue);
            console.log(`❌ [VECINO] Filtro desactivado: ${filterValue}`);
        }

        this.updateSelectAllState();
        this.notifyFiltersChanged();
    }

    updateSelectAllState() {
        const selectAllCheckbox = document.querySelector('.select-all');
        const filterInputs = document.querySelectorAll('.input-filter');
        const checkedInputs = document.querySelectorAll('.input-filter:checked');

        if (selectAllCheckbox) {
            selectAllCheckbox.checked = checkedInputs.length === filterInputs.length;
            selectAllCheckbox.indeterminate = checkedInputs.length > 0 && checkedInputs.length < filterInputs.length;
        }
    }

    notifyFiltersChanged() {
        console.log("🔧 [VECINO] Filtros cambiados:", Array.from(this.activeFilters));

        // Disparar evento personalizado
        window.dispatchEvent(new CustomEvent('vecinoCalendarFiltersChanged', {
            detail: {
                activeFilters: Array.from(this.activeFilters),
                filterConfig: this.filterConfig
            }
        }));

        // Mantener compatibilidad con el código existente
        window.dispatchEvent(new CustomEvent('calendarFiltersChanged', {
            detail: {
                activeFilters: Array.from(this.activeFilters),
                filterConfig: this.filterConfig
            }
        }));
    }

    getActiveFilters() {
        return Array.from(this.activeFilters);
    }

    isFilterActive(filterKey) {
        return this.activeFilters.has(filterKey);
    }

    // Método para filtrar eventos según los filtros activos
    filterEvents(events) {
        if (this.activeFilters.size === 0) {
            return []; // Si no hay filtros activos, no mostrar nada
        }

        return events.filter(event => {
            const eventType = this.getEventFilterType(event);
            return this.activeFilters.has(eventType);
        });
    }

    getEventFilterType(event) {
        const props = event.extendedProps || {};

        // Si es un evento de mantenimiento
        if (props.tipo === 'mantenimiento') {
            return 'mantenimiento';
        }

        // Si es una reserva, determinar el tipo según el calendar
        if (props.tipo === 'reserva') {
            return props.calendar || 'reservado';
        }

        // Fallback
        return 'reservado';
    }

    // Método para actualizar los filtros programáticamente
    updateFilters(filtersToActivate) {
        this.activeFilters.clear();
        filtersToActivate.forEach(filter => {
            this.activeFilters.add(filter);
        });

        // Actualizar UI
        document.querySelectorAll('.input-filter').forEach(input => {
            const filterValue = input.getAttribute('data-value');
            input.checked = this.activeFilters.has(filterValue);
        });

        this.updateSelectAllState();
        this.notifyFiltersChanged();
    }

    // Método para obtener estadísticas de filtros
    getFilterStats(events) {
        const stats = {};

        Object.keys(this.filterConfig).forEach(filterKey => {
            stats[filterKey] = 0;
        });

        events.forEach(event => {
            const filterType = this.getEventFilterType(event);
            if (stats.hasOwnProperty(filterType)) {
                stats[filterType]++;
            }
        });

        return stats;
    }

    // Métodos de conveniencia para acciones rápidas
    showOnlyMyReservations() {
        this.updateFilters(['confirmada', 'no_confirmada', 'cancelada']);
    }

    showOnlyAvailability() {
        this.updateFilters(['reservado', 'mantenimiento']);
    }

    showAll() {
        this.updateFilters(Object.keys(this.filterConfig));
    }

    // Método para destruir el gestor de filtros
    destroy() {
        document.removeEventListener('change', this.handleFilterChange);
        this.activeFilters.clear();
    }
}

// Exportar para uso global
window.VecinoCalendarFiltersManager = VecinoCalendarFiltersManager;