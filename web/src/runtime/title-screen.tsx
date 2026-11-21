import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties, type MouseEvent, type SyntheticEvent } from 'react';
import { flushSync } from 'react-dom';
import { played } from './passport';
import { WATCH, watchWanted } from './watch';
import { saverMinutes, saverOn, setSaverOn } from './saver';
import { appId, catalog, root, type CatalogApp } from './catalog';
import { embedded } from './skin';
import { JAR_JAVA, JAR_KEYS, NO_QUIRKS, eraPage, indexOf, isThen, programFile, quirks, saveStore } from './then-and-now';
import { INVITE, SHARED } from './share';
import { sourcePanel } from './source-viewer';
import { AGAIN_KEY, CATS, newsScript, savedRecord, size } from './records';
import { openLayer } from './ui/layer';
import { JarLink, Quirk } from './ui/cards';
import { Attract } from './title-attract';

/** A line of the boot log: [label, value, class]. */
export type LogLine = [string, string, string];

/** Adds a line to the title screen's boot log, while there is one. */
export let titleLog: ((row: LogLine) => void) | null = null;

/** What the page's start-up (start.ts) tells the title screen; set once it is in the page. */
interface TitleControl { line(row: LogLine): void; done(p: number): void; ready(): void; leave(): void }

interface Opened { ui: TitleControl; screen: HTMLElement; close: () => void }
var early: Opened | null = null, waiting = false;
var onGo: ((e?: Event) => void) | null = null;
var auto = false;   // it starts by itself once ready (chosen, below), until the visitor stays to read it

/** Puts the title screen in the page; apps, when given, is the catalog it shows from its first frame. */
function open(apps?: CatalogApp[]): Opened {
  document.documentElement.setAttribute('data-title', '');
  auto = chosen();
  var ui: TitleControl | null = null, screen: HTMLElement | null = null;
  const close = openLayer(<TitleScreen apps={apps} onGo={e => { if (onGo) onGo(e); }} onMount={(c, el) => { ui = c; screen = el; }} />);
  titleLog = ui!.line;
  return { ui: ui!, screen: screen!, close };
}

// The title screen opens as soon as the runtime has run and apps.json has answered, not at DOMContentLoaded:
// that waits for the app's program (classes.js, the page's biggest file) to arrive and run, and a slow phone
// showed nothing for seconds. The page's head has already asked for the picture (src/pages/app/AppPage.tsx).
// start.ts then takes it over in titleScreen, or, where the page has no title screen after all, it goes again.
export function init() {
  var again = null;
  try { again = sessionStorage.getItem(AGAIN_KEY); } catch (e) { /* no Run again shortcut */ }
  if (!appId() || embedded || watchWanted() || again === appId()) return;   // start.ts's own reasons to skip it
  waiting = true;
  catalog().then(function (apps) { if (waiting) early = open(apps); });
  // After start.ts's listener (its init runs first): by now it has taken the screen over, if the page has one.
  document.addEventListener('DOMContentLoaded', function () {
    waiting = false;
    if (!early) return;
    early.close();
    early = null;
    titleLog = null;
    document.documentElement.removeAttribute('data-title');
  });
}

// An app page opens on a title screen while its files load: the app's thumbnail, name, category, hint and
// passport stamp, a boot log of the files as they arrive (real names and sizes) and a Play button. The app
// starts on the visitor's click or key press, so its first sounds (MusicTheoryExpert's demo, the site's
// start sound) are allowed to play. Run again skips it (see overlay). Returns { loaded(file), ready(start), skip() }.
// Arrived by a link on this site (the story's Play bar, Next, Switch app, Find, a gallery's Full page): the visitor has
// already chosen this app, so it starts the moment its files are in instead of asking for Play a second time. A
// shared link, a search, a reload or Back keep the screen waiting for Play.
function chosen() {
  try {
    var nav = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
    var from = new URL(document.referrer);
    return (!nav || nav.type === 'navigate') && from.origin === location.origin && from.pathname !== location.pathname;
  } catch (e) { return false; }   // no referrer
}

export function titleScreen(files: string[]) {
  var html = document.documentElement;
  const { ui, screen, close } = early || open();
  early = null;
  waiting = false;
  onGo = go;
  var total = files.length + 1;

  // classes.js (then.js) has loaded by now (a deferred script, run before the page's DOMContentLoaded): the app,
  // compiled by TeaVM.
  var program = document.querySelector<HTMLScriptElement>('script[src^="' + programFile() + '"]');   // with its ?v= stamp
  var entry = window.performance && performance.getEntriesByName &&
    (performance.getEntriesByName(program ? program.src : new URL(programFile(), location.href).href)[0] as PerformanceResourceTiming | undefined);
  ui.line([programFile(), entry && entry.decodedBodySize ? size(entry.decodedBodySize) : 'ok', '']);
  var done = 1;
  ui.done(done / total);

  var started: (() => void) | null = null, skipping = false, leaving = false;
  function go(e?: Event) {
    if (!started || leaving) return;
    if (e && e.type === 'keydown') {
      if ((e as KeyboardEvent).key === 'Tab' || (e as KeyboardEvent).key === 'Shift' || (e as KeyboardEvent).key === 'Control' || (e as KeyboardEvent).key === 'Alt' || (e as KeyboardEvent).key === 'Meta') return;
      if ((e.target as Element).closest && (e.target as Element).closest('.shim-toolbar, .shim-drawer, .shim-share, .shim-source, .shim-title-forget, .shim-title-story, .shim-title-clip-pause, .shim-find')) return;   // keys for those panels
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
    leaving = true;
    ui.leave();
    html.removeAttribute('data-title');
    var start = started;
    started = null;
    // Let go of Play, so the app's first button or field can take the keyboard (the shim's TDesktop.show).
    if (screen.contains(document.activeElement)) (document.activeElement as HTMLElement).blur();
    start();
    setTimeout(function () { close(); titleLog = null; }, 400);
  }

  return {
    loaded: function (file: string) {
      var bytes = window.__shimFiles && (window.__shimFiles[file] as Uint8Array | undefined);
      ui.done(++done / total);
      ui.line([file, bytes ? size(bytes.byteLength) : 'missing', bytes ? '' : 'bad']);
    },
    ready: function (start: () => void) {
      started = start;
      ui.line(['Ready.', '', 'ready']);
      ui.ready();
      if (!sourcePanel) screen.querySelector<HTMLElement>('.shim-title-play')!.focus({ preventScroll: true });   // keep the keyboard in an open source panel
      document.addEventListener('keydown', go, true);
      if (skipping) go();
      // Not while prerendered (instant open): the app waits until the visitor actually arrives.
      else if (auto && !sourcePanel) {
        if (document.prerendering) document.addEventListener('prerenderingchange', function () { if (auto) go(); }, { once: true });
        else go();
      }
    },
    // Straight on, as if Play were pressed (now, or once the files are in).
    skip: function () { if (started) go(); else skipping = true; }
  };
}

/** The title screen itself; titleScreen above runs it. */
function TitleScreen({ apps, onGo, onMount }: {
  apps?: CatalogApp[]; onGo: (e: Event) => void; onMount: (control: TitleControl, el: HTMLElement) => void;
}) {
  const id = appId()!;
  const then = isThen();
  const screen = useRef<HTMLDivElement>(null);
  const [Story, setStory] = useState<NonNullable<Window['oldjavaNews']>['Story'] | null>(null);
  const [lines, setLines] = useState<LogLine[]>([]);
  const [done, setDone] = useState(0);   // the share of the files in
  const [ready, setReady] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [starting, setStarting] = useState(auto);
  const first = apps && apps[indexOf(apps, id)];
  const [found, setFound] = useState<{ app: CatalogApp; apps: CatalogApp[] } | null>(first ? { app: first, apps: apps! } : null);
  const [listed, setListed] = useState(!!apps);   // apps.json has answered: the card shows, filled in, not half empty
  const [quirkList, setQuirkList] = useState<string[][] | null>(null);
  const [shown, setShown] = useState(false);   // the thumbnail has loaded (the clip waits for it)
  const [record] = useState(() => savedRecord(saveStore()));
  const [forgotten, setForgotten] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const [saver, setSaver] = useState(saverOn);

  useLayoutEffect(() => {
    onMount({
      // Each is drawn at once, as the old DOM version was, so the log and the Play button are in place for the caller.
      line: row => flushSync(() => setLines(l => l.concat([row]))),
      done: p => flushSync(() => setDone(p)),
      ready: () => flushSync(() => setReady(true)),
      leave: () => flushSync(() => setLeaving(true)),
    }, screen.current!);
  }, []);
  useEffect(() => {
    catalog().then(function (apps) {
      setListed(true);
      var app = apps[indexOf(apps, id)];
      if (!app) return;
      setFound({ app, apps });
      if (then) quirks().then(items => setQuirkList(items.length ? items : [[NO_QUIRKS]]));
    });
    // What's new: the app's story, every change to it from the repo's history (news.js draws it).
    if (!then) newsScript().then(function (news) { if (news.Story) setStory(() => news.Story); });
  }, []);

  const app = found && found.app;
  const count = found ? found.apps.filter(a => played[a.id]).length : 0;
  const was = app && played[app.id];
  function forget() {
    // Forget clears the app's saves (a second click confirms), so it starts again from its bundled files.
    if (!confirm) { setConfirm(true); return; }
    try { localStorage.removeItem('oldjava.saves.' + saveStore()); } catch (e) { /* nothing was kept */ }
    setForgotten(true);
  }
  // Scrolling, a wheel, or a tap or focus on anything but Play: the visitor is reading it, so it waits for Play after all.
  function stay(e: SyntheticEvent) {
    if (!auto || (e.target as Element).closest('.shim-title-play')) return;
    auto = false;
    setStarting(false);
  }
  function onClick(e: MouseEvent) {
    if (!(e.target as Element).closest('a, .shim-title-forget, .shim-title-story')) onGo(e.nativeEvent);
  }
  return (
    <div className={'shim-title' + (app && then ? ' then' : '') + (ready ? ' ready' : '') + (listed ? '' : ' unlisted') + (leaving ? ' leaving' : '')}
      role="dialog" data-sound="none" aria-label={document.title} ref={screen} onClick={onClick}
      onScroll={stay} onWheel={stay} onPointerDown={stay} onFocus={stay}>
      <div className="shim-title-card">
        <div className="shim-title-art">
          {/* Shown as it arrives: today's picture has its room from the start on a phone (shim.css), so nothing moves. */}
          <img alt="" fetchPriority="high" src={app && !then ? app.shot.replace(/[.]png$/, '.webp') : undefined} onLoad={() => setShown(true)} />
          {app && app.clip && !then && <Attract id={id} go={ready && shown} />}
          {app && then && (
            // then.html: the quirks its README still listed when it was first restored, where today's page shows its picture.
            <>
              <h2>Known quirks, from its README then</h2>
              <ul>{quirkList && quirkList.map((parts, i) => <li key={i}><Quirk parts={parts} /></li>)}</ul>
            </>
          )}
        </div>
        <div className="shim-title-text">
          {app && INVITE && <p className="shim-title-shared">{INVITE}</p>}
          {app && SHARED && (
            <p className="shim-title-shared">
              {'Shared with you by a player with ' + Math.min(+SHARED[1], found!.apps.length) + ' of ' + found!.apps.length + ' stamps. ' +
                (count ? 'You have ' + count + '.' : 'Play to start your own passport.')}
            </p>
          )}
          <span className="shim-title-cat">{app ? (then ? 'As first restored' : CATS[app.cat] || '') : ''}</span>
          <h1>{app ? app.name : document.title}</h1>
          {/* Play right under the name, ahead of the hint and the record lines, so a phone shows it without scrolling. */}
          <button type="button" className="shim-title-play" disabled={!ready}>{ready ? '▶ Play' : starting ? 'Starting…' : 'Loading…'}</button>
          <span className="shim-title-press">or press any key</span>
          <p className="shim-title-hint">
            {app ? (then
              ? (app.origin === 'original' ? 'The author’s own source as it was found' : 'The code as it came out of the old jar') +
                ', with the light bug fixes of its 2026 restoration, before the later reworks.'
              : app.hint) : ''}
          </p>
          <p className={'shim-title-stamp' + (was ? ' played' : '')}>
            {app ? (was
              ? '✓ In your passport since ' + new Date(played[app.id]).toLocaleDateString()
              : 'New: playing it adds stamp ' + (count + 1) + ' of ' + found!.apps.length + ' to your passport') : ''}
          </p>
          <p className="shim-title-record" hidden={!record || forgotten}>
            {record + ' '}
            <button type="button" className={'shim-title-forget' + (confirm ? ' confirm' : '')} onClick={forget}>{confirm ? 'Really forget?' : 'Forget'}</button>
          </p>
          {/* Screen Saver is also the site's screen saver (see showSaver): its title screen turns that on and off. */}
          <p className="shim-title-saver" hidden={id !== 'ScreenSaver' || then}>
            {saver ? 'Also this site’s screen saver, after ' + saverMinutes() + ' minutes away. ' : 'This site’s screen saver is off. '}
            <button type="button" className="shim-title-forget" onClick={() => { setSaverOn(!saverOn()); setSaver(saverOn()); }}>{saver ? 'Turn off' : 'Turn on'}</button>
          </p>
          <a className="shim-title-source" href="#source" data-sound="none">{then ? 'Read the source as first restored' : 'Read the source'}</a>
          {/* The games that play themselves: a look before playing (see watch). */}
          <a className="shim-title-watch" href="?watch" hidden={!WATCH[id] || then}>Watch the computer play it first</a>
          {/* Then and now: the other version of the app, once the catalog says it has one. */}
          <a className="shim-title-era" href={eraPage(!then)} hidden={!(app && app.then)}>
            {then ? 'Play today’s version' : 'Play it as first restored, before the reworks'}
          </a>
          {/* Both versions at once, with what changed in between (src/pages/compare); not inside another page's frame. */}
          <a className="shim-title-era" href={root() + 'compare.html?app=' + id} hidden={!(app && app.then) || embedded}>
            Compare then and now side by side
          </a>
          {/* The app for the desktop, today's version only (the jars are built from today's sources). */}
          <p className="shim-title-jar" hidden={then}>
            <JarLink id={id} className="shim-title-jar-link">{'Download ' + id + '.jar'}</JarLink>
            {' for the desktop, ' + JAR_JAVA + '.'}
            {JAR_KEYS[id] && <strong className="shim-title-jar-keys">{' ' + JAR_KEYS[id]}</strong>}
          </p>
          {/* Until news.js answers, its line's room is kept (today's page only), so the meter and log below stay put. */}
          {Story ? <Story id={id} /> : <details className="shim-title-story" hidden={then} aria-hidden="true" />}
        </div>
        <span className="shim-meter shim-title-meter" style={{ '--p': String(done) } as CSSProperties} />
        {/* tabIndex -1: Chrome puts a scroller in the tab order, and this one is hidden from screen readers. Newest first in
            the page, shown last (column-reverse, shim.css): the box stays scrolled to the newest line without reading its layout. */}
        <pre className="shim-title-log" aria-hidden="true" tabIndex={-1}>
          {lines.map(([label, value, cls], i) => (
            <span key={i} className={'row' + (cls ? ' ' + cls : '')}><span>{label}</span><span>{value}</span></span>
          )).reverse()}
        </pre>
      </div>
    </div>
  );
}

