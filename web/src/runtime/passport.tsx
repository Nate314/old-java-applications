import { useLayoutEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import { news, reducedMotion } from './band-news';
import { appId, catalog, type CatalogApp } from './catalog';
import { indexOf, isThen, pageOf, picks } from './then-and-now';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';
import { openShare } from './share';
import { playSound } from './sounds';

declare global {
  interface OldjavaPassport { stamp(id: string): void }
}

/** { AppId: time of the first play } */
export type Played = Record<string, number>;

export let played: Played;
export let passport: OldjavaPassport;

// ---- arcade passport ----
// { AppId: time of the first play } in localStorage. Every page keeps a copy and hears about stamps made in
// other pages (the home page hears its player's iframe) through the storage event.
const PLAYED_KEY = 'oldjava.played';

function readPlayed(): Played {
  try {
    var v = JSON.parse(localStorage.getItem(PLAYED_KEY) || '{}');
    return v && typeof v === 'object' ? v : {};
  } catch (e) { return {}; }
}

const passportListeners: ((fresh: string | null) => void)[] = [];

function passportChanged(id: string | null) { passportListeners.forEach(function (fn) { fn(id); }); }

// A new stamp: a toast with the count and an app not played yet; the last one gets confetti.
export function stampToast(id: string) {
  catalog().then(function (apps: CatalogApp[]) {
    var app = apps[indexOf(apps, id)];
    if (!app) return;
    var count = apps.filter(function (a: CatalogApp) { return played[a.id]; }).length;
    var next = passport.unplayed(apps, id)[0];
    var done = count >= apps.length;
    playSound(done ? 'complete' : 'stamp');
    if (done) confetti();
    if (news) { stampNews(app, count, apps.length, next); return; }
    floatToast(app.shot, done ? 'Arcade complete!' : 'Stamp collected: ' + app.name,
      done ? 'You have played all ' + apps.length + ' apps.' : count + ' of ' + apps.length + ' apps played.',
      <Meter p={count / apps.length} />, function (close) {
        return (
          <>
            {done && <button type="button" className="shim-toast-go" data-sound="none" onClick={() => { close(); openShare(null); }}>{'Share it ›'}</button>}
            {next && toastLink(close, next, 'Try ' + next.name + ' ›')}
          </>
        );
      });
  });
}

function Meter({ p }: { p: number }) {
  return <span className="shim-meter" style={{ '--p': String(p) } as CSSProperties} />;
}

/** The toast on screen, if one is. */
let shownToast: { remove(): void } | null = null;

// A toast in the page's corner (the home page, where there is no band news line): a picture, a title, a line,
// more under it, and actions(close) after it. It goes by itself after 9 seconds; a new one replaces it.
export function floatToast(shot: string, titleText: string, lineText: string, more: ReactNode, actions: (close: () => void) => ReactNode) {
  if (shownToast) shownToast.remove();
  var gone = false;
  function remove() {
    if (gone) return;
    gone = true;
    close();
    if (shownToast === handle) shownToast = null;
  }
  var fade: (() => void) | null = null;
  const close = openLayer(<Toast shot={shot} title={titleText} line={lineText} more={more} actions={actions(remove)} onClose={remove}
    onMount={f => { fade = f; }} />);
  const handle = { remove };
  shownToast = handle;
  setTimeout(function () {
    if (gone) return;
    fade!();
    setTimeout(remove, 600);
  }, 9000);
  return handle;
}

function Toast({ shot, title, line, more, actions, onClose, onMount }: {
  shot: string; title: string; line: string; more: ReactNode; actions: ReactNode; onClose: () => void; onMount: (fade: () => void) => void;
}) {
  const toast = useRef<HTMLDivElement>(null);
  const [going, setGoing] = useState(false);
  useOwnKeys(toast);
  useLayoutEffect(() => { onMount(() => setGoing(true)); }, []);
  return (
    <div className={'shim-toast' + (going ? ' gone' : '')} role="status" ref={toast}>
      <span className="shim-toast-seal"><img src={shot} alt="" /></span>
      <div className="shim-toast-text">
        <strong>{title}</strong>
        <span>{line}</span>
        {more}
      </div>
      {actions}
      <CloseButton onClick={onClose} />
    </div>
  );
}

// A toast's link to an app. On the home page (passport.open) it swaps the app into the player instead.
export function toastLink(close: () => void, app: OldjavaCatalogApp, label: string) {
  return (
    <a className="shim-toast-go" href={pageOf(app)} onClick={e => {
      if (!passport.open || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      close();
      passport.open(app.id);
    }}>{label}</a>
  );
}

function stampNews(app: OldjavaCatalogApp, count: number, total: number, next: OldjavaCatalogApp | undefined) {
  var done = count >= total;
  var brag: HTMLButtonElement | null = null;
  news!.show('stamp', <span className="shim-news-seal"><img src={app.shot} alt="" /></span>, (
    <>
      <strong>{done ? 'Arcade complete! ' : 'Stamp collected: ' + app.name + ' '}</strong>
      <span>{done ? 'You have played all ' + total + ' apps.' : count + ' of ' + total + ' apps played.'}</span>
      <Meter p={count / total} />
    </>
  ), (
    <>
      {done && (
        <button type="button" className="shim-news-go" data-sound="none" ref={b => { brag = b; }}
          onClick={() => { news!.clear(); openShare(null, brag); }}>{'Share it ›'}</button>
      )}
      {next && <a className="shim-news-go" href={pageOf(next)}>{'Try ' + next.name + ' ›'}</a>}
    </>
  ), 9000);
}

function confetti() {
  if (reducedMotion()) return;
  var canvas = document.createElement('canvas');
  canvas.className = 'shim-confetti';
  var w = canvas.width = innerWidth, h = canvas.height = innerHeight;
  document.body.appendChild(canvas);
  var g = canvas.getContext('2d')!;
  var colors = ['#2563eb', '#22c55e', '#f59e0b', '#ef4444', '#a855f7', '#06b6d4'];
  var bits: { x: number; y: number; vx: number; vy: number; r: number; vr: number; c: string }[] = [];
  for (var i = 0; i < 160; i++) {
    bits.push({ x: w / 2, y: h * 0.35, vx: (Math.random() - 0.5) * 14, vy: -Math.random() * 12 - 4,
                r: Math.random() * 6.28, vr: (Math.random() - 0.5) * 0.4, c: colors[i % colors.length] });
  }
  var start = performance.now();
  (function frame(t: number) {
    g.clearRect(0, 0, w, h);
    bits.forEach(function (b) {
      b.vy += 0.35; b.x += b.vx; b.y += b.vy; b.r += b.vr;
      g.save(); g.translate(b.x, b.y); g.rotate(b.r); g.fillStyle = b.c; g.fillRect(-5, -3, 10, 6); g.restore();
    });
    if (t - start < 3000) requestAnimationFrame(frame);
    else canvas.remove();
  })(start);
}

// An app page is stamped by the first click or key press in the app itself (not in the site's own panels),
// so the home page's player does not stamp Minesweeper just by loading it. A console program
// (<body data-console>) has nothing to press: running it counts.
const CHROME = '.shim-toolbar, .shim-drawer, .shim-share, .shim-scrim, .shim-overlay, .shim-toast, .shim-pad, .shim-title, .shim-source';

export function stampOnPlay() {
  var id = appId();
  if (document.body.hasAttribute('data-console')) { passport.stamp(id!); return; }
  function onInput(e: Event) {
    if (e.target && (e.target as Element).closest && (e.target as Element).closest(CHROME)) return;
    document.removeEventListener('pointerdown', onInput, true);
    document.removeEventListener('keydown', onInput, true);
    passport.stamp(id!);
  }
  document.addEventListener('pointerdown', onInput, true);
  document.addEventListener('keydown', onInput, true);
}

export function init() {
  played = readPlayed();
  window.addEventListener('storage', function (e) {
    if (e.key !== PLAYED_KEY && e.key !== null) return;
    var before = played;
    played = readPlayed();
    var fresh = Object.keys(played).filter(function (k) { return !before[k]; });
    passportChanged(fresh[0] || null);
  });
  passport = window.oldjavaPassport = {
    has: function (id: string) { return !!played[id]; },
    stamp: function (id: string) {
      // When it was last played, for What's new (news.js): changes since then are news to this visitor.
      if (!isThen()) try {
        var last = JSON.parse(localStorage.getItem('oldjava.lastPlayed') || '{}') || {};
        last[id] = Date.now();
        localStorage.setItem('oldjava.lastPlayed', JSON.stringify(last));
      } catch (e) { /* not kept */ }
      played = readPlayed();   // another page may have stamped meanwhile
      if (played[id]) return;
      played[id] = Date.now();
      try { localStorage.setItem(PLAYED_KEY, JSON.stringify(played)); } catch (e) { /* kept for this page only */ }
      passportChanged(id);
    },
    // Stamps written to localStorage by this page (saves brought from another device): read them again.
    reload: function () { played = readPlayed(); passportChanged(null); },
    reset: function () {
      played = {};
      try { localStorage.removeItem(PLAYED_KEY); } catch (e) { /* nothing saved */ }
      passportChanged(null);
    },
    // fn(id): id is the app just stamped, or null when the stamps changed some other way (a reset).
    onChange: function (fn: (fresh: string | null) => void) { passportListeners.push(fn); },
    // The home page sets this to swap an app into its player instead of following a link.
    open: null,
    catalog: function () { return catalog(); },
    // Apps not played yet, others of the same category as `id` first (see picks).
    unplayed: function (apps: CatalogApp[], id: string | null) {
      return picks(apps, id, apps.length).filter(function (a: CatalogApp) { return !played[a.id]; });
    }
  };
}
