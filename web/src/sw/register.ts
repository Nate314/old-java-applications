// Page side of offline: registers the service worker (sw.js at the site root) and has it keep this page. boot.js
// calls it (runtime/install.tsx keepOffline), and so do the pages that don't load boot.js (the story, compare.html),
// so a visit that starts on one of them still plays offline later. Imports only keep-pictures.ts.
import { keepPictures } from './keep-pictures';

/** controlledAtLoad: the worker already controlled the page as it loaded, so it saw every file the page fetched. */
export function registerWorker(siteRoot: string, controlledAtLoad: boolean) {
  if (!('serviceWorker' in navigator) || !window.isSecureContext) return;
  navigator.serviceWorker.register(siteRoot + 'sw.js').catch(function (e) {
    console.warn('Offline play is not available: ' + e.message);
  });
  if (!controlledAtLoad) navigator.serviceWorker.ready.then(sendLoaded);
  keepPictures();
}

/** For a page at the site root without boot.js: registers once the page has loaded, so installing the worker (it
 * fetches the whole build) doesn't compete with the page's own files on a slow phone. */
export function registerAfterLoad() {
  const controlled = 'serviceWorker' in navigator && !!navigator.serviceWorker.controller;
  if (document.readyState === 'complete') registerWorker('', controlled);
  else addEventListener('load', () => registerWorker('', controlled), { once: true });
}

/** This page loaded before the worker saw it, so once the worker controls it, it is told what to keep (sw.ts message). */
function sendLoaded() {
  const sw = navigator.serviceWorker;
  if (!sw.controller) { sw.addEventListener('controllerchange', sendLoaded, { once: true }); return; }
  sw.controller.postMessage({ oldjavaKeep: [location.href].concat(performance.getEntriesByType('resource').map(e => e.name)) });
}
