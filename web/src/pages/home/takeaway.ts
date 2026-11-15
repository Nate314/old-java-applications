// Take the arcade with you: Install app (the browser's install prompt, which the runtime keeps for this page)
// and Save all for offline, which puts every file of every app into the service worker's cache (sw.js).
// Saved cards get an Offline chip, and the panel's cartridge case shows each app saved or not (tap one to save it alone); with no connection the banner says so and unsaved cards step back.
// What the panel, the chips and the banner show is in H.take; Home.tsx draws it.
import { APPS, webp } from '../../shared/catalog';
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
  const files = ['./', 'index.html', 'history.txt', 'shim.css', 'forced-colors.css', 'manifest.webmanifest', 'icons/icon.svg',
                 'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png', 'icons/apple-touch-icon.png', 'apps.json', 'assets/files.json'];
  files.push(...bundle);
  files.push('story.html', 'play.html', 'StockChecker/stockCheck.csv', 'agents-timeline.svg');   // the story page and what it shows
  files.push('compare.html');   // Then and now side by side (its frames are play.html)
  APPS.forEach(a => files.push(a.shot, webp(a.shot), webp(a.shot, true)));
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
function saveLabel() {
  const count = countSaved();
  return count === total ? 'Update saved apps' : count > 0 ? 'Save the other ' + (total - count) : 'Save all for offline';
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
    if (!take.saving) take.saveText = saveLabel();
    if (count === total) {
      take.text = 'All ' + total + ' apps are saved on this device. They play with no connection.';
    } else if (count > 0) {
      take.text = count + ' of ' + total + ' apps are saved on this device (apps you play are kept). ' +
        'Save the rest to play all of them offline.';
    }
    draw();
    // what the cache takes on this device, for the case's label (an estimate; the browser rounds it)
    if (navigator.storage && navigator.storage.estimate) navigator.storage.estimate().then(e => { take.used = e.usage || 0; draw(); }, () => {});
  });
}

// ---- save: fetch every file of the shell and the chosen apps, four at a time, and put it in the cache ----
// The files go app by app, so each cartridge in the case lights up as soon as its own files are in.
function megabytes(bytes: number) { return (bytes / 1048576).toFixed(1) + ' MB'; }
/** Save all: the apps not saved yet, or every app again (an update) once all are. */
export function saveClick() {
  const rest = APPS.filter(a => !take.saved[a.id]);
  saveApps((rest.length ? rest : APPS).map(a => a.id));
}
/** One cartridge: that app (and the site's shell, which every app needs) alone. */
export function saveOne(id: string) { saveApps([id]); }
function saveApps(ids: string[]) {
  if (take.saving) return;
  const one = ids.length === 1 ? APPS.find(a => a.id === ids[0])! : null;
  take.saving = true;
  take.saveText = 'Saving...';
  take.meterShown = true;
  take.meterW = 0;
  take.queued = {};
  ids.forEach(id => { if (!take.saved[id]) take.queued[id] = true; });
  if (one) take.queued[one.id] = true;   // saving one saved app again refreshes it
  draw();
  // a saved app that the browser later clears under storage pressure would fail offline; ask it to keep them
  if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});
  let failed = 0;
  let bytes = 0;
  const bundle = fetch('assets/files.json').then(r => {
    if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
    return r.json() as Promise<string[]>;
  });
  Promise.all([caches.open(CACHE), Promise.all(ids.map(filesOf)), bundle])
    .then(([cache, perApp, built]) => {
      const owner: Record<string, string> = {};   // file -> the app it belongs to (none for the shell)
      const left: Record<string, number> = {};    // app -> its files still to come
      const bad: Record<string, boolean> = {};
      let queue = shell(built);
      perApp.forEach((files, i) => files.forEach(f => {
        if (queue.indexOf(f) >= 0) return;
        queue.push(f);
        owner[f] = ids[i];
        left[ids[i]] = (left[ids[i]] || 0) + 1;
      }));
      queue = queue.filter((f, i) => queue.indexOf(f) === i);
      const pending = queue.slice();   // the four workers take files from its front until it is empty
      let done = 0;
      function progress() {
        take.meterW = 100 * done / queue.length;
        take.text = 'Saving ' + (one ? one.name + ': ' : '') + done + ' of ' + queue.length + ' files (' + megabytes(bytes) + ')...';
        draw();
      }
      function finished(url: string, ok: boolean) {
        const id = owner[url];
        if (!id) return;
        if (!ok) bad[id] = true;
        if (--left[id] === 0) {
          take.saved[id] = !bad[id];
          delete take.queued[id];
        }
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
        }).then(() => finished(url, true), () => { failed++; finished(url, false); })
          .then(() => { done++; progress(); return worker(); });
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
      take.queued = {};
      take.saveText = failed && !one ? 'Try again' : saveLabel();
      window.oldjavaSound.play(failed ? 'offline' : 'saved');
      if (failed) {
        take.text = 'Could not save ' + failed + ' file' + (failed === 1 ? '' : 's') +
          '. Check the connection and try again (' + countSaved() + ' of ' + total + ' apps are saved).';
      } else if (one) {
        take.text = one.name + ' is saved on this device (' + megabytes(bytes) + '). It plays with no connection. ' +
          countSaved() + ' of ' + total + ' apps are saved.';
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
