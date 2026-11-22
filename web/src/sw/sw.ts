// The site's service worker: it makes the arcade installable and lets saved apps play with no connection.
//
// Network first: every request goes to the server as usual (so a new build shows up at once), and each good
// answer is kept in the cache. When the network fails, the cached copy answers instead. Apps played once are
// therefore kept, and the home page's "Save all for offline" fills the cache with every app in one go.
// Cache keys leave out the query string, so App/index.html?embed (the home page's player) and the app's own
// page share one entry; an app's program keeps its build's stamp (classes.js?v=<hash>, vite/stamp-programs.mjs),
// so a page only ever gets the program it names, never another build's (cacheKey, offline-files.ts).
// The built scripts and stylesheets (assets/<name>-<hash>.js and .css) and stamped programs never change under their
// name, so they answer from the cache first. This build's (BUILT, filled in by the build) go in on installing.
// A page is kept only together with everything it names (scripts, stylesheets, program, data files), and a new build
// stages fresh copies of every kept page in NEXT while installing, swapped in on activating: so offline a page always
// runs whole, as the newest build that finished saving it, never an old program against a new runtime.
// Preview clips are kept whole the first time one plays (or by Save all) and cut to the asked range offline.
// Built by vite/pages.ts into the site root as sw.js (a service worker's scope is its own folder and below).
import { CACHE, cacheKey, isProgram, pageNames } from './offline-files';
declare const self: ServiceWorkerGlobalScope;
declare const BUILT: string[];   // this build's assets/ files, relative to the site root
const sw = self;

const NEXT = 'oldjava-next';  // a new build's copies of the kept pages, while it installs

const assets = () => new URL('assets/', sw.registration.scope).href;
const built = () => BUILT.map(f => new URL(f, sw.registration.scope).href);
// the names serve.ps1 and nginx.conf send as immutable
const fixedName = (url: URL) => /^\/assets\/.+-[A-Za-z0-9_-]{8}\.(js|css)$/.test(url.pathname) || (isProgram(url) && /^\?v=/.test(url.search));
const isPage = (url: string) => /(\.html|\/)$/.test(new URL(url).pathname);
// preview clips (clips/<App>.mp4): kept whole, answered offline in the ranges a video asks for (ranged)
const isClip = (url: URL) => /\.(mp4|webm)$/.test(url.pathname);
// the desktop jars are not kept
const kept = (url: URL) => url.origin === self.location.origin && !/\/downloads\/[^/]+\.jar$/.test(url.pathname);

/** What a page needs to run: everything it names (offline-files.ts). */
function needs(page: string, html: string): string[] {
  const named = pageNames(page, html);
  return named.code.concat(named.data);
}
function fetchOk(url: string): Promise<Response> {
  return fetch(url).then(r => { if (!r.ok) throw new Error(r.status + ' ' + url); return r; });
}

// ---- install: stage this build in NEXT ----
// This build's scripts and stylesheets (about 210 KB gzipped) and a fresh copy of every page kept so far, with what
// it needs that is not kept yet (a changed program, a new data file), go in NEXT before it takes over. Data files
// keep their names across builds, so a kept copy is reused. If anything fails to load, this build waits and the one
// already installed keeps answering from its own whole copies.
function restage(saved: Cache, next: Cache, request: Request): Promise<unknown> {
  return fetch(request.url, { cache: 'no-cache' }).then((response): Promise<unknown> => {
    if (response.status === 404) return saved.delete(request);   // gone from the site
    if (!response.ok) throw new Error(response.status + ' ' + request.url);
    return response.clone().text().then(html => Promise.all(needs(response.url, html).map(f => next.match(f).then((hit): unknown => hit ||
      saved.match(f).then(kept => kept || fetchOk(f)).then(r => next.put(f, r))))))
      .then(() => next.put(request, response));
  });
}
function stage(saved: Cache, next: Cache): Promise<unknown> {
  return next.addAll(built()).then(() => saved.keys())
    .then(requests => Promise.all(requests.filter(r => isPage(r.url)).map(r => restage(saved, next, r))));
}
sw.addEventListener('install', e => {
  sw.skipWaiting();
  e.waitUntil(caches.delete(NEXT).then(() => Promise.all([caches.open(CACHE), caches.open(NEXT)])).then(([saved, next]) => stage(saved, next))
    .catch(error => caches.delete(NEXT).then(() => { throw error; })));   // the next try starts afresh
});

// ---- activate: swap the staged copies in, then prune (pages' requests wait meanwhile) ----
function swapIn(saved: Cache, next: Cache): Promise<unknown> {
  return next.keys().then(staged => Promise.all(staged.map(r => next.match(r).then(hit => hit && saved.put(r, hit)))));
}
/** Every other oldjava- cache (NEXT, now swapped in, and any older build's) goes. */
function dropOtherCaches(): Promise<unknown> {
  return caches.keys().then(names => Promise.all(names.filter(n => n.indexOf('oldjava-') === 0 && n !== CACHE).map(n => caches.delete(n))));
}
/** Where the cache notes the build before this one's files (a cache key only, never fetched). */
const previousKey = () => new URL('oldjava-previous-build', sw.registration.scope).href;
/**
 * Programs and built files that no kept page names any more leave the cache, except the build before this one's:
 * a tab still open from it (this worker takes it over) loads its Find, skin maker and other parts later, under that
 * build's names, which the server no longer has. They go when the build after this one arrives.
 */
function prune(saved: Cache): Promise<unknown> {
  return Promise.all([saved.keys(), saved.match(previousKey()).then(r => r ? r.json() as Promise<string[]> : [])])
    .then(([requests, previous]) => Promise.all(requests.filter(r => isPage(r.url))
    .map(r => saved.match(r).then(hit => hit ? hit.text().then(html => needs(r.url, html)) : []))).then(named => {
    const keep = new Set(built().concat(assets() + 'files.json', previous, ...named.flat()));   // files.json keeps its name (Save all reads it)
    return Promise.all(requests.filter(r => (r.url.indexOf(assets()) === 0 || isProgram(new URL(r.url))) && !keep.has(r.url))
      .map(r => saved.delete(r)));
  })).then(() => saved.put(previousKey(), new Response(JSON.stringify(built()))));
}
sw.addEventListener('activate', e => {
  e.waitUntil(Promise.all([caches.open(CACHE), caches.open(NEXT)]).then(([saved, next]) =>
    swapIn(saved, next).then(dropOtherCaches).then(() => prune(saved)))
    .then(() => sw.clients.claim()));
});

// ---- keeping answers from the network ----
// Pages from the network wait here until everything they need is kept (it comes through this handler as they load).
const unsettled = new Map<string, { response: Response; needs: string[] }>();
function settle(cache: Cache): Promise<unknown> {
  return Promise.all([...unsettled].map(([page, p]) => Promise.all(p.needs.map(f => cache.match(f))).then(hits => {
    if (!hits.every(Boolean) || unsettled.get(page) !== p) return;
    unsettled.delete(page);
    return cache.put(page, p.response);
  })));
}
/** A preview clip's first answer to the video's "bytes=0-" (206 from GitHub Pages and nginx) holds the whole file: kept as it. */
function wholeFile(response: Response): Response {
  const range = /^bytes 0-(\d+)\/(\d+)$/.exec(response.headers.get('Content-Range') || '');
  if (response.status !== 206 || !range || +range[1] + 1 !== +range[2]) return response;
  const headers = new Headers(response.headers);
  headers.delete('Content-Range');
  return new Response(response.body, { status: 200, headers });
}
/** Keeps a good answer: a page waits in unsettled until everything it needs is kept, anything else goes in at once. */
function keep(k: string, response: Response, page: boolean): Promise<unknown> {
  if (response.type !== 'basic') return Promise.resolve();
  response = wholeFile(response);
  if (response.status !== 200) return Promise.resolve();
  return caches.open(CACHE).then(cache => (page
    ? response.clone().text().then(html => { unsettled.set(k, { response, needs: needs(response.url || k, html) }); })
    : cache.put(k, response)).then(() => settle(cache)));
}

// A page that loaded before this worker controlled it (an app page registers it only once its program loaded, so a
// first visit from a shared link) sends what it loaded once the worker takes over (install.tsx keepOffline). The
// copies come from the HTTP cache, so the page then plays offline from its first visit, not its second.
sw.addEventListener('message', e => {
  const urls: unknown = e.data && e.data.oldjavaKeep;
  if (!Array.isArray(urls)) return;
  e.waitUntil(Promise.all(urls.filter((u): u is string => typeof u === 'string').map(u => {
    const url = new URL(u, sw.registration.scope);
    if (!kept(url) || isClip(url)) return undefined;   // a clip loaded before the worker came is not fetched again (it is kept next time it plays)
    const k = cacheKey(url.href), page = isPage(url.href);
    return caches.match(k).then(hit => hit || fetch(url.href, page ? { cache: 'no-cache' } : undefined).then(r => keep(k, r, page))).catch(() => undefined);
  })));
});

// ---- answering ----
/** A kept clip cut to the request's Range: a video asks for its file in ranges (seeking, looping) and needs a 206. */
function ranged(request: Request, whole: Response): Promise<Response> {
  const range = /^bytes=(\d*)-(\d*)$/.exec(request.headers.get('Range') || '');
  if (!range || !(range[1] || range[2])) return Promise.resolve(whole);
  return whole.arrayBuffer().then(body => {
    const size = body.byteLength;
    const start = range[1] ? +range[1] : Math.max(0, size - +range[2]);   // "bytes=-500" is the last 500
    const end = range[1] && range[2] ? Math.min(+range[2], size - 1) : size - 1;
    if (start > end) return new Response(null, { status: 416, headers: { 'Content-Range': 'bytes */' + size } });
    return new Response(body.slice(start, end + 1), { status: 206, headers: {
      'Content-Type': whole.headers.get('Content-Type') || 'video/mp4', 'Content-Range': 'bytes ' + start + '-' + end + '/' + size,
      'Content-Length': String(end - start + 1), 'Accept-Ranges': 'bytes' } });
  });
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
  if (request.method !== 'GET' || !kept(url)) return;
  const k = cacheKey(request.url);
  // A page is always checked with the server: one the HTTP cache kept (GitHub Pages lets it keep any file 10 minutes)
  // could name the hashed files of an older build, which this cache no longer has.
  const page = request.mode === 'navigate' || isPage(request.url);
  e.respondWith((fixedName(new URL(k)) ? caches.match(k) : Promise.resolve(undefined)).then(hit => hit || fetch(request, page ? { cache: 'no-cache' } : undefined).then(response => {
    e.waitUntil(keep(k, response.clone(), page));
    return response;
  }).catch(error => caches.match(k).then(hit => {
    if (hit) return isClip(url) ? ranged(request, hit) : hit;
    if (request.mode === 'navigate') return offlinePage();
    throw error;
  }))));
});
