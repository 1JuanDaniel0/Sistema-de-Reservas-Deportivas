"use strict";

let reservasData = []; // Variable global para almacenar las reservas
let mantenimientosData = []; // Variable global para almacenar los mantenimientos

// Función para inicializar los datos de reservas desde el servidor
function inicializarReservas(reservas) {
    console.log("Datos de reservas recibidos del servidor:", reservas);

    // Inicializar array vacío si no hay reservas
    if (!reservas || reservas.length === 0) {
        console.log("No hay reservas disponibles, inicializando array vacío");
        reservasData = [];
    } else {
        reservasData = reservas.map(reserva => {
            console.log("Procesando reserva:", reserva);

            // Combinar fecha y hora para crear el datetime completo
            const fechaInicio = new Date(reserva.fecha + 'T' + reserva.horaInicio);
            const fechaFin = new Date(reserva.fecha + 'T' + reserva.horaFin);

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
                case 'Pendiente de confirmación':
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
                id: 'reserva-' + reserva.idReserva,
                title: title,
                start: fechaInicio,
                end: fechaFin,
                allDay: false,
                classNames: [`fc-event-${colorEvento}`],
                extendedProps: {
                    calendar: estadoTexto.toLowerCase().replace(' ', '_'),
                    tipo: 'reserva',
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
    }

    console.log("Eventos de reservas procesados:", reservasData.length);

    // Combinar con mantenimientos y exportar
    combinarEventos();
}

// NUEVA FUNCIÓN: Inicializar los datos de mantenimientos desde el servidor
function inicializarMantenimientos(mantenimientos) {
    console.log("Datos de mantenimientos recibidos del servidor:", mantenimientos);

    // Inicializar array vacío si no hay mantenimientos
    if (!mantenimientos || mantenimientos.length === 0) {
        console.log("No hay mantenimientos disponibles, inicializando array vacío");
        mantenimientosData = [];
    } else {
        mantenimientosData = mantenimientos.map(mantenimiento => {
            console.log("Procesando mantenimiento:", mantenimiento);

            // Crear fechas de inicio y fin combinando fecha y hora
            const fechaInicio = new Date(mantenimiento.fechaInicio + 'T' + mantenimiento.horaInicio);
            const fechaFin = new Date(mantenimiento.fechaFin + 'T' + mantenimiento.horaFin);

            console.log("Mantenimiento - Fecha inicio:", fechaInicio);
            console.log("Mantenimiento - Fecha fin:", fechaFin);

            // Para vecinos, todos los mantenimientos son bloqueantes (color rojo)
            let colorEvento = 'danger'; // Siempre rojo para indicar que no está disponible

            // Determinar título según el tipo de mantenimiento
            let tipoTexto = 'Mantenimiento';
            if (mantenimiento.tipoMantenimiento) {
                switch(mantenimiento.tipoMantenimiento.toString().toUpperCase()) {
                    case 'PREVENTIVO':
                        tipoTexto = 'Mantenimiento Preventivo';
                        break;
                    case 'CORRECTIVO':
                        tipoTexto = 'Mantenimiento Correctivo';
                        break;
                    case 'LIMPIEZA':
                        tipoTexto = 'Limpieza Profunda';
                        break;
                    case 'REPARACION':
                        tipoTexto = 'Reparación';
                        break;
                    case 'INSTALACION':
                        tipoTexto = 'Instalación de Equipos';
                        break;
                    case 'OTRO':
                        tipoTexto = 'Otro Mantenimiento';
                        break;
                    default:
                        tipoTexto = mantenimiento.tipoMantenimiento;
                }
            }

            // Para vecinos, título simplificado
            let title = `🔧 ${tipoTexto}`;

            // Mostrar urgencia solo si es urgente
            const prioridadTexto = mantenimiento.prioridad ? mantenimiento.prioridad.toString() : 'MEDIA';
            if (prioridadTexto === 'URGENTE') {
                title = `🚨 ${tipoTexto} - URGENTE`;
                colorEvento = 'dark'; // Color más destacado para urgente
            }

            const eventoCalendario = {
                id: 'mantenimiento-' + mantenimiento.idMantenimiento,
                title: title,
                start: fechaInicio,
                end: fechaFin,
                allDay: false,
                classNames: [`fc-event-${colorEvento}`, 'fc-event-maintenance'],
                extendedProps: {
                    calendar: 'mantenimiento',
                    tipo: 'mantenimiento',
                    mantenimientoId: mantenimiento.idMantenimiento,
                    tipoMantenimiento: tipoTexto,
                    prioridad: prioridadTexto,
                    estado: mantenimiento.estado || 'PROGRAMADO',
                    descripcion: mantenimiento.descripcion || 'Mantenimiento programado',
                    // Para vecinos, información limitada
                    bloqueaReservas: true // Indicador para validaciones
                }
            };

            console.log("Evento de mantenimiento creado:", eventoCalendario);
            return eventoCalendario;
        });
    }

    console.log("Eventos de mantenimientos procesados:", mantenimientosData.length);

    // Combinar con reservas y exportar
    combinarEventos();
}

// NUEVA FUNCIÓN: Combinar reservas y mantenimientos
function combinarEventos() {
    const todosLosEventos = [...reservasData, ...mantenimientosData];

    console.log("Eventos totales combinados:", todosLosEventos.length);
    console.log("- Reservas:", reservasData.length);
    console.log("- Mantenimientos:", mantenimientosData.length);

    // Exportar la variable para que sea accesible desde otros scripts
    window.reservasEvents = todosLosEventos;

    // Notificar que los datos están listos
    window.dispatchEvent(new CustomEvent('reservasDataReady'));
}

// NUEVA FUNCIÓN: Verificar si hay conflicto con mantenimiento
function verificarConflictoMantenimiento(fechaHoraInicio, fechaHoraFin) {
    return mantenimientosData.some(mantenimiento => {
        const mantenimientoInicio = new Date(mantenimiento.start);
        const mantenimientoFin = new Date(mantenimiento.end);

        // Verificar solapamiento
        return (fechaHoraInicio < mantenimientoFin && fechaHoraFin > mantenimientoInicio);
    });
}

// Exportar función para uso en otros scripts
window.verificarConflictoMantenimiento = verificarConflictoMantenimiento;

// Inicializar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", function() {
    console.log("DOM cargado, verificando datos del servidor...");

    // Verificar si hay datos de reservas del servidor
    if (typeof window.reservasDelServidor !== 'undefined') {
        console.log("Datos de reservas del servidor encontrados:", window.reservasDelServidor);
        inicializarReservas(window.reservasDelServidor);
    } else {
        console.log("No se encontraron datos de reservas del servidor, inicializando con array vacío");
        inicializarReservas([]);
    }

    // NUEVO: Verificar si hay datos de mantenimientos del servidor
    if (typeof window.mantenimientosDelServidor !== 'undefined') {
        console.log("Datos de mantenimientos del servidor encontrados:", window.mantenimientosDelServidor);
        inicializarMantenimientos(window.mantenimientosDelServidor);
    } else {
        console.log("No se encontraron datos de mantenimientos del servidor, inicializando con array vacío");
        inicializarMantenimientos([]);
    }

    // Si no hay ningún dato, asegurar que se notifique
    setTimeout(() => {
        if (typeof window.reservasEvents === 'undefined') {
            console.log("Forzando inicialización con arrays vacíos");
            window.reservasEvents = [];
            window.dispatchEvent(new CustomEvent('reservasDataReady'));
        }
    }, 500);
});

function validarReservaContraMantenimiento(fecha, horaInicio, horaFin) {
    // Crear objetos Date para la reserva propuesta
    const fechaInicioReserva = new Date(fecha + 'T' + horaInicio);
    const fechaFinReserva = new Date(fecha + 'T' + horaFin);

    console.log("Validando reserva:", fechaInicioReserva, "a", fechaFinReserva);

    // Verificar conflicto con mantenimientos
    const hayConflicto = verificarConflictoMantenimiento(fechaInicioReserva, fechaFinReserva);

    if (hayConflicto) {
        // Encontrar el mantenimiento específico que causa conflicto
        const mantenimientoConflictivo = mantenimientosData.find(mantenimiento => {
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

window.validarReservaContraMantenimiento = validarReservaContraMantenimiento;