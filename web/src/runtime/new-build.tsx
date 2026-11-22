import { siteRoot } from './install';
import { news } from './band-news';
import { floatToast } from './passport';

// ---- a newer build went up while this tab was open ----
// Every part the runtime adds later (Find, What's new, the skin maker, ...) is a file named by its build's hash. A tab
// with no service worker (an app page before Play, a browser without one) asks the server for the old name after a
// deploy, or one with a worker two deploys later (sw.ts keeps one build back), and gets a 404: the button did nothing.
// Vite says so with vite:preloadError. If this module's own file is gone too, the site has a newer build: say so and
// offer Reload, instead of a dead button. A file that is still there was a network blip, and offline says nothing.
let newer = false;

function tell() {
  const reload = () => location.reload();
  if (news) {
    news.show('update', <strong className="shim-news-tag">Updated</strong>, <span>A new version is out.</span>,
      <button type="button" className="shim-news-go" onClick={reload}>{'Reload ›'}</button>, 15000);
    return;
  }
  floatToast(siteRoot + 'icons/icon.svg', 'A new version is out', 'Reload this page to use it.', null, function () {
    return <button type="button" className="shim-toast-go" onClick={reload}>{'Reload ›'}</button>;
  });
}

export function init() {
  window.addEventListener('vite:preloadError', function () {
    if (newer) { tell(); return; }
    fetch(import.meta.url, { method: 'HEAD', cache: 'no-store' }).then(function (r) {
      if (r.status !== 404 && r.status !== 410) return;
      newer = true;
      tell();
    }, function () { /* offline: nothing to reload into */ });
  });
}
