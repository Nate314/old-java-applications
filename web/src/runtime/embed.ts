import { siteRoot } from './install';
import { WATCH } from './watch';
import { openShare, shareBox } from './share';
import { playSound } from './sounds';
import type { CatalogApp } from './catalog';

declare global {
  interface OldjavaShare {
    // Set by init: opens the Share panel with "Put it on your site" already open.
    embed?(id: string | null, opener?: Element): void;
  }
}

let embedPromise: Promise<NonNullable<Window['oldjavaEmbed']>> | null;
let embedOpenNext: boolean;

function embedScript() {
  if (!embedPromise) embedPromise = new Promise(function (resolve, reject) {
    import('./lazy/embed').then(function () { resolve(window.oldjavaEmbed!); }, function () { embedPromise = null; reject(new Error('embed.js did not load')); });
  });
  return embedPromise;
}

export function embedSection(id: string | null, apps: CatalogApp[]) {
  var box = document.createElement('details');
  box.className = 'shim-embed';
  var summary = document.createElement('summary');
  summary.innerHTML = '<span aria-hidden="true">&lt;/&gt;</span> Put it on your site';
  summary.setAttribute('data-sound', 'none');
  box.appendChild(summary);
  var built = false;
  if (embedOpenNext) { embedOpenNext = false; box.open = true; }   // Find's "Put an app on your site"
  box.addEventListener('toggle', function () {
    if (!box.open || built) return;
    built = true;
    playSound('open');
    embedScript().then(function (embed) {
      var inner = document.createElement('div');
      inner.className = 'shim-embed-body';
      box.appendChild(inner);
      embed.build(inner, { apps: apps, app: id, siteRoot: siteRoot, watchable: Object.keys(WATCH), sound: playSound });
      box.scrollIntoView({ block: 'nearest' });
    }, function (e) { built = false; console.warn(e.message); });
  });
  return box;
}

export function init() {
  // ---- embed: put an app on your own site (play.html, built by embed.js) ----
  // The Share panel ends with "Put it on your site", closed: opening it adds embed.js, which builds the
  // <iframe> tag for play.html (the embeddable player) with a live preview. See web/README.md, Embed.
  embedPromise = null;
  embedOpenNext = false;
  // Opens the Share panel with "Put it on your site" already open (Find uses it).
  window.oldjavaShare.embed = function (id, opener) {
    if (shareBox) return;
    embedOpenNext = true;
    openShare(id, opener);
  };
}
