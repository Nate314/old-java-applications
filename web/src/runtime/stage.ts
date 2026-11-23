import { redrawBar } from './install';
import { store, stored } from './stored';
import { screenFloor } from './screen-floor';

// An app page's stage: #desktop fills the space between the toolbar band at the top and the controller and
// the output panel at the bottom, so no app window sits under them. The shim reports the stage as the
// screen, so apps that size or center their windows from the screen fit it, and centerWindows keeps the
// windows centered on it. stageEdge keeps a CSS variable at an element's height.
export function stageEdge(el: HTMLElement, name: string) {
  function set() { document.documentElement.style.setProperty(name, (el.isConnected && !el.hidden ? el.offsetHeight : 0) + 'px'); }
  if (!window.ResizeObserver) { set(); addEventListener('resize', set); return; }
  // The observer's first call comes before the next paint, so the height is not read now: that would lay the whole
  // page out mid start-up, and again after the next change. An element of no size gets no first call: 0 now.
  document.documentElement.style.setProperty(name, '0px');
  new ResizeObserver(set).observe(el);
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
  return stored(ZOOM_KEY) !== 'actual';
}

export function setZoomFit(on: boolean) {
  store(ZOOM_KEY, on ? 'fit' : 'actual');
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

// ?embed&full: an app showing one window fills the frame with it, drawn without its title bar, border or shadow
// (data-full-one, shim.css): the box fitted is the window's content, scaled to the frame's width or height,
// whichever comes first. An app with more than one window open (or a <body data-shell> page, which draws its
// own Windows desktop) is centered and framed as in plain ?embed.
function fullBox(desktop: HTMLElement) {
  var shown = Array.prototype.filter.call(desktop.children, function (c: HTMLElement) { return c.offsetWidth > 0; }) as HTMLElement[];
  var one = shown.length === 1 && !document.body.hasAttribute('data-shell');
  document.documentElement.toggleAttribute('data-full-one', one);
  if (!one) return { key: 'framed,' + windowsBox(desktop).key, box: windowsBox(desktop).box, one: false };
  var win = shown[0];
  var body = (Array.prototype.find.call(win.children, function (c: HTMLElement) { return !c.classList.contains('titlebar') && c.offsetWidth > 0; }) as HTMLElement | undefined) || win;
  var x0 = win.offsetLeft + (body === win ? 0 : body.offsetLeft), y0 = win.offsetTop + (body === win ? 0 : body.offsetTop);
  return { key: 'full,' + body.offsetWidth + 'x' + body.offsetHeight, box: [x0, y0, x0 + body.offsetWidth, y0 + body.offsetHeight], one: true };
}

export function centerWindows(desktop: HTMLElement) {
  var key = '', size = '', box: number[] | null = null, fit = zoomFit();
  var full = document.documentElement.hasAttribute('data-full'), one = false;
  zoomListeners.push(function (on: boolean) { fit = on; size = ''; });
  screenFloor(desktop);   // now, so the app's first look at the screen sees it
  setInterval(function () {
    var floor = screenFloor(desktop);   // ?embed&full's pretend screen in a small frame (screen-floor.ts), before anything is measured
    var now = full ? fullBox(desktop) : windowsBox(desktop), k = now.key;
    var z = desktop.clientWidth + 'x' + desktop.clientHeight + ',' + (window.devicePixelRatio || 1) + ',' + floor.fit;
    if (k === key && z === size) return;
    if (k !== key) { box = now.box; one = full && (now as { one?: boolean }).one === true; }
    key = k;
    size = z;
    if (!box) return;
    var x0 = box[0], y0 = box[1], x1 = box[2], y1 = box[3];
    var w = desktop.clientWidth, h = desktop.clientHeight;
    var s = Math.min(w / (x1 - x0), h / (y1 - y0));
    // Zoomed up in quarter steps, so resizing the browser a little does not redraw every canvas.
    if (!one) s = s < 1 ? s : fit ? Math.max(1, Math.min(ZOOM_MAX, Math.floor(s * ZOOM_ROOM * 4) / 4)) : 1;
    // About the desktop's center (the default transform origin, which the Arcade power-on's scale shares).
    var dx = s * (w - x0 - x1) / 2;
    var dy = s * (h - y0 - y1) / 2;
    window.__shimScale = s * floor.k;
    // Whole pixels keep unscaled and zoomed windows crisp; a group scaled down keeps the exact fit instead.
    desktop.style.transform = (floor.fit ? floor.fit + ' ' : '') + (s < 1 ? 'translate(' + dx + 'px, ' + dy + 'px) scale(' + s + ')'
      : 'translate(' + Math.round(dx) + 'px, ' + Math.round(dy) + 'px)' + (s > 1 ? ' scale(' + s + ')' : ''));
    redrawBar();
    var dpr = window.devicePixelRatio || 1;
    var render = Math.max(dpr, Math.min(dpr * s * floor.k, ZOOM_MAX));   // capped: a canvas 9 times bigger paints slowly
    if (render !== (window.__shimRender || dpr)) {
      window.__shimRender = render;
      document.dispatchEvent(new Event('shimrender'));
    }
  }, 250);
}

export function init() {
  window.addEventListener('storage', function (e) { if (e.key === ZOOM_KEY || e.key === null) zoomChanged(); });
  // The home page's player bar has a Fit button too; its iframe hears the change through the storage event.
  window.oldjavaZoom = { fit: zoomFit, set: setZoomFit, onChange: function (fn) { zoomListeners.push(fn); } };
}
