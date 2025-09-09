Dropzone.autoDiscover = false;

const idEspacio = document.getElementById("idEspacio").value;
const existingFiles = [];

const previewTemplate = `
  <div class="dz-preview dz-file-preview">
    <div class="dz-details">
      <div class="dz-thumbnail">
        <img data-dz-thumbnail>
      </div>
      <div class="dz-filename" data-dz-name></div>
      <div class="dz-size" data-dz-size></div>
    </div>
  </div>`;

const myDropzone = new Dropzone("#dropzone-multi", {
  url: `/admin/editar-espacio/${idEspacio}`,
  method: "post",
  autoProcessQueue: false,
  uploadMultiple: true,
  parallelUploads: 3,
  maxFiles: 3,
  acceptedFiles: "image/*",
  addRemoveLinks: true,
  paramName: "file",
  dictRemoveFile: "Quitar foto",
  dictInvalidFileType: "Solo se permiten archivos de imagen.",
  dictMaxFilesExceeded: "Ya se subieron 3 fotos",
  previewTemplate: previewTemplate,
  init: function () {
    const dz = this;

    const fotoUrls = [
      document.getElementById("foto1Url")?.value,
      document.getElementById("foto2Url")?.value,
      document.getElementById("foto3Url")?.value
    ];

    fotoUrls.forEach((url, idx) => {
      if (url) {
        const mockFile = {
          name: `foto${idx + 1}.jpg`,
          size: 123456,
          accepted: true,
          existing: true,
          fotoIndex: idx + 1,
          type: "image/jpeg"
        };
        dz.emit("addedfile", mockFile);
        dz.emit("thumbnail", mockFile, url);
        dz.emit("complete", mockFile);
        dz.files.push(mockFile);
        existingFiles.push(mockFile);
      }
    });

    // ⏳ Asegura que la vista previa se actualice tras renderizar las imágenes
    setTimeout(actualizarPreviewDesdePrimeraImagen, 100);

    dz.on("addedfile", function (file) {
      if (file.existing) return;

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
      }

      actualizarPreviewDesdePrimeraImagen();
      actualizarEstadoBoton(); // Actualizar estado del botón cuando se agreguen fotos
    });

    dz.on("removedfile", function (file) {
      if (file.existing) {
        file.toBeDeleted = true;
      }

      actualizarPreviewDesdePrimeraImagen();
      actualizarEstadoBoton(); // Actualizar estado del botón cuando se quiten fotos
    });
  }
});

function actualizarPreviewDesdePrimeraImagen() {
  const previewContainer = document.getElementById("preview-fotos");
  previewContainer.innerHTML = "";

  const imagen = myDropzone.files.find(f => !f.toBeDeleted && (f.type?.startsWith("image/") || f.existing));
  if (!imagen) return;

  const img = document.createElement("img");
  img.className = "rounded shadow-sm border";
  img.style.width = "100%";
  img.style.maxWidth = "300px";

  if (imagen.existing) {
    // Imagen mock ya cargada
    const idx = imagen.fotoIndex;
    const url = document.getElementById(`foto${idx}Url`)?.value;
    img.src = url;
    previewContainer.appendChild(img);
  } else {
    const reader = new FileReader();
    reader.onload = function (e) {
      img.src = e.target.result;
      previewContainer.appendChild(img);
    };
    reader.readAsDataURL(imagen);
  }
}

function previewUpdate() {
  // Verificar que cada elemento existe antes de modificar su textContent
  const previewNombre = document.getElementById("preview-nombre");
  if (previewNombre) previewNombre.textContent = document.getElementById("nombre")?.value || "(Nombre del Espacio)";

  const previewCosto = document.getElementById("preview-costo");
  if (previewCosto) previewCosto.textContent = document.getElementById("costo")?.value || "0.00";

  const previewDescripcion = document.getElementById("preview-descripcion");
  if (previewDescripcion) previewDescripcion.textContent = document.getElementById("descripcion")?.value || "(Descripción del espacio)";

  const previewLugar = document.getElementById("preview-lugar");
  if (previewLugar) previewLugar.textContent = document.querySelector("#lugar option:checked")?.textContent || "-";

  const previewEstado = document.getElementById("preview-estado");
  if (previewEstado) previewEstado.textContent = document.querySelector("#estado option:checked")?.textContent || "-";

  const previewTipo = document.getElementById("preview-tipo");
  if (previewTipo) previewTipo.textContent = document.querySelector("#tipoEspacio option:checked")?.textContent || "-";

  const previewDeportes = document.getElementById("preview-deportes");
  if (previewDeportes) {
    const deportes = [...document.querySelectorAll("#deporte option:checked")].map(opt => opt.textContent);
    previewDeportes.textContent = deportes.length > 0 ? deportes.join(", ") : "-";
  }

  // Actualizar estado del botón después de cada cambio
  actualizarEstadoBoton();
}

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

// Función para verificar si hay cambios en el formulario
function haycambios() {
  // Obtener valores originales (almacenados cuando se carga la página)
  const valoresOriginales = window.valoresOriginales || {};

  // Obtener valores actuales
  const valoresActuales = {
    nombre: document.getElementById("nombre").value.trim(),
    costo: parseFloat(document.getElementById("costo").value) || 0,
    descripcion: document.getElementById("descripcion").value.trim(),
    lugar: parseInt(document.getElementById("lugar").value) || 0,
    estado: parseInt(document.getElementById("estado").value) || 0,
    tipoEspacio: parseInt(document.getElementById("tipoEspacio").value) || 0,
    deportes: [...document.querySelectorAll("#deporte option:checked")].map(opt => opt.value).sort().join(',')
  };

  // Verificar cambios en campos básicos
  for (let campo in valoresActuales) {
    if (valoresActuales[campo] !== valoresOriginales[campo]) {
      return true;
    }
  }

  // Verificar cambios en archivos (fotos eliminadas o nuevas fotos agregadas)
  const fotosEliminadas = existingFiles.some(file => file.toBeDeleted);
  const fotosNuevas = myDropzone.files.some(file => !file.existing && !file.toBeDeleted);

  return fotosEliminadas || fotosNuevas;
}

// Función para actualizar el estado del botón
// Cambiar la función
function actualizarEstadoBoton() {
  const btn = document.getElementById("btnGuardarEspacio");
  const validacion = validarFormulario();
  const cambios = haycambios();

  if (validacion.valido && cambios) {
    btn.disabled = false;
    btn.classList.remove("btn-secondary");
    btn.classList.add("btn-primary");
    btn.textContent = "Guardar Cambios";
  } else if (!validacion.valido) {
    btn.disabled = true;
    btn.classList.remove("btn-primary");
    btn.classList.add("btn-secondary");
    btn.textContent = "Completar formulario";
  } else if (!cambios) {
    btn.disabled = true;
    btn.classList.remove("btn-primary");
    btn.classList.add("btn-secondary");
    btn.textContent = "Sin cambios";
  }
}

// Función para mostrar spinner de carga
function mostrarSpinner(btn) {
  btn.disabled = true;
  btn.innerHTML = `
    <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
    Guardando cambios...
  `;
}

// Función para restaurar botón
function restaurarBoton(btn) {
  btn.disabled = false;
  btn.innerHTML = "Guardar Cambios";
  btn.classList.remove("btn-secondary");
  btn.classList.add("btn-primary");
}

// Almacenar valores originales cuando se carga la página
function almacenarValoresOriginales() {
  window.valoresOriginales = {
    nombre: document.getElementById("nombre").value.trim(),
    costo: parseFloat(document.getElementById("costo").value) || 0,
    descripcion: document.getElementById("descripcion").value.trim(),
    lugar: parseInt(document.getElementById("lugar").value) || 0,
    estado: parseInt(document.getElementById("estado").value) || 0,
    tipoEspacio: parseInt(document.getElementById("tipoEspacio").value) || 0,
    deportes: [...document.querySelectorAll("#deporte option:checked")].map(opt => opt.value).sort().join(',')
  };
}

document.addEventListener("DOMContentLoaded", () => {
  previewUpdate();
  almacenarValoresOriginales();
  actualizarEstadoBoton(); // Verificar estado inicial del botón
});

["nombre", "costo", "descripcion", "lugar", "estado", "tipoEspacio", "deporte"].forEach(id => {
  const el = document.getElementById(id);
  if (el) el.addEventListener("input", previewUpdate);
  if (el && el.tagName === "SELECT") el.addEventListener("change", previewUpdate);
});

// Botón guardar mejorado
const btnGuardar = document.getElementById("btnGuardarEspacio");
btnGuardar.addEventListener("click", async () => {
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

  // Verificar si hay cambios
  if (!haycambios()) {
    Swal.fire({
      icon: 'info',
      title: 'Sin cambios',
      text: 'No se han detectado cambios en el formulario.',
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
      title: '¿Confirmar cambios?',
      html: `¿Está seguro de guardar los cambios del espacio deportivo <strong>"${nombre}"</strong> en <strong>"${lugarTexto}"</strong>?`,
      showCancelButton: true,
      confirmButtonText: 'Sí, guardar cambios',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#696cff',
      cancelButtonColor: '#8592a3'
    });

    if (!resultado.isConfirmed) {
      return; // Usuario canceló
    }

    // Mostrar spinner de carga
    mostrarSpinner(btnGuardar);

    // Preparar datos del formulario
    const formInfo = document.getElementById("form-info");
    const formData = new FormData(formInfo);

    // Marcar fotos a eliminar
    existingFiles.forEach((file) => {
      if (file.toBeDeleted) {
        formData.append(`deleteFile${file.fotoIndex}`, "true");
      }
    });

    // Agregar archivos nuevos
    myDropzone.files.forEach(file => {
      if (!file.toBeDeleted && !file.existing) {
        formData.append("newFiles", file);
      }
    });

    // Enviar petición
    const response = await fetch(`/admin/editar-espacio/${idEspacio}`, {
      method: "POST",
      body: formData
    });

    const data = await response.json();

    if (data.success) {
      await Swal.fire({
        icon: 'success',
        title: '¡Cambios guardados!',
        text: data.message || `Los cambios del espacio "${nombre}" se guardaron correctamente.`,
        confirmButtonText: 'Ver detalles',
        confirmButtonColor: '#696cff'
      });

      // Redirigir al detalle del espacio
      window.location.href = `/admin/detalles-espacio?id=${idEspacio}`;
    } else {
      restaurarBoton(btnGuardar);
      Swal.fire({
        icon: 'error',
        title: 'Error al guardar',
        text: data.message || "Ocurrió un error al guardar los cambios.",
        confirmButtonText: 'Entendido'
      });
    }

  } catch (error) {
    console.error("❌ Error al enviar:", error);
    restaurarBoton(btnGuardar);
    Swal.fire({
      icon: 'error',
      title: 'Error de conexión',
      text: "Ocurrió un error inesperado. Por favor, intente nuevamente.",
      confirmButtonText: 'Entendido'
    });
  }
});