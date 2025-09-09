"use strict";

// Función para llenar los selects de hora - MEJORADA
function populateTimeSelects(date, selectedTime = null) {
    const horaInicioSelect = document.getElementById('reservaHoraInicio');
    const horaFinSelect = document.getElementById('reservaHoraFin');

    if (!horaInicioSelect || !horaFinSelect) {
        console.warn('No se encontraron los elementos de selección de hora');
        return;
    }

    // Limpiar selects
    horaInicioSelect.innerHTML = '<option value="" disabled selected>Seleccione hora de inicio</option>';
    horaFinSelect.innerHTML = '<option value="" disabled selected>Seleccione hora de fin</option>';

    // Obtener reservas existentes para este día
    const existingReservations = reservations.filter(r => r.date === date);

    for (let h = 8; h <= 22; h++) {
        const timeStr = `${h.toString().padStart(2, '0')}:00`;

        // Verificar si esta hora está disponible (no hay reservas que la cubran)
        const isBlocked = existingReservations.some(r => {
            const startHour = parseInt(r.startTime.split(':')[0]);
            const endHour = parseInt(r.endTime.split(':')[0]);
            return h >= startHour && h < endHour;
        });

        // Verificar si es hora pasada usando la función corregida
        const isPastTime = isPast(date, timeStr);

        if (!isBlocked && !isPastTime) {
            const optionInicio = new Option(timeStr, h);
            horaInicioSelect.appendChild(optionInicio);
        }
    }

    // Si se especificó una hora, seleccionarla
    if (selectedTime) {
        const selectedHour = parseInt(selectedTime.split(':')[0]);
        horaInicioSelect.value = selectedHour;
        updateFinOptions();
    }
}

// Función para actualizar opciones de hora fin - MEJORADA
function updateFinOptions() {
    const horaInicioSelect = document.getElementById('reservaHoraInicio');
    const horaFinSelect = document.getElementById('reservaHoraFin');
    const selectedDate = document.getElementById('reservaFecha')?.value;

    if (!horaInicioSelect || !horaFinSelect || !selectedDate) {
        console.warn('No se encontraron todos los elementos necesarios para actualizar opciones de fin');
        return;
    }

    const inicioHora = parseInt(horaInicioSelect.value);

    // Limpiar opciones de fin
    horaFinSelect.innerHTML = '<option value="" disabled selected>Seleccione hora de fin</option>';

    if (!inicioHora) return;

    // Obtener reservas existentes para verificar disponibilidad
    const existingReservations = reservations.filter(r => r.date === selectedDate);

    // Encontrar el próximo bloque ocupado después de la hora de inicio
    let nextBlockedHour = 24; // Por defecto, permitir hasta las 23:00

    for (let checkHour = inicioHora; checkHour <= 23; checkHour++) {
        const conflict = existingReservations.some(r => {
            const startHour = parseInt(r.startTime.split(':')[0]);
            const endHour = parseInt(r.endTime.split(':')[0]);
            return checkHour >= startHour && checkHour < endHour;
        });

        if (conflict) {
            nextBlockedHour = checkHour;
            break;
        }
    }

    // Agregar opciones válidas hasta el próximo bloque ocupado o hasta las 23:00
    const maxHour = Math.min(nextBlockedHour, 23);

    for (let h = inicioHora + 1; h <= maxHour; h++) {
        const timeStr = `${h.toString().padStart(2, '0')}:00`;
        const option = new Option(timeStr, h);
        horaFinSelect.appendChild(option);
    }
}

// Función para configurar eventos del formulario - MEJORADA
function setupFormEvents() {
    const horaInicioSelect = document.getElementById('reservaHoraInicio');
    const horaFinSelect = document.getElementById('reservaHoraFin');

    if (!horaInicioSelect || !horaFinSelect) {
        console.warn('No se encontraron los elementos de selección de hora para configurar eventos');
        return;
    }

    // Remover eventos anteriores para evitar duplicados
    horaInicioSelect.onchange = null;
    horaFinSelect.onchange = null;

    // Evento para cuando cambia la hora de inicio
    horaInicioSelect.onchange = function() {
        updateFinOptions();
        calcularCosto();
    };

    horaFinSelect.onchange = calcularCosto;
}

// Función para calcular costo - MEJORADA
function calcularCosto() {
    const horaInicioSelect = document.getElementById('reservaHoraInicio');
    const horaFinSelect = document.getElementById('reservaHoraFin');
    const costoElement = document.getElementById('espacioCosto');
    const reservaCostoElement = document.getElementById('reservaCosto');
    const detalleHorasElement = document.getElementById('detalleHoras');

    if (!horaInicioSelect || !horaFinSelect || !costoElement || !reservaCostoElement) {
        console.warn('No se encontraron todos los elementos necesarios para calcular el costo');
        return;
    }

    const inicio = horaInicioSelect.value;
    const fin = horaFinSelect.value;

    if (inicio && fin) {
        const hi = parseInt(inicio);
        const hf = parseInt(fin);
        const duracion = hf - hi;

        if (duracion > 0) {
            const costoHora = parseFloat(costoElement.value) || 25;
            const total = duracion * costoHora;
            reservaCostoElement.value = total.toFixed(2);

            if (detalleHorasElement) {
                detalleHorasElement.textContent =
                    `${duracion} hora${duracion > 1 ? 's' : ''} × S/ ${costoHora.toFixed(2)}`;
            }
        } else {
            reservaCostoElement.value = '0.00';
            if (detalleHorasElement) {
                detalleHorasElement.textContent = '';
            }
        }
    } else {
        reservaCostoElement.value = '0.00';
        if (detalleHorasElement) {
            detalleHorasElement.textContent = '';
        }
    }
}