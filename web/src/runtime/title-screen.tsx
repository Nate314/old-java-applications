import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties, type MouseEvent } from 'react';
import { flushSync } from 'react-dom';
import { played } from './passport';
import { WATCH } from './watch';
import { saverMinutes, saverOn, setSaverOn } from './saver';
import { appId, catalog, type CatalogApp } from './catalog';
import { JAR_JAVA, JAR_KEYS, NO_QUIRKS, eraPage, indexOf, isThen, programFile, quirks, saveStore } from './then-and-now';
import { INVITE, SHARED } from './share';
import { sourcePanel } from './source-viewer';
import { CATS, newsScript, savedRecord, size } from './records';
import { openLayer } from './ui/layer';
import { JarLink, Quirk } from './ui/cards';

/** A line of the boot log: [label, value, class]. */
type LogLine = [string, string, string];

/** What the page's start-up (start.ts) tells the title screen; set once it is in the page. */
interface TitleControl { line(row: LogLine): void; done(n: number): void; ready(): void; leave(): void }

// An app page opens on a title screen while its files load: the app's thumbnail, name, category, hint and
// passport stamp, a boot log of the files as they arrive (real names and sizes) and a Play button. The app
// starts on the visitor's click or key press, so its first sounds (MusicTheoryExpert's demo, the site's
// start sound) are allowed to play. Run again skips it (see overlay). Returns { loaded(file), ready(start), skip() }.
export function titleScreen(files: string[]) {
  var html = document.documentElement;
  html.setAttribute('data-title', '');
  var control: TitleControl | null = null;
  var screen: HTMLElement | null = null;
  const close = openLayer(<TitleScreen total={files.length + 1} onGo={go} onMount={(c, el) => { control = c; screen = el; }} />);
  const ui = control as unknown as TitleControl;

  // classes.js (then.js) has loaded by now (a deferred script, run before the page's DOMContentLoaded): the app,
  // compiled by TeaVM.
  var entry = window.performance && performance.getEntriesByName &&
    (performance.getEntriesByName(new URL(programFile(), location.href).href)[0] as PerformanceResourceTiming | undefined);
  ui.line([programFile(), entry && entry.decodedBodySize ? size(entry.decodedBodySize) : 'ok', '']);
  var done = 1;

  var started: (() => void) | null = null, skipping = false, leaving = false;
  function go(e?: Event) {
    if (!started || leaving) return;
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
    leaving = true;
    ui.leave();
    html.removeAttribute('data-title');
    var start = started;
    started = null;
    start();
    setTimeout(close, 400);
  }

  return {
    loaded: function (file: string) {
      var bytes = window.__shimFiles && (window.__shimFiles[file] as Uint8Array | undefined);
      ui.done(++done);
      ui.line([file, bytes ? size(bytes.byteLength) : 'missing', bytes ? '' : 'bad']);
    },
    ready: function (start: () => void) {
      started = start;
      ui.line(['Ready.', '', 'ready']);
      ui.ready();
      if (!sourcePanel) screen!.querySelector<HTMLElement>('.shim-title-play')!.focus({ preventScroll: true });   // keep the keyboard in an open source panel
      document.addEventListener('keydown', go, true);
      if (skipping) go();
    },
    // Straight on, as if Play were pressed (now, or once the files are in).
    skip: function () { if (started) go(); else skipping = true; }
  };
}

/** The title screen itself; titleScreen above runs it. */
function TitleScreen({ total, onGo, onMount }: {
  total: number; onGo: (e: Event) => void; onMount: (control: TitleControl, el: HTMLElement) => void;
}) {
  const id = appId()!;
  const then = isThen();
  const screen = useRef<HTMLDivElement>(null);
  const log = useRef<HTMLPreElement>(null);
  const story = useRef<HTMLDetailsElement>(null);
  const [lines, setLines] = useState<LogLine[]>([]);
  const [done, setDone] = useState(1);
  const [ready, setReady] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [found, setFound] = useState<{ app: CatalogApp; apps: CatalogApp[] } | null>(null);
  const [quirkList, setQuirkList] = useState<string[][] | null>(null);
  const [shown, setShown] = useState(false);   // the thumbnail, once it has loaded
  const [record] = useState(() => savedRecord(saveStore()));
  const [forgotten, setForgotten] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const [saver, setSaver] = useState(saverOn);

  useLayoutEffect(() => {
    onMount({
      // Each is drawn at once, as the old DOM version was, so the log and the Play button are in place for the caller.
      line: row => flushSync(() => setLines(l => l.concat([row]))),
      done: n => flushSync(() => setDone(n)),
      ready: () => flushSync(() => setReady(true)),
      leave: () => flushSync(() => setLeaving(true)),
    }, screen.current!);
  }, []);
  useLayoutEffect(() => { log.current!.scrollTop = log.current!.scrollHeight; }, [lines]);
  useEffect(() => {
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, id)];
      if (!app) return;
      setFound({ app, apps });
      if (then) quirks().then(items => setQuirkList(items.length ? items : [[NO_QUIRKS]]));
    });
    // What's new: the app's story, every change to it from the repo's history (news.js fills this <details>).
    if (!then) newsScript().then(function (news) { news.story(id, story.current!); });
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
  function onClick(e: MouseEvent) {
    if (!(e.target as Element).closest('a, .shim-title-forget, .shim-title-story')) onGo(e.nativeEvent);
  }
  return (
    <div className={'shim-title' + (app && then ? ' then' : '') + (ready ? ' ready' : '') + (leaving ? ' leaving' : '')}
      role="dialog" data-sound="none" aria-label={document.title} ref={screen} onClick={onClick}>
      <div className="shim-title-card">
        <div className="shim-title-art">
          <img alt="" hidden={!shown} src={app && !then ? app.shot : undefined} onLoad={() => setShown(true)} />
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
          <button type="button" className="shim-title-play" disabled={!ready}>{ready ? '▶ Play' : 'Loading…'}</button>
          <span className="shim-title-press">or press any key</span>
          <a className="shim-title-source" href="#source" data-sound="none">{then ? 'Read the source as first restored' : 'Read the source'}</a>
          {/* The games that play themselves: a look before playing (see watch). */}
          <a className="shim-title-watch" href="?watch" hidden={!WATCH[id] || then}>Watch the computer play it first</a>
          {/* Then and now: the other version of the app, once the catalog says it has one. */}
          <a className="shim-title-era" href={eraPage(!then)} hidden={!(app && app.then)}>
            {then ? 'Play today’s version' : 'Play it as first restored, before the reworks'}
          </a>
          {/* The app for the desktop, today's version only (the jars are built from today's sources). */}
          <p className="shim-title-jar" hidden={then}>
            <JarLink id={id} className="shim-title-jar-link">{'Download ' + id + '.jar'}</JarLink>
            {' for the desktop, ' + JAR_JAVA + '.'}
            {JAR_KEYS[id] && <strong className="shim-title-jar-keys">{' ' + JAR_KEYS[id]}</strong>}
          </p>
          <details className="shim-title-story" hidden ref={story} />
        </div>
        <span className="shim-meter shim-title-meter" style={{ '--p': String(done / total) } as CSSProperties} />
        <pre className="shim-title-log" aria-hidden="true" ref={log}>
          {lines.map(([label, value, cls], i) => (
            <span key={i} className={'row' + (cls ? ' ' + cls : '')}><span>{label}</span><span>{value}</span></span>
          ))}
        </pre>
      </div>
    </div>
  );
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

