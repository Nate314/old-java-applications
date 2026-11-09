/*
 * What's new: the arcade's own history, read from the repo's git log at build time (history.txt, written by
 * web/apps/pom.xml: one "@@<commit time> <subject>" line per commit, newest first, then the top-level folders
 * it changed). Loaded after boot.js, by the home page and (on demand) by the app pages' title screens.
 *
 * - Home page (#news): the latest changes as cards, each with its app's picture and a Play button that swaps
 *   the app into the player, and Show all for the whole history back to the first commit of the recovered apps. Apps the
 *   visitor has played that changed since they last played them are named at the top, with a button that plays
 *   the newest, and get an Updated tag on their gallery card.
 * - App pages: the title screen's "Its story" lists every change to that app, newest first.
 *
 * "Last played" is kept per app in localStorage (oldjava.lastPlayed, written by boot.js when the passport
 * stamps a play); an app played before that existed counts from its first stamp (oldjava.played).
 */

import { useEffect, useState, type ComponentType } from 'react';

interface NewsItem { t: number; title: string; more: string; apps: OldjavaCatalogApp[] }
interface NewsData { items: NewsItem[]; apps: OldjavaCatalogApp[] }

declare global {
  interface Window {
    oldjavaNews?: { load(): Promise<NewsData>; Story: ComponentType<{ id: string }>; lastPlayed(id: string): number };
  }
}
let load!: () => Promise<NewsData>, day!: (t: number) => string, lastPlayed!: (id: string) => number;

// ---- an app page's title screen: the app's story ----
// Every change to the app, newest first, in a <details>; hidden when there is none (and until history.txt is read).
function Story({ id }: { id: string }) {
  const [data, setData] = useState<NewsData | null>(null);
  useEffect(() => { load().then(setData); }, []);
  const mine = data ? data.items.filter(it => it.apps.some(a => a.id === id)) : [];
  if (!mine.length) return <details className="shim-title-story" hidden />;
  const since = lastPlayed(id);
  const fresh = since ? mine.filter(it => it.t > since).length : 0;
  return (
    <details className={'shim-title-story' + (fresh > 0 ? ' fresh' : '')}>
      <summary>
        <span className="shim-story-label">{fresh ? fresh + (fresh === 1 ? ' change' : ' changes') + ' since you played' : 'Latest change'}</span>
        {' ' + mine[0].title + ' '}
        <span className="shim-story-count">{'Its story (' + mine.length + ')'}</span>
      </summary>
      <ol>
        {mine.map((it, i) => (
          <li key={i} className={since && it.t > since ? 'fresh' : undefined}>
            <time>{day(it.t)}</time>
            <span><strong>{it.title}</strong>{it.more ? ': ' + it.more : null}</span>
          </li>
        ))}
      </ol>
    </details>
  );
}

(function () {
  'use strict';
  var BASE = new URL(/* @vite-ignore */ '..', import.meta.url).href;   // this module is assets/news.js
  var LAST_KEY = 'oldjava.lastPlayed';
  var PLAYED_KEY = 'oldjava.played';
  var SHOWN = 6;        // cards before Show all
  var MANY = 4;         // a change to more apps than this is the whole arcade's, not one app's
  var MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  function read(key: string): Record<string, number> {
    try {
      var v = JSON.parse(localStorage.getItem(key) || '{}');
      return v && typeof v === 'object' ? v : {};
    } catch (e) { return {}; }
  }

  // [{ t: ms, title, more, apps: [catalog entries] }], newest first, and the catalog.
  var dataPromise: Promise<NewsData> | null = null;
  load = function () {
    if (!dataPromise) {
      dataPromise = Promise.all([
        fetch(BASE + 'history.txt').then(function (r) {
          if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
          return r.text();
        }),
        window.oldjavaPassport.catalog()
      ]).then(function (res) {
        return { items: parse(res[0], res[1]), apps: res[1] };
      }).catch(function (e) {
        console.warn('What’s new: could not read history.txt: ' + e.message);
        return { items: [], apps: [] };
      });
    }
    return dataPromise;
  };

  function parse(text: string, apps: OldjavaCatalogApp[]) {
    var byId: Record<string, OldjavaCatalogApp> = {};
    apps.forEach(function (a) { byId[a.id] = a; });
    var items: NewsItem[] = [];
    text.split(/\r?\n/).forEach(function (line) {
      var m = /^@@(\d+) (.*)$/.exec(line);
      if (m) {
        var subject = m[2].trim();
        var colon = subject.indexOf(': ');
        var more = colon < 0 ? '' : subject.slice(colon + 2);
        items.push({
          t: +m[1] * 1000,
          title: colon < 0 ? subject : subject.slice(0, colon),
          more: more,
          apps: []
        });
      } else if (items.length && byId[line]) {
        items[items.length - 1].apps.push(byId[line]);
      }
    });
    return items;
  }

  day = function (t: number) {
    var d = new Date(t);
    var s = MONTHS[d.getMonth()] + ' ' + d.getDate();
    return d.getFullYear() === new Date().getFullYear() ? s : s + ', ' + d.getFullYear();
  };

  // When the visitor last played the app (0 if never).
  lastPlayed = function (id: string) { return read(LAST_KEY)[id] || read(PLAYED_KEY)[id] || 0; };

  // The changes to an app since the visitor last played it ([] if never played).
  function changedSince(items: NewsItem[], id: string) {
    var since = lastPlayed(id);
    if (!since) return [];
    return items.filter(function (it) {
      return it.t > since && it.apps.some(function (a) { return a.id === id; });
    });
  }

  function el<K extends keyof HTMLElementTagNameMap>(tag: K, cls?: string, text?: string): HTMLElementTagNameMap[K] {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function open(id: string) {
    var passport = window.oldjavaPassport;
    if (passport.open) passport.open(id);
    else location.href = BASE + id + '/index.html';
  }

  function names(apps: OldjavaCatalogApp[]) {
    var n = apps.map(function (a) { return a.name; });
    return n.length < 2 ? n.join('') : n.slice(0, -1).join(', ') + ' and ' + n[n.length - 1];
  }

  // ---- the home page ----
  function home(section: HTMLElement) {
    var list = section.querySelector('#news-list') as HTMLElement;
    var text = section.querySelector('#news-text') as HTMLElement;
    var playButton = section.querySelector('#news-play') as HTMLElement;
    var allButton = section.querySelector('#news-all') as HTMLElement;
    var all = false;
    var data: NewsData | null = null;

    // `focus`: the app whose picture the card shows (the one the visitor played, for an update to several).
    function card(it: NewsItem, updated: boolean, focus?: OldjavaCatalogApp) {
      var li = el('li', updated ? 'updated' : '');
      var one = it.apps.length === 1 ? it.apps[0] : null;
      var site = !it.apps.length || it.apps.length > MANY;
      var pic = el('span', 'pic');
      var img = el('img');
      img.src = site ? BASE + 'icons/icon.svg' : (focus || it.apps[0]).shot;
      img.alt = '';
      img.loading = 'lazy';
      pic.appendChild(img);
      li.appendChild(pic);
      var body = el('div', 'body');
      var meta = el('span', 'meta');
      meta.appendChild(el('time', '', day(it.t)));
      if (updated) meta.appendChild(el('span', 'tag', 'Since you played'));
      else if (site) meta.appendChild(el('span', 'tag site', it.apps.length > MANY ? it.apps.length + ' apps' : 'All apps'));
      body.appendChild(meta);
      body.appendChild(el('strong', '', it.title));
      if (it.more) body.appendChild(el('span', 'more', it.more.charAt(0).toUpperCase() + it.more.slice(1)));
      if (!site) {
        var row = el('span', 'plays');
        it.apps.forEach(function (a) {
          var b = el('button', 'btn' + (one ? ' primary' : ''), one ? 'Play' : a.name);
          b.type = 'button';
          b.setAttribute('aria-label', 'Play ' + a.name);
          b.addEventListener('click', function () { open(a.id); });
          row.appendChild(b);
        });
        body.appendChild(row);
      }
      li.appendChild(body);
      return li;
    }

    function render() {
      if (!data || !data.items.length) return;
      var items = data.items;
      var changed: Record<string, NewsItem[]> = {};   // app id -> its changes since last played
      var order: OldjavaCatalogApp[] = [];
      data.apps.forEach(function (a) {
        var c = changedSince(items, a.id).filter(function (it) { return it.apps.length <= MANY; });
        if (c.length) { changed[a.id] = c; order.push(a); }
      });
      order.sort(function (a, b) { return changed[b.id][0].t - changed[a.id][0].t; });

      var first = items[items.length - 1];
      if (order.length) {
        text.textContent = 'Changed since you last played ' + (order.length === 1 ? 'it' : 'them') + ': ' +
          names(order) + '.';
        playButton.textContent = 'Play ' + (order.length === 1 ? order[0].name + '’s update' : 'the newest: ' + order[0].name);
        playButton.onclick = function () { open(order[0].id); };
      } else {
        var d = new Date(first.t);
        text.textContent = 'This arcade keeps changing: ' + items.length + ' changes since the recovered apps were first committed on ' +
          MONTHS[d.getMonth()] + ' ' + d.getDate() + ', ' + d.getFullYear() + '. The latest first.';
      }
      playButton.hidden = !order.length;
      section.classList.toggle('fresh', order.length > 0);

      // Updates to what the visitor played lead, then the rest, newest first.
      var lead: NewsItem[] = [];
      order.forEach(function (a) {
        changed[a.id].forEach(function (it) { if (lead.indexOf(it) < 0) lead.push(it); });
      });
      lead.sort(function (a, b) { return b.t - a.t; });
      var shown = lead.concat(items.filter(function (it) { return lead.indexOf(it) < 0; }));
      var count = all ? shown.length : Math.max(SHOWN, Math.min(lead.length, 12));
      list.textContent = '';
      shown.slice(0, count).forEach(function (it) {
        var mine = it.apps.filter(function (a) { return changed[a.id] && changed[a.id].indexOf(it) >= 0; })[0];
        list.appendChild(card(it, !!mine, mine));
      });
      allButton.hidden = shown.length <= count && !all;
      allButton.textContent = all ? 'Show fewer' : 'Show all ' + items.length;
      allButton.setAttribute('aria-expanded', String(all));

      // Updated tags on the gallery cards.
      Array.prototype.forEach.call(document.querySelectorAll('#apps li[data-app]'), function (li: Element) {
        li.classList.toggle('updated', !!changed[li.getAttribute('data-app')!]);
      });
      section.hidden = false;
    }

    allButton.addEventListener('click', function () {
      all = !all;
      render();
      if (!all) section.scrollIntoView({ block: 'start' });
    });
    // A play (here, in the player or another page) moves "last played" on.
    window.addEventListener('storage', function (e) {
      if (e.key === LAST_KEY || e.key === PLAYED_KEY || e.key === null) render();
    });
    window.oldjavaPassport.onChange(render);
    load().then(function (d) { data = d; render(); });
  }

  window.oldjavaNews = { load: load, Story: Story, lastPlayed: lastPlayed };
  var section = document.getElementById('news');
  if (section) home(section);
})();

export {};   // a module, imported when it is needed (its chunk is assets/news.js)
