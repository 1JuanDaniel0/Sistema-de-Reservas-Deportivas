// PROCESADOR DE EVENTOS PARA CALENDARIO DE MANTENIMIENTO
"use strict";

class CalendarEventsProcessor {
    constructor() {
        this.reservasData = [];
        this.mantenimientosData = [];
        this.allEvents = [];
        this.isDataReady = false;
    }

    // Inicializar con datos del servidor
    initialize(reservasDelServidor = [], mantenimientosDelServidor = []) {
        console.log("📊 Inicializando procesador de eventos");
        console.log("📊 Reservas del servidor:", reservasDelServidor.length);
        console.log("📊 Mantenimientos del servidor:", mantenimientosDelServidor.length);

        this.processReservas(reservasDelServidor);
        this.processMantenimientos(mantenimientosDelServidor);
        this.combineEvents();
        this.notifyDataReady();
    }

    processReservas(reservas) {
        console.log("🔄 Procesando reservas...");

        this.reservasData = (reservas || []).map(reserva => {
            try {
                return this.createReservaEvent(reserva);
            } catch (error) {
                console.error("❌ Error procesando reserva:", reserva, error);
                return null;
            }
        }).filter(Boolean); // Eliminar nulls

        console.log("✅ Reservas procesadas:", this.reservasData.length);
    }

    createReservaEvent(reserva) {
        // Validar datos mínimos requeridos
        if (!reserva.fecha || !reserva.horaInicio || !reserva.horaFin) {
            throw new Error("Reserva sin datos de fecha/hora válidos");
        }

        // Crear fechas de inicio y fin
        const fechaInicio = new Date(`${reserva.fecha}T${reserva.horaInicio}`);
        const fechaFin = new Date(`${reserva.fecha}T${reserva.horaFin}`);

        // Validar fechas
        if (isNaN(fechaInicio.getTime()) || isNaN(fechaFin.getTime())) {
            throw new Error("Fechas inválidas en reserva");
        }

        // Determinar color y estado
        const { color, calendar, estadoTexto } = this.getReservaStyle(reserva.estado);

        // Crear título descriptivo
        const vecinoNombre = `${reserva.vecinoNombre || ''} ${reserva.vecinoApellido || ''}`.trim();
        const title = `${estadoTexto} - ${vecinoNombre || 'Sin nombre'}`;

        return {
            id: `reserva-${reserva.idReserva}`,
            title: title,
            start: fechaInicio,
            end: fechaFin,
            allDay: false,
            classNames: [`fc-event-${color}`],
            extendedProps: {
                calendar: calendar,
                tipo: 'reserva',
                reservaId: reserva.idReserva,
                vecino: vecinoNombre,
                estado: reserva.estado,
                costo: reserva.costo,
                tipoPago: reserva.tipoPago || 'No especificado',
                momentoReserva: reserva.momentoReserva,
                espacioNombre: reserva.espacioNombre,
                esPropia: false // Admin ve todas las reservas
            }
        };
    }

    getReservaStyle(estado) {
        console.log("🔍 Mapeando estado de reserva:", estado, typeof estado);

        const stateMap = {
            // Estados por texto completo
            'Confirmada': {
                color: 'success',
                calendar: 'confirmada',
                estadoTexto: 'Confirmada'
            },
            'Pendiente de Confirmación': {
                color: 'warning',
                calendar: 'no_confirmada',
                estadoTexto: 'Pendiente'
            },
            'No confirmada': {
                color: 'warning',
                calendar: 'no_confirmada',
                estadoTexto: 'Pendiente'
            },
            'Cancelada': {
                color: 'danger',
                calendar: 'cancelada',
                estadoTexto: 'Cancelada'
            },
            // Estados alternativos (por si vienen diferentes)
            'CONFIRMADA': {
                color: 'success',
                calendar: 'confirmada',
                estadoTexto: 'Confirmada'
            },
            'PENDIENTE': {
                color: 'warning',
                calendar: 'no_confirmada',
                estadoTexto: 'Pendiente'
            },
            'CANCELADA': {
                color: 'danger',
                calendar: 'cancelada',
                estadoTexto: 'Cancelada'
            }
        };

        // Normalizar el estado para comparación
        const estadoNormalizado = estado ? estado.toString().trim() : '';

        // Buscar coincidencia exacta primero
        if (stateMap[estadoNormalizado]) {
            console.log("✅ Estado mapeado:", estadoNormalizado, "->", stateMap[estadoNormalizado].calendar);
            return stateMap[estadoNormalizado];
        }

        // Buscar coincidencia insensible a mayúsculas
        const estadoUpper = estadoNormalizado.toUpperCase();
        for (const [key, value] of Object.entries(stateMap)) {
            if (key.toUpperCase() === estadoUpper) {
                console.log("✅ Estado mapeado (case-insensitive):", estadoNormalizado, "->", value.calendar);
                return value;
            }
        }

        // Si contiene "confirmad" -> confirmada
        if (estadoNormalizado.toLowerCase().includes('confirmad')) {
            console.log("🔍 Estado contiene 'confirmad', mapeando a confirmada");
            return stateMap['Confirmada'];
        }

        // Si contiene "pendiente" o "no confirmad" -> no_confirmada
        if (estadoNormalizado.toLowerCase().includes('pendiente') ||
            estadoNormalizado.toLowerCase().includes('no confirmad')) {
            console.log("🔍 Estado contiene 'pendiente', mapeando a no_confirmada");
            return stateMap['Pendiente de Confirmación'];
        }

        // Si contiene "cancel" -> cancelada
        if (estadoNormalizado.toLowerCase().includes('cancel')) {
            console.log("🔍 Estado contiene 'cancel', mapeando a cancelada");
            return stateMap['Cancelada'];
        }

        // Fallback - DEFAULT A NO_CONFIRMADA en lugar de confirmada
        console.warn("⚠️ Estado no reconocido:", estadoNormalizado, "- Defaulting to no_confirmada");
        return {
            color: 'warning',
            calendar: 'no_confirmada',
            estadoTexto: estadoNormalizado || 'Estado Desconocido'
        };
    }

    processMantenimientos(mantenimientos) {
        console.log("🔧 Procesando mantenimientos...");

        this.mantenimientosData = (mantenimientos || []).map(mantenimiento => {
            try {
                return this.createMantenimientoEvent(mantenimiento);
            } catch (error) {
                console.error("❌ Error procesando mantenimiento:", mantenimiento, error);
                return null;
            }
        }).filter(Boolean); // Eliminar nulls

        console.log("✅ Mantenimientos procesados:", this.mantenimientosData.length);
    }

    createMantenimientoEvent(mantenimiento) {
        // Validar datos mínimos requeridos
        if (!mantenimiento.fechaInicio || !mantenimiento.fechaFin ||
            !mantenimiento.horaInicio || !mantenimiento.horaFin) {
            throw new Error("Mantenimiento sin datos de fecha/hora válidos");
        }

        // Crear fechas de inicio y fin
        const fechaInicio = new Date(`${mantenimiento.fechaInicio}T${mantenimiento.horaInicio}`);
        const fechaFin = new Date(`${mantenimiento.fechaFin}T${mantenimiento.horaFin}`);

        // Validar fechas
        if (isNaN(fechaInicio.getTime()) || isNaN(fechaFin.getTime())) {
            throw new Error("Fechas inválidas en mantenimiento");
        }

        // Determinar estilo según prioridad y tipo
        const { color, title, icon } = this.getMantenimientoStyle(mantenimiento);

        return {
            id: `mantenimiento-${mantenimiento.idMantenimiento}`,
            title: title,
            start: fechaInicio,
            end: fechaFin,
            allDay: false,
            classNames: [`fc-event-${color}`, 'fc-event-maintenance'],
            extendedProps: {
                calendar: 'mantenimiento',
                tipo: 'mantenimiento',
                mantenimientoId: mantenimiento.idMantenimiento,
                tipoMantenimiento: this.getTipoMantenimientoTexto(mantenimiento.tipoMantenimiento),
                prioridad: (mantenimiento.prioridad || 'MEDIA').toString().toUpperCase(),
                estado: mantenimiento.estado || 'PROGRAMADO',
                descripcion: mantenimiento.descripcion || 'Sin descripción',
                responsable: this.getResponsableNombre(mantenimiento),
                costoEstimado: mantenimiento.costoEstimado || 0,
                creadoPor: this.getCreadoPorNombre(mantenimiento),
                fechaCreacion: mantenimiento.fechaCreacion,
                icon: icon
            }
        };
    }

    getMantenimientoStyle(mantenimiento) {
        const prioridad = (mantenimiento.prioridad || 'MEDIA').toString().toUpperCase();
        const tipo = mantenimiento.tipoMantenimiento || 'OTRO';

        // Determinar color según prioridad
        let color = 'info';
        let icon = '🔧';

        switch (prioridad) {
            case 'BAJA':
                color = 'info';
                break;
            case 'MEDIA':
                color = 'warning';
                break;
            case 'ALTA':
                color = 'danger';
                break;
            case 'URGENTE':
                color = 'dark';
                icon = '🚨';
                break;
        }

        // Obtener texto del tipo
        const tipoTexto = this.getTipoMantenimientoTexto(tipo);

        // Crear título
        let title = `${icon} ${tipoTexto}`;
        if (prioridad === 'URGENTE') {
            title = `🚨 ${tipoTexto} - URGENTE`;
        } else if (prioridad === 'ALTA') {
            title = `⚠️ ${tipoTexto} - ALTA`;
        }

        return { color, title, icon };
    }

    getTipoMantenimientoTexto(tipo) {
        const tipoMap = {
            'PREVENTIVO': 'Mantenimiento Preventivo',
            'CORRECTIVO': 'Mantenimiento Correctivo',
            'LIMPIEZA': 'Limpieza Profunda',
            'REPARACION': 'Reparación',
            'INSTALACION': 'Instalación de Equipos',
            'OTRO': 'Otro Mantenimiento'
        };

        return tipoMap[tipo?.toString().toUpperCase()] || tipo || 'Mantenimiento';
    }

    getResponsableNombre(mantenimiento) {
        if (mantenimiento.responsable) {
            if (mantenimiento.responsable.nombres && mantenimiento.responsable.apellidos) {
                return `${mantenimiento.responsable.nombres} ${mantenimiento.responsable.apellidos}`;
            } else if (typeof mantenimiento.responsable === 'string') {
                return mantenimiento.responsable;
            }
        }
        return 'No asignado';
    }

    getCreadoPorNombre(mantenimiento) {
        if (mantenimiento.creadoPor) {
            if (mantenimiento.creadoPor.nombres && mantenimiento.creadoPor.apellidos) {
                return `${mantenimiento.creadoPor.nombres} ${mantenimiento.creadoPor.apellidos}`;
            } else if (typeof mantenimiento.creadoPor === 'string') {
                return mantenimiento.creadoPor;
            }
        }
        return 'Sistema';
    }

    combineEvents() {
        this.allEvents = [...this.reservasData, ...this.mantenimientosData];

        console.log("🔄 Eventos combinados:");
        console.log("  - Total:", this.allEvents.length);
        console.log("  - Reservas:", this.reservasData.length);
        console.log("  - Mantenimientos:", this.mantenimientosData.length);

        // Exportar para compatibilidad
        window.reservasEvents = this.allEvents;
    }

    notifyDataReady() {
        this.isDataReady = true;
        console.log("✅ Datos del calendario listos");

        // Disparar evento
        window.dispatchEvent(new CustomEvent('reservasDataReady', {
            detail: {
                totalEvents: this.allEvents.length,
                reservas: this.reservasData.length,
                mantenimientos: this.mantenimientosData.length
            }
        }));
    }

    // Métodos de consulta
    getAllEvents() {
        return this.allEvents;
    }

    getReservas() {
        return this.reservasData;
    }

    getMantenimientos() {
        return this.mantenimientosData;
    }

    getEventById(id) {
        return this.allEvents.find(event => event.id === id);
    }

    getEventsByType(tipo) {
        return this.allEvents.filter(event => event.extendedProps.tipo === tipo);
    }

    getEventsByDate(fecha) {
        const targetDate = new Date(fecha);
        return this.allEvents.filter(event => {
            const eventDate = new Date(event.start);
            return eventDate.toDateString() === targetDate.toDateString();
        });
    }

    // Método para refrescar datos
    refresh(reservas = [], mantenimientos = []) {
        console.log("🔄 Refrescando datos del calendario");
        this.initialize(reservas, mantenimientos);
    }

    // Método para obtener estadísticas
    getStats() {
        const stats = {
            total: this.allEvents.length,
            reservas: {
                total: this.reservasData.length,
                confirmadas: this.reservasData.filter(r => r.extendedProps.calendar === 'confirmada').length,
                pendientes: this.reservasData.filter(r => r.extendedProps.calendar === 'no_confirmada').length,
                canceladas: this.reservasData.filter(r => r.extendedProps.calendar === 'cancelada').length
            },
            mantenimientos: {
                total: this.mantenimientosData.length,
                urgentes: this.mantenimientosData.filter(m => m.extendedProps.prioridad === 'URGENTE').length,
                preventivos: this.mantenimientosData.filter(m =>
                    m.extendedProps.tipoMantenimiento?.includes('Preventivo')).length,
                correctivos: this.mantenimientosData.filter(m =>
                    m.extendedProps.tipoMantenimiento?.includes('Correctivo')).length
            }
        };

        return stats;
    }
}

// Instancia global del procesador
window.calendarEventsProcessor = new CalendarEventsProcessor();

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("📊 DOM cargado, inicializando procesador de eventos");

    // Inicializar con datos del servidor si están disponibles
    const reservas = window.reservasDelServidor || [];
    const mantenimientos = window.mantenimientosDelServidor || [];

    window.calendarEventsProcessor.initialize(reservas, mantenimientos);
});