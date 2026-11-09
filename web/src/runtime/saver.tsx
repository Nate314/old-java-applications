import { useLayoutEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { embedded } from './skin';
import { openLayer } from './ui/layer';
import { siteRoot } from './install';
import { news } from './band-news';
import { floatToast, toastLink } from './passport';
import { catalog } from './catalog';
import { indexOf, pageOf } from './then-and-now';

declare global {
  interface OldjavaSaver {
    after: number;
    input(e: PointerEvent): boolean;
    show(): void;
    wake(): void;
  }
}

let saver: OldjavaSaver | null;
/** The saver on screen: fade() takes its on class away, faded() marks it faded, close() takes it out. */
let saverBox: { fade(): void; faded(): void; isFaded: boolean; close(): void } | null;
let saverPrev: HTMLElement | null;
let saverDown: boolean;
let saverFading: boolean;
let saverBase: number[] | null;
let saverPads: string;
let saverLast: number;

// ---- the site's screen saver ----
// Screen Saver is one of the apps, and it is also the site's own screen saver: when a page (the home page, an
// app page) has had no input for saver.after (3 minutes), Screen Saver fades in over everything, in an iframe of
// its page in saver mode (?embed&saver: no title screen, stamp or replay, as in thumbnail mode). It shares the
// app's saves, so it shows the saver the visitor left the app on, cycling or not, at their speed. The first key
// press, click, touch, real mouse move or controller press fades it out again; it never reaches the app beneath.
// Then the page says what that was, with a link to pick a saver and a way to turn it off (localStorage
// oldjava.saver = 'off'); Screen Saver's title screen turns it back on. It waits while a page is hidden or in
// fullscreen, while the computer plays (watch), while the app is popped out or a video plays, and on the apps
// that are read or watched without touching them (SAVER_SKIP).
const SAVER_KEY = 'oldjava.saver';

const SAVER_SKIP: Record<string, boolean> = { ScreenSaver: true, BibleDownloader: true, Clock: true };

export function saverOn() {
  try { return localStorage.getItem(SAVER_KEY) !== 'off'; } catch (e) { return true; }
}

export function setSaverOn(on: boolean) {
  try { localStorage.setItem(SAVER_KEY, on ? 'on' : 'off'); } catch (e) { /* this visit only */ }
}

export function saverHost() {
  if (!embedded) return saver;
  try { return window.parent !== window && window.parent.oldjavaSaver || null; } catch (e) { return null; }
}

export function saverMinutes() {
  var host = saverHost();
  return Math.round((host ? host.after : 180000) / 60000);
}

// Every page, the player's and the saver's frames included: input counts as activity, and input that woke the
// saver (or fell on it) is kept from the app, its key's release too.
const saverHeld: Record<string, boolean> = {};

// The document of the app the visitor sees: this app page's own, or the home page player's.
function shownAppDoc() {
  if (document.body.hasAttribute('data-app')) return document;
  var frame = document.getElementById('player-frame') as HTMLIFrameElement | null;
  try { return frame && frame.contentDocument; } catch (e) { return null; }
}

function saverWanted() {
  if (!saverOn() || !document.body || document.visibilityState !== 'visible' || document.fullscreenElement) return false;
  if (document.documentElement.hasAttribute('data-touring')) return false;   // the home page's tour is showing the apps
  var doc = shownAppDoc();
  if (!doc || !doc.body) return true;
  if (SAVER_SKIP[doc.body.getAttribute('data-app') as string] || doc.documentElement.hasAttribute('data-popped')) return false;
  if (doc.querySelector('.shim-watch:not(.gone)')) return false;
  return ![document, doc].some(function (d) {
    return [].some.call(d.querySelectorAll('video'), function (v: HTMLVideoElement) { return !v.paused && !v.ended; });
  });
}

function showSaver() {
  if (saverBox || !document.body) return;
  saverPrev = document.activeElement as HTMLElement | null;
  saverBase = null;
  saverFading = false;
  var set!: (s: { on: boolean; faded: boolean }) => void;
  const close = openLayer(<SaverBox onMount={s => { set = s; }} />);
  var box = saverBox = {
    fade: function () { set({ on: false, faded: false }); },
    faded: function () { box.isFaded = true; set({ on: false, faded: true }); },
    isFaded: false as boolean,
    close: close,
  };
  requestAnimationFrame(function () { requestAnimationFrame(function () { if (saverBox === box) set({ on: true, faded: false }); }); });
}

/** The Screen Saver app over the whole page, fading in once it is there (on) and out again. */
function SaverBox({ onMount }: { onMount: (set: (s: { on: boolean; faded: boolean }) => void) => void }) {
  const [s, setS] = useState({ on: false, faded: false });
  useLayoutEffect(() => { onMount(v => flushSync(() => setS(v))); }, []);
  return (
    <div className={'shim-saver' + (s.on ? ' on' : '')} data-faded={s.faded ? '' : undefined}>
      <iframe src={siteRoot + 'ScreenSaver/index.html?embed&saver'} title="Screen Saver: press a key or move the mouse to come back" tabIndex={-1} />
    </div>
  );
}

function wakeSaver() {
  saverFading = true;
  saverBox!.fade();
  var box = saverBox!;
  setTimeout(function () {
    if (saverBox !== box) return;
    box.faded();
    saverDone();
  }, 400);
}

// Gone once it has faded and no button is held (the release would otherwise land on the app beneath).
function saverDone() {
  if (!saverBox || !saverBox.isFaded || saverDown) return;
  saverBox.close();
  saverBox = null;
  saverFading = false;
  saverLast = Date.now();
  if (saverPrev && saverPrev.isConnected && saverPrev !== document.body && saverPrev.focus) saverPrev.focus({ preventScroll: true });
  welcomeBack();
}

// What that was, where to pick another saver, and how to turn it off: in the band on an app page, a toast on the
// home page.
function welcomeBack() {
  catalog().then(function (apps: OldjavaCatalogApp[]) {
    var app = apps[indexOf(apps, 'ScreenSaver')];
    if (!app) return;
    var title = 'Welcome back! ';
    var line = 'That was ' + app.name + ', one of the ' + apps.length + ' apps here, minding the screen while you were away.';
    function off() {
      setSaverOn(false);
      var said = 'Screen saver off. ', how = app.name + '’s title screen turns it back on.';
      if (news) news.show('saver', null, <><strong>{said}</strong><span>{how}</span></>, null, 6000);
      else floatToast(app.shot, said, how, null, function () { return null; });
    }
    function offButton(cls: string, close: () => void) {
      return <button type="button" className={cls} onClick={() => { close(); off(); }}>Turn it off</button>;
    }
    if (news) {
      news.show('saver', <span className="shim-news-seal"><img src={app.shot} alt="" /></span>, <><strong>{title}</strong><span>{line}</span></>,
        <><a className="shim-news-go" href={pageOf(app)}>{'Pick your saver ›'}</a>{offButton('shim-news-go', news.clear)}</>, 10000);
      return;
    }
    floatToast(app.shot, title, line, null, function (close) {
      return <>{toastLink(close, app, 'Pick your saver ›')}{offButton('shim-toast-go', close)}</>;
    });
  });
}

export function init() {
  // data-saver (?embed&saver) is set before first paint, with the skin (paintSkin in skin.ts).
  saver = null;   // a top page's saver (window.oldjavaSaver); embedded pages hand their input to the parent's
  ['keydown', 'keyup', 'pointerdown', 'pointerup', 'pointercancel', 'pointermove', 'wheel'].forEach(function (type) {
    window.addEventListener(type, function (e) {
      if (!e.isTrusted) return;
      var host = saverHost();
      var mine = !!host && host.input(e as PointerEvent);
      if (e.type === 'keyup' && saverHeld[(e as KeyboardEvent).code]) { delete saverHeld[(e as KeyboardEvent).code]; mine = true; }
      if (!mine) return;
      if (e.type === 'keydown') saverHeld[(e as KeyboardEvent).code] = true;
      if (e.type === 'keydown' || e.type === 'pointerdown') e.preventDefault();
      e.stopImmediatePropagation();
    }, true);
  });
 if (!embedded) {
    saverBox = null; saverPrev = null; saverDown = false; saverFading = false; saverBase = null; saverPads = '';
    saverLast = Date.now();
    saver = window.oldjavaSaver = {
      after: 3 * 60 * 1000,
      // An input event from this page or a frame in it; true when it belongs to the saver.
      input: function (e) {
        var t = e.type;
        if (t === 'pointerdown') saverDown = true;
        if (t === 'pointerup' || t === 'pointercancel') saverDown = false;
        if (!saverBox) { saverLast = Date.now(); return false; }
        if (saverFading) { if (!saverDown) saverDone(); return true; }
        if (t === 'pointermove') {
          // Showing the saver can send a move from a mouse that has not moved; a real one goes somewhere.
          if (!saverBase) { saverBase = [e.screenX, e.screenY]; return true; }
          if (Math.abs(e.screenX - saverBase[0]) + Math.abs(e.screenY - saverBase[1]) < 12) return true;
        }
        if (t !== 'pointerup' && t !== 'pointercancel') wakeSaver();
        return true;
      },
      show: showSaver,
      wake: function () { if (saverBox && !saverFading) wakeSaver(); },
      showing: function () { return !!saverBox; }
    };
    document.addEventListener('visibilitychange', function () { saverLast = Date.now(); });
    setInterval(function () {
      // A controller counts as input too: the Gamepad API stamps each pad with the time its state last changed.
      var pads = navigator.getGamepads ? [].slice.call(navigator.getGamepads()).map(function (p: Gamepad | null) { return p ? p.timestamp : 0; }).join() : '';
      if (pads !== saverPads) {
        saverPads = pads;
        saverLast = Date.now();
        if (saverBox && !saverFading) wakeSaver();
      }
      if (saverBox) return;
      if (!saverWanted()) { saverLast = Date.now(); return; }   // its minutes start when the reason ends
      if (Date.now() - saverLast >= saver!.after) showSaver();
    }, 1000);
  }
}
