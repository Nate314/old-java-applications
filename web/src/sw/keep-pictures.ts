// Page side of offline (sw.ts keeps what a page loads): pictures below the fold load lazily, so a page seen once
// showed them broken offline (18 on the home page, 2 on the story). Once the worker controls the page and the page
// has loaded and gone idle, the lazy pictures still waiting load now, the browser picking the size this screen
// uses, and come through the worker, which keeps them. Not on Save-Data or 2G links, where it would spend the
// visitor's data on pictures they may never scroll to. Imports nothing, so boot.js gains a few lines only.
// Pictures a page adds later (a tab opened, a filter typed) are kept when they show, online, as before.

export function keepPictures() {
  const sw = navigator.serviceWorker;
  if (!sw || !window.isSecureContext) return;
  const net = (navigator as Navigator & { connection?: { saveData?: boolean; effectiveType?: string } }).connection;
  if (net && (net.saveData || /(^|-)2g$/.test(net.effectiveType || ''))) return;
  if (!sw.controller) { sw.addEventListener('controllerchange', keepPictures, { once: true }); return; }
  if (document.readyState !== 'complete') { addEventListener('load', keepPictures, { once: true }); return; }
  const idle = window.requestIdleCallback || ((f: () => void) => setTimeout(f, 1000));
  idle(() => {
    document.querySelectorAll<HTMLImageElement>('img[loading="lazy"]').forEach(img => { if (!img.complete) img.loading = 'eager'; });
    // Pictures shown before the worker took over, or still loading then, never came through it: it keeps them now
    // (from the HTTP cache; ones it has are skipped, sw.ts message handler).
    sw.controller?.postMessage({ oldjavaKeep: [...document.images].map(img => img.currentSrc).filter(Boolean) });
  });
}
