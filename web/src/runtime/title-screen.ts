import { played } from './passport';
import { WATCH } from './watch';
import { saverMinutes, saverOn, setSaverOn } from './saver';
import { appId, catalog, type CatalogApp } from './catalog';
import { JAR_JAVA, JAR_KEYS, NO_QUIRKS, eraPage, indexOf, isThen, jarLink, programFile, quirkNodes, quirks, saveStore } from './then-and-now';
import { INVITE, SHARED } from './share';
import { sourcePanel } from './source-viewer';
import { AGAIN_KEY, CATS, newsScript, savedRecord, size } from './records';

export function titleScreen(files: string[]) {
  var html = document.documentElement;
  html.setAttribute('data-title', '');
  var screen = document.createElement('div');
  screen.className = 'shim-title';
  screen.setAttribute('role', 'dialog');
  screen.setAttribute('data-sound', 'none');   // starting plays the start sound
  var card = document.createElement('div');
  card.className = 'shim-title-card';
  var art = document.createElement('div');
  art.className = 'shim-title-art';
  var img = document.createElement('img');
  img.alt = '';
  img.hidden = true;
  img.addEventListener('load', function () { img.hidden = false; });
  art.appendChild(img);
  var text = document.createElement('div');
  text.className = 'shim-title-text';
  var cat = document.createElement('span');
  cat.className = 'shim-title-cat';
  var name = document.createElement('h1');
  name.textContent = document.title;
  screen.setAttribute('aria-label', document.title);
  var hint = document.createElement('p');
  hint.className = 'shim-title-hint';
  var stamp = document.createElement('p');
  stamp.className = 'shim-title-stamp';
  var record = document.createElement('p');
  record.className = 'shim-title-record';
  var recordText = savedRecord(saveStore());
  record.hidden = !recordText;
  record.appendChild(document.createTextNode(recordText + ' '));
  // Forget clears the app's saves (a second click confirms), so it starts again from its bundled files.
  var forget = document.createElement('button');
  forget.type = 'button';
  forget.className = 'shim-title-forget';
  forget.textContent = 'Forget';
  forget.addEventListener('click', function () {
    if (!forget.classList.contains('confirm')) {
      forget.classList.add('confirm');
      forget.textContent = 'Really forget?';
      return;
    }
    try { localStorage.removeItem('oldjava.saves.' + saveStore()); } catch (e) { /* nothing was kept */ }
    record.hidden = true;
  });
  record.appendChild(forget);
  var play = document.createElement('button');
  play.type = 'button';
  play.className = 'shim-title-play';
  play.disabled = true;
  play.textContent = 'Loading…';
  var press = document.createElement('span');
  press.className = 'shim-title-press';
  press.textContent = 'or press any key';
  var meter = document.createElement('span');
  meter.className = 'shim-meter shim-title-meter';
  var log = document.createElement('pre');
  log.className = 'shim-title-log';
  log.setAttribute('aria-hidden', 'true');
  var read = document.createElement('a');
  read.className = 'shim-title-source';
  read.href = '#source';
  read.setAttribute('data-sound', 'none');
  read.textContent = isThen() ? 'Read the source as first restored' : 'Read the source';
  // Then and now: the other version of the app, once the catalog says it has one.
  var era = document.createElement('a');
  era.className = 'shim-title-era';
  era.href = eraPage(!isThen());
  era.textContent = isThen() ? 'Play today’s version' : 'Play it as first restored, before the reworks';
  era.hidden = true;
  // The games that play themselves: a look before playing (see watch).
  var watchLink = document.createElement('a');
  watchLink.className = 'shim-title-watch';
  watchLink.href = '?watch';
  watchLink.textContent = 'Watch the computer play it first';
  watchLink.hidden = !WATCH[appId()!] || isThen();
  // The app for the desktop, today's version only (the jars are built from today's sources).
  var jarRow = document.createElement('p');
  jarRow.className = 'shim-title-jar';
  jarRow.hidden = isThen();
  jarRow.appendChild(jarLink(appId()!, 'shim-title-jar-link', 'Download ' + appId() + '.jar'));
  jarRow.appendChild(document.createTextNode(' for the desktop, ' + JAR_JAVA + '.'));
  if (JAR_KEYS[appId()!]) {
    var keys = document.createElement('strong');
    keys.className = 'shim-title-jar-keys';
    keys.textContent = ' ' + JAR_KEYS[appId()!];
    jarRow.appendChild(keys);
  }
  // Screen Saver is also the site's screen saver (see showSaver): its title screen turns that on and off.
  var saverRow = document.createElement('p');
  saverRow.className = 'shim-title-saver';
  saverRow.hidden = appId() !== 'ScreenSaver' || isThen();
  var saverText = document.createTextNode('');
  var saverToggle = document.createElement('button');
  saverToggle.type = 'button';
  saverToggle.className = 'shim-title-forget';   // kept from starting the app, like Forget
  function showSaverRow() {
    var on = saverOn();
    saverText.data = on ? 'Also this site’s screen saver, after ' + saverMinutes() + ' minutes away. '
      : 'This site’s screen saver is off. ';
    saverToggle.textContent = on ? 'Turn off' : 'Turn on';
  }
  saverToggle.addEventListener('click', function () { setSaverOn(!saverOn()); showSaverRow(); });
  showSaverRow();
  saverRow.appendChild(saverText);
  saverRow.appendChild(saverToggle);
  // What's new: the app's story, every change to it from the repo's history (news.js, loaded when needed).
  var story = document.createElement('details');
  story.className = 'shim-title-story';
  story.hidden = true;
  if (!isThen()) newsScript().then(function (news) { news.story(appId()!, story); });
  [cat, name, hint, stamp, record, saverRow, play, press, read, watchLink, era, jarRow, story].forEach(function (el) { text.appendChild(el); });
  card.appendChild(art);
  card.appendChild(text);
  card.appendChild(meter);
  card.appendChild(log);
  screen.appendChild(card);
  document.body.appendChild(screen);

  function line(label: string, value: string, cls?: string) {
    var row = document.createElement('span');
    row.className = 'row' + (cls ? ' ' + cls : '');
    var l = document.createElement('span');
    l.textContent = label;
    var v = document.createElement('span');
    v.textContent = value;
    row.appendChild(l);
    row.appendChild(v);
    log.appendChild(row);
    log.scrollTop = log.scrollHeight;
  }
  // classes.js (then.js) has loaded by now (a blocking script in <head>): the app, compiled by TeaVM.
  var entry = window.performance && performance.getEntriesByName &&
    (performance.getEntriesByName(new URL(programFile(), location.href).href)[0] as PerformanceResourceTiming | undefined);
  line(programFile(), entry && entry.decodedBodySize ? size(entry.decodedBodySize) : 'ok');
  var total = files.length + 1;
  var done = 1;
  function progress() { meter.style.setProperty('--p', String(done / total)); }
  progress();

  catalog().then(function (apps) {
    var app = apps[indexOf(apps, appId())];
    if (!app) return;
    era.hidden = !app.then;
    name.textContent = app.name;
    if (isThen()) thenTitle(app); else {
      img.src = app.shot;
      cat.textContent = CATS[app.cat] || '';
      hint.textContent = app.hint;
    }
    var count = apps.filter(function (a) { return played[a.id]; }).length;
    stamp.textContent = played[app.id]
      ? '✓ In your passport since ' + new Date(played[app.id]).toLocaleDateString()
      : 'New: playing it adds stamp ' + (count + 1) + ' of ' + apps.length + ' to your passport';
    stamp.classList.toggle('played', !!played[app.id]);
    if (INVITE) {
      var invite = document.createElement('p');
      invite.className = 'shim-title-shared';
      invite.textContent = INVITE;
      text.insertBefore(invite, cat);
    }
    if (SHARED) {
      var theirs = Math.min(+SHARED[1], apps.length);
      var gift = document.createElement('p');
      gift.className = 'shim-title-shared';
      gift.textContent = 'Shared with you by a player with ' + theirs + ' of ' + apps.length + ' stamps. ' +
        (count ? 'You have ' + count + '.' : 'Play to start your own passport.');
      text.insertBefore(gift, cat);
    }
  });

  // then.html: the quirks its README still listed when it was first restored, where today's page shows its picture.
  function thenTitle(app: CatalogApp) {
    screen.classList.add('then');
    cat.textContent = 'As first restored';
    hint.textContent = (app.origin === 'original'
      ? 'The author’s own source as it was found'
      : 'The code as it came out of the old jar') +
      ', with the light bug fixes of its 2026 restoration, before the later reworks.';
    var head = document.createElement('h2');
    head.textContent = 'Known quirks, from its README then';
    var list = document.createElement('ul');
    art.appendChild(head);
    art.appendChild(list);
    quirks().then(function (items: string[][]) {
      if (!items.length) items = [[NO_QUIRKS]];
      items.forEach(function (parts: string[]) {
        var li = document.createElement('li');
        quirkNodes(parts).forEach(function (n: Node) { li.appendChild(n); });
        list.appendChild(li);
      });
    });
  }

  var started: (() => void) | null = null, skipping = false;
  function go(e?: Event) {
    if (!started || screen.classList.contains('leaving')) return;
    if (e && e.type === 'keydown') {
      if ((e as KeyboardEvent).key === 'Tab' || (e as KeyboardEvent).key === 'Shift' || (e as KeyboardEvent).key === 'Control' || (e as KeyboardEvent).key === 'Alt' || (e as KeyboardEvent).key === 'Meta') return;
      if ((e.target as Element).closest && (e.target as Element).closest('.shim-toolbar, .shim-drawer, .shim-share, .shim-source, .shim-title-forget, .shim-title-story, .shim-find')) return;   // keys for those panels
      e.preventDefault();
      e.stopPropagation();
      // Its release must not reach the app either: it never saw the press.
      var code = (e as KeyboardEvent).code;
      document.addEventListener('keyup', function swallow(u) {
        if (u.code !== code) return;
        u.stopPropagation();
        document.removeEventListener('keyup', swallow, true);
      }, true);
    }
    document.removeEventListener('keydown', go, true);
    screen.classList.add('leaving');
    html.removeAttribute('data-title');
    var start = started;
    started = null;
    start();
    setTimeout(function () { screen.remove(); }, 400);
  }
  screen.addEventListener('click', function (e) { if (!(e.target as Element).closest('a, .shim-title-forget, .shim-title-story')) go(e); });

  return {
    loaded: function (file: string) {
      var bytes = window.__shimFiles && (window.__shimFiles[file] as Uint8Array | undefined);
      done++;
      progress();
      line(file, bytes ? size(bytes.byteLength) : 'missing', bytes ? '' : 'bad');
    },
    ready: function (start: () => void) {
      started = start;
      line('Ready.', '', 'ready');
      screen.classList.add('ready');
      play.disabled = false;
      play.textContent = '▶ Play';
      if (!sourcePanel) play.focus({ preventScroll: true });   // keep the keyboard in an open source panel
      document.addEventListener('keydown', go, true);
      if (skipping) go();
    },
    // Straight on, as if Play were pressed (now, or once the files are in).
    skip: function () { if (started) go(); else skipping = true; }
  };
}

export function overlay(text: string, button?: string) {
  var o = document.createElement('div');
  o.className = 'shim-overlay';
  var box = document.createElement('div');
  box.className = 'shim-overlay-box';
  var p = document.createElement('p');
  p.textContent = text;
  box.appendChild(p);
  if (button) {
    var b = document.createElement('button');
    b.textContent = button;
    b.addEventListener('click', function () {
      // The visitor already pressed start once: the reloaded page skips the title screen.
      try { sessionStorage.setItem(AGAIN_KEY, appId()!); } catch (e) { /* the title screen shows again */ }
      location.reload();
    });
    box.appendChild(b);
  }
  o.appendChild(box);
  document.body.appendChild(o);
  return o;
}

const IMAGE = /\.(png|jpe?g|gif|bmp|webp)$/i;

export function preload(name: string) {
  window.__shimFiles = window.__shimFiles || {};
  window.__shimImages = window.__shimImages || {};
  var bytes = fetch(name).then(function (r) {
    if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
    return r.arrayBuffer();
  }).then(function (buf) {
    window.__shimFiles[name] = new Uint8Array(buf);
  });
  var image = !IMAGE.test(name) ? Promise.resolve() : new Promise<void>(function (resolve, reject) {
    var img = new Image();
    img.onload = function () { window.__shimImages[name] = img; resolve(); };
    img.onerror = function () { reject(new Error('cannot decode')); };
    img.src = name;
  });
  return Promise.all([bytes, image]).catch(function (e) {
    console.warn('Could not preload ' + name + ': ' + e.message);
  });
}
