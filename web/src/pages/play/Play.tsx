// The embeddable player (play.html?app=<App>): a poster first, the app only after Play, in a frame scaled to the widget.
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { APPS, type App } from '../../shared/catalog';
import './play.css';

// The same four apps the runtime's WATCH table lets the computer play.
const WATCHABLE = ['Minesweeper', 'UltimateTicTacToe', 'TankGame', 'DinoGame'];
// The app's page keeps a viewport of at least 640 x 520 (so a small widget is the app's page scaled down, its
// notes and windows laid out as usual), or as big as its card asks for (CircleSpawner places its own 380 px
// window, Rubik's Cube's is 700 px tall), as in the home page's player. The viewport keeps the widget's shape.
const MIN_W = 640;
const MIN_H = 520;   // the home page player's height, which leaves the watch line room above the windows

type Shown =
  | { mode: 'oops'; text: string }
  | { mode: 'poster'; app: App }
  | { mode: 'stage'; app: App; watch: boolean };

const params = new URLSearchParams(location.search);
const then = params.has('then');

/** What the address asks for: the app's poster, the app already running (the computer playing), or no such app. */
function first(): Shown {
  const id = params.get('app');
  const app = APPS.find(a => a.id === id);
  if (!app) return { mode: 'oops', text: id ? 'There is no app named "' + id + '" here.' : 'Which app? Add ?app= and its name to the address.' };
  if (params.has('watch') && WATCHABLE.indexOf(app.id) >= 0 && !(then && app.then)) return { mode: 'stage', app, watch: true };
  return { mode: 'poster', app };
}

function page(app: App, watch: boolean) {
  return app.id + '/' + (then && app.then ? 'then.html' : 'index.html') + '?embed' + (watch ? '&watch' : '');
}

export function Play() {
  const [shown, setShown] = useState<Shown>(first);
  const [size, setSize] = useState<{ w: number; h: number; scale: number } | null>(null);
  const [lit, setLit] = useState(0);   // how many times the Full page link has been pointed at
  const [full, setFull] = useState(false);   // the page fills the screen
  const main = useRef<HTMLDivElement>(null);
  const frame = useRef<HTMLIFrameElement>(null);
  const fullLink = useRef<HTMLAnchorElement>(null);
  const app = shown.mode === 'oops' ? null : shown.app;
  const running = shown.mode === 'stage' ? shown : null;
  const thenApp = !!(app && then && app.then);
  // The poster stays as it was drawn for the first app (hidden once that runs).
  const [poster] = useState(() => shown.mode === 'poster' ? shown.app : null);
  const thenPoster = !!(poster && then && poster.then);

  function fit() {
    if (!app || !main.current) return;
    const w = main.current.clientWidth;
    const h = main.current.clientHeight;
    setSize({ w, h, scale: Math.min(1, w / Math.max(MIN_W, app.minWidth || 0), h / Math.max(MIN_H, app.minHeight || 0)) });
  }
  function run(a: App, watch: boolean) { setShown({ mode: 'stage', app: a, watch }); }

  useEffect(() => { if (app) document.title = app.name + ' | Old Java Applications'; }, [app]);
  useLayoutEffect(fit, [shown]);
  useEffect(() => {
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  });
  // The frame takes the keys once the app is in it, unless the computer is playing.
  useEffect(() => { if (running && !running.watch) frame.current!.focus(); }, [running && running.app]);
  // What the app's page asks of the page around it (it asks the home page's player the same things).
  useEffect(() => {
    function onMessage(e: MessageEvent) {
      if (e.origin !== location.origin || e.source !== frame.current?.contentWindow || !e.data) return;
      if (e.data.oldjavaPlay) {   // a suggestion on the exit screen
        const next = APPS.find(a => a.id === e.data.oldjavaPlay);
        if (next) run(next, false);
        return;
      }
      // Share (and the replay viewer) belong to the full page; point at it, since a message is no click that
      // may open a tab.
      if (typeof e.data.oldjavaShare === 'string' || e.data.oldjavaClip) setLit(n => n + 1);
    }
    window.addEventListener('message', onMessage);
    return () => window.removeEventListener('message', onMessage);
  }, []);
  // Drawn again for each time (a new link, so its glow starts over), and given the focus.
  useEffect(() => { if (lit) fullLink.current!.focus(); }, [lit]);
  useEffect(() => {
    if (!document.fullscreenEnabled) return;
    function change() {
      setFull(!!document.fullscreenElement);
      setTimeout(fit, 50);
    }
    document.addEventListener('fullscreenchange', change);
    return () => document.removeEventListener('fullscreenchange', change);
  });

  const fsTitle = full ? 'Leave full screen' : 'Fill the screen';
  return (
    <>
      <div className="main" ref={main}>
        <div className={'poster' + (thenPoster ? ' then' : '')} id="poster" hidden={shown.mode !== 'poster'}>
          <img className="bg" id="poster-bg" alt="" src={poster ? poster.shot : undefined} />
          {' '}
          {/* The thumbnail and hint show today's app, so the Then poster keeps only a faded backdrop of it. */}
          <img className="shot" id="poster-shot" alt={poster ? poster.alt : ''} src={poster ? poster.shot : undefined}
            hidden={thenPoster} onClick={() => run(poster!, false)} />
          <div className="text">
            <p className="kicker" id="poster-kicker">{thenPoster ? 'Then: as first restored' : 'Play in your browser'}</p>
            <h1 id="poster-name">{poster ? poster.name : null}</h1>
            <p className="hint" id="poster-hint">{!poster ? null : thenPoster
              ? 'The recovered code with the light bug fixes of its 2026 restoration, before the later reworks.' : poster.hint}</p>
            <button className="play" type="button" id="poster-play" onClick={() => run(poster!, false)}>Play</button>
            <p className="note">Runs right here, nothing to install.</p>
          </div>
        </div>
        <div className="stage" id="stage" hidden={!running}>
          <iframe id="frame" title={running ? running.app.name : 'App'} allow="autoplay; fullscreen; clipboard-write; microphone" ref={frame}
            src={running ? page(running.app, running.watch) : undefined}
            style={running && size ? {
              width: Math.round(size.w / size.scale) + 'px', height: Math.round(size.h / size.scale) + 'px',
              transform: size.scale < 1 ? 'scale(' + size.scale + ')' : undefined,
            } : undefined} />
        </div>
        <div className="oops" id="oops" hidden={shown.mode !== 'oops'}>
          <p id="oops-text">{shown.mode === 'oops' ? shown.text : null}</p>
          <p><a href="index.html" target="_blank" rel="noopener">See all 20 apps</a></p>
        </div>
      </div>
      <nav className="bar" aria-label="Old Java Applications">
        <a className="brand" href="index.html" target="_blank" rel="noopener" title="Old Java Applications: 20 recovered Java apps you can play in the browser">
          <img src="icons/icon.svg" alt="" />
          <span>Old Java Applications</span>
        </a>
        {' '}
        <span className="now" id="now">{app ? app.name + (thenApp ? ' (Then: as first restored)' : '') : null}</span>
        {' '}
        <span className="go">
          <button type="button" id="fullscreen" title={fsTitle} aria-label={fsTitle} hidden={!document.fullscreenEnabled} onClick={() => {
            if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
            else document.documentElement.requestFullscreen().catch(() => {});
          }}>⛶</button>
          {' '}
          <a key={lit} id="full" className={lit ? 'lit' : undefined} href={app ? app.id + '/' + (thenApp ? 'then.html' : 'index.html') : 'index.html'}
            target="_blank" rel="noopener" title="Open this app on its own page, with its source and more" hidden={shown.mode === 'oops'} ref={fullLink}>
            Full page ↗
          </a>
          {' '}
          <a className="more" href="index.html" target="_blank" rel="noopener" title="20 old Java apps, all playable in the browser">
            All 20 apps ↗
          </a>
        </span>
      </nav>
    </>
  );
}
