import { useState } from 'react';
import { appId } from './catalog';
import { store, stored } from './stored';
import { embedded } from './skin';

// ---- Slow-mo: the real-time apps at half or a quarter of their speed ----
// FlappyBird's pipes, DinoGame's cacti and TankGame's shells come at one speed, the one a 2010s player had. The
// band's Slow-mo button runs the app's clock slower instead: the page's time (performance.now, which Java's nanoTime
// is, and the requestAnimationFrame stamps) moves at the chosen rate and its timers (setTimeout, setInterval, which
// Thread.sleep and Swing timers come down to in TeaVM) wait longer by as much, so an app that steps by the frame
// and one that steps by the time both slow down, with no change to its code. Sound keeps its real pitch.
// The clock is warped only once a visitor slows it, and kept behind real time after, so no app sees time jump.
// Date stays the wall clock: CircleSpawner's clock face, FlappyBird's daily course, the passport's dates, photo
// stamps and the Clock's alarms all read it. The site's own clocks that must not slow (instant replay) use `real`.
// Each app keeps its own choice (oldjava.slowmo.<app>).

/** The apps that move on their own, where a slower world helps or is fun to watch. */
export const APPS = ['FlappyBird', 'DinoGame', 'TankGame', 'SpriteFloatyStars', 'CircleSpawner', 'ScreenSaver'];
const RATES = [1, 0.5, 0.25];
const NAMES: Record<number, string> = { 1: 'full speed', 0.5: 'half speed', 0.25: 'quarter speed' };
const FACES: Record<number, string> = { 1: '', 0.5: ' ½×', 0.25: ' ¼×' };

let rate = 1, realAt = 0, warpAt = 0, warping = false, held = false;
/** What came due while held (timers by 't' + id, frames by 'f' + id), to run once the hold lifts. */
const due = new Map<string, () => void>();
/** The clock and timers as they were before any warp, for site code that must keep real time. */
export const real = { now: performance.now.bind(performance), setInterval: setInterval.bind(window), setTimeout: setTimeout.bind(window) };
const realNow = real.now;
/** The page's time now, in performance.now terms: real time at the rates it has run at since warping began. */
function warpNow() { return held ? warpAt : warpAt + (realNow() - realAt) * rate; }
/** The page's time for a real performance.now stamp. */
function warped(real: number) { return held ? warpAt : warpAt + Math.max(0, real - realAt) * rate; }

function warp() {
  if (warping) return;
  warping = true;
  var timeout = setTimeout, interval = setInterval, frame = requestAnimationFrame;
  var clearT = clearTimeout, clearI = clearInterval, clearF = cancelAnimationFrame;
  realAt = warpAt = realNow();
  performance.now = warpNow;
  // A timer that comes due while held waits in `due` (an interval's ticks coalesce into one), cleared with it.
  function wrap(fn: TimerHandler, a: unknown[], key: () => string): TimerHandler {
    if (typeof fn !== 'function') return fn;
    return function () {
      if (held) due.set(key(), function () { timeout(function () { fn(...a); }); });
      else fn(...a);
    };
  }
  window.setTimeout = function (fn: TimerHandler, ms?: number, ...a: unknown[]) {
    var id: number = timeout(wrap(fn, a, function (): string { return 't' + id; }), (ms || 0) / rate);
    return id;
  } as typeof setTimeout;
  window.setInterval = function (fn: TimerHandler, ms?: number, ...a: unknown[]) {
    var id: number = interval(wrap(fn, a, function (): string { return 't' + id; }), (ms || 0) / rate);
    return id;
  } as typeof setInterval;
  window.clearTimeout = function (id?: number) { due.delete('t' + id); clearT(id); } as typeof clearTimeout;
  window.clearInterval = function (id?: number) { due.delete('t' + id); clearI(id); } as typeof clearInterval;
  window.requestAnimationFrame = function (cb: FrameRequestCallback) {
    var id = frame(function (t) {
      if (held) due.set('f' + id, function () { frame(function (t) { cb(warped(t)); }); });
      else cb(warped(t));
    });
    return id;
  };
  window.cancelAnimationFrame = function (id: number) { due.delete('f' + id); clearF(id); };
}

/** Stops the page's clock, its timers and its frames where they are (true), or runs them on from there (false): an
 *  app in a frame nobody can see (hidden-hold.ts) waits instead of playing on unseen. */
export function hold(on: boolean) {
  warp();   // the first call wraps the timers, so any the app sets afterwards can be held
  if (on === held) return;
  if (on) { warpAt = warpNow(); held = true; return; }
  realAt = realNow();
  held = false;
  var runs = Array.from(due.values());
  due.clear();
  runs.forEach(function (run) { run(); });
}

/** Runs the page's clock at `r` (1, 0.5 or 0.25). */
export function setRate(r: number) {
  if (r === rate) return;
  warp();
  warpAt = warpNow();
  realAt = realNow();
  rate = r;
}

function key() { return 'oldjava.slowmo.' + appId(); }
function shown() { return !embedded && APPS.indexOf(appId() || '') >= 0; }

/** The band's Slow-mo button: each click steps to the next slower rate, and from the slowest back to full speed. */
export function SlowMo({ className }: { className: string }) {
  const [r, setR] = useState(rate);
  if (!shown()) return null;
  var next = RATES[(RATES.indexOf(r) + 1) % RATES.length];
  return (
    <button type="button" className={className} aria-pressed={r < 1} aria-label={'Slow motion: ' + NAMES[r]}
      title={(r < 1 ? 'The app runs at ' + NAMES[r] + '. ' : 'Slow the app down, for an easier game or a closer look. ') + 'Click for ' + NAMES[next] + '.'}
      onClick={e => {
        var to = RATES[(RATES.indexOf(rate) + 1) % RATES.length];   // from the clock itself: two quick clicks step twice
        setRate(to);
        setR(to);
        store(key(), String(to));
        e.currentTarget.blur();   // else Space (FlappyBird's flap) would press it again
      }}>
      <span aria-hidden="true">{'🐢'}</span><span> Slow-mo</span><b>{FACES[r]}</b>
    </button>
  );
}

export function init() {
  if (!shown()) return;
  var saved = Number(stored(key())) || 1;
  if (RATES.indexOf(saved) > 0) setRate(saved);
}
