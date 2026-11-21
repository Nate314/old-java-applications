// The home page: the player, Your arcade and the gallery, drawn from H (model.ts); the controllers in
// src/pages/home/*.ts change H and say what the controls do. The lists here (gallery, rail, stamps, tour stops)
// come from apps.json.
import { useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import './home.css';
import { APPS, webp, type App } from '../../shared/catalog';
import { linkState } from '../../shared/link-state';
import { FIRST, H, draw, useHomeDraw } from './model';
import { TourBox, TourStart } from './TourBox';
import { ComeBack } from './ComeBack';
import { MoreButton, MoreMenu, useMore } from './PlayerMore';
import { Records } from './Records';
import { HeroWall } from './HeroWall';
import { Passport, played } from './Stamp';
import * as P from './player';
import { badge, stampCount, tabClick, tabKey, tabShown } from './tabs';
import { WhatsNew } from './WhatsNew';
import { bannerText } from './takeaway';
import { TakeItWithYou } from './TakeItWithYou';
import { cardBlur, cardEnter, cardFocus, cardLeave, clipBroken, clipPlaying } from './clips';
import { inFilter, playedRank, useRecordLines } from './yours';

const page = (id: string) => id + '/index.html';
const THEN_HINT = 'As first restored in 2026: the recovered code with light bug fixes, before the later reworks.';
// The runtime's Find names its key for the platform (findKey in runtime/find.ts; importing it here would split a chunk
// off boot.js), as its own button does (and is off in screenshots and the saver).
const FIND_KEY = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent)
  && !document.documentElement.hasAttribute('data-shot') && !document.documentElement.hasAttribute('data-saver') ? '⌘K' : 'Ctrl K';

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
  const [more, setMore, moreAt] = useMore();
  return (
    <section className={'player' + (H.then ? ' then' : '') + (H.big ? ' big' : '')} id="player" aria-label="Now playing">
      <div className="player-bar">
        <span className="live">Live</span>
        <h2 id="player-name">{a ? a.name : FIRST.name}</h2>
        <div className="era" id="player-era" role="group" aria-label="Version" hidden={a ? !a.then : false}
          title="Then: the app as first restored in 2026, the recovered code with light bug fixes, before the later reworks. Now: today's version.">
          <button type="button" id="player-then" aria-pressed={H.then} onClick={P.thenClick}>Then</button>
          <button type="button" id="player-now" aria-pressed={!H.then} onClick={P.nowClick}>Now</button>
        </div>
        <Hint />
        <Actions>
          <button className="btn" type="button" id="player-shuffle" title="Play a random app" onClick={P.shuffle}>Shuffle</button>
          {' '}
          <button className="btn" type="button" id="player-pad-toggle" aria-pressed={H.pad.called ? H.pad.shown : true} title="Show or hide the on-screen controller"
            hidden={H.pad.called ? !H.pad.has : true} onClick={P.padToggle}>
            Controls
          </button>
          {' '}
          <button className="btn" type="button" id="player-restart" title="Start the app again from the beginning" onClick={P.restart}><span aria-hidden="true">↻</span> Restart</button>
          {' '}
          <button className="btn" type="button" id="player-big" aria-pressed={big} title="Fill the screen with the player" onClick={P.bigClick}>
            <span aria-hidden="true">⛶</span> {big ? 'Exit big screen' : 'Big screen'}
          </button>
          {' '}
          <button className="btn" type="button" id="player-share" data-sound="none" title="Share this app: its link, with a preview card" onClick={e => P.shareClick(e.currentTarget)}>
            ↗ Share
          </button>
          {' '}
          <button className="btn" type="button" id="player-replay" data-sound="none" hidden={!H.replay} aria-label="Instant replay" onClick={e => P.replayClick(e.currentTarget)}
            title="Instant replay: the last 10 to 20 seconds of play, as a video to watch, save or share">
            <span className="shim-replay-dot" aria-hidden="true" /> Replay
          </button>
          {' '}
          <button className="btn" type="button" id="player-photo" data-sound="none" hidden={!H.photo} aria-label="Take a photo" onClick={e => P.photoClick(e.currentTarget)}
            title="Photo: a picture of the app to save, copy or share, in an instant print or postcard frame">
            <span aria-hidden="true">📷</span> Photo
          </button>
          {' '}
          <a className="btn primary" id="player-full" href={a ? P.pageOf(a) + linkState(a.id, H.frame.src).replace('&', '?') : page(FIRST.id)}>Full page</a>
          {' '}
          <MoreButton open={more} setOpen={setMore} />
        </Actions>
        <MoreMenu open={more} setOpen={setMore} at={moreAt} zoom={zoom} arcade={arcade} source={a ? P.pageOf(a) : page(FIRST.id)} />
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
  // A first visit (no stamps when the page loaded) shows the 20 apps before Your arcade, whose passport and records
  // are still empty: on a phone that put the apps about two screens further down. Kept for the visit, so a first
  // stamp does not move the panel above the gallery while the visitor scrolls.
  const [newcomer] = useState(() => !stampCount());
  const arcade = (
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
      <WhatsNew off={off('news')} />
      <Passport off={off('passport')} />
      <section className={'records' + off('records')} id="records" role="tabpanel" aria-labelledby="tab-records">
        <Records onCount={n => { if (H.recordsCount !== n) { H.recordsCount = n; draw(false); } }} />
      </section>
      <TakeItWithYou off={off('takeaway')} />
    </section>
  );
  // The page's <main> and its title and lead are in the HTML (src/routes.tsx), painted before this runs.
  return (
    <>
      <HeroWall />
      <ComeBack />
      {/* What to do first, in one row: the tour, and the story for those who want to know how. */}
      <div className="start">
        <TourStart />
        <p className="story-link"><a href="story.html">How they came back: the story →</a></p>
      </div>
      <p className="offline-banner" id="offline-banner" role="status" hidden={!(t.checked && t.offline)}>{t.checked ? bannerText() : null}</p>
      <Player />
      {!newcomer && arcade}
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
      {newcomer && arcade}
    </>
  );
}
