import { embedded } from './skin';

// ---- stage glow: the app lights the room ----
// On a dark desk (Modern in dark mode, Arcade, a dark Yours skin) an app page's windows cast their own colors on the
// stage around them, like a TV's ambient light: Flappy Bird's sky glows blue round the window, Tank Game's
// battlefield green, a Minesweeper board green and sand. A few times a second the windows are drawn, very small,
// into a canvas behind #desktop (their frames, the colors the app set on its widgets, and its paint canvases),
// and the browser blurs that canvas far past the windows' edges. Each picture is laid over the last at part
// strength, so the light eases from one color to the next instead of flickering. A light desk gets no glow (it
// would only grey it), nor does the home page's player or a thumbnail. With reduced motion the light changes
// once a second. It never takes pointer events and is hidden from screen readers.
const W = 32;          // the glow's own pixels across: it is blurred to nothing finer anyway
const EVERY_MS = 150;  // how often it is drawn again
const CALM_MS = 1000;  // the same, when the system asks for reduced motion
const EASE = 0.35;     // how much of each new picture goes over the last

export function init() {
  if (embedded) return;
  document.addEventListener('DOMContentLoaded', function () {
    var desktop = document.getElementById('desktop');   // start.ts made it, on app pages only
    if (!desktop) return;
    var root = document.documentElement;
    var glow = document.createElement('canvas');
    glow.className = 'shim-glow';
    glow.setAttribute('aria-hidden', 'true');
    glow.width = W;
    glow.style.cssText = 'position:fixed;pointer-events:none;opacity:0;transition:opacity 0.8s;' +
      'filter:blur(64px) saturate(1.8);transform:scale(1.25)';
    document.body.insertBefore(glow, desktop);   // before it, so the windows paint over it
    var g = glow.getContext('2d')!;
    var calm = matchMedia('(prefers-reduced-motion: reduce)');
    var last = -Infinity;

    function draw() {
      var x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity;
      var on = getComputedStyle(root).colorScheme === 'dark';
      if (on) for (var c = desktop!.firstElementChild; c; c = c.nextElementSibling) {
        var r = c.getBoundingClientRect();
        if (!r.width) continue;
        x0 = Math.min(x0, r.left); y0 = Math.min(y0, r.top); x1 = Math.max(x1, r.right); y1 = Math.max(y1, r.bottom);
      }
      if (x1 <= x0 || y1 <= y0) { glow.style.opacity = '0'; return; }
      var w = x1 - x0, h = y1 - y0, H = Math.max(1, Math.round(W * h / w));
      if (glow.height !== H) glow.height = H;   // starts the picture afresh at the new shape
      var s = glow.style;
      s.left = x0 + 'px'; s.top = y0 + 'px'; s.width = w + 'px'; s.height = h + 'px';
      s.opacity = '0.85';
      var sx = W / w, sy = H / h;
      g.globalAlpha = calm.matches ? 1 : EASE;
      desktop!.querySelectorAll<HTMLElement>('.window, .custom-bg, canvas.paint').forEach(function (el) {
        var b = el.getBoundingClientRect();
        if (!b.width || !b.height) return;
        var dx = (b.left - x0) * sx, dy = (b.top - y0) * sy, dw = b.width * sx, dh = b.height * sy;
        if (el instanceof HTMLCanvasElement) {
          if (el.width && el.height) g.drawImage(el, dx, dy, dw, dh);
        } else {
          g.fillStyle = el.style.backgroundColor || getComputedStyle(el).backgroundColor;   // the app's color, else the skin's frame
          g.fillRect(dx, dy, dw, dh);
        }
      });
    }
    function tick(t: number) {
      requestAnimationFrame(tick);   // paused with the tab
      if (t - last < (calm.matches ? CALM_MS : EVERY_MS)) return;
      last = t;
      draw();
    }
    requestAnimationFrame(tick);
  });
}
