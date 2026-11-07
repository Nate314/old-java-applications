// The home page: the player, Your arcade and the gallery. src/pages/home/*.ts give it its behavior, as the page's
// scripts did when it was static HTML; the lists here (gallery, rail, stamps, tour stops) come from apps.json.
import './home.css';
import { APPS, app } from '../../shared/catalog';
import { TOUR } from './tour';
import { Records } from './Records';

// The app the player opens on when the address names none, with the computer playing.
const FIRST = app('Minesweeper');
const page = (id: string) => id + '/index.html';

/** The rail repeats the gallery as small buttons, so switching apps never needs a scroll down to the cards. */
function Rail() {
  return (
    <ol id="rail">
      {APPS.map(a => (
        <li key={a.id}>
          <button type="button" title={a.hint}>
            <span className="pic"><img src={a.shot} alt="" loading="lazy" /></span>
            <span className="name">{a.name}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

/** The arcade passport: a stamp per app played (boot.js keeps them; passport.ts marks them). */
function Stamps() {
  return (
    <ol className="stamps" id="stamps">
      {APPS.map(a => (
        <li key={a.id}>
          <button type="button">
            <span className="seal"><img src={a.shot} alt="" loading="lazy" /></span>
            <span className="label">{a.name}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

/** One bar per tour stop; Back and Next stop are the keyboard's way along, these are for pointing. */
function TourSteps() {
  return (
    <ol className="tour-steps" id="tour-steps">
      {TOUR.map(([id], i) => {
        const label = 'Stop ' + (i + 1) + ': ' + app(id).name;
        return <li key={id}><button type="button" tabIndex={-1} title={label} aria-label={label} /></li>;
      })}
    </ol>
  );
}

/**
 * The gallery: one card per app. The badge (New / Played) and the Offline chip start empty and hidden; the
 * passport and the offline panel fill them in. Preview clips are added on demand (clips.ts).
 */
function Gallery() {
  return (
    <ul className="apps" id="apps">
      {APPS.map(a => (
        <li key={a.id} data-app={a.id} data-clip={a.clip ? '' : undefined} data-then={a.then ? '' : undefined}
            data-origin={a.origin} data-cat={a.cat} data-min-width={a.minWidth} data-min-height={a.minHeight}>
          <a className="thumb" href={page(a.id)}>
            <img src={a.shot} alt={a.alt} loading="lazy" />
            <span className="badge" />
            <span className="saved" hidden />
          </a>
          <div className="body">
            <strong>{a.name}</strong>
            <span>{a.hint}</span>
            <div className="actions">
              <a className="btn primary play" href={page(a.id)}>Play</a>
              <a className="btn" href={page(a.id)}>Full page</a>
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}

export function Home() {
  return (
    <main>
      <h1>Old Java Applications</h1>
      <p className="lead">
        Java Swing and AWT programs recovered from old jars
        <span className="tech">
          , compiled to JavaScript with TeaVM. Each runs on a small AWT/Swing layer that draws the original windows and widgets in the page
        </span>
        . One is already running below: pick any other to swap it in.
      </p>
      <p className="story-link"><a href="story.html">How twenty old programs came back: read the story, with the real code →</a></p>
      <p className="tour-start">
        <button className="btn primary" type="button" id="tour-start">▶ Take the tour</button>
        {' '}
        <span id="tour-start-note">All 20 apps in the player, one at a time, with something to try in each.</span>
      </p>
      <p className="offline-banner" id="offline-banner" role="status" hidden />
      <section className="player" id="player" aria-label="Now playing">
        <div className="player-bar">
          <span className="live">Live</span>
          <h3 id="player-name">{FIRST.name}</h3>
          <div className="era" id="player-era" role="group" aria-label="Version" title="Then: the app as first restored in 2026, the recovered code with light bug fixes, before the later reworks. Now: today's version.">
            <button type="button" id="player-then" aria-pressed="false">Then</button>
            <button type="button" id="player-now" aria-pressed="true">Now</button>
          </div>
          <span className="hint" id="player-hint">{FIRST.hint}</span>
          <div className="actions">
            <button className="btn arcade" type="button" id="player-arcade" aria-pressed="false" title="Neon CRT arcade look for every app (the Arcade skin)">
              Arcade mode
            </button>
            {' '}
            <button className="btn" type="button" id="player-own-skin" data-skin-maker="" data-sound="none" title="Make your own skin for every page and app: a color, a desk, corners and windows">
              🎨 Your skin
            </button>
            {' '}
            <button className="btn" type="button" id="player-shuffle" title="Play a random app">Shuffle</button>
            {' '}
            <button className="btn" type="button" id="player-pad-toggle" aria-pressed="true" title="Show or hide the on-screen controller" hidden>
              Controls
            </button>
            {' '}
            <button className="btn" type="button" id="player-restart">Restart</button>
            {' '}
            <button className="btn" type="button" id="player-zoom" aria-pressed="true">Fit</button>
            {' '}
            <button className="btn" type="button" id="player-big" aria-pressed="false" title="Fill the screen with the player">
              Big screen
            </button>
            {' '}
            <button className="btn" type="button" id="player-pop" aria-pressed="false" data-sound="none" hidden>Pop out</button>
            {' '}
            <button className="btn" type="button" id="player-share" data-sound="none" title="Share this app: its link, with a preview card">
              ↗ Share
            </button>
            {' '}
            <button className="btn" type="button" id="player-replay" data-sound="none" hidden title="Instant replay: the last 10 to 20 seconds of play, as a video to watch, save or share">
              ⟲ Replay
            </button>
            {' '}
            <button className="btn" type="button" id="player-photo" data-sound="none" hidden aria-label="Take a photo" title="Photo: a picture of the app to save, copy or share, in an instant print or postcard frame">
              📷
            </button>
            {' '}
            <a className="btn" id="player-source" href={FIRST.id + "/index.html#source"} title="Open the app's page with its Java source beside it">
              {"</> Source"}
            </a>
            {' '}
            <a className="btn primary" id="player-full" href={FIRST.id + "/index.html"}>Full page</a>
          </div>
        </div>
        <div className="tour" id="tour" role="region" aria-label="Tour of the arcade" hidden>
          <TourSteps />
          <p className="tour-line"><span className="tour-count" id="tour-count" />{' '}<span id="tour-try" aria-live="polite" /></p>
          <div className="tour-actions">
            <button className="btn" type="button" id="tour-back" aria-label="Previous stop">‹ Back</button>
            {' '}
            <button className="btn primary" type="button" id="tour-next">Next stop ›</button>
            {' '}
            <button className="btn" type="button" id="tour-unplayed" hidden>Play one you haven't</button>
            {' '}
            <a className="btn" id="tour-story" href="story.html" hidden>Read the story</a>
            {' '}
            <button className="btn" type="button" id="tour-end">End tour</button>
          </div>
          <div className="tour-time" id="tour-time" aria-hidden="true"><span /></div>
        </div>
        <div className="screen" id="screen">
          <iframe id="player-frame" title={FIRST.name} src={FIRST.id + "/index.html?embed&watch"} allow="autoplay; clipboard-write; microphone" />
        </div>
        <div className="docked" id="player-pad" hidden />
        <nav className="rail" aria-label="Switch app">
          <button className="btn step" type="button" id="player-prev" aria-label="Previous app" title="Previous app">‹</button>
          <Rail />
          <button className="btn step" type="button" id="player-next" aria-label="Next app" title="Next app">›</button>
        </nav>
      </section>
      <section className="arcade" id="arcade" aria-label="Your arcade">
        <div className="arcade-tabs" role="tablist" aria-label="Your arcade">
          <button type="button" role="tab" id="tab-news" aria-controls="news" aria-selected="false" tabIndex={-1} hidden>
            What’s new
            <span className="badge" hidden />
          </button>
          {' '}
          <button type="button" role="tab" id="tab-passport" aria-controls="passport" aria-selected="false" tabIndex={-1}>
            Passport
            <span className="badge" hidden />
          </button>
          {' '}
          <button type="button" role="tab" id="tab-records" aria-controls="records" aria-selected="false" tabIndex={-1}>
            Records
            <span className="badge" hidden />
          </button>
          {' '}
          <button type="button" role="tab" id="tab-takeaway" aria-controls="takeaway" aria-selected="false" tabIndex={-1} hidden>
            Take it with you
          </button>
        </div>
        <section className="news" id="news" role="tabpanel" aria-labelledby="tab-news" hidden>
          <div className="news-head">
            <div className="text"><h2 id="news-title">What’s new</h2><p id="news-text" aria-live="polite" /></div>
            <div className="actions">
              <button className="btn primary" type="button" id="news-play" hidden>Play it</button>
              {' '}
              <button className="btn" type="button" id="news-all" aria-expanded="false" aria-controls="news-list" hidden>Show all</button>
            </div>
          </div>
          <ol className="news-list" id="news-list" />
        </section>
        <section className="passport" id="passport" role="tabpanel" aria-labelledby="tab-passport">
          <div className="passport-head">
            <div className="ring" id="passport-ring"><span id="passport-count">0/20</span></div>
            <div className="text">
              <h2 id="passport-title">Arcade passport</h2>
              <p id="passport-text" aria-live="polite">Play an app to collect its stamp.</p>
            </div>
            <div className="actions">
              <button className="btn primary" type="button" id="passport-new">Play one you haven't</button>
              {' '}
              <button className="btn" type="button" id="passport-share" data-sound="none" title="Share the arcade, with your stamp count">
                ↗ Share
              </button>
              {' '}
              <button className="btn" type="button" id="passport-reset">Reset</button>
            </div>
          </div>
          <Stamps />
        </section>
        <section className="records" id="records" role="tabpanel" aria-labelledby="tab-records">
          <Records />
        </section>
        <section className="takeaway" id="takeaway" role="tabpanel" aria-labelledby="tab-takeaway" hidden>
          <div className="takeaway-head">
            <img className="app-icon" src="icons/icon.svg" alt="" width="76" height="76" />
            <div className="text">
              <h2 id="takeaway-title">Take the arcade with you</h2>
              <p id="takeaway-text" aria-live="polite">Install it as an app, and save all 20 to play with no connection.</p>
              <div className="meter" id="takeaway-meter" hidden><span /></div>
            </div>
            <div className="actions">
              <button className="btn primary" type="button" id="takeaway-install" hidden>Install app</button>
              {' '}
              <button className="btn" type="button" id="takeaway-save">Save all for offline</button>
            </div>
          </div>
          <ul className="perks"><li>Its own window and icon</li><li>Saved apps play with no connection</li><li>No account, no ads</li></ul>
          <p className="note" id="takeaway-note" hidden>To install, use Install app or Add to Home Screen in your browser's menu.</p>
        </section>
      </section>
      <h2>Play in the browser</h2>
      <div className="filters" role="group" aria-label="Filter apps">
        <button type="button" data-filter="all" aria-pressed="true">All</button>
        {' '}
        <button type="button" data-filter="game" aria-pressed="false">Games</button>
        {' '}
        <button type="button" data-filter="music" aria-pressed="false">Music</button>
        {' '}
        <button type="button" data-filter="toy" aria-pressed="false">Toys</button>
        {' '}
        <button type="button" data-filter="tool" aria-pressed="false">Tools</button>
      </div>
      <button type="button" className="find-bar" data-find="" aria-keyshortcuts="Control+K /">
        <span className="glass" aria-hidden="true">⌕</span>
        <span className="text">Find an app, something to do, or any Java source file</span>
        <kbd>Ctrl K</kbd>
      </button>
      <Gallery />
    </main>
  );
}
