/*
 * Find (Ctrl+K, or / on the home page): one box for the whole site, loaded by boot.js the first time it opens.
 * Type a few letters and it finds
 * - apps, by name, hint or category, and their other ways to play: as they were first restored (Then) and, for the four
 *   games with a computer player, watching it play;
 * - things to do on this page or anywhere: skins, sound, fit, share, replay, the source, the passport, records,
 *   a random app not played yet;
 * - every Java source file of every app (each app's source/files.txt), opened in its app's Source panel.
 * With nothing typed it offers the apps played last, a few not played yet and what this page can do.
 * boot.js gives it what it needs through window.oldjavaSite (catalog, the passport, WATCH, sounds).
 */

declare global {
  interface Window {
  oldjavaFind?: { open(opener?: Element | null): void; isOpen(): boolean; key: string };
  }
}
// What boot.js's find.ts has lent window.oldjavaSite by the time this file runs.
type FindSite = OldjavaSite & Required<Pick<OldjavaSite, 'catalog' | 'isThen' | 'played' | 'watchable' | 'sound'>>;
// A row the box can show: an app, its Then or Watch version, a thing to do, or a source file.
interface Item {
  kind: string; app?: OldjavaCatalogApp; name?: string; title: string; sub: string; keys: string;
  pic?: string; glyph?: string; tag: string; tagNew?: boolean; current?: boolean; boost?: number;
  run(full?: boolean): void;
}
(function () {
  'use strict';

  var site = window.oldjavaSite as FindSite;
  var MAC = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);
  var KEY = MAC ? '⌘K' : 'Ctrl K';
  var box: { close(): void } | null = null;

  function el<K extends keyof HTMLElementTagNameMap>(tag: K, cls?: string | null, text?: string | null) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  }
  function onHome() { return !document.body.hasAttribute('data-app'); }
  // A control of this page, if it is there and shown.
  function shown(selector: string | HTMLElement) {
    var n = typeof selector === 'string' ? document.querySelector<HTMLElement>(selector) : selector;
    return n && !n.hidden && n.getClientRects().length ? n : null;
  }
  function toolbarButton(text: string) {
    var all = document.querySelectorAll<HTMLElement>('.shim-toolbar button');
    for (var i = 0; i < all.length; i++) if (all[i].textContent.trim() === text) return shown(all[i]);
    return null;
  }

  // ---- matching ----
  // A score for query q (lower case) in text: a prefix beats a word start beats anywhere, and letters in order
  // ("msw" for Minesweeper) count for less. 0 is no match.
  // A word starts at i of text: after a space or a mark, or a capital after a small letter (GameFrame).
  function wordStart(text: string, i: number) {
    return i === 0 || /[^A-Za-z0-9]/.test(text[i - 1]) || (/[A-Z]/.test(text[i]) && /[a-z]/.test(text[i - 1]));
  }
  // Letters in order count only in a name (strict for the longer words: hints, paths).
  function score(q: string, text: string | undefined, strict?: boolean) {
    if (!text) return 0;
    var t = text.toLowerCase();
    var i = t.indexOf(q);
    if (i === 0) return 100;
    if (i > 0) return (wordStart(text, i) ? 85 : 60) - Math.min(i, 30) / 3;
    if (q.length < 2 || strict) return 0;
    var at = 0, s = 0, last = -2;
    for (var k = 0; k < q.length; k++) {
      var j = t.indexOf(q[k], at);
      if (j < 0) return 0;
      s += (j === last + 1 ? 2 : 0) + (wordStart(text, j) ? 3 : 0);
      last = j;
      at = j + 1;
    }
    return Math.min(45, 15 + s * 3 / q.length);
  }
  // Every word of the query must match the item's name (full weight) or its other words (half).
  function rank(item: Item, words: string[]) {
    var total = 0;
    for (var i = 0; i < words.length; i++) {
      var s = Math.max(score(words[i], item.name || item.title), score(words[i], item.title, true),
        score(words[i], item.keys, true) / 2);
      if (!s) return 0;
      total += s;
    }
    return total / words.length + (item.boost || 0);
  }

  // ---- what it can find ----
  function lastPlayed() {
    try { return JSON.parse(localStorage.getItem('oldjava.lastPlayed') || '{}') || {}; } catch (e) { return {}; }
  }

  function go(href: string) {
    var u = new URL(href, location.href);
    if (u.pathname === location.pathname && u.search === location.search && u.hash) location.hash = u.hash;
    else location.href = u.href;
  }

  // Opens an app: on the home page in its player (as the gallery's Play does), elsewhere on its own page.
  function playApp(app: OldjavaCatalogApp, full?: boolean) {
    var play = onHome() && !full && document.querySelector<HTMLElement>('#apps li[data-app="' + app.id + '"] a.play');
    if (play) {
      play.click();
      var player = document.getElementById('player');
      if (player) player.scrollIntoView({ block: 'start', behavior: 'smooth' });
      return;
    }
    go(site.root() + app.id + '/index.html');
  }

  function appItems(apps: OldjavaCatalogApp[]) {
    var here = site.appId();
    var thenHere = site.isThen();
    var items: Item[] = [];
    apps.forEach(function (app) {
      var cat = ({ game: 'Game', music: 'Music', toy: 'Toy', tool: 'Tool' } as Record<string, string>)[app.cat] || '';
      items.push({
        kind: 'app', app: app, title: app.name, sub: app.id === here && !thenHere ? 'Playing now' : app.hint,
        keys: app.id + ' ' + cat + ' ' + app.hint, pic: app.shot, tag: site.played(app.id) ? 'Played' : 'New',
        tagNew: !site.played(app.id), current: app.id === here && !thenHere,
        run: function (full?: boolean) { playApp(app, full); }
      });
      if (app.then) items.push({
        kind: 'then', app: app, name: app.name, title: app.name + ': as it was', sub: 'As first restored in 2026: light bug fixes, before the later reworks',
        keys: 'then old original recovered restored first 2016 2026 jar version ' + app.id, pic: app.shot, tag: 'Then', boost: -8,
        current: app.id === here && thenHere,
        run: function () { go(site.root() + app.id + '/then.html'); }
      });
      if (site.watchable.indexOf(app.id) >= 0) items.push({
        kind: 'watch', app: app, name: app.name, title: 'Watch the computer play ' + app.name, sub: 'It plays itself; click or press a key to take over',
        keys: 'watch computer bot ai demo plays itself ' + app.id, pic: app.shot, tag: 'Watch', boost: -6,
        run: function () { go(site.root() + app.id + '/index.html?watch'); }
      });
    });
    return items;
  }

  function action(title: string, sub: string, glyph: string, keys: string, run: (full?: boolean) => void): Item {
    return { kind: 'do', title: title, sub: sub, glyph: glyph, keys: keys, tag: 'Do', boost: 5, run: run };
  }
  function clickAction(selector: string, title: string, sub: string, glyph: string, keys: string) {
    var n = shown(selector);
    return n ? action(title, sub, glyph, keys, function () { n!.click(); }) : null;
  }
  // A tab of the home page's Your arcade panel, opened and brought into view.
  function tabAction(id: string, title: string, sub: string, glyph: string, keys: string) {
    var tab = document.getElementById(id);
    if (!tab || tab.hidden) return null;
    return action(title, sub, glyph, keys, function () {
      tab!.click();
      tab!.scrollIntoView({ block: 'start', behavior: 'smooth' });
      tab!.focus({ preventScroll: true });
    });
  }

  function actionItems(apps: OldjavaCatalogApp[]) {
    var list: Item[] = [];
    function add(a: Item | null) { if (a) list.push(a); }
    if (onHome()) {
      add(clickAction('#tour-start', 'Take the tour', 'All 20 apps in the player, one at a time, with something to try in each', '▶', 'tour guide walkthrough start begin all apps continue'));
      add(clickAction('#player-shuffle', 'Shuffle', 'Swap a random app into the player', '⇄', 'random shuffle any'));
      var big = shown('#player-big');
      add(clickAction('#player-big', big && big.getAttribute('aria-pressed') === 'true' ? 'Exit big screen' : 'Big screen',
        'Fill the screen with the player', '⛶', 'fullscreen big screen large'));
      add(clickAction('#player-photo', 'Take a photo', 'A picture of the app in the player, in an instant print or postcard frame', '▣', 'photo picture screenshot snapshot camera image postcard print'));
      add(clickAction('#player-restart', 'Restart the player\'s app', 'Start it again from the beginning', '↻', 'restart reload again'));
      add(tabAction('tab-news', 'What\'s new', 'The latest changes to the apps', '★', 'news changes updates history'));
      add(tabAction('tab-passport', 'Arcade passport', 'A stamp for every app you have played', '✓', 'passport stamps badges progress'));
      add(tabAction('tab-records', 'Your records', 'Best times, scores and stars; send them to your phone', '☰', 'records saves scores best export phone qr download'));
      add(tabAction('tab-takeaway', 'Take the arcade with you', 'Install it, and save every app to play offline', '⤓', 'install offline save download pwa'));
    } else {
      add(clickAction('.shim-help', 'How to play', 'Show this app\'s hint in the toolbar', '?', 'help how to play hint keys controls'));
      add(clickAction('.shim-source-open', 'Read the source', 'The Java this app was compiled from, beside it', '</>', 'source code java read'));
      add(clickAction('.shim-share-open', 'Share this app', 'Its link with a preview card', '↗', 'share link send'));
      add(clickAction('.shim-photo-open', 'Take a photo', 'A picture of the app in an instant print or postcard frame', '▣', 'photo picture screenshot snapshot camera image postcard print'));
      add(clickAction('.shim-replay', 'Instant replay', 'The last 10 to 20 seconds as a video', '●', 'replay video record clip save'));
      var zoom = shown('.shim-zoom');
      if (zoom) add(action(zoom.getAttribute('aria-pressed') === 'false' ? 'Fit to screen' : 'Actual size',
        'Zoom the app to the page, or back to its own size', '⤢', 'zoom fit actual size bigger smaller',
        function () { zoom!.click(); }));
      add(clickAction('.shim-dock-open', 'Side by side', 'Run a second app beside this one: a timer, the chord finder...', '◫',
        'side by side split two apps beside dock companion multitask second'));
      add(clickAction('.shim-pop', 'Pop out', 'Keep the app in a small window on top of everything', '⧉', 'pop out picture in picture window'));
      var controls = toolbarButton('Controls');
      if (controls) add(action('On-screen controller', 'Show or hide the touch buttons', '✚', 'controls pad touch controller buttons', function () { controls!.click(); }));
      var switcher = toolbarButton('Switch app');
      if (switcher) add(action('Switch app', 'Every app in a panel on the right', '☰', 'switch drawer list apps', function () { switcher!.click(); }));
      add(action('All apps', 'Back to the home page', '⌂', 'home all apps gallery back index', function () { go(site.root() + 'index.html'); }));
    }
    if (window.oldjavaShare && window.oldjavaShare.embed) add(action('Put an app on your site',
      'The tag that plays it in your own page, blog or class site', '</>', 'embed iframe widget website blog html code site put',
      function () { window.oldjavaShare.embed!(site.appId() || null); }));
    add(action('The story', 'How these 20 old programs were recovered and brought to the browser', '❦',
      'story history about how recovered decompiled then origin 2016 chapter', function () { go(site.root() + 'story.html'); }));
    var notYet = apps.filter(function (a) { return !site.played(a.id) && a.id !== site.appId(); });
    var pool = notYet.length ? notYet : apps.filter(function (a) { return a.id !== site.appId(); });
    if (pool.length) add(action('Surprise me', notYet.length ? 'Open an app you have not played yet (' + notYet.length + ' left)' : 'Open a random app',
      '⚄', 'random surprise lucky pick for me new unplayed', function () {
        playApp(pool[Math.floor(Math.random() * pool.length)]);
      }));
    var skin = window.oldjavaSkin && window.oldjavaSkin.get();
    [['modern', 'Modern', 'Clean and flat, follows your light or dark mode', 'dark light'],
     ['classic', 'Classic', 'Swing\'s own Metal look, as in 2016', 'metal swing old'],
     ['arcade', 'Arcade', 'Neon CRT cabinet with scanlines', 'dark neon crt retro']].forEach(function (s) {
      if (s[0] !== skin) add(action('Skin: ' + s[1], s[2], '◐', 'skin theme look style ' + s[3], function () { window.oldjavaSkin.set(s[0]); }));
    });
    if (window.oldjavaSkin && window.oldjavaSkin.make) add(action(skin === 'own' ? 'Change your skin' : 'Make your own skin',
      'Pick a color, a desk, corners and windows for every page and app', '🎨', 'skin theme look style own custom yours color colour make design paint dark light wallpaper',
      function () { window.oldjavaSkin.make!(); }));
    var sound = shown('.shim-sound');
    if (sound) add(action(sound.getAttribute('aria-pressed') === 'false' ? 'Turn site sounds on' : 'Turn site sounds off',
      'The short sounds of the site\'s own buttons', '♪', 'sound mute audio quiet volume', function () { sound!.click(); }));
    return list;
  }

  // Every app's source files, read once, the first time a search is typed.
  var sources: Promise<Item[]> | null = null;
  function sourceItems(apps: OldjavaCatalogApp[]) {
    if (!sources) sources = Promise.all(apps.map(function (app) {
      return fetch(site.root() + app.id + '/source/files.txt').then(function (r) {
        return r.ok ? r.text() : '';
      }).catch(function () { return ''; }).then(function (text) {
        return text.split(/\r?\n/).filter(function (p) { return /\.java$/.test(p); }).map(function (path) {
          var file = path.split('/').pop()!;
          return {
            kind: 'source', app: app, title: file, sub: app.name + ' · ' + path, keys: path + ' ' + app.name,
            glyph: '{ }', tag: 'Source', boost: -15,
            run: function () {
              var href = site.root() + app.id + '/index.html#source:' + path;
              if (site.appId() === app.id && !site.isThen()) location.hash = '#source:' + path;
              else go(href);
            }
          };
        });
      });
    })).then(function (lists) { return ([] as Item[]).concat.apply([], lists); });
    return sources;
  }

  // ---- the box ----
  function open(opener?: Element | null) {
    if (box) { box.close(); return; }
    var returnTo = (opener || document.activeElement) as HTMLElement | null;
    var scrim = el('div', 'shim-find-scrim');
    var dialog = el('div', 'shim-find');
    dialog.setAttribute('role', 'dialog');
    dialog.setAttribute('aria-modal', 'true');
    dialog.setAttribute('aria-label', 'Find');
    var head = el('div', 'shim-find-head');
    var glass = el('span', 'shim-find-glass', '⌕');
    glass.setAttribute('aria-hidden', 'true');
    var input = el('input', 'shim-find-input');
    input.type = 'text';
    input.autocomplete = 'off';
    input.spellcheck = false;
    input.placeholder = 'Apps, things to do, source files';
    input.setAttribute('role', 'combobox');
    input.setAttribute('aria-expanded', 'true');
    input.setAttribute('aria-controls', 'shim-find-list');
    input.setAttribute('aria-autocomplete', 'list');
    var esc = el('kbd', 'shim-find-esc', 'Esc');
    esc.title = 'Close';
    head.appendChild(glass);
    head.appendChild(input);
    head.appendChild(esc);
    var list = el('div', 'shim-find-list');
    list.id = 'shim-find-list';
    list.setAttribute('role', 'listbox');
    list.setAttribute('aria-label', 'Results');
    var foot = el('div', 'shim-find-foot');
    [['↑↓', 'move'], ['Enter', 'open'], ['Shift Enter', 'full page'], [KEY, 'close']].forEach(function (k, i) {
      if (i === 2 && !onHome()) return;
      var span = el('span');
      span.appendChild(el('kbd', null, k[0]));
      span.appendChild(document.createTextNode(' ' + k[1]));
      foot.appendChild(span);
    });
    var status = el('p', 'shim-find-status');
    status.setAttribute('role', 'status');
    dialog.appendChild(head);
    dialog.appendChild(list);
    dialog.appendChild(status);
    dialog.appendChild(foot);

    var apps: OldjavaCatalogApp[] = [], items: Item[] = [], shownItems: Item[] = [], active = 0, srcItems: Item[] = [];
    // The page may move the focus while the box is open (the home page's player focuses its iframe when it loads
    // the next demo game): the box takes it back, so typing and Enter stay here.
    function keepFocus(e: FocusEvent) {
      if (!dialog.contains(e.target as Node | null)) input.focus({ preventScroll: true });
    }
    document.addEventListener('focusin', keepFocus);
    // A frame that focuses itself (the player's next demo game loading) fires no focusin here, only a blur on
    // this window: if the focus went into a frame, take it back.
    function keepFocusFromFrames() {
      setTimeout(function () {
        if (box && document.activeElement && document.activeElement.tagName === 'IFRAME') input.focus({ preventScroll: true });
      }, 0);
    }
    window.addEventListener('blur', keepFocusFromFrames);
    function close() {
      document.removeEventListener('focusin', keepFocus);
      window.removeEventListener('blur', keepFocusFromFrames);
      scrim.remove();
      dialog.remove();
      box = null;
      document.documentElement.removeAttribute('data-find');
      site.sound('close');
      if (returnTo && returnTo.isConnected && returnTo.focus) returnTo.focus({ preventScroll: true });
    }
    function choose(item: Item | undefined, full?: boolean) {
      if (!item) return;
      close();
      item.run(full);
    }

    function row(item: Item, i: number) {
      var r = el('div', 'shim-find-item kind-' + item.kind + (item.current ? ' current' : ''));
      r.id = 'shim-find-' + i;
      r.setAttribute('role', 'option');
      var pic = el('span', 'shim-find-pic');
      if (item.pic) {
        var img = el('img');
        img.src = item.pic;
        img.alt = '';
        img.loading = 'lazy';
        pic.appendChild(img);
      } else pic.textContent = item.glyph || '';
      pic.setAttribute('aria-hidden', 'true');
      var text = el('span', 'shim-find-text');
      text.appendChild(el('strong', null, item.title));
      text.appendChild(el('span', null, item.sub));
      var tag = el('span', 'shim-find-tag' + (item.tagNew ? ' new' : ''), item.current ? 'Here' : item.tag);
      r.appendChild(pic);
      r.appendChild(text);
      r.appendChild(tag);
      r.addEventListener('mousemove', function () { if (active !== i) setActive(i, false); });
      r.addEventListener('click', function (e) { choose(item, e.shiftKey); });
      return r;
    }
    function heading(text: string) {
      var h = el('div', 'shim-find-group', text);
      h.setAttribute('role', 'presentation');
      return h;
    }
    function setActive(i: number, scroll: boolean) {
      if (!shownItems.length) { input.removeAttribute('aria-activedescendant'); return; }
      active = (i + shownItems.length) % shownItems.length;
      var rows = list.querySelectorAll('.shim-find-item');
      for (var k = 0; k < rows.length; k++) rows[k].setAttribute('aria-selected', String(k === active));
      input.setAttribute('aria-activedescendant', 'shim-find-' + active);
      if (scroll && rows[active]) rows[active].scrollIntoView({ block: 'nearest' });
      // The highlighted app gets ready while the visitor reads (boot.js instant open), so Enter opens it at once.
      var it = shownItems[active];
      if (site.ready && it.app && !it.current && (it.kind === 'app' || it.kind === 'then')) {
        site.ready(it.app.id, it.kind === 'then', it.kind === 'app' && onHome());
      }
    }
    // groups: [[heading, items], ...]
    function render(groups: [string, Item[]][]) {
      list.textContent = '';
      shownItems = [];
      groups.forEach(function (g) {
        if (!g[1].length) return;
        list.appendChild(heading(g[0]));
        g[1].forEach(function (item) {
          list.appendChild(row(item, shownItems.length));
          shownItems.push(item);
        });
      });
      setActive(0, true);
      list.scrollTop = 0;
    }

    function suggestions() {
      var last = lastPlayed();
      var appRows = items.filter(function (it) { return it.kind === 'app' && !it.current; });
      var recent = appRows.filter(function (it) { return last[it.app!.id]; })
        .sort(function (a, b) { return last[b.app!.id] - last[a.app!.id]; }).slice(0, 3);
      var fresh = appRows.filter(function (it) { return !site.played(it.app!.id); }).slice(0, recent.length ? 3 : 5);
      var acts = actionItems(apps);
      render([['Jump back in', recent], ['Not played yet', fresh], [onHome() ? 'On this page' : 'This app and the site', acts]]);
      status.textContent = '';
    }

    function search() {
      var q = input.value.trim().toLowerCase();
      if (!q) { suggestions(); return; }
      var words = q.split(/\s+/);
      var scored = [items, actionItems(apps), srcItems].map(function (pool) {
        return pool.map(function (it): [number, Item] { return [rank(it, words), it]; })
          .filter(function (p) { return p[0] > 0; })
          .sort(function (a, b) { return b[0] - a[0]; });
      });
      // A close match anywhere hides the far-fetched ones (letters scattered through a name).
      var floor = Math.max.apply(null, scored.map(function (s) { return s.length ? s[0][0] : 0; })) * 0.5;
      function best(i: number, max: number) {
        return scored[i].filter(function (p) { return p[0] >= floor; }).slice(0, max).map(function (p) { return p[1]; });
      }
      var appHits = best(0, 8);
      var actHits = best(1, 6);
      var srcHits = best(2, 8);
      // The strongest kind leads: a query that names an action ("dark", "mute") shows it first.
      var groups: [string, Item[]][] = [['Apps', appHits], ['Do', actHits], ['Source files', srcHits]];
      var top = function (g: [string, Item[]]) { return g[1].length ? rank(g[1][0], words) - (g[0] === 'Apps' ? 0 : 1) : -1; };
      groups.sort(function (a, b) { return top(b) - top(a); });
      render(groups);
      var n = shownItems.length;
      status.textContent = n ? n + (n === 1 ? ' result' : ' results') : 'Nothing matches “' + input.value.trim() + '”. Try a game, a skin, or a class name.';
      if (!srcItems.length) sourceItems(apps).then(function (s) {
        srcItems = s;
        if (box && input.value.trim()) search();
      });
    }

    input.addEventListener('input', search);
    input.addEventListener('keydown', function (e) {
      // Keys typed here must not reach the app (the shim listens on the document).
      e.stopPropagation();
      if (e.key === 'ArrowDown') { e.preventDefault(); setActive(active + 1, true); }
      else if (e.key === 'ArrowUp') { e.preventDefault(); setActive(active - 1, true); }
      else if (e.key === 'PageDown') { e.preventDefault(); setActive(Math.min(active + 5, shownItems.length - 1), true); }
      else if (e.key === 'PageUp') { e.preventDefault(); setActive(Math.max(active - 5, 0), true); }
      else if (e.key === 'Enter') { e.preventDefault(); choose(shownItems[active], e.shiftKey); }
      else if (e.key === 'Escape') { e.preventDefault(); close(); }
      else if (e.key === 'Tab') e.preventDefault();   // the box keeps the focus while it is open
    });
    input.addEventListener('keyup', function (e) { e.stopPropagation(); });
    esc.addEventListener('click', close);
    scrim.addEventListener('click', close);
    dialog.addEventListener('mousedown', function (e) { if (e.target !== input) e.preventDefault(); });   // focus stays in the box

    document.body.appendChild(scrim);
    document.body.appendChild(dialog);
    document.documentElement.setAttribute('data-find', '');
    box = { close: close };
    site.sound('open');
    input.focus({ preventScroll: true });
    site.catalog().then(function (list) {
      apps = list;
      items = appItems(apps);
      search();
    });
  }

  window.oldjavaFind = { open: open, isOpen: function () { return !!box; }, key: KEY };
})();

export {};   // a module: built on its own into find.js (vite/pages.ts)
