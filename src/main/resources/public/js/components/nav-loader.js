document.addEventListener('DOMContentLoaded', async function () {
    try {
        const response = await fetch('/partials/nav.html');
        if (!response.ok) throw new Error('No se pudo cargar nav.html');

        const data = await response.text();
        const navContainer = document.createElement('div');
        navContainer.innerHTML = data;
        document.body.prepend(navContainer);

        // Inicializar menú hamburguesa
        const hamburgerBtn = document.querySelector('.hamburger-btn');
        const sidebarMenu = document.querySelector('.sidebar-menu');
        const overlay = document.querySelector('.sidebar-overlay');

        const toggleMenu = () => {
            sidebarMenu.classList.toggle('active');
            overlay.classList.toggle('active');
            hamburgerBtn.classList.toggle('active');
        };

        if (hamburgerBtn && overlay) {
            hamburgerBtn.addEventListener('click', toggleMenu);
            overlay.addEventListener('click', toggleMenu);
        }

        let usuario = null;

        // Obtener información del usuario solo si hay token
        if (document.cookie.includes("token=")) {
            try {
                const userResponse = await fetch('/api/yo', { credentials: 'include' });
                if (userResponse.ok) {
                    usuario = await userResponse.json();
                    console.log("🔐 Usuario logueado:", usuario);
                }
            } catch (e) {
                console.warn("⚠️ No autenticado:", e);
            }
        }

        // Mostrar u ocultar secciones según rol
        const adminLinks = document.querySelectorAll('.admin-only');
        adminLinks.forEach(link => {
            link.style.display = (usuario && usuario.esAdmin) ? 'block' : 'none';
        });

        // Mostrar nombre de usuario
        const usernameSpan = document.querySelector('.username');
        if (usernameSpan && usuario) {
            usernameSpan.textContent = usuario.username;
        }

        // Mostrar/ocultar botones Login y Logout
        const logoutBtn = document.querySelector('.logout-btn');
        const loginBtn = document.querySelector('.login-btn');

        if (usuario) {
            if (logoutBtn) logoutBtn.style.display = 'inline-block';
            if (loginBtn) loginBtn.style.display = 'none';
        } else {
            if (logoutBtn) logoutBtn.style.display = 'none';
            if (loginBtn) loginBtn.style.display = 'inline-block';
        }

        // 🧹 Logout
        if (logoutBtn && usuario) {
            logoutBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                try {
                    const res = await fetch('/logout', {
                        method: 'POST',
                        credentials: 'include'
                    });

                    // 🔥 Eliminar cookie manualmente en frontend
                    document.cookie = "token=; Max-Age=0; path=/";

                    if (res.ok) {
                        console.log("✅ Logout confirmado");
                        // Pequeña espera para asegurar sync con backend y ServiceWorker
                        setTimeout(() => {
                            window.location.href = '/index';
                        }, 150);
                    } else {
                        alert('No se pudo cerrar sesión correctamente.');
                    }
                } catch (err) {
                    console.error('Error al cerrar sesión:', err);
                }
            });
        }

    } catch (error) {
        console.error('🛑 Error cargando el menú de navegación:', error);
    }
});
