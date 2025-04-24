// login.js
const formsWrapper = document.querySelector('.forms-wrapper');
const showRegister = document.getElementById('show-register');
const showLogin = document.getElementById('show-login');

// Toggle entre formularios
showRegister.addEventListener('click', (e) => {
    e.preventDefault();
    formsWrapper.classList.add('active');
});

showLogin.addEventListener('click', (e) => {
    e.preventDefault();
    formsWrapper.classList.remove('active');
});

// Manejo de envío de formularios
document.querySelector('.login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);

    try {
        const response = await fetch('/login', {
            method: 'POST',
            body: formData
        });

        if(response.ok) {
            window.location.href = '/index';
        } else {
            alert('Error en el login');
        }
    } catch (error) {
        console.error('Error:', error);
    }
});

document.querySelector('.register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);

    try {
        const response = await fetch('/register', {
            method: 'POST',
            body: formData
        });

        if(response.ok) {
            alert('Registro exitoso! Por favor inicia sesión');
            formsWrapper.classList.remove('active');
        } else {
            alert('Error en el registro');
        }
    } catch (error) {
        console.error('Error:', error);
    }
});