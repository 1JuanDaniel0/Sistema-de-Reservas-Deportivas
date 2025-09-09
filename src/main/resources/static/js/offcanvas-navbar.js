// Mobile Offcanvas Navigation Script - Versión Corregida
// Control manual completo sin dependencias de Bootstrap

document.addEventListener('DOMContentLoaded', function() {
    console.log('🔧 Offcanvas script iniciando...');

    // Variables globales
    let isOffcanvasOpen = false;
    let animationTimeout;
    let isInitialized = false;

    // Elementos del DOM
    const mobileOffcanvas = document.getElementById('landingNav');
    const mobileNavLinks = document.querySelectorAll('.mobile-nav-link');
    const mobileContactLink = document.querySelector('.mobile-contact-link');
    const mobileCloseBtn = document.querySelector('.mobile-close-btn');

    // Verificar que el offcanvas existe
    if (!mobileOffcanvas) {
        console.log('🔧 Offcanvas no encontrado, saliendo...');
        return;
    }

    console.log('🔧 Offcanvas encontrado, inicializando...');

    // Inicializar después de que el DOM esté completamente cargado
    setTimeout(() => {
        initializeMobileOffcanvas();
    }, 100);

    function initializeMobileOffcanvas() {
        if (isInitialized) {
            console.log('🔧 Ya está inicializado, saliendo...');
            return;
        }

        console.log('🔧 Inicializando control manual del offcanvas...');
        isInitialized = true;

        // Prevenir cualquier instancia existente de Bootstrap
        if (mobileOffcanvas._bsOffcanvas) {
            console.log('🔧 Destruyendo instancia Bootstrap existente...');
            try {
                mobileOffcanvas._bsOffcanvas.dispose();
                mobileOffcanvas._bsOffcanvas = null;
            } catch (e) {
                console.log('🔧 Error al destruir instancia Bootstrap:', e);
            }
        }

        // Buscar y configurar botones toggle
        setupToggleButtons();

        // Configurar eventos del offcanvas
        setupOffcanvasEvents();

        // Marcar enlace activo
        markActiveLink();

        // Agregar estilos CSS necesarios
        addRequiredStyles();

        console.log('🔧 Inicialización completada');
    }

    function setupToggleButtons() {
        // Buscar todos los posibles botones toggle
        const toggleSelectors = [
            '[data-bs-toggle="offcanvas"][data-bs-target="#landingNav"]',
            '[data-custom-offcanvas="true"]',
            '.navbar-toggler'
        ];

        let buttonsFound = 0;

        toggleSelectors.forEach(selector => {
            const buttons = document.querySelectorAll(selector);
            buttons.forEach(button => {
                // Remover atributos Bootstrap
                button.removeAttribute('data-bs-toggle');
                button.removeAttribute('data-bs-target');
                button.removeAttribute('aria-controls');

                // Marcar como procesado
                button.setAttribute('data-custom-offcanvas', 'true');

                // Agregar event listener
                button.addEventListener('click', function(e) {
                    console.log('🔧 Click en botón toggle detectado');
                    e.preventDefault();
                    e.stopPropagation();
                    toggleOffcanvas();
                    return false;
                });

                buttonsFound++;
                console.log(`🔧 Botón configurado: ${selector}`);
            });
        });

        console.log(`🔧 Total de botones configurados: ${buttonsFound}`);
    }

    function setupOffcanvasEvents() {
        // Evento del botón de cerrar
        if (mobileCloseBtn) {
            mobileCloseBtn.addEventListener('click', function(e) {
                console.log('🔧 Click en botón cerrar detectado');
                e.preventDefault();
                e.stopPropagation();
                closeMobileOffcanvas();
            });
        }

        // Eventos para los enlaces de navegación
        mobileNavLinks.forEach((link, index) => {
            // Agregar animación de delay
            link.style.transitionDelay = `${index * 0.05}s`;

            // Evento de click con efecto ripple
            link.addEventListener('click', function(e) {
                addRippleEffect(this, e);

                // Si no es un enlace especial, cerrar el offcanvas
                if (!this.closest('form') && !this.classList.contains('mobile-contact-link')) {
                    setTimeout(() => {
                        closeMobileOffcanvas();
                    }, 200);
                }
            });

            // Efectos hover
            link.addEventListener('mouseenter', function() {
                if (!this.style.transition) {
                    this.style.transition = 'transform 0.2s ease';
                }
                this.style.transform = 'translateX(8px) scale(1.02)';
            });

            link.addEventListener('mouseleave', function() {
                this.style.transform = 'translateX(0) scale(1)';
            });
        });

        // Manejo especial para el enlace de contacto
        if (mobileContactLink) {
            mobileContactLink.addEventListener('click', function(e) {
                e.preventDefault();
                closeMobileOffcanvas();

                setTimeout(() => {
                    const contactSection = document.getElementById('contacto') ||
                        document.querySelector('#landingContact') ||
                        document.querySelector('.contact-section');
                    if (contactSection) {
                        contactSection.scrollIntoView({
                            behavior: 'smooth',
                            block: 'start'
                        });
                    }
                }, 300);
            });
        }

        // Cerrar con tecla ESC
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && isOffcanvasOpen) {
                console.log('🔧 Cerrando por tecla ESC');
                closeMobileOffcanvas();
            }
        });

        // Cerrar al redimensionar ventana
        window.addEventListener('resize', function() {
            if (window.innerWidth >= 992 && isOffcanvasOpen) {
                console.log('🔧 Cerrando por resize a desktop');
                closeMobileOffcanvas();
            }
        });
    }

    function toggleOffcanvas() {
        console.log('🔧 Toggle offcanvas, estado actual:', isOffcanvasOpen);

        if (isOffcanvasOpen) {
            closeMobileOffcanvas();
        } else {
            openMobileOffcanvas();
        }
    }

    function openMobileOffcanvas() {
        if (isOffcanvasOpen) {
            console.log('🔧 Ya está abierto, saliendo...');
            return;
        }

        console.log('🔧 Abriendo offcanvas manualmente...');
        isOffcanvasOpen = true;

        // Crear backdrop
        createBackdrop();

        // Aplicar clases y estilos
        mobileOffcanvas.classList.add('show');
        mobileOffcanvas.style.visibility = 'visible';
        mobileOffcanvas.style.transform = 'translateX(0)';

        document.body.classList.add('mobile-offcanvas-open');
        document.body.style.overflow = 'hidden';

        // Animar elementos después de un pequeño delay
        setTimeout(() => {
            animateNavItems();
        }, 100);

        // Enfocar primer enlace para accesibilidad
        setTimeout(() => {
            const firstLink = mobileOffcanvas.querySelector('.mobile-nav-link');
            if (firstLink) {
                firstLink.focus();
            }
        }, 300);
    }

    function closeMobileOffcanvas() {
        if (!isOffcanvasOpen) {
            console.log('🔧 Ya está cerrado, saliendo...');
            return;
        }

        console.log('🔧 Cerrando offcanvas manualmente...');
        isOffcanvasOpen = false;

        // Limpiar animaciones
        clearTimeout(animationTimeout);
        resetAnimations();

        // Remover clases y estilos
        mobileOffcanvas.classList.remove('show');
        mobileOffcanvas.style.transform = 'translateX(-100%)';

        document.body.classList.remove('mobile-offcanvas-open');
        document.body.style.overflow = '';

        // Remover backdrop
        removeBackdrop();

        // Ocultar completamente después de la transición
        setTimeout(() => {
            if (!isOffcanvasOpen) {
                mobileOffcanvas.style.visibility = 'hidden';
            }
        }, 300);
    }

    function createBackdrop() {
        // Remover backdrop existente
        let existingBackdrop = document.querySelector('.offcanvas-backdrop');
        if (existingBackdrop) {
            existingBackdrop.remove();
        }

        const backdrop = document.createElement('div');
        backdrop.className = 'offcanvas-backdrop fade show';
        backdrop.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            z-index: 1040;
            width: 100vw;
            height: 100vh;
            background-color: rgba(0, 0, 0, 0.6);
            backdrop-filter: blur(4px);
        `;

        // Click en backdrop para cerrar
        backdrop.addEventListener('click', () => {
            console.log('🔧 Click en backdrop detectado');
            closeMobileOffcanvas();
        });

        document.body.appendChild(backdrop);
    }

    function removeBackdrop() {
        const backdrop = document.querySelector('.offcanvas-backdrop');
        if (backdrop) {
            backdrop.classList.remove('show');
            setTimeout(() => {
                if (backdrop.parentNode) {
                    backdrop.parentNode.removeChild(backdrop);
                }
            }, 150);
        }
    }

    function animateNavItems() {
        const navItems = mobileOffcanvas.querySelectorAll('.mobile-nav-item, .mobile-nav-link');

        navItems.forEach((item, index) => {
            // Resetear estilos
            item.style.opacity = '0';
            item.style.transform = 'translateX(-30px)';
            item.style.transition = 'opacity 0.3s ease, transform 0.3s ease';

            // Animar con delay
            setTimeout(() => {
                item.style.opacity = '1';
                item.style.transform = 'translateX(0)';
            }, index * 50);
        });
    }

    function resetAnimations() {
        const navItems = mobileOffcanvas.querySelectorAll('.mobile-nav-item, .mobile-nav-link');

        navItems.forEach(item => {
            item.style.opacity = '0';
            item.style.transform = 'translateX(-30px)';
        });
    }

    function addRippleEffect(element, event) {
        // Verificar que el elemento y evento existen
        if (!element || !event) return;

        const ripple = document.createElement('span');
        const rect = element.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = (event.clientX || 0) - rect.left - size / 2;
        const y = (event.clientY || 0) - rect.top - size / 2;

        ripple.style.cssText = `
            position: absolute;
            width: ${size}px;
            height: ${size}px;
            left: ${x}px;
            top: ${y}px;
            background: rgba(255, 255, 255, 0.3);
            border-radius: 50%;
            transform: scale(0);
            animation: mobileRippleAnimation 0.6s ease-out;
            pointer-events: none;
            z-index: 1;
        `;

        element.style.position = 'relative';
        element.style.overflow = 'hidden';
        element.appendChild(ripple);

        setTimeout(() => {
            if (ripple.parentNode) {
                ripple.parentNode.removeChild(ripple);
            }
        }, 600);
    }

    function markActiveLink() {
        const currentPath = window.location.pathname;

        mobileNavLinks.forEach(link => {
            try {
                const linkPath = new URL(link.href).pathname;

                if (linkPath === currentPath ||
                    (currentPath === '/' && linkPath === '/') ||
                    (currentPath.startsWith(linkPath) && linkPath !== '/')) {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            } catch (e) {
                console.log('🔧 Error procesando enlace:', link.href);
            }
        });
    }

    function addRequiredStyles() {
        if (!document.querySelector('#mobile-offcanvas-styles')) {
            const style = document.createElement('style');
            style.id = 'mobile-offcanvas-styles';
            style.textContent = `
                @keyframes mobileRippleAnimation {
                    to {
                        transform: scale(2);
                        opacity: 0;
                    }
                }
                
                .mobile-offcanvas-open {
                    overflow: hidden !important;
                }

                #landingNav {
                    transition: transform 0.3s ease-in-out;
                }

                #landingNav:not(.show) {
                    transform: translateX(-100%);
                    visibility: hidden;
                }

                #landingNav.show {
                    transform: translateX(0);
                    visibility: visible;
                }

                .offcanvas-backdrop {
                    transition: opacity 0.15s linear;
                }

                .offcanvas-backdrop.fade:not(.show) {
                    opacity: 0;
                }

                .mobile-nav-link {
                    transition: transform 0.2s ease, opacity 0.3s ease;
                }
            `;
            document.head.appendChild(style);
        }
    }

    // API pública para uso externo
    window.MobileOffcanvasUtils = {
        close: closeMobileOffcanvas,
        open: openMobileOffcanvas,
        toggle: toggleOffcanvas,
        isOpen: () => isOffcanvasOpen,
        markActive: markActiveLink
    };

    // Debug en desarrollo
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        console.log('🚀 Mobile Offcanvas Navigation initialized successfully');

        // Exponer funciones para debugging
        window.debugOffcanvas = {
            state: () => ({ isOffcanvasOpen, isInitialized }),
            elements: { mobileOffcanvas, mobileNavLinks, mobileContactLink, mobileCloseBtn }
        };
    }
});