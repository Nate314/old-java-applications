import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { embedded } from './skin';
import { played } from './passport';
import { appId, catalog, root, type CatalogApp } from './catalog';
import { CloseButton, useOwnKeys } from './ui/layer';
import { openPanel } from './ui/panel';
import { AppCard, PassportLine } from './ui/cards';
import { fetchOk } from './fetch-ok';
import { quirkItems } from './title-card';

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
    quirkLists[id] = fetchOk(root() + id + '/then-source/README.md').then(function (r) { return r.text(); }).then(quirkItems).catch(function (e) {
      console.warn('Could not read the known quirks: ' + e.message);
      return [];
    });
  }
  return quirkLists[id];
}

export { NO_QUIRKS } from './title-card';

// Then | Now: two links, the current one marked. Hidden until the catalog says the app has a then.
export function EraSwitch({ className }: { className: string }) {
  const [then, setThen] = useState(false);
  useEffect(() => {
    catalog().then(apps => {
      const app = apps[indexOf(apps, appId())];
      setThen(!!(app && app.then));
    });
  }, []);
  return (
    <div className={className} role="group" aria-label="Version" hidden={!then}>
      {([[true, 'Then', 'As first restored in 2026: the recovered code with light bug fixes, before the later reworks'],
        [false, 'Now', 'Today, fixed and reworked']] as [boolean, string, string][]).map(o => (
        <a key={o[1]} href={eraPage(o[0])} title={o[2]} aria-current={o[0] === isThen() ? 'page' : undefined}>{o[1]}</a>
      ))}
    </div>
  );
}

export function indexOf(apps: { id: string }[], id: string | null) {
  for (var i = 0; i < apps.length; i++) if (apps[i].id === id) return i;
  return -1;
}

export function pageOf(app: { id: string }) { return root() + app.id + '/index.html' + (embedded ? '?embed' : ''); }

// Apps to try after this one (three unless `count` says otherwise): apps not played yet first, then the
// played ones; within each, others of the same category first, in gallery order. Every "what next" on the site
// (the stamp toast, the exit screen, the band's Next, the home page's Welcome back and passport) asks this, so a
// visitor is offered the same next app wherever they look.
export function picks<T extends { id: string; cat: string }>(apps: T[], id: string | null, count?: number) {
  var self = apps[indexOf(apps, id)];
  var result: T[] = [];
  [false, true].forEach(function (wasPlayed) {
    var others = apps.filter(function (a) { return a.id !== id && !!played[a.id] === wasPlayed; });
    result = result.concat(others.filter(function (a) { return self && a.cat === self.cat; }),
      others.filter(function (a) { return !self || a.cat !== self.cat; }));
  });
  return result.slice(0, count || 3);
}

export function openDrawer(opener: HTMLElement) {
  if (drawer) return;
  openPanel(shut => <SwitchDrawer onClose={shut} onPanel={el => { drawer = el; }} />, opener, () => { drawer = null; });
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
      <aside className="shim-drawer" role="dialog" aria-modal="true" aria-label="Switch app" ref={panel}>
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
  window.oldjavaThen = { quirks: quirks };
  // Switch app: every app in gallery order, in a panel on the right.
  drawer = null;
}
