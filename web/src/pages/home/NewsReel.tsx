// The top of What's new: a reel of the apps that changed most recently, newest first, one big card each with the
// app's picture (its preview clip while the card is hovered or focused, or centred on a touch screen), the change
// in its own words and Play. The numbered updates below it keep the whole history.
import { useEffect, useRef, useState } from 'react';
import './news-reel.css';
import { clipMoves, webp } from '../../shared/catalog';
import { H, type NewsItem } from './model';
import { MANY, day, openApp } from './news';

const REEL = 8;   // apps on the reel

/** The newest change a visitor would notice for each of the REEL apps changed last (history is newest first). */
export function reelOf(items: NewsItem[]) {
  const seen = new Set<string>(), out: { it: NewsItem; app: OldjavaCatalogApp }[] = [];
  for (const it of items) {
    if (it.quiet || !it.apps.length || it.apps.length > MANY) continue;
    const app = it.apps.find(a => !seen.has(a.id));
    if (!app) continue;
    seen.add(app.id);
    out.push({ it, app });
    if (out.length === REEL) break;
  }
  return out;
}

// "SimpleAlgorithms: drag the rectangle" reads "Drag the rectangle" under the app's name; other subjects whole.
const said = (it: NewsItem) => {
  const s = window.oldjavaNews!.titledApp(it) && it.more ? it.more : it.title + (it.more ? ': ' + it.more : '');
  return s.charAt(0).toUpperCase() + s.slice(1);
};

export function NewsReel({ items }: { items: NewsItem[] }) {
  const reel = reelOf(items);
  const [live, setLive] = useState<string | null>(null);
  const box = useRef<HTMLOListElement>(null);
  const moves = clipMoves();
  const changed = H.news.view ? H.news.view.changed : {};

  // Touch screens: the card in the middle of the reel plays.
  useEffect(() => {
    if (!moves || matchMedia('(hover: hover) and (pointer: fine)').matches || !box.current || !window.IntersectionObserver) return;
    const io = new IntersectionObserver(es => es.forEach(e => {
      const id = (e.target as HTMLElement).dataset.app!;
      if (e.isIntersecting) setLive(id); else setLive(l => (l === id ? null : l));
    }), { root: box.current, rootMargin: '0px -40% 0px -40%' });
    box.current.querySelectorAll('li').forEach(li => io.observe(li));
    return () => io.disconnect();
  }, [reel.length]);

  if (!reel.length) return null;
  return (
    <ol className="news-reel" aria-label="Apps changed most recently" ref={box}>
      {reel.map(({ it, app }) => (
        <li key={app.id} data-app={app.id} className={changed[app.id] ? 'updated' : undefined}
            onMouseEnter={() => setLive(app.id)} onMouseLeave={() => setLive(l => (l === app.id ? null : l))}
            onFocus={() => setLive(app.id)}>
          <span className="reel-pic">
            <img src={webp(app.shot)} alt="" loading="lazy" />
            {moves && app.clip && live === app.id && (
              <video muted loop playsInline autoPlay aria-hidden="true" src={'clips/' + app.id + '.mp4'} />
            )}
          </span>
          <span className="reel-text">
            <span className="reel-meta">
              <strong>{app.name}</strong>
              <time>{day(it.t)}</time>
              {changed[app.id] && <span className="tag">Since you played</span>}
            </span>
            <span className="reel-said">{said(it)}</span>
          </span>
          <button className="btn primary" type="button" aria-label={'Play ' + app.name} onClick={() => openApp(app.id)}>
            ▶ Play
          </button>
        </li>
      ))}
    </ol>
  );
}
