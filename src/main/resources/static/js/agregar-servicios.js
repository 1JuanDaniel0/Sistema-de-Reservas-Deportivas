Dropzone.autoDiscover = false;

const previewTemplate = `
  <div class="dz-preview dz-file-preview">
    <div class="dz-details">
      <div class="dz-thumbnail">
        <img data-dz-thumbnail>
        <span class="dz-nopreview">No preview</span>
        <div class="dz-success-mark"></div>
        <div class="dz-error-mark"></div>
        <div class="dz-error-message"><span data-dz-errormessage></span></div>
        <div class="progress">
          <div class="progress-bar progress-bar-primary" role="progressbar" aria-valuemin="0" aria-valuemax="100" data-dz-uploadprogress></div>
        </div>
      </div>
      <div class="dz-filename" data-dz-name></div>
      <div class="dz-size" data-dz-size></div>
    </div>
  </div>`;

const myDropzone = new Dropzone("#dropzone-multi", {
    url: "/admin/agregar-servicios",
    autoProcessQueue: false,
    uploadMultiple: true,
    parallelUploads: 3,
    maxFiles: 3,
    maxFilesize: 5,
    paramName: "file",
    acceptedFiles: "image/*",
    addRemoveLinks: true,
    dictRemoveFile: "Quitar foto",
    previewTemplate: previewTemplate,
    init: function () {
        const dz = this;

        dz.on("addedfile", function (file) {
            const validTypes = ["image/jpeg", "image/png", "image/gif", "image/webp"];
            const totalCount = dz.files.filter(f => !f.toBeDeleted).length;

            if (!validTypes.includes(file.type)) {
                dz.removeFile(file);
                Swal.fire("Archivo no válido", "Solo puedes subir imágenes (jpg, png, gif, webp).", "warning");
                return;
            }

            if (totalCount > 3) {
                dz.removeFile(file);
                Swal.fire("Límite alcanzado", "Ya tienes 3 fotos en total.", "info");
                return;
            }

            actualizarPreviewDesdePrimeraImagen();
        });

        dz.on("removedfile", function () {
            actualizarPreviewDesdePrimeraImagen();
        });

        // Cargar vista previa si ya hay imágenes agregadas desde el navegador (al recargar la página)
        setTimeout(actualizarPreviewDesdePrimeraImagen, 100);
    }
});

function actualizarPreviewDesdePrimeraImagen() {
    const previewContainer = document.getElementById("preview-fotos");
    previewContainer.innerHTML = "";

    const primeraImagen = myDropzone.files.find(f => !f.toBeDeleted && f.type?.startsWith("image/"));
    if (!primeraImagen) return;

    const reader = new FileReader();
    reader.onload = function (e) {
        const img = document.createElement("img");
        img.src = e.target.result;
        img.className = "rounded shadow-sm border";
        img.style.width = "100%";
        img.style.maxWidth = "300px";
        previewContainer.appendChild(img);
    };
    reader.readAsDataURL(primeraImagen);
}

const previewUpdate = () => {
    document.getElementById("preview-nombre").textContent = document.getElementById("nombre").value || "(Nombre del Espacio)";
    document.getElementById("preview-costo").textContent = document.getElementById("costo").value || "0.00";
    document.getElementById("preview-descripcion").textContent = document.getElementById("descripcion").value || "(Descripción del espacio)";
    document.getElementById("preview-lugar").textContent = document.querySelector("#lugar option:checked")?.textContent || "-";
    document.getElementById("preview-estado").textContent = document.querySelector("#estado option:checked")?.textContent || "-";
    document.getElementById("preview-tipo").textContent = document.querySelector("#tipoEspacio option:checked")?.textContent || "-";

    const deportes = [...document.querySelectorAll("#deporte option:checked")].map(opt => opt.textContent);
    document.getElementById("preview-deportes").textContent = deportes.length > 0 ? deportes.join(", ") : "-";

    // Actualizar estado del botón después de cada cambio
    actualizarEstadoBoton();
};

// Función para validar todos los campos obligatorios
function validarFormulario() {
    const nombre = document.getElementById("nombre").value.trim();
    const costo = document.getElementById("costo").value.trim();
    const descripcion = document.getElementById("descripcion").value.trim();
    const lugar = document.getElementById("lugar").value;
    const estado = document.getElementById("estado").value;
    const tipoEspacio = document.getElementById("tipoEspacio").value;
    const deportes = [...document.querySelectorAll("#deporte option:checked")];

    const camposRequeridos = [
        { valor: nombre, nombre: "Nombre" },
        { valor: costo, nombre: "Costo" },
        { valor: descripcion, nombre: "Descripción" },
        { valor: lugar, nombre: "Lugar" },
        { valor: estado, nombre: "Estado" },
        { valor: tipoEspacio, nombre: "Tipo de Espacio" },
    ];

    // Verificar campos básicos
    for (let campo of camposRequeridos) {
        if (!campo.valor) {
            return { valido: false, mensaje: `El campo "${campo.nombre}" es obligatorio.` };
        }
    }

    // Verificar deportes (al menos uno seleccionado)
    if (deportes.length === 0) {
        return { valido: false, mensaje: "Debe seleccionar al menos un deporte." };
    }

    // Verificar que el costo sea un número válido mayor a 0
    const costoNum = parseFloat(costo);
    if (isNaN(costoNum) || costoNum <= 0) {
        return { valido: false, mensaje: "El costo debe ser un número mayor a 0." };
    }

    return { valido: true };
}

// Función para actualizar el estado del botón
function actualizarEstadoBoton() {
    const btn = document.getElementById("btnGuardarEspacio");
    const validacion = validarFormulario();

    if (validacion.valido) {
        btn.disabled = false;
        btn.classList.remove("btn-secondary");
        btn.classList.add("btn-primary");
        btn.textContent = "Crear Espacio";
    } else {
        btn.disabled = true;
        btn.classList.remove("btn-primary");
        btn.classList.add("btn-secondary");
        btn.textContent = "Completar formulario";
    }
}

// Función para mostrar spinner de carga
function mostrarSpinner(btn) {
    btn.disabled = true;
    btn.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        Creando espacio...
    `;
}

// Función para restaurar botón
function restaurarBoton(btn) {
    btn.disabled = false;
    btn.innerHTML = "Crear Espacio";
    btn.classList.remove("btn-secondary");
    btn.classList.add("btn-primary");
}

["nombre", "costo", "descripcion", "lugar", "estado", "tipoEspacio", "deporte"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener("input", previewUpdate);
    if (el && el.tagName === "SELECT") el.addEventListener("change", previewUpdate);
});

document.addEventListener("DOMContentLoaded", () => {
    previewUpdate();
    actualizarPreviewDesdePrimeraImagen();
    actualizarEstadoBoton(); // Verificar estado inicial del botón
});

document.getElementById("btnGuardarEspacio").addEventListener("click", async () => {
    const btn = document.getElementById("btnGuardarEspacio");

    // Validar formulario antes de proceder
    const validacion = validarFormulario();
    if (!validacion.valido) {
        Swal.fire({
            icon: 'warning',
            title: 'Formulario incompleto',
            text: validacion.mensaje,
            confirmButtonText: 'Entendido'
        });
        return;
    }

    // Obtener datos para confirmación
    const nombre = document.getElementById("nombre").value.trim();
    const lugarTexto = document.querySelector("#lugar option:checked")?.textContent || "";

    try {
        // Mostrar confirmación
        const resultado = await Swal.fire({
            icon: 'question',
            title: '¿Confirmar creación?',
            html: `¿Está seguro de crear el espacio deportivo <strong>"${nombre}"</strong> en <strong>"${lugarTexto}"</strong>?`,
            showCancelButton: true,
            confirmButtonText: 'Sí, crear espacio',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#696cff',
            cancelButtonColor: '#8592a3'
        });

        if (!resultado.isConfirmed) {
            return; // Usuario canceló
        }

        // Mostrar spinner de carga
        mostrarSpinner(btn);

        // Preparar datos del formulario
        const form = document.getElementById("form-info");
        const formData = new FormData(form);

        // Agregar archivos de Dropzone
        myDropzone.files.forEach(file => {
            if (file.status === Dropzone.ADDED || file.status === Dropzone.QUEUED) {
                const actualFile = file.upload?.blob || file;
                formData.append("file", actualFile);
            }
        });

        // Enviar petición
        const response = await fetch("/admin/agregar-servicios", {
            method: "POST",
            body: formData
        });

        const data = await response.json();

        if (data.success) {
            await Swal.fire({
                icon: 'success',
                title: '¡Espacio creado exitosamente!',
                text: data.message || `El espacio deportivo "${nombre}" fue registrado correctamente.`,
                confirmButtonText: 'Ver detalles',
                confirmButtonColor: '#696cff'
            });

            // Redirigir al detalle del espacio
            if (data.espacioId) {
                window.location.href = `/admin/detalles-espacio?id=${data.espacioId}`;
            } else {
                // Fallback si no se devuelve el ID
                window.location.href = "/admin/agregar-servicios?success";
            }
        } else {
            restaurarBoton(btn);
            Swal.fire({
                icon: 'error',
                title: 'Error al crear espacio',
                text: data.message || "Ocurrió un error al guardar el espacio deportivo.",
                confirmButtonText: 'Entendido'
            });
        }

    } catch (error) {
        console.error("❌ Error al enviar:", error);
        restaurarBoton(btn);
        Swal.fire({
            icon: 'error',
            title: 'Error de conexión',
            text: "Ocurrió un error inesperado. Por favor, intente nuevamente.",
            confirmButtonText: 'Entendido'
        });
    }
});