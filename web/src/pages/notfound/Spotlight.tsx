// The lost page's spotlight: one app, big and moving, with a Play button, so a mistyped link lands one click from
// a game instead of on a list. It shows the search's best match and follows the search as the visitor types; with
// no match it offers the app the visitor played last (oldjava.lastPlayed), or the gallery's first app.
import './spotlight.css';
import { useEffect, useState } from 'react';
import { webp } from '../../shared/catalog';
import { LOST_APPS, type LostApp } from './match';

// Same test as play/Shot.tsx and runtime/title-attract.tsx (separate bundles): no clip for reduced motion,
// Save-Data or links slower than 4G.
const moves = () => !matchMedia('(prefers-reduced-motion: reduce)').matches && !navigator.connection?.saveData &&
  (navigator.connection?.effectiveType || '4g') === '4g' && !!document.createElement('video').canPlayType('video/mp4');

export function lastPlayed(): { app: LostApp; at: number } | null {
  let times: Record<string, number> = {};
  try { times = JSON.parse(localStorage.getItem('oldjava.lastPlayed') || '{}') || {}; } catch (e) { /* storage blocked */ }
  let best: { app: LostApp; at: number } | null = null;
  LOST_APPS.forEach(app => {
    const at = Number(times[app.id]) || 0;
    if (at > 0 && (!best || at > best.at)) best = { app, at };
  });
  return best;
}

function ago(at: number): string {
  const days = Math.floor((Date.now() - at) / 86400000);
  return days <= 0 ? 'today' : days === 1 ? 'yesterday' : days < 60 ? days + ' days ago' : 'a while ago';
}

/** match: the search's best app, or null (empty box, no match, or only the address's words in its hint); named: a word names it. */
export function Spotlight({ root, match, named, query, recent }:
  { root: string; match: LostApp | null; named: boolean; query: string; recent: { app: LostApp; at: number } | null }) {
  const [live] = useState(moves);
  const want = match || recent?.app || LOST_APPS[0];
  // Follow the typing a moment late, so each key press does not start fetching another clip.
  const [app, setApp] = useState(want);
  useEffect(() => {
    if (want.id === app.id) return;
    const t = setTimeout(() => setApp(want), 250);
    return () => clearTimeout(t);
  }, [want, app]);
  const [playing, setPlaying] = useState(false);
  const [broken, setBroken] = useState<string | null>(null);
  useEffect(() => setPlaying(false), [app]);

  const kicker = match && app.id === match.id
    ? (named ? 'Did you mean' : 'Best match for “' + query + '”')
    : recent && app.id === recent.app.id ? 'Pick up where you left off' : 'Not sure? Start here';
  const note = !match && recent && app.id === recent.app.id ? 'You last played it ' + ago(recent.at) + '.' : null;
  const href = root + app.id + '/index.html';

  return (
    <section className="spot" id="spot" aria-labelledby="spot-title">
      <a className="spot-media" href={href} tabIndex={-1} aria-hidden="true">
        <img src={root + webp(app.shot)} alt="" />
        {live && app.clip && broken !== app.id && (
          <video key={app.id} className={playing ? 'on' : ''} muted loop playsInline autoPlay preload="auto"
                 src={root + 'clips/' + app.id + '.mp4'} onPlaying={() => setPlaying(true)} onError={() => setBroken(app.id)} />
        )}
      </a>
      <div className="spot-body">
        <p className="spot-kicker">{kicker}</p>
        <h2 id="spot-title">{app.name + (kicker === 'Did you mean' ? '?' : '')}</h2>
        <p className="spot-hint">{app.hint}</p>
        {note && <p className="spot-note">{note}</p>}
        <div className="spot-go">
          <a className="spot-play" id="spot-play" href={href}>{'Play ' + app.name}</a>
          {app.then && <a className="spot-then" href={root + app.id + '/then.html'}>Then version</a>}
        </div>
        <small className="short">{'Short link: /' + app.short}</small>
      </div>
    </section>
  );
}
