// Then and now (compare.html?app=<App>): an app as first restored and as it is today, both live side by side in
// the embeddable player (play.html, one with &then), and every change to the app in between, read from history.txt.
// From 760 px wide the two players lie one over the other, full width, split by a line to drag (Wipe below): Then
// left of it, Now right of it, both started by one Play.
import { useEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type PointerEvent } from 'react';
import { webp } from '../../shared/catalog';
import { SITE } from '../../shared/ShareTags';
import { WITH_THEN, pick } from './Intro';

// The Then versions are built from commit 5a28e46 (then.commit in web/then/pom.xml); this is its commit time, so the
// changes listed are the ones made after it. history.txt carries no hashes, so change this together with then.commit
// (web/then/pom.xml and web/then-nathanos/pom.xml).
const THEN_AT = 1793558285 * 1000;
const app = pick(new URLSearchParams(location.search).get('app'));

interface Change { t: number; title: string; more: string }

/** The app's changes after the Then commit, newest first, from history.txt ("@@<time> <subject>", then its folders). */
function changesOf(history: string, id: string) {
  const out: Change[] = [];
  let cur: (Change & { mine: boolean }) | null = null;
  history.split(/\r?\n/).forEach(line => {
    const m = /^@@(\d+) (.*)$/.exec(line);
    if (m) {
      const subject = m[2].trim(), colon = subject.indexOf(': ');
      cur = { t: +m[1] * 1000, title: colon < 0 ? subject : subject.slice(0, colon), more: colon < 0 ? '' : subject.slice(colon + 2), mine: false };
      out.push(cur);
    } else if (cur && line.trim() === id) cur.mine = true;
  });
  return (out as (Change & { mine: boolean })[]).filter(c => c.mine && c.t > THEN_AT);
}

const day = (t: number) => new Date(t).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });

function Pane({ then, shown, covered }: { then: boolean; shown: boolean; covered: boolean }) {
  const label = then ? 'Then' : 'Now';
  return (
    <figure className={'pane' + (then ? ' then' : '') + (shown ? ' shown' : '')} id={then ? 'pane-then' : 'pane-now'}>
      <figcaption>
        <strong>{label}</strong>
        <span>{then ? 'As first restored in 2026, before the reworks' : 'Today, after every change below'}</span>
      </figcaption>
      <iframe title={app.name + ', ' + label.toLowerCase()} allow="autoplay; fullscreen; clipboard-write"
        src={'play.html?app=' + encodeURIComponent(app.id) + (then ? '&then' : '')} />
      {/* Wide only: the app's picture over the poster until Play both (Then's in old-photo sepia). */}
      {covered && <img className="cover" src={webp(app.shot)} alt="" />}
    </figure>
  );
}

/** Presses Play on a frame's poster (play.html), once it has been drawn; a frame already playing is left alone. */
function press(f: HTMLIFrameElement) {
  const d = f.contentDocument, poster = d && d.getElementById('poster');
  if (poster) { if (!poster.hidden) d!.getElementById('poster-play')!.click(); }
  else f.addEventListener('load', () => press(f), { once: true });
}

const KEY_STEP: Record<string, number> = { ArrowLeft: -5, ArrowDown: -5, ArrowRight: 5, ArrowUp: 5, PageDown: -25, PageUp: 25, Home: -100, End: 100 };

/** The line between Then (left) and Now (right): dragged, or moved with the arrow keys, as a slider. */
function Wipe({ cut, setCut, box }: { cut: number; setCut: (n: number) => void; box: () => HTMLElement }) {
  function drag(e: PointerEvent<HTMLDivElement>) {
    const r = box().getBoundingClientRect();
    setCut(Math.max(0, Math.min(100, ((e.clientX - r.left) / r.width) * 100)));
  }
  function key(e: KeyboardEvent<HTMLDivElement>) {
    if (!(e.key in KEY_STEP)) return;
    e.preventDefault();
    setCut(Math.max(0, Math.min(100, cut + KEY_STEP[e.key])));
  }
  return (
    <div className="wipe" role="slider" tabIndex={0} aria-label="Line between Then and Now" aria-orientation="horizontal"
      aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(cut)}
      aria-valuetext={cut <= 0 ? 'All Now' : cut >= 100 ? 'All Then' : Math.round(cut) + '% Then, ' + Math.round(100 - cut) + '% Now'}
      onPointerDown={e => { e.currentTarget.setPointerCapture(e.pointerId); box().classList.add('dragging'); drag(e); }}
      onPointerMove={e => { if (e.currentTarget.hasPointerCapture(e.pointerId)) drag(e); }}
      onPointerUp={() => box().classList.remove('dragging')} onPointerCancel={() => box().classList.remove('dragging')}
      onKeyDown={key}>
      <span className="knob" aria-hidden="true">‹ ›</span>
    </div>
  );
}

export function Compare() {
  const [changes, setChanges] = useState<Change[] | null>(null);
  // On a narrow screen one pane at a time, full size, flipped with the switch (both stay loaded, each keeps its game).
  const [then, setThen] = useState(true);
  const [cut, setCut] = useState(50);              // wide: how much of the width is Then, in percent
  const [started, setStarted] = useState(false);   // wide: Play both pressed
  const panes = useRef<HTMLDivElement>(null);
  function playBoth() {
    setStarted(true);
    panes.current!.querySelectorAll('iframe').forEach(press);
  }
  useEffect(() => {
    document.title = app.name + ', then and now | ' + SITE;
    fetch('history.txt').then(r => { if (!r.ok) throw new Error('history.txt: ' + r.status); return r.text(); })
      .then(h => setChanges(changesOf(h, app.id)))
      .catch(e => { console.warn('Then and now: ' + e.message); setChanges([]); });
  }, []);
  return (
    <>
      <div className="panes" ref={panes} style={{ '--cut': cut + '%' } as CSSProperties}>
        <div className="flip" role="group" aria-label="Show">
          <button type="button" className="then" aria-pressed={then} aria-controls="pane-then" onClick={() => setThen(true)}>
            Then <small>2026 restore</small>
          </button>
          <button type="button" aria-pressed={!then} aria-controls="pane-now" onClick={() => setThen(false)}>
            Now <small>{changes && changes.length ? '+' + changes.length + (changes.length === 1 ? ' change' : ' changes') : 'today'}</small>
          </button>
        </div>
        <Pane then shown={then} covered={!started} />
        <Pane then={false} shown={!then} covered={!started} />
        <Wipe cut={cut} setCut={setCut} box={() => panes.current!} />
        {!started && (
          <div className="play-both">
            <button type="button" onClick={playBoth}><span aria-hidden="true">▶</span> Play both</button>
            <p>Then and Now start together. Drag the line to compare.</p>
          </div>
        )}
      </div>
      <nav className="picks" aria-label="Compare another app">
        <span>Compare another:</span>
        {WITH_THEN.map(a => (
          <a key={a.id} href={'compare.html?app=' + a.id} aria-current={a === app ? 'page' : undefined}>{a.name}</a>
        ))}
      </nav>
      <section className="changes" aria-labelledby="changes-title" hidden={!changes || !changes.length}>
        <h2 id="changes-title">What changed in between</h2>
        <p className="count">
          {changes && changes.length + (changes.length === 1 ? ' change' : ' changes') + ' to ' + app.name + ' since it was first restored, newest first.'}
        </p>
        <ol>
          {changes && changes.map((c, i) => (
            <li key={i}>
              <time dateTime={new Date(c.t).toISOString()}>{day(c.t)}</time>
              {/* "Minesweeper: Odds shows ..." reads as "Odds shows ..." here, under the app's own name. */}
              {c.title === app.id && c.more
                ? <span><strong>{c.more.charAt(0).toUpperCase() + c.more.slice(1)}</strong></span>
                : <span><strong>{c.title}</strong>{c.more ? ': ' + c.more : null}</span>}
            </li>
          ))}
        </ol>
        <p className="pages">
          <a href={app.id + '/then.html'}>Open the Then page</a>
          {' · '}
          <a href={app.id + '/index.html'}>Open today’s page</a>
        </p>
      </section>
    </>
  );
}
