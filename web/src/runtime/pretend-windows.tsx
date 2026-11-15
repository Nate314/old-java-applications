import type { PointerEvent as ReactPointerEvent, ReactNode } from 'react';
import { useLayoutEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import { played } from './passport';
import { catalog, type CatalogApp } from './catalog';
import { playSound } from './sounds';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';
import { play, playProps } from './ui/cards';

// ---- pretend Windows (<body data-shell>: Windows and Windows+E) ----
// Those two apps do nothing but press keys with java.awt.Robot: the Windows key once, or Win+E over and
// over. The shim's Robot hands Windows key shortcuts to window.__shimShell instead of the app's window, and
// this plays the Windows they were written for, inside the stage: the Windows key alone opens a Start menu
// of every app on the site (type to search, Enter to play), and Win+E opens a File Explorer window on the
// folder of apps (double-click one to play it). Windows outlives the program that pressed its keys, so an
// exit waits until the Start menu is closed. Windows+E's keyboard also presses Win+D (show desktop), Win+R
// (a Run box that opens an app by name), Win+Tab (task view) and Win+arrows (snap, maximize, restore,
// minimize the File Explorer window in front). Returns { holdExit(show) }.
// What is on screen is kept below as plain state, and every change draws the layer again (Shell) at once, so the
// code that changes it can measure the result straight away, as it did when it moved elements itself.
const MAX_EXPLORERS = 40;

/** A File Explorer window: where it is, its size before a snap (left, top, width, height), and its selected app. */
interface Win {
  seq: number; left: number; top: number; width: number; height: number; z: number;
  snap: string; min: boolean; moving: boolean; restore?: number[]; timer?: ReturnType<typeof setTimeout>; sel: CatalogApp | null;
  el?: HTMLDivElement | null;
}
interface Run { seq: number; z: number; left?: number; top?: number; error: string }
interface Tasks { seq: number; head: string; cards: { w: Win; n: number; scale: number; min: boolean }[] }

declare global {
  interface Window {
    /** The shim's Robot hands Windows key shortcuts (AWT key codes) here. */
    __shimShell?: (keycode: number) => void;
  }
}

export function pretendWindows() {
  // The order things were put on the layer, which is the order they stand in it.
  var seq = 0;
  var menu: { seq: number; query: string } | null = null, heldExit: (() => void) | null = null, explorers: Win[] = [], opened = 0, front = 0;
  var tray: { seq: number } | null = null, active: Win | null = null, apps: CatalogApp[] | null = null;
  var peeked: Win[] | null = null, runBox: Run | null = null, tasks: Tasks | null = null, note: { seq: number; text: string } | null = null;   // Win+D's hidden windows, Win+R, Win+Tab
  var layer!: HTMLDivElement, redraw!: () => void;
  function draw() { redraw(); }
  function need() { catalog().then(function (a) { if (!apps) { apps = a; draw(); } }); }

  function closeMenu() {
    if (!menu) return;
    menu = null;
    draw();
    document.removeEventListener('pointerdown', outside, true);
    playSound('close');
    if (heldExit) {
      var show = heldExit;
      heldExit = null;
      show();
    }
  }
  function outside(e: Event) { var box = layer.querySelector('.shim-start'); if (menu && box && !box.contains(e.target as Node)) closeMenu(); }
  function openMenu() {
    menu = { seq: ++seq, query: '' };
    draw();
    document.addEventListener('pointerdown', outside, true);
    playSound('open');
    layer.querySelector<HTMLInputElement>('.shim-start-search')!.focus({ preventScroll: true });
    need();
  }

  function forget(w: Win) {
    explorers = explorers.filter(function (o) { return o !== w; });
    if (active === w) active = null;
    if (!explorers.length) tray = null;
  }
  function closeAll() {
    explorers = [];
    active = null;
    tray = null;
    peeked = null;
    draw();
  }
  function raise(w: Win) {
    w.z = ++front;
    active = w;
  }
  function openExplorer() {
    if (explorers.length >= MAX_EXPLORERS) return;
    // Cascaded down and to the right from the top left, as Windows does, starting over when the next would
    // not fit. The bottom 44 px are left to the tray.
    var tall = layer.clientHeight - 44;
    var width = Math.min(560, layer.clientWidth - 16), height = Math.max(160, Math.min(380, tall - 16));
    var step = 28;
    var room = Math.max(1, Math.floor(Math.min(layer.clientWidth - width, tall - height) / step));
    var at = 8 + (opened++ % room) * step;
    var w: Win = { seq: ++seq, left: at, top: at, width: width, height: height, z: 0, snap: '', min: false, moving: false, sel: null };
    explorers.push(w);
    raise(w);
    if (!tray) tray = { seq: ++seq };
    draw();
    playSound('open');
    need();
  }
  function closeExplorer(w: Win) {
    forget(w);
    draw();
    playSound('close');
  }
  function select(w: Win, app: CatalogApp) {
    w.sel = app;
    draw();
  }

  // Dragging a window by its title bar (a File Explorer window, or the Run box).
  var grab: { x: number; y: number } | null = null;
  function dragStart(e: ReactPointerEvent<HTMLElement>, box: Win | Run, el: HTMLElement) {
    if (e.button !== 0 || (e.target as Element).closest('button')) return;
    grab = { x: e.clientX - el.offsetLeft, y: e.clientY - el.offsetTop };
    e.currentTarget.setPointerCapture(e.pointerId);
  }
  function dragMove(e: ReactPointerEvent<HTMLElement>, box: Win | Run, el: HTMLElement) {
    if (!grab) return;
    var w = box as Win;
    if (w.snap && w.restore) {   // dragging a snapped window takes it back to its size
      w.snap = '';
      grab.x = Math.min(grab.x, w.restore[2] / 2);
      w.width = w.restore[2];
      w.height = w.restore[3];
      draw();
    }
    box.left = Math.max(80 - el.offsetWidth, Math.min(layer.clientWidth - 80, e.clientX - grab.x));
    box.top = Math.max(0, Math.min(layer.clientHeight - 32, e.clientY - grab.y));
    draw();
  }
  function dragEnd() { grab = null; }

  // A line at the top of the layer for a shortcut with nothing to act on.
  function say(text: string) {
    var n = note = { seq: ++seq, text: text };
    draw();
    setTimeout(function () { if (note === n) { note = null; draw(); } }, 2600);
  }
  function shown() { return explorers.filter(function (w) { return !w.min; }); }
  function topWindow() {
    return shown().reduce<Win | null>(function (best, w) { return !best || w.z > best.z ? w : best; }, null);
  }
  function minimize(w: Win) {
    w.min = true;
    if (active === w) active = null;
    var next = topWindow();
    if (next) raise(next);
  }
  function restore(w: Win) {
    w.min = false;
    raise(w);
  }

  // Win+arrows on the window in front, as on Windows: Left and Right snap it to a half (the other way from a
  // half puts it back), Up maximizes it, Down puts a snapped or maximized window back and minimizes one that is
  // not. The bottom 44 px stay free for the tray. Its size before snapping is kept in w.restore.
  function snap(how: string) {
    var w = topWindow();
    if (!w) { say(explorers.length ? 'Every window is minimized. Win+Tab shows them.' : 'No window to move. Win+E opens one.'); return; }
    var now = w.snap;
    var to = how === 'down' ? (now ? '' : 'min') : how === 'up' ? 'max'
      : now === (how === 'left' ? 'right' : 'left') ? '' : how;
    if (to === 'min') { minimize(w); draw(); playSound('close'); return; }
    if (!now) w.restore = [w.left, w.top, w.width, w.height];
    var wide = layer.clientWidth, tall = layer.clientHeight - 44, half = Math.floor(wide / 2);
    var r = to === 'left' ? [0, 0, half, tall] : to === 'right' ? [half, 0, wide - half, tall]
      : to === 'max' ? [0, 0, wide, tall] : w.restore!;
    w.snap = to;
    w.moving = true;
    w.left = r[0];
    w.top = r[1];
    w.width = r[2];
    w.height = r[3];
    clearTimeout(w.timer);
    w.timer = setTimeout(function () { w!.moving = false; draw(); }, 260);
    raise(w);
    draw();
    playSound('open');
  }

  // Win+D: every window out of the way, and pressed again (or Show desktop in the tray) back where it was.
  function showDesktop() {
    var up = shown();
    if (up.length || menu || runBox || tasks) {
      if (menu) closeMenu();
      if (runBox) closeRun();
      if (tasks) closeTasks();
      up.forEach(function (w) { w.min = true; if (active === w) active = null; });
      peeked = (peeked || []).concat(up);
      draw();
      playSound('close');
    } else if (peeked && peeked.length) {
      peeked.filter(function (w) { return explorers.indexOf(w) >= 0; }).forEach(restore);
      peeked = null;
      draw();
      playSound('open');
    } else {
      say('Nothing to hide. Win+E opens a window.');
    }
  }

  // Win+R: the Run box, at the bottom left as on Windows. It opens an app by its name (or the start of it), and
  // "explorer" opens File Explorer.
  function runInput() { return layer.querySelector<HTMLInputElement>('.shim-run input')!; }
  function closeRun() {
    if (!runBox) return;
    runBox = null;
    draw();
  }
  function openRun() {
    if (runBox) { runInput().focus({ preventScroll: true }); return; }
    runBox = { seq: ++seq, z: ++front, error: '' };
    draw();
    runInput().focus({ preventScroll: true });
    playSound('open');
    need();
  }
  function match(q: string) {
    var named = function (a: CatalogApp) { return [a.name.toLowerCase(), a.id.toLowerCase()]; };
    return apps!.filter(function (a) { return named(a).indexOf(q) >= 0; })[0] ||
      apps!.filter(function (a) { return named(a).some(function (n) { return n.indexOf(q) === 0; }); })[0] ||
      apps!.filter(function (a) { return named(a).some(function (n) { return n.indexOf(q) >= 0; }); })[0];
  }
  function runTyped() {
    if (!apps || !runBox) return;
    var typed = runInput().value.trim(), q = typed.toLowerCase();
    if (!q) return;
    if (q === 'explorer' || q === 'explorer.exe') { closeRun(); openExplorer(); return; }
    var app = match(q);
    if (app) { play(app); return; }
    runBox.error = 'Windows cannot find \'' + typed + '\'. Make sure you typed the name correctly, and then try again.';
    draw();
  }
  function runClear() {
    if (runBox && runBox.error) { runBox.error = ''; draw(); }
  }

  // Win+Tab: task view, every File Explorer window as a card (a scaled copy of it) to bring to the front or close.
  function closeTasks() {
    if (!tasks) return;
    tasks = null;
    draw();
  }
  function openTasks() {
    if (tasks) { closeTasks(); return; }
    tasks = {
      seq: ++seq,
      head: explorers.length ? 'Task view: ' + explorers.length + (explorers.length === 1 ? ' window' : ' windows')
        : 'Task view: no windows open. Win+E opens File Explorer.',
      cards: explorers.slice().sort(function (a, b) { return b.z - a.z; }).map(function (w, i) {
        return { w: w, n: i + 1, min: w.min, scale: 200 / Math.max(1, (w.el && w.el.offsetWidth) || w.width) };
      }),
    };
    draw();
    var first = layer.querySelector<HTMLElement>('.shim-tasks button');
    if (first) first.focus({ preventScroll: true });
    playSound('open');
  }
  function pickTask(w: Win) {
    tasks = null;
    restore(w);
    draw();
  }
  function closeTask(w: Win) {
    forget(w);
    tasks!.cards = tasks!.cards.filter(function (c) { return c.w !== w; });
    if (!explorers.length) tasks = null;
    draw();
  }

  const shell: ShellModel = {
    get menu() { return menu; }, get explorers() { return explorers; }, get tray() { return tray; }, get active() { return active; },
    get apps() { return apps; }, get runBox() { return runBox; }, get tasks() { return tasks; }, get note() { return note; },
    closeMenu, closeAll, closeExplorer, raise: function (w) { raise(w); draw(); }, select, showDesktop, closeRun, runTyped, runClear,
    closeTasks, pickTask, closeTask, dragStart, dragMove, dragEnd,
    filter: function (q) { menu!.query = q; draw(); },
  };
  openLayer(<Shell shell={shell} onMount={(el, r) => { layer = el; redraw = r; }} />);

  // The keys Windows+E's keyboard sends with the Windows key held (Windows itself only sends the key alone).
  var SHORTCUTS: Record<number, () => void> = {
    524: function () { if (menu) closeMenu(); else openMenu(); },
    69: openExplorer,
    68: showDesktop,
    82: openRun,
    9: openTasks,
    37: function () { snap('left'); },
    38: function () { snap('up'); },
    39: function () { snap('right'); },
    40: function () { snap('down'); }
  };
  window.__shimShell = function (keycode: number) {
    if (SHORTCUTS[keycode]) SHORTCUTS[keycode]();
  };
  return { holdExit: function (show: () => void) { if (!menu) return false; heldExit = show; return true; } };
}

/** What the layer is drawn from, and what its controls do. */
interface ShellModel {
  readonly menu: { seq: number; query: string } | null; readonly explorers: Win[]; readonly tray: { seq: number } | null;
  readonly active: Win | null; readonly apps: CatalogApp[] | null; readonly runBox: Run | null; readonly tasks: Tasks | null;
  readonly note: { seq: number; text: string } | null;
  closeMenu(): void; closeAll(): void; closeExplorer(w: Win): void; raise(w: Win): void; select(w: Win, app: CatalogApp): void;
  showDesktop(): void; closeRun(): void; runTyped(): void; runClear(): void; closeTasks(): void; pickTask(w: Win): void; closeTask(w: Win): void;
  dragStart(e: ReactPointerEvent<HTMLElement>, box: Win | Run, el: HTMLElement): void;
  dragMove(e: ReactPointerEvent<HTMLElement>, box: Win | Run, el: HTMLElement): void;
  dragEnd(): void; filter(q: string): void;
}

/** The layer over the stage, and what is on it, in the order each was put there. */
function Shell({ shell, onMount }: { shell: ShellModel; onMount: (layer: HTMLDivElement, redraw: () => void) => void }) {
  const layer = useRef<HTMLDivElement>(null);
  const [, setTick] = useState(0);
  useLayoutEffect(() => { onMount(layer.current!, () => flushSync(() => setTick(n => n + 1))); }, []);
  const parts: [number, ReactNode][] = [];
  if (shell.menu) parts.push([shell.menu.seq, <StartMenu key="menu" shell={shell} query={shell.menu.query} />]);
  if (shell.tray) parts.push([shell.tray.seq, <Tray key="tray" shell={shell} />]);
  for (const w of shell.explorers) parts.push([w.seq, <Explorer key={'w' + w.seq} shell={shell} w={w} />]);
  if (shell.note) parts.push([shell.note.seq, <div key={'note' + shell.note.seq} className="shim-shell-note" role="status">{shell.note.text}</div>]);
  if (shell.runBox) parts.push([shell.runBox.seq, <RunBox key="run" shell={shell} run={shell.runBox} />]);
  if (shell.tasks) parts.push([shell.tasks.seq, <TaskView key="tasks" shell={shell} tasks={shell.tasks} />]);
  parts.sort((a, b) => a[0] - b[0]);
  return <div className="shim-shell" ref={layer}>{parts.map(p => p[1])}</div>;
}

function Pic({ app, className }: { app: CatalogApp; className: string }) {
  return <span className={className}><img src={app.shot} alt="" /></span>;
}

function StartMenu({ shell, query }: { shell: ShellModel; query: string }) {
  const box = useRef<HTMLDivElement>(null);
  useOwnKeys(box, shell.closeMenu);
  const apps = shell.apps, q = query.trim().toLowerCase();
  const hit = (app: CatalogApp) => !q || (app.name + ' ' + app.id + ' ' + app.hint).toLowerCase().indexOf(q) >= 0;
  const hits = apps ? apps.filter(hit) : [];
  return (
    <div className="shim-start" role="dialog" aria-label="Start" ref={box}>
      <input className="shim-start-search" type="search" placeholder="Type here to search" aria-label="Search apps"
        onInput={e => shell.filter(e.currentTarget.value)}
        onKeyDownCapture={e => {
          if (e.key === 'Enter' && hits.length) box.current!.querySelector<HTMLElement>('.shim-tile:not([hidden])')!.click();
        }} />
      <div className="shim-start-head">
        <strong>{!apps || !q ? 'All apps' : hits.length ? 'Best match' : 'Search'}</strong>
        <span>{apps ? hits.length + (hits.length === 1 ? ' app' : ' apps') : ''}</span>
      </div>
      <div className="shim-start-grid" hidden={!!apps && !hits.length}>
        {apps && apps.map(app => (
          <a key={app.id} className={'shim-tile' + (q && app === hits[0] ? ' first' : '')} {...playProps(app)} title={app.hint} hidden={!hit(app)}>
            <span className="shim-tile-pic">
              <img src={app.shot} alt="" />
              {played[app.id] && <span className="shim-badge played" title="Played">{'✓'}</span>}
            </span>
            <span className="shim-tile-name">{app.name}</span>
          </a>
        ))}
      </div>
      <p className="shim-start-none" hidden={!apps || hits.length > 0}>{apps ? 'No app matches "' + query.trim() + '".' : ''}</p>
      <div className="shim-start-foot">
        <span className="shim-start-user">{apps ? 'Visitor · ' + apps.filter(a => played[a.id]).length + ' of ' + apps.length + ' apps played' : 'Visitor'}</span>
        <button className="shim-start-power" type="button" title="Close the Start menu" aria-label="Close the Start menu" data-sound="none"
          onClick={shell.closeMenu}>{'⏻'}</button>
      </div>
    </div>
  );
}

function Tray({ shell }: { shell: ShellModel }) {
  const n = shell.explorers.length;
  return (
    <div className="shim-shell-tray">
      <span className="shim-folder-mini" />
      <span className="shim-shell-count">{'File Explorer: ' + n + (n === 1 ? ' window' : ' windows') + (n >= MAX_EXPLORERS ? ' (the most this page opens)' : '')}</span>
      <button type="button" title="Hide the windows, or bring them back (Win+D)" onClick={shell.showDesktop}>Show desktop</button>
      <button type="button" onClick={shell.closeAll}>Close all</button>
    </div>
  );
}

/** A File Explorer window on the folder of apps, or (copy, its scale) its picture in task view, which does nothing. */
function Explorer({ shell, w, copy }: { shell: ShellModel; w: Win; copy?: number }) {
  const box = useRef<HTMLDivElement>(null);
  useOwnKeys(box);
  useLayoutEffect(() => { if (!copy) w.el = box.current; }, []);
  const apps = shell.apps;
  const cls = 'shim-explorer' + (shell.active === w ? ' active' : '') + (w.min && !copy ? ' min' : '') + (w.moving && !copy ? ' moving' : '');
  const style = copy ? { left: 0, top: 0, width: w.width, height: w.height, zIndex: w.z, transform: 'scale(' + copy + ')' }
    : { left: w.left, top: w.top, width: w.width, height: w.height, zIndex: w.z };
  return (
    <div className={cls} role={copy ? undefined : 'dialog'} aria-label="File Explorer: java-applications" data-snap={w.snap || undefined}
      aria-hidden={copy ? 'true' : undefined} inert={!!copy} style={style} ref={box}
      onPointerDown={copy ? undefined : () => shell.raise(w)}>
      <div className="shim-ex-title"
        onPointerDown={copy ? undefined : e => shell.dragStart(e, w, box.current!)}
        onPointerMove={copy ? undefined : e => shell.dragMove(e, w, box.current!)}
        onPointerUp={shell.dragEnd} onPointerCancel={shell.dragEnd}>
        <span className="shim-folder-mini" />
        <span className="shim-ex-name">java-applications</span>
        <CloseButton onClick={() => { if (!copy) shell.closeExplorer(w); }} sound="none" />
      </div>
      <div className="shim-ex-address">
        <span>This PC</span>
        <span className="shim-ex-sep">{'›'}</span>
        <span>java-applications</span>
      </div>
      <div className="shim-ex-items">
        {apps && apps.map(app => (
          <button key={app.id} type="button" className={'shim-ex-item' + (w.sel === app ? ' sel' : '')} title={app.name + ': ' + app.hint} data-sound="none"
            onClick={copy ? undefined : () => shell.select(w, app)} onDoubleClick={copy ? undefined : () => play(app)}
            onKeyDownCapture={e => {
              if (copy || e.key !== 'Enter') return;
              e.preventDefault();
              play(app);
            }}>
            <Pic app={app} className="shim-folder" />
            <span className="shim-ex-label">{app.id}</span>
          </button>
        ))}
      </div>
      <div className="shim-ex-status">
        <span className="shim-ex-info">{w.sel ? w.sel.name + ': ' + w.sel.hint : apps ? apps.length + ' items' : ''}</span>
        <button type="button" hidden={!w.sel} onClick={() => { if (w.sel && !copy) play(w.sel); }}>Open</button>
      </div>
    </div>
  );
}

function RunBox({ shell, run }: { shell: ShellModel; run: Run }) {
  const box = useRef<HTMLDivElement>(null);
  const cancel = () => { shell.closeRun(); playSound('close'); };
  useOwnKeys(box, cancel);
  return (
    <div className="shim-run" role="dialog" aria-label="Run" style={{ zIndex: run.z, left: run.left, top: run.top }} ref={box}>
      <div className="shim-ex-title"
        onPointerDown={e => shell.dragStart(e, run, box.current!)}
        onPointerMove={e => shell.dragMove(e, run, box.current!)}
        onPointerUp={shell.dragEnd} onPointerCancel={shell.dragEnd}>
        <span className="shim-run-icon" />
        <span className="shim-ex-name">Run</span>
        <CloseButton onClick={cancel} sound="none" />
      </div>
      <div className="shim-run-body">
        <p>Type the name of an app, and Windows will open it for you.</p>
        <label className="shim-run-row">
          {'Open: '}
          <input type="text" list="shim-run-apps" autoComplete="off" spellCheck={false}
            onInput={shell.runClear} onKeyDownCapture={e => { if (e.key === 'Enter') shell.runTyped(); }} />
        </label>
        <datalist id="shim-run-apps">
          {shell.apps && shell.apps.map(app => <option key={app.id} value={app.name} />)}
        </datalist>
        <p className="shim-run-error" role="alert">{run.error}</p>
        <div className="shim-run-buttons">
          <button type="button" onClick={shell.runTyped}>OK</button>
          <button type="button" onClick={cancel}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

function TaskView({ shell, tasks }: { shell: ShellModel; tasks: Tasks }) {
  const view = useRef<HTMLDivElement>(null);
  const grid = useRef<HTMLDivElement>(null);
  useOwnKeys(view, shell.closeTasks);
  return (
    <div className="shim-tasks" role="dialog" aria-label="Task view" ref={view}
      onClick={e => { if (e.target === view.current || e.target === grid.current) shell.closeTasks(); }}>
      <strong className="shim-tasks-head">{tasks.head}</strong>
      <div className="shim-tasks-grid" ref={grid}>
        {tasks.cards.map(c => (
          <div key={c.w.seq} className="shim-task">
            <button type="button" className="shim-task-pick" aria-label={'File Explorer window ' + c.n + (c.min ? ', minimized' : '')}
              onClick={() => shell.pickTask(c.w)}>
              <span className="shim-task-pic"><Explorer shell={shell} w={c.w} copy={c.scale} /></span>
              <span className="shim-task-name">{'File Explorer' + (c.min ? ' (minimized)' : '')}</span>
            </button>
            <CloseButton onClick={() => shell.closeTask(c.w)} />
          </div>
        ))}
      </div>
    </div>
  );
}
