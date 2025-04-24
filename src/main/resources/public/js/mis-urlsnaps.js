// public/js/mis-urlsnaps.js

document.addEventListener("DOMContentLoaded", async () => {
    const urlsList = document.getElementById("urls-list");

    // 2) Función para renderizar la lista de URLs
    function renderUrls(urls) {
        urlsList.innerHTML = "";
        if (!urls.length) {
            urlsList.innerHTML = "<p class='empty-message'>No has creado ninguna URL.</p>";
            return;
        }
        urls.forEach(url => {
            const row = document.createElement("div");          // ← aquí
            row.className = "url-row";
            const fecha = new Date(url.createdAt || url.fechaCreacion);
            const fechaForm = isNaN(fecha.getTime())
                ? "Desconocida"
                : fecha.toISOString().split("T")[0];

            row.innerHTML = `
              <div class="url-item original">${url.originalUrl}</div>
              <div class="url-item snap"><a href="/${url.shortCode}" target="_blank">/${url.shortCode}</a></div>
              <div class="url-item fecha">${fechaForm}</div>
              <div class="url-item opciones">
                <button class="delete-btn" data-shortcode="${url.shortCode}">Eliminar</button>
              </div>`;
            urlsList.appendChild(row);
        });
    }

    // 3) Cargar URLs (fetch → SW cache-first → fallback automático)
    async function loadUrls() {
        try {
            const res = await fetch("/api/mis-urlsnaps", { credentials: "include" });
            if (res.status === 401) {
                // No autenticado: redirigir o mostrar mensaje
                console.error("No autenticado");
                return;
            }
            if (!res.ok) {
                console.error("Error cargando URLs:", res.status);
                return;
            }
            const urls = await res.json();
            renderUrls(urls);
        } catch (err) {
            console.warn("Fetch falló, intentando desde cache...", err);
            const cacheRes = await caches.match("/api/mis-urlsnaps");
            if (cacheRes) {
                const urls = await cacheRes.json();
                renderUrls(urls);
            } else {
                urlsList.innerHTML = "<p class='error-message'>No hay datos disponibles offline.</p>";
            }
        }
    }

    await loadUrls();

    // 4) Manejador de click para botón Eliminar
    urlsList.addEventListener("click", async e => {
        if (!e.target.classList.contains("delete-btn")) return;
        const shortCode = e.target.dataset.shortcode;
        if (!confirm("¿Estás seguro de eliminar esta URL?")) return;

        // Invocamos fetch normalmente; en offline el SW devuelve {status:'queued'}
        const res = await fetch(`/api/url/${shortCode}`, {
            method: "DELETE",
            credentials: "include"
        }).catch(() => new Response(JSON.stringify({ status: 'queued' }), {
            status: 200, headers: { 'Content-Type': 'application/json' }
        }));

        if (!res.ok) {
            console.error("Error inesperado en DELETE:", res.status);
        }
        // Borramos visualmente la fila siempre
        e.target.closest(".url-row").remove();
        if (!document.querySelector(".url-row")) {
            urlsList.innerHTML = "<p class='empty-message'>No has creado ninguna URL.</p>";
        }
    });
});

self.addEventListener('fetch', event => {
    const req = event.request;
    // Sólo interceptamos GET de recursos estáticos
    if (req.method !== 'GET') return;

    event.respondWith(
        caches.match(req).then(cachedRes => {
            if (cachedRes) {
                // 1) Si está en caché, lo devolvemos
                return cachedRes;
            }
            // 2) Si no, lo pedimos a la red y lo cacheamos
            return fetch(req).then(networkRes => {
                return caches.open(CACHE_NAME).then(cache => {
                    // Evitamos cachear peticiones cross-origin no-controladas
                    if (networkRes.ok && req.url.startsWith(self.location.origin)) {
                        cache.put(req, networkRes.clone());
                    }
                    return networkRes;
                });
            });
        })
    );
});