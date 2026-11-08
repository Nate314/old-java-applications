import { useEffect, useLayoutEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import { flushSync } from 'react-dom';
import { embedded } from './skin';
import { openClip } from './replay';
import { appId, catalog, root } from './catalog';
import type { CatalogApp } from './catalog';
import { indexOf } from './then-and-now';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';
import { AppCard } from './ui/cards';
import { openShare } from './share';
import { AGAIN_KEY } from './records';
import { playSound } from './sounds';
import { findOff, openFind } from './find';
import { openMaker } from './skin-maker';
import { whenShown } from './toolbar-more';

/** The open panel: its frame, what shows another app in it, what takes it away, and the app it has. */
interface Dock { frame: HTMLIFrameElement; show(app: CatalogApp): void; close(): void; id: string | null; name: string }
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
    if (!dock) dockBuild(app); else dock.show(app);
    dock!.id = id;
    dock!.name = app.name;
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
  dock.close();
  dock = null;
  document.documentElement.removeAttribute('data-dock');
  document.documentElement.style.removeProperty('--dock-w');
  dockShowButton();
}

function dockBuild(app: CatalogApp) {
  var show: ((app: CatalogApp) => void) | null = null, frame: HTMLIFrameElement | null = null;
  const close = openLayer(<DockPanel app={app} onMount={(s, f) => { show = s; frame = f; }} />);
  dock = { frame: frame!, show: show!, close, id: null, name: '' };
}

/** A tool in the panel's head: its glyph, and its name where there is room. */
function DockTool({ glyph, text }: { glyph: string; text: string }) {
  return <><span aria-hidden="true">{glyph}</span><span className="shim-dock-label">{' ' + text}</span></>;
}

/** The panel on the right of the stage: the docked app's page in an iframe, under a head of tools, with an edge. */
function DockPanel({ app: first, onMount }: { app: CatalogApp; onMount: (show: (app: CatalogApp) => void, frame: HTMLIFrameElement) => void }) {
  const box = useRef<HTMLElement>(null);
  const frame = useRef<HTMLIFrameElement>(null);
  const change = useRef<HTMLButtonElement>(null);
  const [app, setApp] = useState(first);
  const [active, setActive] = useState(false);
  useOwnKeys(box);
  useLayoutEffect(() => { onMount(a => flushSync(() => setApp(a)), frame.current!); }, []);
  // Which app has the keys: the panel lights while its frame has the focus.
  useEffect(() => {
    const lit = () => setActive(document.activeElement === frame.current);
    const blur = () => { setTimeout(lit, 0); };
    window.addEventListener('blur', blur);
    window.addEventListener('focus', lit);
    document.addEventListener('focusin', lit);
    return () => {
      window.removeEventListener('blur', blur);
      window.removeEventListener('focus', lit);
      document.removeEventListener('focusin', lit);
    };
  }, []);
  function swap() {
    if (!dock) return;
    var to = dock.id!;
    dockSave({ app: appId()!, w: dockFraction() });
    try { sessionStorage.setItem(AGAIN_KEY, to); } catch (e) { /* it opens on its title screen */ }
    location.href = root() + to + '/index.html';
  }
  function close() {
    dockSave(null);
    dockClose();
    playSound('close');
    var b = dockButton();
    if (b) b.focus();
  }
  return (
    <aside className={'shim-dock' + (active ? ' shim-dock-active' : '')} aria-label={app.name + ', side by side'} ref={box}>
      <DockEdge />
      <div className="shim-dock-head">
        <img className="shim-dock-pic" alt="" src={app.shot} />
        <strong className="shim-dock-name">{app.name}</strong>
        <span className="shim-dock-keys" title="Keys go to this app until you click the other one">Keys here</span>
        <button type="button" className="shim-dock-swap" title="Swap: make this the main app, with the other one beside it" aria-label="Swap" onClick={swap}>
          <DockTool glyph={'⇄'} text="Swap" />
        </button>
        <button type="button" className="shim-dock-change" title="Pick another app for this side" aria-label="Change" ref={change} onClick={() => openDockPicker(change.current!)}>
          <DockTool glyph={'☰'} text="Change" />
        </button>
        <a className="shim-dock-full" title="Open it on its own page" aria-label="Full page" href={root() + app.id + '/index.html'}>
          <DockTool glyph={'↗'} text="Full page" />
        </a>
        <CloseButton onClick={close} label="Close the app beside" title="Close it" />
      </div>
      <iframe className="shim-dock-frame" allow="autoplay; clipboard-write; microphone" title={app.name + ', beside ' + document.title}
        src={root() + app.id + '/index.html?embed'} ref={frame} />
    </aside>
  );
}

// The panel's left edge: drag it, or use the arrow keys on it, to share the page between the two apps.
function DockEdge() {
  const dragging = useRef(false);
  function to(f: number) {
    f = Math.max(0.15, Math.min(0.75, f));
    dockSize(f);
    return f;
  }
  function keep(f: number) {
    var d = dockSaved();
    if (d) dockSave({ app: d.app, w: f });
  }
  function stop(e: ReactPointerEvent) {
    if (!dragging.current) return;
    dragging.current = false;
    document.documentElement.removeAttribute('data-dock-drag');
    keep(to((innerWidth - e.clientX) / innerWidth));
  }
  return (
    <div className="shim-dock-edge" tabIndex={0} role="separator" aria-orientation="vertical" aria-label="Resize the two apps (arrow keys)"
      title="Drag to give either app more room"
      onPointerDown={e => {
        if (e.button !== 0) return;
        e.preventDefault();
        e.stopPropagation();
        dragging.current = true;
        e.currentTarget.setPointerCapture(e.pointerId);
        document.documentElement.setAttribute('data-dock-drag', '');
      }}
      onPointerMove={e => { if (dragging.current) to((innerWidth - e.clientX) / innerWidth); }}
      onPointerUp={stop}
      onPointerCancel={stop}
      onKeyDown={e => {
        var step = e.key === 'ArrowLeft' ? 0.05 : e.key === 'ArrowRight' ? -0.05 : 0;
        if (!step) return;
        e.preventDefault();
        var px = dock ? parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--dock-w')) : 0;
        keep(to((px || dockPixels(dockFraction())) / innerWidth + step));
      }} />
  );
}

// The picker: good companions for this app first, then every app, in the same cards as Switch app.
function openDockPicker(opener: HTMLElement) {
  if (dockPicker) return;
  function shut() {
    playSound('close');
    gone();
    if (opener && opener.isConnected) opener.focus();
  }
  function gone() {
    close();
    dockPicker = null;
  }
  const close = openLayer(<DockPicker onClose={shut} onPick={id => { gone(); dockOpen(id, true); }} onPanel={el => { dockPicker = el; }} />);
  playSound('open');
}

function DockPicker({ onClose, onPick, onPanel }: { onClose: () => void; onPick: (id: string) => void; onPanel: (el: HTMLElement) => void }) {
  const panel = useRef<HTMLElement>(null);
  const list = useRef<HTMLDivElement>(null);
  const [apps, setApps] = useState<CatalogApp[] | null>(null);
  useOwnKeys(panel, onClose);
  useLayoutEffect(() => { onPanel(panel.current!); }, []);
  useEffect(() => { catalog().then(setApps); }, []);
  useLayoutEffect(() => {
    if (apps) (list.current!.querySelector<HTMLElement>('.shim-app:not(.current)') || panel.current!.querySelector<HTMLElement>('.shim-close')!).focus();
  }, [apps]);
  const here = appId(), open = dock && dock.id;
  const good = apps ? (DOCK_PAIRS[here!] || []).concat(DOCK_EVERY).filter(function (id, i, all) {
    return id !== here && all.indexOf(id) === i && indexOf(apps, id) >= 0;
  }) : [];
  function card(app: CatalogApp) {
    const text = app.id === open ? 'Beside this app now' : DOCK_WHY[app.id] && good.indexOf(app.id) >= 0 ? DOCK_WHY[app.id] : undefined;
    return <AppCard key={app.id} app={app} current={app.id === open} text={text} onOpen={() => onPick(app.id)} />;
  }
  return (
    <>
      <div className="shim-scrim" onClick={onClose} />
      <aside className="shim-drawer shim-dock-picker" aria-label="Side by side" ref={panel}>
        <div className="shim-drawer-head">
          <strong>Side by side</strong>
          <CloseButton onClick={onClose} />
        </div>
        <p className="shim-dock-intro">Run a second app beside this one, each in its own half. It stays open as you go from app to app.</p>
        <div className="shim-drawer-list" ref={list}>
          {apps && good.length > 0 && <h3 className="shim-dock-heading">Good together</h3>}
          {apps && good.map(id => card(apps[indexOf(apps, id)]))}
          {apps && good.length > 0 && <h3 className="shim-dock-heading">Every app</h3>}
          {apps && apps.filter(app => app.id !== here && good.indexOf(app.id) < 0).map(card)}
        </div>
      </aside>
    </>
  );
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
