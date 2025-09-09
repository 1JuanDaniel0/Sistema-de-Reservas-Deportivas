const dniInput = document.getElementById("dni");

// Bloquear letras al escribir
dniInput.addEventListener("keypress", e => {
    if (!/[0-9]/.test(e.key)) e.preventDefault();
});

// Bloquear letras al pegar
dniInput.addEventListener("paste", e => {
    const paste = (e.clipboardData || window.clipboardData).getData("text");
    if (!/^\d{1,8}$/.test(paste)) e.preventDefault();
});

// Validación en vivo
dniInput.addEventListener("input", () => {
    dniInput.value = dniInput.value.replace(/\D/g, '').slice(0, 8);
    document.getElementById("dni-validation-msg").classList.toggle("d-none", dniInput.value.length === 8);
    const dniError = document.getElementById("dni-error");
    if (!dniError.classList.contains("d-none")) {
        dniError.classList.add("d-none");
        dniError.textContent = "";
    }
});

window.addEventListener("load", () => {
    if (localStorage.getItem("registroBloqueadoHasta")) {
        mostrarBloqueoTemporal(); // usará la marca existente
    }
});

document.getElementById("btnContinuar").addEventListener("click", () => {
    const btn = document.getElementById("btnContinuar");
    const dni = dniInput.value.trim();
    const terms = document.getElementById("terms-conditions");
    const dniError = document.getElementById("dni-error");
    const termsError = document.getElementById("terms-error");

    dniError.classList.add("d-none");
    termsError.classList.add("d-none");

    if (!terms.checked) {
        termsError.classList.remove("d-none");
        return;
    }

    if (dni.length !== 8) {
        document.getElementById("dni-validation-msg").classList.remove("d-none");
        return;
    }

    btn.disabled = true;
    const originalText = btn.textContent;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>&nbsp;Consultando...`;

    fetch('/registro/verificar-dni?dni=' + dni, { credentials: 'include' })
        .then(res => res.json())
        .then(data => {
            if (data.existe) {
                dniError.textContent = "El DNI ya está registrado.";
                dniError.classList.remove("d-none");
                btn.disabled = false;
                btn.innerHTML = originalText;
                return;
            }

            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
            const formData = new URLSearchParams();
            formData.append("dni", dni);

            fetch('/registro/consultar-dni-api', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    [csrfHeader]: csrfToken
                },
                body: formData
            })
                .then(async res => {
                    if (res.status === 429) {
                        Swal.fire({
                            icon: 'warning',
                            title: 'Demasiadas consultas',
                            text: 'Has superado el límite de intentos. Intenta nuevamente en unos minutos.'
                        });
                        mostrarBloqueoTemporal();
                        return;
                    }
                    const datos = await res.json();
                    if (datos.status === "ok") {
                        const uuid = datos.uuid;
                        fetch('/registro/datos-session?uuid=' + uuid, { credentials: 'include' })
                            .then(res => res.json())
                            .then(data => {
                                if (!data.nombres || !data.apellidos) {
                                    alert("⚠️ No se encontraron datos válidos del DNI.");
                                    btn.disabled = false;
                                    btn.innerHTML = originalText;
                                    return;
                                }

                                // ✅ Mostrar paso 2 normalmente
                                document.getElementById("registro-imagen").classList.replace("col-xl-7", "col-xl-5");
                                document.getElementById("registro-formulario").classList.replace("col-xl-5", "col-xl-7");
                                document.getElementById("registro-contenido").classList.replace("w-px-400", "w-px-600");

                                document.getElementById("nombres").value = data.nombres;
                                document.getElementById("apellidos").value = data.apellidos;

                                document.getElementById("dni-section").style.display = 'none';
                                dniInput.removeAttribute('required');
                                dniInput.disabled = true;
                                document.getElementById("dni-hidden").value = dni;

                                document.getElementById("form-fields").style.display = 'block';
                                activarAdvertenciaSalida();

                                document.querySelectorAll("#form-fields .row").forEach((row, index) => {
                                    row.style.opacity = 0;
                                    row.style.transform = 'scale(0.9)';
                                    setTimeout(() => {
                                        row.style.transition = 'all 0.3s ease';
                                        row.style.opacity = 1;
                                        row.style.transform = 'scale(1)';
                                    }, 150 * index);
                                });

                                setupLiveValidation();
                                setupTogglePassword();
                                setupPasswordStrengthValidation();
                            });

                    } else if (datos.status === "bloqueado") {
                        mostrarBloqueoTemporal(); // arranca / retoma contador
                    } else {
                        alert("⚠️ No se pudo obtener información del DNI. Verifica el número o intenta más tarde.");
                    }
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                })
                .catch(err => {
                    alert("❌ Error inesperado al consultar DNI.");
                    console.error(err);
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                });

        });
});

function setupLiveValidation() {
    const correo = document.getElementById("correo");
    const contrasena = document.getElementById("contrasena");
    const confirmContrasena = document.getElementById("confirmContrasena");
    const correoError = document.getElementById("correo-error");

    // Validación en vivo de correo
    correo.addEventListener("input", () => {
        correo.setCustomValidity(
            /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.value) ? "" : "Correo inválido"
        );
    });

    correo.addEventListener("blur", () => {
        const correoVal = correo.value.trim();
        if (!correoVal) return;

        fetch(`/registro/verificar-correo?correo=${encodeURIComponent(correoVal)}`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
                if (data.existe) {
                    correo.setCustomValidity("Este correo ya está registrado.");
                    correo.classList.add("is-invalid");
                    correoError.textContent = "Este correo ya está registrado.";
                    correoError.classList.remove("d-none");
                    mostrarAnimacionError(correo);
                } else {
                    correo.setCustomValidity("");
                    correo.classList.remove("is-invalid");
                    correoError.classList.add("d-none");
                }
            });
    });

    setupTelefonoValidation();
}

function normalizarTelefono(raw) {
    let limpio = raw.replace(/\D/g, '');
    if (limpio.startsWith('51') && limpio.length === 11) {
        limpio = limpio.slice(2);
    }
    return limpio;
}

function formatearTelefono(telefono) {
    if (telefono.length !== 9) return telefono;
    return `+51 ${telefono.slice(0, 3)} ${telefono.slice(3, 6)} ${telefono.slice(6)}`;
}

function setupTelefonoValidation() {
    const telefono = document.getElementById("telefono");
    const telefonoError = document.getElementById("telefono-error");

    telefono.addEventListener("input", () => {
        let limpio = telefono.value.replace(/\D/g, '');

        if (limpio.startsWith('51')) limpio = limpio.slice(2);
        if (limpio.length > 9) limpio = limpio.slice(0, 9);

        telefono.value = limpio; // temporal sin formateo
        telefono.setCustomValidity("");
        telefono.classList.remove("is-invalid");
        telefonoError.classList.add("d-none");
    });

    telefono.addEventListener("blur", () => {
        const limpio = normalizarTelefono(telefono.value);

        if (!limpio) {
            telefono.setCustomValidity("");
            telefono.classList.remove("is-invalid");
            telefonoError.classList.add("d-none");
            return;
        }

        if (!/^[9]\d{8}$/.test(limpio)) {
            telefono.setCustomValidity("Formato inválido. Debe ser 9 dígitos iniciando con 9.");
            telefono.classList.add("is-invalid");
            telefonoError.textContent = "Ej: 912345678 o +51 912345678";
            telefonoError.classList.remove("d-none");
            mostrarAnimacionError(telefono);
            return;
        }

        // Formatear visualmente
        telefono.value = formatearTelefono(limpio);

        // Verificar duplicado
        fetch(`/registro/verificar-telefono?telefono=${encodeURIComponent(limpio)}`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
                if (data.existe) {
                    telefono.setCustomValidity("Este número ya está registrado.");
                    telefono.classList.add("is-invalid");
                    telefonoError.textContent = "Este número ya está registrado.";
                    telefonoError.classList.remove("d-none");
                    mostrarAnimacionError(telefono);
                } else {
                    telefono.setCustomValidity("");
                    telefono.classList.remove("is-invalid");
                    telefonoError.classList.add("d-none");
                }
            });
    });
}

document.getElementById("formRegistro").addEventListener("submit", function (e) {
    desactivarAdvertenciaSalida();

    const btn = document.getElementById("btnRegistrarme");
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>&nbsp;Registrando...`;
    }
});

let advertenciaActiva = false;

function activarAdvertenciaSalida() {
    if (!advertenciaActiva) {
        window.addEventListener("beforeunload", mostrarAdvertencia);
        advertenciaActiva = true;
    }
}

function desactivarAdvertenciaSalida() {
    window.removeEventListener("beforeunload", mostrarAdvertencia);
    advertenciaActiva = false;
}

function mostrarAdvertencia(e) {
    e.preventDefault();
    let hayError = false;

    const campos = [
        { id: "correo", ayuda: "Correo inválido" },
        { id: "telefono", ayuda: "Teléfono inválido" },
        { id: "contrasena", ayuda: "La contraseña es obligatoria." },
        { id: "confirmContrasena", ayuda: "Confirma tu contraseña." }
    ];

    campos.forEach(campo => {
        const input = document.getElementById(campo.id);
        const help = document.getElementById(campo.id === "confirmContrasena" ? "confirmPasswordHelp" : "passwordHelp");

        if (!input.checkValidity() || (campo.id === "confirmContrasena" && input.value !== document.getElementById("contrasena").value)) {
            input.classList.add("is-invalid");
            input.classList.add("shake");
            if (help) {
                help.style.display = "block";
                help.innerText = campo.ayuda;
            }
            hayError = true;

            setTimeout(() => input.classList.remove("shake"), 500);
        } else {
            input.classList.remove("is-invalid");
            if (help) help.style.display = "none";
        }
    });

    if (hayError) return;

    e.returnValue = '';
}

function mostrarAnimacionError(input) {
    input.classList.add("shake");
    setTimeout(() => input.classList.remove("shake"), 500);
}

function setupPasswordStrengthValidation() {
    const passwordInput = document.getElementById('contrasena');
    const confirmInput = document.getElementById('confirmContrasena');
    const passwordHelp = document.getElementById('passwordHelp');
    const confirmHelp = document.getElementById('confirmPasswordHelp');
    const strengthBar = document.getElementById('passwordStrengthBar');

    function evaluateStrength(pwd) {
        let score = 0;
        if (pwd.length >= 8) score++;
        if (/[a-z]/.test(pwd)) score++;
        if (/[A-Z]/.test(pwd)) score++;
        if (/\d/.test(pwd)) score++;
        if (/[\W_]/.test(pwd)) score++;
        return score;
    }

    function validatePassword() {
        const pwd = passwordInput.value;
        const strength = evaluateStrength(pwd);
        const percent = (strength / 5) * 100;
        strengthBar.style.width = percent + "%";

        if (strength <= 2) {
            strengthBar.style.backgroundColor = 'red';
        } else if (strength <= 4) {
            strengthBar.style.backgroundColor = 'orange';
        } else {
            strengthBar.style.backgroundColor = 'green';
        }

        if (pwd.length === 0) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'La contraseña es obligatoria.';
        } else if (pwd.length < 8) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'Debe tener mínimo 8 caracteres.';
        } else if (!/[a-z]/.test(pwd) || (!/\d/.test(pwd) && !/[\W_]/.test(pwd))) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'Debe tener al menos una minúscula y un número o símbolo.';
        } else {
            passwordHelp.style.display = 'none';
        }
    }

    function validateConfirmPassword() {
        const pwd = passwordInput.value;
        const confirm = confirmInput.value;

        if (confirm.length === 0) {
            confirmHelp.style.display = 'block';
            confirmHelp.innerText = 'Confirma tu contraseña.';
        } else if (pwd !== confirm) {
            confirmHelp.style.display = 'block';
            confirmHelp.innerText = 'Las contraseñas no coinciden.';
        } else {
            confirmHelp.style.display = 'none';
        }
    }

    passwordInput.addEventListener('input', () => {
        validatePassword();
        validateConfirmPassword();
    });

    confirmInput.addEventListener('input', validateConfirmPassword);
}

function mostrarBloqueoTemporal(duracionMs = 5 * 60 * 1000) {
    const msg = document.getElementById("bloqueo-msg");
    const timer = document.getElementById("bloqueo-timer");
    const aviso = document.getElementById("aviso-consulta");

    aviso?.classList.add("d-none"); // Oculta aviso informativo

    msg.classList.remove("d-none");

    const bloqueoKey = "registroBloqueadoHasta";
    const ahora = Date.now();
    // Calcula o reutiliza la marca de fin guardada
    const bloqueoHasta = localStorage.getItem(bloqueoKey)
        ? parseInt(localStorage.getItem(bloqueoKey), 10)
        : (ahora + duracionMs);

    // Guarda el timer si ya se inició
    localStorage.setItem(bloqueoKey, bloqueoHasta);

    const intervalo = setInterval(() => {
        const restante = bloqueoHasta - Date.now();
        if (restante <= 0) {
            clearInterval(intervalo);
            msg.classList.add("d-none");
            localStorage.removeItem(bloqueoKey);
            aviso?.classList.remove("d-none"); // Muestra nuevamente el aviso
            return;
        }

        const m = Math.floor(restante / 60000);
        const s = Math.floor((restante % 60000) / 1000).toString().padStart(2, '0');
        timer.textContent = `${m}:${s}`;
    }, 1000);
}

window.initValidacionReestablecer = function () {
    const passwordInput = document.getElementById('contrasena');
    const confirmInput = document.getElementById('confirmContrasena');
    const passwordHelp = document.getElementById('passwordHelp');
    const confirmHelp = document.getElementById('confirmPasswordHelp');
    const strengthBar = document.getElementById('passwordStrengthBar');

    function evaluateStrength(pwd) {
        let score = 0;
        if (pwd.length >= 8) score++;
        if (/[a-z]/.test(pwd)) score++;
        if (/[A-Z]/.test(pwd)) score++;
        if (/\d/.test(pwd)) score++;
        if (/[\W_]/.test(pwd)) score++;
        return score;
    }

    function validatePassword() {
        const pwd = passwordInput.value;
        const strength = evaluateStrength(pwd);
        const percent = (strength / 5) * 100;
        strengthBar.style.width = percent + "%";

        if (strength <= 2) {
            strengthBar.style.backgroundColor = 'red';
        } else if (strength <= 4) {
            strengthBar.style.backgroundColor = 'orange';
        } else {
            strengthBar.style.backgroundColor = 'green';
        }

        if (pwd.length === 0) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'La contraseña es obligatoria.';
        } else if (pwd.length < 8) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'Debe tener mínimo 8 caracteres.';
        } else if (!/[a-z]/.test(pwd)) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'Debe tener al menos una minúscula.';
        } else if (!/\d/.test(pwd) && !/[\W_]/.test(pwd)) {
            passwordHelp.style.display = 'block';
            passwordHelp.innerText = 'Debe incluir al menos un número o un símbolo.';
        } else {
            passwordHelp.style.display = 'none';
        }
    }

    function validateConfirmPassword() {
        const pwd = passwordInput.value;
        const confirm = confirmInput.value;

        if (confirm.length === 0) {
            confirmHelp.style.display = 'block';
            confirmHelp.innerText = 'Confirma tu contraseña.';
        } else if (pwd !== confirm) {
            confirmHelp.style.display = 'block';
            confirmHelp.innerText = 'Las contraseñas no coinciden.';
        } else {
            confirmHelp.style.display = 'none';
        }
    }

    passwordInput.addEventListener('input', () => {
        validatePassword();
        validateConfirmPassword();
    });

    confirmInput.addEventListener('input', validateConfirmPassword);
}

