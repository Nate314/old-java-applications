import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { embedded } from './skin';
import { played } from './passport';
import { appId, catalog, root, type CatalogApp } from './catalog';
import { playSound } from './sounds';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';
import { AppCard, PassportLine } from './ui/cards';

let drawer: HTMLElement | null;

// ---- then and now ----
// An app whose gallery card has data-then can also run as it was first restored: <App>/then.html
// (<body data-then>) runs then.js, compiled by web/then (web/then-nathanos) from master as it was after #9, the
// recovered code with the light bug fixes of "Modernize all 20 apps", before the reworks from #10 on. It reads
// its source viewer files from then-source/ and keeps its own saves (oldjava.saves.<App>.then, as web.rt.Saves
// does). The band's Then | Now switch and the title screens go between the two pages, and the title screen of
// then.html lists the quirks its README still listed at that commit.
export function isThen() { return document.body.hasAttribute('data-then'); }

// Download .jar: every app also runs on the desktop from downloads/<App>.jar (built by web/jars), with Java 17 or
// newer. Three of them press real keys on the computer, and say so beside the link.
export const JAR_JAVA = 'Java 17 or newer';

export const JAR_KEYS: Record<string, string> = {
  Windows: 'It presses the Windows key on your computer as soon as it starts.',
  WindowsE: 'It presses real Windows shortcuts on your computer when you ask it to.',
  DinoGame: 'Its screen mode (java -jar DinoGame.jar screen) presses keys on your computer to play a game in another window.'
};

// The short form of JAR_KEYS for the Source panel's download link (SourcePanel in source-viewer.tsx).
export const JAR_KEYS_LABEL: Record<string, string> = { DinoGame: 'screen mode presses real keys' };

export function sourceDir() { return isThen() ? 'then-source/' : 'source/'; }

export function programFile() { return isThen() ? 'then.js' : 'classes.js'; }

export function saveStore() { return appId() + (isThen() ? '.then' : ''); }

export function eraPage(then: boolean) { return (then ? 'then.html' : 'index.html') + (embedded ? '?embed' : ''); }

// The "## Known quirks" list of an app's README as first restored (this page's app unless `id` says otherwise):
// one entry per bullet, as [text, code, text, ...].
const quirkLists: Record<string, Promise<string[][]>> = {};

export function quirks(id?: string | null) {
  id = id || appId()!;
  if (!quirkLists[id]) {
    quirkLists[id] = fetch(root() + id + '/then-source/README.md').then(function (r) {
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r.text();
    }).then(function (text) {
      var section = /^## Known quirks[^\n]*\n([\s\S]*?)(?=^## |(?![\s\S]))/m.exec(text.replace(/\r\n?/g, '\n'));
      if (!section) return [];
      return section[1].split(/^- /m).slice(1).map(function (item) {
        return item.replace(/\*\*/g, '').replace(/\s+/g, ' ').trim().split('`');
      });
    }).catch(function (e) {
      console.warn('Could not read the known quirks: ' + e.message);
      return [];
    });
  }
  return quirkLists[id];
}

// A quirk as nodes: its `code` spans as <code>.
export function quirkNodes(parts: string[]) {
  return parts.map(function (part, i) {
    if (!(i % 2)) return document.createTextNode(part);
    var code = document.createElement('code');
    code.textContent = part;
    return code;
  });
}

// #6 fixed the quirks the READMEs listed before it, so most then.html pages have none to list.
export const NO_QUIRKS = 'None left: its README at that commit lists no known quirks.';

// Then | Now: two links, the current one marked. Hidden until the catalog says the app has a then.
export function eraSwitch(className: string) {
  var box = document.createElement('div');
  box.className = className;
  box.setAttribute('role', 'group');
  box.setAttribute('aria-label', 'Version');
  box.hidden = true;
  ([[true, 'Then', 'As first restored in 2026: the recovered code with light bug fixes, before the later reworks'],
   [false, 'Now', 'Today, fixed and reworked']] as [boolean, string, string][]).forEach(function (o) {
    var a = document.createElement('a');
    a.href = eraPage(o[0]);
    a.textContent = o[1];
    a.title = o[2];
    if (o[0] === isThen()) a.setAttribute('aria-current', 'page');
    box.appendChild(a);
  });
  catalog().then(function (apps) {
    var app = apps[indexOf(apps, appId())];
    box.hidden = !(app && app.then);
  });
  return box;
}

export function indexOf(apps: { id: string }[], id: string | null) {
  for (var i = 0; i < apps.length; i++) if (apps[i].id === id) return i;
  return -1;
}

export function pageOf(app: { id: string }) { return root() + app.id + '/index.html' + (embedded ? '?embed' : ''); }

// Apps to try after this one (three unless `count` says otherwise): apps not played yet first, then the
// played ones; within each, others of the same category first, each group shuffled.
export function picks<T extends OldjavaCatalogApp>(apps: T[], id: string | null, count?: number) {
  var self = apps[indexOf(apps, id)];
  function shuffled<T>(list: T[]) {
    for (var i = list.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var t = list[i]; list[i] = list[j]; list[j] = t;
    }
    return list;
  }
  var result: T[] = [];
  [false, true].forEach(function (wasPlayed) {
    var others = apps.filter(function (a) { return a.id !== id && !!played[a.id] === wasPlayed; });
    var same = others.filter(function (a) { return self && a.cat === self.cat; });
    var rest = others.filter(function (a) { return !self || a.cat !== self.cat; });
    result = result.concat(shuffled(same), shuffled(rest));
  });
  return result.slice(0, count || 3);
}

// "4 of 16 apps played" and a bar, for the drawer and the exit screen.
export function passportLine(apps: { id: string }[]) {
  var count = apps.filter(function (a) { return played[a.id]; }).length;
  var p = document.createElement('div');
  p.className = 'shim-passport';
  var text = document.createElement('span');
  text.textContent = 'Passport: ' + count + ' of ' + apps.length + ' apps played';
  var meter = document.createElement('span');
  meter.className = 'shim-meter';
  meter.style.setProperty('--p', String(count / apps.length));
  p.appendChild(text);
  p.appendChild(meter);
  return p;
}

export function closeButton(onClick: () => void) {
  var b = document.createElement('button');
  b.type = 'button';
  b.className = 'shim-close';
  b.setAttribute('aria-label', 'Close');
  b.textContent = '\u00d7';
  b.addEventListener('click', onClick);
  return b;
}

// A link to an app's page. In the home page's player it asks the page to swap the app in instead,
// so the player's title, rail and address stay in step.
// Played apps get a check on their thumbnail; with showNew, the others get a New badge.
export function appCard(app: OldjavaCatalogApp, current: boolean, showNew?: boolean) {
  var a = document.createElement('a');
  a.className = 'shim-app' + (current ? ' current' : '');
  if (current) a.setAttribute('aria-current', 'page');
  var pic = document.createElement('span');
  pic.className = 'shim-app-pic';
  var img = document.createElement('img');
  img.src = app.shot;
  img.alt = '';
  pic.appendChild(img);
  if (played[app.id] || showNew) {
    var badge = document.createElement('span');
    badge.className = 'shim-badge' + (played[app.id] ? ' played' : ' new');
    badge.textContent = played[app.id] ? '✓' : 'New';
    badge.title = played[app.id] ? 'Played' : 'Not played yet';
    pic.appendChild(badge);
  }
  var text = document.createElement('span');
  text.className = 'shim-app-text';
  var name = document.createElement('strong');
  name.textContent = app.name;
  var hint = document.createElement('span');
  hint.textContent = current ? 'Now playing' : app.hint;
  text.appendChild(name);
  text.appendChild(hint);
  a.appendChild(pic);
  a.appendChild(text);
  playLink(a, app);
  return a;
}

// Makes a link open the app: in the home page's player, by asking the page to swap it in.
export function playLink(a: HTMLAnchorElement, app: { id: string }) {
  a.href = pageOf(app);
  if (embedded && window.parent !== window) {
    a.setAttribute('data-sound', 'none');   // the home page plays the new app's start
    a.addEventListener('click', function (e) {
      if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      window.parent.postMessage({ oldjavaPlay: app.id }, location.origin);
    });
  }
}

// Keys typed in the page chrome must not reach the app (the shim listens on the document).
export function ownKeys(el: HTMLElement, onEscape?: () => void) {
  el.addEventListener('keydown', function (e) {
    e.stopPropagation();
    if (e.key === 'Escape' && onEscape) onEscape();
  });
  el.addEventListener('keyup', function (e) { e.stopPropagation(); });
}

export function openDrawer(opener: HTMLElement) {
  if (drawer) return;
  function shut() {
    playSound('close');
    close();
    drawer = null;
    opener.focus();
  }
  const close = openLayer(<SwitchDrawer onClose={shut} onPanel={el => { drawer = el; }} />);
  playSound('open');
}

/** Switch app: every app in gallery order, in a panel on the right, under the passport line. */
function SwitchDrawer({ onClose, onPanel }: { onClose: () => void; onPanel: (el: HTMLElement) => void }) {
  const panel = useRef<HTMLElement>(null);
  const list = useRef<HTMLDivElement>(null);
  const [apps, setApps] = useState<CatalogApp[] | null>(null);
  useOwnKeys(panel, onClose);
  useLayoutEffect(() => { onPanel(panel.current!); }, [onPanel]);
  useEffect(() => { catalog().then(setApps); }, []);
  useLayoutEffect(() => {
    if (!apps) return;
    const here = list.current!.querySelector('.current');
    if (here) here.scrollIntoView({ block: 'nearest' });
    (list.current!.querySelector<HTMLElement>('.shim-app:not(.current)') || panel.current!.querySelector<HTMLElement>('.shim-close')!).focus();
  }, [apps]);
  const id = appId();
  return (
    <>
      <div className="shim-scrim" onClick={onClose} />
      <aside className="shim-drawer" aria-label="Switch app" ref={panel}>
        <div className="shim-drawer-head">
          <strong>Switch app</strong>
          <CloseButton onClick={onClose} />
        </div>
        {apps && apps.length > 0 && <PassportLine apps={apps} />}
        <div className="shim-drawer-list" ref={list}>
          {apps && apps.map(app => <AppCard key={app.id} app={app} current={app.id === id} />)}
        </div>
      </aside>
    </>
  );
}

export function init() {
  // The home page's player shows the quirks of the app it plays as first restored.
  window.oldjavaThen = { quirks: quirks, nodes: quirkNodes };
  // Switch app: every app in gallery order, in a panel on the right.
  drawer = null;
}
