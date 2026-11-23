// One page of the arcade passport: each app played gets a rubber stamp in its category's ink (round for games and
// music, a box for tools and toys), worn like real ink, tilted its own way, dated the day it was first played.
// The app's picture sits inside the ink, in its own colors.
// Apps not played yet show a dashed outline of their stamp around a faded picture. The Passport tab of Your arcade
// (count ring, buttons, the page of stamps) is here too. Styles: passport.css.
import { type CSSProperties } from 'react';
import { APPS, webp, type App } from '../../shared/catalog';
import { H } from './model';
import * as P from './player';
import { stampCount } from './tabs';
import { passportPicture } from './passport-picture';
import './passport.css';

export const played = (a: App) => window.oldjavaPassport.has(a.id);

const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

/** A stamp's date as passports print it: 3 NOV 2026. */
function stampDate(t: number) {
  const d = new Date(t);
  return d.getDate() + ' ' + MONTHS[d.getMonth()] + ' ' + d.getFullYear();
}

/** A small number that stays the same for an app, so its stamp always lands the same way. */
function hash(s: string) {
  let h = 7;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 9973;
  return h;
}

/** Squeezes long text into `max` units; short text keeps its own spacing. */
const fit = (text: string, per: number, max: number) => (text.length * per > max ? max : undefined);

/** The scalloped edge of a music stamp: 36 bumps round a circle. */
const SCALLOP = Array.from({ length: 72 }, (_, i) => {
  const a = (i / 72) * 2 * Math.PI, r = i % 2 ? 44 : 48;
  return (50 + r * Math.cos(a)).toFixed(1) + ',' + (50 + r * Math.sin(a)).toFixed(1);
}).join(' ');

const OCTAGON = '22,8 78,8 94,24 94,76 78,92 22,92 6,76 6,24';

/** The stamp's outline, alone (for the dashed blank) or with its inner line. */
function Edge({ cat, inner }: { cat: App['cat']; inner: boolean }) {
  if (cat === 'game' || cat === 'music') return (
    <>
      {cat === 'music' ? <polygon points={SCALLOP} className="edge" /> : <circle cx="50" cy="50" r="46" className="edge" />}
      {inner && <circle cx="50" cy="50" r="35" className="line" />}
    </>
  );
  return (
    <>
      {cat === 'toy' ? <polygon points={OCTAGON} className="edge" /> : <rect x="5" y="12" width="90" height="76" rx="7" className="edge" />}
      {inner && <rect x="13" y="28" width="74" height="44" rx="3" className="line" />}
    </>
  );
}

/** An app's stamp; `prefix` keeps its ids apart from the same stamp drawn elsewhere on the page. */
export function Stamp({ a, when, prefix = 'stamp-' }: { a: App; when: number; prefix?: string }) {
  const h = hash(a.id), id = prefix + a.id, round = a.cat === 'game' || a.cat === 'music';
  const style = { '--tilt': ((h % 25) - 12) + 'deg', '--dx': ((h % 7) - 3) + 'px', '--dy': ((h % 5) - 2) + 'px' } as CSSProperties;
  const name = a.name.toUpperCase(), date = when ? stampDate(when) : 'PLAYED';
  if (!when && !window.oldjavaPassport.has(a.id)) return (
    <svg className="stamp blank" viewBox="0 0 100 100" aria-hidden="true">
      <Edge cat={a.cat} inner={false} />
      <image href={webp(a.shot, true)} x="24" y="24" width="52" height="52" preserveAspectRatio="xMidYMid slice"
        style={{ clipPath: round ? 'circle(26px)' : 'inset(4px round 4px)' }} />
    </svg>
  );
  return (
    <svg className={'stamp ink-' + a.cat} viewBox="0 0 100 100" style={style} aria-hidden="true">
      <defs>
        {/* Worn ink: noise knocks small holes in it and shakes its edges; each app's own seed wears it differently. */}
        <filter id={id + '-worn'} x="-5%" y="-5%" width="110%" height="110%">
          <feTurbulence type="fractalNoise" baseFrequency="0.7" numOctaves="2" seed={h % 97} result="noise" />
          <feColorMatrix in="noise" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  -3 0 0 0 2.5" result="holes" />
          <feComposite in="SourceGraphic" in2="holes" operator="in" result="holed" />
          <feDisplacementMap in="holed" in2="noise" scale="1.2" />
        </filter>
        <path id={id + '-top'} d={round ? 'M 13 50 A 37 37 0 0 1 87 50' : 'M 10 23 L 90 23'} />
        <path id={id + '-bottom'} d={round ? 'M 6.5 50 A 43.5 43.5 0 0 0 93.5 50' : 'M 10 84 L 90 84'} />
      </defs>
      <g filter={'url(#' + id + '-worn)'}>
        <Edge cat={a.cat} inner />
        <text className="name" textLength={fit(name, 6.2, round ? 108 : 76)} lengthAdjust="spacingAndGlyphs">
          <textPath href={'#' + id + '-top'} startOffset="50%" textAnchor="middle">{name}</textPath>
        </text>
        <text className="date">
          <textPath href={'#' + id + '-bottom'} startOffset="50%" textAnchor="middle">{date}</textPath>
        </text>
        {round && <><text x="7.5" y="53" className="star">★</text><text x="92.5" y="53" className="star">★</text></>}
      </g>
      <image href={webp(a.shot, true)} x={round ? 24 : 15} y={round ? 24 : 30} width={round ? 52 : 70} height={round ? 52 : 40}
        preserveAspectRatio="xMidYMid slice" style={{ clipPath: round ? 'circle(25px)' : 'inset(1px round 2px)' }} />
    </svg>
  );
}

/** The passport's machine-readable lines, as at the foot of a real passport's photo page: holder, count, dates. */
function Mrz({ count }: { count: number }) {
  const times = APPS.map(a => window.oldjavaPassport.when(a.id)).filter(Boolean);
  const ymd = (t: number) => { const d = new Date(t); return [d.getFullYear() % 100, d.getMonth() + 1, d.getDate()].map(n => String(n).padStart(2, '0')).join(''); };
  const pad = (s: string) => (s + '<'.repeat(44)).slice(0, 44);
  const first = times.length ? ymd(Math.min(...times)) : '<<<<<<', last = times.length ? ymd(Math.max(...times)) : '<<<<<<';
  return (
    <p className="mrz" aria-hidden="true">
      <span>{pad('P<JAVA<<ARCADE<<VISITOR')}</span>
      <span>{pad(String(count).padStart(2, '0') + 'OF' + APPS.length + '<<' + first + '<<' + last)}</span>
    </p>
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

export function Passport({ off }: { off: string }) {
  const count = stampCount(), left = APPS.length - count;
  return (
    <section className={'passport' + off} id="passport" role="tabpanel" aria-labelledby="tab-passport">
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
            onClick={e => window.oldjavaShare.open(null, e.currentTarget, passportPicture)}>
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
