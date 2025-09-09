"use strict";

/**
 * Gestor de tooltips para reservas del calendario
 */
class TooltipManager {
    constructor() {
        this.tooltips = new Map();
        this.init();
    }

    init() {
        // Crear contenedor para tooltips si no existe
        if (!document.getElementById('tooltip-container')) {
            const container = document.createElement('div');
            container.id = 'tooltip-container';
            container.style.position = 'absolute';
            container.style.top = '0';
            container.style.left = '0';
            container.style.zIndex = '9999';
            container.style.pointerEvents = 'none';
            document.body.appendChild(container);
        }
    }

    // Crear tooltip para reservas propias
    createTooltipContent(reservation) {
        if (!reservation.isOwn) {
            return null; // No mostrar tooltip para reservas de otros
        }

        const formatCurrency = (amount) => `S/ ${parseFloat(amount || 0).toFixed(2)}`;
        const formatTime = (time) => time.substring(0, 5); // HH:MM

        let statusText = '';
        let statusIcon = '';

        switch (reservation.status) {
            case 'Confirmada':
                statusText = 'Confirmada';
                statusIcon = '✅';
                break;
            case 'No confirmada':
                statusText = 'Pendiente de confirmación';
                statusIcon = '⏳';
                break;
            case 'Cancelada':
                statusText = 'Cancelada';
                statusIcon = '❌';
                break;
            default:
                statusText = reservation.status;
                statusIcon = 'ℹ️';
        }

        return `${statusIcon} ${statusText}
📅 ${this.formatDate(reservation.date)}
🕐 ${formatTime(reservation.startTime)} - ${formatTime(reservation.endTime)}
💰 ${formatCurrency(reservation.cost)}
👤 ${reservation.user}`;
    }

    // Formatear fecha en español
    formatDate(dateStr) {
        const date = new Date(dateStr);
        return date.toLocaleDateString('es-PE', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }

    // Mostrar tooltip
    showTooltip(element, reservation, event) {
        if (!reservation.isOwn) return;

        const content = this.createTooltipContent(reservation);
        if (!content) return;

        // Crear tooltip
        const tooltip = document.createElement('div');
        tooltip.className = 'custom-tooltip';
        tooltip.innerHTML = content;
        tooltip.style.opacity = '0';
        tooltip.style.transition = 'opacity 0.25s ease-in-out';

        // Agregar al contenedor
        const container = document.getElementById('tooltip-container');
        container.appendChild(tooltip);

        // Posicionar tooltip
        this.positionTooltip(tooltip, event || element);

        // Mostrar con animación
        requestAnimationFrame(() => {
            tooltip.style.opacity = '1';
        });

        // Guardar referencia
        this.tooltips.set(element, tooltip);

        return tooltip;
    }

    // Ocultar tooltip
    hideTooltip(element) {
        const tooltip = this.tooltips.get(element);
        if (tooltip) {
            tooltip.style.opacity = '0';
            setTimeout(() => {
                if (tooltip.parentNode) {
                    tooltip.parentNode.removeChild(tooltip);
                }
                this.tooltips.delete(element);
            }, 250);
        }
    }

    // Posicionar tooltip
    positionTooltip(tooltip, eventOrElement) {
        let x, y;

        if (eventOrElement.clientX !== undefined) {
            // Es un evento de mouse
            x = eventOrElement.clientX;
            y = eventOrElement.clientY;
        } else {
            // Es un elemento
            const rect = eventOrElement.getBoundingClientRect();
            x = rect.left + rect.width / 2;
            y = rect.top;
        }

        // Obtener dimensiones del tooltip
        const tooltipRect = tooltip.getBoundingClientRect();
        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;

        // Ajustar posición horizontal
        let finalX = x - tooltipRect.width / 2;
        if (finalX < 10) {
            finalX = 10;
        } else if (finalX + tooltipRect.width > windowWidth - 10) {
            finalX = windowWidth - tooltipRect.width - 10;
        }

        // Ajustar posición vertical
        let finalY = y - tooltipRect.height - 10;
        if (finalY < 10) {
            finalY = y + 25; // Mostrar debajo del cursor
        }

        tooltip.style.left = finalX + 'px';
        tooltip.style.top = finalY + 'px';
    }

    // Actualizar posición del tooltip durante el movimiento del mouse
    updateTooltipPosition(element, event) {
        const tooltip = this.tooltips.get(element);
        if (tooltip) {
            this.positionTooltip(tooltip, event);
        }
    }

    // Configurar eventos para un elemento de reserva
    setupReservationTooltip(element, reservation) {
        if (!reservation.isOwn) return;

        // Agregar clase para identificar elementos con tooltip
        element.classList.add('hoverable');

        // Mouse enter
        element.addEventListener('mouseenter', (e) => {
            this.showTooltip(element, reservation, e);
        });

        // Mouse move
        element.addEventListener('mousemove', (e) => {
            this.updateTooltipPosition(element, e);
        });

        // Mouse leave
        element.addEventListener('mouseleave', () => {
            this.hideTooltip(element);
        });

        // Touch events para móviles
        element.addEventListener('touchstart', (e) => {
            e.preventDefault();
            this.showTooltip(element, reservation, e.touches[0]);
        });

        element.addEventListener('touchend', () => {
            setTimeout(() => this.hideTooltip(element), 2000);
        });
    }

    // Configurar tooltips para FullCalendar
    setupFullCalendarTooltips() {
        // Esta función se puede llamar después de que FullCalendar renderice los eventos
        document.addEventListener('DOMContentLoaded', () => {
            // Observar cambios en el DOM para detectar nuevos eventos de FullCalendar
            const observer = new MutationObserver((mutations) => {
                mutations.forEach((mutation) => {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            const events = node.querySelectorAll('.fc-event');
                            events.forEach(eventElement => {
                                this.setupFullCalendarEventTooltip(eventElement);
                            });
                        }
                    });
                });
            });

            // Observar el contenedor del calendario
            const calendarContainer = document.querySelector('.fc-view-harness');
            if (calendarContainer) {
                observer.observe(calendarContainer, {
                    childList: true,
                    subtree: true
                });
            }
        });
    }

    // Configurar tooltip para un evento específico de FullCalendar
    setupFullCalendarEventTooltip(eventElement) {
        const eventData = this.getFullCalendarEventData(eventElement);
        if (eventData && eventData.isOwn) {
            this.setupReservationTooltip(eventElement, eventData);
        }
    }

    // Obtener datos del evento de FullCalendar
    getFullCalendarEventData(eventElement) {
        // Intentar obtener datos del evento desde diferentes fuentes
        const fcEvent = eventElement.fcSeg?.eventRange?.def;
        if (fcEvent && fcEvent.extendedProps) {
            const props = fcEvent.extendedProps;
            return {
                id: props.reservaId,
                isOwn: props.esPropia,
                status: props.estado,
                date: fcEvent.publicId ? new Date(fcEvent.start).toISOString().split('T')[0] : '',
                startTime: fcEvent.start ? new Date(fcEvent.start).toTimeString().substring(0, 5) : '',
                endTime: fcEvent.end ? new Date(fcEvent.end).toTimeString().substring(0, 5) : '',
                cost: props.costo,
                user: props.vecino
            };
        }
        return null;
    }

    // Limpiar todos los tooltips
    clearAllTooltips() {
        this.tooltips.forEach((tooltip, element) => {
            this.hideTooltip(element);
        });
    }
}

// Crear instancia global
window.tooltipManager = new TooltipManager();

// Configurar tooltips cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    // Configurar para FullCalendar si está presente
    if (typeof FullCalendar !== 'undefined') {
        window.tooltipManager.setupFullCalendarTooltips();
    }
});

// Función global para configurar tooltips en reservas del calendario personalizado
window.setupCustomCalendarTooltips = function(reservationElements) {
    reservationElements.forEach(element => {
        const reservationId = element.getAttribute('data-reservation-id');
        const reservation = window.reservations?.find(r => r.id == reservationId);
        if (reservation) {
            window.tooltipManager.setupReservationTooltip(element, reservation);
        }
    });
};