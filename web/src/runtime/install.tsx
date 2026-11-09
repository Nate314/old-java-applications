import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import { flushSync } from 'react-dom';
import { SKINS, setSkin } from './skin';
import { bandNews, news, setNews, setZoomFit, stageEdge, zoomFit } from './band-news';
import { popOut, poppable, popped } from './pop-out';
import { replayNow } from './replay';
import { snapPhoto, takePhoto } from './photo-booth';
import { pad, padOnPage, padShown, togglePad } from './touch-pad';
import { appId, catalog, root } from './catalog';
import { EraSwitch, indexOf, openDrawer, pageOf } from './then-and-now';
import { openShare } from './share';
import { howToPlay } from './source-viewer';
import { setSoundOn, soundOn } from './sounds';
import { findMac, findOff, openFind } from './find';
import { docked, openDockPicker } from './side-by-side';
import { openMaker } from './skin-maker';
import { More } from './toolbar-more';
import { openLayer } from './ui/layer';

export let siteRoot: string;
let theme: HTMLMetaElement;

function headLink(rel: string, href: string, type?: string) {
  var link = document.createElement('link');
  link.rel = rel;
  link.href = siteRoot + href;
  if (type) link.type = type;
  document.head.appendChild(link);
}

// The band at the top of every page but an embedded one: on an app page the app's name (where the band's news
// line shows), its controls and a link to the next app; on every page Find, Sound, the skin picker and Edit (your
// own skin), and the Clock chip while one of Clock's times runs (lazy/clockbar.tsx says what it shows). At its end,
// More (toolbar-more.tsx) hides what does not fit, by a class on the controls.
let redraw: (() => void) | null = null;
/** Draws the band's toggles again, so Sound, Fit and Pop out say what they are doing now. */
export function redrawBar() { if (redraw) redraw(); }

/** The Clock chip: a ring and the time for each of Clock's times running (part: how much of it is left). */
export interface ClockChip { segs: { kind: string; label: string; text: string; part: number }[]; soon: boolean; label: string }
let chip: ClockChip | null = null;
export function setClockChip(c: ClockChip | null) {
  chip = c;
  redrawBar();
}

export function toolbar(isApp: boolean) {
  var parts!: BarParts;
  openLayer(<Toolbar isApp={isApp} onMount={p => { parts = p; }} />);
  if (!isApp) return;
  setNews(bandNews(parts.slot));
  news!.onKind(function (kind: string | null) { parts.help(kind === 'hint'); });
  stageEdge(parts.bar, '--bar-h');
  if (pad.has(appId()!)) padOnPage();
}

interface BarParts { bar: HTMLDivElement; slot: HTMLDivElement; help: (on: boolean) => void }

function Toolbar({ isApp, onMount }: { isApp: boolean; onMount: (parts: BarParts) => void }) {
  const bar = useRef<HTMLDivElement>(null);
  const slot = useRef<HTMLDivElement>(null);
  const [, setTick] = useState(0);
  const [hint, setHint] = useState(false);
  const [next, setNext] = useState<OldjavaCatalogApp | null>(null);
  useLayoutEffect(() => {
    redraw = () => flushSync(() => setTick(n => n + 1));
    onMount({ bar: bar.current!, slot: slot.current!, help: on => flushSync(() => setHint(on)) });
    if (isApp) catalog().then(apps => {
      const i = indexOf(apps, appId());
      if (i >= 0) setNext(apps[(i + 1) % apps.length]);
    });
  }, []);
  const blur = (e: { currentTarget: HTMLElement }) => e.currentTarget.blur();
  const on = soundOn(), fit = zoomFit(), s = window.__shimScale || 1, beside = docked(), key = findMac ? '⌘K' : 'Ctrl K';
  // data-sound="none" on Find, Side by side and Edit too: what they open plays its own sound
  const find = !findOff && (
    <button type="button" className="shim-find-open" title={'Find an app, something to do or a source file (' + key + ')'}
      aria-keyshortcuts={findMac ? 'Meta+K' : 'Control+K'} data-sound="none" onClick={e => { openFind(e.currentTarget); blur(e); }}>
      <span aria-hidden="true">{'⌕'}</span><span className="shim-find-label"> Find</span><kbd>{key}</kbd>
    </button>
  );
  return (
    <div className="shim-toolbar" ref={bar}>
      {!isApp && find}
      {isApp && (
        <>
          <div className="shim-bar-slot" ref={slot}><strong className="shim-bar-name">{document.title}</strong></div>
          <button type="button" className="shim-help" title="How to play: show the hint in this band" aria-label="How to play" aria-pressed={hint}
            onClick={e => { if (news!.kind() === 'hint') news!.clear(); else howToPlay(); blur(e); }}>?</button>
          {find}
          <EraSwitch className="shim-era" />
          <a href="../index.html">All apps</a>
          <button type="button" className="shim-switch-open" onClick={e => openDrawer(e.currentTarget)}>Switch app</button>
          {!findOff && (
            <button type="button" className="shim-dock-open" aria-pressed={!!beside} aria-label="Side by side" data-sound="none"
              title={beside ? beside + ' is open beside this app. Click to pick another, or close it from its panel.' : 'Side by side: run a second app beside this one'}
              onClick={e => { openDockPicker(e.currentTarget); blur(e); }}>
              <span aria-hidden="true">{'◫'}</span><span className="shim-dock-label"> Side by side</span>
            </button>
          )}
          {/* data-sound="none" on these three: the panel plays its own open sound */}
          <a href="#source" className="shim-source-open" title="Read the Java source of this app while it runs" data-sound="none">
            {'</>'}<span> Source</span>
          </a>
          <button type="button" className="shim-share-open" title="Share this app: its link, with a preview card" data-sound="none"
            onClick={e => openShare(appId(), e.currentTarget)}>{'↗'}<span> Share</span></button>
          {/* Replay shows once the app runs and replay is recording */}
          <button type="button" className="shim-replay" title="Instant replay: the last 10 to 20 seconds of play, as a video to watch, save or share"
            aria-label="Instant replay" data-sound="none" onClick={e => replayNow(e.currentTarget)}>
            <span className="shim-replay-dot" aria-hidden="true" /><span> Replay</span>
          </button>
          {/* Photo shows once the app runs (data-photo); the shutter is its sound */}
          <button type="button" className="shim-photo-open" title="Take a photo of the app: a picture to save, copy or share, in an instant print or postcard frame"
            aria-label="Take a photo" data-sound="none" onClick={e => snapPhoto(takePhoto, document.getElementById('desktop'), e.currentTarget)}>
            <span aria-hidden="true">{'📷'}</span><span> Photo</span>
          </button>
          {pad.has(appId()!) && (
            // blurred after, else Space (FlappyBird's flap) would press it again
            <button type="button" className="shim-controls-open" title="Show or hide the on-screen controller" aria-pressed={padShown()}
              onClick={e => { togglePad(); blur(e); }}>Controls</button>
          )}
          <a className="shim-next" hidden={!next} href={next ? pageOf(next) : undefined}>{next ? 'Next: ' + next.name + ' ›' : ''}</a>
        </>
      )}
      {isApp && !document.body.hasAttribute('data-own-layout') && (
        <button type="button" className="shim-zoom" aria-pressed={fit}
          aria-label={fit ? 'Fit to screen' + (s > 1 ? ', zoomed ' + s + ' times' : '') : 'Actual size'}
          title={fit ? 'The app is zoomed to fit the screen' + (s > 1 ? ' (' + s + '×)' : '') + '. Click for its actual size.'
            : 'The app is shown at its actual size. Click to zoom it to fit the screen.'}
          onClick={e => { setZoomFit(!zoomFit()); blur(e); }}>
          {'⤢ '}<span>{fit ? 'Fit' : 'Actual size'}</span>{fit && s > 1 && <>{' '}<b>{s + '×'}</b></>}
        </button>
      )}
      {/* only a top-level page may open the window; popOut and bringBack play their own sounds */}
      {isApp && poppable() && window.top === window && (
        <button type="button" className="shim-pop" data-sound="none" aria-pressed={!!popped} aria-label={popped ? 'Bring back' : 'Pop out'}
          title={popped ? 'Bring the app back into this page' : 'Pop out: play this app in its own small window that stays on top of your other windows'}
          onClick={e => { popOut(); blur(e); }}>
          {'⧉ '}<span>{popped ? 'Bring back' : 'Pop out'}</span>
        </button>
      )}
      {chip && (
        <a className="shim-clock" href={root() + 'Clock/index.html'} onClick={() => window.oldjavaSite.skipTitle('Clock')}
          data-soon={chip.soon ? '' : undefined} title={chip.label} aria-label={chip.label}>
          {chip.segs.map(seg => (
            <span key={seg.kind} className="shim-clock-seg" data-kind={seg.kind}>
              <i className="shim-clock-ring" style={{ '--p': seg.part.toFixed(3) } as CSSProperties} />
              <span className="shim-clock-name">{seg.label + ' '}</span>
              <b>{seg.text}</b>
            </span>
          ))}
        </a>
      )}
      {/* the word is hidden on narrow pages */}
      <button type="button" className="shim-sound" data-sound="none" aria-pressed={on} aria-label="Site sounds"
        title={on ? 'Site sounds are on (the apps keep their own sound). Click to mute.' : 'Site sounds are muted. Click to turn them on.'}
        onClick={e => { setSoundOn(!soundOn()); blur(e); }}>
        {'♪ '}<span>{on ? 'Sound' : 'Muted'}</span>
      </button>
      <label>
        {'Skin '}
        {/* uncontrolled: the skin's own code sets its value when the skin changes */}
        <select defaultValue={document.documentElement.getAttribute('data-skin')!} onChange={e => {
          const select = e.currentTarget;
          setSkin(select.value);
          blur(e);
          if (!findOff && select.value === 'own') openMaker(select);
        }}>
          {SKINS.map(o => <option key={o[0]} value={o[0]}>{o[1]}</option>)}
        </select>
      </label>
      {!findOff && (
        <button type="button" className="shim-own-edit" title="Change your skin: its color, desk, corners and windows" data-sound="none"
          onClick={e => { openMaker(e.currentTarget); blur(e); }}>
          <span aria-hidden="true">{'✎'}</span><span className="shim-own-edit-label"> Edit</span>
        </button>
      )}
      {isApp && <More />}
    </div>
  );
}

export function init() {
  // ---- installable and offline: the web app manifest, icons and the service worker (sw.js) ----
  // Every page links them, so the arcade can be installed from any page and every page can be kept offline.
  siteRoot = new URL(/* @vite-ignore */ '..', import.meta.url).href;   // this module is assets/boot.js
  headLink('manifest', 'manifest.webmanifest');
  headLink('icon', 'icons/icon.svg', 'image/svg+xml');
  headLink('apple-touch-icon', 'icons/apple-touch-icon.png');
  theme = document.createElement('meta');
  theme.name = 'theme-color';
  theme.content = '#170a2e';
  document.head.appendChild(theme);
  if ('serviceWorker' in navigator && window.isSecureContext) {
    navigator.serviceWorker.register(siteRoot + 'sw.js').catch(function (e) {
      console.warn('Offline play is not available: ' + e.message);
    });
  }
  // The home page shows its own Install button, so it keeps the browser's install prompt for that button.
  window.addEventListener('beforeinstallprompt', function (e) {
    if (document.body && document.body.hasAttribute('data-app')) return;
    e.preventDefault();
    window.oldjavaInstall = e as Event & OldjavaInstallPrompt;
    document.dispatchEvent(new Event('oldjava-installable'));
  });
}
