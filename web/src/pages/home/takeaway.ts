// Take the arcade with you: Install app (the browser's install prompt, which boot.js keeps for this page)
// and Save all for offline, which puts every file of every app into the service worker's cache (sw.js).
// Saved cards get an Offline chip; with no connection the banner says so and unsaved cards step back.
import { all, byId, one } from '../../shared/dom';

const CACHE = 'oldjava-v1';   // the cache sw.js serves from

export function initTakeaway(): void {
  const panel = byId('takeaway');
  if (!('serviceWorker' in navigator) || !window.caches || !window.isSecureContext) return;
  panel.hidden = false;

  const text = byId('takeaway-text');
  const meter = byId('takeaway-meter');
  const installButton = byId('takeaway-install');
  const saveButton = byId<HTMLButtonElement>('takeaway-save');
  const note = byId('takeaway-note');
  const banner = byId('offline-banner');
  const cards = all<HTMLLIElement>('#apps li[data-app]');
  const total = cards.length;
  const appOf = (li: HTMLLIElement) => li.getAttribute('data-app') as string;

  // ---- install ----
  function installed() {
    return matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
  }
  function showInstall() {
    installButton.hidden = !window.oldjavaInstall || installed();
    note.hidden = !installButton.hidden || installed();
  }
  document.addEventListener('oldjava-installable', showInstall);
  installButton.addEventListener('click', () => {
    const prompt = window.oldjavaInstall;
    if (!prompt) return;
    prompt.prompt();
    prompt.userChoice.then(() => { window.oldjavaInstall = null; showInstall(); });
  });
  window.addEventListener('appinstalled', () => {
    window.oldjavaInstall = null;
    showInstall();
    text.textContent = 'Installed. Open Old Java from your apps, or keep playing here.';
  });
  showInstall();

  // ---- which files each app needs: its page, classes.js and the files its page preloads (data-files) ----
  const abs = (path: string) => new URL(path, location.href).href;
  // The pages' own scripts and stylesheets are listed by the build (assets/files.json, see vite/pages.ts).
  function shell(bundle: string[]) {
    const files = ['./', 'index.html', 'history.txt', 'shim.css', 'manifest.webmanifest', 'icons/icon.svg',
                   'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png', 'icons/apple-touch-icon.png', 'apps.json', 'assets/files.json'];
    files.push(...bundle);
    files.push('story.html', 'play.html', 'StockChecker/stockCheck.csv', 'agents-timeline.svg');   // the story page and what it shows
    cards.forEach(li => files.push(one('img', li).getAttribute('src') as string));
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
  const saved: Record<string, boolean> = {};
  const chips = cards.map(li => one('.saved', li));
  function countSaved() { return cards.filter(li => saved[appOf(li)]).length; }
  function render() {
    const offline = !navigator.onLine;
    const count = countSaved();
    cards.forEach((li, i) => {
      const ok = !!saved[appOf(li)];
      li.classList.toggle('unavailable', offline && !ok);
      chips[i].hidden = !ok && !offline;
      chips[i].textContent = ok ? 'Offline ✓' : 'Needs a connection';
    });
    banner.hidden = !offline;
    banner.textContent = count === 0
      ? 'You are offline. No apps are saved on this device yet, so they need a connection.'
      : 'You are offline. ' + count + ' saved ' + (count === 1 ? 'app plays' : 'apps play') +
        ' here as usual' + (count === total ? '.' : total - count === 1 ? '; the other one needs a connection.' : '; the others need a connection.');
  }
  function check() {
    return caches.open(CACHE).then(cache => Promise.all(cards.map(li => {
      const id = appOf(li);
      return filesOf(id).then(files => Promise.all(files.map(f => cache.match(f))))
        .then(hits => { saved[id] = hits.every(Boolean); }, () => { saved[id] = false; });
    }))).then(() => {
      render();
      const count = countSaved();
      if (count === total) {
        text.textContent = 'All ' + total + ' apps are saved on this device. They play with no connection.';
        saveButton.textContent = 'Update saved apps';
      } else if (count > 0) {
        text.textContent = count + ' of ' + total + ' apps are saved on this device (apps you play are kept). ' +
          'Save the rest to play all of them offline.';
      }
    });
  }

  // ---- save all: fetch every file, four at a time, and put it in the cache ----
  function megabytes(bytes: number) { return (bytes / 1048576).toFixed(1) + ' MB'; }
  saveButton.addEventListener('click', () => {
    saveButton.disabled = true;
    saveButton.textContent = 'Saving...';
    meter.hidden = false;
    const bar = meter.firstElementChild as HTMLElement;
    bar.style.width = '0';
    let failed = 0;
    let bytes = 0;
    const bundle = fetch('assets/files.json').then(r => {
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r.json() as Promise<string[]>;
    });
    Promise.all([caches.open(CACHE), Promise.all(cards.map(li => filesOf(appOf(li)))), bundle])
      .then(([cache, perApp, built]) => {
        let queue = shell(built);
        perApp.forEach(files => { queue = queue.concat(files); });
        queue = queue.filter((f, i) => queue.indexOf(f) === i);
        const pending = queue.slice();   // the four workers take files from its front until it is empty
        let done = 0;
        function progress() {
          bar.style.width = (100 * done / queue.length) + '%';
          text.textContent = 'Saving ' + done + ' of ' + queue.length + ' files (' + megabytes(bytes) + ')...';
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
        saveButton.disabled = false;
        meter.hidden = true;
        window.oldjavaSound.play(failed ? 'offline' : 'saved');
        if (failed) {
          text.textContent = 'Could not save ' + failed + ' file' + (failed === 1 ? '' : 's') +
            '. Check the connection and try again (' + countSaved() + ' of ' + total + ' apps are saved).';
          saveButton.textContent = 'Try again';
        } else {
          text.textContent = 'All ' + total + ' apps saved on this device (' + megabytes(bytes) + '). ' +
            'They play with no connection' + (installed() ? '.' : ', in this browser or installed as an app.');
        }
      });
  });

  addEventListener('online', check);   // the cache may have changed since (apps played meanwhile)
  addEventListener('offline', () => { window.oldjavaSound.play('offline'); check(); });
  check();
}
