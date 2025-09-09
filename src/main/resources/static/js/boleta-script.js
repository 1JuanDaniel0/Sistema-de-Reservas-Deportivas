// boleta-script.js
// Script para la funcionalidad de la boleta de pago

const dropzone = document.getElementById("dropzonePreview");
const fileInput = document.getElementById("comprobanteInput");
const previewImg = document.getElementById("previewImg");
const uploadPrompt = document.getElementById("uploadPrompt");

// Event listeners para drag and drop
dropzone.addEventListener("click", () => fileInput.click());

dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('border-blue-400', 'bg-blue-50');
});

dropzone.addEventListener('dragleave', () => {
    dropzone.classList.remove('border-blue-400', 'bg-blue-50');
});

dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('border-blue-400', 'bg-blue-50');
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        fileInput.files = files;
        fileInput.dispatchEvent(new Event('change'));
    }
});

// Manejo de cambio de archivo
fileInput.addEventListener("change", () => {
    const file = fileInput.files[0];
    if (!file) return;

    if (!file.type.startsWith("image/") && file.type !== "application/pdf") {
        Swal.fire({
            icon: "error",
            title: "Archivo no válido",
            text: "Solo se permiten imágenes (JPG, PNG) o archivos PDF",
            confirmButtonColor: "#3b82f6"
        });
        fileInput.value = '';
        return;
    }

    if (file.size > 5 * 1024 * 1024) {
        Swal.fire({
            icon: "error",
            title: "Archivo demasiado grande",
            text: "El tamaño máximo permitido es 5MB",
            confirmButtonColor: "#3b82f6"
        });
        fileInput.value = '';
        return;
    }

    if (file.type.startsWith("image/")) {
        const reader = new FileReader();
        reader.onload = () => {
            previewImg.src = reader.result;
            previewImg.classList.remove("hidden");
            uploadPrompt.classList.add("hidden");
        };
        reader.readAsDataURL(file);
    } else {
        // Para PDF, mostrar un ícono
        uploadPrompt.innerHTML = `
            <i class="bx bx-file-pdf text-red-500 text-6xl mb-4"></i>
            <p class="text-gray-600 font-medium">${file.name}</p>
            <p class="text-gray-500 text-sm">PDF seleccionado</p>
        `;
    }
});

// Función para mostrar modal de comprobante
function mostrarModalComprobante() {
    const boton = document.getElementById("botonSubirComprobante");
    const capturaKey = boton.dataset.capturaKey;

    if (capturaKey && capturaKey !== "null") {
        mostrarComprobante(); // ya fue subido
    } else {
        document.getElementById("modalComprobante").classList.remove("hidden");
    }
}

// Función para cerrar modal
function cerrarModalComprobante() {
    document.getElementById("modalComprobante").classList.add("hidden");
    // Resetear el formulario
    document.getElementById("formComprobante").reset();
    previewImg.classList.add("hidden");
    uploadPrompt.classList.remove("hidden");
    uploadPrompt.innerHTML = `
        <i class="bx bx-cloud-upload text-blue-400 text-4xl mb-4"></i>
        <p class="text-gray-600 mb-2">Arrastra tu imagen aquí</p>
        <p class="text-gray-500 text-sm">o haz clic para seleccionar archivo</p>
        <p class="text-gray-400 text-xs mt-2">Formatos: JPG, PNG, PDF (máx. 5MB)</p>
    `;
}

// Manejo del envío del formulario
document.getElementById("formComprobante").addEventListener("submit", function (e) {
    e.preventDefault();
    const file = fileInput.files[0];
    if (!file) {
        Swal.fire({
            icon: "warning",
            title: "Falta archivo",
            text: "Por favor seleccione su comprobante de pago",
            confirmButtonColor: "#3b82f6"
        });
        return;
    }

    const btn = this.querySelector("button[type='submit']");
    const originalContent = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `
        <div class="flex items-center justify-center space-x-3">
            <div class="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
            <span>Enviando comprobante...</span>
        </div>
    `;

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    const formData = new FormData(this);

    fetch("/vecino/subir-comprobante", {
        method: "POST",
        body: formData,
        headers: {
            [csrfHeader]: csrfToken
        },
    }).then(res => {
        if (!res.ok) throw new Error("Error al subir el comprobante");
        return res.json();
    }).then(data => {
        Swal.fire({
            icon: "success",
            title: "¡Comprobante enviado!",
            text: "Su comprobante ha sido enviado al coordinador y está en espera de confirmación. Recibirá una notificación cuando sea revisado.",
            confirmButtonColor: "#10b981",
            confirmButtonText: "Entendido"
        }).then(() => {
            cerrarModalComprobante();

            // Actualizar el botón principal
            const botonPrincipal = document.getElementById("botonSubirComprobante");
            botonPrincipal.innerHTML = `
                <i class="bx bx-check-circle text-xl"></i>
                <span>Ver Comprobante Enviado</span>
            `;
            botonPrincipal.className = "w-full bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 text-white font-bold py-4 px-6 rounded-xl transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl flex items-center justify-center space-x-3";
            botonPrincipal.dataset.capturaKey = "true";

            // Actualizar el botón flotante (si existe)
            const botonFlotante = document.querySelector('.floating-action');
            if (botonFlotante) {
                botonFlotante.style.display = 'none';
            }

            // Actualizar los pasos
            actualizarPasos();
        });
    }).catch((error) => {
        console.error('Error:', error);
        Swal.fire({
            icon: "error",
            title: "Error al enviar",
            text: "Ocurrió un problema al subir el comprobante. Por favor, inténtelo nuevamente.",
            confirmButtonColor: "#ef4444"
        });
    }).finally(() => {
        btn.disabled = false;
        btn.innerHTML = originalContent;
    });
});

// Función para actualizar los pasos visuales
function actualizarPasos() {
    // Actualizar el paso 3 (subir comprobante)
    const pasos = document.querySelectorAll('.payment-steps > div');
    if (pasos.length >= 3) {
        const paso3 = pasos[2];
        paso3.className = "step-active rounded-xl p-4 relative z-10";
        paso3.innerHTML = `
            <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-white rounded-full flex items-center justify-center">
                    <i class="bx bx-check text-green-600 font-bold"></i>
                </div>
                <div>
                    <p class="font-semibold">Comprobante enviado</p>
                    <p class="text-sm opacity-90">En espera de revisión</p>
                </div>
            </div>
        `;

        // Activar el paso 4 (revisión)
        if (pasos.length >= 4) {
            const paso4 = pasos[3];
            paso4.className = "step-pending rounded-xl p-4 relative z-10 border-2 border-dashed border-yellow-300";
            paso4.querySelector('.w-8').className = "w-8 h-8 bg-yellow-100 rounded-full flex items-center justify-center";
            paso4.querySelector('.w-8 span').textContent = "⏳";
            paso4.querySelector('.font-semibold').className = "font-semibold text-yellow-700";
            paso4.querySelector('.text-sm').className = "text-sm text-yellow-600";
        }
    }
}

// Función para mostrar comprobante ya enviado
function mostrarComprobante() {
    const boton = document.getElementById("botonSubirComprobante");
    const idReserva = boton.dataset.reservaId;

    if (!idReserva) {
        Swal.fire({
            icon: "error",
            title: "Error",
            text: "No se pudo obtener la información de la reserva",
            confirmButtonColor: "#ef4444"
        });
        return;
    }

    Swal.fire({
        title: "Cargando comprobante...",
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    fetch("/vecino/url-comprobante/" + idReserva)
        .then(res => {
            if (!res.ok) throw new Error("No se pudo obtener el comprobante");
            return res.json();
        })
        .then(data => {
            Swal.fire({
                title: "Comprobante enviado",
                html: `
                    <div class="text-center">
                        <p class="text-gray-600 mb-4">Su comprobante está siendo revisado por el coordinador</p>
                        <img src="${data.url}" alt="Comprobante de pago" class="mx-auto max-w-full max-h-96 rounded-lg shadow-lg" />
                    </div>
                `,
                width: '600px',
                showCloseButton: true,
                confirmButtonText: "Cerrar",
                confirmButtonColor: "#3b82f6"
            });
        })
        .catch(err => {
            console.error('Error:', err);
            Swal.fire({
                icon: "error",
                title: "Error",
                text: "No se pudo cargar el comprobante. Inténtelo más tarde.",
                confirmButtonColor: "#ef4444"
            });
        });
}

// Función para copiar texto al portapapeles
function copiarTexto(texto, elemento) {
    navigator.clipboard.writeText(texto).then(() => {
        const originalText = elemento.textContent;
        elemento.textContent = '¡Copiado!';
        elemento.classList.add('text-green-400');
        setTimeout(() => {
            elemento.textContent = originalText;
            elemento.classList.remove('text-green-400');
        }, 2000);
    }).catch(() => {
        // Fallback para navegadores que no soportan clipboard API
        const textArea = document.createElement('textarea');
        textArea.value = texto;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);

        const originalText = elemento.textContent;
        elemento.textContent = '¡Copiado!';
        elemento.classList.add('text-green-400');
        setTimeout(() => {
            elemento.textContent = originalText;
            elemento.classList.remove('text-green-400');
        }, 2000);
    });
}

// Event listeners para cerrar modal
document.getElementById("modalComprobante").addEventListener("click", function(e) {
    if (e.target === this) {
        cerrarModalComprobante();
    }
});

// Cerrar modal con tecla Escape
document.addEventListener("keydown", function(e) {
    if (e.key === "Escape") {
        const modal = document.getElementById("modalComprobante");
        if (!modal.classList.contains("hidden")) {
            cerrarModalComprobante();
        }
    }
});

// Animaciones y efectos al cargar la página
document.addEventListener("DOMContentLoaded", function() {
    // Animar elementos al cargar
    const elementos = document.querySelectorAll('.bg-white, .bank-card');
    elementos.forEach((el, index) => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        setTimeout(() => {
            el.style.transition = 'all 0.6s ease';
            el.style.opacity = '1';
            el.style.transform = 'translateY(0)';
        }, index * 100);
    });

    // Configurar tooltips dinámicos
    document.querySelectorAll('[data-tooltip]').forEach(element => {
        element.addEventListener('mouseenter', function() {
            const tooltip = document.createElement('div');
            tooltip.className = 'absolute bg-gray-800 text-white text-xs rounded py-1 px-2 -top-8 left-1/2 transform -translate-x-1/2 whitespace-nowrap z-50';
            tooltip.textContent = this.dataset.tooltip;
            this.style.position = 'relative';
            this.appendChild(tooltip);
        });

        element.addEventListener('mouseleave', function() {
            const tooltip = this.querySelector('.absolute.bg-gray-800');
            if (tooltip) {
                tooltip.remove();
            }
        });
    });

    // Verificar si ya se subió comprobante y actualizar pasos
    const boton = document.getElementById("botonSubirComprobante");
    if (boton && boton.dataset.capturaKey && boton.dataset.capturaKey !== "null") {
        actualizarPasos();
        // Ocultar botón flotante si ya se subió
        const botonFlotante = document.querySelector('.floating-action');
        if (botonFlotante) {
            botonFlotante.style.display = 'none';
        }
    }
});

// Función para imprimir solo la información relevante
window.addEventListener('beforeprint', function() {
    // Ocultar elementos no necesarios para impresión
    document.body.classList.add('printing');
});

window.addEventListener('afterprint', function() {
    // Restaurar elementos después de imprimir
    document.body.classList.remove('printing');
});

// Validación adicional para formularios
function validarComprobante(file) {
    const formatosPermitidos = ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf'];
    const tamañoMaximo = 5 * 1024 * 1024; // 5MB

    if (!formatosPermitidos.includes(file.type)) {
        return {
            valido: false,
            mensaje: "Formato de archivo no permitido. Use JPG, PNG o PDF."
        };
    }

    if (file.size > tamañoMaximo) {
        return {
            valido: false,
            mensaje: "El archivo es demasiado grande. Máximo 5MB."
        };
    }

    return { valido: true };
}

// Función para mostrar notificaciones toast simples
function mostrarNotificacion(mensaje, tipo = 'info') {
    const colores = {
        'success': 'bg-green-500',
        'error': 'bg-red-500',
        'warning': 'bg-yellow-500',
        'info': 'bg-blue-500'
    };

    const notificacion = document.createElement('div');
    notificacion.className = `fixed top-4 right-4 ${colores[tipo]} text-white px-6 py-3 rounded-lg shadow-lg z-50 transform translate-x-full transition-transform duration-300`;
    notificacion.textContent = mensaje;

    document.body.appendChild(notificacion);

    // Animar entrada
    setTimeout(() => {
        notificacion.classList.remove('translate-x-full');
    }, 100);

    // Animar salida
    setTimeout(() => {
        notificacion.classList.add('translate-x-full');
        setTimeout(() => {
            document.body.removeChild(notificacion);
        }, 300);
    }, 3000);
}

// Hacer funciones globales disponibles
window.mostrarModalComprobante = mostrarModalComprobante;
window.cerrarModalComprobante = cerrarModalComprobante;
window.mostrarComprobante = mostrarComprobante;
window.copiarTexto = copiarTexto;