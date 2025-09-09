"use strict";
// Variables globales
let mercadoPagoPublicKey = null;
let mercadoPagoLoaded = false;
// Función para cargar la configuración de MercadoPago
async function cargarConfiguracionMercadoPago() {
    try {
        const response = await fetch('/pago/mercadopago/config');
        const config = await response.json();
        mercadoPagoPublicKey = config.publicKey;

        // Cargar el SDK de MercadoPago
        if (!mercadoPagoLoaded) {
            const script = document.createElement('script');
            script.src = 'https://sdk.mercadopago.com/js/v2';
            script.onload = () => {
                mercadoPagoLoaded = true;
                console.log('MercadoPago SDK cargado correctamente');
            };
            document.head.appendChild(script);
        }
    } catch (error) {
        console.error('Error cargando configuración de MercadoPago:', error);
    }
}

// Toast avanzado con progress bar y efectos especiales
function mostrarToastAvanzado(mensaje, tipo = 'info', opciones = {}) {
    const defaultOpciones = {
        duracion: tipo === 'error' ? 6000 : 4000,
        mostrarProgressBar: true,
        posicion: 'top-end', // top-start, top-center, top-end, bottom-start, etc.
        sound: false,
        autoDismiss: true
    };

    const config = { ...defaultOpciones, ...opciones };

    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';

        // Configurar posición del container
        const positionClasses = {
            'top-start': 'top-0 start-0',
            'top-center': 'top-0 start-50 translate-middle-x',
            'top-end': 'top-0 end-0',
            'bottom-start': 'bottom-0 start-0',
            'bottom-center': 'bottom-0 start-50 translate-middle-x',
            'bottom-end': 'bottom-0 end-0'
        };

        toastContainer.className = `toast-container position-fixed ${positionClasses[config.posicion]} p-3`;
        toastContainer.style.zIndex = '9999';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();

    const tipoConfig = {
        success: {
            bgGradient: 'linear-gradient(135deg, #00d4aa 0%, #01a085 100%)',
            shadowColor: 'rgba(0, 212, 170, 0.4)',
            icon: 'bx-check-circle',
            titulo: '¡Perfecto!',
            emoji: '🎉'
        },
        error: {
            bgGradient: 'linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%)',
            shadowColor: 'rgba(255, 107, 107, 0.4)',
            icon: 'bx-error-circle',
            titulo: '¡Ups!',
            emoji: '😞'
        },
        warning: {
            bgGradient: 'linear-gradient(135deg, #feca57 0%, #ff9ff3 100%)',
            shadowColor: 'rgba(254, 202, 87, 0.4)',
            icon: 'bx-error',
            titulo: '¡Cuidado!',
            emoji: '⚡'
        },
        info: {
            bgGradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            shadowColor: 'rgba(102, 126, 234, 0.4)',
            icon: 'bx-info-circle',
            titulo: '¡Info!',
            emoji: '💡'
        }
    };

    const typeConfig = tipoConfig[tipo] || tipoConfig.info;

    const progressBarHtml = config.mostrarProgressBar ? `
            <div class="toast-progress-bar" style="
                position: absolute;
                bottom: 0;
                left: 0;
                height: 3px;
                background: rgba(255, 255, 255, 0.8);
                width: 100%;
                animation: progressBar ${config.duracion}ms linear forwards;
            "></div>
        ` : '';

    const toastHtml = `
            <div id="${toastId}" class="toast border-0"
                 role="alert" aria-live="assertive" aria-atomic="true"
                 style="
                    min-width: 350px;
                    background: ${typeConfig.bgGradient};
                    box-shadow: 0 10px 40px ${typeConfig.shadowColor};
                    border-radius: 15px;
                    position: relative;
                    overflow: hidden;
                    animation: toastSlideIn 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
                    backdrop-filter: blur(10px);
                 ">
                <div class="toast-body text-white p-4 position-relative">
                    <div class="d-flex align-items-start">
                        <div class="me-3">
                            <div style="
                                width: 40px;
                                height: 40px;
                                background: rgba(255, 255, 255, 0.2);
                                border-radius: 50%;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                backdrop-filter: blur(10px);
                                color: white;
                            ">
                                <i class="bx ${typeConfig.icon}" style="font-size: 1.5rem;"></i>
                            </div>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex align-items-center mb-2">
                                <span class="me-2" style="font-size: 1.2rem;">${typeConfig.emoji}</span>
                                <h6 class="mb-0 fw-bold text-white">${typeConfig.titulo}</h6>
                            </div>
                            <p class="mb-0 opacity-90" style="font-size: 0.9rem; line-height: 1.4;">
                                ${mensaje}
                            </p>
                        </div>
                        <button type="button" class="btn-close btn-close-white ms-3"
                                data-bs-dismiss="toast" aria-label="Close"
                                style="
                                    background-size: 12px;
                                    filter: drop-shadow(0 0 3px rgba(0, 0, 0, 0.3));
                                "></button>
                    </div>

                    <!-- Efecto de brillo -->
                    <div style="
                        position: absolute;
                        top: 0;
                        left: -100%;
                        width: 100%;
                        height: 100%;
                        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
                        animation: shine 3s infinite;
                    "></div>

                    ${progressBarHtml}
                </div>
            </div>
        `;

    // Agregar estilos de animación si no existen
    if (!document.getElementById('toast-animations')) {
        const style = document.createElement('style');
        style.id = 'toast-animations';
        style.textContent = `
                @keyframes toastSlideIn {
                    from {
                        transform: translateX(100%) scale(0.8);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0) scale(1);
                        opacity: 1;
                    }
                }

                @keyframes toastSlideOut {
                    from {
                        transform: translateX(0) scale(1);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(100%) scale(0.8);
                        opacity: 0;
                    }
                }

                @keyframes progressBar {
                    from { width: 100%; }
                    to { width: 0%; }
                }

                @keyframes shine {
                    0% { left: -100%; }
                    100% { left: 100%; }
                }

                .toast:hover .toast-progress-bar {
                    animation-play-state: paused;
                }

                /* Responsive para dispositivos móviles */
                @media (max-width: 576px) {
                    #toast-container {
                        left: 1rem;
                        right: 1rem;
                        top: 1rem !important;
                        max-width: none;
                    }

                    #toast-container .toast {
                        min-width: auto !important;
                        width: 100%;
                    }
                }
            `;
        document.head.appendChild(style);
    }

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        autohide: config.autoDismiss,
        delay: config.duracion
    });

    // Efecto de sonido (opcional)
    if (config.sound) {
        playToastSound(tipo);
    }

    toast.show();

    // Pausar progress bar al hacer hover
    toastElement.addEventListener('mouseenter', () => {
        const progressBar = toastElement.querySelector('.toast-progress-bar');
        if (progressBar) {
            progressBar.style.animationPlayState = 'paused';
        }
    });

    toastElement.addEventListener('mouseleave', () => {
        const progressBar = toastElement.querySelector('.toast-progress-bar');
        if (progressBar) {
            progressBar.style.animationPlayState = 'running';
        }
    });

    toastElement.addEventListener('hidden.bs.toast', function() {
        this.style.animation = 'toastSlideOut 0.3s ease-in forwards';
        setTimeout(() => this.remove(), 300);
    });
}

// Función opcional para efectos de sonido
function playToastSound(tipo) {
    try {
        // Solo si el usuario ha interactuado con la página (requerimiento del navegador)
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();

        const frequencies = {
            success: [523.25, 659.25, 783.99], // C, E, G
            error: [349.23, 293.66], // F, D
            warning: [440, 554.37], // A, C#
            info: [261.63, 329.63] // C, E
        };

        const freqs = frequencies[tipo] || frequencies.info;

        freqs.forEach((freq, index) => {
            setTimeout(() => {
                const oscillator = audioContext.createOscillator();
                const gainNode = audioContext.createGain();

                oscillator.connect(gainNode);
                gainNode.connect(audioContext.destination);

                oscillator.frequency.setValueAtTime(freq, audioContext.currentTime);
                oscillator.type = 'sine';

                gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
                gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.2);

                oscillator.start(audioContext.currentTime);
                oscillator.stop(audioContext.currentTime + 0.2);
            }, index * 100);
        });
    } catch (error) {
        console.log('Audio no disponible:', error);
    }
}

// Función para validar el formulario del offcanvas
function validarFormularioReserva() {
    const fecha = document.getElementById('reservaFecha').value;
    const horaInicio = document.getElementById('reservaHoraInicio').value;
    const horaFin = document.getElementById('reservaHoraFin').value;
    const tipoPago = document.getElementById('reservaTipoPago').value;
    const coordinador = document.getElementById('reservaCoordinador').value;

    if (!fecha || !horaInicio || !horaFin || !tipoPago || !coordinador) {
        mostrarToastAvanzado('Por favor, complete todos los campos requeridos.', 'warning', {
            duracion: 4000
        });
        return false;
    }

    if (parseInt(horaInicio) >= parseInt(horaFin)) {
        mostrarToastAvanzado('La hora de inicio debe ser anterior a la hora de fin.', 'error', {
            duracion: 5000
        });
        return false;
    }

    return true;
}

// Función para procesar el pago en línea
async function procesarPagoEnLinea() {
    if (!mercadoPagoLoaded) {
        mostrarToastAvanzado('Error: MercadoPago no se ha cargado correctamente.', 'error', {
            duracion: 6000
        });
        return;
    }

    try {
        console.log('=== INICIANDO PAGO EN LÍNEA ===');

        const espacioId = document.getElementById('espacioId').value;
        const fecha = document.getElementById('reservaFecha').value;
        const horaInicio = document.getElementById('reservaHoraInicio').value;
        const horaFin = document.getElementById('reservaHoraFin').value;

        const detallePago = {
            idEspacio: parseInt(espacioId),
            fecha: fecha,
            horaInicio: parseInt(horaInicio),
            horaFin: parseInt(horaFin)
        };

        console.log('Datos a enviar:', detallePago);

        const btnPagar = document.getElementById('btnPagarReservar');
        btnPagar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Procesando pago...';
        btnPagar.disabled = true;

        // Mostrar toast de progreso
        mostrarToastAvanzado('Procesando pago con MercadoPago...', 'info', {
            duracion: 3000,
            autoDismiss: true
        });

        const response = await fetch('/pago/mercadopago/preferencia', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(detallePago)
        });

        const resultado = await response.json();
        console.log('Respuesta del servidor:', resultado);

        if (resultado.url) {
            console.log('Redirigiendo a MercadoPago:', resultado.url);
            mostrarToastAvanzado('Redirigiendo a MercadoPago...', 'success', {
                duracion: 2000
            });
            setTimeout(() => {
                window.location.href = resultado.url;
            }, 1500);
        } else {
            // Verificar si es error de mantenimiento
            let errorMessage = resultado.error || 'Error desconocido';
            let tipoError = 'error';

            if (errorMessage.includes('mantenimiento') || errorMessage.includes('no disponible')) {
                tipoError = 'warning';
                errorMessage = `🔧 ${errorMessage}`;
            }

            mostrarToastAvanzado(errorMessage, tipoError, {
                duracion: 8000
            });
            actualizarTextoBoton();
            btnPagar.disabled = false;
        }

    } catch (error) {
        console.error('Error en pago en línea:', error);
        mostrarToastAvanzado('Error inesperado al procesar el pago.', 'error', {
            duracion: 6000
        });
        actualizarTextoBoton();
        document.getElementById('btnPagarReservar').disabled = false;
    }
}

// Función para procesar reserva en banco con redirección a boleta
async function procesarReservaEnBanco() {
    try {
        console.log('=== PROCESANDO RESERVA EN BANCO ===');

        const horaInicioValor = document.getElementById('reservaHoraInicio').value;
        const horaFinValor = document.getElementById('reservaHoraFin').value;

        console.log('Valores originales:', { horaInicioValor, horaFinValor });

        const formData = new FormData();
        formData.append('idEspacio', document.getElementById('espacioId').value);
        formData.append('fecha', document.getElementById('reservaFecha').value);
        formData.append('horaInicio', horaInicioValor);
        formData.append('horaFin', horaFinValor);
        formData.append('tipoPago', 'En banco');
        formData.append('idCoordinador', document.getElementById('reservaCoordinador').value);

        console.log('Datos a enviar:', Object.fromEntries(formData));

        const btnPagar = document.getElementById('btnPagarReservar');
        btnPagar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creando reserva...';
        btnPagar.disabled = true;

        // Mostrar toast de progreso
        mostrarToastAvanzado('Creando su reserva...', 'info', {
            duracion: 3000,
            autoDismiss: true
        });

        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        const response = await fetch('/vecino/crearReserva', {
            method: 'POST',
            body: formData,
            headers: {
                [csrfHeader]: csrfToken,
                'Accept': 'application/json' // Indica que esperamos JSON
            }
        });

        console.log('Response status:', response.status);

        if (response.ok) {
            const result = await response.json();
            console.log('Respuesta del servidor:', result);

            if (result.success) {
                mostrarToastAvanzado('¡Reserva creada exitosamente! Será redirigido a su boleta.', 'success', {
                    duracion: 3000,
                    sound: false
                });

                // Cerrar el modal/offcanvas
                const offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById('crearReservaOffcanvas'));
                if (offcanvas) {
                    offcanvas.hide();
                }

                // Redirigir a la boleta después de un breve delay
                setTimeout(() => {
                    if (result.redirectUrl) {
                        window.location.href = result.redirectUrl;
                    } else if (result.idReserva) {
                        window.location.href = `/vecino/boleta/${result.idReserva}`;
                    } else {
                        // Fallback
                        window.location.href = '/vecino/espacios-disponibles';
                    }
                }, 2000);

            } else {
                // Mejorar el manejo de errores específicos
                let errorMessage = result.message || 'Error al crear la reserva';
                let tipoError = 'error';

                // Detectar diferentes tipos de error
                if (errorMessage.includes('mantenimiento') || errorMessage.includes('Mantenimiento')) {
                    tipoError = 'warning';
                    errorMessage = `🔧 No se puede reservar: ${errorMessage}`;
                } else if (errorMessage.includes('conflicto') || errorMessage.includes('Conflicto')) {
                    tipoError = 'warning';
                    errorMessage = `⚠️ ${errorMessage}`;
                }

                mostrarToastAvanzado(errorMessage, tipoError, {
                    duracion: 10000, // Más tiempo para leer
                    autoDismiss: false // No cerrar automáticamente
                });

                actualizarTextoBoton();
                btnPagar.disabled = false;
            }
        } else {
            // Mejorar manejo de errores HTTP
            let errorMessage = '😞 ¡Ups! Error al crear la reserva.';

            try {
                const errorResult = await response.json();
                if (errorResult.message) {
                    if (errorResult.message.includes('mantenimiento')) {
                        errorMessage = `🔧 ${errorResult.message}`;
                    } else {
                        errorMessage = errorResult.message;
                    }
                }
            } catch (e) {
                console.log('No se pudo parsear respuesta de error como JSON');
                // Para errores 500, verificar si es por mantenimiento
                if (response.status === 500) {
                    errorMessage = '🔧 No se puede crear la reserva. Puede haber un conflicto con mantenimientos programados.';
                }
            }

            mostrarToastAvanzado(errorMessage, 'error', {
                duracion: 8000
            });
            actualizarTextoBoton();
            btnPagar.disabled = false;
        }

    } catch (error) {
        console.error('Error en reserva en banco:', error);
        mostrarToastAvanzado('Error inesperado al crear la reserva.', 'error', {
            duracion: 6000
        });
        actualizarTextoBoton();
        document.getElementById('btnPagarReservar').disabled = false;
    }
}

// Función para calcular y mostrar el costo total
function calcularCostoTotal() {
    const horaInicio = parseInt(document.getElementById('reservaHoraInicio').value);
    const horaFin = parseInt(document.getElementById('reservaHoraFin').value);
    const costoPorHora = parseFloat(document.getElementById('espacioCosto').value);

    if (horaInicio && horaFin && horaInicio < horaFin) {
        const horas = horaFin - horaInicio;
        const total = costoPorHora * horas;

        document.getElementById('reservaCosto').value = total.toFixed(2);
        document.getElementById('detalleHoras').textContent = `${horas} hora${horas > 1 ? 's' : ''} × S/ ${costoPorHora.toFixed(2)}`;
    } else {
        document.getElementById('reservaCosto').value = '0.00';
        document.getElementById('detalleHoras').textContent = '';
    }
}

// Función para actualizar el texto del botón según el tipo de pago
function actualizarTextoBoton() {
    const tipoPago = document.getElementById('reservaTipoPago').value;
    const btnPagar = document.getElementById('btnPagarReservar');

    if (tipoPago === 'En línea') {
        btnPagar.textContent = 'Pagar en línea';
        btnPagar.className = 'btn btn-success w-100';
    } else if (tipoPago === 'En banco') {
        btnPagar.textContent = 'Crear reserva';
        btnPagar.className = 'btn btn-primary w-100';
    } else {
        btnPagar.textContent = 'Seleccione tipo de pago';
        btnPagar.className = 'btn btn-secondary w-100';
    }
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    cargarConfiguracionMercadoPago();

    // Event listener para cambios en las horas
    const horaInicioSelect = document.getElementById('reservaHoraInicio');
    const horaFinSelect = document.getElementById('reservaHoraFin');

    if (horaInicioSelect && horaFinSelect) {
        horaInicioSelect.addEventListener('change', calcularCostoTotal);
        horaFinSelect.addEventListener('change', calcularCostoTotal);
    }

    // Event listener para cambios en el tipo de pago
    const tipoPagoSelect = document.getElementById('reservaTipoPago');
    if (tipoPagoSelect) {
        tipoPagoSelect.addEventListener('change', actualizarTextoBoton);
    }

    // Event listener para el formulario
    const formCrearReserva = document.getElementById('formCrearReserva');
    if (formCrearReserva) {
        formCrearReserva.addEventListener('submit', function(e) {
            e.preventDefault();

            if (!validarFormularioReserva()) {
                return;
            }

            const tipoPago = document.getElementById('reservaTipoPago').value;

            if (tipoPago === 'En línea') {
                procesarPagoEnLinea();
            } else if (tipoPago === 'En banco') {
                procesarReservaEnBanco();
            }
        });
    }

    // Toast de bienvenida (opcional)
    setTimeout(() => {
        mostrarToastAvanzado('¡Bienvenido! Seleccione una fecha para crear una reserva.', 'info', {
            duracion: 3000,
            mostrarProgressBar: true
        });
    }, 1000);
});

// Función para llenar el offcanvas cuando se selecciona un día
function llenarOffcanvasReserva(fecha) {
    document.getElementById('reservaFecha').value = fecha;
    calcularCostoTotal();

    // Resetear el botón
    const btnPagar = document.getElementById('btnPagarReservar');
    btnPagar.textContent = 'Seleccione tipo de pago';
    btnPagar.className = 'btn btn-secondary w-100';
    btnPagar.disabled = false;

    // Toast informativo
    mostrarToastAvanzado(`Fecha seleccionada: ${fecha}. Configure los detalles de su reserva.`, 'info', {
        duracion: 3000
    });
}

// Exponer la función globalmente
window.llenarOffcanvasReserva = llenarOffcanvasReserva;