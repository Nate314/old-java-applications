import { embedded } from './skin';
import { siteRoot } from './install';
import { catalog, type CatalogApp } from './catalog';
import { indexOf } from './then-and-now';

/** [app id or '' for the home page, then?] */
export type TripPlace = [string, boolean];

let tripClick: { el: Element | null; t: number };
let tripApps: CatalogApp[] | null;
let tripUnname: (() => void) | null;

// ---- one machine: page-to-page transitions and prefetch ----
// Going from one page of the site to another used to be a blank page and a fresh start. Now the browser's
// cross-document view transitions (shim.css opts in with @view-transition) carry the visitor across: the thumbnail
// they clicked (a Switch app card, an exit suggestion) or the home page's player grows into the
// new app's title card, Next and Previous slide the stage sideways under a toolbar band that stays put, Then and Now
// fade through sepia, All apps shrinks the stage back into the home page's player, and in Arcade every change is a
// CRT changing channel. The old page says what kind of trip it was (TRIP_KEY in sessionStorage), the new page plays
// it. Pages in the player (?embed) and the screen saver never animate their own navigations.
const TRIP_KEY = 'oldjava.trip';

// [app id or '' for the home page, then?] of a site address, or null for anything else.
export function tripPlace(url: string | URL): TripPlace | null {
  var u = new URL(url, location.href);
  if (u.origin !== location.origin || u.pathname.indexOf(new URL(siteRoot).pathname) !== 0) return null;
  var rest = u.pathname.slice(new URL(siteRoot).pathname.length);
  if (rest === '' || rest === 'index.html') return ['', false];
  var m = /^([A-Za-z0-9]+)\/(index|then)\.html$/.exec(rest);
  return m ? [m[1], m[2] === 'then'] : null;
}

function tripKind(from: TripPlace, to: TripPlace) {
  if (!to[0]) return 'home';
  if (from[0] === to[0]) return from[1] === to[1] ? 'app' : 'era';
  if (from[0] && tripApps) {
    var i = indexOf(tripApps, from[0]), j = indexOf(tripApps, to[0]), n = tripApps.length;
    if (i >= 0 && j >= 0 && (i + 1) % n === j) return 'forward';
    if (i >= 0 && j >= 0 && (j + 1) % n === i) return 'back';
  }
  return 'app';
}

function inView(el: Element | null) {
  if (!el || !el.isConnected) return false;
  var r = el.getBoundingClientRect();
  return r.width > 0 && r.height > 0 && r.bottom > 0 && r.right > 0 && r.top < innerHeight && r.left < innerWidth;
}

// The element that becomes (or came from) the other page: an app page's title card or its stage, the home page's
// player screen.
function tripStage() {
  if (!document.body) return null;
  if (!document.body.hasAttribute('data-app')) return document.querySelector('#player .screen');
  return document.querySelector('.shim-title-card') || document.getElementById('desktop');
}

function tripName(el: Element) {
  (el as HTMLElement).style.viewTransitionName = 'oldjava-hero';
  return function () { (el as HTMLElement).style.viewTransitionName = ''; };
}

export function init() {
  tripClick = { el: null, t: 0 };
  document.addEventListener('click', function (e) {
    var a = e.target && (e.target as Element).closest && (e.target as Element).closest('a[href]');
    if (a) tripClick = { el: a, t: Date.now() };
  }, true);
  tripApps = null;   // the catalog, once read, so pageswap can place the next page synchronously
  document.addEventListener('DOMContentLoaded', function () { catalog().then(function (apps: CatalogApp[]) { tripApps = apps; }); });
  window.addEventListener('pageswap', function (e) {
    if (!e.viewTransition) return;
    var to = e.activation && e.activation.entry && tripPlace(e.activation.entry.url!);
    var from = tripPlace(location.href);
    if (embedded || !to || !from) { e.viewTransition.skipTransition(); return; }
    var kind = tripKind(from, to), hero = null;
    // Arcade changes the channel instead; a slide or a sepia fade moves the whole stage.
    if (document.documentElement.getAttribute('data-skin') !== 'arcade' && (kind === 'app' || kind === 'home')) {
      var clicked = Date.now() - tripClick.t < 2000 ? tripClick.el : null;
      var pic = clicked && clicked.querySelector('img');
      if (kind === 'app' && inView(pic)) hero = pic;
      else if (kind === 'home' || (clicked && clicked.closest('#player'))) hero = tripStage();
      if (!inView(hero)) hero = null;
    }
    if (hero) tripUnname = tripName(hero);   // taken off again if this page comes back (bfcache)
    try { sessionStorage.setItem(TRIP_KEY, JSON.stringify({ kind: kind, hero: !!hero, t: Date.now() })); } catch (err) { /* plain fade */ }
  });
  tripUnname = null;
  window.addEventListener('pagereveal', function (e) {
    if (tripUnname) tripUnname();
    tripUnname = null;
    if (!e.viewTransition) return;
    if (embedded) { e.viewTransition.skipTransition(); return; }
    var trip = null;
    try {
      trip = JSON.parse(sessionStorage.getItem(TRIP_KEY)!);
      sessionStorage.removeItem(TRIP_KEY);
    } catch (err) { /* a plain fade */ }
    if (!trip || Date.now() - trip.t > 10000) trip = { kind: 'app', hero: false };
    var vt = e.viewTransition;
    try { vt.types.add(trip.kind); } catch (err) { /* no types: the default fade */ }
    if (!trip.hero) return;
    var unname: (() => void) | null = null;
    function name() {
      var el = tripStage();
      if (!unname && inView(el)) unname = tripName(el!);
    }
    // The home page's player is parsed by now (its <link rel="expect"> holds the first frame for it); an app page's
    // title card is made on DOMContentLoaded, which may come before or after this event.
    name();
    if (!unname && document.readyState === 'loading') document.addEventListener('DOMContentLoaded', name);
    vt.finished.finally(function () { if (unname) unname(); });
  });
}
