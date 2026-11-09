import type { KeyboardEvent as ReactKeyboardEvent, MouseEvent as ReactMouseEvent, ReactNode } from 'react';
import { embedded } from './skin';
import { playSound } from './sounds';
import { redrawBar } from './install';

// ---- one-row toolbar: what does not fit waits under More ----
// An app page's band holds many controls, and which ones show depends on the width, the skin (Yours adds Edit),
// the app (Controls, Then | Now) and the moment (Replay and Photo once it runs, a Clock chip while one counts).
// Rather than wrap to a second row that pushes the stage down, the band keeps to one row (two on narrow pages,
// where the app's name has a row of its own): the least needed controls step out, last of BAND_ORDER first, and
// a More button at the end lists them in a menu. The controls stay where they are in the band, only hidden
// (.shim-more-out), so their own code keeps working; a menu entry clicks the real one.
// Which controls fit is measured in the page, so it is worked out here and the band (More, which the toolbar draws
// at its end) is drawn again from what was found, at once, before the next measure.
const BAND_ORDER = [   // kept the longest first
  '.shim-find-open', 'a[href="../index.html"]', '.shim-switch-open', '.shim-controls-open', '.shim-era',
  '.shim-next', '.shim-share-open', '.shim-source-open', '.shim-sound', '.shim-zoom', 'label',
  '.shim-dock-open', '.shim-own-edit', '.shim-replay', '.shim-pop', '.shim-photo-open'
];

/** A line of the menu: a heading, or an entry that does what a hidden control does. */
type Entry =
  | { head: string }
  | { role: string; what?: ReactNode; html?: string; title: string; run: () => void; sound?: string | null; checked?: string; key?: string };

/** What the More button and its menu show. */
const state = { hidden: true, label: '', open: false, right: 0, maxHeight: 0, entries: [] as Entry[] };
let bar: Element | null = null, button: HTMLButtonElement | null = null, menu: HTMLDivElement | null = null;
let narrow: { matches: boolean; addEventListener?: MediaQueryList['addEventListener'] };

function items() {   // the band's controls that can step out, in the order they leave
  var list: Element[] = [];
  BAND_ORDER.forEach(function (sel) {
    Array.prototype.forEach.call(bar!.children, function (c: Element) {
      if (c.matches(sel) && list.indexOf(c) < 0) list.push(c);
    });
  });
  return list.reverse();
}
function rows() {   // how many rows the band's visible controls take
  var centers: number[] = [];
  Array.prototype.forEach.call(bar!.children, function (c: Element) {
    if (c === menu) return;
    var r = c.getBoundingClientRect();
    if (!r.width) return;
    var y = r.top + r.height / 2;
    if (!centers.some(function (v) { return Math.abs(v - y) < 10; })) centers.push(y);
  });
  return centers.length;
}
function setHidden(h: boolean) {
  if (state.hidden === h) return;
  state.hidden = h;
  redrawBar();
}
function fit() {
  var list = items(), out = [], allowed = narrow.matches ? 2 : 1;
  list.forEach(function (c) { c.classList.remove('shim-more-out'); });
  setHidden(true);
  if (rows() > allowed) {
    setHidden(false);
    for (var i = 0; i < list.length && rows() > allowed; i++) {
      if (!list[i].getBoundingClientRect().width) continue;   // not showing anyway
      list[i].classList.add('shim-more-out');
      out.push(list[i]);
    }
  }
  if (!out.length) state.hidden = true;
  state.label = 'More: ' + out.length + (out.length === 1 ? ' control' : ' controls');
  if (state.open) build();
  redrawBar();
}
var queued = false;
function refit() {
  if (queued) return;
  queued = true;
  requestAnimationFrame(function () { queued = false; fit(); });
}

// The menu: the hidden controls in the band's order, each drawn as it is in the band. The skin picker and
// Then | Now become choices; a toggle shows its state.
function build() {
  var list: Entry[] = [];
  Array.prototype.forEach.call(bar!.querySelectorAll('.shim-more-out'), function (c: HTMLElement) {
    if (c.classList.contains('shim-era')) {
      list.push({ head: 'Version' });
      Array.prototype.forEach.call(c.querySelectorAll('a'), function (a: HTMLAnchorElement) {
        list.push({ role: 'menuitemradio', what: a.textContent, title: a.title, run: function () { a.click(); }, checked: String(a.hasAttribute('aria-current')) });
      });
    } else if (c.tagName === 'LABEL') {
      var select = c.querySelector('select');
      if (!select) return;
      list.push({ head: 'Skin' });
      Array.prototype.forEach.call(select.options, function (o: HTMLOptionElement) {
        list.push({
          role: 'menuitemradio', what: <><i className="shim-more-swatch" data-swatch={o.value} />{o.textContent}</>, title: '',
          run: function () {
            if (select!.value === o.value) return;
            select!.value = o.value;
            select!.dispatchEvent(new Event('change', { bubbles: true }));
          },
          checked: String(select!.value === o.value),
        });
      });
    } else {
      // The control's own face, as it is drawn in the band, but for its key, which goes at the end.
      var face = c.cloneNode(true) as HTMLElement;
      Array.prototype.forEach.call(face.querySelectorAll('kbd'), function (k: Element) { k.remove(); });
      var key = c.querySelector('kbd');
      var pressed = c.getAttribute('aria-pressed');
      list.push({
        role: pressed ? 'menuitemcheckbox' : 'menuitem', html: face.innerHTML,
        title: c.title, run: function () { c.click(); }, sound: c.getAttribute('data-sound'), checked: pressed || undefined,
        key: key ? key.textContent || '' : undefined,
      });
    }
  });
  state.entries = list;
}
function entries() { return menu ? Array.prototype.slice.call(menu.querySelectorAll<HTMLElement>('.shim-more-item')) as HTMLElement[] : []; }
function open(byKey: boolean) {
  build();
  state.open = true;
  // Under the More button, its right edges lined up, and no taller than the page below the band.
  var barBox = bar!.getBoundingClientRect(), box = button!.getBoundingClientRect();
  state.right = Math.max(4, Math.round(barBox.right - box.right));
  state.maxHeight = Math.max(160, Math.round(innerHeight - barBox.bottom - 12));
  redrawBar();
  playSound('open');
  var first = entries()[0];
  if (byKey && first) first.focus();
}
function close(refocus: boolean) {
  if (!state.open) return;
  state.open = false;
  redrawBar();
  if (refocus) button!.focus();
}

function start(band: Element) {
  bar = band;
  narrow = (window as Partial<Window>).matchMedia ? matchMedia('(max-width: 560px)') : { matches: false };
  document.addEventListener('pointerdown', function (e) {
    if (state.open && !button!.contains(e.target as Node) && !menu!.contains(e.target as Node)) close(false);
  }, true);
  addEventListener('blur', function () { close(false); });   // a click in an app iframe (Side by side) or another window

  fit();
  addEventListener('resize', refit);
  if (narrow.addEventListener) narrow.addEventListener('change', refit);
  if (window.ResizeObserver) {
    // Any control changing its width (Fit's zoom chip, Muted, Next's name arriving, a Clock chip, Replay
    // showing once the app runs) refits the band. A control that steps out reports width 0 once; the refit
    // that follows lands on the same result, so the band settles.
    var watch = new ResizeObserver(refit);
    Array.prototype.forEach.call(bar.children, function (c: Element) { if (c !== menu) watch.observe(c); });
    new MutationObserver(function (changes) {
      changes.forEach(function (m) {
        Array.prototype.forEach.call(m.addedNodes, function (c: Node) { if (c.nodeType === 1 && c !== menu) watch.observe(c as Element); });
      });
      refit();
    }).observe(bar, { childList: true });
  }
  new MutationObserver(refit).observe(document.documentElement,
    { attributes: true, attributeFilter: ['data-skin', 'data-replay', 'data-photo', 'data-title', 'data-source', 'data-dock'] });
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(refit);
}

function buttonClick(e: ReactMouseEvent) {
  if (!state.open) open(e.detail === 0); else close(false);
}
function buttonKey(e: ReactKeyboardEvent) {
  if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && !state.open) {
    e.preventDefault();
    open(true);
    if (e.key === 'ArrowUp') { var l = entries(); if (l.length) l[l.length - 1].focus(); }
  }
}
function menuKey(e: ReactKeyboardEvent) {
  var l = entries(), i = l.indexOf(document.activeElement as HTMLElement);
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault();
    var n = l.length;
    if (n) l[((i < 0 ? (e.key === 'ArrowDown' ? -1 : 0) : i) + (e.key === 'ArrowDown' ? 1 : -1) + n) % n].focus();
  } else if (e.key === 'Home' || e.key === 'End') {
    e.preventDefault();
    if (l.length) l[e.key === 'Home' ? 0 : l.length - 1].focus();
  } else if (e.key === 'Escape') {
    e.preventDefault();
    e.stopPropagation();
    close(true);
  } else if (e.key === 'Tab') {
    close(false);
  }
}

/** The More button and its menu, at the end of an app page's band. */
export function More() {
  return (
    <>
      {/* data-sound="none": opening plays its own */}
      <button type="button" className="shim-more" hidden={state.hidden} title="More: the controls that do not fit in this band"
        aria-haspopup="menu" aria-expanded={state.open} aria-label={state.label || undefined} data-sound="none"
        ref={b => { button = b; }} onClick={buttonClick} onKeyDown={buttonKey}>
        <span aria-hidden="true">{'⋯'}</span><span className="shim-more-label"> More</span>
      </button>
      <div className="shim-more-menu" role="menu" aria-label="More controls" hidden={!state.open}
        style={state.right ? { right: state.right + 'px', maxHeight: state.maxHeight + 'px' } : undefined}
        ref={m => { menu = m; }} onKeyDown={menuKey}>
        {state.entries.map((en, i) => 'head' in en
          ? <div key={i} className="shim-more-head" role="presentation">{en.head}</div>
          : (
            <button key={i} type="button" className="shim-more-item" role={en.role} tabIndex={-1} data-sound={en.sound || undefined}
              aria-checked={en.checked as "true" | "false" | undefined} onClick={() => { close(false); en.run(); }}>
              {en.html !== undefined ? <span className="shim-more-what" dangerouslySetInnerHTML={{ __html: en.html }} /> : <span className="shim-more-what">{en.what}</span>}
              {en.title && <small>{en.title}</small>}
              {en.key !== undefined && <kbd>{en.key}</kbd>}
            </button>
          ))}
      </div>
    </>
  );
}

// What has an effect beyond the page (a running app with its sounds, saves and replay recording, Clock's bell
// and its open beat) waits until a prerendered page is opened; a page that is never opened leaves no trace.
export function whenShown(fn: () => void) {
  if (document.prerendering) document.addEventListener('prerenderingchange', function () { fn(); }, { once: true });
  else fn();
}

export function init() {
  // After the toolbar and what the other parts do with the page at load (Side by side opening, the skin offer).
  if (!embedded) document.addEventListener('DOMContentLoaded', function () {
    var band = document.querySelector('.shim-toolbar');
    if (band && document.body.hasAttribute('data-app')) start(band);
  });
}
