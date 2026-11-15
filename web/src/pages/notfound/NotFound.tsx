// Lost and found: the page shown at an address with nothing at it (404.html loads this from the site root it finds).
// The search over the apps is React state; every link starts from the root, since the page is served at any depth.
import './notfound.css';
import { useState, type FormEvent } from 'react';
import { LOST_APPS, named, score, type LostApp } from './match';

function Card({ root, app, best }: { root: string; app: LostApp; best: boolean }) {
  return (
    <li>
      <a className={'card' + (best ? ' best' : '')} href={root + app.id + '/index.html'}>
        <img src={root + app.shot} alt="" loading="lazy" />
        <div className="body">
          {best && <em className="tag">Closest</em>}
          <strong>{app.name}</strong>
          <span>{app.hint}</span>
          <small className="short">{'/' + app.short}</small>
        </div>
      </a>
    </li>
  );
}

function Lost({ root, missing }: { root: string; missing: string }) {
  const apps = LOST_APPS;
  const [query, setQuery] = useState(() => missing.replace(/\.html?$/i, '').split(/[\/\-_+.%\s]+/).filter(Boolean).join(' '));
  const q = query.trim();
  let ranked = apps.map(a => ({ app: a, s: score(a, q) })).filter(x => x.s > 0)
    .sort((x, y) => y.s - x.s || x.app.order - y.app.order);
  const none = !ranked.length;
  if (none) ranked = apps.map(a => ({ app: a, s: 0 }));
  const count = !q ? 'All ' + apps.length + ' apps'
    : none ? 'No app matches “' + q + '”, so here are all ' + apps.length + '.'
    : ranked.length + (ranked.length === 1 ? ' app matches' : ' apps match') + ' “' + q + '”';

  function go(e: FormEvent) {
    e.preventDefault();
    location.href = root + ranked[0].app.id + '/index.html';
  }

  return (
    <div className="lost" id="lost">
      <section aria-labelledby="lost-title">
        <p className="kicker">404: page not found</p>
        <h1 id="lost-title">Nothing here by that name</h1>
        <p className="lead" id="lead">
          There is no page at <code>{'/' + missing}</code>
          {'. Search the ' + apps.length + ' apps for what you were after, or pick one below.'}
        </p>
        <form className="search" id="search" role="search" onSubmit={go}>
          <input type="search" id="q" aria-label="Search the 20 apps" placeholder="Search the 20 apps" autoComplete="off"
                 spellCheck="false" value={query} onChange={e => setQuery(e.target.value)} />
        </form>
        <p className="count" id="count" aria-live="polite">{count}</p>
        <ul className="cards" id="cards">
          {ranked.map((x, i) => <Card key={x.app.id} root={root} app={x.app} best={i === 0 && !none && named(x.app, q)} />)}
        </ul>
        <div className="shorts">
          <p>Every app has a short link. Type one after the site's address:</p>
          <div className="chips" id="chips">
            {apps.map(a => <a key={a.id} className="chip" href={root + a.short} title={a.name}>{'/' + a.short}</a>)}
          </div>
        </div>
      </section>
      <aside className="panel" id="dino" aria-labelledby="dino-title" hidden={!apps.some(a => a.id === 'DinoGame')}>
        <h2 id="dino-title">While you are here</h2>
        <p>Browsers show a dinosaur when the page will not load. Ours is one of the 20 apps: jump the cacti, or let the one-pixel bot play.</p>
        <iframe id="dino-frame" title="Dino Game" allow="autoplay; fullscreen; clipboard-write" loading="lazy"
                src={root + 'play.html?app=DinoGame'} />
      </aside>
    </div>
  );
}

/** finding: the status line while the page works out where to go; missing: the address part after the root. */
export function NotFound({ root, missing, finding }: { root: string; missing: string; finding: string | null }) {
  return (
    <>
      <header className="top">
        <a className="home" id="home" href={root + 'index.html'}>Old Java Applications</a>
        {' '}
        <a id="story" href={root + 'story.html'}>The story</a>
      </header>
      <main>
        <p className="finding" id="finding" role="status" hidden={!finding}>{finding}</p>
        {!finding && <Lost root={root} missing={missing} />}
      </main>
    </>
  );
}
