document.addEventListener('DOMContentLoaded', async () => {
    const urlSelect = document.getElementById('url-select');
    const estadisticasContenedor = document.getElementById('estadisticas-contenido');
    const selectedUrlTitle = document.getElementById('selected-url');
    const totalAccessCount = document.getElementById('total-access-count');
    let visitasChart, navegadoresChart, sistemasChart, referersChart, tiempoChart;

    async function cargarUrls() {
        const res = await fetch('/api/yo');
        const usuario = await res.json();

        const urlsRes = await fetch(usuario.esAdmin ? '/api/todas-las-urls' : '/api/mis-urlsnaps');
        const urls = await urlsRes.json();

        urls.forEach(url => {
            const option = document.createElement('option');
            option.value = url.shortCode;
            option.textContent = url.originalUrl;
            urlSelect.appendChild(option);
        });
    }

    function crearGrafico(id, label, labels, data) {
        const ctx = document.getElementById(id).getContext('2d');
        return new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label,
                    data,
                    backgroundColor: [
                        '#4dc9f6', '#f67019', '#f53794', '#537bc4', '#acc236', '#166a8f'
                    ],
                    borderColor: '#fff',
                    borderWidth: 1,
                    borderRadius: 6,
                }]
            },
            options: {
                responsive: true,
                animation: {
                    duration: 800,
                    easing: 'easeOutBounce'
                },
                plugins: {
                    legend: { display: false },
                    title: {
                        display: true,
                        text: label,
                        font: { size: 16 }
                    },
                    tooltip: {
                        callbacks: {
                            label: ctx => `🔹 ${ctx.label}: ${ctx.parsed.y}`
                        }
                    }
                },
                scales: {
                    x: {
                        ticks: { color: '#444', font: { size: 12 } }
                    },
                    y: {
                        beginAtZero: true,
                        ticks: { color: '#444', stepSize: 1, font: { size: 12 } }
                    }
                }
            }
        });
    }

    function crearGraficoLinea(id, label, labels, data) {
        const ctx = document.getElementById(id).getContext('2d');
        return new Chart(ctx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label,
                    data,
                    fill: false,
                    borderColor: '#4dc9f6',
                    backgroundColor: '#4dc9f6',
                    tension: 0.3,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                animation: {
                    duration: 800,
                    easing: 'easeOutQuart'
                },
                plugins: {
                    legend: { display: false },
                    title: {
                        display: true,
                        text: label,
                        font: { size: 16 }
                    },
                    tooltip: {
                        callbacks: {
                            label: ctx => `📅 ${ctx.label}: ${ctx.parsed.y} accesos`
                        }
                    }
                },
                scales: {
                    x: {
                        title: { display: true, text: 'Fecha y hora de acceso', font: { size: 13 } },
                        ticks: {
                            autoSkip: true,
                            maxTicksLimit: 10,
                            color: '#444',
                            font: { size: 11 }
                        }
                    },
                    y: {
                        title: { display: true, text: 'Cantidad de accesos', font: { size: 13 } },
                        beginAtZero: true,
                        ticks: { color: '#444', stepSize: 1, font: { size: 11 } }
                    }
                }
            }
        });
    }

    async function cargarEstadisticas(shortCode) {
        const res = await fetch(`/api/estadisticas/${shortCode}`);
        const stats = await res.json();

        const visitas = stats.length;
        const navegadores = {};
        const sistemas = {};
        const referers = {};
        const accesosPorHora = {};

        stats.forEach(s => {
            // Fecha/hora
            const fecha = new Date(s.accessedAt);
            const opciones = {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                timeZone: 'UTC'
            };
            const fechaFormateada = fecha.toLocaleString('es-ES', opciones);
            accesosPorHora[fechaFormateada] = (accesosPorHora[fechaFormateada] || 0) + 1;

            // Navegador
            const navegador = s.browser || 'Desconocido';
            navegadores[navegador] = (navegadores[navegador] || 0) + 1;

            // Sistema operativo
            const sistema = s.os || 'Desconocido';
            sistemas[sistema] = (sistemas[sistema] || 0) + 1;

            // Referer
            const referrer = s.referrer || 'Directo';
            referers[referrer] = (referers[referrer] || 0) + 1;
        });

        totalAccessCount.textContent = `🔁 Total de accesos: ${visitas}`;

        if (visitasChart) visitasChart.destroy();
        if (navegadoresChart) navegadoresChart.destroy();
        if (sistemasChart) sistemasChart.destroy();
        if (referersChart) referersChart.destroy();
        if (tiempoChart) tiempoChart.destroy();

        visitasChart = crearGrafico('visitasChart', 'Accesos', ['Visitas'], [visitas]);
        navegadoresChart = crearGrafico('navegadoresChart', 'Navegadores', Object.keys(navegadores), Object.values(navegadores));
        sistemasChart = crearGrafico('sistemasChart', 'Sistemas Operativos', Object.keys(sistemas), Object.values(sistemas));
        referersChart = crearGrafico('referersChart', 'Referers', Object.keys(referers), Object.values(referers));
        tiempoChart = crearGraficoLinea('tiempoChart', 'Accesos en el tiempo', Object.keys(accesosPorHora), Object.values(accesosPorHora));

        estadisticasContenedor.style.display = 'block';
        selectedUrlTitle.textContent = `📊 ${shortCode}`;
    }

    document.getElementById('refresh-btn').addEventListener('click', () => {
        const shortCode = urlSelect.value;
        if (shortCode) cargarEstadisticas(shortCode);
    });

    document.getElementById('generate-qr').addEventListener('click', () => {
        try {
            const url = `http://localhost:7000/${urlSelect.value}`;
            const qrContainer = document.getElementById("qrcode");
            qrContainer.innerHTML = "";
            new QRCode(qrContainer, {
                text: url,
                width: 200,
                height: 200,
                colorDark: "#000000",
                colorLight: "#ffffff",
                correctLevel: QRCode.CorrectLevel.H
            });
        } catch (e) {
            console.error('QR error', e);
        }
    });

    document.getElementById('download-btn').addEventListener('click', () => {
        const canvasElements = [
            document.getElementById('visitasChart'),
            document.getElementById('navegadoresChart'),
            document.getElementById('sistemasChart'),
            document.getElementById('referersChart'),
            document.getElementById('tiempoChart')
        ];

        const combinedCanvas = document.createElement('canvas');
        const width = canvasElements[0].width;
        const height = canvasElements[0].height * canvasElements.length;
        combinedCanvas.width = width;
        combinedCanvas.height = height;
        const ctx = combinedCanvas.getContext('2d');

        canvasElements.forEach((c, i) => {
            ctx.drawImage(c, 0, i * c.height);
        });

        const link = document.createElement('a');
        link.download = 'estadisticas.png';
        link.href = combinedCanvas.toDataURL('image/png');
        link.click();
    });

    urlSelect.addEventListener('change', () => {
        const shortCode = urlSelect.value;
        if (shortCode) cargarEstadisticas(shortCode);
    });

    cargarUrls();
});
