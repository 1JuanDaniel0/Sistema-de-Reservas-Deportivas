"use strict";

class ToastManager {
    constructor() {
        this.activeToasts = new Map(); // Mapa de tipos de toast activos
        this.lastToastTime = new Map(); // Tiempo del último toast por tipo
        this.debounceTime = 1000; // 1 segundo de debounce
    }

    canShowToast(type) {
        const now = Date.now();
        const lastTime = this.lastToastTime.get(type) || 0;

        // Si hay un toast activo del mismo tipo, no mostrar
        if (this.activeToasts.has(type)) {
            console.log(`🚫 Toast bloqueado - ya existe uno activo del tipo: ${type}`);
            return false;
        }

        // Si es muy pronto desde el último toast del mismo tipo, no mostrar
        if (now - lastTime < this.debounceTime) {
            console.log(`🚫 Toast bloqueado - muy pronto desde el último: ${type}`);
            return false;
        }

        return true;
    }

    showToast(message, type = 'info', duration = 3000) {
        // Verificar si se puede mostrar el toast
        if (!this.canShowToast(type)) {
            return false;
        }

        // Marcar como activo
        this.activeToasts.set(type, true);
        this.lastToastTime.set(type, Date.now());

        // Mostrar el toast
        this.displayToast(message, type, duration, () => {
            // Callback cuando se oculta el toast
            this.activeToasts.delete(type);
        });

        return true;
    }

    displayToast(message, type, duration, onHidden) {
        // Usar la función avanzada si está disponible
        if (window.mostrarToastAvanzado) {
            window.mostrarToastAvanzado(message, type, {
                duracion: duration,
                onHidden: onHidden
            });

            // Simular el callback ya que mostrarToastAvanzado no lo soporta
            setTimeout(onHidden, duration);
            return;
        }

        // Fallback simple
        const typeMap = {
            'error': 'danger',
            'success': 'success',
            'warning': 'warning',
            'info': 'info'
        };

        const bootstrapType = typeMap[type] || 'info';
        const toastId = 'toast-' + Date.now();

        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${bootstrapType} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;

        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: duration });

        toastElement.addEventListener('hidden.bs.toast', function() {
            this.remove();
            if (onHidden) onHidden();
        });

        toast.show();
    }

    // Limpiar toasts activos (útil para debugging)
    clearActiveToasts() {
        this.activeToasts.clear();
        this.lastToastTime.clear();
    }
}

// Instancia global del gestor de toasts
window.toastManager = new ToastManager();

class DateFormatter {
    static formatToLocal(dateString) {
        try {
            if (!dateString) return '';

            let date;

            // Si es formato YYYY-MM-DD, forzar interpretación local
            if (dateString.includes('-') && dateString.split('-').length === 3) {
                const [year, month, day] = dateString.split('-');
                // Crear fecha local evitando problemas de zona horaria
                date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
            }
            // Si ya es un objeto Date
            else if (dateString instanceof Date) {
                date = dateString;
            }
            // Intentar parsear directamente
            else {
                date = new Date(dateString);
            }

            if (isNaN(date.getTime())) {
                console.warn('Fecha inválida:', dateString);
                return dateString; // Retornar original si no se puede parsear
            }

            // Formatear como DD/MM/YYYY
            const day = date.getDate().toString().padStart(2, '0');
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const year = date.getFullYear();

            return `${day}/${month}/${year}`;
        } catch (error) {
            console.error('Error formateando fecha:', error);
            return dateString;
        }
    }

    static formatToServer(localDateString) {
        try {
            if (!localDateString) return '';

            // Si es formato DD/MM/YYYY, convertir a YYYY-MM-DD
            if (localDateString.includes('/') && localDateString.split('/').length === 3) {
                const [day, month, year] = localDateString.split('/');
                return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
            }

            // Si ya es formato servidor, retornar tal como está
            return localDateString;
        } catch (error) {
            console.error('Error convirtiendo fecha a formato servidor:', error);
            return localDateString;
        }
    }

    static formatDateForDisplay(date) {
        if (!date) return '';

        try {
            let dateObj;

            // Si es formato YYYY-MM-DD, crear fecha local
            if (typeof date === 'string' && date.includes('-') && date.split('-').length === 3) {
                const [year, month, day] = date.split('-');
                dateObj = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
            } else {
                dateObj = typeof date === 'string' ? new Date(date) : date;
            }

            if (isNaN(dateObj.getTime())) {
                return date.toString();
            }

            // Formatear para mostrar: "31 de julio de 2025"
            const options = {
                day: 'numeric',
                month: 'long',
                year: 'numeric'
            };

            return dateObj.toLocaleDateString('es-PE', options);
        } catch (error) {
            console.error('Error formateando fecha para display:', error);
            return date.toString();
        }
    }
}

// Hacer disponible globalmente
window.DateFormatter = DateFormatter;

class OffcanvasManager {
    static init() {
        const offcanvasElement = document.getElementById('crearReservaOffcanvas');
        if (!offcanvasElement) return;

        // Función para limpiar todos los campos del formulario
        const limpiarFormulario = () => {
            console.log('🧹 Limpiando formulario del offcanvas...');

            // Limpiar campos de texto y hidden
            const campos = [
                'reservaFecha',
                'reservaCosto'
            ];

            campos.forEach(id => {
                const campo = document.getElementById(id);
                if (campo) {
                    campo.value = '';
                }
            });

            // Resetear selects a sus valores por defecto - VERSIÓN CORREGIDA PARA SELECTPICKER
            const selects = [
                'reservaHoraInicio',
                'reservaHoraFin',
                'reservaTipoPago',
                'reservaCoordinador'
            ];

            selects.forEach(id => {
                const select = document.getElementById(id);
                if (select) {
                    // Si es un Bootstrap Selectpicker, usar su método específico
                    if (select.classList.contains('selectpicker')) {
                        try {
                            // Destruir el selectpicker existente ANTES de resetear
                            $(select).selectpicker('destroy');

                            // Resetear el select original
                            select.value = '';
                            select.selectedIndex = 0;

                            // Buscar y seleccionar la opción con "disabled selected"
                            const defaultOption = select.querySelector('option[disabled][selected]');
                            if (defaultOption) {
                                // Remover selected de todas las opciones
                                Array.from(select.options).forEach(option => {
                                    option.selected = false;
                                });
                                // Seleccionar la opción por defecto
                                defaultOption.selected = true;
                            }

                            // Reinicializar el selectpicker
                            $(select).selectpicker();

                            console.log(`✅ Selectpicker ${id} reinicializado correctamente`);
                        } catch (e) {
                            console.log('⚠️ Selectpicker no disponible para', id, '- usando método estándar');
                            // Fallback para selects normales
                            select.value = '';
                            select.selectedIndex = 0;
                        }
                    } else {
                        // Para selects normales (sin selectpicker)
                        select.value = '';

                        // Buscar y seleccionar la opción con "disabled selected"
                        const defaultOption = select.querySelector('option[disabled][selected]');
                        if (defaultOption) {
                            Array.from(select.options).forEach(option => {
                                option.selected = false;
                            });
                            defaultOption.selected = true;
                        }
                    }

                    // Disparar evento change para notificar el reseteo
                    select.dispatchEvent(new Event('change', { bubbles: true }));
                }
            });

            // Limpiar texto de detalles de hora
            const detalleHoras = document.getElementById('detalleHoras');
            if (detalleHoras) {
                detalleHoras.textContent = '';
            }

            // Resetear botón a estado inicial
            const btnPagar = document.getElementById('btnPagarReservar');
            if (btnPagar) {
                btnPagar.textContent = 'Seleccione tipo de pago';
                btnPagar.className = 'btn btn-secondary w-100';
                btnPagar.disabled = false;
            }

            // Resetear label de fecha
            const fechaLabel = document.querySelector('label[for="reservaFecha"]');
            if (fechaLabel) {
                fechaLabel.innerHTML = '<i class="bx bx-calendar me-1"></i>Fecha de reserva';
            }

            console.log('✅ Formulario limpiado completamente (incluyendo selectpickers)');
        };

        // Escuchar eventos del offcanvas
        offcanvasElement.addEventListener('hidden.bs.offcanvas', function() {
            console.log('🔧 Offcanvas cerrado, limpiando formulario y removiendo blur...');

            // Limpiar formulario
            limpiarFormulario();

            // Remover clases de blur y backdrop
            document.body.classList.remove('modal-open');

            // Remover todos los backdrops que puedan quedar
            const backdrops = document.querySelectorAll('.offcanvas-backdrop, .modal-backdrop');
            backdrops.forEach(backdrop => {
                backdrop.remove();
            });

            document.body.style.overflow = '';
            document.body.style.paddingRight = '';

            // Forzar repaint
            document.body.offsetHeight;

            console.log('✅ Offcanvas cerrado y limpiado correctamente');
        });

        // También limpiar cuando se abre para asegurar estado limpio
        offcanvasElement.addEventListener('show.bs.offcanvas', function() {
            console.log('📱 Offcanvas abriéndose, asegurando estado limpio...');
            limpiarFormulario();
        });

        console.log('🔧 OffcanvasManager inicializado');
    }

    // Método público para limpiar manualmente
    static limpiarFormulario() {
        const event = new Event('hidden.bs.offcanvas');
        const offcanvasElement = document.getElementById('crearReservaOffcanvas');
        if (offcanvasElement) {
            offcanvasElement.dispatchEvent(event);
        }
    }
}

function llenarOffcanvasReserva(fecha) {
    console.log('📅 llenarOffcanvasReserva llamada con fecha:', fecha);

    // Formatear fecha para mostrar (corrigiendo el problema de zona horaria)
    const fechaFormateada = DateFormatter.formatToLocal(fecha);
    const fechaDisplay = DateFormatter.formatDateForDisplay(fecha);

    // Llenar el campo de fecha (mantener formato servidor internamente)
    const campoFecha = document.getElementById('reservaFecha');
    if (campoFecha) {
        campoFecha.value = fecha; // Formato servidor para el backend
    }

    // Actualizar label con fecha formateada
    const fechaLabel = document.querySelector('label[for="reservaFecha"]');
    if (fechaLabel) {
        fechaLabel.innerHTML = `<i class="bx bx-calendar me-1"></i>Fecha: ${fechaFormateada}`;
    }

    // Llamar a la función de cálculo de costo si existe
    if (window.calcularCostoTotal) {
        window.calcularCostoTotal();
    }

    // Resetear el botón (esto se maneja automáticamente con la limpieza, pero por seguridad)
    const btnPagar = document.getElementById('btnPagarReservar');
    if (btnPagar) {
        btnPagar.textContent = 'Seleccione tipo de pago';
        btnPagar.className = 'btn btn-secondary w-100';
        btnPagar.disabled = false;
    }

    // Mostrar toast SOLO UNA VEZ con fecha corregida
    const toastMessage = `Fecha seleccionada: ${fechaDisplay}. Configure los detalles de su reserva.`;

    // Usar el nuevo sistema de toast con control de duplicados
    window.toastManager.showToast(toastMessage, 'info', 3000);
}

// Sobrescribir la función global para evitar duplicados
window.llenarOffcanvasReserva = llenarOffcanvasReserva;

// Función para manejar clicks en fechas pasadas con control de spam
function handlePastDateClick() {
    const message = 'No puedes reservar en fechas pasadas';

    // Usar el sistema de control de toasts
    window.toastManager.showToast(message, 'warning', 4000);
}

// Función para manejar selección de fechas válidas
function handleValidDateSelection(fecha) {
    // Abrir offcanvas
    const crearReservaOffcanvas = new bootstrap.Offcanvas(document.getElementById('crearReservaOffcanvas'));
    crearReservaOffcanvas.show();

    // Llenar datos
    llenarOffcanvasReserva(fecha);
}

// Función para parcheado de fechas en eventos existentes
function patchDateHandling() {
    // Sobrescribir métodos problemáticos del VecinoCalendarManager si existe
    if (window.vecinoCalendarManager && window.vecinoCalendarManager.handleDateClick) {
        const originalHandleDateClick = window.vecinoCalendarManager.handleDateClick.bind(window.vecinoCalendarManager);

        window.vecinoCalendarManager.handleDateClick = function(info) {
            console.log("📅 [PATCHED] Fecha clickeada:", info.dateStr);

            // Crear fecha local para evitar problemas de zona horaria
            const fechaParts = info.dateStr.split('-');
            const fechaSeleccionada = new Date(parseInt(fechaParts[0]), parseInt(fechaParts[1]) - 1, parseInt(fechaParts[2]));

            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            if (fechaSeleccionada < hoy) {
                handlePastDateClick();
                return;
            }

            handleValidDateSelection(info.dateStr);
        };

        console.log('✅ handleDateClick parcheado');
    }

    if (window.vecinoCalendarManager && window.vecinoCalendarManager.handleDateSelect) {
        const originalHandleDateSelect = window.vecinoCalendarManager.handleDateSelect.bind(window.vecinoCalendarManager);

        window.vecinoCalendarManager.handleDateSelect = function(info) {
            console.log("📅 [PATCHED] Rango seleccionado:", info);

            // Usar la fecha de inicio del rango
            const fechaSeleccionada = new Date(info.start);
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            if (fechaSeleccionada < hoy) {
                handlePastDateClick();
                // Deseleccionar
                if (this.calendar) {
                    this.calendar.unselect();
                }
                return;
            }

            const fechaStr = moment(fechaSeleccionada).format("YYYY-MM-DD");
            handleValidDateSelection(fechaStr);
        };

        console.log('✅ handleDateSelect parcheado');
    }

    if (window.vecinoCalendarManager && window.vecinoCalendarManager.showToast) {
        window.vecinoCalendarManager.showToast = function(message, type = 'info', duration = 3000) {
            return window.toastManager.showToast(message, type, duration);
        };

        console.log('✅ showToast parcheado');
    }

    // Patchear función de fallback si existe
    if (window.handleDateClickFallback) {
        const originalHandleDateClickFallback = window.handleDateClickFallback;

        window.handleDateClickFallback = function(info) {
            console.log("📅 [PATCHED FALLBACK] Fecha clickeada:", info.dateStr);

            const fechaParts = info.dateStr.split('-');
            const fechaSeleccionada = new Date(parseInt(fechaParts[0]), parseInt(fechaParts[1]) - 1, parseInt(fechaParts[2]));

            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            if (fechaSeleccionada < hoy) {
                handlePastDateClick();
                return;
            }

            handleValidDateSelection(info.dateStr);
        };

        console.log('✅ handleDateClickFallback parcheado');
    }
}

// Inicialización principal
document.addEventListener('DOMContentLoaded', function() {
    console.log('🔧 Aplicando correcciones del calendario...');

    // Inicializar gestión del offcanvas
    OffcanvasManager.init();

    // Aplicar parches después de un delay para asegurar que los objetos existan
    setTimeout(() => {
        patchDateHandling();
    }, 1000);

    console.log('🎉 Todas las correcciones aplicadas');
});

// Funciones para debug
window.clearToasts = function() {
    window.toastManager.clearActiveToasts();
    console.log('🧹 Toasts activos limpiados');
};

window.debugToasts = function() {
    console.log('📊 Estado del ToastManager:');
    console.log('  - Toasts activos:', window.toastManager.activeToasts);
    console.log('  - Últimos tiempos:', window.toastManager.lastToastTime);
};

window.limpiarOffcanvas = function() {
    OffcanvasManager.limpiarFormulario();
    console.log('🧹 Offcanvas limpiado manualmente');
};

console.log('✅ Script de correcciones actualizado cargado completamente');