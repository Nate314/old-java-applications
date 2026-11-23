import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { clipMode, embedded } from './skin';
import { siteRoot } from './install';
import { popped } from './pop-out';
import { appId, catalog } from './catalog';
import { indexOf } from './then-and-now';
import { CloseButton, useOwnKeys } from './ui/layer';
import { openPanel } from './ui/panel';
import { appOut, audio } from './midi';
import { playSound } from './sounds';
import { real } from './slow-mo';
import { paintWatch, paintedCanvases } from './painted';

/** A recorded clip: its video, how many seconds it holds, and the app it shows. */
export interface ReplayClip { blob: Blob; seconds: number; app: string }
/** The page's recorder (startReplay). */
export interface Replay { live(): boolean; take(): Promise<ReplayClip | null> }
/** One of the MediaRecorders taking turns, with its chunks so far and when its clip starts. */
interface Recording { chunks: Blob[]; t0: number; rec: MediaRecorder; pausedAt: number }
/** An element to draw, in stacking order (z, then i). */
interface Layer { el: Element; cs: CSSStyleDeclaration; z: number; i: number }
/** What to draw again (rects in page pixels; null: everything), where each drawn element was, and the last drawing's frame. */
interface Damage { rects: DOMRect[] | null; seen: WeakMap<Element, DOMRect>; frame: string }

export let replay: Replay | null;
let clipBox: HTMLElement | null;

// ---- instant replay ----
// The best moments of a game are over before anyone thinks to press record. Every running app keeps its last
// moments instead: its windows are drawn about 30 times a second into a canvas of their own (drawDesk walks
// their elements: backgrounds, borders, the apps' paint canvases, pictures and text), and two MediaRecorders
// take turns on that canvas's stream, each restarted every REPLAY_SEG ms, half a segment apart, so one of
// them always holds between half a segment and a whole one. Replay (toolbar, exit screen, the home page's
// player bar) stops that one and shows its video (openClip) with the app's own sound, if it made any
// (appOut), to watch, save or share. The video stays in the page unless the visitor saves or shares it.
// Only a changed app is drawn again: the shim flags every canvas it draws on (painted.ts) and a
// MutationObserver sees the rest (text, buttons, windows moving), so an app at rest (a solved cube, a clock
// between ticks, a menu) costs one frame a second (REPLAY_IDLE), and the encoder has nothing new to encode.
// Nothing is drawn or recorded while nobody can see the app: its tab is hidden, or the page has scrolled the
// app out of view (the home page's player, once the visitor scrolls down to the gallery).
// A drawing is done a few ms per frame (paintDesk, REPLAY_SLICE), so a big app never holds a tap for a whole drawing;
// when frames come far apart (a window behind others) a frame may draw a tenth of the time since the last one, up to
// REPLAY_SLICE_MAX, so the clip's newest picture stays fresh there too.
// Only the part that changed is drawn again (damage): the canvases the shim drew on and the elements that changed,
// where they are and where they were, over the last picture; an app of many widgets (Minesweeper's 256 tiles)
// where one tile changed draws that tile, not the board. A whole drawing still comes every REPLAY_WHOLE, and at once
// when the windows move as one (scrolling, a new size, zoom, Pop out, a skin).
const REPLAY_SEG = 20000;

const REPLAY_FPS = 30;     // at most; fewer where drawing the windows costs more (REPLAY_SHARE)

const REPLAY_IDLE = 1000;  // ms between frames while nothing changes, so the clip's last frame is never stale

const REPLAY_SHARE = 0.1;  // of the page's time, at most, spent drawing them

const REPLAY_MAX = 1280;   // the clip's longest side, in pixels

const REPLAY_MIN = 640;    // the clip's longest side at least, in pixels, where the windows are small

const REPLAY_PAD = 16;     // desk around the windows, in page pixels

const REPLAY_SLICE = 6;    // ms of drawing per frame at most (one element may run over): a slow phone drew big apps in 0.1-0.5 s at once

const REPLAY_WHOLE = 2000;   // ms: a whole drawing at least this often, for what damage cannot see (CSS animations, overflowing children);
                             // or 4 times as long as the last one took, so a slow phone mostly draws what changed

const REPLAY_SLICE_MAX = 24;   // ms, when frames come far apart: half a long task, so a tap still never waits long

export function setReplay(v: Replay | null) { replay = v; }

const CLIP_SIZE = [640, 400];   // clip mode (?embed&clip): the gallery thumbnail's 16:10, at twice its usual width

const CLIP_BPS = 400000;   // the clips play in four places, often on phones; 900k made SpriteFloatyStars 1 MB for no visible gain

// MP4 first: phones and most sites take it, and Chrome records it. Firefox records WebM.
function replayType() {
  if (!window.MediaRecorder || !MediaRecorder.isTypeSupported || !window.MediaStream) return null;
  if (!HTMLCanvasElement.prototype.captureStream) return null;
  return ['video/mp4', 'video/webm'].filter(function (t) { return MediaRecorder.isTypeSupported(t); })[0] || null;
}

// A CSS background as a canvas fill: its color, or a gradient through the colors of its first gradient.
function cssFill(g: CanvasRenderingContext2D, image: string, color: string, x: number, y: number, w: number, h: number) {
  var stops = /gradient/.test(image) && image.match(/rgba?\([^)]*\)|#[0-9a-f]{3,8}\b/gi);
  if (stops && stops.length > 1) {
    var fill = /to (left|right)|\b(90|270)deg/.test(image) ? g.createLinearGradient(x, 0, x + w, 0)
      : /135deg|to bottom right/.test(image) ? g.createLinearGradient(x, y, x + w, y + h)
      : g.createLinearGradient(0, y, 0, y + h);
    stops.forEach(function (c, i) { fill.addColorStop(i / ((stops as RegExpMatchArray).length - 1), c); });
    return fill;
  }
  if (color && !/^(transparent|rgba\(.*,\s*0\))$/.test(color)) return color;
  return stops ? stops[0] : null;
}

// Paints a text node where the page lays it out, a line box at a time.
function drawText(g: CanvasRenderingContext2D, node: Text, cs: CSSStyleDeclaration, k: number) {
  var range = node.ownerDocument.createRange();
  range.selectNodeContents(node);
  var rects = range.getClientRects();
  if (!rects.length) return;
  var text = /^pre/.test(cs.whiteSpace) ? node.data : node.data.replace(/\s+/g, ' ').trim();
  if (cs.textTransform === 'uppercase') text = text.toUpperCase();
  else if (cs.textTransform === 'lowercase') text = text.toLowerCase();
  g.font = cs.fontStyle + ' ' + cs.fontWeight + ' ' + parseFloat(cs.fontSize) * k + 'px ' + cs.fontFamily;
  g.fillStyle = cs.color;
  g.textBaseline = 'middle';
  var lines = /^pre/.test(cs.whiteSpace) ? text.split('\n') : null;
  var words = text.split(' '), w = 0;
  for (var i = 0; i < rects.length; i++) {
    var r = rects[i], line;
    if (lines) line = lines[i] || '';
    else if (rects.length === 1) line = text;
    else {   // wrapped: as many words as fit each line box
      if (w >= words.length) break;
      line = words[w++];
      while (w < words.length && g.measureText(line + ' ' + words[w]).width <= r.width + 1) line += ' ' + words[w++];
    }
    g.fillText(line, r.left, r.top + r.height / 2);
  }
}

// Paints el and everything in it into g, at its place on the page (k: the desktop's CSS scale), pausing
// (yield) after each element so a big app can be drawn over several frames (paintDesk).
// With damage, an element that meets none of its rects is left as the last picture had it, and seen keeps where
// each drawn element was (for the next damage).
function* drawNode(g: CanvasRenderingContext2D, el: Element, cs: CSSStyleDeclaration, k: number, damage?: Damage): Generator<void, void> {
  if (cs.display === 'none' || cs.visibility === 'hidden') return;
  var r = el.getBoundingClientRect();
  if (!r.width || !r.height) return;
  if (damage) {
    if (damage.rects && !damage.rects.some(function (d) { return d.left < r.right && r.left < d.right && d.top < r.bottom && r.top < d.bottom; })) return;
    damage.seen.set(el, r);
  }
  g.save();
  g.globalAlpha *= +cs.opacity;
  var rad = (parseFloat(cs.borderTopLeftRadius) || 0) * k;
  function outline(inset: number) {
    g.beginPath();
    if (rad && g.roundRect) g.roundRect(r.left + inset, r.top + inset, r.width - 2 * inset, r.height - 2 * inset, Math.max(0, rad - inset));
    else g.rect(r.left + inset, r.top + inset, r.width - 2 * inset, r.height - 2 * inset);
  }
  var fill = cssFill(g, cs.backgroundImage, cs.backgroundColor, r.left, r.top, r.width, r.height);
  if (fill) { g.fillStyle = fill; outline(0); g.fill(); }
  var bw = parseFloat(cs.borderTopWidth) * k;
  if (bw && cs.borderTopColor === cs.borderLeftColor && parseFloat(cs.borderLeftWidth) * k === bw) {
    g.strokeStyle = cs.borderTopColor;
    g.lineWidth = bw;
    outline(bw / 2);
    g.stroke();
  } else {
    ([['Top', 0, 0, 1, 0], ['Bottom', 0, 1, 1, 0], ['Left', 0, 0, 0, 1], ['Right', 1, 0, 0, 1]] as [string, number, number, number, number][]).forEach(function (s) {
      var w = parseFloat((cs as unknown as Record<string, string>)['border' + s[0] + 'Width']) * k;
      if (!w) return;
      g.fillStyle = (cs as unknown as Record<string, string>)['border' + s[0] + 'Color'];
      g.fillRect(r.left + s[1] * (r.width - w), r.top + s[2] * (r.height - w), s[3] ? r.width : w, s[4] ? r.height : w);
    });
  }
  // Window frames are inset box shadows (computed as "color x y blur spread inset").
  var inset = /(rgba?\([^)]*\)|#\w+) -?[\d.]+px -?[\d.]+px [\d.]+px ([\d.]+)px inset/g, m: RegExpExecArray | null;
  while ((m = inset.exec(cs.boxShadow))) {
    g.strokeStyle = m[1];
    g.lineWidth = (m[2] as unknown as number) * k;
    outline((m[2] as unknown as number) * k / 2);
    g.stroke();
  }
  if (cs.overflow !== 'visible') { outline(0); g.clip(); }
  var tag = el.tagName;
  if (tag === 'CANVAS' && (el as HTMLCanvasElement).width) {
    g.drawImage(el as HTMLCanvasElement, r.left, r.top, r.width, r.height);
  } else if (tag === 'IMG' && (el as HTMLImageElement).complete && (el as HTMLImageElement).naturalWidth) {
    g.drawImage(el as HTMLImageElement, r.left, r.top, r.width, r.height);
  } else if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') {
    var value = tag === 'SELECT' ? ((el as HTMLSelectElement).selectedOptions[0] || {} as HTMLOptionElement).text || '' : (el as HTMLInputElement).value;
    g.font = cs.fontWeight + ' ' + parseFloat(cs.fontSize) * k + 'px ' + cs.fontFamily;
    g.fillStyle = cs.color;
    g.textBaseline = 'middle';
    g.fillText(value, r.left + (parseFloat(cs.paddingLeft) + parseFloat(cs.borderLeftWidth)) * k, r.top + r.height / 2);
  } else {
    // Children in stacking order: z-index below 0 (the paint canvas, under its component's children), the
    // text, then the rest by z-index (the shim stacks a component's first child on top, as AWT does).
    var view = el.ownerDocument.defaultView!, kids: Layer[] = [], texts: Text[] = [];
    for (var n = el.firstChild; n; n = n.nextSibling) {
      if (n.nodeType === 3 && /\S/.test((n as Text).data)) texts.push(n as Text);
      else if (n.nodeType === 1) {
        var ks = view.getComputedStyle(n as Element);
        kids.push({ el: n as Element, cs: ks, z: parseInt(ks.zIndex, 10) || 0, i: kids.length });
      }
    }
    kids.sort(function (a, b) { return a.z - b.z || a.i - b.i; });
    for (var c of kids) if (c.z < 0) yield* drawNode(g, c.el, c.cs, k, damage);
    texts.forEach(function (t) { drawText(g, t, cs, k); });
    for (c of kids) if (c.z >= 0) yield* drawNode(g, c.el, c.cs, k, damage);
  }
  g.restore();
  yield;
}

// Draws the app's windows, on the skin's desk, fitted into canvas. Returns the size they and the desk around
// them have at actual size (unzoomed), or null while there is no window. paintDesk does it a slice at a time
// (each next() draws one element); drawDesk all at once. With damage over the picture already in canvas, only
// its rects are drawn again, unless the windows' frame changed (then damage.rects is set to null: all of it).
function* paintDesk(g: CanvasRenderingContext2D, canvas: HTMLCanvasElement, desktop: Element, damage?: Damage): Generator<void, number[] | null> {
  var view = desktop.ownerDocument.defaultView!;
  var x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity, wins: Layer[] = [];
  for (var c = desktop.firstElementChild; c; c = c.nextElementSibling) {
    var r = c.getBoundingClientRect();
    if (!r.width || !r.height) continue;
    var cs = view.getComputedStyle(c);
    wins.push({ el: c, cs: cs, z: parseInt(cs.zIndex, 10) || 0, i: wins.length });
    x0 = Math.min(x0, r.left); y0 = Math.min(y0, r.top);
    x1 = Math.max(x1, r.right); y1 = Math.max(y1, r.bottom);
  }
  if (!wins.length) return null;
  var k = window.__shimScale || 1;
  x0 -= REPLAY_PAD * k; y0 -= REPLAY_PAD * k; x1 += REPLAY_PAD * k; y1 += REPLAY_PAD * k;
  var W = canvas.width, H = canvas.height;
  var s = Math.min(W / (x1 - x0), H / (y1 - y0));
  var root = view.getComputedStyle(desktop.ownerDocument.documentElement);
  var frame = [x0, y0, x1, y1, W, H, view === window].join();
  if (damage) {
    if (damage.frame !== frame) damage.rects = null;
    damage.frame = frame;
  }
  var desk = function () { g.setTransform(s, 0, 0, s, (W - (x1 - x0) * s) / 2 - x0 * s, (H - (y1 - y0) * s) / 2 - y0 * s); };
  g.save();
  if (damage && damage.rects) {
    desk();
    g.beginPath();
    damage.rects.forEach(function (d) { g.rect(d.left, d.top, d.width, d.height); });
    g.clip();
  }
  g.setTransform(1, 0, 0, 1, 0, 0);
  g.globalAlpha = 1;
  g.fillStyle = cssFill(g, root.getPropertyValue('--desk'), root.getPropertyValue('--desk').trim(), 0, 0, W, H) || '#1e293b';
  g.fillRect(0, 0, W, H);
  desk();
  wins.sort(function (a, b) { return a.z - b.z || a.i - b.i; });
  try {
    for (var w of wins) yield* drawNode(g, w.el, w.cs, k, damage);
  } finally {
    g.restore();
  }
  return [(x1 - x0) / k, (y1 - y0) / k];
}

export function drawDesk(g: CanvasRenderingContext2D, canvas: HTMLCanvasElement, desktop: Element) {
  var it = paintDesk(g, canvas, desktop), step;
  while (!(step = it.next()).done);
  return step.value;
}

// The clip's size for windows of that size: the pixels the screen gives them (the shim's canvases are drawn at
// that density), at least REPLAY_MIN on the longest side (small windows make small videos) and at most twice as
// big or REPLAY_MAX, in even pixels (which video encoders want). Clip mode's size is fixed. Recording past the
// screen's pixels only fed the encoder upscaled copies: on a 1x screen Flappy Bird's window was encoded at 3.3
// times its pixels, a fifth of Chrome's time while playing (headless), for nothing a viewer could see.
function clipSize(box: number[]) {
  if (clipMode) return CLIP_SIZE.slice();
  var m = Math.max(box[0], box[1]), screen = window.__shimRender || window.devicePixelRatio || 1;
  var s = Math.min(2, REPLAY_MAX / m, Math.max(screen, REPLAY_MIN / m));
  return [2 * Math.round(box[0] * s / 2), 2 * Math.round(box[1] * s / 2)];
}

// Starts keeping the last moments of the app on desktop. Returns { live(), take() } or null where the
// browser cannot record a canvas.
export function startReplay(desktop: Element): Replay | null {
  var type = replayType();
  if (!type) return null;
  var canvas = document.createElement('canvas');
  var g = canvas.getContext('2d')!;
  // Drawn here a slice per frame, then copied to canvas whole, so the recording never shows a half-drawn frame.
  var back = document.createElement('canvas'), bg = back.getContext('2d')!;
  var job: Generator<void, number[] | null> | null = null, spent = 0, started = false;
  var track: MediaStreamTrack | null = null, stream: MediaStream | null = null, tap: MediaStreamAudioDestinationNode | null = null, recs: Recording[] = [], last = 0, cost = 0, moved = 0, changed = true, inView = true;
  var drew = paintWatch();
  // What changed since the last drawing began: the canvases the shim drew on, the nodes changed or let go.
  var painted = paintedCanvases(), touched = new Set<Node>(), whole = -Infinity, wholeTook = 0, began = 0, damage: Damage = { rects: null, seen: new WeakMap(), frame: '' };
  function look(records: MutationRecord[]) {
    changed = true;
    records.forEach(function (m) {
      touched.add(m.target);
      m.removedNodes.forEach(function (n) { touched.add(n); });
    });
    if (touched.size > 2000) { touched.clear(); whole = -Infinity; }   // long out of sight: drawn whole when back
  }
  if (window.MutationObserver) {
    new MutationObserver(look).observe(desktop, { subtree: true, childList: true, attributes: true, characterData: true });
    new MutationObserver(function () { changed = true; whole = -Infinity; }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-skin'] });
  }
  // The rects to draw again, where each changed element is now and where the last drawing had it (2 px wider for
  // smoothed edges), or null for everything.
  function damaged(t: number) {
    var rects: DOMRect[] | null = [];
    if (t - whole >= Math.max(REPLAY_WHOLE, 4 * wholeTook)) rects = null;
    function add(r: DOMRect | undefined) { if (rects && r && r.width && r.height) rects.push(new DOMRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4)); }
    function at(n: Node) {
      var el = n.nodeType === 1 ? n as Element : n.parentElement;
      if (!el || !rects) return;
      if (el === desktop) { rects = null; return; }   // a window opened or closed
      add(damage.seen.get(el));
      if (el.isConnected) add(el.getBoundingClientRect());
    }
    touched.forEach(at);
    painted.forEach(function (c) { if (desktop.contains(c)) at(c); });
    touched.clear();
    painted.clear();
    return rects;
  }
  // The body, not the desktop: it stays in this page while the desktop is popped out. In the home page's
  // player it is the whole iframe, so this says whether the visitor has scrolled the player away.
  if (window.IntersectionObserver) {
    new IntersectionObserver(function (seen) {
      inView = seen[seen.length - 1].isIntersecting;
      if (inView) changed = true;
      sync();
    }).observe(document.body);
  }

  function begin() {
    if (!clipMode && audio && !tap && audio.createMediaStreamDestination) {   // the app's voices, not the site's sounds
      tap = audio.createMediaStreamDestination();
      appOut(audio).connect(tap);
      stream = null;   // a recording's tracks cannot change under it, so the voices start a stream of their own
    }
    if (!track) return;   // let go of (a new size, or the page put away in the back/forward cache): the next frame makes one
    // One stream for every recording of this track: a MediaStream lives as long as its tracks run, so a new one
    // per recording (six a minute) was never freed.
    if (!stream) stream = new MediaStream([track!].concat(tap ? tap.stream.getAudioTracks() : []));
    var r = { chunks: [], t0: Date.now() } as unknown as Recording;
    try {
      r.rec = new MediaRecorder(stream, { mimeType: type!, videoBitsPerSecond: clipMode ? CLIP_BPS : 5000000 });
      r.rec.ondataavailable = function (e: BlobEvent) { if (e.data.size) r.chunks.push(e.data); };
      r.rec.start(1000);
    } catch (e) {
      console.warn('Instant replay:', e);
      return;
    }
    if (away()) {
      r.rec.pause();
      r.pausedAt = Date.now();
    }
    recs.push(r);
  }
  function away() { return !popped && (document.hidden || !inView); }   // a popped out app is still in sight
  // A paused recording leaves that time out of its video, so its start moves up by as much (the clip's seconds).
  function sync() {
    recs.forEach(function (r) {
      if (away() && r.rec.state === 'recording') {
        r.rec.pause();
        r.pausedAt = Date.now();
      } else if (!away() && r.rec.state === 'paused') {
        r.rec.resume();
        r.t0 += Date.now() - r.pausedAt;
      }
    });
  }
  function rotate() {
    if (away()) return;
    while (recs.length > 1) recs.shift()!.rec.stop();
    begin();
  }
  var pressed = false;   // the visitor has pressed or let go since the last frame
  ['pointerdown', 'pointerup', 'keydown', 'keyup'].forEach(function (type) {
    document.addEventListener(type, function (e) { if (!(e as KeyboardEvent).repeat) pressed = true; }, { capture: true, passive: true });
  });
  var on: Window = window;   // the window the next frame was asked of
  var prev = 0;   // when the last frame came
  function frame() {
    var w = popped && !popped.win.closed ? popped.win : window;
    // A closed pop out window never answers, so the drawing would stop for good once the app is brought back.
    if (w !== on && w !== window) w.addEventListener('pagehide', function () { if (on === w) { on = window; requestAnimationFrame(frame); } });
    on = w;
    w.requestAnimationFrame(frame);
    var t = real.now();   // not the frame's time, which a popped out window counts from its own start
    // 6 ms at 60 fps; at 1.5 fps a 6 ms slice left a big app's drawing seconds old, so the clip lost its last seconds.
    var budget = Math.min(REPLAY_SLICE_MAX, Math.max(REPLAY_SLICE, (t - prev) * REPLAY_SHARE));
    prev = t;
    var answering = pressed;
    pressed = false;
    if (away()) return;
    if (drew()) changed = true;
    // Not in the frame that answers a tap or key: on a slow phone one drawing held that answer for 0.1 to 0.5 s.
    if (answering) return;
    if (!job) {
      // An app of many widgets (Minesweeper's Expert board has 256 tiles) takes several ms to draw, so it is drawn
      // less often: the app keeps at least nine tenths of the time.
      if (t - last < (changed ? Math.max(1000 / REPLAY_FPS, cost / REPLAY_SHARE) : REPLAY_IDLE) - 4) return;
      last = t;
      changed = false;
      spent = 0;
      began = t;
      damage.rects = damaged(t);
      if (back.width !== canvas.width || back.height !== canvas.height) { back.width = canvas.width; back.height = canvas.height; damage.rects = null; }
      // Nothing to draw again: the last picture still holds (it is copied below, so the video keeps a frame a second).
      job = damage.rects && !damage.rects.length ? null : paintDesk(bg, back, desktop, damage);
    }
    if (!job) {
      if (track) g.drawImage(back, 0, 0);
      return;
    }
    var t0 = real.now(), step;
    while (!(step = job.next()).done && real.now() - t0 < budget);
    spent += real.now() - t0;
    if (!step.done) return;
    job = null;
    if (!damage.rects) { whole = t; wholeTook = t - began; }
    cost = 0.8 * cost + 0.2 * spent;
    var box = step.value;
    if (!box) return;
    // A recording keeps the size it started with (Chrome's MP4 does not follow a resize), so windows much
    // bigger or smaller than it (a splash before the app's window, Minesweeper's Expert board) are fitted in at
    // first, and after a second at their new size the recording starts over at it.
    var want = clipSize(box);
    var fits = Math.max(Math.abs(want[0] / canvas.width - 1), Math.abs(want[1] / canvas.height - 1)) < 0.1;
    if (track && fits) moved = 0;
    else if (track && !moved) moved = t;
    if (!track && fits || track && (fits || t - moved < 1000)) {
      g.drawImage(back, 0, 0);
      if (track) return;
    } else {
      // Drawn again at the new size before a new recording starts on it.
      moved = 0;
      recs.forEach(function (r) { r.rec.stop(); });
      recs = [];
      if (track) track.stop();
      track = null;
      canvas.width = want[0];
      canvas.height = want[1];
      changed = true;
      last = 0;
      return;
    }
    track = canvas.captureStream(REPLAY_FPS).getVideoTracks()[0];
    stream = null;
    begin();
    if (started) return;
    started = true;
    real.setInterval(rotate, REPLAY_SEG / 2);   // real time: a slowed app's clip still covers 10 to 20 s
    document.documentElement.setAttribute('data-replay', '');
    if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaReplay: true }, location.origin);
  }
  requestAnimationFrame(frame);
  document.addEventListener('visibilitychange', sync);
  // Leaving the page: Chrome keeps no page with a live media track in its back/forward cache, so Back to an app
  // reloaded it at its title screen. Its recordings and tracks are let go; once the page is back, the next frame
  // starts recording again (the clip starts over from there).
  window.addEventListener('pagehide', function () {
    recs.forEach(function (r) { r.rec.stop(); });
    recs = [];
    if (track) track.stop();
    track = null;
    stream = null;
    if (tap) {
      tap.stream.getTracks().forEach(function (t) { t.stop(); });
      if (audio) appOut(audio).disconnect(tap);
      tap = null;
    }
    changed = true;
    last = 0;
  });

  return {
    live: function () { return recs.length > 0; },
    // Stops the longest recording and starts a new one in its place; resolves to the clip, or null.
    take: function () {
      var r = recs.shift();
      if (!r) return Promise.resolve(null);
      var end = r.rec.state === 'paused' ? r.pausedAt : Date.now();
      return new Promise<ReplayClip>(function (done) {
        r!.rec.onstop = function () {
          done({
            blob: new Blob(r!.chunks, { type: (r!.rec.mimeType || type!).split(';')[0] }),
            seconds: Math.max(1, Math.floor((end - r!.t0) / 1000)),
            app: appId()!
          });
        };
        r!.rec.stop();
        begin();
      });
    }
  };
}

export function replayNow(opener: HTMLElement | null) {
  if (replay) replay.take().then(function (clip) { if (clip) openClip(clip, opener); });
}

export function openClip(clip: ReplayClip, opener: HTMLElement | null) {
  if (embedded && window.parent !== window) {   // the home page shows it over its player, at full size
    window.parent.postMessage({ oldjavaClip: clip }, location.origin);
    return;
  }
  if (clipBox) return;
  if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(function () {});
  var url = URL.createObjectURL(clip.blob);
  var video: HTMLVideoElement | null = null, save: HTMLElement | null = null;
  openPanel(shut => <ClipPanel clip={clip} url={url} onClose={shut} onPanel={(el, v, s) => { clipBox = el; video = v; save = s; }} />, opener, () => {
    video!.pause();
    URL.revokeObjectURL(url);
    clipBox = null;
  });
  // Started by the visitor's click, so it may play with sound; muted if the browser says otherwise.
  video!.play().catch(function () { video!.muted = true; video!.play().catch(function () {}); });
  save!.focus();
}

/** The replay panel: the clip playing on a loop, Save video and Share... (where the browser can share files). */
function ClipPanel({ clip, url, onClose, onPanel }: {
  clip: ReplayClip; url: string; onClose: () => void; onPanel: (el: HTMLElement, video: HTMLVideoElement, save: HTMLElement) => void;
}) {
  const panel = useRef<HTMLDivElement>(null);
  const video = useRef<HTMLVideoElement>(null);
  const save = useRef<HTMLAnchorElement>(null);
  const [app, setApp] = useState(clip.app);   // its name, once the catalog says
  useOwnKeys(panel, onClose);
  useLayoutEffect(() => { onPanel(panel.current!, video.current!, save.current!); }, []);
  useEffect(() => {
    catalog().then(function (apps: OldjavaCatalogApp[]) {
      var found = apps[indexOf(apps, clip.app)];
      if (found) setApp(found.name);
    });
  }, []);
  const stamp = useState(() => new Date().toISOString().slice(0, 16).replace(/[-:]/g, '').replace('T', '-'))[0];
  const name = clip.app + '-replay-' + stamp + (/mp4/.test(clip.blob.type) ? '.mp4' : '.webm');
  const file = useState(() => window.File ? new File([clip.blob], name, { type: clip.blob.type }) : null)[0];
  const title = 'Instant replay: ' + app;
  const share = app + ', played in my browser:';
  return (
    <>
      <div className="shim-scrim" onClick={onClose} />
      <div className="shim-share shim-clip" role="dialog" aria-modal="true" aria-label={title} ref={panel}>
        <div className="shim-drawer-head">
          <strong>{title}</strong>
          <CloseButton onClick={onClose} />
        </div>
        <div className="shim-share-body">
          <video className="shim-clip-video" src={url} controls loop playsInline ref={video} />
          <p className="shim-clip-note" data-share={share}>
            {'The last ' + clip.seconds + ' seconds of ' + app + '. The video stays on this device until you save or share it.'}
          </p>
          <div className="shim-share-ways">
            <a className="shim-share-way primary" href={url} download={name} data-sound="none" ref={save} onClick={() => playSound('saved')}>Save video</a>
            {file && navigator.canShare && navigator.canShare({ files: [file] }) && (
              <button type="button" className="shim-share-way"
                onClick={() => navigator.share({ files: [file], title, text: share + ' ' + siteRoot + clip.app + '/index.html' }).catch(function () { /* closed */ })}>
                {'Share…'}
              </button>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

export function init() {
 replay = null;       // the page's recorder (startReplay), once the app runs
  // The replay panel: the clip playing on a loop, Save video and Share... (the system share sheet, with the
  // video and the app's link, where the browser can share files).
  clipBox = null;
  window.oldjavaReplay = {
    open: openClip,
    live: function () { return !!replay && replay.live(); },
    take: function () { return replay ? replay.take() : Promise.resolve(null); }
  };
}
