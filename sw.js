// Network-first Service Worker so updates always fetch latest code immediately
const CACHE_NAME = 'saavn-pro-cache-v18';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(keys.map((key) => caches.delete(key)));
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // NEVER intercept cross-origin API / relay calls (ntfy.sh, peerjs, etc.)
  if (url.origin !== self.location.origin) {
    return;
  }

  // Always fetch latest code from network for same-origin app files, fallback to cache only when offline
  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        return networkResponse;
      })
      .catch(() => caches.match(event.request))
  );
});
