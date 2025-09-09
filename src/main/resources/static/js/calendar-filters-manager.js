// GESTOR DE FILTROS PARA CALENDARIO DE MANTENIMIENTO
"use strict";

class CalendarFiltersManager {
    constructor() {
        this.activeFilters = new Set();
        this.filterConfig = {
            // Filtros para reservas
            'confirmada': {
                label: 'Reservas Confirmadas',
                icon: 'bx-check',
                color: 'success',
                category: 'reservas'
            },
            'no_confirmada': {
                label: 'Pendientes de Confirmación',
                icon: 'bx-time',
                color: 'warning',
                category: 'reservas'
            },
            'cancelada': {
                label: 'Reservas Canceladas',
                icon: 'bx-x',
                color: 'danger',
                category: 'reservas'
            },
            // Filtros para mantenimientos
            'mantenimiento': {
                label: 'Todos los Mantenimientos',
                icon: 'bx-wrench',
                color: 'info',
                category: 'mantenimiento'
            },
            'mantenimiento_preventivo': {
                label: 'Mantenimiento Preventivo',
                icon: 'bx-shield',
                color: 'primary',
                category: 'mantenimiento'
            },
            'mantenimiento_correctivo': {
                label: 'Mantenimiento Correctivo',
                icon: 'bx-wrench',
                color: 'warning',
                category: 'mantenimiento'
            },
            'mantenimiento_limpieza': {
                label: 'Limpieza Profunda',
                icon: 'bx-spray-can',
                color: 'info',
                category: 'mantenimiento'
            },
            'mantenimiento_urgente': {
                label: 'Mantenimientos Urgentes',
                icon: 'bx-error',
                color: 'danger',
                category: 'mantenimiento'
            }
        };

        this.init();
    }

    init() {
        console.log("🔧 Inicializando gestor de filtros del calendario");
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

        // Limpiar contenido existente
        filterContainer.innerHTML = '';

        // Agrupar por categorías
        const reservasFilters = Object.entries(this.filterConfig)
            .filter(([key, config]) => config.category === 'reservas');

        const mantenimientoFilters = Object.entries(this.filterConfig)
            .filter(([key, config]) => config.category === 'mantenimiento');

        // Renderizar sección de reservas
        if (reservasFilters.length > 0) {
            filterContainer.appendChild(this.createFilterSection('Reservas', reservasFilters));
        }

        // Renderizar sección de mantenimientos
        if (mantenimientoFilters.length > 0) {
            filterContainer.appendChild(this.createFilterSection('Mantenimientos', mantenimientoFilters));
        }
    }

    createFilterSection(title, filters) {
        const section = document.createElement('div');
        section.className = 'filter-section mb-3';

        const titleElement = document.createElement('h6');
        titleElement.className = 'text-white mb-2 filter-section-title';
        titleElement.innerHTML = `<i class="bx bx-filter-alt me-2"></i>${title}`;
        section.appendChild(titleElement);

        filters.forEach(([key, config]) => {
            const filterItem = this.createFilterItem(key, config);
            section.appendChild(filterItem);
        });

        return section;
    }

    createFilterItem(key, config) {
        const div = document.createElement('div');
        div.className = 'form-check mb-2';

        const checkbox = document.createElement('input');
        checkbox.className = `form-check-input input-filter filter-${config.category}`;
        checkbox.type = 'checkbox';
        checkbox.id = `filter-${key}`;
        checkbox.setAttribute('data-value', key);
        checkbox.checked = this.activeFilters.has(key);

        const label = document.createElement('label');
        label.className = 'form-check-label text-white';
        label.setAttribute('for', checkbox.id);
        label.innerHTML = `<i class="bx ${config.icon} me-1"></i>${config.label}`;

        div.appendChild(checkbox);
        div.appendChild(label);

        return div;
    }

    setupFilterEventListeners() {
        // Listener para el checkbox "Seleccionar Todo"
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
        console.log(`🔧 Seleccionar todo: ${isChecked}`);

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
            console.log(`✅ Filtro activado: ${filterValue}`);
        } else {
            this.activeFilters.delete(filterValue);
            console.log(`❌ Filtro desactivado: ${filterValue}`);
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
        console.log("🔧 Filtros cambiados:", Array.from(this.activeFilters));

        // Disparar evento personalizado
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
            // Verificar prioridad urgente primero
            if (props.prioridad === 'URGENTE') {
                return 'mantenimiento_urgente';
            }

            // Luego verificar tipo específico
            const tipoMantenimiento = props.tipoMantenimiento;
            if (tipoMantenimiento) {
                const tipoLower = tipoMantenimiento.toString().toLowerCase();
                if (tipoLower.includes('preventivo')) {
                    return 'mantenimiento_preventivo';
                } else if (tipoLower.includes('correctivo')) {
                    return 'mantenimiento_correctivo';
                } else if (tipoLower.includes('limpieza')) {
                    return 'mantenimiento_limpieza';
                }
            }

            // Fallback para mantenimientos no categorizados
            return 'mantenimiento';
        }

        // Si es una reserva, usar el estado
        return props.calendar || 'confirmada';
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

    // Método para destruir el gestor de filtros
    destroy() {
        document.removeEventListener('change', this.handleFilterChange);
        this.activeFilters.clear();
    }
}

// Exportar para uso global
window.CalendarFiltersManager = CalendarFiltersManager;