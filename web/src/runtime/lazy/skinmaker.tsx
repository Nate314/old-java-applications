/*
 * Your skin: the panel where a visitor makes the site's fourth skin, loaded by the runtime the first time it opens.
 * Five choices (light or dark, a color, a desk wallpaper, corners, window chrome) are put on as they are picked,
 * on this page and every other open page of the site, so the running app is the preview. Surprise me rolls all
 * five, the link shares the skin (?skin=<code>), and Undo goes back to the skin worn when the panel opened.
 * A page opened from such a link already wears the shared skin (the runtime); offer() asks whether to keep it.
 * The runtime makes the CSS (window.oldjavaSkin.own.css) and lends the rest through window.oldjavaSkin.own.
 * The skin being made is kept here (spec); each change puts it on and draws the panel again at once.
 */
import { useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { flushSync } from 'react-dom';
import { openLayer } from '../ui/layer';

declare global {
  interface Window {
    oldjavaSkinMaker?: { open(from?: Element | null): void; offer(): void; close(): void; isOpen(): boolean };
  }
}
// The runtime's skin-maker.ts has lent window.oldjavaSkin.own by the time this file runs.
type OwnApi = NonNullable<OldjavaSkin['own']>;
type OwnSkin = NonNullable<ReturnType<OwnApi['parse']>>;

const skins = window.oldjavaSkin as OldjavaSkin & { own: OwnApi }, own = skins.own, site = window.oldjavaSite || {};
const COLORS = ['#2563eb', '#7c3aed', '#db2777', '#dc2626', '#ea580c', '#ca8a04', '#16a34a', '#0d9488', '#0891b2', '#475569'];
const DESKS = [['plain', 'Plain'], ['fade', 'Fade'], ['grid', 'Grid'], ['dots', 'Dots'], ['stripes', 'Stripes'], ['aurora', 'Aurora']];
const CORNERS = [['0', 'Square'], ['1', 'Soft'], ['2', 'Round']];
const CHROMES = [['flat', 'Flat'], ['gloss', 'Glossy'], ['glow', 'Glow']];
const NAMES: Record<string, string> = { modern: 'Modern', classic: 'Classic', arcade: 'Arcade', own: 'your skin' };

let spec: OwnSkin = null!, before: { code: string; skin: string } | null = null, opener: HTMLElement | null = null;
let shown: { el: HTMLElement; redraw: () => void; close: () => void } | null = null, offerClose: (() => void) | null = null;
// What Copy link says for a moment after a copy; kept across closing and opening the panel, as the panel itself was.
let said: string | null = null;

function sound(name: string) { if (site.sound) site.sound(name); }
function deskOf(s: OwnSkin) { return (/--desk: ([^;]*);/.exec(own.css(s)) || [])[1] || ''; }
function shareLink() { return location.origin + location.pathname + '?skin=' + own.code(spec); }

// Puts the current choices on, here and on every open page, and shows them in the panel.
function wear() {
  own.wear(own.code(spec));
  if (skins.get() !== 'own') skins.own.setSkin('own');
  if (shown) shown.redraw();
}

function hsl(h: number, s: number, l: number) {
  function f(n: number) { var k = (n + h / 30) % 12; return l - s * Math.min(l, 1 - l) * Math.max(-1, Math.min(k - 3, 9 - k, 1)); }
  return '#' + [f(0), f(8), f(4)].map(function (v) { return ('0' + Math.round(v * 255).toString(16)).slice(-2); }).join('');
}

/** A labelled row of toggle buttons; pick(value) changes the spec. */
function Row({ title, options, cls, pressed, pick, children }: {
  title: string; options: string[][]; cls: string; pressed: (value: string) => boolean; pick: (value: string) => void; children?: ReactNode;
}) {
  const id = 'shim-own-' + title.toLowerCase();
  return (
    <div className="shim-own-row">
      <div className="shim-own-label" id={id}>{title}</div>
      <div className={'shim-own-options ' + cls} role="group" aria-labelledby={id}>
        {options.map(o => (
          <button key={o[0]} className="shim-own-option" type="button" value={o[0]} aria-pressed={pressed(o[0])}
            style={cls === 'colors' ? { background: o[0] } : undefined} aria-label={cls === 'colors' ? o[1] : undefined}
            title={cls === 'colors' ? o[1] : undefined} onClick={() => { pick(o[0]); wear(); }}>
            {cls === 'desks' ? (
              <>
                <span className="shim-own-desk" style={{ background: deskOf({ dark: spec.dark, color: spec.color, desk: o[0], corners: spec.corners, chrome: spec.chrome }) }} />
                <span className="shim-own-name">{o[1]}</span>
              </>
            ) : cls === 'colors' ? null : o[1]}
          </button>
        ))}
        {children}
      </div>
    </div>
  );
}

function SkinPanel({ onMount }: { onMount: (el: HTMLElement, redraw: () => void) => void }) {
  const panel = useRef<HTMLElement>(null);
  const picker = useRef<HTMLInputElement>(null);
  const link = useRef<HTMLInputElement>(null);
  const [, setTick] = useState(0);
  useLayoutEffect(() => { onMount(panel.current!, () => flushSync(() => setTick(n => n + 1))); }, []);
  // Their values, not their value attributes, as they were set before.
  useLayoutEffect(() => {
    picker.current!.value = spec.color;
    link.current!.value = shareLink();
  });
  function copy() {
    function say(text: string) {
      said = text;
      if (shown) shown.redraw();
      setTimeout(function () { said = null; if (shown) shown.redraw(); }, 1800);
    }
    if (navigator.clipboard && 'writeText' in navigator.clipboard) {
      navigator.clipboard.writeText(link.current!.value).then(function () { say('Copied'); sound('saved'); },
        function () { link.current!.focus(); say('Press Ctrl C'); });
    } else { link.current!.focus(); say('Press Ctrl C'); }
  }
  function surprise() {
    function any<T>(list: T[]) { return list[Math.floor(Math.random() * list.length)]; }
    var hue = Math.floor(Math.random() * 360);
    spec = { dark: Math.random() < 0.5, color: hsl(hue, 0.55 + Math.random() * 0.35, 0.38 + Math.random() * 0.2),
      desk: any(DESKS)[0], corners: Math.floor(Math.random() * 3), chrome: any(CHROMES)[0] };
    wear();
    sound('skin');
  }
  return (
    <section className="shim-own" role="dialog" aria-label="Your skin" ref={panel}>
      <div className="shim-own-head">
        <h2 className="shim-own-title">Your skin</h2>
        <button className="shim-own-x" type="button" aria-label="Done" onClick={shut}>{'×'}</button>
      </div>
      <p className="shim-own-lead">Every page and app wears it as you choose. Make it yours, then send it to a friend.</p>
      <Row title="Mode" options={[['l', 'Light'], ['d', 'Dark']]} cls="base" pressed={v => (v === 'd') === spec.dark} pick={v => { spec.dark = v === 'd'; }} />
      <Row title="Color" options={COLORS.map(c => [c, c])} cls="colors" pressed={v => v === spec.color} pick={v => { spec.color = v; }}>
        <input type="color" className="shim-own-picker" title="Any color" aria-label="Any color" ref={picker}
          onInput={e => { spec.color = e.currentTarget.value.toLowerCase(); wear(); }} />
      </Row>
      <Row title="Desk" options={DESKS} cls="desks" pressed={v => v === spec.desk} pick={v => { spec.desk = v; }} />
      <Row title="Corners" options={CORNERS} cls="corners" pressed={v => +v === spec.corners} pick={v => { spec.corners = +v; }} />
      <Row title="Windows" options={CHROMES} cls="chromes" pressed={v => v === spec.chrome} pick={v => { spec.chrome = v; }} />
      <div className="shim-own-share">
        <input type="text" readOnly className="shim-own-link" aria-label="Link to this skin" ref={link} onFocus={e => e.currentTarget.select()} />
        <button className="shim-own-copy" type="button" onClick={copy}>{said || 'Copy link'}</button>
      </div>
      <div className="shim-own-foot">
        <button className="shim-own-btn" type="button" onClick={surprise}>{'⚄ Surprise me'}</button>
        <button className="shim-own-btn shim-own-undo" type="button" onClick={() => {
          own.wear(before!.code);
          skins.own.setSkin(before!.skin);
          shut();
        }}>{before!.skin === 'own' ? 'Undo' : 'Back to ' + NAMES[before!.skin]}</button>
        <button className="shim-own-btn primary" type="button" onClick={shut}>Done</button>
      </div>
    </section>
  );
}

function open(from?: Element | null) {
  if (offerClose) dismissOffer();
  opener = (from || document.activeElement) as HTMLElement | null;
  if (shown) { shown.el.querySelector('button')!.focus(); return; }
  before = { code: own.saved(), skin: skins.get() };
  spec = own.parse(own.saved())!;
  var el!: HTMLElement, redraw!: () => void;
  const close = openLayer(<SkinPanel onMount={(e, r) => { el = e; redraw = r; }} />);
  shown = { el: el, redraw: redraw, close: close };
  wear();
  sound('open');
  var first = el.querySelector<HTMLElement>('.shim-own-option[aria-pressed="true"]') || el.querySelector('button');
  first!.focus();
}
function shut() {
  if (!shown) return;
  shown.close();
  shown = null;
  sound('close');
  if (opener && opener.isConnected && opener.focus) opener.focus();
}

// ---- a shared skin (?skin=): the runtime has put it on; keep it, change it, or go back ----
function dismissOffer() {
  if (offerClose) offerClose();
  offerClose = null;
  try { history.replaceState(history.state, '', location.href.replace(/([?&])skin=[^&#]*&?/, '$1').replace(/[?&](#|$)/, '$1')); } catch (e) { /* stays */ }
}
function offer() {
  var o = own.offer();
  if (!o || offerClose) return;
  offerClose = openLayer(
    <div className="shim-own-offer" role="status">
      <strong>You are wearing a skin a friend made.</strong>
      <span> Every page and app now looks like this.</span>
      <div className="shim-own-foot">
        <button className="shim-own-btn primary" type="button" onClick={() => { dismissOffer(); sound('saved'); }}>Keep it</button>
        <button className="shim-own-btn" type="button" onClick={() => { dismissOffer(); open(); }}>Change it</button>
        <button className="shim-own-btn" type="button" onClick={() => {
          own.wear(o!.code);
          skins.own.setSkin(o!.skin);
          dismissOffer();
        }}>{o.skin === 'own' ? 'Back to mine' : 'Back to ' + NAMES[o.skin]}</button>
      </div>
    </div>
  );
}

// A change made in another page (its own panel, or a shared link opened there) shows here too.
window.addEventListener('storage', function (e) {
  if (e.key !== 'oldjava.ownSkin' || !shown || !own.parse(e.newValue)) return;
  spec = own.parse(e.newValue)!;
  shown.redraw();
});

window.oldjavaSkinMaker = { open: open, offer: offer, close: shut, isOpen: function () { return !!shown; } };

export {};   // a module, imported when it is needed (its chunk is assets/skinmaker.js)
