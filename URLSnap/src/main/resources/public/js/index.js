// index.js
document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector(".url-form");
    const input = document.querySelector(".url-input");
    const urlsList = document.querySelector(".urls-list");
    const previewContainer = document.getElementById("preview-container");
    const previewBox = document.getElementById("preview");

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const originalUrl = input.value.trim();
        if (!originalUrl) return;

        try {
            // Enviar la URL para ser acortada
            const response = await fetch("/api/acortar", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ url: originalUrl })
            });

            if (!response.ok) {
                const error = await response.json();
                alert(error.error || "Hubo un error al acortar la URL.");
                return;
            }

            const data = await response.json();
            input.value = ""; // Limpiar campo

            // Crear nuevo bloque de URL
            const urlItem = document.createElement("div");
            urlItem.className = "url-item fade-in"; // puedes animarlo con CSS si quieres
            urlItem.innerHTML = `
            <div class="original-url">${data.originalUrl}</div>
            <div class="short-url-container">
                <span class="short-url">${data.shortUrl}</span>
                <button class="copy-button">Copiar</button>
            </div>
        `;

            // Insertar al inicio
            urlsList.prepend(urlItem);

            // Limpiar la vista previa
            previewContainer.style.display = "none";

        } catch (err) {
            console.error(err);
            alert("Error al conectarse con el servidor.");
        }
    });

    input.addEventListener("input", async () => {
        const url = input.value.trim();
        if (url) {
            // Mostrar la vista previa cuando el usuario ingrese la URL
            previewContainer.style.display = "block";

            // Usar Microlink para obtener la vista previa
            const response = await fetch(`https://api.microlink.io/?url=${encodeURIComponent(url)}`);
            const previewData = await response.json();

            if (previewData.error) {
                previewBox.innerHTML = `<p>Hubo un problema al cargar la vista previa.</p>`;
            } else {
                previewBox.innerHTML = `
                    <img src="${previewData.data.image.url}" alt="Vista previa" style="max-width: 100%; height: auto;">
                    <p><strong>${previewData.data.title}</strong></p>
                    <p>${previewData.data.description}</p>
                `;
            }
        } else {
            previewContainer.style.display = "none"; // Ocultar la vista previa si la URL está vacía
        }
    });

    // Evento global para botones "Copiar"
    document.addEventListener("click", (e) => {
        if (e.target.classList.contains("copy-button")) {
            const shortUrl = e.target.previousElementSibling.textContent;
            navigator.clipboard.writeText(shortUrl).then(() => {
                e.target.textContent = "Copiado!";
                setTimeout(() => e.target.textContent = "Copiar", 1500);
            });
        }
    });
});
