// Welcome back: a visitor who has played here before is offered, at the top of the page, the app they played last
// (with its record line, from its saves) and the next app they have not played, each one tap from playing in the
// player. Without it a returning visitor saw the same page as a first visit and scrolled to the gallery for either.
import { useState } from 'react';
import { APPS, webp, type App } from '../../shared/catalog';
import { PLAYED, readJson } from './saves';
import * as P from './player';
import { stampCount } from './tabs';
import { useRecordLines } from './yours';
import './come-back.css';

/** "today", "yesterday", "3 days ago". */
export function ago(t: number, now = Date.now()) {
  const days = Math.round((new Date(now).setHours(0, 0, 0, 0) - new Date(t).setHours(0, 0, 0, 0)) / 864e5);
  return days <= 0 ? 'today' : days === 1 ? 'yesterday' : days + ' days ago';
}

/** The app played last (oldjava.lastPlayed, written by the runtime's passport; else the newest stamp) and when. */
function lastPlayed(): { app: App; at: number } | null {
  const times = { ...readJson(PLAYED), ...readJson('oldjava.lastPlayed') };
  const id = Object.keys(times).filter(k => APPS.some(a => a.id === k)).sort((a, b) => +times[b] - +times[a])[0];
  return id ? { app: APPS.find(a => a.id === id)!, at: +times[id] } : null;
}

function Pick({ app, label, note, line }: { app: App; label: string; note: string; line?: string }) {
  return (
    <button type="button" className="come-back-pick" onClick={() => P.choose(app)}>
      <img src={webp(app.shot, true)} alt="" width="96" height="60" />
      <span className="come-back-text">
        <strong>{label}</strong>
        <span>{note}</span>
        {line && <span className="come-back-line">{line}</span>}
      </span>
    </button>
  );
}

export function ComeBack() {
  // Chosen once, as the page opens, so the card does not change under the visitor while they play.
  const [seen] = useState(() => {
    const last = stampCount() ? lastPlayed() : null;
    if (!last) return null;
    // The next new one: of the same kind as the last first (as the exit screen's picks), else in gallery order.
    const fresh = APPS.filter(a => !window.oldjavaPassport.has(a.id));
    return { last, next: fresh.find(a => a.cat === last.app.cat) || fresh[0], count: stampCount() };
  });
  const lines = useRecordLines();
  if (!seen) return null;
  const { last, next, count } = seen;
  return (
    <section className="come-back" aria-labelledby="come-back-h">
      <h2 id="come-back-h">Welcome back</h2>
      <div className="come-back-picks">
        <Pick app={last.app} label={'▶ ' + last.app.name + ' again'} note={'Last played ' + ago(last.at) + '.'} line={lines[last.app.id]} />
        {next && (
          <Pick app={next} label={'▶ Try ' + next.name} note={'New: stamp ' + (count + 1) + ' of ' + APPS.length + ' in your passport.'} />
        )}
      </div>
    </section>
  );
}
