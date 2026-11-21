// The site's service worker: it makes the arcade installable and lets saved apps play with no connection.
//
// Network first: every request goes to the server as usual (so a new build shows up at once), and each good
// answer is kept in the cache. When the network fails, the cached copy answers instead. Apps played once are
// therefore kept, and the home page's "Save all for offline" fills the cache with every app in one go.
// Cache keys leave out the query string, so App/index.html?embed (the home page's player) and the app's own
// page share one entry; an app's program keeps its build's stamp (classes.js?v=<hash>, vite/stamp-programs.mjs),
// so a page only ever gets the program it names, never another build's.
// The built scripts and stylesheets (assets/<name>-<hash>.js and .css) and stamped programs never change under their
// name, so they answer from the cache first. This build's (BUILT, filled in by the build) go in on installing.
// A page is kept only together with everything it names (scripts, stylesheets, program, data files), and a new build
// stages fresh copies of every kept page in NEXT while installing, swapped in on activating: so offline a page always
// runs whole, as the newest build that finished saving it, never an old program against a new runtime.
// Built by vite/pages.ts into the site root as sw.js (a service worker's scope is its own folder and below).
export {};   // a module, so this file's self can be typed as the service worker's
declare const self: ServiceWorkerGlobalScope;
declare const BUILT: string[];   // this build's assets/ files, relative to the site root
const sw = self;

const CACHE = 'oldjava-v1';   // the home page's offline panel (index.html) writes to the same cache
const NEXT = 'oldjava-next';  // a new build's copies of the kept pages, while it installs

const assets = () => new URL('assets/', sw.registration.scope).href;
const built = () => BUILT.map(f => new URL(f, sw.registration.scope).href);
const program = /\/(classes|then)\.js$/;
// the names serve.ps1 and nginx.conf send as immutable
const fixedName = (url: URL) => /^\/assets\/.+-[A-Za-z0-9_-]{8}\.(js|css)$/.test(url.pathname) || (program.test(url.pathname) && /^\?v=/.test(url.search));
const isPage = (url: string) => /(\.html|\/)$/.test(new URL(url).pathname);

/** What a page needs to run: the scripts and stylesheets it names (its program with its ?v=) and its data-files. */
function needs(page: string, html: string): string[] {
  const named = [...html.matchAll(/\s(?:src|href)="([^"]+\.(?:js|css)(?:\?[^"]*)?)"/g)].map(m => m[1]);
  const data = /<body[^>]*\sdata-files="([^"]*)"/.exec(html);
  return named.concat(data ? data[1].split(/\s+/).filter(Boolean) : []).map(f => new URL(f, page))
    .filter(u => u.origin === self.location.origin).map(u => u.href);
}
function fetchOk(url: string): Promise<Response> {
  return fetch(url).then(r => { if (!r.ok) throw new Error(r.status + ' ' + url); return r; });
}

// On installing, this build's scripts and stylesheets (about 210 KB gzipped) and a fresh copy of every page kept
// so far, with what it needs that is not kept yet (a changed program, a new data file), go in NEXT before it takes
// over. Data files keep their names across builds, so a kept copy is reused. If anything fails to load, this build
// waits and the one already installed keeps answering from its own whole copies.
function restage(saved: Cache, next: Cache, request: Request): Promise<unknown> {
  return fetch(request.url, { cache: 'no-cache' }).then((response): Promise<unknown> => {
    if (response.status === 404) return saved.delete(request);   // gone from the site
    if (!response.ok) throw new Error(response.status + ' ' + request.url);
    return response.clone().text().then(html => Promise.all(needs(response.url, html).map(f => next.match(f).then((hit): unknown => hit ||
      saved.match(f).then(kept => kept || fetchOk(f)).then(r => next.put(f, r))))))
      .then(() => next.put(request, response));
  });
}
sw.addEventListener('install', e => {
  sw.skipWaiting();
  e.waitUntil(caches.delete(NEXT).then(() => Promise.all([caches.open(CACHE), caches.open(NEXT)])).then(([saved, next]) =>
    next.addAll(built()).then(() => saved.keys()).then(requests => Promise.all(requests.filter(r => isPage(r.url)).map(r => restage(saved, next, r)))))
    .catch(error => caches.delete(NEXT).then(() => { throw error; })));   // the next try starts afresh
});
// On activating (pages' requests wait meanwhile), the staged copies replace the old ones, then programs and built
// files that no kept page names any more leave the cache.
sw.addEventListener('activate', e => {
  e.waitUntil(Promise.all([caches.open(CACHE), caches.open(NEXT)]).then(([saved, next]) =>
    next.keys().then(staged => Promise.all(staged.map(r => next.match(r).then(hit => hit && saved.put(r, hit)))))
      .then(() => caches.keys()).then(names => Promise.all(names.filter(n => n.indexOf('oldjava-') === 0 && n !== CACHE).map(n => caches.delete(n))))
      .then(() => saved.keys()).then(requests => Promise.all(requests.filter(r => isPage(r.url))
        .map(r => saved.match(r).then(hit => hit ? hit.text().then(html => needs(r.url, html)) : []))).then(named => {
          const keep = new Set(built().concat(assets() + 'files.json', ...named.flat()));   // files.json keeps its name (Save all reads it)
          return Promise.all(requests.filter(r => (r.url.indexOf(assets()) === 0 || program.test(new URL(r.url).pathname)) && !keep.has(r.url))
            .map(r => saved.delete(r)));
        })))
    .then(() => sw.clients.claim()));
});

function key(request: Request): string {
  const url = new URL(request.url);
  if (!program.test(url.pathname)) url.search = '';
  url.hash = '';
  return url.href;
}

// Pages from the network wait here until everything they need is kept (it comes through this handler as they load).
const unsettled = new Map<string, { response: Response; needs: string[] }>();
function settle(cache: Cache): Promise<unknown> {
  return Promise.all([...unsettled].map(([page, p]) => Promise.all(p.needs.map(f => cache.match(f))).then(hits => {
    if (!hits.every(Boolean) || unsettled.get(page) !== p) return;
    unsettled.delete(page);
    return cache.put(page, p.response);
  })));
}

function offlinePage(): Response {
  const home = new URL('./index.html', sw.registration.scope).href;
  return new Response('<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">' +
    '<title>Offline</title><body style="margin:0;display:grid;place-items:center;min-height:100vh;background:#170a2e;' +
    'color:#e6e0ff;font:16px/1.5 system-ui,sans-serif;text-align:center"><div style="padding:24px;max-width:420px">' +
    '<h1 style="margin:0 0 8px;font-size:22px;color:#7df9ff">You are offline</h1>' +
    '<p style="margin:0 0 16px">This app is not saved on this device yet. Next time you are online, use ' +
    '"Save all for offline" on the home page, or play the app once.</p>' +
    '<a href="' + home + '" target="_top" style="color:#ff2bd6;font-weight:700">Back to all apps</a></div></body>',
    { headers: { 'Content-Type': 'text/html; charset=utf-8' } });
}

sw.addEventListener('fetch', e => {
  const request = e.request;
  const url = new URL(request.url);
  if (request.method !== 'GET' || url.origin !== self.location.origin) return;
  if (/\/downloads\/[^/]+\.jar$/.test(url.pathname)) return;   // the desktop jars are not kept
  const k = key(request);
  // A page is always checked with the server: one the HTTP cache kept (GitHub Pages lets it keep any file 10 minutes)
  // could name the hashed files of an older build, which this cache no longer has.
  const page = request.mode === 'navigate' || /(\.html|\/)$/.test(url.pathname);
  e.respondWith((fixedName(new URL(k)) ? caches.match(k) : Promise.resolve(undefined)).then(hit => hit || fetch(request, page ? { cache: 'no-cache' } : undefined).then(response => {
    if (response.status === 200 && response.type === 'basic') {
      const copy = response.clone();
      e.waitUntil(caches.open(CACHE).then(cache => (page
        ? copy.clone().text().then(html => { unsettled.set(k, { response: copy, needs: needs(response.url || k, html) }); })
        : cache.put(k, copy)).then(() => settle(cache))));
    }
    return response;
  }).catch(error => caches.match(k).then(hit => {
    if (hit) return hit;
    if (request.mode === 'navigate') return offlinePage();
    throw error;
  }))));
});
