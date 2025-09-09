// PROCESADOR DE EVENTOS PARA CALENDARIO DE VECINO - VERSIÓN ADAPTADA
"use strict";

class VecinoCalendarEventsProcessor {
    constructor() {
        this.reservasData = [];
        this.mantenimientosData = [];
        this.allEvents = [];
        this.isDataReady = false;
        this.usuarioActual = null;
    }

    // Inicializar con datos del servidor
    initialize(reservasDelServidor = [], mantenimientosDelServidor = [], usuarioId = null) {
        console.log("📊 [VECINO] Inicializando procesador de eventos");
        console.log("📊 Reservas del servidor:", reservasDelServidor.length);
        console.log("📊 Mantenimientos del servidor:", mantenimientosDelServidor.length);
        console.log("👤 Usuario actual ID:", usuarioId);

        this.usuarioActual = usuarioId;
        this.processReservas(reservasDelServidor);
        this.processMantenimientos(mantenimientosDelServidor);
        this.combineEvents();
        this.notifyDataReady();
    }

    processReservas(reservas) {
        console.log("🔄 [VECINO] Procesando reservas...");

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

        // Determinar si es reserva propia del usuario
        const esPropia = reserva.esPropia === true;

        // Determinar color y estado según propiedad y estado
        const { color, calendar, estadoTexto, title } = this.getReservaStyle(reserva, esPropia);

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
                vecino: esPropia ? 'Mi reserva' : 'Reservado',
                estado: reserva.estado,
                costo: esPropia ? reserva.costo : null,
                tipoPago: esPropia ? (reserva.tipoPago || 'No especificado') : null,
                momentoReserva: esPropia ? reserva.momentoReserva : null,
                espacioNombre: reserva.espacioNombre,
                esPropia: esPropia,
                // Datos completos del vecino para reservas propias
                vecinoNombre: esPropia ? reserva.vecinoNombre : null,
                vecinoApellido: esPropia ? reserva.vecinoApellido : null
            }
        };
    }

    getReservaStyle(reserva, esPropia) {
        // Si NO es propia, siempre mostrar como "Reservado" en rojo
        if (!esPropia) {
            return {
                color: 'danger',
                calendar: 'reservado',
                estadoTexto: 'Reservado',
                title: 'Reservado'
            };
        }

        // Para reservas propias, usar el estado real
        const estado = reserva.estado ? reserva.estado.toString().trim() : '';

        const stateMap = {
            'Confirmada': {
                color: 'success',
                calendar: 'confirmada',
                estadoTexto: 'Confirmada'
            },
            'Pendiente de confirmación': {
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
                color: 'secondary',
                calendar: 'cancelada',
                estadoTexto: 'Cancelada'
            }
        };

        // Buscar coincidencia exacta
        if (stateMap[estado]) {
            const config = stateMap[estado];
            return {
                ...config,
                title: `${config.estadoTexto} - Mi reserva`
            };
        }

        // Buscar coincidencia parcial
        const estadoLower = estado.toLowerCase();
        if (estadoLower.includes('confirmad')) {
            return {
                ...stateMap['Confirmada'],
                title: 'Confirmada - Mi reserva'
            };
        }
        if (estadoLower.includes('pendiente') || estadoLower.includes('no confirmad')) {
            return {
                ...stateMap['Pendiente de confirmación'],
                title: 'Pendiente - Mi reserva'
            };
        }
        if (estadoLower.includes('cancel')) {
            return {
                ...stateMap['Cancelada'],
                title: 'Cancelada - Mi reserva'
            };
        }

        // Fallback para estados desconocidos (propia)
        return {
            color: 'info',
            calendar: 'otra',
            estadoTexto: estado || 'Estado Desconocido',
            title: `${estado || 'Mi reserva'}`
        };
    }

    processMantenimientos(mantenimientos) {
        console.log("🔧 [VECINO] Procesando mantenimientos...");

        this.mantenimientosData = (mantenimientos || []).map(mantenimiento => {
            try {
                return this.createMantenimientoEvent(mantenimiento);
            } catch (error) {
                console.error("❌ Error procesando mantenimiento:", mantenimiento, error);
                return null;
            }
        }).filter(Boolean);

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

        // Para vecinos, mostrar información limitada pero clara
        const { color, title } = this.getMantenimientoStyleForVecino(mantenimiento);

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
                descripcion: 'Mantenimiento programado', // Información limitada para vecinos
                bloqueaReservas: true,
                // Para vecinos, información muy limitada
                esVisible: true
            }
        };
    }

    getMantenimientoStyleForVecino(mantenimiento) {
        const prioridad = (mantenimiento.prioridad || 'MEDIA').toString().toUpperCase();
        const tipo = this.getTipoMantenimientoTexto(mantenimiento.tipoMantenimiento);

        // Para vecinos, todos los mantenimientos son "bloqueantes" (rojo)
        let color = 'danger';
        let icon = '🔧';

        // Solo mostrar urgencia si es crítica
        if (prioridad === 'URGENTE') {
            color = 'dark';
            icon = '🚨';
        }

        // Título simplificado para vecinos
        let title = `${icon} ${tipo}`;
        if (prioridad === 'URGENTE') {
            title = `🚨 ${tipo} - URGENTE`;
        }

        return { color, title };
    }

    getTipoMantenimientoTexto(tipo) {
        const tipoMap = {
            'PREVENTIVO': 'Mantenimiento',
            'CORRECTIVO': 'Reparación',
            'LIMPIEZA': 'Limpieza',
            'REPARACION': 'Reparación',
            'INSTALACION': 'Instalación',
            'OTRO': 'Mantenimiento'
        };

        return tipoMap[tipo?.toString().toUpperCase()] || 'Mantenimiento';
    }

    combineEvents() {
        this.allEvents = [...this.reservasData, ...this.mantenimientosData];

        console.log("🔄 [VECINO] Eventos combinados:");
        console.log("  - Total:", this.allEvents.length);
        console.log("  - Reservas:", this.reservasData.length);
        console.log("  - Mantenimientos:", this.mantenimientosData.length);

        // Compatibilidad con el código existente
        window.reservasEvents = this.allEvents;
    }

    notifyDataReady() {
        this.isDataReady = true;
        console.log("✅ [VECINO] Datos del calendario listos");

        // Disparar evento
        window.dispatchEvent(new CustomEvent('vecinoReservasDataReady', {
            detail: {
                totalEvents: this.allEvents.length,
                reservas: this.reservasData.length,
                mantenimientos: this.mantenimientosData.length
            }
        }));

        // Mantener compatibilidad
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

    // Verificar conflictos con mantenimiento (para validaciones de reserva)
    verificarConflictoMantenimiento(fechaHoraInicio, fechaHoraFin) {
        return this.mantenimientosData.some(mantenimiento => {
            const mantenimientoInicio = new Date(mantenimiento.start);
            const mantenimientoFin = new Date(mantenimiento.end);

            // Verificar solapamiento
            return (fechaHoraInicio < mantenimientoFin && fechaHoraFin > mantenimientoInicio);
        });
    }

    // Validar reserva contra mantenimiento (compatible con código existente)
    validarReservaContraMantenimiento(fecha, horaInicio, horaFin) {
        const fechaInicioReserva = new Date(fecha + 'T' + horaInicio);
        const fechaFinReserva = new Date(fecha + 'T' + horaFin);

        console.log("Validando reserva:", fechaInicioReserva, "a", fechaFinReserva);

        const hayConflicto = this.verificarConflictoMantenimiento(fechaInicioReserva, fechaFinReserva);

        if (hayConflicto) {
            const mantenimientoConflictivo = this.mantenimientosData.find(mantenimiento => {
                const mantenimientoInicio = new Date(mantenimiento.start);
                const mantenimientoFin = new Date(mantenimiento.end);
                return (fechaInicioReserva < mantenimientoFin && fechaFinReserva > mantenimientoInicio);
            });

            if (mantenimientoConflictivo) {
                const tipoMantenimiento = mantenimientoConflictivo.extendedProps.tipoMantenimiento;
                alert(`No se puede reservar en este horario debido a: ${tipoMantenimiento}`);
            } else {
                alert('No se puede reservar en este horario debido a mantenimiento programado.');
            }

            return false;
        }

        return true;
    }

    // Método para refrescar datos
    refresh(reservas = [], mantenimientos = []) {
        console.log("🔄 [VECINO] Refrescando datos del calendario");
        this.initialize(reservas, mantenimientos, this.usuarioActual);
    }

    // Método para obtener estadísticas
    getStats() {
        const reservasPropias = this.reservasData.filter(r => r.extendedProps.esPropia);
        const reservasAjenas = this.reservasData.filter(r => !r.extendedProps.esPropia);

        const stats = {
            total: this.allEvents.length,
            reservas: {
                total: this.reservasData.length,
                propias: reservasPropias.length,
                ajenas: reservasAjenas.length,
                confirmadas: reservasPropias.filter(r => r.extendedProps.calendar === 'confirmada').length,
                pendientes: reservasPropias.filter(r => r.extendedProps.calendar === 'no_confirmada').length,
                canceladas: reservasPropias.filter(r => r.extendedProps.calendar === 'cancelada').length
            },
            mantenimientos: {
                total: this.mantenimientosData.length,
                urgentes: this.mantenimientosData.filter(m => m.extendedProps.prioridad === 'URGENTE').length,
                activos: this.mantenimientosData.filter(m =>
                    m.extendedProps.estado === 'PROGRAMADO' ||
                    m.extendedProps.estado === 'EN_PROCESO').length
            }
        };

        return stats;
    }
}

// Instancia global del procesador para vecinos
window.vecinoCalendarEventsProcessor = new VecinoCalendarEventsProcessor();

// Mantener compatibilidad con el sistema existente
window.calendarEventsProcessor = window.vecinoCalendarEventsProcessor;

// Exportar funciones para compatibilidad con el código existente
window.verificarConflictoMantenimiento = function(fechaHoraInicio, fechaHoraFin) {
    return window.vecinoCalendarEventsProcessor.verificarConflictoMantenimiento(fechaHoraInicio, fechaHoraFin);
};

window.validarReservaContraMantenimiento = function(fecha, horaInicio, horaFin) {
    return window.vecinoCalendarEventsProcessor.validarReservaContraMantenimiento(fecha, horaInicio, horaFin);
};

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("📊 [VECINO] DOM cargado, inicializando procesador de eventos");

    // Obtener ID del usuario actual
    let usuarioId = null;
    if (window.usuarioActualId) {
        usuarioId = window.usuarioActualId;
    }

    // Inicializar con datos del servidor si están disponibles
    const reservas = window.reservasDelServidor || [];
    const mantenimientos = window.mantenimientosDelServidor || [];

    console.log("📊 [VECINO] Datos encontrados:");
    console.log("  - Reservas:", reservas.length);
    console.log("  - Mantenimientos:", mantenimientos.length);
    console.log("  - Usuario ID:", usuarioId);

    window.vecinoCalendarEventsProcessor.initialize(reservas, mantenimientos, usuarioId);
});