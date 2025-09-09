"use strict";

/**
 * Utilidades para manejo correcto de zona horaria de Perú (UTC-5)
 */

// Función para obtener la fecha y hora actual de Perú de forma más precisa
function getPeruDateTime() {
    // Crear fecha en zona horaria de Lima
    const now = new Date();
    const utc = now.getTime() + (now.getTimezoneOffset() * 60000);
    const peruTime = new Date(utc + (-5 * 3600000)); // UTC-5

    console.log('Hora UTC:', new Date(utc).toISOString());
    console.log('Hora Perú calculada:', peruTime.toISOString());
    console.log('Hora Perú usando toLocaleString:', new Date().toLocaleString("en-US", {timeZone: 'America/Lima'}));

    return peruTime;
}

// Función alternativa usando Intl API (más precisa)
function getPeruDateTimeIntl() {
    const now = new Date();
    const formatter = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'America/Lima',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    });

    const parts = formatter.formatToParts(now);
    const dateStr = `${parts.find(p => p.type === 'year').value}-${parts.find(p => p.type === 'month').value}-${parts.find(p => p.type === 'day').value}`;
    const timeStr = `${parts.find(p => p.type === 'hour').value}:${parts.find(p => p.type === 'minute').value}:${parts.find(p => p.type === 'second').value}`;

    const peruDateTime = new Date(`${dateStr}T${timeStr}`);

    console.log('Hora Perú con Intl API:', peruDateTime.toISOString());
    return peruDateTime;
}

// Función para comparar fechas y horas en zona horaria de Perú
function isPastInPeru(dateStr, timeStr = null) {
    const peruNow = getPeruDateTimeIntl();

    if (timeStr) {
        // Comparar fecha y hora específica
        const [hours, minutes] = timeStr.split(':').map(Number);
        const checkDateTime = new Date(dateStr + 'T00:00:00');
        checkDateTime.setHours(hours, minutes, 0, 0);

        const isPast = checkDateTime < peruNow;
        console.log(`Verificando ${dateStr} ${timeStr}:`);
        console.log(`  - Fecha a verificar: ${checkDateTime.toISOString()}`);
        console.log(`  - Hora actual Perú: ${peruNow.toISOString()}`);
        console.log(`  - ¿Es pasado? ${isPast}`);

        return isPast;
    } else {
        // Comparar solo fecha (sin hora)
        const checkDate = new Date(dateStr + 'T00:00:00');
        const today = new Date(peruNow.getFullYear(), peruNow.getMonth(), peruNow.getDate());

        const isPast = checkDate < today;
        console.log(`Verificando fecha ${dateStr}:`);
        console.log(`  - Fecha a verificar: ${checkDate.toDateString()}`);
        console.log(`  - Fecha actual Perú: ${today.toDateString()}`);
        console.log(`  - ¿Es pasado? ${isPast}`);

        return isPast;
    }
}

// Función para formatear fecha en formato peruano
function formatPeruDate(date) {
    return new Intl.DateTimeFormat('es-PE', {
        timeZone: 'America/Lima',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long'
    }).format(date);
}

// Función para formatear hora en formato peruano
function formatPeruTime(date) {
    return new Intl.DateTimeFormat('es-PE', {
        timeZone: 'America/Lima',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    }).format(date);
}

// Función para obtener el rango de fechas válidas (desde hoy hasta 6 meses)
function getValidDateRange() {
    const peruNow = getPeruDateTimeIntl();
    const startDate = new Date(peruNow.getFullYear(), peruNow.getMonth(), peruNow.getDate());
    const endDate = new Date(startDate);
    endDate.setMonth(endDate.getMonth() + 6);

    return {
        start: startDate.toISOString().split('T')[0],
        end: endDate.toISOString().split('T')[0]
    };
}

// Función para verificar si una fecha está en el rango válido
function isDateInValidRange(dateStr) {
    const range = getValidDateRange();
    return dateStr >= range.start && dateStr <= range.end;
}

// Exportar funciones para uso global
window.PeruTimezone = {
    getDateTime: getPeruDateTimeIntl,
    isPast: isPastInPeru,
    formatDate: formatPeruDate,
    formatTime: formatPeruTime,
    getValidDateRange: getValidDateRange,
    isDateInValidRange: isDateInValidRange
};