"use strict";

document.addEventListener("DOMContentLoaded", function () {

    const formDatos = document.querySelector("#editUserForm");
    const formFoto = document.querySelector("#dropzone-basic");
    const botonUnico = document.querySelector("#btnActualizarPerfil");
    const spinner = document.getElementById("spinnerEditar");
    const textoBoton = document.getElementById("textoEditar");

    // 🔐 CSRF tokens desde las <meta> tags
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute("content");

    // Variable global para controlar el estado del dropzone
    let dropzoneInstance = null;
    let teniaFotoInicial = window.fotoPerfilActual && window.fotoPerfilActual !== 'null' && window.fotoPerfilActual.trim() !== '';

    console.log("🔍 Estado inicial - Tenía foto:", teniaFotoInicial, "URL:", window.fotoPerfilActual);

    // 🖼 Dropzone inicialización con vista previa
    if (formFoto) {
        const previewTemplate = `
      <div class="dz-preview dz-file-preview">
        <div class="dz-details">
          <div class="dz-thumbnail">
            <img data-dz-thumbnail style="width: 120px; height: 120px; object-fit: cover;" />
            <span class="dz-nopreview">No preview</span>
            <div class="dz-success-mark"><i class="bx bx-check"></i></div>
            <div class="dz-error-mark"><i class="bx bx-error"></i></div>
            <div class="dz-error-message"><span data-dz-errormessage></span></div>
            <div class="progress mt-2">
              <div class="progress-bar progress-bar-primary" role="progressbar"
                   aria-valuemin="0" aria-valuemax="100" data-dz-uploadprogress></div>
            </div>
          </div>
          <div class="dz-filename" data-dz-name></div>
          <div class="dz-size" data-dz-size></div>
        </div>
      </div>`;

        dropzoneInstance = new Dropzone(formFoto, {
            url: "/fake-url", // nunca se usa
            autoProcessQueue: false,
            maxFiles: 1,
            maxFilesize: 2, // MB
            acceptedFiles: ".jpg,.jpeg,.png,.webp",
            previewTemplate: previewTemplate,
            addRemoveLinks: true,
            dictDefaultMessage: "Arrastra tu imagen aquí",
            dictInvalidFileType: "Tipo de archivo no permitido (solo JPG, PNG o WEBP)",
            dictFileTooBig: "Archivo demasiado grande (máx. 2MB)",
            init: function () {
                const dzInstance = this;

                // 🟢 Precargar imagen si existe
                if (teniaFotoInicial) {
                    const mockFile = {
                        name: "Foto actual",
                        size: 123456,
                        type: "image/jpeg",
                        status: Dropzone.SUCCESS
                    };

                    // Agregar el archivo mock al dropzone
                    dzInstance.emit("addedfile", mockFile);

                    // Mostrar la miniatura con la URL completa
                    dzInstance.emit("thumbnail", mockFile, window.fotoPerfilActual);

                    // Marcar como completado
                    dzInstance.emit("complete", mockFile);

                    // Agregar clases de éxito
                    if (mockFile.previewElement) {
                        mockFile.previewElement.classList.add("dz-success");
                        mockFile.previewElement.classList.add("dz-complete");
                    }

                    // Agregar al array de archivos
                    dzInstance.files.push(mockFile);

                    // Marcar como archivo existente para no enviarlo
                    mockFile.isExisting = true;
                }

                this.on("addedfile", file => {
                    console.log("🟢 Archivo añadido:", file.name, "isExisting:", file.isExisting);

                    // Si es un archivo nuevo y ya hay una imagen existente, remover la anterior
                    if (!file.isExisting && dzInstance.files.length > 1) {
                        const existingFile = dzInstance.files.find(f => f.isExisting);
                        if (existingFile) {
                            console.log("🗑️ Removiendo archivo existente para reemplazar");
                            dzInstance.removeFile(existingFile);
                        }
                    }
                });

                this.on("error", function (file, message) {
                    this.removeFile(file);
                    Swal.fire({
                        icon: "error",
                        title: "Error al subir imagen",
                        text: typeof message === "string" ? message : "Archivo no válido. Solo JPG, PNG, WEBP"
                    });
                });

                this.on("removedfile", function(file) {
                    console.log("🗑️ Archivo eliminado del dropzone:", file.name, "isExisting:", file.isExisting);
                });

                this.on("success", () => {
                    console.log("✅ Imagen preparada");
                });
            }
        });
    }

    // 🚀 Botón Editar - Envío combinado
    if (formDatos && formFoto && botonUnico) {
        botonUnico.addEventListener("click", function (e) {
            e.preventDefault();

            // 🔁 Bloqueo visual
            botonUnico.disabled = true;
            spinner.classList.remove("d-none");
            textoBoton.textContent = "Actualizando perfil...";

            // 📦 Datos
            const formData = new FormData();
            formData.append("correo", formDatos.querySelector("input[name='correo']").value);
            formData.append("telefono", formDatos.querySelector("input[name='telefono']").value);

            // 📁 Análisis del estado de la foto
            let hayArchivoNuevo = false;
            let seEliminoFoto = false;

            console.log("📊 Análisis del dropzone:");
            console.log("- Archivos en dropzone:", dropzoneInstance?.files.length || 0);
            console.log("- Tenía foto inicial:", teniaFotoInicial);

            if (dropzoneInstance && dropzoneInstance.files.length > 0) {
                const archivo = dropzoneInstance.files[0];
                console.log("- Archivo encontrado:", archivo.name, "isExisting:", archivo.isExisting);

                if (!archivo.isExisting) {
                    // Es un archivo nuevo
                    formData.append("fotoPerfil", archivo);
                    hayArchivoNuevo = true;
                    console.log("📤 Enviando archivo nuevo");
                }
            } else {
                // No hay archivos en dropzone
                if (teniaFotoInicial) {
                    // Tenía foto pero ya no hay nada = eliminó la foto
                    formData.append("eliminarFoto", "true");
                    seEliminoFoto = true;
                    console.log("🗑️ Marcando para eliminar foto");
                }
            }

            console.log("📋 Resumen:", { hayArchivoNuevo, seEliminoFoto });

            // 🚀 Envío con CSRF
            fetch("/coordinador/actualizar-perfil-completo", {
                method: "POST",
                headers: {
                    [csrfHeader]: csrfToken
                },
                body: formData
            })
                .then(res => {
                    console.log("📡 Respuesta del servidor:", res.status, res.statusText);

                    // Verificar si la respuesta es JSON o HTML
                    const contentType = res.headers.get("content-type");
                    if (contentType && contentType.includes("application/json")) {
                        return res.json();
                    } else {
                        // Si es HTML, probablemente es un redirect o error del servidor
                        if (res.ok) {
                            // Probablemente exitoso pero devolvió HTML (redirect)
                            return { success: true, message: "Perfil actualizado correctamente" };
                        } else {
                            throw new Error(`Error del servidor: ${res.status} ${res.statusText}`);
                        }
                    }
                })
                .then(data => {
                    console.log("📋 Datos recibidos del servidor:", data);

                    // Verificar específicamente el campo success
                    if (data.success === true) {
                        // Éxito
                        let mensaje = data.message || "Perfil actualizado correctamente";

                        Swal.fire({
                            icon: "success",
                            title: "¡Éxito!",
                            text: mensaje,
                            showConfirmButton: false,
                            timer: 1500
                        }).then(() => {
                            // Cerrar modal
                            const modal = bootstrap.Modal.getInstance(document.getElementById('editUser'));
                            if (modal) {
                                modal.hide();
                            }

                            // Actualizar la interfaz si es necesario
                            if (data.nuevaFotoUrl === null) {
                                console.log("🗑️ Foto eliminada, se recargará la página");
                            } else if (data.nuevaFotoUrl) {
                                console.log("📸 Nueva foto URL:", data.nuevaFotoUrl);
                            }

                            // Recargar página para mostrar cambios
                            setTimeout(() => {
                                location.reload();
                            }, 300);
                        });
                    } else {
                        // Error del backend
                        Swal.fire({
                            icon: "error",
                            title: "Error",
                            text: data.message || "No se pudo actualizar el perfil"
                        });
                    }
                })
                .catch(err => {
                    console.error("❌ Error en la petición:", err);
                    Swal.fire({
                        icon: "error",
                        title: "Error de conexión",
                        text: "No se pudo conectar con el servidor. Inténtalo de nuevo."
                    });
                })
                .finally(() => {
                    // Restaurar botón
                    botonUnico.disabled = false;
                    spinner.classList.add("d-none");
                    textoBoton.textContent = "Editar";
                });
        });
    }
});