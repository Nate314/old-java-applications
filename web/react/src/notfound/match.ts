// Short links and search for the lost page: which app (or page) a missing address names.
import { APPS, type App } from '../shared/catalog';

// The words that name each app (lower case, letters and digits only); the first is its short link. The app's own
// folder and name count too. Keep in step with apps.json when an app is added.
const SHORT: Record<string, string[]> = {
  Minesweeper: ['mines', 'sweeper'],
  FlappyBird: ['flappy', 'bird'],
  DinoGame: ['dino', 'trex', 'runner'],
  NathaNOS: ['os', 'desktop', 'paint'],
  TankGame: ['tanks', 'tank'],
  UltimateTicTacToe: ['tictactoe', 'ttt', 'ultimate', 'xo'],
  RubiksCube2D: ['cube', 'rubik', 'rubiks'],
  SpriteFloatyStars: ['stars', 'orbits', 'starcatcher'],
  ScreenSaver: ['saver', 'aquarium'],
  CircleSpawner: ['circles', 'circle'],
  Clock: ['timer', 'stopwatch', 'focus', 'world', 'timezones'],
  SynthesizerKeyboard: ['piano', 'synth', 'keyboard', 'looper'],
  MusicTheoryExpert: ['music', 'chords', 'theory', 'songwriter'],
  NumberGuess: ['guess', 'numbers', 'magic'],
  SimpleAlgorithms: ['algorithms', 'sorting', 'sort', 'paths', 'hanoi'],
  GroupCreator: ['groups', 'wheel', 'class'],
  StockChecker: ['stocks', 'trading'],
  BibleDownloader: ['bible', 'reader'],
  Windows: ['start'],
  WindowsE: ['explorer'],
};
// Words for the site's other pages.
const PAGES: Record<string, string> = { story: 'story.html', history: 'story.html', home: 'index.html', arcade: 'index.html', apps: 'index.html',
  index: 'index.html', gallery: 'index.html', embed: 'play.html', player: 'play.html',
  tour: 'index.html#tour', guide: 'index.html#tour' };
// A second word picks how the app opens.
const MODES: Record<string, string> = { then: 'then', old: 'then', original: 'then', watch: 'watch', demo: 'watch', source: 'source', code: 'source' };

export interface LostApp extends App { order: number; short: string; words: string[] }

export function norm(s: string): string { return String(s).toLowerCase().replace(/\.html?$/, '').replace(/[^a-z0-9]/g, ''); }

export const LOST_APPS: LostApp[] = APPS.map((a, i) => {
  const words = [norm(a.id), norm(a.name)].concat(SHORT[a.id] || []);
  return { ...a, order: i, short: (SHORT[a.id] || [norm(a.id)])[0], words: words.filter((w, k) => words.indexOf(w) === k) };
});

function distance(a: string, b: string): number {   // Levenshtein, for misspellings
  let prev: number[] = [];
  for (let j = 0; j <= b.length; j++) prev[j] = j;
  for (let i = 1; i <= a.length; i++) {
    const cur = [i];
    for (let j = 1; j <= b.length; j++) {
      cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    }
    prev = cur;
  }
  return prev[b.length];
}

// How surely the word names the app: 100 its own word, 60 the start of one, 50 to 30 a close misspelling.
function nameScore(app: LostApp, w: string): number {
  if (!w) return 0;
  let best = 0;
  app.words.forEach(word => {
    let s = 0;
    if (word === w) s = 100;
    else if (w.length >= 3 && word.indexOf(w) === 0) s = 60;
    else {
      // Short words are too close to too many others ("tube" is not "cube"), so only longer ones may be misspelled.
      const d = distance(w, word), allowed = w.length <= 7 ? 1 : 2;
      if (w.length >= 5 && d <= allowed) s = 50 - d * 10;
    }
    if (s > best) best = s;
  });
  return best;
}

// The one app the word surely names, or null when none does or two tie.
function sureApp(apps: LostApp[], w: string): LostApp | null {
  const scored = apps.map(a => ({ app: a, s: nameScore(a, w) }))
    .filter(x => x.s >= 30)
    .sort((x, y) => y.s - x.s);
  if (!scored.length || (scored.length > 1 && scored[1].s === scored[0].s)) return null;
  return scored[0].app;
}

/** Where the missing address surely means to go (relative to the site root), or null. */
export function destination(apps: LostApp[], segments: string[], search: string, hash: string): string | null {
  let app: LostApp | null = null, page: string | null = null, mode: string | null = null;
  segments.forEach(seg => {
    const w = norm(seg);
    if (!w || w === 'index') return;
    if (MODES[w] && !mode) { mode = MODES[w]; return; }
    // A page's own word wins over a misspelled app ("story" is not a typo of "stars").
    if (PAGES[w]) { if (!page) page = PAGES[w]; return; }
    if (!app) app = sureApp(apps, w);
  });
  const found = app as LostApp | null;
  if (found) {
    const file = mode === 'then' && found.then ? 'then.html' : 'index.html';
    if (mode === 'watch') search = search ? search + '&watch' : '?watch';
    if (mode === 'source' && !hash) hash = '#source';
    return found.id + '/' + file + search + hash;
  }
  const to = page as string | null;
  if (!to) return null;
  const at = to.indexOf('#');   // a page word with its own hash (tour: index.html#tour)
  return at < 0 ? to + search + hash : to.slice(0, at) + search + (hash || to.slice(at));
}

export function score(app: LostApp, q: string): number {
  const words = q.split(/\s+/).map(norm).filter(Boolean);
  if (!words.length) return 0;
  const text = (app.name + ' ' + app.hint).toLowerCase();
  return words.reduce((sum, w) =>   // words under three letters only count when they name the app
    sum + Math.max(nameScore(app, w), w.length >= 3 && text.indexOf(w) >= 0 ? 20 : 0), 0);
}

// Whether one of the words names the app (not only appears in its hint): that app is tagged Closest.
export function named(app: LostApp, q: string): boolean {
  return q.split(/\s+/).map(norm).some(w => nameScore(app, w) >= 30);
}
