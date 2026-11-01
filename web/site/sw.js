/*
 * The site's service worker: it makes the arcade installable and lets saved apps play with no connection.
 *
 * Network first: every request goes to the server as usual (so a new build shows up at once), and each good
 * answer is kept in the cache. When the network fails, the cached copy answers instead. Apps played once are
 * therefore kept, and the home page's "Save all for offline" fills the cache with every app in one go.
 * Cache keys leave out the query string, so App/index.html?embed (the home page's player) and the app's own
 * page share one entry.
 */
'use strict';
var CACHE = 'oldjava-v1';   // the home page's offline panel (index.html) writes to the same cache

self.addEventListener('install', function () { self.skipWaiting(); });
self.addEventListener('activate', function (e) {
  e.waitUntil(caches.keys().then(function (names) {
    return Promise.all(names.filter(function (n) { return n.indexOf('oldjava-') === 0 && n !== CACHE; })
      .map(function (n) { return caches.delete(n); }));
  }).then(function () { return self.clients.claim(); }));
});

function key(request) {
  var url = new URL(request.url);
  url.search = '';
  url.hash = '';
  return url.href;
}

function offlinePage() {
  var home = new URL('./index.html', self.registration.scope).href;
  return new Response('<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">' +
    '<title>Offline</title><body style="margin:0;display:grid;place-items:center;min-height:100vh;background:#170a2e;' +
    'color:#e6e0ff;font:16px/1.5 system-ui,sans-serif;text-align:center"><div style="padding:24px;max-width:420px">' +
    '<h1 style="margin:0 0 8px;font-size:22px;color:#7df9ff">You are offline</h1>' +
    '<p style="margin:0 0 16px">This app is not saved on this device yet. Next time you are online, use ' +
    '"Save all for offline" on the home page, or play the app once.</p>' +
    '<a href="' + home + '" target="_top" style="color:#ff2bd6;font-weight:700">Back to all apps</a></div></body>',
    { headers: { 'Content-Type': 'text/html; charset=utf-8' } });
}

self.addEventListener('fetch', function (e) {
  var request = e.request;
  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
  e.respondWith(fetch(request).then(function (response) {
    if (response.status === 200 && response.type === 'basic') {
      var copy = response.clone();
      e.waitUntil(caches.open(CACHE).then(function (cache) { return cache.put(key(request), copy); }));
    }
    return response;
  }).catch(function (error) {
    return caches.match(key(request)).then(function (hit) {
      if (hit) return hit;
      if (request.mode === 'navigate') return offlinePage();
      throw error;
    });
  }));
});
