import { redrawBar } from './install';
import { useLayoutEffect, useRef, useState, type ReactNode, type RefObject } from 'react';
import { flushSync } from 'react-dom';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';

export interface BandNews {
  /** kind: 'hint', 'stamp', ...; lead: kept at the start; text: may scroll; actions: links or buttons kept in view after it. */
  show(k: string, lead: ReactNode, text: ReactNode, actions: ReactNode, ms: number): void;
  clear(): void;
  kind(): string | null;
  onKind(fn: (k: string | null) => void): void;
}

export let news: BandNews | null;

// ---- band news (app pages): nothing floats over the stage ----
// The toolbar band's name slot slides the app's name up and a news line in: How to play when the app starts
// (or on the band's ? button) and a new passport stamp. A line longer than the slot scrolls through like a
// marquee, twice, then the name comes back. Pointing at the line holds it; x or Escape closes it.
export function setNews(v: BandNews | null) { news = v; }

/** What the line says: [kind, lead, text, actions]; it keeps saying it, hidden, once the slot's data-news goes. */
type Said = [string, ReactNode, ReactNode, ReactNode];

/**
 * The parts of the line the band works with (the text's box and track exist once it has said something), and title(t),
 * which gives the text's box its title (the whole text, when it is too long to scroll) or takes it away.
 */
interface LineParts {
  line: RefObject<HTMLDivElement | null>; box: RefObject<HTMLSpanElement | null>; track: RefObject<HTMLSpanElement | null>;
  title(t: string | null): void;
}

// mark(on): the slot shows the line (its data-news, which the toolbar draws), at once, so the line can be measured.
export function bandNews(slot: Element, mark: (on: boolean) => void): BandNews {
  var on = false, timer: ReturnType<typeof setTimeout> | 0 = 0, deadline = 0, anim: Animation | null = null, listeners: ((k: string | null) => void)[] = [];
  var say: ((s: Said) => void) | null = null, parts: LineParts | null = null;
  function told() { var k = kind(); listeners.forEach(function (fn) { fn(k); }); }
  function kind() { return on ? parts!.line.current!.getAttribute('data-kind') : null; }
  function clear() {
    clearTimeout(timer);
    if (anim) anim.cancel();
    anim = null;
    if (!on) return;
    on = false;
    mark(false);
    told();
  }
  function wait(ms: number) {
    clearTimeout(timer);
    deadline = Date.now() + ms;
    timer = setTimeout(clear, ms);
  }
  function enter() {
    if (!kind()) return;
    clearTimeout(timer);
    deadline = Math.max(0, deadline - Date.now());   // now the time that was left
    if (anim) anim.pause();
  }
  function leave() {
    if (!kind()) return;
    if (anim) anim.play();
    wait(Math.max(deadline, 3000));
  }
  openLayer(<NewsLine onEnter={enter} onLeave={leave} onClear={clear} onMount={(s, p) => { say = s; parts = p; }} />, slot);
  return {
    show: function (k: string, lead: ReactNode, text: ReactNode, actions: ReactNode, ms: number) {
      clear();
      say!([k, lead, text, actions]);
      on = true;
      mark(true);
      told();
      var box = parts!.box.current!, track = parts!.track.current!;
      parts!.title(null);
      var over = track.scrollWidth - box.clientWidth;
      if (over > 4 && track.animate && !reducedMotion()) {
        var hold = 1500, ride = over / 45 * 1000, one = hold + ride + hold;
        anim = track.animate([
          { transform: 'translateX(0)', offset: 0 },
          { transform: 'translateX(0)', offset: hold / one },
          { transform: 'translateX(' + -over + 'px)', offset: (hold + ride) / one },
          { transform: 'translateX(' + -over + 'px)', offset: 1 }
        ], { duration: one, iterations: 2 });
        ms = Math.max(ms, 2 * one + 1000);
      } else if (over > 4) {
        parts!.title(track.textContent!);
      }
      wait(ms);
    },
    clear: clear,
    kind: kind,
    onKind: function (fn: (k: string | null) => void) { listeners.push(fn); }
  };
}

function NewsLine({ onEnter, onLeave, onClear, onMount }: {
  onEnter: () => void; onLeave: () => void; onClear: () => void; onMount: (say: (s: Said) => void, parts: LineParts) => void;
}) {
  const line = useRef<HTMLDivElement>(null);
  const box = useRef<HTMLSpanElement>(null);
  const track = useRef<HTMLSpanElement>(null);
  const [said, setSaid] = useState<Said | null>(null);
  const [title, setTitle] = useState<string | null>(null);
  useOwnKeys(line, onClear);
  useLayoutEffect(() => {
    // Drawn at once, so the band can measure the text for its marquee.
    onMount(s => flushSync(() => setSaid(s)), { line, box, track, title: t => flushSync(() => setTitle(t)) });
    // The pointer's own enter and leave (React's onPointerEnter is made from pointerover and pointerout).
    const el = line.current!;
    el.addEventListener('pointerenter', onEnter);
    el.addEventListener('pointerleave', onLeave);
    return () => { el.removeEventListener('pointerenter', onEnter); el.removeEventListener('pointerleave', onLeave); };
  }, []);
  return (
    <div className="shim-news" role="status" data-kind={said ? said[0] : undefined} ref={line}>
      {said && (
        <>
          {said[1]}
          <span className="shim-news-text" title={title ?? undefined} ref={box}><span className="shim-news-track" ref={track}>{said[2]}</span></span>
          {said[3]}
          <CloseButton onClick={onClear} />
        </>
      )}
    </div>
  );
}

export function reducedMotion() {
  return !!((window.matchMedia as typeof matchMedia | undefined) && matchMedia('(prefers-reduced-motion: reduce)').matches);
}

// An app page's stage: #desktop fills the space between the toolbar band at the top and the controller and
// the output panel at the bottom, so no app window sits under them. The shim reports the stage as the
// screen, so apps that size or center their windows from the screen fit it, and centerWindows keeps the
// windows centered on it. stageEdge keeps a CSS variable at an element's height.
export function stageEdge(el: HTMLElement, name: string) {
  function set() { document.documentElement.style.setProperty(name, (el.isConnected && !el.hidden ? el.offsetHeight : 0) + 'px'); }
  set();
  if (window.ResizeObserver) new ResizeObserver(set).observe(el);
  else addEventListener('resize', set);
}

// Embedded pages and app pages: keep the windows, as a group, centered on the desktop, scaled down to fit it
// when they are bigger (window.__shimScale, which the shim divides mouse positions by). Only a change in
// which windows are open or their sizes recenters them, so dragging a window or moving it with keys works as
// usual. A change in the desktop's size alone (the source panel docking, the controller shown, the browser
// resized) keeps the group's last centering and only moves and scales it, so a dragged window keeps its place
// beside the others instead of snapping back.
//
// Fit (the toolbar's zoom button, on unless the visitor picks Actual size) also zooms small windows up to
// fill the stage, up to ZOOM_MAX times, leaving a margin around them: most of these apps were made for an
// 800 x 600 screen and would otherwise sit in a corner of today's. The shim's paint canvases then get more
// pixels (window.__shimRender, announced with a shimrender event), so drawing stays sharp at the bigger size.
const ZOOM_KEY = 'oldjava.zoom';

const ZOOM_MAX = 2.5;

const ZOOM_ROOM = 0.9;

const zoomListeners: ((on: boolean) => void)[] = [];

export function zoomFit() {
  try { return localStorage.getItem(ZOOM_KEY) !== 'actual'; } catch (e) { return true; }
}

export function setZoomFit(on: boolean) {
  try { localStorage.setItem(ZOOM_KEY, on ? 'fit' : 'actual'); } catch (e) { /* this page only */ }
  zoomChanged();
}

function zoomChanged() {
  zoomListeners.forEach(function (fn) { fn(zoomFit()); });
  redrawBar();
}

// The open windows' sizes (a key that changes when one opens, closes or is resized) and the box around them
// at their actual size ([x0, y0, x1, y1], or null when none shows).
export function windowsBox(desktop: HTMLElement) {
  var x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity, k = '';
  for (var c = desktop.firstElementChild as HTMLElement | null; c; c = c.nextElementSibling as HTMLElement | null) {
    if (!c.offsetWidth) continue;
    k += c.offsetWidth + 'x' + c.offsetHeight + ',';
    x0 = Math.min(x0, c.offsetLeft); y0 = Math.min(y0, c.offsetTop);
    x1 = Math.max(x1, c.offsetLeft + c.offsetWidth); y1 = Math.max(y1, c.offsetTop + c.offsetHeight);
  }
  return { key: k, box: x1 < x0 ? null : [x0, y0, x1, y1] };
}

export function centerWindows(desktop: HTMLElement) {
  var key = '', size = '', box: number[] | null = null, fit = zoomFit();
  zoomListeners.push(function (on: boolean) { fit = on; size = ''; });
  setInterval(function () {
    var now = windowsBox(desktop), k = now.key;
    var z = desktop.clientWidth + 'x' + desktop.clientHeight + ',' + (window.devicePixelRatio || 1);
    if (k === key && z === size) return;
    if (k !== key) box = now.box;
    key = k;
    size = z;
    if (!box) return;
    var x0 = box[0], y0 = box[1], x1 = box[2], y1 = box[3];
    var w = desktop.clientWidth, h = desktop.clientHeight;
    var s = Math.min(w / (x1 - x0), h / (y1 - y0));
    // Zoomed up in quarter steps, so resizing the browser a little does not redraw every canvas.
    s = s < 1 ? s : fit ? Math.max(1, Math.min(ZOOM_MAX, Math.floor(s * ZOOM_ROOM * 4) / 4)) : 1;
    // About the desktop's center (the default transform origin, which the Arcade power-on's scale shares).
    var dx = s * (w - x0 - x1) / 2;
    var dy = s * (h - y0 - y1) / 2;
    window.__shimScale = s;
    // Whole pixels keep unscaled and zoomed windows crisp; a group scaled down keeps the exact fit instead.
    desktop.style.transform = s < 1 ? 'translate(' + dx + 'px, ' + dy + 'px) scale(' + s + ')'
      : 'translate(' + Math.round(dx) + 'px, ' + Math.round(dy) + 'px)' + (s > 1 ? ' scale(' + s + ')' : '');
    redrawBar();
    var dpr = window.devicePixelRatio || 1;
    var render = Math.max(dpr, Math.min(dpr * s, ZOOM_MAX));   // capped: a canvas 9 times bigger paints slowly
    if (render !== (window.__shimRender || dpr)) {
      window.__shimRender = render;
      document.dispatchEvent(new Event('shimrender'));
    }
  }, 250);
}

export function init() {
 news = null;
  window.addEventListener('storage', function (e) { if (e.key === ZOOM_KEY || e.key === null) zoomChanged(); });
  // The home page's player bar has a Fit button too; its iframe hears the change through the storage event.
  window.oldjavaZoom = { fit: zoomFit, set: setZoomFit, onChange: function (fn) { zoomListeners.push(fn); } };
}
