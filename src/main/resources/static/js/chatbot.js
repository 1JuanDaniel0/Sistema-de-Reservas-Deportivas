document.addEventListener('DOMContentLoaded', function () {
    // Verificar que los elementos del chatbot existan antes de continuar
    const toggleBtn = document.getElementById('chatbot-toggle');
    const chatbotWindow = document.getElementById('chatbot-window');

    // Si no existe el chatbot, no ejecutar nada de este script
    if (!toggleBtn || !chatbotWindow) {
        console.log('Chatbot no disponible para este rol de usuario');
        return;
    }

    // Variables para mejorar la experiencia
    let isTyping = false;
    let messageQueue = [];
    let chatHistory = [];
    let tiempoInactividad;

    // Para mostrar bien la hora de los mensajes en el chat del frontend
    function formatearHora(fechaStr) {
        const fecha = new Date(fechaStr);
        return fecha.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    // Función mejorada para cerrar chatbot con animación
    function cerrarChatbot() {
        chatbotWindow.style.transform = 'translateY(40px) scale(0.85)';
        chatbotWindow.style.opacity = '0';

        setTimeout(() => {
            chatbotWindow.classList.remove('show');
            chatbotWindow.style.transform = '';
            chatbotWindow.style.opacity = '';
        }, 300);
    }

    // Función mejorada para abrir chatbot
    function abrirChatbot() {
        chatbotWindow.classList.add('show');

        // Animación de entrada
        setTimeout(() => {
            chatbotWindow.style.animation = 'slide-in-bounce 0.6s ease-out forwards';
        }, 50);

        // Focus automático en el input
        setTimeout(() => {
            const input = document.getElementById('chatbot-message');
            if (input) input.focus();
        }, 400);
    }

    // Cerrar chatbot al hacer clic fuera de él (mejorado)
    document.addEventListener('click', function (event) {
        if (!chatbotWindow || !toggleBtn) return;

        const isClickInsideChat = chatbotWindow.contains(event.target);
        const isClickToggleButton = toggleBtn.contains(event.target);

        if (!isClickInsideChat && !isClickToggleButton && chatbotWindow.classList.contains('show')) {
            cerrarChatbot();
        }
    });

    // Mostrar respuesta del bot letra por letra con velocidad variable
    function escribirMensajeLento(texto, elemento, callback) {
        let i = 0;
        const velocidadBase = 20; // ms por carácter
        const variacionVelocidad = 15; // variación aleatoria

        function escribir() {
            if (i < texto.length) {
                elemento.innerHTML += texto.charAt(i);
                i++;

                // Velocidad variable para hacer más natural
                const velocidad = velocidadBase + Math.random() * variacionVelocidad;

                // Pausa más larga en puntos y comas
                const char = texto.charAt(i - 1);
                const pausaExtra = (char === '.' || char === ',' || char === '!' || char === '?') ? 200 : 0;

                setTimeout(escribir, velocidad + pausaExtra);
            } else {
                if (callback) callback();
                // Scroll automático al final
                scrollToBottom();
            }
        }
        escribir();
    }

    // Función para scroll suave al final
    function scrollToBottom() {
        const messageList = chatbotWindow.querySelector('.chat-history');
        if (messageList) {
            messageList.scrollTo({
                top: messageList.scrollHeight,
                behavior: 'smooth'
            });
        }
    }

    // Función para mostrar indicador de escritura mejorado
    function mostrarIndicadorEscritura() {
        const typing = document.getElementById('typing-indicator');
        if (typing) {
            isTyping = true;
            typing.classList.remove('d-none');
            typing.style.animation = 'slideUp 0.3s ease-out';

            // Asegurar que el scroll esté al final para ver el indicador
            setTimeout(() => {
                scrollToBottom();
            }, 100);
        }
    }

    // Función para ocultar indicador de escritura
    function ocultarIndicadorEscritura() {
        const typing = document.getElementById('typing-indicator');
        if (typing) {
            isTyping = false;
            typing.style.animation = 'slideDown 0.3s ease-out';
            setTimeout(() => {
                typing.classList.add('d-none');
            }, 300);
        }
    }

    // Obtener elementos del DOM
    const messageList = chatbotWindow.querySelector('.chat-history');
    const form = document.getElementById('chatbot-form');
    const input = document.getElementById('chatbot-message');
    const typing = document.getElementById('typing-indicator');
    const minimizeBtn = document.getElementById('chatbot-minimize');

    // Event listener para el botón minimizar corregido
    if (minimizeBtn) {
        minimizeBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            cerrarChatbot();
        });
    }

    // Verificar que todos los elementos necesarios existan
    if (!messageList || !form || !input || !typing) {
        console.warn('Algunos elementos del chatbot no se encontraron');
        return;
    }

    // Mostrar historial de mensajes con animación mejorada
    toggleBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();

        const wasOpen = chatbotWindow.classList.contains('show');

        if (wasOpen) {
            cerrarChatbot();
        } else {
            abrirChatbot();

            // Cargar historial si es la primera vez
            if (messageList.children.length === 0) {
                mostrarIndicadorEscritura();

                try {
                    const resp = await fetch('/vecino/api/chatbot/historial');
                    const data = await resp.json();
                    const nombre = data.nombre || 'usuario';
                    const mensajes = data.mensajes || [];

                    ocultarIndicadorEscritura();

                    if (mensajes.length === 0) {
                        const mensajeBienvenida = `¡Hola, ${nombre}! 👋 Soy tu asistente virtual. ¿En qué puedo ayudarte hoy?`;
                        agregarMensaje('bot', mensajeBienvenida, true);

                        // Mostrar sugerencias de bienvenida después de un momento
                        setTimeout(() => {
                            mostrarSugerenciasBienvenida();
                        }, 1500);
                    } else {
                        // Cargar mensajes con efecto escalonado
                        mensajes.forEach((msg, index) => {
                            setTimeout(() => {
                                agregarMensaje(
                                    msg.rol === "USUARIO" ? "user" : "bot",
                                    msg.contenido,
                                    false,
                                    msg.fecha
                                );
                            }, index * 100);
                        });

                        chatHistory = mensajes;
                    }
                } catch (e) {
                    ocultarIndicadorEscritura();
                    agregarMensaje("bot", "❌ Error al cargar historial. Pero estoy aquí para ayudarte.");
                }
            }
        }
    });

    // Función para mostrar sugerencias de bienvenida
    function mostrarSugerenciasBienvenida() {
        const sugerencias = {
            "Ver lugares disponibles": "lugares",
            "Hacer una reserva": "reserva",
            "Consultar disponibilidad": "disponibilidad",
            "Ver mis reservas": "mis_reservas"
        };

        const ultimaBurbuja = messageList.lastElementChild;
        if (ultimaBurbuja) {
            const chatMessageText = ultimaBurbuja.querySelector('.chat-message-text');
            if (chatMessageText) {
                const sugerenciasDiv = crearSugerencias(sugerencias);
                chatMessageText.appendChild(sugerenciasDiv);
            }
        }
    }

    // Enviar mensaje al chatbot con validaciones mejoradas
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const texto = input.value.trim();
        if (!texto || isTyping) return;

        // Validar longitud del mensaje
        if (texto.length > 500) {
            mostrarNotificacionError('El mensaje es demasiado largo (máximo 500 caracteres)');
            return;
        }

        // Agregar mensaje del usuario con animación
        const burbujaUser = agregarMensaje("user", texto);
        chatHistory.push({ rol: "USUARIO", contenido: texto, fecha: new Date() });

        // Limpiar input y mostrar estado de carga
        input.value = "";
        actualizarContadorCaracteres();
        form.classList.add('loading');

        try {
            mostrarIndicadorEscritura();

            const resp = await fetch('/vecino/api/chatbot', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mensaje: texto })
            });

            if (!resp.ok) {
                throw new Error(`HTTP ${resp.status}: ${resp.statusText}`);
            }

            const data = await resp.json();

            // Simular tiempo de procesamiento mínimo para mejor UX
            setTimeout(() => {
                ocultarIndicadorEscritura();
                form.classList.remove('loading');

                // Mostrar mensaje del bot con efecto de escritura
                const respuesta = data.respuesta || data.error || 'Respuesta no válida';
                const burbujaBot = agregarMensaje('bot', respuesta, true);

                chatHistory.push({ rol: "BOT", contenido: respuesta, fecha: new Date() });

                // Manejar sugerencias con animación
                if (data.sugerencias && Object.keys(data.sugerencias).length > 0) {
                    setTimeout(() => {
                        const sugerenciasDiv = crearSugerencias(data.sugerencias);
                        const chatMessageText = burbujaBot.querySelector('.chat-message-text');
                        if (chatMessageText) {
                            chatMessageText.appendChild(sugerenciasDiv);
                        }
                    }, respuesta.length * 15 + 500); // Esperar a que termine de escribir
                }

                // Manejar acciones especiales
                if (data.accion === 'crear_reserva') {
                    console.log('🎯 Iniciando flujo de reserva:', data.parametros);
                    mostrarNotificacionInfo('Te estoy guiando para crear tu reserva');
                }

            }, Math.max(800, texto.length * 50)); // Tiempo mínimo realista

        } catch (e) {
            ocultarIndicadorEscritura();
            form.classList.remove('loading');
            console.error("Error en fetch:", e);

            let mensajeError = "❌ Ups, algo salió mal. ";
            if (e.message.includes('Failed to fetch')) {
                mensajeError += "Verifica tu conexión a internet.";
            } else if (e.message.includes('500')) {
                mensajeError += "El servidor está temporalmente no disponible.";
            } else {
                mensajeError += "Inténtalo de nuevo en un momento.";
            }

            agregarMensaje("bot", mensajeError);
        }
    });

    // Función mejorada para crear sugerencias
    function crearSugerencias(sugerencias) {
        const sugerenciasDiv = document.createElement("div");
        sugerenciasDiv.classList.add("sugerencias-botones");
        sugerenciasDiv.style.opacity = '0';
        sugerenciasDiv.style.transform = 'translateY(10px)';

        Object.entries(sugerencias).forEach(([key, value], index) => {
            const btn = document.createElement("button");
            btn.textContent = key;
            btn.className = "btn-sugerencia";
            btn.style.animationDelay = `${index * 100}ms`;

            // Agregar icono según el tipo de sugerencia
            const icono = obtenerIconoSugerencia(key);
            if (icono) {
                btn.innerHTML = `<i class="${icono} me-2"></i>${key}`;
            }

            btn.onclick = () => {
                // Enviar la clave (nombre visible) no el valor (ID)
                input.value = key;

                // Animación de click
                btn.style.transform = 'scale(0.95)';
                setTimeout(() => {
                    form.dispatchEvent(new Event('submit'));
                }, 150);
            };

            sugerenciasDiv.appendChild(btn);
        });

        // Animación de aparición
        setTimeout(() => {
            sugerenciasDiv.style.transition = 'all 0.5s ease-out';
            sugerenciasDiv.style.opacity = '1';
            sugerenciasDiv.style.transform = 'translateY(0)';
        }, 100);

        return sugerenciasDiv;
    }

    // Función para obtener iconos apropiados para las sugerencias
    function obtenerIconoSugerencia(texto) {
        const iconos = {
            'reserva': 'bx bx-calendar-plus',
            'lugar': 'bx bx-building',
            'disponibilidad': 'bx bx-calendar-check',
            'cancelar': 'bx bx-calendar-x',
            'ayuda': 'bx bx-help-circle',
            'precio': 'bx bx-money',
            'contacto': 'bx bx-phone',
            'horario': 'bx bx-time',
            'espacios': 'bx bx-building',
            'información': 'bx bx-info-circle'
        };

        for (let [palabra, icono] of Object.entries(iconos)) {
            if (texto.toLowerCase().includes(palabra)) {
                return icono;
            }
        }

        return 'bx bx-chevron-right';
    }

    // Función para detectar si el texto contiene HTML
    function esHTML(texto) {
        const htmlRegex = /<[^>]*>/g;
        return htmlRegex.test(texto);
    }

    // Función para limpiar y validar HTML seguro
    function limpiarHTML(html) {
        // Lista de tags permitidos para el chatbot
        const tagsPermitidos = ['div', 'span', 'p', 'strong', 'b', 'i', 'em', 'br', 'ul', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'];

        // Crear un elemento temporal para parsear el HTML
        const temp = document.createElement('div');
        temp.innerHTML = html;

        // Función recursiva para limpiar elementos
        function limpiarElemento(elemento) {
            const tagName = elemento.tagName?.toLowerCase();

            if (!tagsPermitidos.includes(tagName)) {
                // Si el tag no está permitido, reemplazar con span
                const span = document.createElement('span');
                span.innerHTML = elemento.innerHTML;
                return span;
            }

            // Limpiar atributos peligrosos
            Array.from(elemento.attributes).forEach(attr => {
                if (!['class', 'style', 'id'].includes(attr.name)) {
                    elemento.removeAttribute(attr.name);
                }
            });

            // Procesar hijos recursivamente
            Array.from(elemento.children).forEach(child => {
                const cleaned = limpiarElemento(child);
                if (cleaned !== child) {
                    elemento.replaceChild(cleaned, child);
                }
            });

            return elemento;
        }

        // Limpiar todos los elementos
        Array.from(temp.children).forEach(child => {
            limpiarElemento(child);
        });

        return temp.innerHTML;
    }

    // Estructura mejorada de mensajes con animaciones - CORREGIDA
    function agregarMensaje(tipo, texto, lento = false, fecha = null) {
        const li = document.createElement('li');
        li.className = tipo === 'bot' ? 'chat-message' : 'chat-message chat-message-right';
        li.style.opacity = '0';
        li.style.transform = 'translateY(20px)';

        const horaFormateada = formatearHora(fecha || new Date());
        const estadoMensaje = tipo === 'user' ?
            '<i class="bx bx-check-double text-success"></i>' :
            '<i class="bx bx-bot text-primary"></i>';

        // Determinar si el contenido es HTML o texto plano
        const esContenidoHTML = tipo === 'bot' && esHTML(texto);
        let contenidoMensaje;

        if (esContenidoHTML) {
            // Si es HTML, limpiarlo y usarlo directamente
            contenidoMensaje = limpiarHTML(texto);
        } else {
            // Si es texto plano, usar un párrafo
            contenidoMensaje = lento ? '' : `<p class="mb-0">${texto}</p>`;
        }

        li.innerHTML = tipo === 'bot' ? `
          <div class="d-flex">
            <div class="user-avatar flex-shrink-0 me-3">
              <div class="avatar avatar-sm">
                <img src="https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/estaticos/asistente2.webp" alt="Bot" class="rounded-circle" style="border: 2px solid #696cff;">
                <div class="position-absolute bottom-0 end-0 bg-success rounded-circle" 
                     style="width: 8px; height: 8px; border: 1px solid white;"></div>
              </div>
            </div>
            <div class="chat-message-wrapper flex-grow-1 mt-2">
              <div class="chat-message-text bg-white p-3 rounded-4 shadow-sm position-relative">
                <div class="mensaje-contenido">${contenidoMensaje}</div>
                <div class="position-absolute top-0 start-0 w-100 h-100 rounded-4 bg-gradient" 
                     style="opacity: 0.05; pointer-events: none; background: linear-gradient(135deg, #696cff 0%, #5a54e6 100%);"></div>
              </div>
              <div class="text-muted mt-1 d-flex align-items-center justify-content-between">
                <small>${estadoMensaje} ${horaFormateada}</small>
                <small class="opacity-50">Bot</small>
              </div>
            </div>
          </div>` : `
          <div class="d-flex justify-content-end">
            <div class="chat-message-wrapper mt-2">
              <div class="chat-message-text text-white p-3 rounded-4 position-relative overflow-hidden">
                <p class="mb-0">${texto}</p>
                <div class="position-absolute top-0 start-0 w-100 h-100" 
                     style="background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, transparent 50%);"></div>
              </div>
              <div class="text-end text-muted mt-1 d-flex align-items-center justify-content-between">
                <small class="opacity-50">Tú</small>
                <small>${estadoMensaje} ${horaFormateada}</small>
              </div>
            </div>
          </div>`;

        messageList.appendChild(li);

        // Animación de aparición
        setTimeout(() => {
            li.style.transition = 'all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1)';
            li.style.opacity = '1';
            li.style.transform = 'translateY(0)';
        }, 50);

        // Efecto de escritura para mensajes del bot (solo para texto plano)
        if (tipo === 'bot' && lento && !esContenidoHTML) {
            const contenedor = li.querySelector('.mensaje-contenido');
            contenedor.innerHTML = '<p class="mb-0"></p>';
            const p = contenedor.querySelector('p');
            escribirMensajeLento(texto, p, () => {
                // Callback al finalizar escritura
                li.classList.add('message-complete');
            });
        } else {
            scrollToBottom();
        }

        return li;
    }

    // Funciones para notificaciones
    function mostrarNotificacionError(mensaje) {
        mostrarNotificacion(mensaje, 'error');
    }

    function mostrarNotificacionInfo(mensaje) {
        mostrarNotificacion(mensaje, 'info');
    }

    function mostrarNotificacion(mensaje, tipo = 'info') {
        const notification = document.createElement('div');
        notification.className = `alert alert-${tipo === 'error' ? 'danger' : 'info'} alert-dismissible position-fixed`;
        notification.style.cssText = `
            top: 20px; right: 20px; z-index: 10000; min-width: 300px;
            animation: slideInFromRight 0.5s ease-out;
            box-shadow: 0 8px 32px rgba(0,0,0,0.15);
        `;
        notification.innerHTML = `
            <i class="bx ${tipo === 'error' ? 'bx-error' : 'bx-info-circle'} me-2"></i>
            ${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(notification);

        setTimeout(() => {
            notification.remove();
        }, 5000);
    }

    // Contador de caracteres mejorado
    function actualizarContadorCaracteres() {
        const charCounter = document.getElementById('char-counter');
        const length = input.value.length;

        if (charCounter) {
            charCounter.textContent = `${length}/500`;

            // Cambiar estilos según proximidad al límite
            charCounter.className = 'position-absolute end-0 bottom-0 me-2 mb-1';
            charCounter.style.fontSize = '0.7rem';

            if (length > 450) {
                charCounter.classList.add('text-warning');
            } else if (length > 480) {
                charCounter.classList.add('text-danger');
                charCounter.style.fontWeight = 'bold';
            } else {
                charCounter.classList.add('text-muted');
                charCounter.style.fontWeight = 'normal';
                charCounter.style.opacity = '0.6';
            }
        }
    }

    // Función para detectar inactividad y mostrar sugerencias
    function reiniciarTimerInactividad() {
        clearTimeout(tiempoInactividad);
        tiempoInactividad = setTimeout(() => {
            if (chatbotWindow && chatbotWindow.classList.contains('show')) {
                mostrarSugerenciasInactividad();
            }
        }, 60000); // 1 minuto de inactividad
    }

    function mostrarSugerenciasInactividad() {
        const mensajesInactividad = [
            "¿Necesitas ayuda con algo más? 🤔",
            "Estoy aquí si tienes alguna pregunta 💭",
            "¿Te gustaría ver qué puedo hacer por ti? ✨"
        ];

        const mensaje = mensajesInactividad[Math.floor(Math.random() * mensajesInactividad.length)];
        agregarMensaje('bot', mensaje, true);
    }

    // Función para actualizar visibilidad de botones de scroll
    function actualizarBotonesScroll(container) {
        const leftBtn = container.parentElement.querySelector('.scroll-btn:first-of-type');
        const rightBtn = container.parentElement.querySelector('.scroll-btn:last-of-type');

        if (leftBtn && rightBtn) {
            const isAtStart = container.scrollLeft <= 0;
            const isAtEnd = container.scrollLeft >= container.scrollWidth - container.clientWidth - 1;

            leftBtn.style.opacity = isAtStart ? '0.5' : '1';
            leftBtn.style.pointerEvents = isAtStart ? 'none' : 'auto';

            rightBtn.style.opacity = isAtEnd ? '0.5' : '1';
            rightBtn.style.pointerEvents = isAtEnd ? 'none' : 'auto';
        }
    }

    // Event listeners adicionales
    input.addEventListener('input', actualizarContadorCaracteres);

    // Shortcuts de teclado
    input.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            e.preventDefault();
            form.dispatchEvent(new Event('submit'));
        }

        if (e.key === 'Escape') {
            cerrarChatbot();
        }
    });

    // Event listeners globales para efectos mejorados
    if (toggleBtn) {
        // Efecto de hover mejorado
        toggleBtn.addEventListener('mouseenter', function() {
            this.style.transform = 'scale(1.1) rotate(5deg)';
        });

        toggleBtn.addEventListener('mouseleave', function() {
            this.style.transform = 'scale(1) rotate(0deg)';
        });

        // Efecto de click con ondas
        toggleBtn.addEventListener('click', function(e) {
            const onda = document.createElement('div');
            onda.style.cssText = `
                position: absolute;
                border-radius: 50%;
                background: rgba(255,255,255,0.6);
                transform: scale(0);
                animation: onda-click 0.6s ease-out;
                left: 50%;
                top: 50%;
                width: 100px;
                height: 100px;
                margin-left: -50px;
                margin-top: -50px;
                pointer-events: none;
            `;

            this.appendChild(onda);

            setTimeout(() => {
                onda.remove();
            }, 600);
        });
    }

    // Inicializar timer de inactividad
    document.addEventListener('click', reiniciarTimerInactividad);
    document.addEventListener('keypress', reiniciarTimerInactividad);
    reiniciarTimerInactividad();

    // Observador de scroll para comandos rápidos
    const container = document.getElementById("quickCommandScroll");
    if (container) {
        container.addEventListener('scroll', () => {
            actualizarBotonesScroll(container);
        });

        // Inicializar estado de botones
        setTimeout(() => {
            actualizarBotonesScroll(container);
        }, 100);
    }

    // Detectar si el usuario está escribiendo
    if (input) {
        let escribiendoTimeout;

        input.addEventListener('input', function() {
            clearTimeout(escribiendoTimeout);

            escribiendoTimeout = setTimeout(() => {
                // Usuario dejó de escribir - para futuras mejoras
            }, 1000);
        });
    }

    // Inicialización
    actualizarContadorCaracteres();

    console.log('🤖 Chatbot mejorado inicializado correctamente');
});

// Función mejorada para enviar comandos rápidos
function enviarComando(comando) {
    const input = document.getElementById("chatbot-message");
    const form = document.getElementById("chatbot-form");
    const chatbotWindow = document.getElementById('chatbot-window');

    if (input && form) {
        // Abrir chatbot si está cerrado
        if (!chatbotWindow.classList.contains('show')) {
            document.getElementById('chatbot-toggle').click();

            // Esperar a que se abra para enviar el comando
            setTimeout(() => {
                enviarComandoInterno(input, form, comando);
            }, 600);
        } else {
            enviarComandoInterno(input, form, comando);
        }
    } else {
        console.warn("Input o formulario del chatbot no encontrado.");
    }
}

// Función interna para enviar comando
function enviarComandoInterno(input, form, comando) {
    // Animación de escritura del comando
    input.value = '';
    let i = 0;

    const escribirComando = () => {
        if (i < comando.length) {
            input.value += comando.charAt(i);
            i++;
            setTimeout(escribirComando, 50);
        } else {
            // Efecto visual en el input
            input.style.transform = 'scale(1.02)';
            input.style.boxShadow = '0 0 20px rgba(105, 108, 255, 0.3)';

            setTimeout(() => {
                input.style.transform = '';
                input.style.boxShadow = '';
                form.requestSubmit();
            }, 300);
        }
    };

    escribirComando();
}

// Función mejorada para scroll de comandos rápidos
function scrollQuickCommands(direction) {
    const container = document.getElementById("quickCommandScroll");
    if (container) {
        const scrollAmount = 150;
        const currentScroll = container.scrollLeft;
        const maxScroll = container.scrollWidth - container.clientWidth;

        let newPosition = currentScroll + (direction * scrollAmount);

        // Controlar límites
        if (newPosition < 0) newPosition = 0;
        if (newPosition > maxScroll) newPosition = maxScroll;

        container.scrollTo({
            left: newPosition,
            behavior: 'smooth'
        });

        // Actualizar visibilidad de botones de scroll
        setTimeout(() => {
            actualizarBotonesScroll(container);
        }, 300);
    }
}

// Función para actualizar visibilidad de botones de scroll
function actualizarBotonesScroll(container) {
    const leftBtn = container.parentElement.querySelector('.scroll-btn:first-of-type');
    const rightBtn = container.parentElement.querySelector('.scroll-btn:last-of-type');

    if (leftBtn && rightBtn) {
        const isAtStart = container.scrollLeft <= 0;
        const isAtEnd = container.scrollLeft >= container.scrollWidth - container.clientWidth - 1;

        leftBtn.style.opacity = isAtStart ? '0.5' : '1';
        leftBtn.style.pointerEvents = isAtStart ? 'none' : 'auto';

        rightBtn.style.opacity = isAtEnd ? '0.5' : '1';
        rightBtn.style.pointerEvents = isAtEnd ? 'none' : 'auto';
    }
}

// Función para agregar efectos de partículas (opcional)
function agregarEfectoParticulas(elemento) {
    const particulas = document.createElement('div');
    particulas.className = 'particulas-efecto';
    particulas.style.cssText = `
        position: absolute;
        top: 50%;
        left: 50%;
        width: 4px;
        height: 4px;
        background: #696cff;
        border-radius: 50%;
        pointer-events: none;
        animation: particula-explosion 0.6s ease-out forwards;
    `;

    elemento.appendChild(particulas);

    setTimeout(() => {
        particulas.remove();
    }, 600);
}

// Función para mostrar notificación de nuevo mensaje
function mostrarNotificacionMensaje(texto = 'Nuevo mensaje') {
    const notification = document.getElementById('chatbot-notification');
    if (notification) {
        notification.innerHTML = `<i class="bx bx-bell me-1"></i>${texto}`;
        notification.style.display = 'block';

        setTimeout(() => {
            notification.style.display = 'none';
        }, 3000);
    }
}

// Función de utilidad para debugging
function debugChatbot() {
    const chatbotWindow = document.getElementById('chatbot-window');
    const messageList = chatbotWindow?.querySelector('.chat-history');

    console.log('🔍 Debug Chatbot:', {
        version: '2.0 Mejorado',
        elementos: {
            toggle: !!document.getElementById('chatbot-toggle'),
            window: !!chatbotWindow,
            form: !!document.getElementById('chatbot-form'),
            input: !!document.getElementById('chatbot-message'),
            messageList: !!messageList
        },
        estado: chatbotWindow?.classList.contains('show') ? 'abierto' : 'cerrado',
        historial: messageList?.children.length + ' mensajes' || '0 mensajes',
        tema: chatbotWindow?.classList.contains('tema-oscuro') ? 'oscuro' : 'claro'
    });
}

// Función para limpiar historial del chat
function limpiarHistorialChat() {
    const messageList = document.querySelector('.chat-history');
    if (messageList) {
        // Animación de desvanecimiento
        Array.from(messageList.children).forEach((mensaje, index) => {
            setTimeout(() => {
                mensaje.style.animation = 'message-appear 0.3s ease-out reverse';
                setTimeout(() => {
                    mensaje.remove();
                }, 300);
            }, index * 50);
        });

        // Mensaje de confirmación después de limpiar
        setTimeout(() => {
            const chatHistory = [];
            console.log('🧹 Historial del chat limpiado');
        }, messageList.children.length * 50 + 500);
    }
}

// Función para exportar conversación (para desarrollo/debug)
function exportarConversacion() {
    const chatHistory = [];
    const mensajes = document.querySelectorAll('.chat-message');

    mensajes.forEach(mensaje => {
        const esBot = !mensaje.classList.contains('chat-message-right');
        const texto = mensaje.querySelector('.chat-message-text p')?.textContent || '';
        const hora = mensaje.querySelector('small')?.textContent || '';

        chatHistory.push({
            tipo: esBot ? 'bot' : 'usuario',
            mensaje: texto,
            hora: hora
        });
    });

    console.log('📋 Conversación exportada:', chatHistory);
    return chatHistory;
}

// Función para detectar dispositivo móvil
function esMobile() {
    return window.innerWidth <= 768 || /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}

// Función para ajustar UI según dispositivo
function ajustarUISegunDispositivo() {
    const chatbotWindow = document.getElementById('chatbot-window');
    const toggleBtn = document.getElementById('chatbot-toggle');

    if (esMobile() && chatbotWindow && toggleBtn) {
        // Ajustes específicos para móviles
        chatbotWindow.style.width = '380px';
        chatbotWindow.style.right = '16px';

        toggleBtn.style.width = '56px';
        toggleBtn.style.height = '56px';
        toggleBtn.style.fontSize = '24px';
    }
}

// Función para manejar cambios de orientación en móviles
function manejarCambioOrientacion() {
    setTimeout(() => {
        ajustarUISegunDispositivo();

        // Recalcular scroll de comandos rápidos
        const container = document.getElementById("quickCommandScroll");
        if (container) {
            actualizarBotonesScroll(container);
        }
    }, 300);
}

// Función para precargar avatares e imágenes
function precargarRecursos() {
    const imagenes = [
        'https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/estaticos/asistente2.webp',
        // Agregar más recursos si es necesario
    ];

    imagenes.forEach(src => {
        const img = new Image();
        img.src = src;
    });
}

// Función para manejar errores de red
function manejarErrorRed(error) {
    console.error('🔴 Error de red en chatbot:', error);

    const mensajeError = "🌐 Parece que hay problemas de conexión. Verificando...";

    // Intentar reconexión después de un momento
    setTimeout(() => {
        if (navigator.onLine) {
            console.log('🟢 Conexión restaurada');
        } else {
            console.log('🔴 Sin conexión a internet');
        }
    }, 2000);

    return mensajeError;
}

// Función para validar entrada del usuario
function validarEntradaUsuario(texto) {
    const validaciones = {
        vacio: texto.trim().length === 0,
        muyLargo: texto.length > 500,
        soloEspacios: /^\s+$/.test(texto),
        caracteresInvalidos: /[<>{}[\]\\]/.test(texto)
    };

    if (validaciones.vacio || validaciones.soloEspacios) {
        return { valido: false, error: 'El mensaje no puede estar vacío' };
    }

    if (validaciones.muyLargo) {
        return { valido: false, error: 'El mensaje es demasiado largo (máximo 500 caracteres)' };
    }

    if (validaciones.caracteresInvalidos) {
        return { valido: false, error: 'El mensaje contiene caracteres no permitidos' };
    }

    return { valido: true };
}

// Función para formatear texto con markdown básico
function formatearTextoMarkdown(texto) {
    return texto
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/\n/g, '<br>');
}

// Event listeners para eventos del sistema
window.addEventListener('resize', manejarCambioOrientacion);
window.addEventListener('orientationchange', manejarCambioOrientacion);

// Event listener para detectar cambios de conectividad
window.addEventListener('online', () => {
    console.log('🟢 Conexión restaurada');
});

window.addEventListener('offline', () => {
    console.log('🔴 Conexión perdida');
});

// Event listener para prevenir cierre accidental
window.addEventListener('beforeunload', (e) => {
    const chatbotWindow = document.getElementById('chatbot-window');
    const messageList = chatbotWindow?.querySelector('.chat-history');

    // Solo prevenir si hay mensajes no guardados
    if (messageList && messageList.children.length > 0) {
        // No mostrar diálogo por defecto, solo log para debug
        console.log('🚪 Usuario intentando salir con conversación activa');
    }
});

// Inicialización adicional cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', function() {
    // Ajustar UI según dispositivo
    ajustarUISegunDispositivo();

    // Precargar recursos
    precargarRecursos();

    // Event listener para cambios de tema del sistema
    if (window.matchMedia) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            try {
                const temaGuardado = localStorage.getItem('chatbot-tema');
                if (!temaGuardado) { // Solo cambiar si no hay preferencia guardada
                    const chatWindow = document.getElementById('chatbot-window');
                    if (chatWindow) {
                        if (e.matches) {
                            chatWindow.classList.add('tema-oscuro');
                        } else {
                            chatWindow.classList.remove('tema-oscuro');
                        }
                    }
                }
            } catch (err) {
                console.log('Error al cambiar tema automáticamente');
            }
        });
    }
});

// Exponer funciones útiles globalmente para desarrollo y debug
window.chatbotUtils = {
    debug: debugChatbot,
    limpiarHistorial: limpiarHistorialChat,
    exportarConversacion: exportarConversacion,
    alternarTema: () => {
        const chatWindow = document.getElementById('chatbot-window');
        if (chatWindow) {
            chatWindow.classList.toggle('tema-oscuro');
            try {
                const esTemaOscuro = chatWindow.classList.contains('tema-oscuro');
                localStorage.setItem('chatbot-tema', esTemaOscuro ? 'oscuro' : 'claro');
                console.log(`🎨 Tema cambiado a: ${esTemaOscuro ? 'oscuro' : 'claro'}`);
            } catch (e) {
                console.log('No se pudo guardar preferencia de tema');
            }
        }
    },
    enviarMensajeProgramatico: (mensaje) => {
        const input = document.getElementById('chatbot-message');
        const form = document.getElementById('chatbot-form');
        if (input && form) {
            input.value = mensaje;
            form.dispatchEvent(new Event('submit'));
        }
    }
};

console.log('🚀 Chatbot JavaScript completo cargado');
console.log('💡 Usa window.chatbotUtils para funciones de debug');
console.log('🎯 Ejemplo: window.chatbotUtils.debug() para ver estado del chatbot');