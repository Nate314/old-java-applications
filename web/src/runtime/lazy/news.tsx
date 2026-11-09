/*
 * What's new: the arcade's own history, read from the repo's git log at build time (history.txt, written by
 * web/apps/pom.xml: one "@@<commit time> <subject>" line per commit, newest first, then the top-level folders
 * it changed). Loaded after boot.js, by the home page and (on demand) by the app pages' title screens.
 *
 * - Home page (#news): src/pages/home/news.ts shows the latest changes from load(), and what changed since the
 *   visitor last played each app (lastPlayed).
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
    oldjavaNews?: { load(): Promise<NewsData>; Story: ComponentType<{ id: string }>; lastPlayed(id: string): number; day(t: number): string };
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

  window.oldjavaNews = { load: load, Story: Story, lastPlayed: lastPlayed, day: day };
})();

export {};   // a module, imported when it is needed (its chunk is assets/news.js)
