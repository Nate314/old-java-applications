// What's new on the home page (#news; WhatsNew.tsx draws it). Apps the visitor has played that changed since they
// last played them are named at the top, with a button that plays the newest, and get an Updated tag on their
// gallery card. news.js (the runtime's lazy/news.tsx) reads history.txt and keeps when each app was last played;
// this works out that lead text, the Play button and which apps changed, into H.news.view.
import { H, draw, type NewsItem } from './model';

export const MANY = 4;         // a change to more apps than this is the whole arcade's, not one app's
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const LAST_KEY = 'oldjava.lastPlayed';
const PLAYED_KEY = 'oldjava.played';

export interface NewsView { text: string; play: { label: string; app: string } | null; fresh: boolean; changed: Record<string, boolean> }

let news: NonNullable<Window['oldjavaNews']> | null = null;
let playLabel = 'Play it';   // the Play button keeps its last words while it is hidden

export function day(t: number) { return news!.day(t); }

function names(apps: OldjavaCatalogApp[]) {
  var n = apps.map(function (a) { return a.name; });
  return n.length < 2 ? n.join('') : n.slice(0, -1).join(', ') + ' and ' + n[n.length - 1];
}
// The changes to an app since the visitor last played it ([] if never played).
function changedSince(items: NewsItem[], id: string) {
  var since = news!.lastPlayed(id);
  if (!since) return [];
  return items.filter(function (it) { return it.t > since && it.apps.some(function (a) { return a.id === id; }); });
}

/** Works out the panel from the history and the visitor's plays, and draws it. */
function render() {
  const items = H.news.items;
  if (!items || !items.length) return;
  var changed: Record<string, NewsItem[]> = {};   // app id -> its changes since last played
  var order: OldjavaCatalogApp[] = [];
  H.news.apps.forEach(function (a) {
    var c = changedSince(items, a.id).filter(function (it) { return it.apps.length <= MANY; });
    if (c.length) { changed[a.id] = c; order.push(a); }
  });
  order.sort(function (a, b) { return changed[b.id][0].t - changed[a.id][0].t; });

  var first = items[items.length - 1], text: string;
  if (order.length) {
    text = 'Changed since you last played ' + (order.length === 1 ? 'it' : 'them') + ': ' + names(order) + '.';
    playLabel = 'Play ' + (order.length === 1 ? order[0].name + '’s update' : 'the newest: ' + order[0].name);
  } else {
    var d = new Date(first.t);
    text = 'This arcade keeps changing: ' + items.length + ' changes since the recovered apps were first committed on ' +
      MONTHS[d.getMonth()] + ' ' + d.getDate() + ', ' + d.getFullYear() + '. The latest first.';
  }
  H.news.view = {
    text, play: order.length ? { label: playLabel, app: order[0].id } : { label: playLabel, app: '' }, fresh: order.length > 0,
    changed: Object.fromEntries(Object.keys(changed).map(id => [id, true])),
  };
  draw();
}

/** Show all, or Show fewer (which scrolls back to the panel's top). */
export function allClick() {
  H.news.all = !H.news.all;
  render();
  if (!H.news.all) document.getElementById('news')!.scrollIntoView({ block: 'start' });
}

/** A card's Play, or the panel's: the app in the player (or its page, if the player cannot have it). */
export function openApp(id: string) {
  var passport = window.oldjavaPassport;
  if (passport.open) passport.open(id);
  else location.href = new URL(id + '/index.html', location.href).href;
}

export function initNews() {
  import('../../runtime/lazy/news').then(function () {
    news = window.oldjavaNews!;
    // A play (here, in the player or another page) moves "last played" on.
    window.addEventListener('storage', function (e) {
      if (e.key === LAST_KEY || e.key === PLAYED_KEY || e.key === null) render();
    });
    window.oldjavaPassport.onChange(render);
    news.load().then(function (d) { H.news.items = d.items; H.news.apps = d.apps; render(); });
  });
}
