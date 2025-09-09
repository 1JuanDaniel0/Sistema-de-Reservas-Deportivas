if (typeof Swal !== 'undefined') {
    // Configuración por defecto para todos los alerts
    Swal.mixin({
        customClass: {
            confirmButton: 'btn btn-primary me-2',
            cancelButton: 'btn btn-secondary',
            denyButton: 'btn btn-warning me-2'
        },
        buttonsStyling: false,
        allowOutsideClick: false,
        allowEscapeKey: true,
        showCloseButton: true
    });

    // Funciones de utilidad para alerts comunes
    window.SwalUtils = {
        success: function(title, text = '') {
            return Swal.fire({
                icon: 'success',
                title: title,
                text: text,
                timer: 3000,
                showConfirmButton: false,
                toast: true,
                position: 'top-end'
            });
        },

        error: function(title, text = '') {
            return Swal.fire({
                icon: 'error',
                title: title,
                text: text,
                confirmButtonText: 'Entendido'
            });
        },

        warning: function(title, text = '') {
            return Swal.fire({
                icon: 'warning',
                title: title,
                text: text,
                confirmButtonText: 'Entendido'
            });
        },

        confirm: function(title, text, confirmText = 'Sí', cancelText = 'No') {
            return Swal.fire({
                title: title,
                text: text,
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: confirmText,
                cancelButtonText: cancelText,
                reverseButtons: true
            });
        },

        loading: function(title = 'Procesando...') {
            return Swal.fire({
                title: title,
                allowOutsideClick: false,
                allowEscapeKey: false,
                showConfirmButton: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });
        }
    };
} else {
    console.warn('SweetAlert2 no está disponible. Asegúrate de incluir la librería.');

    // Fallback a alerts nativos si SweetAlert2 no está disponible
    window.SwalUtils = {
        success: function(title, text = '') {
            alert(title + (text ? '\n' + text : ''));
        },
        error: function(title, text = '') {
            alert('Error: ' + title + (text ? '\n' + text : ''));
        },
        warning: function(title, text = '') {
            alert('Advertencia: ' + title + (text ? '\n' + text : ''));
        },
        confirm: function(title, text) {
            return Promise.resolve({
                isConfirmed: confirm(title + (text ? '\n' + text : ''))
            });
        },
        loading: function(title = 'Procesando...') {
            console.log(title);
        }
    };
}