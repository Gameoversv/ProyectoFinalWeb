// mis-urlsnapsadmin.js
document.addEventListener("DOMContentLoaded", async () => {
    const urlsList = document.getElementById("urls-list");

    try {
        // Verifica si el usuario es administrador
        const userRes = await fetch("/api/yo");
        if (!userRes.ok) throw new Error("No se pudo verificar el usuario");

        const usuario = await userRes.json();
        if (!usuario.esAdmin) {
            urlsList.innerHTML = "<p class='error-message'>Acceso no autorizado.</p>";
            return;
        }

        // Carga todas las URLs
        const res = await fetch("/api/todas-las-urls");
        if (!res.ok) throw new Error("No se pudieron obtener las URLs");

        const urls = await res.json();
        urlsList.innerHTML = "";

        if (!Array.isArray(urls) || urls.length === 0) {
            urlsList.innerHTML = "<p class='empty-message'>No hay URLs registradas.</p>";
            return;
        }

        // Renderiza cada URL
        urls.forEach(url => {
            const row = document.createElement("div");
            row.className = "url-row";

            const fecha = new Date(url.createdAt || url.fechaCreacion);
            const fechaFormateada = !isNaN(fecha.getTime())
                ? fecha.toLocaleDateString()
                : "Fecha inválida";

            row.innerHTML = `
        <div class="url-item original">${url.originalUrl}</div>
        <div class="url-item snap">
          <a href="/${url.shortCode}" target="_blank">/${url.shortCode}</a>
        </div>
        <div class="url-item fecha">${fechaFormateada}</div>
        <div class="url-item usuario">${url.username || "Anónimo"}</div>
        <div class="url-item opciones">
          <button class="delete-btn" data-shortcode="${url.shortCode}">Eliminar</button>
        </div>
      `;

            urlsList.appendChild(row);
        });

        // Delegación para botón eliminar
        urlsList.addEventListener("click", async (e) => {
            if (e.target.classList.contains("delete-btn")) {
                const shortCode = e.target.dataset.shortcode;
                if (!confirm("¿Seguro que deseas eliminar esta URL?")) return;

                const delRes = await fetch(`/api/url/${shortCode}`, {
                    method: "DELETE",
                });

                if (delRes.ok) {
                    e.target.closest(".url-row").remove();
                } else {
                    alert("Error al eliminar la URL.");
                }
            }
        });

    } catch (error) {
        console.error("🛑 Error al cargar URLs de administrador:", error);
        urlsList.innerHTML = "<p class='error-message'>Error al cargar las URLs.</p>";
    }
});
