"use strict";

/**
 * Gestor de tooltips simplificado para el calendario personalizado
 */
class SimpleTooltipManager {
    constructor() {
        this.currentTooltip = null;
        this.currentElement = null;
        this.hideTimeout = null;
        this.init();
    }

    init() {
        // Crear contenedor para tooltips si no existe
        if (!document.getElementById('simple-tooltip-container')) {
            const container = document.createElement('div');
            container.id = 'simple-tooltip-container';
            container.style.position = 'absolute';
            container.style.top = '0';
            container.style.left = '0';
            container.style.zIndex = '1000';
            container.style.pointerEvents = 'none';
            document.body.appendChild(container);
        }

        // Agregar evento global para ocultar tooltip cuando el mouse sale del viewport
        document.addEventListener('mouseleave', () => {
            this.hideTooltip();
        });

        // Agregar evento para ocultar tooltip al hacer scroll
        window.addEventListener('scroll', () => {
            this.hideTooltip();
        });
    }

    // Crear contenido del tooltip para reservas propias
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

        const date = new Date(reservation.date);
        const formattedDate = date.toLocaleDateString('es-PE', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        return `${statusIcon} ${statusText}
📅 ${formattedDate}
🕐 ${formatTime(reservation.startTime)} - ${formatTime(reservation.endTime)}
💰 ${formatCurrency(reservation.cost)}
👤 ${reservation.user}`;
    }

    // Mostrar tooltip
    showTooltip(element, reservation, event) {
        if (!reservation.isOwn) return;

        // Cancelar timeout de ocultar si existe
        if (this.hideTimeout) {
            clearTimeout(this.hideTimeout);
            this.hideTimeout = null;
        }

        // Si ya hay un tooltip para este elemento, no crear otro
        if (this.currentElement === element && this.currentTooltip) {
            return;
        }

        // Ocultar tooltip anterior si existe
        this.hideTooltip();

        const content = this.createTooltipContent(reservation);
        if (!content) return;

        // Crear tooltip
        const tooltip = document.createElement('div');
        tooltip.className = 'custom-tooltip';
        tooltip.innerHTML = content;

        // Agregar al contenedor
        const container = document.getElementById('simple-tooltip-container');
        container.appendChild(tooltip);

        // Guardar referencias
        this.currentTooltip = tooltip;
        this.currentElement = element;

        // Posicionar tooltip
        this.positionTooltip(tooltip, event || element);

        // Mostrar con animación
        requestAnimationFrame(() => {
            tooltip.classList.add('show');
        });

        return tooltip;
    }

    // Ocultar tooltip con delay opcional
    hideTooltip(immediate = false) {
        if (immediate) {
            this.performHide();
        } else {
            // Agregar un pequeño delay para evitar parpadeo
            if (this.hideTimeout) {
                clearTimeout(this.hideTimeout);
            }
            this.hideTimeout = setTimeout(() => {
                this.performHide();
            }, 50);
        }
    }

    // Realizar el ocultamiento del tooltip
    performHide() {
        if (this.currentTooltip) {
            this.currentTooltip.classList.remove('show');
            setTimeout(() => {
                if (this.currentTooltip && this.currentTooltip.parentNode) {
                    this.currentTooltip.parentNode.removeChild(this.currentTooltip);
                }
                this.currentTooltip = null;
                this.currentElement = null;
            }, 250);
        }
        if (this.hideTimeout) {
            clearTimeout(this.hideTimeout);
            this.hideTimeout = null;
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

        // Posicionar inmediatamente para obtener dimensiones
        tooltip.style.left = x + 'px';
        tooltip.style.top = y + 'px';

        // Ajustar posición después de que esté en el DOM
        requestAnimationFrame(() => {
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
            let finalY = y - tooltipRect.height - 15;
            if (finalY < 10) {
                finalY = y + 30; // Mostrar debajo del cursor
            }

            tooltip.style.left = finalX + 'px';
            tooltip.style.top = finalY + 'px';
        });
    }

    // Configurar eventos para un elemento de reserva
    setupReservationTooltip(element, reservation) {
        if (!reservation.isOwn) return;

        // Limpiar eventos anteriores para evitar duplicados
        this.cleanupElementEvents(element);

        // Agregar clase para identificar elementos con tooltip
        element.classList.add('hoverable');

        // Crear funciones con referencias para poder removerlas después
        const showTooltipHandler = (e) => {
            e.stopPropagation();
            this.showTooltip(element, reservation, e);
        };

        const hideTooltipHandler = (e) => {
            // Verificar si el mouse realmente salió del elemento
            const rect = element.getBoundingClientRect();
            const isInsideElement = e.clientX >= rect.left && e.clientX <= rect.right &&
                e.clientY >= rect.top && e.clientY <= rect.bottom;

            if (!isInsideElement) {
                this.hideTooltip();
            }
        };

        const mouseMoveHandler = (e) => {
            if (this.currentTooltip && this.currentElement === element) {
                this.positionTooltip(this.currentTooltip, e);
            }
        };

        // Mouse events
        element.addEventListener('mouseenter', showTooltipHandler);
        element.addEventListener('mouseleave', hideTooltipHandler);
        element.addEventListener('mousemove', mouseMoveHandler);

        // Touch events para móviles
        element.addEventListener('touchstart', (e) => {
            e.preventDefault();
            this.showTooltip(element, reservation, e.touches[0]);
            // Auto-ocultar después de 3 segundos en móviles
            setTimeout(() => {
                if (this.currentElement === element) {
                    this.hideTooltip(true);
                }
            }, 3000);
        });

        // Guardar referencias para limpieza posterior
        element._tooltipHandlers = {
            mouseenter: showTooltipHandler,
            mouseleave: hideTooltipHandler,
            mousemove: mouseMoveHandler
        };
    }

    // Limpiar eventos de un elemento
    cleanupElementEvents(element) {
        if (element._tooltipHandlers) {
            element.removeEventListener('mouseenter', element._tooltipHandlers.mouseenter);
            element.removeEventListener('mouseleave', element._tooltipHandlers.mouseleave);
            element.removeEventListener('mousemove', element._tooltipHandlers.mousemove);
            delete element._tooltipHandlers;
        }
    }

    // Limpiar todos los tooltips y eventos
    cleanup() {
        this.hideTooltip(true);
        const elements = document.querySelectorAll('.reservation.hoverable');
        elements.forEach(element => {
            this.cleanupElementEvents(element);
            element.classList.remove('hoverable');
        });
    }
}

// Crear instancia global
window.simpleTooltipManager = new SimpleTooltipManager();

// Función global para configurar tooltips en reservas del calendario personalizado
window.setupCalendarTooltips = function() {
    if (typeof window.simpleTooltipManager === 'undefined' || typeof window.reservations === 'undefined') {
        return;
    }

    // Limpiar tooltips anteriores
    window.simpleTooltipManager.cleanup();

    const reservationElements = document.querySelectorAll('.reservation[data-reservation-id]');
    reservationElements.forEach(element => {
        const reservationId = element.getAttribute('data-reservation-id');
        const reservation = window.reservations.find(r => r.id == reservationId);
        if (reservation) {
            window.simpleTooltipManager.setupReservationTooltip(element, reservation);
        }
    });
};

// Configurar tooltips cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    // Escuchar cuando las reservas estén listas
    window.addEventListener('reservasDataReady', () => {
        setTimeout(() => {
            window.setupCalendarTooltips();
        }, 100);
    });
});

// Limpiar tooltips al cambiar de página
window.addEventListener('beforeunload', () => {
    if (window.simpleTooltipManager) {
        window.simpleTooltipManager.cleanup();
    }
});