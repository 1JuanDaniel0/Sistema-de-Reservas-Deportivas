"use strict";

let reservasData = []; // Variable global para almacenar las reservas

// Función para inicializar los datos de reservas desde el servidor - ADAPTADA
function inicializarReservas(reservas) {
    console.log("Datos de reservas recibidos del servidor:", reservas);

    // Inicializar array vacío si no hay reservas
    if (!reservas || reservas.length === 0) {
        console.log("No hay reservas disponibles, inicializando array vacío");
        reservasData = [];
        window.reservasEvents = [];
        window.dispatchEvent(new CustomEvent('reservasDataReady'));
        return;
    }

    // Procesar reservas para FullCalendar y para el calendario personalizado
    reservasData = reservas.map(reserva => {
        console.log("Procesando reserva:", reserva);

        // Crear fechas con zona horaria local (sin conversión UTC)
        const fechaBase = reserva.fecha; // YYYY-MM-DD
        const horaInicio = reserva.horaInicio; // HH:mm
        const horaFin = reserva.horaFin; // HH:mm

        // Para FullCalendar - crear Date objects locales
        const fechaInicio = new Date(fechaBase + 'T' + horaInicio + ':00');
        const fechaFin = new Date(fechaBase + 'T' + horaFin + ':00');

        console.log("Fecha inicio procesada:", fechaInicio);
        console.log("Fecha fin procesada:", fechaFin);

        // Determinar si es reserva del usuario actual
        const esPropia = reserva.esPropia === true;

        // Determinar el color y texto según el estado
        let colorEvento;
        let estadoTexto;
        switch(reserva.estado) {
            case 'Confirmada':
                colorEvento = 'success';
                estadoTexto = 'Confirmada';
                break;
            case 'No confirmada':
                colorEvento = 'warning';
                estadoTexto = 'No confirmada';
                break;
            default:
                colorEvento = 'info';
                estadoTexto = reserva.estado;
        }

        // Si la reserva NO es del usuario, forzamos visualización privada
        let title = `${estadoTexto} - ${reserva.vecinoNombre} ${reserva.vecinoApellido}`;
        if (!esPropia) {
            colorEvento = 'danger';
            title = 'Reservado';
        }

        const eventoCalendario = {
            id: reserva.idReserva,
            title: title,
            start: fechaInicio,
            end: fechaFin,
            allDay: false,
            classNames: [`fc-event-${colorEvento}`],
            extendedProps: {
                calendar: estadoTexto.toLowerCase().replace(' ', '_'),
                reservaId: reserva.idReserva,
                vecino: reserva.vecinoNombre + ' ' + reserva.vecinoApellido,
                estado: reserva.estado,
                costo: reserva.costo,
                tipoPago: reserva.tipoPago || 'No especificado',
                momentoReserva: reserva.momentoReserva,
                espacioNombre: reserva.espacioNombre,
                esPropia: esPropia
            }
        };

        console.log("Evento de calendario creado:", eventoCalendario);
        return eventoCalendario;
    });

    console.log("Eventos totales procesados:", reservasData.length);
    console.log("Datos finales para FullCalendar:", reservasData);

    // Exportar para FullCalendar
    window.reservasEvents = reservasData;

    // También actualizar las reservas para el calendario personalizado
    actualizarReservasCalendarioPersonalizado(reservas);

    // Notificar que los datos están listos
    window.dispatchEvent(new CustomEvent('reservasDataReady'));
}

// Función para actualizar las reservas del calendario personalizado
function actualizarReservasCalendarioPersonalizado(reservas) {
    if (typeof window.reservations !== 'undefined') {
        // Actualizar el array global del calendario personalizado
        window.reservations = reservas.map(reserva => ({
            id: reserva.idReserva,
            date: reserva.fecha,
            startTime: reserva.horaInicio,
            endTime: reserva.horaFin,
            isOwn: reserva.esPropia === true,
            status: reserva.estado,
            user: `${reserva.vecinoNombre} ${reserva.vecinoApellido}`,
            cost: reserva.costo || 0
        }));

        console.log("Reservas actualizadas para calendario personalizado:", window.reservations);

        // Si el calendario personalizado ya está inicializado, re-renderizarlo
        if (typeof window.renderCalendar === 'function') {
            window.renderCalendar();
        }
    } else {
        // Si no existe, crear el array global
        window.reservations = reservas.map(reserva => ({
            id: reserva.idReserva,
            date: reserva.fecha,
            startTime: reserva.horaInicio,
            endTime: reserva.horaFin,
            isOwn: reserva.esPropia === true,
            status: reserva.estado,
            user: `${reserva.vecinoNombre} ${reserva.vecinoApellido}`,
            cost: reserva.costo || 0
        }));
    }
}

// Función para sincronizar datos entre ambos calendarios
function sincronizarCalendarios() {
    // Esta función puede ser llamada cuando se actualicen las reservas
    if (typeof window.reservasDelServidor !== 'undefined') {
        inicializarReservas(window.reservasDelServidor);
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("DOM cargado, verificando datos del servidor...");

    // Verificar si hay datos del servidor (puede ser array vacío)
    if (typeof window.reservasDelServidor !== 'undefined') {
        console.log("Datos del servidor encontrados:", window.reservasDelServidor);
        inicializarReservas(window.reservasDelServidor);
    } else {
        console.log("No se encontraron datos del servidor, inicializando con array vacío");
        inicializarReservas([]);
    }

    // Escuchar eventos de actualización de reservas
    window.addEventListener('reservasActualizadas', function(event) {
        console.log("Evento de reservas actualizadas recibido:", event.detail);
        if (event.detail && event.detail.reservas) {
            inicializarReservas(event.detail.reservas);
        }
    });
});