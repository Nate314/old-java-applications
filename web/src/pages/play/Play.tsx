// The embeddable player (play.html?app=<App>): a poster first, the app only after Play. run.ts runs it.
import './play.css';
export function Play() {
  return (
    <>
      <div className="main">
        <div className="poster" id="poster" hidden>
          <img className="bg" id="poster-bg" alt="" />
          {' '}
          <img className="shot" id="poster-shot" alt="" />
          <div className="text">
            <p className="kicker" id="poster-kicker">Play in your browser</p>
            <h1 id="poster-name" />
            <p className="hint" id="poster-hint" />
            <button className="play" type="button" id="poster-play">Play</button>
            <p className="note">Runs right here, nothing to install.</p>
          </div>
        </div>
        <div className="stage" id="stage" hidden>
          <iframe id="frame" title="App" allow="autoplay; fullscreen; clipboard-write; microphone" />
        </div>
        <div className="oops" id="oops" hidden>
          <p id="oops-text" />
          <p><a href="index.html" target="_blank" rel="noopener">See all 20 apps</a></p>
        </div>
      </div>
      <nav className="bar" aria-label="Old Java Applications">
        <a className="brand" href="index.html" target="_blank" rel="noopener" title="Old Java Applications: 20 recovered Java apps you can play in the browser">
          <img src="icons/icon.svg" alt="" />
          <span>Old Java Applications</span>
        </a>
        {' '}
        <span className="now" id="now" />
        {' '}
        <span className="go">
          <button type="button" id="fullscreen" title="Fill the screen" aria-label="Fill the screen" hidden>⛶</button>
          {' '}
          <a id="full" href="index.html" target="_blank" rel="noopener" title="Open this app on its own page, with its source and more">
            Full page ↗
          </a>
          {' '}
          <a className="more" href="index.html" target="_blank" rel="noopener" title="20 old Java apps, all playable in the browser">
            All 20 apps ↗
          </a>
        </span>
      </nav>
    </>
  );
}
