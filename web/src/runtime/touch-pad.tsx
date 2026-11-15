import type { CSSProperties } from 'react';
import { useLayoutEffect, useRef, useState } from 'react';
import { setPadLight } from './gamepads';
import { flushSync } from 'react-dom';
import { openLayer } from './ui/layer';
import { redrawBar } from './install';
import { appId } from './catalog';

export let pad: OldjavaPad;

/** One group of an app's on-screen controller: a titled D-pad and fire button, a piano, or a row of buttons. */
interface PadGroup {
  title?: string;
  tint?: string;
  dpad?: string[];
  fire?: string;
  fireLabel?: string;
  piano?: string[];
  buttons?: { k: string; label: string; big?: boolean }[];
}

// ---- touch controller ----
// The keyboard games get an on-screen controller, so they can be played on a phone or tablet. Its buttons
// press and release the same keys the app listens for (taken from the apps' sources), and they light up
// when the real key is pressed, so the controller doubles as a key chart. The home page docks it under
// its player (window.oldjavaPad.render); an app page floats it at the bottom.
const PADS: Record<string, PadGroup[]> = {
  FlappyBird: [
    { buttons: [{ k: 'Space', label: 'Flap', big: true }] },
    { buttons: [{ k: 'Z', label: 'Rewind' }, { k: 'C', label: 'Course' }, { k: 'P', label: 'Pause' }, { k: 'R', label: 'Reset' }] }
  ],
  DinoGame: [
    { buttons: [{ k: 'Space', label: 'Jump', big: true }, { k: 'ArrowDown', label: 'Duck' }] },
    { buttons: [{ k: 'B', label: 'Bot' }, { k: 'X', label: 'Reset eye' }, { k: 'E', label: 'Evolve' }, { k: 'F', label: 'Speed' }, { k: 'M', label: 'Sound' }] }
  ],
  TankGame: [
    { title: 'Red tank', tint: 'red', dpad: ['W', 'A', 'S', 'D'], fire: 'Q' },
    { title: 'Green tank', tint: 'green', dpad: ['ArrowUp', 'ArrowLeft', 'ArrowDown', 'ArrowRight'], fire: 'M' }
  ],
  SynthesizerKeyboard: [
    { piano: ['Z', 'S', 'X', 'D', 'C', 'V', 'G', 'B', 'H', 'N', 'J', 'M', ','] },
    { buttons: [{ k: 'T', label: 'Song' }, { k: 'L', label: 'Listen' }, { k: 'R', label: 'Restart' },
                { k: 'Space', label: 'Record' }, { k: 'Enter', label: 'Copy link' }, { k: 'I', label: 'Mic' },
                { k: 'O', label: 'Looper' }, { k: 'P', label: 'Play loop' }] },
    { buttons: [{ k: 'ArrowLeft', label: 'Octave −' }, { k: 'ArrowRight', label: 'Octave +' },
                { k: 'ArrowDown', label: 'Sound −' }, { k: 'ArrowUp', label: 'Sound +' }] }
  ],
  SpriteFloatyStars: [
    { title: 'Star Catcher', tint: 'gold', dpad: ['ArrowUp', 'ArrowLeft', 'ArrowDown', 'ArrowRight'], fire: 'Space', fireLabel: 'Catch' },
    { buttons: [{ k: 'N', label: 'Next' }, { k: 'R', label: 'Again' }, { k: 'M', label: 'Nights' },
                { k: 'ArrowUp', label: 'Add star' }, { k: 'ArrowDown', label: 'Remove star' }, { k: 'T', label: 'Trails' },
                { k: 'H', label: 'Help' }] }
  ]
};

const ARROWS: Record<string, [number, string]> = { ArrowUp: [38, '↑'], ArrowDown: [40, '↓'], ArrowLeft: [37, '←'], ArrowRight: [39, '→'] };

// A key name from PADS as the fields of a DOM key event, plus the label on its keycap.
export function keyOf(name: string) {
  if (name === 'Space') return { key: ' ', code: 'Space', which: 32, cap: 'Space' };
  if (name === 'Enter') return { key: 'Enter', code: 'Enter', which: 13, cap: 'Enter' };
  if (ARROWS[name]) return { key: name, code: name, which: ARROWS[name][0], cap: ARROWS[name][1] };
  if (name === ',') return { key: ',', code: 'Comma', which: 188, cap: ',' };
  return { key: name.toLowerCase(), code: 'Key' + name, which: name.charCodeAt(0), cap: name };
}

// A key event the shim cannot tell from a real one (keyCode cannot be set through the constructor).
export function fireKey(type: string, name: string) {
  var k = keyOf(name);
  var e = new KeyboardEvent(type, { key: k.key, code: k.code, bubbles: true, cancelable: true });
  Object.defineProperty(e, 'keyCode', { get: function () { return k.which; } });
  Object.defineProperty(e, 'which', { get: function () { return k.which; } });
  document.body.dispatchEvent(e);
}

const PAD_KEY = 'oldjava.pad';

/** The controllers drawn, by the element they are in, with what takes each out again. */
const drawn = new WeakMap<HTMLElement, () => void>();

/** An app's controller: its groups, each a D-pad and fire button, a piano or a row of buttons. */
function Pad({ groups, send, onMount }: {
  groups: PadGroup[]; send: (type: string, key: string) => void; onMount: (light: (which: number, down: boolean) => void) => void;
}) {
  const [lit, setLit] = useState<Record<number, boolean>>({});
  useLayoutEffect(() => { onMount((which, down) => setLit(l => ({ ...l, [which]: down }))); }, []);
  const button = (name: string, cls: string, label?: string, style?: CSSProperties) =>
    <PadButton key={cls + name} name={name} cls={cls} label={label} style={style} lit={!!lit[keyOf(name).which]} send={send} />;
  return (
    <>
      {groups.map((g, gi) => {
        let white = 0;
        return (
          <div key={gi} className={'shim-pad-group' + (g.tint ? ' tint-' + g.tint : '')}>
            {g.title && <span className="shim-pad-title">{g.title}</span>}
            <div className="shim-pad-body">
              {g.dpad && <div className="shim-dpad">{['up', 'left', 'down', 'right'].map((dir, i) => button(g.dpad![i], 'shim-pad-btn ' + dir))}</div>}
              {g.dpad && g.fire && button(g.fire, 'shim-pad-btn fire', g.fireLabel || 'Fire')}
              {g.piano && (
                <div className="shim-piano">
                  {g.piano.map((name, i) => {
                    const black = [1, 3, 6, 8, 10].indexOf(i % 12) >= 0;
                    if (!black) white++;
                    // a black key stands on the line between two white keys
                    return button(name, 'shim-pad-btn ' + (black ? 'black' : 'white'), undefined, black ? { left: 'calc(' + (white * 12.5) + '% - 5%)' } : undefined);
                  })}
                </div>
              )}
              {(g.buttons || []).map(o => button(o.k, 'shim-pad-btn' + (o.big ? ' big' : ''), o.label))}
            </div>
          </div>
        );
      })}
    </>
  );
}

/** One of the controller's buttons: held, it holds its key down; lit, its key is down on the keyboard. */
function PadButton({ name, cls, label, style, lit, send }: {
  name: string; cls: string; label?: string; style?: CSSProperties; lit: boolean; send: (type: string, key: string) => void;
}) {
  const held = useRef(false);
  const [on, setOn] = useState(false);
  const k = keyOf(name);
  function up() {
    if (!held.current) return;
    held.current = false;
    setOn(false);
    send('keyup', name);
  }
  // tabIndex -1: it never takes focus, so the app keeps the keyboard
  return (
    <button type="button" tabIndex={-1} className={cls + (on ? ' on' : '') + (lit ? ' lit' : '')} data-which={k.which} style={style}
      title={(label ? label + ': ' : '') + k.cap + ' key'}
      onPointerDown={e => {
        e.preventDefault();   // no focus change, text selection or (on touch) emulated mouse events
        if (held.current) return;
        held.current = true;
        try { e.currentTarget.setPointerCapture(e.pointerId); } catch (x) { /* released by pointerup anyway */ }
        setOn(true);
        send('keydown', name);
      }}
      onPointerUp={up} onPointerCancel={up} onLostPointerCapture={up} onContextMenu={e => e.preventDefault()}>
      {label && <span>{label}</span>}
      <kbd>{k.cap}</kbd>
    </button>
  );
}

// An app page's own controller, at the bottom of the page; Controls in the toolbar shows or hides it (togglePad).
// It starts shown where the main pointer is a finger.
let onPage: { shown: boolean; el: HTMLDivElement; redraw: () => void } | null = null;
/** Whether the app page's controller shows: Controls in the toolbar is pressed while it does. */
export function padShown() { return !!onPage && onPage.shown; }

export function padOnPage() {
  var light!: (which: number, down: boolean) => void;
  openLayer(<PadOnPage onMount={(el, redraw, l) => { onPage = { shown: true, el: el, redraw: redraw }; light = l; }} />);
  setPadLight(light);
  showPad(pad.wanted(!!((window.matchMedia as typeof matchMedia | undefined) && matchMedia('(pointer: coarse)').matches)));
  addEventListener('resize', function () { if (onPage!.shown) showPad(true); });
  ['keydown', 'keyup'].forEach(function (type) {
    document.addEventListener(type, function (e) { if (e.isTrusted) light((e as KeyboardEvent).keyCode, type === 'keydown'); }, true);
  });
}
function showPad(on: boolean) {
  onPage!.shown = on;
  onPage!.redraw();
  redrawBar();
  // From its top edge to the bottom of the page, so the gap under it is kept off the stage too.
  document.documentElement.style.setProperty('--pad-h', on ? Math.ceil(innerHeight - onPage!.el.getBoundingClientRect().top) + 'px' : '0px');
}
/** Controls in the toolbar: shows or hides the app page's controller, and remembers which. */
export function togglePad() {
  showPad(!onPage!.shown);
  pad.remember(onPage!.shown);
}

function PadOnPage({ onMount }: { onMount: (el: HTMLDivElement, redraw: () => void, light: (which: number, down: boolean) => void) => void }) {
  const el = useRef<HTMLDivElement>(null);
  const light = useRef<(which: number, down: boolean) => void>(null);
  const [, setTick] = useState(0);
  useLayoutEffect(() => { onMount(el.current!, () => flushSync(() => setTick(n => n + 1)), (w, d) => light.current!(w, d)); }, []);
  return (
    <div className="shim-pad" aria-label="Controller" hidden={!!onPage && !onPage.shown} ref={el}>
      <Pad groups={PADS[appId()!]} send={fireKey} onMount={l => { light.current = l; }} />
    </div>
  );
}

// In the home page's player the controller lives on the home page: it sends presses here, and hears
// about real key presses (which go to this frame) to light its buttons.
export function padInPlayer() {
  window.addEventListener('message', function (e) {
    var m = e.data && e.data.oldjavaKey;
    if (e.origin !== location.origin || e.source !== window.parent || !m) return;
    if ((m.type === 'keydown' || m.type === 'keyup') && typeof m.name === 'string') fireKey(m.type, m.name);
  });
  ['keydown', 'keyup'].forEach(function (type) {
    document.addEventListener(type, function (e) {
      if (!e.isTrusted) return;
      window.parent.postMessage({ oldjavaKeyLit: { which: (e as KeyboardEvent).keyCode, down: type === 'keydown' } }, location.origin);
    }, true);
  });
}

export function init() {
  pad = window.oldjavaPad = {
    has: function (id) { return !!PADS[id]; },
    // Shown or not: the visitor's last choice, else `byDefault`.
    wanted: function (byDefault) {
      var v = null;
      try { v = localStorage.getItem(PAD_KEY); } catch (e) { /* not remembered */ }
      return v === 'on' || (v !== 'off' && byDefault);
    },
    remember: function (on) {
      try { localStorage.setItem(PAD_KEY, on ? 'on' : 'off'); } catch (e) { /* not remembered */ }
    },
    // Builds app `id`'s controller in `el` (in place of what it held). send(type, keyName) delivers each press and
    // release. Returns light(keyCode, down), which shows a real key press on the matching buttons.
    render: function (el, id, send) {
      var was = drawn.get(el);
      if (was) was();
      var light!: (which: number, down: boolean) => void;
      drawn.set(el, openLayer(<Pad groups={PADS[id]} send={send} onMount={l => { light = l; }} />, el));
      return function (which, down) { light(which, down); };
    }
  };
}
