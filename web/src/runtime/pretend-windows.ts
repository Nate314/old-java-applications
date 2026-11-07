import { played } from './passport';
import { catalog, type CatalogApp } from './catalog';
import { closeButton, ownKeys, playLink } from './then-and-now';
import { playSound } from './sounds';

// ---- pretend Windows (<body data-shell>: Windows and Windows+E) ----
// Those two apps do nothing but press keys with java.awt.Robot: the Windows key once, or Win+E over and
// over. The shim's Robot hands Windows key shortcuts to window.__shimShell instead of the app's window, and
// this plays the Windows they were written for, inside the stage: the Windows key alone opens a Start menu
// of every app on the site (type to search, Enter to play), and Win+E opens a File Explorer window on the
// folder of apps (double-click one to play it). Windows outlives the program that pressed its keys, so an
// exit waits until the Start menu is closed. Windows+E's keyboard also presses Win+D (show desktop), Win+R
// (a Run box that opens an app by name), Win+Tab (task view) and Win+arrows (snap, maximize, restore,
// minimize the File Explorer window in front). Returns { holdExit(show) }.
const MAX_EXPLORERS = 40;

/** A File Explorer window: its size before a snap (left, top, width, height) and its pending snap-slide timer. */
interface ExplorerWindow extends HTMLDivElement { _restore?: number[]; _moving?: ReturnType<typeof setTimeout> }

declare global {
  interface Window {
    /** The shim's Robot hands Windows key shortcuts (AWT key codes) here. */
    __shimShell?: (keycode: number) => void;
  }
}

export function pretendWindows() {
  var layer = element('div', 'shim-shell');
  document.body.appendChild(layer);
  var menu: HTMLDivElement | null = null, heldExit: (() => void) | null = null, explorers: ExplorerWindow[] = [], opened = 0, front = 0, tray: HTMLDivElement | null = null;
  var peeked: ExplorerWindow[] | null = null, runBox: HTMLDivElement | null = null, tasks: HTMLDivElement | null = null, note: HTMLDivElement | null = null;   // Win+D's hidden windows, Win+R, Win+Tab

  function element<K extends keyof HTMLElementTagNameMap>(tag: K, cls?: string | null, text?: string) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text) e.textContent = text;
    return e;
  }
  function go(app: CatalogApp) {
    var a = document.createElement('a');
    playLink(a, app);
    a.click();
  }
  function picture(app: CatalogApp, cls: string) {
    var pic = element('span', cls);
    var img = element('img');
    img.src = app.shot;
    img.alt = '';
    pic.appendChild(img);
    return pic;
  }

  function closeMenu() {
    if (!menu) return;
    menu.remove();
    menu = null;
    document.removeEventListener('pointerdown', outside, true);
    playSound('close');
    if (heldExit) {
      var show = heldExit;
      heldExit = null;
      show();
    }
  }
  function outside(e: Event) { if (menu && !menu.contains(e.target as Node)) closeMenu(); }
  function openMenu() {
    var box = menu = element('div', 'shim-start');
    box.setAttribute('role', 'dialog');
    box.setAttribute('aria-label', 'Start');
    var search = element('input', 'shim-start-search');
    search.type = 'search';
    search.placeholder = 'Type here to search';
    search.setAttribute('aria-label', 'Search apps');
    var head = element('div', 'shim-start-head');
    var heading = element('strong', null, 'All apps');
    var count = element('span');
    head.appendChild(heading);
    head.appendChild(count);
    var grid = element('div', 'shim-start-grid');
    var none = element('p', 'shim-start-none');
    none.hidden = true;
    var foot = element('div', 'shim-start-foot');
    var who = element('span', 'shim-start-user', 'Visitor');
    var power = element('button', 'shim-start-power', '⏻');
    power.type = 'button';
    power.title = 'Close the Start menu';
    power.setAttribute('aria-label', 'Close the Start menu');
    power.setAttribute('data-sound', 'none');
    power.addEventListener('click', closeMenu);
    foot.appendChild(who);
    foot.appendChild(power);
    [search, head, grid, none, foot].forEach(function (n) { box.appendChild(n); });
    ownKeys(box, closeMenu);
    layer.appendChild(box);
    document.addEventListener('pointerdown', outside, true);
    playSound('open');
    search.focus({ preventScroll: true });
    catalog().then(function (apps) {
      if (menu !== box) return;
      var tiles = apps.map(function (app) {
        var a = element('a', 'shim-tile');
        playLink(a, app);
        a.title = app.hint;
        var pic = picture(app, 'shim-tile-pic');
        if (played[app.id]) {
          var badge = element('span', 'shim-badge played', '✓');
          badge.title = 'Played';
          pic.appendChild(badge);
        }
        a.appendChild(pic);
        a.appendChild(element('span', 'shim-tile-name', app.name));
        grid.appendChild(a);
        return { app: app, a: a };
      });
      function shown() { return tiles.filter(function (t) { return !t.a.hidden; }); }
      function filter() {
        var q = search.value.trim().toLowerCase();
        tiles.forEach(function (t) {
          t.a.hidden = !!q && (t.app.name + ' ' + t.app.id + ' ' + t.app.hint).toLowerCase().indexOf(q) < 0;
          t.a.classList.remove('first');
        });
        var hits = shown();
        if (q && hits.length) hits[0].a.classList.add('first');
        heading.textContent = !q ? 'All apps' : hits.length ? 'Best match' : 'Search';
        count.textContent = hits.length + (hits.length === 1 ? ' app' : ' apps');
        none.hidden = hits.length > 0;
        grid.hidden = !hits.length;
        none.textContent = 'No app matches "' + search.value.trim() + '".';
      }
      search.addEventListener('input', filter);
      search.addEventListener('keydown', function (e) {
        var hits = shown();
        if (e.key === 'Enter' && hits.length) hits[0].a.click();
      });
      filter();
      var n = apps.filter(function (a) { return played[a.id]; }).length;
      who.textContent = 'Visitor · ' + n + ' of ' + apps.length + ' apps played';
    });
  }

  function showTray() {
    if (!explorers.length) {
      if (tray) tray.remove();
      tray = null;
      return;
    }
    if (!tray) {
      tray = element('div', 'shim-shell-tray');
      tray.appendChild(element('span', 'shim-folder-mini'));
      tray.appendChild(element('span', 'shim-shell-count'));
      var desk = element('button', null, 'Show desktop');
      desk.type = 'button';
      desk.title = 'Hide the windows, or bring them back (Win+D)';
      desk.addEventListener('click', showDesktop);
      tray.appendChild(desk);
      var all = element('button', null, 'Close all');
      all.type = 'button';
      all.addEventListener('click', function () {
        explorers.forEach(function (w) { w.remove(); });
        explorers = [];
        peeked = null;
        showTray();
      });
      tray.appendChild(all);
      layer.appendChild(tray);
    }
    tray.querySelector('.shim-shell-count')!.textContent = 'File Explorer: ' + explorers.length +
      (explorers.length === 1 ? ' window' : ' windows') + (explorers.length >= MAX_EXPLORERS ? ' (the most this page opens)' : '');
  }
  function raise(w: ExplorerWindow) {
    w.style.zIndex = String(++front);
    explorers.forEach(function (o) { o.classList.toggle('active', o === w); });
  }
  function draggable(w: ExplorerWindow, handle: HTMLElement) {
    handle.addEventListener('pointerdown', function (e) {
      if (e.button !== 0 || (e.target as Element).closest('button')) return;
      var x = e.clientX - w.offsetLeft, y = e.clientY - w.offsetTop;
      handle.setPointerCapture(e.pointerId);
      function move(m: PointerEvent) {
        if (w.hasAttribute('data-snap') && w._restore) {   // dragging a snapped window takes it back to its size
          w.removeAttribute('data-snap');
          x = Math.min(x, w._restore[2] / 2);
          w.style.width = w._restore[2] + 'px';
          w.style.height = w._restore[3] + 'px';
        }
        w.style.left = Math.max(80 - w.offsetWidth, Math.min(layer.clientWidth - 80, m.clientX - x)) + 'px';
        w.style.top = Math.max(0, Math.min(layer.clientHeight - 32, m.clientY - y)) + 'px';
      }
      function up() {
        handle.removeEventListener('pointermove', move);
        handle.removeEventListener('pointerup', up);
        handle.removeEventListener('pointercancel', up);
      }
      handle.addEventListener('pointermove', move);
      handle.addEventListener('pointerup', up);
      handle.addEventListener('pointercancel', up);
    });
  }
  function openExplorer() {
    if (explorers.length >= MAX_EXPLORERS) return;
    var w: ExplorerWindow = element('div', 'shim-explorer');
    w.setAttribute('role', 'dialog');
    w.setAttribute('aria-label', 'File Explorer: java-applications');
    var bar = element('div', 'shim-ex-title');
    bar.appendChild(element('span', 'shim-folder-mini'));
    bar.appendChild(element('span', 'shim-ex-name', 'java-applications'));
    var close = closeButton(function () {
      w.remove();
      explorers = explorers.filter(function (o) { return o !== w; });
      showTray();
      playSound('close');
    });
    close.setAttribute('data-sound', 'none');
    bar.appendChild(close);
    var address = element('div', 'shim-ex-address');
    ['This PC', 'java-applications'].forEach(function (crumb, i) {
      if (i) address.appendChild(element('span', 'shim-ex-sep', '›'));
      address.appendChild(element('span', null, crumb));
    });
    var items = element('div', 'shim-ex-items');
    var status = element('div', 'shim-ex-status');
    var info = element('span', 'shim-ex-info');
    var open = element('button', null, 'Open');
    open.type = 'button';
    open.hidden = true;
    status.appendChild(info);
    status.appendChild(open);
    [bar, address, items, status].forEach(function (n) { w.appendChild(n); });
    ownKeys(w);
    w.addEventListener('pointerdown', function () { raise(w); });
    draggable(w, bar);

    // Cascaded down and to the right from the top left, as Windows does, starting over when the next would
    // not fit. The bottom 44 px are left to the tray.
    var tall = layer.clientHeight - 44;
    var width = Math.min(560, layer.clientWidth - 16), height = Math.max(160, Math.min(380, tall - 16));
    var step = 28;
    var room = Math.max(1, Math.floor(Math.min(layer.clientWidth - width, tall - height) / step));
    var at = 8 + (opened++ % room) * step;
    w.style.left = at + 'px';
    w.style.top = at + 'px';
    w.style.width = width + 'px';
    w.style.height = height + 'px';
    layer.appendChild(w);
    explorers.push(w);
    raise(w);
    showTray();
    playSound('open');

    catalog().then(function (apps) {
      var selected: CatalogApp | null = null;
      info.textContent = apps.length + ' items';
      open.addEventListener('click', function () { if (selected) go(selected); });
      apps.forEach(function (app) {
        var b = element('button', 'shim-ex-item');
        b.type = 'button';
        b.title = app.name + ': ' + app.hint;
        b.setAttribute('data-sound', 'none');
        b.appendChild(picture(app, 'shim-folder'));
        b.appendChild(element('span', 'shim-ex-label', app.id));
        b.addEventListener('click', function () {
          selected = app;
          Array.prototype.forEach.call(items.children, function (o: Element) { o.classList.toggle('sel', o === b); });
          info.textContent = app.name + ': ' + app.hint;
          open.hidden = false;
        });
        b.addEventListener('dblclick', function () { go(app); });
        b.addEventListener('keydown', function (e) {
          if (e.key !== 'Enter') return;
          e.preventDefault();
          go(app);
        });
        items.appendChild(b);
      });
    });
  }

  // A line at the top of the layer for a shortcut with nothing to act on.
  function say(text: string) {
    if (note) note.remove();
    var n = note = element('div', 'shim-shell-note', text);
    n.setAttribute('role', 'status');
    layer.appendChild(n);
    setTimeout(function () { if (note === n) { n.remove(); note = null; } }, 2600);
  }
  function shown() { return explorers.filter(function (w) { return !w.classList.contains('min'); }); }
  function topWindow() {
    return shown().reduce<ExplorerWindow | null>(function (best, w) { return !best || +w.style.zIndex > +best.style.zIndex ? w : best; }, null);
  }
  function minimize(w: ExplorerWindow) {
    w.classList.add('min');
    w.classList.remove('active');
    var next = topWindow();
    if (next) raise(next);
  }
  function restore(w: ExplorerWindow) {
    w.classList.remove('min');
    raise(w);
  }

  // Win+arrows on the window in front, as on Windows: Left and Right snap it to a half (the other way from a
  // half puts it back), Up maximizes it, Down puts a snapped or maximized window back and minimizes one that is
  // not. The bottom 44 px stay free for the tray. Its size before snapping is kept in w._restore.
  function snap(how: string) {
    var w = topWindow();
    if (!w) { say(explorers.length ? 'Every window is minimized. Win+Tab shows them.' : 'No window to move. Win+E opens one.'); return; }
    var now = w.getAttribute('data-snap') || '';
    var to = how === 'down' ? (now ? '' : 'min') : how === 'up' ? 'max'
      : now === (how === 'left' ? 'right' : 'left') ? '' : how;
    if (to === 'min') { minimize(w); playSound('close'); return; }
    // From its style, not its layout, which lags behind while a snap is still sliding.
    if (!now) w._restore = (['left', 'top', 'width', 'height'] as const).map(function (k) { return parseFloat(w!.style[k]) || 0; });
    var wide = layer.clientWidth, tall = layer.clientHeight - 44, half = Math.floor(wide / 2);
    var r = to === 'left' ? [0, 0, half, tall] : to === 'right' ? [half, 0, wide - half, tall]
      : to === 'max' ? [0, 0, wide, tall] : w._restore!;
    if (to) w.setAttribute('data-snap', to); else w.removeAttribute('data-snap');
    w.classList.add('moving');
    w.style.left = r[0] + 'px';
    w.style.top = r[1] + 'px';
    w.style.width = r[2] + 'px';
    w.style.height = r[3] + 'px';
    clearTimeout(w._moving);
    w._moving = setTimeout(function () { w!.classList.remove('moving'); }, 260);
    raise(w);
    playSound('open');
  }

  // Win+D: every window out of the way, and pressed again (or Show desktop in the tray) back where it was.
  function showDesktop() {
    var up = shown();
    if (up.length || menu || runBox || tasks) {
      if (menu) closeMenu();
      if (runBox) closeRun();
      if (tasks) closeTasks();
      up.forEach(function (w) { w.classList.add('min'); w.classList.remove('active'); });
      peeked = (peeked || []).concat(up);
      playSound('close');
    } else if (peeked && peeked.length) {
      peeked.filter(function (w) { return explorers.indexOf(w) >= 0; }).forEach(restore);
      peeked = null;
      playSound('open');
    } else {
      say('Nothing to hide. Win+E opens a window.');
    }
  }

  // Win+R: the Run box, at the bottom left as on Windows. It opens an app by its name (or the start of it), and
  // "explorer" opens File Explorer.
  function closeRun() {
    if (!runBox) return;
    runBox.remove();
    runBox = null;
  }
  function openRun() {
    if (runBox) { runBox.querySelector('input')!.focus({ preventScroll: true }); return; }
    var box = runBox = element('div', 'shim-run');
    box.setAttribute('role', 'dialog');
    box.setAttribute('aria-label', 'Run');
    var bar = element('div', 'shim-ex-title');
    bar.appendChild(element('span', 'shim-run-icon'));
    bar.appendChild(element('span', 'shim-ex-name', 'Run'));
    var x = closeButton(function () { closeRun(); playSound('close'); });
    x.setAttribute('data-sound', 'none');
    bar.appendChild(x);
    var body = element('div', 'shim-run-body');
    body.appendChild(element('p', null, 'Type the name of an app, and Windows will open it for you.'));
    var row = element('label', 'shim-run-row', 'Open: ');
    var input = element('input');
    input.type = 'text';
    input.setAttribute('list', 'shim-run-apps');
    input.setAttribute('autocomplete', 'off');
    input.spellcheck = false;
    row.appendChild(input);
    var list = element('datalist');
    list.id = 'shim-run-apps';
    var error = element('p', 'shim-run-error');
    error.setAttribute('role', 'alert');
    var buttons = element('div', 'shim-run-buttons');
    var ok = element('button', null, 'OK');
    ok.type = 'button';
    var cancel = element('button', null, 'Cancel');
    cancel.type = 'button';
    cancel.addEventListener('click', function () { closeRun(); playSound('close'); });
    buttons.appendChild(ok);
    buttons.appendChild(cancel);
    [row, list, error, buttons].forEach(function (n) { body.appendChild(n); });
    box.appendChild(bar);
    box.appendChild(body);
    ownKeys(box, function () { closeRun(); playSound('close'); });
    draggable(box, bar);
    box.style.zIndex = String(++front);
    layer.appendChild(box);
    input.focus({ preventScroll: true });
    playSound('open');
    catalog().then(function (apps) {
      if (runBox !== box) return;
      apps.forEach(function (app) {
        var o = element('option');
        o.value = app.name;
        list.appendChild(o);
      });
      function match(q: string) {
        var named = function (a: CatalogApp) { return [a.name.toLowerCase(), a.id.toLowerCase()]; };
        return apps.filter(function (a) { return named(a).indexOf(q) >= 0; })[0] ||
          apps.filter(function (a) { return named(a).some(function (n) { return n.indexOf(q) === 0; }); })[0] ||
          apps.filter(function (a) { return named(a).some(function (n) { return n.indexOf(q) >= 0; }); })[0];
      }
      function runTyped() {
        var typed = input.value.trim(), q = typed.toLowerCase();
        if (!q) return;
        if (q === 'explorer' || q === 'explorer.exe') { closeRun(); openExplorer(); return; }
        var app = match(q);
        if (app) { go(app); return; }
        error.textContent = 'Windows cannot find \'' + typed + '\'. Make sure you typed the name correctly, and then try again.';
      }
      ok.addEventListener('click', runTyped);
      input.addEventListener('keydown', function (e) { if (e.key === 'Enter') runTyped(); });
      input.addEventListener('input', function () { error.textContent = ''; });
    });
  }

  // Win+Tab: task view, every File Explorer window as a card (a scaled copy of it) to bring to the front or close.
  function closeTasks() {
    if (!tasks) return;
    tasks.remove();
    tasks = null;
  }
  function openTasks() {
    if (tasks) { closeTasks(); return; }
    var view = tasks = element('div', 'shim-tasks');
    view.setAttribute('role', 'dialog');
    view.setAttribute('aria-label', 'Task view');
    view.appendChild(element('strong', 'shim-tasks-head', explorers.length ? 'Task view: ' + explorers.length +
      (explorers.length === 1 ? ' window' : ' windows') : 'Task view: no windows open. Win+E opens File Explorer.'));
    var grid = element('div', 'shim-tasks-grid');
    explorers.slice().sort(function (a, b) { return +b.style.zIndex - +a.style.zIndex; }).forEach(function (w, i) {
      var card = element('div', 'shim-task');
      var pick = element('button', 'shim-task-pick');
      pick.type = 'button';
      pick.setAttribute('aria-label', 'File Explorer window ' + (i + 1) + (w.classList.contains('min') ? ', minimized' : ''));
      var pic = element('span', 'shim-task-pic');
      var copy = w.cloneNode(true) as ExplorerWindow;
      copy.classList.remove('min', 'moving');
      copy.removeAttribute('role');
      copy.setAttribute('aria-hidden', 'true');
      copy.inert = true;
      copy.style.left = copy.style.top = '0';
      copy.style.transform = 'scale(' + (200 / Math.max(1, w.offsetWidth || parseInt(w.style.width, 10))) + ')';
      pic.appendChild(copy);
      pick.appendChild(pic);
      pick.appendChild(element('span', 'shim-task-name', 'File Explorer' + (w.classList.contains('min') ? ' (minimized)' : '')));
      pick.addEventListener('click', function () { closeTasks(); restore(w); });
      var x = closeButton(function () {
        w.remove();
        explorers = explorers.filter(function (o) { return o !== w; });
        card.remove();
        showTray();
        if (!explorers.length) closeTasks();
      });
      card.appendChild(pick);
      card.appendChild(x);
      grid.appendChild(card);
    });
    view.appendChild(grid);
    view.addEventListener('click', function (e) { if (e.target === view || e.target === grid) closeTasks(); });
    ownKeys(view, closeTasks);
    layer.appendChild(view);
    var first = view.querySelector('button');
    if (first) first.focus({ preventScroll: true });
    playSound('open');
  }

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
