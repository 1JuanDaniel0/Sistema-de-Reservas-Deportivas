function initValidacionReestablecer() {
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

    function isPasswordValid(pwd) {
        return (
            pwd.length >= 8 &&
            /[a-z]/.test(pwd) &&
            (/\d/.test(pwd) || /[\W_]/.test(pwd))
        );
    }

    function validatePassword() {
        const pwd = passwordInput.value;
        const strength = evaluateStrength(pwd);
        strengthBar.style.width = (strength / 5) * 100 + "%";

        if (strength <= 2) {
            strengthBar.style.backgroundColor = 'red';
        } else if (strength <= 4) {
            strengthBar.style.backgroundColor = 'orange';
        } else {
            strengthBar.style.backgroundColor = 'green';
        }

        // Mostrar mensajes
        if (pwd.length === 0) {
            passwordHelp.innerText = 'La contraseña es obligatoria.';
            passwordHelp.style.display = 'block';
            return false;
        } else if (pwd.length < 8) {
            passwordHelp.innerText = 'Debe tener mínimo 8 caracteres.';
            passwordHelp.style.display = 'block';
            return false;
        } else if (!/[a-z]/.test(pwd)) {
            passwordHelp.innerText = 'Debe tener al menos una minúscula.';
            passwordHelp.style.display = 'block';
            return false;
        } else if (!/\d/.test(pwd) && !/[\W_]/.test(pwd)) {
            passwordHelp.innerText = 'Debe incluir al menos un número o un símbolo.';
            passwordHelp.style.display = 'block';
            return false;
        }

        passwordHelp.style.display = 'none';
        return true;
    }

    function validateConfirmPassword() {
        const pwd = passwordInput.value;
        const confirm = confirmInput.value;

        // Solo comparar si la contraseña principal es válida
        if (!isPasswordValid(pwd)) {
            confirmHelp.style.display = 'none';
            return;
        }

        if (confirm.length === 0) {
            confirmHelp.innerText = 'Confirma tu contraseña.';
            confirmHelp.style.display = 'block';
        } else if (pwd !== confirm) {
            confirmHelp.innerText = 'Las contraseñas no coinciden.';
            confirmHelp.style.display = 'block';
        } else {
            confirmHelp.style.display = 'none';
        }
    }

    passwordInput.addEventListener('input', () => {
        const esValida = validatePassword();
        if (esValida) validateConfirmPassword(); // Solo si pasa validación
    });

    confirmInput.addEventListener('input', validateConfirmPassword);
}
