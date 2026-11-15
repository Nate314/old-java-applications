// What's new (#news) as release notes: the history grouped into numbered updates (changes made close together),
// newest first. Each update leads with what a visitor would notice, as cards with the app's picture and Play;
// housekeeping (lazy/news-kind.ts) folds into its "Behind the scenes". Changes since the visitor last looked at
// this panel are tagged New and counted on the tab; changes to an app since it was last played keep news.ts's
// "Since you played" tag and the Play button that opens the newest.
import { useEffect, useRef } from 'react';
import './whats-new.css';
import { webp } from '../../shared/catalog';
import { H, type NewsItem } from './model';
import { allClick, day, openApp } from './news';

const GAP = 10 * 60 * 1000;   // changes further apart than this are separate updates
const SHOWN = 6;              // headline changes before Show all
const SEEN_KEY = 'oldjava.newsSeen';   // the newest change the visitor has had on screen
const SITE_ICON = new URL('icons/icon.svg', location.href).href;
const MANY = 4;               // a change to more apps than this is the whole arcade's (as news.ts counts it)

function seenAt() { try { return Number(localStorage.getItem(SEEN_KEY)) || 0; } catch (e) { return 0; } }
const seen = seenAt();        // as it was when the page opened, so New tags stay while the visitor reads them

interface Update { n: number; t: number; front: NewsItem[]; back: NewsItem[] }

/** The history (newest first) as updates: each change joins the one before it if it came within GAP of it. */
export function updates(items: NewsItem[]): Update[] {
  const groups: NewsItem[][] = [];
  items.forEach((it, i) => {
    if (i && items[i - 1].t - it.t <= GAP) groups[groups.length - 1].push(it);
    else groups.push([it]);
  });
  return groups.map((g, i) => ({ n: groups.length - i, t: g[0].t, front: g.filter(it => !it.quiet), back: g.filter(it => it.quiet) }));
}

const isNew = (it: NewsItem) => seen > 0 && it.t > seen && !it.quiet;
/** Visitor-facing changes since the visitor last looked at What's new (0 on a first visit). */
export function unseenCount() { return (H.news.items || []).filter(isNew).length; }

// "SimpleAlgorithms: ..." is titled with the app's own name, "Simple Algorithms".
const titleOf = (it: NewsItem) => { const a = window.oldjavaNews!.titledApp(it); return a ? a.name : it.title; };

/** The app this change is to that the visitor played before it (news.ts's "changed since you last played"). */
function sincePlayed(it: NewsItem) {
  const changed = H.news.view ? H.news.view.changed : {};
  return it.apps.filter(a => changed[a.id] && it.t > window.oldjavaNews!.lastPlayed(a.id))[0];
}

function Card({ it }: { it: NewsItem }) {
  const mine = sincePlayed(it);
  const site = !it.apps.length || it.apps.length > MANY, one = it.apps.length === 1;
  return (
    <li className={[mine && 'updated', isNew(it) && 'unseen'].filter(Boolean).join(' ') || undefined}>
      <span className="pic"><img src={site ? SITE_ICON : webp((mine || it.apps[0]).shot, true)} alt="" loading="lazy" /></span>
      <div className="body">
        <span className="meta">
          {isNew(it) && <span className="tag fresh-tag">New</span>}
          {mine ? <span className="tag">Since you played</span>
            : site ? <span className="tag site">{it.apps.length > MANY ? it.apps.length + ' apps' : 'All apps'}</span> : null}
        </span>
        <strong>{titleOf(it)}</strong>
        {it.more && <span className="more">{it.more.charAt(0).toUpperCase() + it.more.slice(1)}</span>}
        {!site && (
          <span className="plays">
            {it.apps.map(x => (
              <button key={x.id} className={'btn' + (one ? ' primary' : '')} type="button" aria-label={'Play ' + x.name} onClick={() => openApp(x.id)}>
                {one ? 'Play' : x.name}
              </button>
            ))}
          </span>
        )}
      </div>
    </li>
  );
}

function Release({ u }: { u: Update }) {
  const fresh = u.front.some(isNew);
  return (
    <li className={'release' + (fresh ? ' unseen' : '')}>
      <h3 className="release-head" id={'update-' + u.n}>
        <span className="release-n">Update {u.n}</span>
        <time>{day(u.t)}</time>
        <span className="release-sum">
          {u.front.length ? u.front.length + (u.front.length === 1 ? ' change' : ' changes') : 'Tidy-ups only'}
          {fresh ? ', new since you last looked' : ''}
        </span>
      </h3>
      {u.front.length > 0 && <ol className="news-list">{u.front.map(it => <Card key={it.t + it.title} it={it} />)}</ol>}
      {u.back.length > 0 && (
        <details className="backstage">
          <summary>Behind the scenes ({u.back.length})</summary>
          <ul>{u.back.map(it => <li key={it.t + it.title}><strong>{it.title}</strong>{it.more ? ': ' + it.more : ''}</li>)}</ul>
        </details>
      )}
    </li>
  );
}

export function WhatsNew({ off }: { off: string }) {
  const v = H.news.view, items = H.news.items || [];
  const all = updates(items);
  // Enough updates for SHOWN headline changes (more while the visitor has new ones to see), and at least down to
  // the newest change to an app they played since, or all of them.
  let shown = all.length, count = 0;
  if (!H.news.all) {
    const played = all.findIndex(u => u.front.some(sincePlayed));
    for (let i = 0; i < all.length; i++) {
      count += all[i].front.length;
      if (count >= SHOWN && i >= played && !(all[i + 1] && all[i + 1].front.some(isNew) && count < 3 * SHOWN)) { shown = i + 1; break; }
    }
  }
  const unseen = unseenCount(), first = items[items.length - 1];
  const text = !v ? null : v.fresh ? v.text
    : unseen ? unseen + (unseen === 1 ? ' new change' : ' new changes') + ' since you last looked.'
    : all.length + ' updates since the recovered apps were first committed' + (first ? ' on ' + day(first.t) : '') + '. The latest first.';

  // Once the panel has been on screen for a second, its newest change counts as seen (for the next visit's New tags).
  const box = useRef<HTMLElement>(null);
  const newest = items.length ? items[0].t : 0;
  useEffect(() => {
    if (!newest || newest <= seen || !box.current) return;
    let timer = 0;
    const io = new IntersectionObserver(es => {
      clearTimeout(timer);
      if (es[es.length - 1].isIntersecting) timer = window.setTimeout(() => {
        try { localStorage.setItem(SEEN_KEY, String(newest)); } catch (e) { /* not kept */ }
        io.disconnect();
      }, 1000);
    }, { threshold: 0.25 });
    io.observe(box.current);
    return () => { clearTimeout(timer); io.disconnect(); };
  }, [newest]);

  return (
    <section className={'news' + (v && v.fresh ? ' fresh' : '') + (unseen ? ' unseen' : '') + off} id="news" role="tabpanel" aria-labelledby="tab-news" hidden={!v} ref={box}>
      <div className="news-head">
        <div className="text"><h2 id="news-title">What’s new</h2><p id="news-text" aria-live="polite">{text}</p></div>
        <div className="actions">
          <button className="btn primary" type="button" id="news-play" hidden={!(v && v.fresh)} onClick={() => { if (v && v.play && v.play.app) openApp(v.play.app); }}>
            {v && v.play ? v.play.label : 'Play it'}
          </button>
          {' '}
          <button className="btn" type="button" id="news-all" aria-expanded={H.news.all} aria-controls="news-list" hidden={shown >= all.length && !H.news.all} onClick={allClick}>
            {H.news.all ? 'Show fewer' : 'Show all ' + all.length + ' updates'}
          </button>
        </div>
      </div>
      <ol className="releases" id="news-list">{all.slice(0, shown).map(u => <Release key={u.n} u={u} />)}</ol>
    </section>
  );
}
