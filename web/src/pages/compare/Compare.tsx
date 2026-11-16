// Then and now (compare.html?app=<App>): an app as first restored and as it is today, both live side by side in
// the embeddable player (play.html, one with &then), and every change to the app in between, read from history.txt.
import { useEffect, useState } from 'react';
import { APPS } from '../../shared/catalog';
import { SITE } from '../../shared/ShareTags';

// The Then versions are built from commit 5a28e46 (then.commit in web/then/pom.xml); this is its commit time, so the
// changes listed are the ones made after it. history.txt carries no hashes, so change this together with then.commit
// (web/then/pom.xml and web/then-nathanos/pom.xml).
const THEN_AT = 1793558285 * 1000;
const WITH_THEN = APPS.filter(a => a.then);
const asked = new URLSearchParams(location.search).get('app');
const app = WITH_THEN.find(a => a.id === asked) || WITH_THEN.find(a => a.id === 'FlappyBird') || WITH_THEN[0];

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

function Pane({ then }: { then: boolean }) {
  const label = then ? 'Then' : 'Now';
  return (
    <figure className={'pane' + (then ? ' then' : '')}>
      <figcaption>
        <strong>{label}</strong>
        <span>{then ? 'As first restored in 2026, before the reworks' : 'Today, after every change below'}</span>
      </figcaption>
      <iframe title={app.name + ', ' + label.toLowerCase()} allow="autoplay; fullscreen; clipboard-write"
        src={'play.html?app=' + encodeURIComponent(app.id) + (then ? '&then' : '')} />
    </figure>
  );
}

export function Compare() {
  const [changes, setChanges] = useState<Change[] | null>(null);
  useEffect(() => {
    document.title = app.name + ', then and now | ' + SITE;
    fetch('history.txt').then(r => { if (!r.ok) throw new Error('history.txt: ' + r.status); return r.text(); })
      .then(h => setChanges(changesOf(h, app.id)))
      .catch(e => { console.warn('Then and now: ' + e.message); setChanges([]); });
  }, []);
  return (
    <>
      <header className="compare-top">
        <a className="home" href="index.html">{SITE}</a>
        {' '}
        <a href="story.html">The story</a>
      </header>
      <main className="compare">
        <p className="kicker">Then and now</p>
        <h1>{app.name}, then and now</h1>
        <p className="lead">
          {'On the left, ' + app.name + ' as it ran when it was first brought back: the ' +
            (app.origin === 'original' ? 'author’s own source' : 'code recovered from the old jar') +
            ' with light bug fixes. On the right, the same app today. Play both, then click the one you want the keys.'}
        </p>
        <div className="panes">
          <Pane then />
          <Pane then={false} />
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
      </main>
    </>
  );
}
