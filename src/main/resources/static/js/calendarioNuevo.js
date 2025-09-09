"use strict";

// Variables globales
let currentDate = new Date();
let currentView = 'month';
let reservations = [];

// Función para obtener la fecha y hora actual de Perú (UTC-5)
function getCurrentPeruTime() {
    // Usar las utilidades de zona horaria si están disponibles
    if (typeof window.PeruTimezone !== 'undefined') {
        return window.PeruTimezone.getDateTime();
    }

    // Fallback: método anterior mejorado
    const now = new Date();
    const utc = now.getTime() + (now.getTimezoneOffset() * 60000);
    const peruTime = new Date(utc + (-5 * 3600000)); // UTC-5
    console.log('Hora actual de Perú (fallback):', peruTime.toISOString());
    return peruTime;
}

// Función para verificar si una fecha/hora está en el pasado - CORREGIDA
function isPast(date, time = null) {
    // Usar las utilidades de zona horaria si están disponibles
    if (typeof window.PeruTimezone !== 'undefined') {
        return window.PeruTimezone.isPast(date, time);
    }

    // Fallback: lógica mejorada
    const now = getCurrentPeruTime();

    if (time) {
        // Para verificar hora específica
        const [hours, minutes] = time.split(':').map(Number);
        const checkDateTime = new Date(date + 'T00:00:00');
        checkDateTime.setHours(hours, minutes, 0, 0);

        const isPastTime = checkDateTime < now;
        console.log(`Verificando ${date} ${time}: checkDateTime=${checkDateTime.toISOString()}, now=${now.toISOString()}, isPast=${isPastTime}`);
        return isPastTime;
    } else {
        // Para fechas completas, solo comparar el día
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const compareDate = new Date(date + 'T00:00:00');
        const compareDateOnly = new Date(compareDate.getFullYear(), compareDate.getMonth(), compareDate.getDate());

        const isPastDate = compareDateOnly < today;
        console.log(`Verificando fecha ${date}: compareDate=${compareDateOnly.toDateString()}, today=${today.toDateString()}, isPast=${isPastDate}`);
        return isPastDate;
    }
}

// Función para cambiar vista
function changeView(view) {
    currentView = view;
    document.querySelectorAll('.view-btn-func').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    renderCalendar();
}

// Función para navegar
function navigateCalendar(direction) {
    if (currentView === 'month') {
        currentDate.setMonth(currentDate.getMonth() + direction);
    } else if (currentView === 'week') {
        currentDate.setDate(currentDate.getDate() + (direction * 7));
    } else if (currentView === 'day') {
        currentDate.setDate(currentDate.getDate() + direction);
    }
    renderCalendar();
}

// Función para renderizar el calendario
function renderCalendar() {
    const grid = document.getElementById('calendarGrid');
    const title = document.getElementById('calendarTitle');

    if (currentView === 'month') {
        renderMonthView(grid, title);
    } else if (currentView === 'week') {
        renderWeekView(grid, title);
    } else if (currentView === 'day') {
        renderDayView(grid, title);
    }
}

// Vista de Mes
function renderMonthView(grid, title) {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());

    title.textContent = firstDay.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });

    let html = '<div class="month-view">';

    // Headers de días
    const dayNames = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
    dayNames.forEach(day => {
        html += `<div class="month-header">${day}</div>`;
    });

    // Días del mes
    for (let i = 0; i < 42; i++) {
        const currentDay = new Date(startDate);
        currentDay.setDate(startDate.getDate() + i);
        const dayStr = currentDay.toISOString().split('T')[0];
        const isCurrentMonth = currentDay.getMonth() === month;

        // Usar hora de Perú para determinar si es hoy
        const today = getCurrentPeruTime();
        const isToday = currentDay.toDateString() === today.toDateString();
        const isPastDay = isPast(dayStr);

        let classes = 'month-day';
        if (!isCurrentMonth) classes += ' other-month';
        if (isToday) classes += ' today';
        if (isPastDay) classes += ' disabled';

        // Buscar reservas para este día
        const dayReservations = reservations.filter(r => r.date === dayStr);
        let reservationsHtml = '';

        dayReservations.forEach(reservation => {
            const reservClass = reservation.isOwn ?
                (reservation.status === 'Confirmada' ? 'own confirmada' : 'own no-confirmada') :
                'other';

            const displayText = reservation.isOwn ?
                `${reservation.startTime}-${reservation.endTime}` :
                'Reservado';

            reservationsHtml += `
                <div class="reservation ${reservClass}" 
                     data-reservation-id="${reservation.id}"
                     onclick="handleReservationClick(${reservation.id})" 
                     title="${reservation.isOwn ? 'Mi reserva - Hover para más detalles' : 'Reserva de otro usuario'}">
                    ${displayText}
                </div>
            `;
        });

        html += `
            <div class="${classes}" onclick="handleDayClick('${dayStr}')">
                <div class="day-number">${currentDay.getDate()}</div>
                ${reservationsHtml}
                ${!isPastDay && isCurrentMonth ? '<button class="add-reservation-btn" onclick="event.stopPropagation(); openReservationModal(\'' + dayStr + '\')"><i class="fas fa-plus"></i></button>' : ''}
            </div>
        `;
    }

    html += '</div>';
    grid.innerHTML = html;
}

// Vista de Semana
function renderWeekView(grid, title) {
    // Cambiar para que la semana empiece en lunes
    const startOfWeek = new Date(currentDate);
    const dayOfWeek = startOfWeek.getDay();
    const daysToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    startOfWeek.setDate(currentDate.getDate() + daysToMonday);

    const endOfWeek = new Date(startOfWeek);
    endOfWeek.setDate(startOfWeek.getDate() + 6);

    title.textContent = `${startOfWeek.toLocaleDateString('es-ES')} - ${endOfWeek.toLocaleDateString('es-ES')}`;

    let html = '<div class="week-view">';

    // Header de horas
    html += '<div class="week-header">Intervalo</div>';
    const dayNames = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
    for (let i = 0; i < 7; i++) {
        const day = new Date(startOfWeek);
        day.setDate(startOfWeek.getDate() + i);
        html += `<div class="week-header">${dayNames[i]}<br><small>${day.getDate()}</small></div>`;
    }

    // Intervalos de tiempo de 1 hora (8:00-9:00, 9:00-10:00, etc.)
    for (let hour = 8; hour <= 22; hour++) {
        const startTime = `${hour.toString().padStart(2, '0')}:00`;
        const endTime = `${(hour + 1).toString().padStart(2, '0')}:00`;
        html += `<div class="time-slot">${startTime}<br>-<br>${endTime}</div>`;

        for (let day = 0; day < 7; day++) {
            const currentDay = new Date(startOfWeek);
            currentDay.setDate(startOfWeek.getDate() + day);
            const dayStr = currentDay.toISOString().split('T')[0];

            // Verificar si este intervalo de hora está en el pasado
            const isPastSlot = isPast(dayStr, startTime);

            // Usar hora de Perú para determinar si es hoy
            const today = getCurrentPeruTime();
            const isToday = currentDay.toDateString() === today.toDateString();

            let classes = 'week-day-slot';
            if (isPastSlot) classes += ' disabled';
            if (isToday) classes += ' today';

            // Buscar reservas que cubran este intervalo de hora
            const slotReservations = reservations.filter(r => {
                if (r.date !== dayStr) return false;
                const startHour = parseInt(r.startTime.split(':')[0]);
                const endHour = parseInt(r.endTime.split(':')[0]);
                return hour >= startHour && hour < endHour;
            });

            let reservationsHtml = '';
            slotReservations.forEach(reservation => {
                const reservClass = reservation.isOwn ?
                    (reservation.status === 'Confirmada' ? 'own confirmada' : 'own no-confirmada') :
                    'other';

                const displayText = reservation.isOwn ?
                    `${reservation.startTime}-${reservation.endTime}` :
                    'Reservado';

                reservationsHtml += `
                    <div class="reservation ${reservClass}" 
                         data-reservation-id="${reservation.id}"
                         onclick="handleReservationClick(${reservation.id})">
                        ${displayText}
                    </div>
                `;
            });

            html += `
                <div class="${classes}" onclick="handleTimeSlotClick('${dayStr}', '${startTime}')">
                    ${reservationsHtml}
                    ${!isPastSlot ? '<button class="add-reservation-btn" onclick="event.stopPropagation(); openReservationModal(\'' + dayStr + '\', \'' + startTime + '\')"><i class="fas fa-plus"></i></button>' : ''}
                </div>
            `;
        }
    }

    html += '</div>';
    grid.innerHTML = html;
}

// Vista de Día
function renderDayView(grid, title) {
    const dayStr = currentDate.toISOString().split('T')[0];
    title.textContent = currentDate.toLocaleDateString('es-ES', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });

    let html = '<div class="day-view">';

    // Header
    html += '<div class="week-header">Intervalo</div>';
    html += '<div class="week-header">Reservas</div>';

    // Intervalos de tiempo de 1 hora
    const now = getCurrentPeruTime();
    const currentHour = now.getHours();

    for (let hour = 8; hour <= 22; hour++) {
        const startTime = `${hour.toString().padStart(2, '0')}:00`;
        const endTime = `${(hour + 1).toString().padStart(2, '0')}:00`;

        // Verificar si este intervalo está en el pasado
        const isPastSlot = isPast(dayStr, startTime);

        // Determinar si es el intervalo actual
        const isCurrentHour = currentDate.toDateString() === now.toDateString() &&
            hour <= currentHour && currentHour < (hour + 1);

        let classes = 'day-slot';
        if (isPastSlot) classes += ' disabled';
        if (isCurrentHour) classes += ' current-hour';

        // Buscar reservas que cubran este intervalo
        const slotReservations = reservations.filter(r => {
            if (r.date !== dayStr) return false;
            const startHour = parseInt(r.startTime.split(':')[0]);
            const endHour = parseInt(r.endTime.split(':')[0]);
            return hour >= startHour && hour < endHour;
        });

        let reservationsHtml = '';
        slotReservations.forEach(reservation => {
            const reservClass = reservation.isOwn ?
                (reservation.status === 'Confirmada' ? 'own confirmada' : 'own no-confirmada') :
                'other';

            const displayText = reservation.isOwn ?
                `Mi reserva: ${reservation.startTime}-${reservation.endTime}` :
                'Reservado';

            reservationsHtml += `
                <div class="reservation ${reservClass}" 
                     data-reservation-id="${reservation.id}"
                     onclick="handleReservationClick(${reservation.id})">
                    ${displayText}
                    ${reservation.isOwn ? `<span class="reservation-time">S/. ${reservation.cost}</span>` : ''}
                </div>
            `;
        });

        html += `<div class="time-slot">${startTime}<br>-<br>${endTime}</div>`;
        html += `
            <div class="${classes}" onclick="handleTimeSlotClick('${dayStr}', '${startTime}')">
                ${reservationsHtml}
                ${!isPastSlot ? '<button class="add-reservation-btn" onclick="event.stopPropagation(); openReservationModal(\'' + dayStr + '\', \'' + startTime + '\')"><i class="fas fa-plus"></i></button>' : ''}
            </div>
        `;
    }

    html += '</div>';
    grid.innerHTML = html;
}

// Manejadores de eventos
function handleDayClick(date) {
    if (isPast(date)) {
        return;
    }
    openReservationModal(date);
}

function handleTimeSlotClick(date, time) {
    if (isPast(date, time)) {
        return;
    }
    openReservationModal(date, time);
}

function handleReservationClick(reservationId) {
    const reservation = reservations.find(r => r.id === reservationId);
    if (reservation && reservation.isOwn) {
        console.log('Mi reserva:', reservation);
    } else {
        console.log('Reserva de otro usuario');
    }
}

// Función para abrir modal de reserva
function openReservationModal(date, time = null) {
    const offcanvas = new bootstrap.Offcanvas(document.getElementById('crearReservaOffcanvas'));
    offcanvas.show();

    // Llenar el formulario usando la función global si existe
    if (typeof window.llenarOffcanvasReserva === 'function') {
        window.llenarOffcanvasReserva(date);
        return;
    }

    // Fallback: llenar manualmente
    document.getElementById('reservaFecha').value = date;

    if (window.usuarioSesionNombre) {
        document.getElementById('reservaVecino').value = window.usuarioSesionNombre;
    }

    if (window.espacioSeleccionadoNombre) {
        document.getElementById('reservaEspacio').value = window.espacioSeleccionadoNombre;
    }

    if (window.espacioSeleccionadoCostoHora) {
        document.getElementById('espacioCosto').value = window.espacioSeleccionadoCostoHora;
    }

    // Llenar horas disponibles
    populateTimeSelects(date, time);
    setupFormEvents();
}

// Función para cargar datos desde el servidor - MEJORADA
function loadReservationsFromServer() {
    if (typeof window.reservasDelServidor !== 'undefined' && window.reservasDelServidor.length > 0) {
        console.log('Datos del servidor encontrados:', window.reservasDelServidor);

        // Mapear datos del servidor al formato del calendario
        reservations = window.reservasDelServidor.map(reserva => {
            console.log('Procesando reserva:', reserva);

            return {
                id: reserva.idReserva,
                date: reserva.fecha,
                startTime: reserva.horaInicio,
                endTime: reserva.horaFin,
                isOwn: reserva.esPropia === true,
                status: reserva.estado,
                user: `${reserva.vecinoNombre} ${reserva.vecinoApellido}`,
                cost: reserva.costo || 0
            };
        });

        console.log('Reservas procesadas para calendario:', reservations);
    } else {
        console.log('No hay datos del servidor, inicializando array vacío');
        reservations = [];
    }
}

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    console.log('Inicializando calendario personalizado...');

    // Cargar datos del servidor
    loadReservationsFromServer();

    // Configurar variables globales del espacio si existen
    if (typeof window.espacioSeleccionadoNombre !== 'undefined') {
        const espacioElement = document.getElementById('reservaEspacio');
        if (espacioElement) {
            espacioElement.value = window.espacioSeleccionadoNombre;
        }
    }

    if (typeof window.espacioSeleccionadoCostoHora !== 'undefined') {
        const costoElement = document.getElementById('espacioCosto');
        if (costoElement) {
            costoElement.value = window.espacioSeleccionadoCostoHora;
        }
    }

    if (typeof window.usuarioSesionNombre !== 'undefined') {
        const vecinoElement = document.getElementById('reservaVecino');
        if (vecinoElement) {
            vecinoElement.value = window.usuarioSesionNombre;
        }
    }

    // Renderizar calendario inicial
    renderCalendar();

    // Configurar formulario una vez al cargar
    setupFormEvents();

    // Configurar tooltips después del primer render
    setTimeout(() => {
        if (typeof window.setupCalendarTooltips === 'function') {
            window.setupCalendarTooltips();
        }
    }, 100);

    // Actualizar cada minuto para mantener sincronización
    setInterval(() => {
        renderCalendar();
        // Reconfigurar tooltips después de cada render
        setTimeout(() => {
            if (typeof window.setupCalendarTooltips === 'function') {
                window.setupCalendarTooltips();
            }
        }, 100);
    }, 60000);

    console.log('Calendario personalizado inicializado correctamente');
});