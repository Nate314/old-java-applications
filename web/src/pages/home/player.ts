// The player: "Play" swaps the app in the iframe instead of leaving the page. #<App> in the address picks
// the app on load, so a link can point straight at one. Ctrl/Shift/middle clicks still open the full page.
// Also the passport, Arcade mode, Fit and the tour, which all drive the player. What they show is in H (model.ts);
// Home.tsx draws it, and its controls call the functions here.
import type { MouseEvent as ReactMouseEvent, SyntheticEvent } from 'react';
import { APPS, type App } from '../../shared/catalog';
import { byId } from '../../shared/dom';
import { H, draw, indexOfApp } from './model';
import { TOUR } from './tour';

const MIN_WIDTH = 900;
const MIN_HEIGHT = 520;
const MIN_SCREEN = 320;   // shortest the screen gets on a short browser window, in page pixels
const ASPECT = 1.6;
// Demo: until the visitor picks an app or takes over, the player shows the games that play themselves
// (the runtime's watch, ?watch), the computer at the controls, a new one every REEL_MS while the player is in view.
const REEL_MS = 40000;
// A tour stop moves on by itself after TOUR_MS unless the visitor touches the app.
export const TOUR_MS = 30000;
const TOUR_KEY = 'oldjava.tour';
// A controller's chart stays in the hint this long (the runtime's gamepads), then the app's hint comes back.
const CHART_MS = 15000;

interface TourSaved { stop?: number; done?: boolean }

const player = () => byId('player');
const screen = () => byId('screen');
const frame = () => byId<HTMLIFrameElement>('player-frame');
const appById = (id: string | null | undefined) => APPS.find(a => a.id === id) || null;
// Every caller below runs after the player has an app (the page always starts with one).
const playing = () => H.current as App;

// The screen is 16:10 but no taller than the browser window leaves room for (so the whole app is in view
// under the player bar). The iframe gets a viewport of at least MIN_WIDTH x MIN_HEIGHT (more for apps
// with minHeight, like Rubik's Cube's 700 px window) and is scaled down to the screen.
// On the big screen it takes whatever room the bar and the rail leave.
export function isBig() {
  const p = document.getElementById('player');   // (none yet while the page is first drawn)
  return (!!p && document.fullscreenElement === p) || H.big;
}

// A phone (the pocket player, see the page's style) gives the iframe the screen's own size instead: scaled
// down from 900 px, its apps were a third of their size (Minesweeper's tiles 7 px wide), while the app's
// page fits its windows to a phone-sized screen itself, as it does when opened on a phone.
const pocket = matchMedia('(max-width: 640px)');
let fitted = [0, 0];   // the window's size at the last fit()
let firstView = true;  // no app picked yet

export function fit() {
  const current = H.current;
  const minHeight = Math.max(MIN_HEIGHT, current ? current.minHeight || 0 : 0);
  const width = screen().clientWidth;
  let height: number;
  fitted = [window.innerWidth, window.innerHeight];
  if (isBig()) {
    H.screenH = null;
    draw();
    height = screen().clientHeight;
  } else {
    // Room for the bar and the rail too, so the whole player fits in the window. A phone's screen is as
    // tall as that room, not 16:10, since the phone is taller than wide.
    const around = player().offsetHeight - screen().offsetHeight + (pocket.matches ? 12 : 24);
    let room = window.innerHeight - around;
    // Until the visitor picks an app (which scrolls the player to the top), a phone's screen ends where the
    // first view does, under the title, so the app that is already running is seen whole.
    if (pocket.matches && firstView) room = Math.min(room, window.innerHeight - 12 - (screen().getBoundingClientRect().top + window.scrollY));
    room = Math.max(MIN_SCREEN, room);
    height = Math.round(pocket.matches ? room : Math.min(width / ASPECT, room));
  }
  // (CircleSpawner places its own 380 px window, so its card asks for a wider screen with minWidth.)
  const scale = pocket.matches ? Math.min(1, width / (current ? current.minWidth || 0 : 0))
    : Math.min(1, width / MIN_WIDTH, height / minHeight);
  H.frame.size = { w: Math.round(width / scale), h: Math.round(height / scale), scale };
  H.screenH = height;
  draw();
}

function mark(a: App) {
  H.current = a;
  draw();
  // Scroll only the rail (scrollIntoView would also scroll the page).
  const rail = byId('rail');
  const b = rail.querySelectorAll<HTMLElement>('button')[indexOfApp(a.id)];
  const left = b.offsetLeft - rail.offsetLeft;
  if (left < rail.scrollLeft || left + b.offsetWidth > rail.scrollLeft + rail.clientWidth) {
    rail.scrollTo({ left: left - (rail.clientWidth - b.offsetWidth) / 2, behavior: 'smooth' });
  }
}

let demo = false;
let reelTimer = 0;
function reel() {
  clearTimeout(reelTimer);
  if (!demo) return;
  reelTimer = window.setTimeout(() => {
    const box = screen().getBoundingClientRect();
    if (document.hidden || box.bottom < 0 || box.top > window.innerHeight) { reel(); return; }
    for (let k = 1; k < APPS.length; k++) {
      const a = APPS[(indexOfApp(playing().id) + k) % APPS.length];
      if (window.oldjavaWatch.has(a.id)) { play(a, false, false, true); return; }
    }
  }, REEL_MS);
}
function endDemo() {
  demo = false;
  clearTimeout(reelTimer);
}

/** The page the player loads for app a, as it plays now (then: as first restored). */
export function pageOf(a: App) { return a.id + (H.then ? '/then.html' : '/index.html'); }

function play(a: App, focus: boolean, asThen?: boolean, watch?: boolean) {
  demo = !!watch;
  reel();
  mark(a);
  H.played = a;
  H.then = !!asThen && !!a.then;
  H.hintOpen = false;
  H.chart = null;
  H.quirk = null;
  // As first restored: its first known quirk (if its README still listed any), and a link to its page, whose title screen lists them all.
  if (H.then) window.oldjavaThen.quirks(a.id).then(items => {
    if (!H.then || H.current !== a || !items.length) return;
    H.quirk = { app: a.id, items: items as string[][] };
    draw();
  });
  showPad(a.id);
  fit();
  // A new iframe each time, so the old app's timers and audio stop with it. A popped out app comes back
  // first, so its window closes with it.
  const api = popApi();
  if (api) api.back();
  H.replay = false;   // until the new app's page says it is recording
  H.photo = false;    // until the new app's page says it runs (the runtime's photo booth)
  H.frame = { ...H.frame, key: H.frame.key + 1, src: pageOf(a) + '?embed' + (watch ? '&watch' : ''), title: a.name, focus };
  draw();
  syncPop();
  if (focus) {
    window.oldjavaSound.play('start');
    if (!isBig()) player().scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

/** The player's iframe has loaded a page. */
export function frameLoaded() {
  syncPop();
  if (H.frame.focus) frame().focus();
}

// Pop out: the app in the player moves, still running, into a small window of its own that stays on top of
// the visitor's other windows (the runtime's popOut). The player's iframe may not open that window, so this page
// opens it and hands it to the app's page.
function popApi(): OldjavaPop | null {
  try { return frame().contentWindow?.oldjavaPop || null; } catch (e) { return null; }
}
function syncPop() {
  const api = popApi();
  H.pop = { shown: !(!('documentPictureInPicture' in window) || window.top !== window || !api || !api.can()), on: !!api && api.popped(), synced: true };
  draw();
}
export function popClick() {
  const api = popApi();
  if (!api) return;
  if (api.popped()) {
    api.back();
    syncPop();
    return;
  }
  documentPictureInPicture.requestWindow(api.size()).then(win => {
    win.addEventListener('pagehide', () => { setTimeout(syncPop, 0); });
    api.into(win);
    syncPop();
  }, e => { console.warn('Pop out:', e); });
}

export function choose(a: App, asThen?: boolean) {
  if (H.tour.on && a !== H.current) tourEnd();   // picking another app leaves the tour (it can be continued)
  firstView = false;
  history.replaceState(null, '', '#' + a.id + (asThen ? ':then' : ''));
  play(a, true, asThen);
}
export function thenClick() { if (!H.then) choose(playing(), true); }
export function nowClick() { if (H.then) choose(playing(), false); }

function step(by: number) { choose(APPS[(indexOfApp(playing().id) + by + APPS.length) % APPS.length]); }
export function prev() { step(-1); }
export function next() { step(1); }
export function shuffle() { step(1 + Math.floor(Math.random() * (APPS.length - 1))); }   // any app but the current one
export function restart() { play(playing(), true, H.then); }

/** A gallery card's Play or picture: swap the app in, unless the click asks for the full page. */
export function cardClick(e: ReactMouseEvent, a: App) {
  if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
  e.preventDefault();
  choose(a);
}

// Big screen: real fullscreen where the browser allows it, else the player covers the window.
function setBig(on: boolean) {
  H.big = on;
  document.body.classList.toggle('locked', on);
  draw();
  fit();
}
export function bigClick() {
  if (document.fullscreenElement) { document.exitFullscreen(); return; }
  if (H.big) { setBig(false); return; }
  const p = player();
  if (p.requestFullscreen) p.requestFullscreen().catch(() => setBig(true));
  else setBig(true);
}

export function filterClick(f: string) {
  H.filter = f;
  draw();
}

// The keyboard games get a controller docked under the screen (the runtime builds it). Its presses go into
// the player's iframe; real key presses there come back to light its buttons. Shown unless turned off.
let padLight: ((which: number, down: boolean) => void) | null = null;
function showPad(id: string) {
  const pad = window.oldjavaPad;
  const has = pad.has(id);
  H.pad = { called: true, has, shown: has && pad.wanted(true), drawn: H.pad.drawn || has };
  draw();
  padLight = has ? pad.render(byId('player-pad'), id, (type, key) => {
    // A press on the controller takes over from the computer first (the runtime's watch).
    if (demo) {
      endDemo();
      frame().contentWindow?.postMessage({ oldjavaWatchTake: true }, location.origin);
    }
    frame().contentWindow?.postMessage({ oldjavaKey: { type, name: key } }, location.origin);
  }) : null;
}
export function padToggle() {
  H.pad = { ...H.pad, shown: !H.pad.shown };
  draw();
  window.oldjavaPad.remember(H.pad.shown);
  fit();
  frame().focus();
}

// Instant replay: the player's app keeps its last moments (the runtime's startReplay); this shows them here.
export function replayClick(button: HTMLElement) {
  let api: OldjavaReplay | undefined;
  try { api = frame().contentWindow?.oldjavaReplay; } catch (e) { /* not loaded */ }
  if (api) api.take().then(clip => { if (clip) window.oldjavaReplay.open(clip, button); });
}
// Photo: the player's app draws its windows (the runtime's takePhoto in the iframe); the print shows on this page.
export function photoClick(button: HTMLElement) {
  window.oldjavaPhoto.snap(() => {
    let api: OldjavaPhoto | undefined;
    try { api = frame().contentWindow?.oldjavaPhoto; } catch (e) { /* not loaded */ }
    return api && api.can() ? api.take() : Promise.resolve(null);
  }, screen(), button);
}
export function shareClick(button: HTMLElement) { window.oldjavaShare.open(playing().id, button); }

// ---- the passport ----
export function passportNew() {
  const passport = window.oldjavaPassport;
  passport.catalog().then(apps => {
    const n = passport.unplayed(apps, H.current && H.current.id)[0];
    const a = n && appById(n.id);
    if (a) choose(a);
  });
}
// Reset asks once more on the button itself, for 4 seconds.
let resetTimer = 0;
export function resetClick() {
  if (resetTimer) {
    clearTimeout(resetTimer);
    resetTimer = 0;
    H.resetAsk = false;
    draw();
    window.oldjavaPassport.reset();
    return;
  }
  H.resetAsk = true;
  draw();
  resetTimer = window.setTimeout(() => { resetTimer = 0; H.resetAsk = false; draw(); }, 4000);
}

// Fit: small app windows zoom up to fill the player (the runtime's centerWindows, in the player's page).
export function zoomClick(button: HTMLElement) {
  window.oldjavaZoom.set(!window.oldjavaZoom.fit());
  button.blur();
  frame().focus();
}

// Arcade mode: the Arcade skin on this page, its player and every other open page, and back to the
// skin from before. The button follows the skin however it changes (the picker, another tab).
let skinBefore = 'modern';
function syncArcade() {
  if (window.oldjavaSkin.get() !== 'arcade') skinBefore = window.oldjavaSkin.get();
  draw();
}
export function arcadeClick(button: HTMLElement) {
  window.oldjavaSkin.set(window.oldjavaSkin.get() === 'arcade' ? skinBefore : 'arcade');
  button.blur();
  frame().focus();
}

// The pocket bar: two lines of hint until tapped.
export function hintClick(e: ReactMouseEvent) {
  if (pocket.matches && !(e.target as Element).closest('a')) { H.hintOpen = !H.hintOpen; draw(); }
}

// ---- the tour ----
// Take the tour plays all 20 apps in the player, one stop at a time, each with one thing to try.
// The games that play themselves start watched. A stop moves on by itself after TOUR_MS unless the visitor
// touches the app, which hands it over to them: then the tour waits for Next stop. The stop reached is kept
// (oldjava.tour), so the tour can be continued on a later visit, and #tour in the address starts it.
const tour = H.tour;
export function tourSaved(): TourSaved {
  try { return JSON.parse(localStorage.getItem(TOUR_KEY) as string) || {}; } catch (e) { return {}; }
}
function tourSave(o: TourSaved) {
  try { localStorage.setItem(TOUR_KEY, JSON.stringify(o)); } catch (e) { /* private mode: not kept */ }
}
const tourApp = (i: number) => appById(TOUR[i][0]) as App;
function tourShown() {
  tour.rendered = true;
  if (tour.on && !tour.done) document.documentElement.setAttribute('data-touring', '');
  else document.documentElement.removeAttribute('data-touring');
  draw();
}
// The visitor touching the app (a click, key or touch inside the player's page, or the controller)
// hands the stop over to them.
export function tourTouch(e?: Event | SyntheticEvent) {
  if (!tour.on || tour.done || tour.touched || (e && e.isTrusted === false)) return;
  tour.touched = true;
  tourShown();
}
function tourHook() {
  let doc: Document | null = null;
  try { doc = frame().contentDocument; } catch (e) { /* not ours */ }
  if (!doc || doc === tour.doc || doc.readyState !== 'complete' || doc.URL === 'about:blank') return;
  tour.doc = doc;
  ['pointerdown', 'keydown', 'touchstart', 'wheel'].forEach(t => (doc as Document).addEventListener(t, tourTouch, true));
}
function tourTick() {
  tourHook();
  const now = Date.now();
  const dt = now - tour.last;
  tour.last = now;
  if (tour.done || tour.touched) return;
  const box = screen().getBoundingClientRect();
  const api = popApi();
  const saverUp = window.oldjavaSaver && window.oldjavaSaver.showing();
  if (document.visibilityState !== 'visible' || box.bottom < 0 || box.top > window.innerHeight || (api && api.popped()) || saverUp) return;
  tour.spent += dt;
  if (tour.spent >= TOUR_MS) { tourGo(tour.stop + 1, false); return; }
  draw();
}
// Stop i in the player (i === TOUR.length: the end card). byHand: a click, so a start sound may play.
export function tourGo(i: number, byHand: boolean) {
  const first = !tour.on;
  tour.on = true;
  tour.touched = false;
  tour.spent = 0;
  tour.last = Date.now();
  if (i >= TOUR.length) {
    tour.done = true;
    tour.stop = TOUR.length - 1;
    tourSave({ done: true });
    tourShown();
    return;
  }
  tour.done = false;
  tour.stop = Math.max(0, i);
  tourSave({ stop: tour.stop });
  firstView = false;
  clearInterval(tour.timer);
  tour.timer = window.setInterval(tourTick, 250);
  history.replaceState(null, '', '#tour');
  const a = tourApp(tour.stop);
  play(a, false, false, !!(window.oldjavaWatch && window.oldjavaWatch.has(a.id)));
  clearTimeout(reelTimer);   // the tour moves on, not the demo reel
  tourShown();
  fit();
  if (byHand && window.oldjavaSound) window.oldjavaSound.play('start');
  if (first && !isBig()) player().scrollIntoView({ behavior: 'smooth', block: 'start' });
}
function tourEnd() {
  tour.on = false;
  tour.done = false;
  clearInterval(tour.timer);
  tourShown();
  fit();
  if (/^#tour$/.test(location.hash)) history.replaceState(null, '', '#' + playing().id);
}
// Where a new start goes: the stop the last tour reached, or the first once it was finished.
function tourResume() {
  const saved = tourSaved();
  return saved.done || !(saved.stop !== undefined && saved.stop > 0 && saved.stop < TOUR.length) ? 0 : saved.stop;
}
export function tourStartClick() {
  const nextButton = byId('tour-next');
  if (tour.on) { if (!isBig()) player().scrollIntoView({ behavior: 'smooth', block: 'start' }); nextButton.focus({ preventScroll: true }); return; }
  tourGo(tourResume(), true);
  nextButton.focus({ preventScroll: true });
}
export function tourNextClick() { tourGo(tour.stop + 1, true); }
export function tourBackClick() { tourGo(tour.done ? TOUR.length - 1 : tour.stop - 1, true); }
export function tourEndClick() { tourEnd(); }
export function tourUnplayedClick() { byId('passport-new').click(); }

export function initPlayer(): void {
  const passport = window.oldjavaPassport;
  document.addEventListener('fullscreenchange', () => { setBig(false); frame().focus(); });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && H.big) setBig(false);
  });

  // An app's exit screen in the player suggests other apps; picking one asks this page to swap it in.
  window.addEventListener('message', e => {
    if (e.origin !== location.origin || e.source !== frame().contentWindow || !e.data) return;
    const lit = e.data.oldjavaKeyLit;
    if (lit) { if (padLight) padLight(lit.which, !!lit.down); return; }
    if (typeof e.data.oldjavaShare === 'string') { window.oldjavaShare.open(e.data.oldjavaShare || null, frame()); return; }
    if (e.data.oldjavaReplay) { H.replay = true; draw(); return; }
    if (e.data.oldjavaPhoto === true) { H.photo = true; draw(); return; }
    if (e.data.oldjavaClip) { window.oldjavaReplay.open(e.data.oldjavaClip, frame()); return; }
    if (e.data.oldjavaWatch === 'taken') { endDemo(); return; }
    const a = appById(e.data.oldjavaPlay);
    if (a) choose(a);
  });

  // The arcade passport: a stamp per app played, here, in the player or on an app page (the runtime keeps them).
  passport.onChange(fresh => { H.fresh = fresh; draw(); });
  passport.open = id => { const a = appById(id); if (a) choose(a); };
  window.oldjavaZoom.onChange(() => draw());
  new MutationObserver(syncArcade).observe(document.documentElement, { attributes: true, attributeFilter: ['data-skin'] });
  syncArcade();

  // Phone browsers resize the window as their address bar slides in and out while the page scrolls; refitting
  // then would resize the app under the visitor's finger, so a phone refits only for a real change.
  window.addEventListener('resize', () => {
    if (pocket.matches && !isBig() && window.innerWidth === fitted[0] && Math.abs(window.innerHeight - fitted[1]) < 160) return;
    fit();
  });

  // A controller's buttons in the hint, for a while (the runtime's gamepads, when a controller is used here).
  window.oldjavaPlayerHint = (parts: string[][]) => {
    H.chart = parts;
    draw();
    setTimeout(() => { if (H.chart === parts) { H.chart = null; draw(); } }, CHART_MS);
  };
  window.oldjavaTour = { start: tourStartClick, on: () => tour.on };

  const hash = /^#([^:]*)(:then)?$/.exec(decodeURIComponent(location.hash)) || [];
  const start = appById(hash[1]);
  if (start) play(start, false, !!hash[2]);
  else if (hash[1] === 'tour') {   // #tour: the tour, from where it was left
    tourGo(tourResume(), false);
  } else { mark(appById('Minesweeper') as App); fit(); demo = true; reel(); }   // the page's own frame: Minesweeper, watched
}
