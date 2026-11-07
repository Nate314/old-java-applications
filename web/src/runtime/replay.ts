import { clipMode, embedded } from './skin';
import { siteRoot } from './install';
import { popped } from './pop-out';
import { appId, catalog } from './catalog';
import { closeButton, indexOf, ownKeys } from './then-and-now';
import { appOut, audio } from './midi';
import { playSound } from './sounds';

/** A recorded clip: its video, how many seconds it holds, and the app it shows. */
export interface ReplayClip { blob: Blob; seconds: number; app: string }
/** The page's recorder (startReplay). */
export interface Replay { live(): boolean; take(): Promise<ReplayClip | null> }
/** One of the MediaRecorders taking turns, with its chunks so far and when its clip starts. */
interface Recording { chunks: Blob[]; t0: number; rec: MediaRecorder; pausedAt: number }
/** An element to draw, in stacking order (z, then i). */
interface Layer { el: Element; cs: CSSStyleDeclaration; z: number; i: number }

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
// Only a changed app is drawn again: the shim flags every canvas it draws on (window.__shimPainted) and a
// MutationObserver sees the rest (text, buttons, windows moving), so an app at rest (a solved cube, a clock
// between ticks, a menu) costs one frame a second (REPLAY_IDLE), and the encoder has nothing new to encode.
// Nothing is drawn or recorded while nobody can see the app: its tab is hidden, or the page has scrolled the
// app out of view (the home page's player, once the visitor scrolls down to the gallery).
const REPLAY_SEG = 20000;

const REPLAY_FPS = 30;     // at most; fewer where drawing the windows costs more (REPLAY_SHARE)

const REPLAY_IDLE = 1000;  // ms between frames while nothing changes, so the clip's last frame is never stale

const REPLAY_SHARE = 0.1;  // of the page's time, at most, spent drawing them

const REPLAY_MAX = 1280;   // the clip's longest side, in pixels

const REPLAY_PAD = 16;     // desk around the windows, in page pixels

export function setReplay(v: Replay | null) { replay = v; }

const CLIP_SIZE = [640, 400];   // clip mode (?embed&clip): the gallery thumbnail's 16:10, at twice its usual width

const CLIP_BPS = 900000;

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

// Paints el and everything in it into g, at its place on the page (k: the desktop's CSS scale).
function drawNode(g: CanvasRenderingContext2D, el: Element, cs: CSSStyleDeclaration, k: number) {
  if (cs.display === 'none' || cs.visibility === 'hidden') return;
  var r = el.getBoundingClientRect();
  if (!r.width || !r.height) return;
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
    kids.forEach(function (c) { if (c.z < 0) drawNode(g, c.el, c.cs, k); });
    texts.forEach(function (t) { drawText(g, t, cs, k); });
    kids.forEach(function (c) { if (c.z >= 0) drawNode(g, c.el, c.cs, k); });
  }
  g.restore();
}

// Draws the app's windows, on the skin's desk, fitted into canvas. Returns the size they and the desk around
// them have at actual size (unzoomed), or null while there is no window.
export function drawDesk(g: CanvasRenderingContext2D, canvas: HTMLCanvasElement, desktop: Element) {
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
  g.setTransform(1, 0, 0, 1, 0, 0);
  g.globalAlpha = 1;
  g.fillStyle = cssFill(g, root.getPropertyValue('--desk'), root.getPropertyValue('--desk').trim(), 0, 0, W, H) || '#1e293b';
  g.fillRect(0, 0, W, H);
  g.setTransform(s, 0, 0, s, (W - (x1 - x0) * s) / 2 - x0 * s, (H - (y1 - y0) * s) / 2 - y0 * s);
  wins.sort(function (a, b) { return a.z - b.z || a.i - b.i; });
  wins.forEach(function (w) { drawNode(g, w.el, w.cs, k); });
  return [(x1 - x0) / k, (y1 - y0) / k];
}

// The clip's size for windows of that size: up to twice as big (small windows make small videos), at most
// REPLAY_MAX on the longest side, in even pixels (which video encoders want). Clip mode's size is fixed.
function clipSize(box: number[]) {
  if (clipMode) return CLIP_SIZE.slice();
  var s = Math.min(2, REPLAY_MAX / Math.max(box[0], box[1]));
  return [2 * Math.round(box[0] * s / 2), 2 * Math.round(box[1] * s / 2)];
}

// Starts keeping the last moments of the app on desktop. Returns { live(), take() } or null where the
// browser cannot record a canvas.
export function startReplay(desktop: Element): Replay | null {
  var type = replayType();
  if (!type) return null;
  var canvas = document.createElement('canvas');
  var g = canvas.getContext('2d')!;
  var track: MediaStreamTrack | null = null, tap: MediaStreamAudioDestinationNode | null = null, recs: Recording[] = [], last = 0, cost = 0, moved = 0, changed = true, inView = true;
  function look() { changed = true; }
  if (window.MutationObserver) {
    new MutationObserver(look).observe(desktop, { subtree: true, childList: true, attributes: true, characterData: true });
    new MutationObserver(look).observe(document.documentElement, { attributes: true, attributeFilter: ['data-skin'] });
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
    }
    var stream = new MediaStream([track!].concat(tap ? tap.stream.getAudioTracks() : []));
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
  function frame() {
    (popped && !popped.win.closed ? popped.win : window).requestAnimationFrame(frame);
    var t = performance.now();   // not the frame's time, which a popped out window counts from its own start
    if (away()) return;
    if (window.__shimPainted) {
      window.__shimPainted = false;
      changed = true;
    }
    // An app of many widgets (Minesweeper's Expert board has 256 tiles) takes several ms to draw, so it is drawn
    // less often: the app keeps at least nine tenths of the time.
    if (t - last < (changed ? Math.max(1000 / REPLAY_FPS, cost / REPLAY_SHARE) : REPLAY_IDLE) - 4) return;
    last = t;
    changed = false;
    var t0 = performance.now(), box = drawDesk(g, canvas, desktop);
    cost = 0.8 * cost + 0.2 * (performance.now() - t0);
    if (!box) return;
    // A recording keeps the size it started with (Chrome's MP4 does not follow a resize), so windows much
    // bigger or smaller than it (a splash before the app's window, Minesweeper's Expert board) are fitted in at
    // first, and after a second at their new size the recording starts over at it.
    var want = clipSize(box);
    var off = Math.max(Math.abs(want[0] / canvas.width - 1), Math.abs(want[1] / canvas.height - 1));
    if (track && off < 0.1) { moved = 0; return; }
    if (track && !moved) moved = t;
    if (track && t - moved < 1000) return;
    moved = 0;
    recs.forEach(function (r) { r.rec.stop(); });
    recs = [];
    if (track) track.stop();
    canvas.width = want[0];
    canvas.height = want[1];
    drawDesk(g, canvas, desktop);
    var first = !track;
    track = canvas.captureStream(REPLAY_FPS).getVideoTracks()[0];
    begin();
    if (!first) return;
    setInterval(rotate, REPLAY_SEG / 2);
    document.documentElement.setAttribute('data-replay', '');
    if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaReplay: true }, location.origin);
  }
  requestAnimationFrame(frame);
  document.addEventListener('visibilitychange', sync);

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
  var scrim = document.createElement('div');
  scrim.className = 'shim-scrim';
  var panel = document.createElement('div');
  panel.className = 'shim-share shim-clip';
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-modal', 'true');
  panel.setAttribute('aria-label', 'Instant replay');
  var head = document.createElement('div');
  head.className = 'shim-drawer-head';
  var title = document.createElement('strong');
  title.textContent = 'Instant replay';
  function shut() {
    playSound('close');
    video.pause();
    URL.revokeObjectURL(url);
    scrim.remove();
    panel.remove();
    clipBox = null;
    if (opener && opener.isConnected) opener.focus();
  }
  head.appendChild(title);
  head.appendChild(closeButton(shut));
  panel.appendChild(head);
  var body = document.createElement('div');
  body.className = 'shim-share-body';
  var video = document.createElement('video');
  video.className = 'shim-clip-video';
  video.src = url;
  video.controls = true;
  video.loop = true;
  video.playsInline = true;
  var note = document.createElement('p');
  note.className = 'shim-clip-note';
  var ways = document.createElement('div');
  ways.className = 'shim-share-ways';
  var stamp = new Date().toISOString().slice(0, 16).replace(/[-:]/g, '').replace('T', '-');
  var name = clip.app + '-replay-' + stamp + (/mp4/.test(clip.blob.type) ? '.mp4' : '.webm');
  var save = document.createElement('a');
  save.className = 'shim-share-way primary';
  save.href = url;
  save.download = name;
  save.textContent = 'Save video';
  save.setAttribute('data-sound', 'none');
  save.addEventListener('click', function () { playSound('saved'); });
  ways.appendChild(save);
  var file = window.File ? new File([clip.blob], name, { type: clip.blob.type }) : null;
  var link = siteRoot + clip.app + '/index.html';
  if (file && navigator.canShare && navigator.canShare({ files: [file] })) {
    var native = document.createElement('button');
    native.type = 'button';
    native.className = 'shim-share-way';
    native.textContent = 'Share…';
    native.addEventListener('click', function () {
      navigator.share({ files: [file!], title: title.textContent, text: note.dataset.share + ' ' + link })
        .catch(function () { /* closed */ });
    });
    ways.appendChild(native);
  }
  [video, note, ways].forEach(function (el) { body.appendChild(el); });
  panel.appendChild(body);
  scrim.addEventListener('click', shut);
  ownKeys(panel, shut);
  document.body.appendChild(scrim);
  document.body.appendChild(panel);
  clipBox = panel;
  playSound('open');
  // Started by the visitor's click, so it may play with sound; muted if the browser says otherwise.
  video.play().catch(function () { video.muted = true; video.play().catch(function () {}); });
  save.focus();
  function describe(app: string) {
    title.textContent = 'Instant replay: ' + app;
    panel.setAttribute('aria-label', title.textContent);
    note.textContent = 'The last ' + clip.seconds + ' seconds of ' + app + '. The video stays on this device'
      + ' until you save or share it.';
    note.dataset.share = app + ', played in my browser:';
  }
  describe(clip.app);
  catalog().then(function (apps: OldjavaCatalogApp[]) {
    var app = apps[indexOf(apps, clip.app)];
    if (app && clipBox === panel) describe(app.name);
  });
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
