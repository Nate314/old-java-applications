import { embedded } from './skin';
import { openClip } from './replay';
import { appId, catalog, root } from './catalog';
import type { CatalogApp } from './catalog';
import { appCard, closeButton, indexOf, ownKeys } from './then-and-now';
import { openShare } from './share';
import { AGAIN_KEY } from './records';
import { playSound } from './sounds';
import { findOff, openFind } from './find';
import { openMaker } from './skin-maker';
import { whenShown } from './toolbar-more';

interface Dock { box: HTMLElement; frame: HTMLIFrameElement; pic: HTMLImageElement; label: HTMLElement; full: HTMLAnchorElement; id: string | null; name: string }
interface DockSaved { app: string; w: number }

let dockWide: Pick<MediaQueryList, 'matches' | 'addEventListener'>;
let dock: Dock | null;
let dockPicker: HTMLElement | null;

// ---- side by side (app pages): a second app docked beside this one ----
// These were desktop programs, and on a desktop you ran two at once: a timer beside a game, the chord finder
// beside the keyboard. The toolbar's Side by side button opens a second app in a panel on the right of the stage
// (its page in an iframe, ?embed, the way the home page's player runs one), and the stage ends at its edge, so
// both apps have their own room. The docked app stays as the visitor goes from app to app (oldjava.dock keeps
// which one and the panel's width), Swap trades the two places, and the panel's edge drags (or arrow keys) to
// resize it. Wide pages only: below 901 px the panel and its button are not offered.
const DOCK_KEY = 'oldjava.dock';

const DOCK_MIN = 320;

const DOCK_STAGE = 420;

const DOCK_FRACTION = 0.4;

// Good together: the companions offered first in the picker, with why. Clock (a timer, a stopwatch or a focus
// session beside anything) and ScreenSaver (something to watch) suit every app; the music pair suit each other.
const DOCK_PAIRS: Record<string, string[]> = { SynthesizerKeyboard: ['MusicTheoryExpert'], MusicTheoryExpert: ['SynthesizerKeyboard'] };

const DOCK_EVERY = ['Clock', 'ScreenSaver'];

const DOCK_WHY: Record<string, string> = {
  Clock: 'A focus session, timer or stopwatch, running while you play or work',
  ScreenSaver: 'An aquarium, starfield or pipes to keep you company',
  MusicTheoryExpert: 'Name the chord you are playing, or loop a progression to play over',
  SynthesizerKeyboard: 'Play what the theory shows on a keyboard with sound'
};

function dockSaved(): DockSaved | null {
  try {
    var d = JSON.parse(localStorage.getItem(DOCK_KEY) || 'null');
    return d && typeof d.app === 'string' && /^[A-Za-z0-9]+$/.test(d.app) ? d : null;
  } catch (e) { return null; }
}

function dockSave(d: DockSaved | null) {
  try {
    if (d) localStorage.setItem(DOCK_KEY, JSON.stringify(d)); else localStorage.removeItem(DOCK_KEY);
  } catch (e) { /* this page only */ }
}

function dockFraction() {
  var d = dockSaved();
  return d && d.w > 0 && d.w < 1 ? d.w : DOCK_FRACTION;
}

// The panel's width in pixels for a fraction of the page: never narrower than DOCK_MIN, and the stage keeps DOCK_STAGE.
function dockPixels(f: number) {
  var w = innerWidth;
  return Math.round(Math.max(DOCK_MIN, Math.min(w - DOCK_STAGE, f * w)));
}

function dockSize(f: number) {
  if (dock) document.documentElement.style.setProperty('--dock-w', dockPixels(f) + 'px');
}

function dockButton() { return document.querySelector<HTMLElement>('.shim-toolbar .shim-dock-open'); }

function dockShowButton() {
  var b = dockButton();
  if (!b) return;
  b.setAttribute('aria-pressed', String(!!dock));
  b.title = dock ? dock.name + ' is open beside this app. Click to pick another, or close it from its panel.'
    : 'Side by side: run a second app beside this one';
}

function dockOpen(id: string, focus: boolean) {
  if (!id || id === appId()) return;
  catalog().then(function (apps: CatalogApp[]) {
    var app = apps[indexOf(apps, id)];
    if (!app) return;
    dockSave({ app: id, w: dockFraction() });
    if (!dockWide.matches) return;
    if (dock && dock.id === id) { if (focus) dock.frame.focus(); return; }
    if (!dock) dockBuild();
    dock!.id = id;
    dock!.name = app.name;
    dock!.label.textContent = app.name;
    dock!.pic.src = app.shot;
    dock!.full.href = root() + id + '/index.html';
    dock!.frame.title = app.name + ', beside ' + document.title;
    dock!.frame.src = root() + id + '/index.html?embed';
    dock!.box.setAttribute('aria-label', app.name + ', side by side');
    document.documentElement.setAttribute('data-dock', '');
    dockSize(dockFraction());
    dockShowButton();
    if (focus) {
      dock!.frame.addEventListener('load', function () { dock && dock.frame.focus(); }, { once: true });
      playSound('open');
    }
  });
}

function dockClose() {
  if (!dock) return;
  dock.box.remove();
  dock = null;
  document.documentElement.removeAttribute('data-dock');
  document.documentElement.style.removeProperty('--dock-w');
  dockShowButton();
}

function dockBuild() {
  var box = document.createElement('aside');
  box.className = 'shim-dock';
  var head = document.createElement('div');
  head.className = 'shim-dock-head';
  var pic = document.createElement('img');
  pic.className = 'shim-dock-pic';
  pic.alt = '';
  var label = document.createElement('strong');
  label.className = 'shim-dock-name';
  var keys = document.createElement('span');
  keys.className = 'shim-dock-keys';
  keys.textContent = 'Keys here';
  keys.title = 'Keys go to this app until you click the other one';
  function tool(cls: string, glyph: string, text: string, title: string, onClick?: () => void) {
    var b = document.createElement(onClick ? 'button' : 'a');
    if (onClick) { b.type = 'button'; b.addEventListener('click', onClick); }
    b.className = cls;
    b.title = title;
    b.innerHTML = '<span aria-hidden="true">' + glyph + '</span><span class="shim-dock-label"> ' + text + '</span>';
    b.setAttribute('aria-label', text);
    return b;
  }
  var swap = tool('shim-dock-swap', '⇄', 'Swap', 'Swap: make this the main app, with the other one beside it', function () {
    if (!dock) return;
    var to = dock.id!;
    dockSave({ app: appId()!, w: dockFraction() });
    try { sessionStorage.setItem(AGAIN_KEY, to); } catch (e) { /* it opens on its title screen */ }
    location.href = root() + to + '/index.html';
  });
  var change = tool('shim-dock-change', '☰', 'Change', 'Pick another app for this side', function () { openDockPicker(change); });
  var full = tool('shim-dock-full', '↗', 'Full page', 'Open it on its own page');
  var close = closeButton(function () {
    dockSave(null);
    dockClose();
    playSound('close');
    var b = dockButton();
    if (b) b.focus();
  });
  close.setAttribute('aria-label', 'Close the app beside');
  close.title = 'Close it';
  [pic, label, keys, swap, change, full, close].forEach(function (n) { head.appendChild(n); });
  var frame = document.createElement('iframe');
  frame.className = 'shim-dock-frame';
  frame.setAttribute('allow', 'autoplay; clipboard-write; microphone');
  var edge = document.createElement('div');
  edge.className = 'shim-dock-edge';
  edge.tabIndex = 0;
  edge.setAttribute('role', 'separator');
  edge.setAttribute('aria-orientation', 'vertical');
  edge.setAttribute('aria-label', 'Resize the two apps (arrow keys)');
  edge.title = 'Drag to give either app more room';
  dockEdge(edge);
  box.appendChild(edge);
  box.appendChild(head);
  box.appendChild(frame);
  ownKeys(box);
  // Which app has the keys: the panel lights while its frame has the focus.
  function lit() { box.classList.toggle('shim-dock-active', document.activeElement === frame); }
  window.addEventListener('blur', function () { setTimeout(lit, 0); });
  window.addEventListener('focus', lit);
  document.addEventListener('focusin', lit);
  document.body.appendChild(box);
  dock = { box: box, frame: frame, pic: pic, label: label, full: full as HTMLAnchorElement, id: null, name: '' };
}

// The panel's left edge: drag it, or use the arrow keys on it, to share the page between the two apps.
function dockEdge(edge: HTMLElement) {
  var dragging = false;
  function to(f: number) {
    f = Math.max(0.15, Math.min(0.75, f));
    dockSize(f);
    return f;
  }
  function keep(f: number) {
    var d = dockSaved();
    if (d) dockSave({ app: d.app, w: f });
  }
  edge.addEventListener('pointerdown', function (e) {
    if (e.button !== 0) return;
    e.preventDefault();
    e.stopPropagation();
    dragging = true;
    edge.setPointerCapture(e.pointerId);
    document.documentElement.setAttribute('data-dock-drag', '');
  });
  edge.addEventListener('pointermove', function (e) {
    if (dragging) to((innerWidth - e.clientX) / innerWidth);
  });
  function stop(e: PointerEvent) {
    if (!dragging) return;
    dragging = false;
    document.documentElement.removeAttribute('data-dock-drag');
    keep(to((innerWidth - e.clientX) / innerWidth));
  }
  edge.addEventListener('pointerup', stop);
  edge.addEventListener('pointercancel', stop);
  edge.addEventListener('keydown', function (e) {
    var step = e.key === 'ArrowLeft' ? 0.05 : e.key === 'ArrowRight' ? -0.05 : 0;
    if (!step) return;
    e.preventDefault();
    var px = dock ? parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--dock-w')) : 0;
    keep(to((px || dockPixels(dockFraction())) / innerWidth + step));
  });
}

// The picker: good companions for this app first, then every app, in the same cards as Switch app.
function openDockPicker(opener: HTMLElement) {
  if (dockPicker) return;
  var scrim = document.createElement('div');
  scrim.className = 'shim-scrim';
  var panel = document.createElement('aside');
  panel.className = 'shim-drawer shim-dock-picker';
  panel.setAttribute('aria-label', 'Side by side');
  var head = document.createElement('div');
  head.className = 'shim-drawer-head';
  var title = document.createElement('strong');
  title.textContent = 'Side by side';
  var intro = document.createElement('p');
  intro.className = 'shim-dock-intro';
  intro.textContent = 'Run a second app beside this one, each in its own half. It stays open as you go from app to app.';
  var list = document.createElement('div');
  list.className = 'shim-drawer-list';
  function shut() {
    playSound('close');
    scrim.remove();
    panel.remove();
    dockPicker = null;
    if (opener && opener.isConnected) opener.focus();
  }
  var close = closeButton(shut);
  head.appendChild(title);
  head.appendChild(close);
  panel.appendChild(head);
  panel.appendChild(intro);
  panel.appendChild(list);
  scrim.addEventListener('click', shut);
  ownKeys(panel, shut);
  document.body.appendChild(scrim);
  document.body.appendChild(panel);
  dockPicker = panel;
  playSound('open');
  catalog().then(function (apps: CatalogApp[]) {
    var here = appId(), open = dock && dock.id;
    var good = (DOCK_PAIRS[here!] || []).concat(DOCK_EVERY).filter(function (id, i, all) {
      return id !== here && all.indexOf(id) === i && indexOf(apps, id) >= 0;
    });
    function heading(text: string) {
      var h = document.createElement('h3');
      h.className = 'shim-dock-heading';
      h.textContent = text;
      list.appendChild(h);
    }
    function card(app: CatalogApp) {
      var a = appCard(app, false);
      if (app.id === open) {
        a.classList.add('current');
        a.setAttribute('aria-current', 'true');
        a.querySelector('.shim-app-text span')!.textContent = 'Beside this app now';
      } else if (DOCK_WHY[app.id] && good.indexOf(app.id) >= 0) {
        a.querySelector('.shim-app-text span')!.textContent = DOCK_WHY[app.id];
      }
      a.setAttribute('data-sound', 'none');   // the panel plays its own open sound
      a.addEventListener('click', function (e) {
        if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
        e.preventDefault();
        scrim.remove();
        panel.remove();
        dockPicker = null;
        dockOpen(app.id, true);
      });
      list.appendChild(a);
    }
    if (good.length) {
      heading('Good together');
      good.forEach(function (id) { card(apps[indexOf(apps, id)]); });
      heading('Every app');
    }
    apps.forEach(function (app) { if (app.id !== here && good.indexOf(app.id) < 0) card(app); });
    (list.querySelector<HTMLElement>('.shim-app:not(.current)') || close).focus();
  });
}

export function init() {
  dockWide = (window.matchMedia as typeof matchMedia | undefined) ? matchMedia('(min-width: 901px)') : { matches: true, addEventListener: function () {} };
  dock = null;   // the open panel: { box, frame, id, name }
  dockPicker = null;
  // What the docked app asks of the page around it (it is an embedded page, like the home page's player): its exit
  // screen's suggestions open beside, and Find, Share, Replay and Your skin open over the whole page.
  window.addEventListener('message', function (e) {
    if (!dock || e.origin !== location.origin || e.source !== dock.frame.contentWindow || !e.data) return;
    var d = e.data;
    if (typeof d.oldjavaPlay === 'string') dockOpen(d.oldjavaPlay, true);
    else if (typeof d.oldjavaShare === 'string') openShare(d.oldjavaShare || null, dock.frame);
    else if (d.oldjavaClip) openClip(d.oldjavaClip, dock.frame);
    else if (d.oldjavaFind) openFind(dock.frame);
    else if (d.oldjavaSkinMaker) openMaker(dock.frame);
  });
  if (!embedded && !findOff) document.addEventListener('DOMContentLoaded', function () {
    var bar = document.querySelector('.shim-toolbar');
    if (!bar || !document.body.hasAttribute('data-app')) return;
    var b = document.createElement('button');
    b.type = 'button';
    b.className = 'shim-dock-open';
    b.setAttribute('aria-pressed', 'false');
    b.setAttribute('aria-label', 'Side by side');
    b.setAttribute('data-sound', 'none');   // the picker plays its own open sound
    b.innerHTML = '<span aria-hidden="true">◫</span><span class="shim-dock-label"> Side by side</span>';
    b.addEventListener('click', function () { openDockPicker(b); b.blur(); });
    var at = bar.querySelector('.shim-source-open');
    bar.insertBefore(b, at || null);
    dockShowButton();
    // The app picked on an earlier page opens again here, once the page is in view (a prerendered page waits).
    whenShown(function () {
      var d = dockSaved();
      if (d) dockOpen(d.app, false);
    });
    function fit() {
      if (!dockWide.matches) { dockClose(); return; }
      var d = dockSaved();
      if (d && !dock) dockOpen(d.app, false);
      else dockSize(dockFraction());
    }
    if (dockWide.addEventListener) dockWide.addEventListener('change', function () { whenShown(fit); });
    addEventListener('resize', function () { if (dock) dockSize(dockFraction()); });
  });
}
