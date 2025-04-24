const CACHE_NAME = 'urlsnap-cache-v2.4'; // 🔁 Se actualizó para forzar recarga
const URLS_TO_CACHE = [
    '/',
    '/mis-urlsnaps',
    '/css/index.css',
    '/css/mis-urlsnaps.css',
    '/js/components/nav-loader.js',
    '/js/mis-urlsnaps.js',
    '/partials/nav.html'
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(async cache => {
            try {
                await cache.addAll(URLS_TO_CACHE);
            } catch (err) {
                console.warn('⚠️ Algunos recursos fallaron en cache:', err);
            }
        })
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(
                keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))
            ).then(() => self.clients.claim())
        )
    );
});

// IndexedDB para encolar POST/DELETE
function openDb() {
    return new Promise((res, rej) => {
        const req = indexedDB.open('urlsnap-store', 1);
        req.onupgradeneeded = () => {
            const db = req.result;
            if (!db.objectStoreNames.contains('pending')) {
                db.createObjectStore('pending', { autoIncrement: true });
            }
        };
        req.onsuccess = () => res(req.result);
        req.onerror = () => rej(req.error);
    });
}

function addToQueue(record) {
    return openDb().then(db =>
        new Promise((res, rej) => {
            const tx = db.transaction('pending', 'readwrite');
            tx.objectStore('pending').add(record);
            tx.oncomplete = () => res();
            tx.onerror = () => rej(tx.error);
        })
    );
}

function replayQueue() {
    return openDb().then(db =>
        new Promise((res, rej) => {
            const tx = db.transaction('pending', 'readwrite');
            const store = tx.objectStore('pending');
            const cursorReq = store.openCursor();

            cursorReq.onsuccess = async () => {
                const cursor = cursorReq.result;
                if (!cursor) return res();
                const { url, method, body } = cursor.value;
                try {
                    const response = await fetch(url, {
                        method,
                        headers: { 'Content-Type': 'application/json' },
                        body,
                        credentials: 'include'
                    });
                    if (!response.ok) throw new Error('HTTP error');
                    store.delete(cursor.key);
                    cursor.continue();
                } catch {
                    return res();
                }
            };
            cursorReq.onerror = () => rej(cursorReq.error);
        })
    );
}

self.addEventListener('sync', event => {
    if (event.tag === 'sync-urls') {
        event.waitUntil(replayQueue());
    }
});

self.addEventListener('fetch', event => {
    const { request } = event;
    const url = new URL(request.url);

    // ❌ Nunca cachear /api/yo
    if (url.pathname === '/api/yo') return;

    // ✅ Siempre fetch directo para /api/mis-urlsnaps y actualizar el caché
    if (request.method === 'GET' && url.pathname === '/api/mis-urlsnaps') {
        event.respondWith(
            fetch(request)
                .then(netRes => {
                    if (netRes.ok) {
                        const clone = netRes.clone();
                        caches.open(CACHE_NAME).then(cache => cache.put(request, clone));
                    }
                    return netRes;
                })
                .catch(async () => {
                    const cached = await caches.match(request);
                    return cached || new Response('[]', {
                        headers: { 'Content-Type': 'application/json' }
                    });
                })
        );
        return;
    }

    // 📨 Encolar POST o DELETE en caso de fallo
    if (
        (request.method === 'POST' && url.pathname === '/api/acortar') ||
        (request.method === 'DELETE' && url.pathname.startsWith('/api/url/'))
    ) {
        event.respondWith(
            fetch(request.clone()).catch(async () => {
                let body = null;
                try { body = await request.clone().json(); } catch {}
                await addToQueue({
                    url: request.url,
                    method: request.method,
                    body: body ? JSON.stringify(body) : null
                });
                await self.registration.sync.register('sync-urls');
                return new Response(JSON.stringify({ status: 'queued' }), {
                    status: 200,
                    headers: { 'Content-Type': 'application/json' }
                });
            })
        );
        return;
    }

    // Navegación SPA → fallback offline
    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request).catch(() =>
                caches.match(request).then(c => c || caches.match('/partials/offline.html'))
            )
        );
        return;
    }

    // Cache-first para recursos estáticos únicamente (excluye /api/*)
    if (request.method === 'GET' && !url.pathname.startsWith('/api/')) {
        event.respondWith(
            caches.match(request).then(cached => {
                return fetch(request)
                    .then(netRes => {
                        if (!netRes || !netRes.ok) return cached || netRes;
                        const cloned = netRes.clone();
                        if (request.url.startsWith(self.location.origin)) {
                            caches.open(CACHE_NAME).then(cache => cache.put(request, cloned));
                        }
                        return netRes;
                    })
                    .catch(() => cached);
            })
        );
    }
});
