// The home page: the player, Your arcade and the gallery, drawn from H (model.ts); the controllers in
// src/pages/home/*.ts change H and say what the controls do. The lists here (gallery, rail, stamps, tour stops)
// come from apps.json.
import { useLayoutEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import './home.css';
import { APPS, webp, type App } from '../../shared/catalog';
import { linkState } from '../../shared/link-state';
import { FIRST, H, draw, useHomeDraw } from './model';
import { TourBox, TourStart } from './TourBox';
import { Records } from './Records';
import { HeroWall } from './HeroWall';
import { Mrz, Stamp, stampDate } from './Stamp';
import * as P from './player';
import { TABS, badge, stampCount, tabClick, tabKey, tabShown } from './tabs';
import { allClick, day, openApp } from './news';
import { bannerText, installClick, saveClick } from './takeaway';
import { cardBlur, cardEnter, cardFocus, cardLeave, clipBroken, clipPlaying } from './clips';
import { inFilter, playedRank, useRecordLines } from './yours';

const page = (id: string) => id + '/index.html';
// What's new pictures a change to the whole arcade with the site's icon, at its full address as news.js gave it.
const SITE_ICON = new URL('icons/icon.svg', location.href).href;
const THEN_HINT = 'As first restored in 2026: the recovered code with light bug fixes, before the later reworks.';
// The runtime's Find names its key for the platform (findKey in runtime/find.ts; importing it here would split a chunk
// off boot.js), as its own button does (and is off in screenshots and the saver).
const FIND_KEY = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent)
  && !document.documentElement.hasAttribute('data-shot') && !document.documentElement.hasAttribute('data-saver') ? '⌘K' : 'Ctrl K';

const played = (a: App) => window.oldjavaPassport.has(a.id);

/** The rail repeats the gallery as small buttons, so switching apps never needs a scroll down to the cards. */
function Rail() {
  return (
    <ol id="rail">
      {APPS.map(a => (
        <li key={a.id}>
          <button type="button" data-id={a.id} title={a.hint} className={played(a) ? 'played' : undefined}
            aria-current={H.current ? String(H.current === a) as 'true' | 'false' : undefined} onClick={() => P.choose(a)}>
            <span className="pic"><img src={webp(a.shot, true)} alt="" loading="lazy" /></span>
            <span className="name">{a.name}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

/** The arcade passport: a stamp per app played (the runtime keeps them). */
function Stamps() {
  return (
    <ol className="stamps" id="stamps">
      {APPS.map(a => (
        <li key={a.id}>
          <button type="button" className={[played(a) && 'played', a.id === H.fresh && 'fresh'].filter(Boolean).join(' ') || undefined}
            title={a.name + (!played(a) ? ': not played yet' : window.oldjavaPassport.when(a.id) ? ': first played ' + stampDate(window.oldjavaPassport.when(a.id)) : ': played')}
            onClick={() => P.choose(a)}>
            <span className="seal"><Stamp a={a} when={window.oldjavaPassport.when(a.id)} /></span>
            <span className="label">{a.name}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

/** The player bar's buttons: on a phone a row that fades out at its right edge while there is more to scroll to. */
function Actions({ children }: { children: ReactNode }) {
  const row = useRef<HTMLDivElement>(null);
  const [end, setEnd] = useState(false);
  function edge() {
    const r = row.current!;
    setEnd(r.scrollLeft + r.clientWidth >= r.scrollWidth - 4);
  }
  useLayoutEffect(edge);   // after every drawing, as buttons show and hide
  useLayoutEffect(() => {
    window.addEventListener('resize', edge);
    return () => window.removeEventListener('resize', edge);
  }, []);
  return <div className={'actions' + (end ? ' end' : '')} ref={row} onScroll={edge}>{children}</div>;
}

/** The player's hint: the app's, a Then app's first quirk, or a controller's buttons for a while. */
function Hint() {
  const a = H.played;
  return (
    <span className={'hint' + (H.hintOpen ? ' open' : '')} id="player-hint" onClick={P.hintClick}>
      {H.chart ? (
        <>
          <strong className="shim-news-tag">Controller</strong>
          {' '}
          {H.chart.map((p, i) => [i ? ' · ' : null, <kbd key={i} className="shim-gp-cap">{p[0]}</kbd>, ' ' + p[1]])}
        </>
      ) : (
        <>
          {a ? (H.then ? THEN_HINT : a.hint) : FIRST.hint}
          {/* As first restored: its first known quirk (if its README still listed any), and a link to its page, whose title screen lists them all. */}
          {H.quirk && a && H.quirk.app === a.id && (
            <>
              <span className="quirk">{' Known quirk: '}{H.quirk.items[0].map((part, i) => i % 2 ? <code key={i}>{part}</code> : part)}</span>
              {H.quirk.items.length > 1 && <a href={P.pageOf(a)}>{' +' + (H.quirk.items.length - 1) + ' more'}</a>}
            </>
          )}
        </>
      )}
    </span>
  );
}

function Player() {
  const a = H.played;
  const big = P.isBig();
  const zoom = window.oldjavaZoom.fit();
  const arcade = window.oldjavaSkin.get() === 'arcade';
  const size = H.frame.size;
  return (
    <section className={'player' + (H.then ? ' then' : '') + (H.big ? ' big' : '')} id="player" aria-label="Now playing">
      <div className="player-bar">
        <span className="live">Live</span>
        <h3 id="player-name">{a ? a.name : FIRST.name}</h3>
        <div className="era" id="player-era" role="group" aria-label="Version" hidden={a ? !a.then : false}
          title="Then: the app as first restored in 2026, the recovered code with light bug fixes, before the later reworks. Now: today's version.">
          <button type="button" id="player-then" aria-pressed={H.then} onClick={P.thenClick}>Then</button>
          <button type="button" id="player-now" aria-pressed={!H.then} onClick={P.nowClick}>Now</button>
        </div>
        <Hint />
        <Actions>
          <button className="btn arcade" type="button" id="player-arcade" aria-pressed={arcade} title="Neon CRT arcade look for every app (the Arcade skin)"
            onClick={e => P.arcadeClick(e.currentTarget)}>
            Arcade mode
          </button>
          {' '}
          <button className="btn" type="button" id="player-own-skin" data-skin-maker="" data-sound="none" title="Make your own skin for every page and app: a color, a desk, corners and windows">
            🎨 Your skin
          </button>
          {' '}
          <button className="btn" type="button" id="player-shuffle" title="Play a random app" onClick={P.shuffle}>Shuffle</button>
          {' '}
          <button className="btn" type="button" id="player-pad-toggle" aria-pressed={H.pad.called ? H.pad.shown : true} title="Show or hide the on-screen controller"
            hidden={H.pad.called ? !H.pad.has : true} onClick={P.padToggle}>
            Controls
          </button>
          {' '}
          <button className="btn" type="button" id="player-restart" onClick={P.restart}>Restart</button>
          {' '}
          <button className="btn" type="button" id="player-zoom" aria-pressed={zoom} onClick={e => P.zoomClick(e.currentTarget)}
            title={zoom ? 'Small apps are zoomed to fill the player. Click for their actual size.' : 'Apps are shown at their actual size. Click to zoom small ones to fill the player.'}>Fit</button>
          {' '}
          <button className="btn" type="button" id="player-big" aria-pressed={big} title="Fill the screen with the player" onClick={P.bigClick}>
            {big ? 'Exit big screen' : 'Big screen'}
          </button>
          {' '}
          <button className="btn" type="button" id="player-pop" aria-pressed={H.pop.on} data-sound="none" hidden={!H.pop.shown} onClick={P.popClick}
            title={!H.pop.synced ? undefined : H.pop.on ? 'Bring the app back into the player' : 'Play this app in its own small window that stays on top of your other windows'}>
            {H.pop.on ? 'Bring back' : 'Pop out'}
          </button>
          {' '}
          <button className="btn" type="button" id="player-share" data-sound="none" title="Share this app: its link, with a preview card" onClick={e => P.shareClick(e.currentTarget)}>
            ↗ Share
          </button>
          {' '}
          <button className="btn" type="button" id="player-replay" data-sound="none" hidden={!H.replay} onClick={e => P.replayClick(e.currentTarget)}
            title="Instant replay: the last 10 to 20 seconds of play, as a video to watch, save or share">
            ⟲ Replay
          </button>
          {' '}
          <button className="btn" type="button" id="player-photo" data-sound="none" hidden={!H.photo} aria-label="Take a photo" onClick={e => P.photoClick(e.currentTarget)}
            title="Photo: a picture of the app to save, copy or share, in an instant print or postcard frame">
            📷
          </button>
          {' '}
          <a className="btn" id="player-source" href={(a ? P.pageOf(a) : page(FIRST.id)) + '#source'} title="Open the app's page with its Java source beside it">
            {"</> Source"}
          </a>
          {' '}
          <a className="btn primary" id="player-full" href={a ? P.pageOf(a) + linkState(a.id, H.frame.src).replace('&', '?') : page(FIRST.id)}>Full page</a>
        </Actions>
      </div>
      <TourBox />
      <div className="screen" id="screen" style={H.screenH !== null ? { height: H.screenH + 'px' } : undefined}>
        {/* The app's own controls can be many stops (Minesweeper's tiles): the keyboard's way past them to the rest of the page. */}
        <button type="button" className="btn skip-app" onClick={() => document.getElementById('player-prev')!.focus()}>
          {'Skip past ' + (a ? a.name : FIRST.name) + ' to the app list'}
        </button>
        <iframe key={H.frame.key} id="player-frame" title={H.frame.title} src={H.frame.src} allow="autoplay; clipboard-write; microphone" onLoad={P.frameLoaded}
          style={size ? { width: size.w + 'px', height: size.h + 'px', transform: size.scale < 1 ? 'scale(' + size.scale + ')' : undefined } : undefined} />
      </div>
      {/* The controller, docked under the screen: the runtime draws it in here (oldjavaPad.render). */}
      <div className={'docked' + (H.pad.drawn ? ' shim-pad' : '')} id="player-pad" aria-label={H.pad.drawn ? 'Controller' : undefined} hidden={!H.pad.shown}
        onPointerDown={P.tourTouch} />
      <nav className="rail" aria-label="Switch app">
        <button className="btn step" type="button" id="player-prev" aria-label="Previous app" title="Previous app" onClick={P.prev}>‹</button>
        <Rail />
        <button className="btn step" type="button" id="player-next" aria-label="Next app" title="Next app" onClick={P.next}>›</button>
      </nav>
    </section>
  );
}

/** A tab of Your arcade. */
function Tab({ id, label }: { id: string; label: string }) {
  const on = H.tab === id;
  const b = badge(id);
  // On a narrow page the tabs scroll sideways: bring the chosen one into view (without scrolling the page).
  const tab = useRef<HTMLButtonElement>(null);
  useLayoutEffect(() => {
    if (!on) return;
    const t = tab.current!, strip = t.parentNode as HTMLElement;
    if (t.offsetLeft < strip.scrollLeft) strip.scrollLeft = t.offsetLeft;
    else if (t.offsetLeft + t.offsetWidth > strip.scrollLeft + strip.clientWidth) strip.scrollLeft = t.offsetLeft + t.offsetWidth - strip.clientWidth;
  });
  return (
    <button type="button" role="tab" id={'tab-' + id} aria-controls={id} aria-selected={on} tabIndex={on ? 0 : -1} hidden={!tabShown(id)} ref={tab}
      onClick={() => tabClick(id)} onKeyDown={e => tabKey(e, id)}>
      {label}
      {id !== 'takeaway' && <span className={'badge' + (H.tab !== null && b.isNew ? ' new' : '')} hidden={!b.text}>{b.text}</span>}
    </button>
  );
}

const off = (id: string) => (H.tab !== null && H.tab !== id ? ' off' : '');

function News() {
  const v = H.news.view;
  return (
    <section className={'news' + (v && v.fresh ? ' fresh' : '') + off('news')} id="news" role="tabpanel" aria-labelledby="tab-news" hidden={!v}>
      <div className="news-head">
        <div className="text"><h2 id="news-title">What’s new</h2><p id="news-text" aria-live="polite">{v ? v.text : null}</p></div>
        <div className="actions">
          <button className="btn primary" type="button" id="news-play" hidden={!(v && v.fresh)} onClick={() => { if (v && v.play && v.play.app) openApp(v.play.app); }}>
            {v && v.play ? v.play.label : 'Play it'}
          </button>
          {' '}
          <button className="btn" type="button" id="news-all" aria-expanded={H.news.all} aria-controls="news-list" hidden={!(v && v.more)} onClick={allClick}>
            {v ? v.allLabel : 'Show all'}
          </button>
        </div>
      </div>
      <ol className="news-list" id="news-list">
        {v && v.cards.map((c, k) => (
          <li key={k} className={c.updated ? 'updated' : undefined}>
            <span className="pic"><img src={c.site ? SITE_ICON : webp((c.focus || c.it.apps[0]).shot, true)} alt="" loading="lazy" /></span>
            <div className="body">
              <span className="meta">
                <time>{day(c.it.t)}</time>
                {c.updated ? <span className="tag">Since you played</span>
                  : c.site ? <span className="tag site">{c.it.apps.length > 4 ? c.it.apps.length + ' apps' : 'All apps'}</span> : null}
              </span>
              <strong>{c.it.title}</strong>
              {c.it.more && <span className="more">{c.it.more.charAt(0).toUpperCase() + c.it.more.slice(1)}</span>}
              {!c.site && (
                <span className="plays">
                  {c.it.apps.map(x => (
                    <button key={x.id} className={'btn' + (c.one ? ' primary' : '')} type="button" aria-label={'Play ' + x.name} onClick={() => openApp(x.id)}>
                      {c.one ? 'Play' : x.name}
                    </button>
                  ))}
                </span>
              )}
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}

function Passport() {
  const count = stampCount(), left = APPS.length - count;
  return (
    <section className={'passport' + off('passport')} id="passport" role="tabpanel" aria-labelledby="tab-passport">
      <div className="passport-head">
        <div className="ring" id="passport-ring" style={{ '--p': String(count / APPS.length) } as CSSProperties}><span id="passport-count">{count + '/' + APPS.length}</span></div>
        <div className="text">
          <h2 id="passport-title">Arcade passport</h2>
          <p id="passport-text" aria-live="polite">
            {count === 0 ? 'Play an app to collect its stamp. ' + APPS.length + ' to collect.'
              : left === 0 ? 'Every app played. Arcade complete!' : count + ' of ' + APPS.length + ' apps played. ' + left + ' to go.'}
          </p>
        </div>
        <div className="actions">
          <button className="btn primary" type="button" id="passport-new" hidden={left === 0} onClick={P.passportNew}>Play one you haven't</button>
          {' '}
          <button className="btn" type="button" id="passport-share" data-sound="none" title="Share the arcade, with your stamp count"
            onClick={e => window.oldjavaShare.open(null, e.currentTarget)}>
            ↗ Share
          </button>
          {' '}
          <button className="btn" type="button" id="passport-reset" onClick={P.resetClick}>{H.resetAsk ? 'Clear all stamps?' : 'Reset'}</button>
        </div>
      </div>
      <Stamps />
      <Mrz count={count} />
    </section>
  );
}

function Takeaway() {
  const t = H.take;
  return (
    <section className={'takeaway' + off('takeaway')} id="takeaway" role="tabpanel" aria-labelledby="tab-takeaway" hidden={!t.shown}>
      <div className="takeaway-head">
        <img className="app-icon" src="icons/icon.svg" alt="" width="76" height="76" />
        <div className="text">
          <h2 id="takeaway-title">Take the arcade with you</h2>
          <p id="takeaway-text" aria-live="polite">{t.text}</p>
          <div className="meter" id="takeaway-meter" hidden={!t.meterShown}><span style={t.meterW !== null ? { width: t.meterW + '%' } : undefined} /></div>
        </div>
        <div className="actions">
          <button className="btn primary" type="button" id="takeaway-install" hidden={!t.install} onClick={installClick}>Install app</button>
          {' '}
          <button className="btn" type="button" id="takeaway-save" disabled={t.saving} onClick={saveClick}>{t.saveText}</button>
        </div>
      </div>
      <ul className="perks"><li>Its own window and icon</li><li>Saved apps play with no connection</li><li>No account, no ads</li></ul>
      <p className="note" id="takeaway-note" hidden={!t.note}>To install, use Install app or Add to Home Screen in your browser's menu.</p>
    </section>
  );
}

/**
 * The gallery: one card per app, with its passport badge (New / Played), its Offline chip once the cache has been
 * checked, and a preview clip while it is wanted (clips.ts).
 */
function Gallery() {
  const t = H.take, c = H.clip, news = H.news.view, records = useRecordLines(), rank = H.filter === 'yours' ? playedRank() : null;
  return (
    <ul className="apps" id="apps">
      {/* Yours puts the last played first in the page itself (not with CSS order), so Tab goes in the order shown. */}
      {(rank ? [...APPS].sort((x, y) => (rank[x.id] ?? APPS.length) - (rank[y.id] ?? APPS.length)) : APPS).map(a => {
        const ok = !!t.saved[a.id];
        const cls = [played(a) && 'played', H.current === a && 'playing', news && news.changed[a.id] && 'updated',
          t.checked && t.offline && !ok && 'unavailable', c.current === a.id && c.playing && 'previewing'].filter(Boolean).join(' ');
        return (
          <li key={a.id} data-app={a.id} data-clip={a.clip && !c.broken[a.id] ? '' : undefined} data-then={a.then ? '' : undefined}
              data-origin={a.origin} data-cat={a.cat} data-min-width={a.minWidth} data-min-height={a.minHeight}
              className={cls || undefined} hidden={!inFilter(a, H.filter)}
              onMouseEnter={() => cardEnter(a.id)} onMouseLeave={() => cardLeave(a.id)} onFocus={e => cardFocus(e, a.id)} onBlur={e => cardBlur(e, a.id)}>
            <a className="thumb" href={page(a.id)} onClick={e => P.cardClick(e, a)}>
              <img src={webp(a.shot)} alt={a.alt} loading="lazy" />
              <span className="badge">{played(a) ? 'Played ✓' : 'New'}</span>
              <span className="saved" hidden={!(t.checked && (ok || t.offline))}>{t.checked ? (ok ? 'Offline ✓' : 'Needs a connection') : null}</span>
              {c.wanted[a.id] && (
                <>
                  <video muted loop playsInline preload="none" aria-hidden="true" src={'clips/' + a.id + '.mp4'}
                    onPlaying={() => clipPlaying(a.id)} onError={() => clipBroken(a.id)} />
                  <span className="clip-cue" aria-hidden="true">▶ Preview</span>
                </>
              )}
            </a>
            <div className="body">
              <strong>{a.name}</strong>
              <span>{a.hint}</span>
              {records[a.id] && <p className="record">{records[a.id]}</p>}
              <div className="actions">
                <a className="btn primary play" href={page(a.id)} aria-label={'Play ' + a.name} onClick={e => P.cardClick(e, a)}>Play</a>
                <a className="btn" href={page(a.id)} aria-label={'Full page: ' + a.name}>Full page</a>
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

export function Home() {
  useHomeDraw();
  const t = H.take;
  if (H.filter === 'yours' && !stampCount()) H.filter = 'all';   // the passport was reset, here or on another page
  return (
    <main>
      <HeroWall />
      <h1>Old Java Applications</h1>
      <p className="lead">
        Java Swing and AWT programs recovered from old jars
        <span className="tech">
          , compiled to JavaScript with TeaVM. Each runs on a small AWT/Swing layer that draws the original windows and widgets in the page
        </span>
        . One is already running below: pick any other to swap it in.
      </p>
      <p className="story-link"><a href="story.html">How twenty old programs came back: read the story, with the real code →</a></p>
      <TourStart />
      <p className="offline-banner" id="offline-banner" role="status" hidden={!(t.checked && t.offline)}>{t.checked ? bannerText() : null}</p>
      <Player />
      <section className="arcade" id="arcade" aria-label="Your arcade">
        <div className="arcade-tabs" role="tablist" aria-label="Your arcade">
          <Tab id="news" label="What’s new" />
          {' '}
          <Tab id="passport" label="Passport" />
          {' '}
          <Tab id="records" label="Records" />
          {' '}
          <Tab id="takeaway" label="Take it with you" />
        </div>
        <News />
        <Passport />
        <section className={'records' + off('records')} id="records" role="tabpanel" aria-labelledby="tab-records">
          <Records onCount={n => { if (H.recordsCount !== n) { H.recordsCount = n; draw(false); } }} />
        </section>
        <Takeaway />
      </section>
      <h2>Play in the browser</h2>
      <div className="filters" role="group" aria-label="Filter apps">
        {[['all', 'All'], ['game', 'Games'], ['music', 'Music'], ['toy', 'Toys'], ['tool', 'Tools'], ...(stampCount() ? [['yours', 'Yours']] : [])].map(([f, label], i) => [
          i ? ' ' : null,
          <button key={f} type="button" data-filter={f} aria-pressed={H.filter === f} onClick={() => P.filterClick(f)}>{label}</button>,
        ])}
      </div>
      <button type="button" className="find-bar" data-find="" aria-keyshortcuts="Control+K /">
        <span className="glass" aria-hidden="true">⌕</span>
        <span className="text">Find an app, something to do, or any Java source file</span>
        <kbd>{FIND_KEY}</kbd>
      </button>
      <Gallery />
    </main>
  );
}
