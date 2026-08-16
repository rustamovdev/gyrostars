// Service Worker for GyroStars WebApp Instant Loading
const CACHE_NAME = 'gyrostars-v2';
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/app.css',
  '/app.js'
];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    }).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  // Only cache static GET requests, bypass API calls
  if (e.request.method === 'GET' && !e.request.url.includes('/api/')) {
    e.respondWith(
      caches.match(e.request).then((cached) => {
        return cached || fetch(e.request).then((res) => {
          return caches.open(CACHE_NAME).then((cache) => {
            cache.put(e.request, res.clone());
            return res;
          });
        });
      }).catch(() => fetch(e.request))
    );
  }
});
