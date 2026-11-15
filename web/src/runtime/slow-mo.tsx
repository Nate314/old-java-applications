import { useState } from 'react';
import { appId } from './catalog';
import { embedded } from './skin';

// ---- Slow-mo: the real-time apps at half or a quarter of their speed ----
// FlappyBird's pipes, DinoGame's cacti and TankGame's shells come at one speed, the one a 2010s player had. The
// band's Slow-mo button runs the app's clock slower instead: the page's time (Date, performance.now and the
// requestAnimationFrame stamps) moves at the chosen rate and its timers (setTimeout, setInterval, which
// Thread.sleep and Swing timers come down to in TeaVM) wait longer by as much, so an app that steps by the frame
// and one that steps by the time both slow down, with no change to its code. Sound keeps its real pitch.
// The clock is warped only once a visitor slows it, and kept behind real time after, so no app sees time jump.
// Each app keeps its own choice (oldjava.slowmo.<app>).

/** The apps that move on their own, where a slower world helps or is fun to watch. */
const APPS = ['FlappyBird', 'DinoGame', 'TankGame', 'SpriteFloatyStars', 'CircleSpawner', 'ScreenSaver'];
const RATES = [1, 0.5, 0.25];
const NAMES: Record<number, string> = { 1: 'full speed', 0.5: 'half speed', 0.25: 'quarter speed' };
const FACES: Record<number, string> = { 1: '', 0.5: ' ½×', 0.25: ' ¼×' };

let rate = 1, realAt = 0, warpAt = 0;
let realNow: (() => number) | null = null;
/** The page's time now, in performance.now terms: real time at the rates it has run at since warping began. */
function warpNow() { return warpAt + (realNow!() - realAt) * rate; }
/** The page's time for a real performance.now stamp. */
function warped(real: number) { return warpAt + (real - realAt) * rate; }

function warp() {
  if (realNow) return;
  var perf = performance, RealDate = Date, start = perf.now.bind(perf), timeout = setTimeout, interval = setInterval;
  var frame = requestAnimationFrame;
  realNow = start;
  realAt = warpAt = start();
  var behind = function () { return start() - warpNow(); };
  perf.now = warpNow;
  // A Date made with no arguments is now in the page's time; with arguments, or called as a function, as before.
  var WarpDate = function (this: unknown, ...a: unknown[]) {
    if (!new.target) return RealDate();
    return a.length ? new (RealDate as unknown as new (...b: unknown[]) => Date)(...a) : new RealDate(RealDate.now() - behind());
  } as unknown as DateConstructor;
  (WarpDate as { prototype: Date }).prototype = RealDate.prototype;
  WarpDate.now = function () { return RealDate.now() - behind(); };
  WarpDate.parse = RealDate.parse;
  WarpDate.UTC = RealDate.UTC;
  window.Date = WarpDate;
  window.setTimeout = function (fn: TimerHandler, ms?: number, ...a: unknown[]) { return timeout(fn, (ms || 0) / rate, ...a); } as typeof setTimeout;
  window.setInterval = function (fn: TimerHandler, ms?: number, ...a: unknown[]) { return interval(fn, (ms || 0) / rate, ...a); } as typeof setInterval;
  window.requestAnimationFrame = function (cb: FrameRequestCallback) { return frame(function (t) { cb(warped(t)); }); };
}

/** Runs the page's clock at `r` (1, 0.5 or 0.25). */
export function setRate(r: number) {
  if (r === rate) return;
  warp();
  warpAt = warpNow();
  realAt = realNow!();
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
        try { localStorage.setItem(key(), String(to)); } catch (err) { /* this visit only */ }
        e.currentTarget.blur();   // else Space (FlappyBird's flap) would press it again
      }}>
      <span aria-hidden="true">{'🐢'}</span><span> Slow-mo</span><b>{FACES[r]}</b>
    </button>
  );
}

export function init() {
  if (!shown()) return;
  var saved = 1;
  try { saved = Number(localStorage.getItem(key())) || 1; } catch (e) { /* full speed */ }
  if (RATES.indexOf(saved) > 0) setRate(saved);
}
