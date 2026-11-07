import { news, reducedMotion } from './band-news';
import { appId, catalog, type CatalogApp } from './catalog';
import { closeButton, indexOf, isThen, ownKeys, pageOf, picks } from './then-and-now';
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
    var meter = document.createElement('span');
    meter.className = 'shim-meter';
    meter.style.setProperty('--p', String(count / apps.length));
    floatToast(app.shot, done ? 'Arcade complete!' : 'Stamp collected: ' + app.name,
      done ? 'You have played all ' + apps.length + ' apps.' : count + ' of ' + apps.length + ' apps played.',
      [meter], function (toast) {
        var actions: HTMLElement[] = [];
        if (done) {
          var brag = document.createElement('button');
          brag.type = 'button';
          brag.className = 'shim-toast-go';
          brag.setAttribute('data-sound', 'none');
          brag.textContent = 'Share it ›';
          brag.addEventListener('click', function () { toast.remove(); openShare(null); });
          actions.push(brag);
        }
        if (next) actions.push(toastLink(toast, next, 'Try ' + next.name + ' ›'));
        return actions;
      });
  });
}

// A toast floating at the top of the home page (an app page uses its band's news line instead): an app's
// thumbnail as a seal, a title, a line, more nodes under them, and the buttons or links actions(toast) makes.
// It replaces the toast before it and fades away after 9 seconds.
export function floatToast(shot: string, titleText: string, lineText: string, more: Node[], actions: (toast: HTMLElement) => Node[]) {
  var old = document.querySelector('.shim-toast');
  if (old) old.remove();
  var toast = document.createElement('div');
  toast.className = 'shim-toast';
  toast.setAttribute('role', 'status');
  var seal = document.createElement('span');
  seal.className = 'shim-toast-seal';
  var img = document.createElement('img');
  img.src = shot;
  img.alt = '';
  seal.appendChild(img);
  var text = document.createElement('div');
  text.className = 'shim-toast-text';
  var title = document.createElement('strong');
  var line = document.createElement('span');
  title.textContent = titleText;
  line.textContent = lineText;
  ([title, line] as Node[]).concat(more).forEach(function (n) { text.appendChild(n); });
  toast.appendChild(seal);
  toast.appendChild(text);
  actions(toast).forEach(function (n: Node) { toast.appendChild(n); });
  toast.appendChild(closeButton(function () { toast.remove(); }));
  ownKeys(toast);
  document.body.appendChild(toast);
  setTimeout(function () {
    toast.classList.add('gone');
    setTimeout(function () { toast.remove(); }, 600);
  }, 9000);
  return toast;
}

// A toast's link to an app: on the home page a plain click swaps the app into the player instead.
export function toastLink(toast: HTMLElement, app: OldjavaCatalogApp, label: string) {
  var go = document.createElement('a');
  go.className = 'shim-toast-go';
  go.href = pageOf(app);
  go.textContent = label;
  go.addEventListener('click', function (e) {
    if (!passport.open || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
    e.preventDefault();
    toast.remove();
    passport.open(app.id);
  });
  return go;
}

// The same news on an app page, in the band: the thumbnail pressed in as a seal, the count, and where to go.
function stampNews(app: OldjavaCatalogApp, count: number, total: number, next: OldjavaCatalogApp | undefined) {
  var done = count >= total;
  var seal = document.createElement('span');
  seal.className = 'shim-news-seal';
  var img = document.createElement('img');
  img.src = app.shot;
  img.alt = '';
  seal.appendChild(img);
  var title = document.createElement('strong');
  title.textContent = done ? 'Arcade complete! ' : 'Stamp collected: ' + app.name + ' ';
  var line = document.createElement('span');
  line.textContent = done ? 'You have played all ' + total + ' apps.' : count + ' of ' + total + ' apps played.';
  var meter = document.createElement('span');
  meter.className = 'shim-meter';
  meter.style.setProperty('--p', String(count / total));
  var actions: HTMLElement[] = [];
  if (done) {
    var brag = document.createElement('button');
    brag.type = 'button';
    brag.className = 'shim-news-go';
    brag.setAttribute('data-sound', 'none');
    brag.textContent = 'Share it \u203a';
    brag.addEventListener('click', function () { news!.clear(); openShare(null, brag); });
    actions.push(brag);
  }
  if (next) {
    var go = document.createElement('a');
    go.className = 'shim-news-go';
    go.href = pageOf(next);
    go.textContent = 'Try ' + next.name + ' \u203a';
    actions.push(go);
  }
  news!.show('stamp', [seal], [title, line, meter], actions, 9000);
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
