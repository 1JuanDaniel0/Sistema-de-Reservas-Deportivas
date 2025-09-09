// SCRIPT PARA PROCESAR RESERVAS Y MANTENIMIENTOS EN EL CALENDARIO DE ADMINISTRADOR
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

            // Determinar el color y texto según el estado
            let colorEvento;
            let estadoTexto;

            switch(reserva.estado) {
                case 'Confirmada':
                    colorEvento = 'success';
                    estadoTexto = 'Confirmada';
                    break;
                case 'Pendiente de Confirmación':
                    colorEvento = 'warning';
                    estadoTexto = 'No confirmada';
                    break;
                default:
                    colorEvento = 'info';
                    estadoTexto = reserva.estado;
            }

            // Para el admin, mostrar información completa
            let title = `${estadoTexto} - ${reserva.vecinoNombre} ${reserva.vecinoApellido}`;

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
                    esPropia: false // Admin ve todas las reservas
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

// Función para inicializar los datos de mantenimientos desde el servidor
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

            // Determinar color según prioridad y estado
            let colorEvento = 'danger'; // Rojo para mantenimiento por defecto

            // Ajustar color según prioridad
            if (mantenimiento.prioridad) {
                switch(mantenimiento.prioridad.toString().toUpperCase()) {
                    case 'BAJA':
                        colorEvento = 'info';
                        break;
                    case 'MEDIA':
                        colorEvento = 'warning';
                        break;
                    case 'ALTA':
                        colorEvento = 'danger';
                        break;
                    case 'URGENTE':
                        colorEvento = 'dark';
                        break;
                }
            }

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

            // Crear título del evento
            const prioridadTexto = mantenimiento.prioridad ? mantenimiento.prioridad.toString() : 'MEDIA';
            let title = `🔧 ${tipoTexto}`;
            if (prioridadTexto === 'URGENTE') {
                title = `🚨 ${tipoTexto} - URGENTE`;
            } else if (prioridadTexto === 'ALTA') {
                title = `⚠️ ${tipoTexto} - ALTA`;
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
                    descripcion: mantenimiento.descripcion || 'Sin descripción',
                    responsable: getResponsableName(mantenimiento),
                    costoEstimado: mantenimiento.costoEstimado || 0,
                    creadoPor: getCreadoPorName(mantenimiento),
                    fechaCreacion: mantenimiento.fechaCreacion
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

// Función para combinar reservas y mantenimientos
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

    // Verificar si hay datos de mantenimientos del servidor
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

// Función helper para obtener el nombre del responsable
function getResponsableName(mantenimiento) {
    if (mantenimiento.responsable) {
        if (mantenimiento.responsable.nombres && mantenimiento.responsable.apellidos) {
            return mantenimiento.responsable.nombres + ' ' + mantenimiento.responsable.apellidos;
        } else if (typeof mantenimiento.responsable === 'string') {
            return mantenimiento.responsable;
        }
    }
    return 'No asignado';
}

// Función helper para obtener el nombre de quien creó el mantenimiento
function getCreadoPorName(mantenimiento) {
    if (mantenimiento.creadoPor) {
        if (mantenimiento.creadoPor.nombres && mantenimiento.creadoPor.apellidos) {
            return mantenimiento.creadoPor.nombres + ' ' + mantenimiento.creadoPor.apellidos;
        } else if (typeof mantenimiento.creadoPor === 'string') {
            return mantenimiento.creadoPor;
        }
    }
    return 'Sistema';
}