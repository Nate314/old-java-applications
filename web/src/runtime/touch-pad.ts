import { setPadLight } from './gamepads';
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

// An app page's own controller, at the bottom of the page; Controls in the toolbar shows or hides it.
// It starts shown where the main pointer is a finger.
export function padOnPage(toggle: HTMLElement) {
  var el = document.createElement('div');
  var light = pad.render(el, appId()!, fireKey);
  setPadLight(light);
  document.body.appendChild(el);
  function show(on: boolean) {
    el.hidden = !on;
    toggle.setAttribute('aria-pressed', String(on));
    // From its top edge to the bottom of the page, so the gap under it is kept off the stage too.
    document.documentElement.style.setProperty('--pad-h', on ? Math.ceil(innerHeight - el.getBoundingClientRect().top) + 'px' : '0px');
  }
  show(pad.wanted(!!((window.matchMedia as typeof matchMedia | undefined) && matchMedia('(pointer: coarse)').matches)));
  toggle.addEventListener('click', function () {
    show(el.hidden);
    pad.remember(!el.hidden);
    toggle.blur();   // else Space (FlappyBird's flap) would press it again
  });
  addEventListener('resize', function () { if (!el.hidden) show(true); });
  ['keydown', 'keyup'].forEach(function (type) {
    document.addEventListener(type, function (e) { if (e.isTrusted) light((e as KeyboardEvent).keyCode, type === 'keydown'); }, true);
  });
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
    // Builds app `id`'s controller in `el`. send(type, keyName) delivers each press and release.
    // Returns light(keyCode, down), which shows a real key press on the matching buttons.
    render: function (el, id, send) {
      el.textContent = '';
      el.classList.add('shim-pad');
      el.setAttribute('aria-label', 'Controller');
      var buttons: HTMLButtonElement[] = [];
      function button(name: string, cls: string, label?: string) {
        var k = keyOf(name);
        var b = document.createElement('button');
        b.type = 'button';
        b.tabIndex = -1;   // never takes focus, so the app keeps the keyboard
        b.className = cls;
        b.setAttribute('data-which', String(k.which));
        if (label) {
          var l = document.createElement('span');
          l.textContent = label;
          b.appendChild(l);
        }
        var cap = document.createElement('kbd');
        cap.textContent = k.cap;
        b.appendChild(cap);
        b.title = (label ? label + ': ' : '') + k.cap + ' key';
        var held = false;
        function up() {
          if (!held) return;
          held = false;
          b.classList.remove('on');
          send('keyup', name);
        }
        b.addEventListener('pointerdown', function (e) {
          e.preventDefault();   // no focus change, text selection or (on touch) emulated mouse events
          if (held) return;
          held = true;
          try { b.setPointerCapture(e.pointerId); } catch (x) { /* released by pointerup anyway */ }
          b.classList.add('on');
          send('keydown', name);
        });
        b.addEventListener('pointerup', up);
        b.addEventListener('pointercancel', up);
        b.addEventListener('lostpointercapture', up);
        b.addEventListener('contextmenu', function (e) { e.preventDefault(); });
        buttons.push(b);
        return b;
      }
      PADS[id].forEach(function (g) {
        var group = document.createElement('div');
        group.className = 'shim-pad-group' + (g.tint ? ' tint-' + g.tint : '');
        if (g.title) {
          var t = document.createElement('span');
          t.className = 'shim-pad-title';
          t.textContent = g.title;
          group.appendChild(t);
        }
        var body = document.createElement('div');
        body.className = 'shim-pad-body';
        if (g.dpad) {
          var cross = document.createElement('div');
          cross.className = 'shim-dpad';
          ['up', 'left', 'down', 'right'].forEach(function (dir, i) {
            cross.appendChild(button(g.dpad![i], 'shim-pad-btn ' + dir));
          });
          body.appendChild(cross);
          if (g.fire) body.appendChild(button(g.fire, 'shim-pad-btn fire', g.fireLabel || 'Fire'));
        }
        if (g.piano) {
          var keys = document.createElement('div');
          keys.className = 'shim-piano';
          var white = 0;
          g.piano.forEach(function (name, i) {
            var black = [1, 3, 6, 8, 10].indexOf(i % 12) >= 0;
            var b = button(name, 'shim-pad-btn ' + (black ? 'black' : 'white'));
            if (black) b.style.left = 'calc(' + (white * 12.5) + '% - 5%)';   // on the line between two white keys
            else white++;
            keys.appendChild(b);
          });
          body.appendChild(keys);
        }
        (g.buttons || []).forEach(function (o) {
          body.appendChild(button(o.k, 'shim-pad-btn' + (o.big ? ' big' : ''), o.label));
        });
        group.appendChild(body);
        el.appendChild(group);
      });
      return function light(which, down) {
        buttons.forEach(function (b) {
          if (b.getAttribute('data-which') === String(which)) b.classList.toggle('lit', down);
        });
      };
    }
  };
}
