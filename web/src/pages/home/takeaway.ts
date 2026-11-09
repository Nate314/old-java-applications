// Take the arcade with you: Install app (the browser's install prompt, which the runtime keeps for this page)
// and Save all for offline, which puts every file of every app into the service worker's cache (sw.js).
// Saved cards get an Offline chip; with no connection the banner says so and unsaved cards step back.
// What the panel, the chips and the banner show is in H.take; Home.tsx draws it.
import { APPS } from '../../shared/catalog';
import { H, draw } from './model';

const CACHE = 'oldjava-v1';   // the cache sw.js serves from
const total = APPS.length;
const take = H.take;

function installed() {
  return matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
}
function showInstall() {
  take.install = !!window.oldjavaInstall && !installed();
  take.note = take.install || installed() ? false : true;
  draw();
}
export function installClick() {
  const prompt = window.oldjavaInstall;
  if (!prompt) return;
  prompt.prompt();
  prompt.userChoice.then(() => { window.oldjavaInstall = null; showInstall(); });
}

// ---- which files each app needs: its page, classes.js and the files its page preloads (data-files) ----
const abs = (path: string) => new URL(path, location.href).href;
// The pages' own scripts and stylesheets are listed by the build (assets/files.json, see vite/pages.ts).
function shell(bundle: string[]) {
  const files = ['./', 'index.html', 'history.txt', 'shim.css', 'manifest.webmanifest', 'icons/icon.svg',
                 'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png', 'icons/apple-touch-icon.png', 'apps.json', 'assets/files.json'];
  files.push(...bundle);
  files.push('story.html', 'play.html', 'StockChecker/stockCheck.csv', 'agents-timeline.svg');   // the story page and what it shows
  APPS.forEach(a => files.push(a.shot));
  return files.map(abs);
}
const lists: Record<string, Promise<string[]>> = {};
function filesOf(id: string) {
  if (!lists[id]) {
    lists[id] = fetch(id + '/index.html').then(r => {
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r.text();
    }).then(html => {
      const body = new DOMParser().parseFromString(html, 'text/html').body;
      const extra = (body.getAttribute('data-files') || '').split(/\s+/).filter(Boolean);
      return [id + '/index.html', id + '/classes.js'].concat(extra.map(f => id + '/' + f)).map(abs);
    });
    lists[id].catch(() => { delete lists[id]; });
  }
  return lists[id];
}

// ---- saved state: an app counts as saved when every one of its files is in the cache ----
export function countSaved() { return APPS.filter(a => take.saved[a.id]).length; }
/** The banner's words with no connection. */
export function bannerText() {
  const count = countSaved();
  return count === 0
    ? 'You are offline. No apps are saved on this device yet, so they need a connection.'
    : 'You are offline. ' + count + ' saved ' + (count === 1 ? 'app plays' : 'apps play') +
      ' here as usual' + (count === total ? '.' : total - count === 1 ? '; the other one needs a connection.' : '; the others need a connection.');
}
function check() {
  return caches.open(CACHE).then(cache => Promise.all(APPS.map(a => {
    const id = a.id;
    return filesOf(id).then(files => Promise.all(files.map(f => cache.match(f))))
      .then(hits => { take.saved[id] = hits.every(Boolean); }, () => { take.saved[id] = false; });
  }))).then(() => {
    take.offline = !navigator.onLine;
    take.checked = true;
    const count = countSaved();
    if (count === total) {
      take.text = 'All ' + total + ' apps are saved on this device. They play with no connection.';
      take.saveText = 'Update saved apps';
    } else if (count > 0) {
      take.text = count + ' of ' + total + ' apps are saved on this device (apps you play are kept). ' +
        'Save the rest to play all of them offline.';
    }
    draw();
  });
}

// ---- save all: fetch every file, four at a time, and put it in the cache ----
function megabytes(bytes: number) { return (bytes / 1048576).toFixed(1) + ' MB'; }
export function saveClick() {
  take.saving = true;
  take.saveText = 'Saving...';
  take.meterShown = true;
  take.meterW = 0;
  draw();
  let failed = 0;
  let bytes = 0;
  const bundle = fetch('assets/files.json').then(r => {
    if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
    return r.json() as Promise<string[]>;
  });
  Promise.all([caches.open(CACHE), Promise.all(APPS.map(a => filesOf(a.id))), bundle])
    .then(([cache, perApp, built]) => {
      let queue = shell(built);
      perApp.forEach(files => { queue = queue.concat(files); });
      queue = queue.filter((f, i) => queue.indexOf(f) === i);
      const pending = queue.slice();   // the four workers take files from its front until it is empty
      let done = 0;
      function progress() {
        take.meterW = 100 * done / queue.length;
        take.text = 'Saving ' + done + ' of ' + queue.length + ' files (' + megabytes(bytes) + ')...';
        draw();
      }
      function worker(): Promise<void> {
        const url = pending.shift();
        if (!url) return Promise.resolve();
        return fetch(url, { cache: 'no-cache' }).then(r => {
          if (!r.ok) throw new Error(String(r.status));
          return r.blob().then(blob => {
            bytes += blob.size;
            return cache.put(url, new Response(blob, { headers: r.headers }));
          });
        }).catch(() => { failed++; }).then(() => { done++; progress(); return worker(); });
      }
      const workers: Promise<void>[] = [];
      for (let i = 0; i < 4; i++) workers.push(worker());
      progress();
      return Promise.all(workers);
    })
    .catch(() => { failed++; })
    .then(() => check())
    .then(() => {
      take.saving = false;
      take.meterShown = false;
      window.oldjavaSound.play(failed ? 'offline' : 'saved');
      if (failed) {
        take.text = 'Could not save ' + failed + ' file' + (failed === 1 ? '' : 's') +
          '. Check the connection and try again (' + countSaved() + ' of ' + total + ' apps are saved).';
        take.saveText = 'Try again';
      } else {
        take.text = 'All ' + total + ' apps saved on this device (' + megabytes(bytes) + '). ' +
          'They play with no connection' + (installed() ? '.' : ', in this browser or installed as an app.');
      }
      draw();
    });
}

export function initTakeaway(): void {
  if (!('serviceWorker' in navigator) || !window.caches || !window.isSecureContext) return;
  take.shown = true;
  document.addEventListener('oldjava-installable', showInstall);
  window.addEventListener('appinstalled', () => {
    window.oldjavaInstall = null;
    showInstall();
    take.text = 'Installed. Open Old Java from your apps, or keep playing here.';
    draw();
  });
  showInstall();
  addEventListener('online', check);   // the cache may have changed since (apps played meanwhile)
  addEventListener('offline', () => { window.oldjavaSound.play('offline'); check(); });
  check();
}
