// The embeddable player's behavior: the poster, then the app in a frame scaled to the widget.
import { APPS, type App } from '../../shared/catalog';
import { byId } from '../../shared/dom';

// The same four apps boot.js's WATCH table lets the computer play.
const WATCHABLE = ['Minesweeper', 'UltimateTicTacToe', 'TankGame', 'DinoGame'];
// The app's page keeps a viewport of at least 640 x 520 (so a small widget is the app's page scaled down, its
// notes and windows laid out as usual), or as big as its card asks for (CircleSpawner places its own 380 px
// window, Rubik's Cube's is 700 px tall), as in the home page's player. The viewport keeps the widget's shape.
const MIN_W = 640;
const MIN_H = 520;   // the home page player's height, which leaves the watch line room above the windows

export function initPlay(): void {
  const params = new URLSearchParams(location.search);
  const poster = byId('poster');
  const stage = byId('stage');
  const frame = byId<HTMLIFrameElement>('frame');
  const full = byId<HTMLAnchorElement>('full');
  const now = byId('now');
  let current: App | null = null;
  const then = params.has('then');

  function page(app: App, watch: boolean) {
    return app.id + '/' + (then && app.then ? 'then.html' : 'index.html') + '?embed' + (watch ? '&watch' : '');
  }

  function label(app: App) {
    now.textContent = app.name + (then && app.then ? ' (Then: as first restored)' : '');
    full.href = app.id + '/' + (then && app.then ? 'then.html' : 'index.html');
    document.title = app.name + ' | Old Java Applications';
  }

  function showPoster(app: App) {
    byId<HTMLImageElement>('poster-bg').src = app.shot;
    const shot = byId<HTMLImageElement>('poster-shot');
    shot.src = app.shot;
    shot.alt = app.alt;
    byId('poster-name').textContent = app.name;
    byId('poster-hint').textContent = app.hint;
    // The thumbnail and hint show today's app, so the Then poster keeps only a faded backdrop of it.
    if (then && app.then) {
      byId('poster-kicker').textContent = 'Then: as first restored';
      byId('poster-hint').textContent =
        'The recovered code with the light bug fixes of its 2026 restoration, before the later reworks.';
      shot.hidden = true;
      poster.classList.add('then');
    }
    poster.hidden = false;
  }

  function fit() {
    if (!current) return;
    const box = stage.parentNode as HTMLElement;
    const w = box.clientWidth;
    const h = box.clientHeight;
    const scale = Math.min(1, w / Math.max(MIN_W, current.minWidth || 0), h / Math.max(MIN_H, current.minHeight || 0));
    frame.style.width = Math.round(w / scale) + 'px';
    frame.style.height = Math.round(h / scale) + 'px';
    frame.style.transform = scale < 1 ? 'scale(' + scale + ')' : '';
  }
  window.addEventListener('resize', fit);

  function run(app: App, watch: boolean) {
    current = app;
    label(app);
    poster.hidden = true;
    stage.hidden = false;
    fit();
    frame.title = app.name;
    frame.src = page(app, watch);
    if (!watch) frame.focus();
  }

  // What the app's page asks of the page around it (it asks the home page's player the same things).
  window.addEventListener('message', e => {
    if (e.origin !== location.origin || e.source !== frame.contentWindow || !e.data) return;
    if (e.data.oldjavaPlay) {   // a suggestion on the exit screen
      const next = APPS.find(a => a.id === e.data.oldjavaPlay);
      if (next) run(next, false);
      return;
    }
    // Share (and the replay viewer) belong to the full page; point at it, since a message is no click that
    // may open a tab.
    if (typeof e.data.oldjavaShare === 'string' || e.data.oldjavaClip) {
      full.classList.remove('lit');
      void full.offsetWidth;
      full.classList.add('lit');
      full.focus();
    }
  });

  const fs = byId('fullscreen');
  if (document.fullscreenEnabled) {
    fs.hidden = false;
    fs.addEventListener('click', () => {
      if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
      else document.documentElement.requestFullscreen().catch(() => {});
    });
    document.addEventListener('fullscreenchange', () => {
      fs.title = document.fullscreenElement ? 'Leave full screen' : 'Fill the screen';
      fs.setAttribute('aria-label', fs.title);
      setTimeout(fit, 50);
    });
  }

  function oops(text: string) {
    byId('oops-text').textContent = text;
    byId('oops').hidden = false;
    full.hidden = true;
  }

  const id = params.get('app');
  const app = APPS.find(a => a.id === id);
  if (!app) { oops(id ? 'There is no app named "' + id + '" here.' : 'Which app? Add ?app= and its name to the address.'); return; }
  current = app;
  label(app);
  if (params.has('watch') && WATCHABLE.indexOf(app.id) >= 0 && !(then && app.then)) { run(app, true); return; }
  showPoster(app);
  byId('poster-play').addEventListener('click', () => run(app, false));
  byId('poster-shot').addEventListener('click', () => run(app, false));
}
