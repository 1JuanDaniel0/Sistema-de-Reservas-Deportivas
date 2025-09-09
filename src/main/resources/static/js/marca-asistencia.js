document.addEventListener('DOMContentLoaded', () => {
    const iframe = document.getElementById('mapIframe');
    const loader = document.getElementById('mapLoader');
    const latlonInput = document.getElementById('latlon');
    const locationStatus = document.getElementById('locationStatus');
    const attendanceAction = document.getElementById('attendanceAction');
    const submitButton = document.getElementById('submitAttendance');
    const modal = document.getElementById('modalAsistencia');
    const form = document.getElementById('attendanceForm');

    // Función para forzar mostrar el mapa
    function showMap() {
        console.log("=== FORZANDO MOSTRAR MAPA ===");
        // Forzar ocultar spinner con múltiples métodos
        loader.style.display = 'none';
        loader.style.visibility = 'hidden';
        loader.style.opacity = '0';
        loader.classList.add('d-none');
        // Forzar mostrar iframe con múltiples métodos
        iframe.style.display = 'block';
        iframe.style.visibility = 'visible';
        iframe.style.opacity = '1';
        iframe.classList.remove('d-none');
        // Verificar estados después del cambio
        console.log("Loader display:", loader.style.display);
        console.log("Loader classList:", loader.classList.toString());
        console.log("Iframe display:", iframe.style.display);
        console.log("Iframe classList:", iframe.classList.toString());
        // Verificar si los elementos son visibles
        const loaderRect = loader.getBoundingClientRect();
        const iframeRect = iframe.getBoundingClientRect();
        console.log("Loader visible:", loaderRect.width > 0 && loaderRect.height > 0);
        console.log("Iframe visible:", iframeRect.width > 0 && iframeRect.height > 0);
    }
    // Al abrir el modal
    modal.addEventListener('show.bs.modal', () => {
        console.log("Modal abierto - iniciando geolocalización");
        // Estado inicial - mostrar spinner
        loader.style.display = 'flex';
        loader.style.visibility = 'visible';
        loader.style.opacity = '1';
        loader.classList.remove('d-none');
        // Ocultar iframe inicialmente
        iframe.style.display = 'none';
        iframe.style.visibility = 'hidden';
        iframe.style.opacity = '0';
        iframe.classList.add('d-none');
        locationStatus.textContent = "";
        submitButton.disabled = true;
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lon = position.coords.longitude;
                // Openstreetmap porque maps pide api key
                const mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${lon-0.01},${lat-0.01},${lon+0.01},${lat+0.01}&layer=mapnik&marker=${lat},${lon}`;
                latlonInput.value = `${lat},${lon}`;
                // Verificar si la ubicación es válida
                //fetch(`/coordinador/verificar-ubicacion?latlon=${lat},${lon}`)
                //    .then(res => res.json())
                //    .then(data => {
                //        console.log("Respuesta de verificación de ubicación:", data);
                //        if (data.ubicacionValida === true) {
                //            submitButton.disabled = false;
                //            locationStatus.textContent = `Ubicación válida: ${data.lugarNombre}. Puedes marcar asistencia.`;
                //            locationStatus.classList.remove("text-danger");
                //            locationStatus.classList.add("text-success");
                //        } else {
                //            submitButton.disabled = true;
                //            locationStatus.textContent = "No estás cerca de ningún lugar asignado (distancia máxima: 5 km).";
                //            locationStatus.classList.remove("text-success");
                //            locationStatus.classList.add("text-danger");
                //        }
                //    })
                //    .catch(error => {
                //        console.error("Error verificando ubicación:", error);
                //        // En caso de error, habilitamos el botón y dejamos que la validación
                //        // ocurra en el servidor cuando se envíe el formulario
                //        submitButton.disabled = false;
                //        locationStatus.textContent = "No se pudo verificar la ubicación. Intenta marcar asistencia de todas formas.";
                //        locationStatus.classList.remove("text-success");
                //        locationStatus.classList.add("text-warning");
                //    });
                console.log("Coordenadas obtenidas:");
                console.log("Latitud:", lat);
                console.log("Longitud:", lon);
                console.log("Iframe src:", mapUrl);
                // Configurar onload
                iframe.onload = function() {
                    console.log("Iframe terminó de cargar - mostrando mapa");
                    setTimeout(() => {
                        showMap();
                    }, 300);
                };
                // Timeout de respaldo más agresivo
                setTimeout(() => {
                    console.log("Timeout alcanzado - forzando mostrar mapa");
                    showMap();
                }, 3000);
                // Establecer la src
                iframe.src = mapUrl;
                // Consultar estado actual
                console.log("Consultando estado actual...");
                fetch('/coordinador/asistencia/estado-actual')
                    .then(res => {
                        console.log("Response status:", res.status);
                        if (!res.ok) {
                            throw new Error(`HTTP ${res.status}`);
                        }
                        return res.json();
                    })
                    .then(data => {
                        console.log("Estado actual recibido:", data);
                        attendanceAction.textContent = data.enCurso ? 'Marcar Salida' : 'Marcar Entrada';
                        submitButton.classList.toggle('btn-success', !data.enCurso);
                        submitButton.classList.toggle('btn-warning', data.enCurso);
                        submitButton.disabled = false; // Habilitar el botón después de obtener la ubicación
                    })
                    .catch(error => {
                        console.error("Error consultando estado:", error);
                        attendanceAction.textContent = 'Marcar Asistencia';
                    });
            },
            (error) => {
                console.error("Error de geolocalización:", error);
                loader.style.display = 'none';
                loader.classList.add('d-none');
                iframe.style.display = 'none';
                locationStatus.textContent = "Error al detectar la ubicación. Asegúrate de tener el GPS activado y dar permisos al navegador.";
                submitButton.disabled = true;
            }
        );
    });
    // Limpiar al cerrar modal
    modal.addEventListener('hidden.bs.modal', () => {
        console.log("Modal cerrado - limpiando iframe");
        iframe.src = '';
        iframe.onload = null;
        // Restablecer estado inicial
        loader.style.display = 'flex';
        loader.style.visibility = 'visible';
        loader.style.opacity = '1';
        loader.classList.remove('d-none');
        iframe.style.display = 'none';
        iframe.style.visibility = 'hidden';
        iframe.style.opacity = '0';
        iframe.classList.add('d-none');
    });
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        console.log("Enviando formulario de asistencia");
        const formData = new FormData(form);
        console.log("Datos del formulario:");
        for (let [key, value] of formData.entries()) {
            console.log(key, value);
        }
        // Imprimir también los valores de latlon explícitamente
        console.log("Valor de latlon:", document.getElementById('latlon').value);
        // Deshabilitar botón para evitar doble envío
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Procesando...';
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        fetch('/coordinador/marcarAsistencia', {
            method: 'POST',
            body: formData,
            headers: {
                [csrfHeader]: csrfToken,
                'Accept': 'application/json'
            }
        })
            .then(response => {
                console.log("Response status:", response.status);
                console.log("Response redirected:", response.redirected);
                console.log("Response URL:", response.url);
                return response.text().then(text => ({
                    success: response.ok,
                    redirected: response.redirected,
                    status: response.status,
                    text: text,
                    url: response.url
                }));
            })
            .then((result) => {
                // Reactivar botón
                submitButton.disabled = false;
                submitButton.innerHTML = '<i class="bx bx-check-circle"></i> <span id="attendanceAction">Confirmar</span>';
                console.log("Analizando respuesta del formulario:", result);

                // Cerrar modal antes de mostrar cualquier mensaje
                const modalInstance = bootstrap.Modal.getInstance(modal);
                if (modalInstance) modalInstance.hide();

                // MODIFICACIÓN: Evaluar la respuesta basada principalmente en el código de estado HTTP
                // En lugar de buscar patrones de texto que pueden ser ambiguos

                // Si el código de estado es un error (4xx o 5xx)
                if (result.status >= 400) {
                    console.log("Error HTTP detectado:", result.status);

                    // Mensaje específico para error de ubicación si se puede detectar
                    if (result.text && result.text.toLowerCase().includes('no está cerca de ningún lugar')) {
                        Swal.fire({
                            icon: 'warning',
                            title: 'Ubicación no válida',
                            text: 'No estás cerca de ninguno de tus lugares asignados. Debes estar dentro de 5 km para marcar asistencia.',
                            confirmButtonText: 'Entendido',
                            confirmButtonColor: '#f39c12'
                        });
                    } else {
                        // Error general
                        Swal.fire({
                            icon: 'error',
                            title: 'Error al registrar asistencia',
                            text: 'Hubo un problema al registrar tu asistencia. Por favor, intenta nuevamente.',
                            confirmButtonText: 'Reintentar',
                            confirmButtonColor: '#e74c3c'
                        });
                    }
                    return;
                }

                // Si hay redirección y el estado es OK (2xx), es un éxito
                if (result.success) {
                    console.log("Operación exitosa");
                    Swal.fire({
                        icon: 'success',
                        title: '¡Asistencia registrada!',
                        text: 'Tu asistencia se ha registrado exitosamente.',
                        timer: 3000,
                        showConfirmButton: false,
                        confirmButtonColor: '#28a745'
                    }).then(() => {
                        // Recargar la página para mostrar datos actualizados
                        window.location.reload();
                    });
                    return;
                }

                // Si llegamos aquí, algo inesperado ocurrió
                console.log("Respuesta inesperada - mostrando mensaje genérico");
                Swal.fire({
                    icon: 'error',
                    title: 'Error inesperado',
                    text: 'Hubo un problema al procesar tu solicitud. Por favor, intenta nuevamente.',
                    confirmButtonText: 'Reintentar',
                    confirmButtonColor: '#e74c3c'
                });
            })
            .catch(err => {
                console.error("Error enviando formulario:", err);
                // Reactivar botón
                submitButton.disabled = false;
                submitButton.innerHTML = '<i class="bx bx-check-circle"></i> <span id="attendanceAction">Confirmar</span>';
                // Cerrar modal
                const modalInstance = bootstrap.Modal.getInstance(modal);
                if (modalInstance) modalInstance.hide();
                // Mostrar error de conexión
                Swal.fire({
                    icon: 'error',
                    title: 'Error de conexión',
                    text: 'No se pudo conectar con el servidor. Verifica tu conexión a internet.',
                    confirmButtonText: 'Reintentar',
                    confirmButtonColor: '#e74c3c'
                });
            });
    });

});