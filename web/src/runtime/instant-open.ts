import { embedded } from './skin';
import { siteRoot } from './install';
import { tripPlace } from './one-machine';
import { findOff } from './find';
import { titleLog } from './title-screen';

let padsNow: typeof navigator.getGamepads;
let net: { saveData?: boolean; effectiveType?: string };
let instantOff: boolean | undefined;
let speculates: boolean;
let instantChosen: { url: string; tag: HTMLScriptElement } | null;
let instantSoon: ReturnType<typeof setTimeout> | 0;
let instantTimer: ReturnType<typeof setTimeout> | 0;
let instantArm: (e: Event) => void;

const INSTANT_HOLD = 120;   // ms of pointing (or focus) before a page is warmed, so a sweep across the cards fetches nothing

function speculate() {
  var base = new URL(siteRoot).pathname;
  function page(path: string) { return { href_matches: { pathname: base + path, baseURL: location.href } }; }
  var rules = { prerender: [{
    source: 'document',
    where: { and: [
      { or: [page(':app/index.html'), page(':app/then.html'), page('index.html'), page(''), page('story.html')] },
      { not: { href_matches: { pathname: location.pathname, baseURL: location.href } } },   // this page, ?watch or #source
      // the player's own pages, the games that start playing at once, and the gallery links that play in the player
      { not: { selector_matches: '[href*="embed"], [href*="watch"], [download], [target="_blank"], #apps a.play, #apps a.thumb' } }
    ] },
    eagerness: 'moderate'   // on hover (about 200 ms) or pointer down, the same as a tap on a phone
  }] };
  var tag = document.createElement('script');
  tag.type = 'speculationrules';
  tag.textContent = JSON.stringify(rules);
  document.head.appendChild(tag);
}

// Warm: an app page's HTML, its program and the files its <body data-files> lists, into the HTTP cache (and the
// service worker's), each once.
const warmed: Record<string, Promise<string | ArrayBuffer | null> | undefined> = {};

function warmFile(url: string): Promise<string | ArrayBuffer | null> {
  if (warmed[url]) return warmed[url];
  warmed[url] = fetch(url, { priority: 'low' } as RequestInit).then(function (r): Promise<string | ArrayBuffer> {
    if (!r.ok) throw new Error(r.status as unknown as string);
    return r.headers.get('Content-Type') && /html/.test(r.headers.get('Content-Type')!) ? r.text() : r.arrayBuffer();
  }).catch(function () { delete warmed[url]; return null; });
  return warmed[url]!;
}

function warmApp(id: string, then: boolean) {
  var dir = siteRoot + id + '/';
  warmFile(dir + (then ? 'then.js' : 'classes.js'));
  warmFile(dir + (then ? 'then.html' : 'index.html')).then(function (html) {
    if (typeof html !== 'string') return;
    var m = /<body[^>]*\sdata-files="([^"]*)"/.exec(html);
    if (m) m[1].split(/\s+/).filter(Boolean).forEach(function (f) { warmFile(dir + f); });
  });
}

// What a pointed-at element would open: { app, then, player } for a link to an app page, or for one of the home
// page's player controls (its rail, whose buttons follow the gallery's order, and the gallery's Play and picture
// links, which play in the player), whose app is warmed rather than prerendered.
function instantTarget(el: Element | null): { app: string; then: boolean; player: boolean } | null {
  if (!el || !el.closest) return null;
  var rail = el.closest('#rail button');
  if (rail) {
    var li = document.querySelectorAll('#apps li[data-app]')[([] as Element[]).indexOf.call(document.querySelectorAll('#rail button'), rail)];
    return li ? { app: li.getAttribute('data-app')!, then: false, player: true } : null;
  }
  var a = el.closest<HTMLAnchorElement>('a[href]');
  var place = a && !a.hasAttribute('download') && tripPlace(a.href);
  if (!place || !place[0]) return null;
  var here = tripPlace(location.href);
  if (here && here[0] === place[0] && here[1] === place[1]) return null;
  return { app: place[0], then: place[1], player: !!a!.closest('#apps a.play, #apps a.thumb') };
}

function prerenderOne(url: string) {
  if (instantChosen && instantChosen.url === url) return;
  if (instantChosen) instantChosen.tag.remove();
  var tag = document.createElement('script');
  tag.type = 'speculationrules';
  tag.textContent = JSON.stringify({ prerender: [{ source: 'list', urls: [url] }] });
  document.head.appendChild(tag);
  instantChosen = { url: url, tag: tag };
}

// A page that was prerendered says so in its title screen's boot log once the visitor opens it.
function headStart() {
  var nav = performance.getEntriesByType && (performance.getEntriesByType('navigation')[0] as (PerformanceNavigationTiming & { activationStart: number }) | undefined);
  var ahead = (nav && nav.activationStart) as number;
  if (!(ahead > 0) || !titleLog) return;
  titleLog(['Loaded ahead, while you chose it', document.querySelector('.shim-title-play:not([disabled])') ? 'no wait'
    : (ahead < 1000 ? Math.round(ahead) + ' ms' : (ahead / 1000).toFixed(1) + ' s') + ' head start', 'ready']);
}

export function init() {
 if (document.prerendering && navigator.getGamepads) {
    padsNow = navigator.getGamepads;
    navigator.getGamepads = function () { return document.prerendering ? [] : padsNow.call(navigator); };
  }
  net = navigator.connection || {};
  instantOff = embedded || findOff || net.saveData || /(^|-)2g$/.test(net.effectiveType || '');
  speculates = !!(window.HTMLScriptElement && HTMLScriptElement.supports && HTMLScriptElement.supports('speculationrules'));
  // One page chosen ahead by the page itself (Find's highlighted result): a list rule of its own, replaced by the
  // next choice, which cancels the last one's prerender.
  instantChosen = null;
  instantSoon = 0;
  // The site's other scripts (find.js) get an app ready with this: on its own page, or in the home page's player.
  window.oldjavaSite.ready = function (id, then, player) {
    if (instantOff) return;
    clearTimeout(instantSoon);
    instantSoon = setTimeout(function () {
      if (speculates && !player) prerenderOne(siteRoot + id + (then ? '/then.html' : '/index.html'));
      else warmApp(id, then);
    }, INSTANT_HOLD);
  };
 if (!instantOff) {
    if (speculates) speculate();
    instantTimer = 0;
    instantArm = function (e) {
      var t = instantTarget(e.target as Element | null);
      if (!t || (speculates && !t.player)) return;   // the speculation rules prerender it
      clearTimeout(instantTimer);
      if (e.type === 'pointerdown') warmApp(t.app, t.then);
      else instantTimer = setTimeout(function () { warmApp(t!.app, t!.then); }, INSTANT_HOLD);
    };
    document.addEventListener('pointerover', instantArm, true);
    document.addEventListener('focusin', instantArm, true);
    document.addEventListener('pointerdown', instantArm, { capture: true, passive: true });
    document.addEventListener('pointerout', function () { clearTimeout(instantTimer); }, true);
  }
  if (document.prerendering) document.addEventListener('prerenderingchange', headStart, { once: true });
}
