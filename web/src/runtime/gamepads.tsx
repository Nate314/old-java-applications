import { Fragment, useLayoutEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { openLayer } from './ui/layer';
import { embedded } from './skin';
import { news } from './band-news';
import { fireKey, keyOf } from './touch-pad';
import { appId, catalog, root } from './catalog';
import { indexOf } from './then-and-now';

let padLight: ((which: number, down: boolean) => void) | null;

/** A controller button's key name and the label the chart shows. */
type PadKey = [string, string];
/** A standard layout button's name (GP). */
type GpButton = keyof typeof GP;
/** One controller's map in an app: buttons to keys, and whether the left stick plays the D-pad's keys. */
type GamepadMap = { stick?: boolean } & Partial<Record<GpButton, PadKey>>;
/** A controller snapshot: per pad, its buttons pressed and its first four axes. */
interface GamepadSnap { t: number; pads: { b: boolean[]; a: number[] }[] }

// ---- game controllers ----
// A game controller (the Gamepad API's standard layout) plays every app. Each app's map below gives its
// buttons the keys the app listens for, with the label the chart shows (taken from PADS and the apps'
// READMEs). What a map leaves free works a pointer drawn on the page: the left stick (unless the map gives
// it the D-pad's keys) and the D-pad move it, A clicks (hold to drag) and B right-clicks. The right stick,
// RT and LT point, click and right-click in every app, and LB / RB go to the previous / next app. Back
// shows the chart again. A second controller gets the map's second entry (TankGame's green tank).
const GAMEPADS: Record<string, GamepadMap[]> = {
  FlappyBird: [{ a: ['Space', 'Flap'], up: ['Space', 'Flap'], b: ['Z', 'Rewind a crash'], x: ['C', 'Course'], y: ['R', 'Reset'], start: ['P', 'Pause'] }],
  DinoGame: [{ a: ['Space', 'Jump'], up: ['Space', 'Jump'], down: ['ArrowDown', 'Duck'], b: ['ArrowDown', 'Duck'],
               x: ['B', 'Bot'], y: ['X', 'Reset eye'], start: ['E', 'Evolve bots'] }],
  TankGame: [
    { stick: true, up: ['W', 'Forward'], down: ['S', 'Back'], left: ['A', 'Turn left'], right: ['D', 'Turn right'],
      a: ['Q', 'Fire (on the title card: vs the computer)'], x: ['X', 'Supply crates on/off'] },
    { stick: true, up: ['ArrowUp', 'Forward'], down: ['ArrowDown', 'Back'], left: ['ArrowLeft', 'Turn left'],
      right: ['ArrowRight', 'Turn right'], a: ['M', 'Fire (on the title card: two players)'] }
  ],
  // The D-pad and the face buttons play the white keys, C to C, going round each cross.
  SynthesizerKeyboard: [{ left: ['Z', 'C'], down: ['X', 'D'], right: ['C', 'E'], up: ['V', 'F'],
                          x: ['B', 'G'], a: ['N', 'A'], b: ['M', 'B'], y: [',', 'High C'],
                          start: ['Space', 'Record'] }],
  SpriteFloatyStars: [{ stick: true, up: ['ArrowUp', 'Up'], down: ['ArrowDown', 'Down'], left: ['ArrowLeft', 'Left'],
                        right: ['ArrowRight', 'Right'], a: ['Space', 'Catch (stars: burst)'], b: ['M', 'Nights'],
                        x: ['N', 'Next (stars: next mode)'],
                        y: ['R', 'Again'], start: ['T', 'Trails'] }],
  ScreenSaver: [{ stick: true, a: ['N', 'Next saver'], b: ['P', 'Previous'], x: ['C', 'Cycle'], y: ['H', 'Keys'],
                  left: ['ArrowLeft', 'Slower'], right: ['ArrowRight', 'Faster'], up: ['ArrowUp', 'Smaller tiles'],
                  down: ['ArrowDown', 'Bigger tiles'], start: ['F', 'Feed the fish'] }],
  RubiksCube2D: [{ up: ['W', 'Turn'], left: ['A', 'Turn'], down: ['S', 'Turn'], right: ['D', 'Turn'], b: ['Z', 'Undo'],
                   x: ['T', 'Teach me'], y: ['R', 'Retry'], start: ['N', 'Next level'] }],
  MusicTheoryExpert: [{ b: ['Space', 'Play / next'], up: ['ArrowUp', 'Previous drill'], down: ['ArrowDown', 'Next drill'],
                        x: ['R', 'Restart'], y: ['N', 'Next drill'] }],
  NumberGuess: [{ up: ['ArrowUp', 'Higher / yes'], down: ['ArrowDown', 'Lower / no'], b: ['Enter', 'Enter'],
                  x: ['S', 'How it works'] }],
  Clock: [{ b: ['Space', 'Start / pause'], x: ['R', 'Reset'], y: ['L', 'Lap / lengths'], start: ['N', 'Skip (Focus)'], up: ['ArrowUp', 'Minute +'],
            down: ['ArrowDown', 'Minute −'], left: ['ArrowLeft', 'Earlier (World)'], right: ['ArrowRight', 'Later (World)'] }],
  StockChecker: [{ up: ['ArrowUp', 'Pick'], down: ['ArrowDown', 'Pick'], x: ['B', 'Buy 1'], y: ['S', 'Sell 1'],
                   b: ['Space', 'Pause'], start: ['R', 'Race'] }],
  GroupCreator: [{ x: ['N', 'New round'], y: ['R', 'Reshuffle'], b: ['Space', 'New round / spin'],
                   start: ['P', 'Pick someone / groups'] }],
  SimpleAlgorithms: [{ b: ['Space', 'Play / pause'], right: ['ArrowRight', 'One move'], x: ['R', 'Reset'], y: ['F', 'Speed'] }]
};

// Standard layout button numbers.
const GP = { a: 0, b: 1, x: 2, y: 3, lb: 4, rb: 5, lt: 6, rt: 7, back: 8, start: 9, up: 12, down: 13, left: 14, right: 15 };

const DIRS: ('up' | 'down' | 'left' | 'right')[] = ['up', 'down', 'left', 'right'];

function gamepadMap(id: string | null, i: number) {
  var maps = GAMEPADS[id as string] || [{}];
  return maps[Math.min(i, maps.length - 1)];
}

// The chart: what each button does in app `id`, as <kbd> chips and labels.
function gamepadChart(id: string) {
  var map = gamepadMap(id, 0);
  var parts: string[][] = [];
  function add(cap: string, text: string) { parts.push([cap, text]); }
  var arrows = { up: '↑', down: '↓', left: '←', right: '→' };
  DIRS.forEach(function (d) { if (map[d]) add('D-pad ' + arrows[d], map[d][1]); });
  if (map.stick) add('Left stick', 'as the D-pad');
  (['a', 'b', 'x', 'y', 'start'] as const).forEach(function (n) { if (map[n]) add(n === 'start' ? 'Start' : n.toUpperCase(), map[n][1]); });
  var free = !map.stick ? (DIRS.some(function (d) { return map[d]; }) ? 'Left stick' : 'Stick or D-pad') : 'Right stick';
  add(free, 'pointer');
  if (!map.a) add('A', 'click (hold to drag)');
  if (!map.b) add('B', 'right-click');
  if (map.a || map.b || map.stick) add('RT / LT', 'click / right-click');
  if ((GAMEPADS[id] || []).length > 1) add('2nd controller', 'player 2');
  add('LB / RB', 'other apps');
  add('Back', 'this chart');
  return parts;
}

/** The chart for the band's news line: each button's cap, and what it does. */
function GamepadChart({ parts }: { parts: string[][] }) {
  return <>{parts.map((p, i) => <Fragment key={i}>{i ? ' · ' : null}<kbd className="shim-gp-cap">{p[0]}</kbd>{' ' + p[1]}</Fragment>)}</>;
}

/** The same chart as nodes, for the home page's player hint (src/pages/home/player.ts keeps that element itself). */
function gamepadChartNodes(parts: string[][]) {
  var nodes: Node[] = [];
  parts.forEach(function (p, i) {
    if (i) nodes.push(document.createTextNode(' · '));
    var k = document.createElement('kbd');
    k.className = 'shim-gp-cap';
    k.textContent = p[0];
    nodes.push(k, document.createTextNode(' ' + p[1]));
  });
  return nodes;
}

/** The pointer a controller's stick moves: at() says where it is and whether it is hidden. */
function GamepadCursor({ at, onMount }: { at: () => [number, number, boolean]; onMount: (draw: () => void) => void }) {
  const [, setTick] = useState(0);
  useLayoutEffect(() => { onMount(() => flushSync(() => setTick(n => n + 1))); }, []);
  const [x, y, hidden] = at();
  return <div className="shim-gp-cursor" aria-hidden="true" hidden={hidden} style={{ transform: 'translate(' + x + 'px, ' + y + 'px)' }} />;
}

// Works one page's app from controller snapshots ({ t, pads: [{ b: [pressed...], a: [axes...] }] }): presses
// and releases the mapped keys, and moves and clicks the pointer. The page polls the controllers itself, or
// (in the home page's player) gets the snapshots from the home page.
function gamepadDriver() {
  var prev: Record<string, boolean>[] = [];       // per controller: the inputs held at the last snapshot
  var eaten: Record<string, boolean>[] = [];      // per controller: inputs that pressed Play on the title screen, ignored until released
  var holders: Record<string, number> = {};    // key name -> how many inputs hold it down
  var lastT = 0;
  var cursor: { hidden: boolean; draw(): void } | null = null, cx = innerWidth / 2, cy = innerHeight / 2, hideTimer: ReturnType<typeof setTimeout> | null = null;
  var downEl: Element | null = null, downButton = -1;

  function lit(name: string, down: boolean) {
    var which = keyOf(name).which;
    if (padLight) padLight(which, down);
    if (embedded && window.parent !== window) {
      window.parent.postMessage({ oldjavaKeyLit: { which: which, down: down } }, location.origin);
    }
  }
  function key(name: string, down: boolean) {
    var n = (holders[name] || 0) + (down ? 1 : -1);
    holders[name] = Math.max(n, 0);
    if (down && n === 1) { fireKey('keydown', name); lit(name, true); }
    if (!down && n === 0) { fireKey('keyup', name); lit(name, false); }
  }

  function showCursor() {
    if (!cursor) {
      var c = cursor = { hidden: false, draw: function () { /* until it is drawn */ } };
      openLayer(<GamepadCursor at={() => [cx, cy, c.hidden]} onMount={draw => { c.draw = draw; }} />);
    }
    cursor.hidden = false;
    cursor.draw();
    clearTimeout(hideTimer as ReturnType<typeof setTimeout>);
    hideTimer = setTimeout(function () { if (cursor && downButton < 0) { cursor.hidden = true; cursor.draw(); } }, 5000);
  }
  // A real mouse takes over from the pointer.
  document.addEventListener('mousemove', function (e) {
    if (!e.isTrusted || !cursor) return;
    cx = e.clientX;
    cy = e.clientY;
    cursor.hidden = true;
    cursor.draw();
  }, true);

  function mouse(type: string, el: Element, button: number, buttons: number) {
    el.dispatchEvent(new MouseEvent(type, {
      bubbles: true, cancelable: true, view: window, detail: 1,
      clientX: cx, clientY: cy, screenX: cx, screenY: cy, button: button, buttons: buttons
    }));
  }
  function under() { return document.elementFromPoint(cx, cy); }
  // The same events, in the same order, as a mouse button: down, moves while held, up, then click (or contextmenu).
  function mouseButton(button: number, down: boolean) {
    showCursor();
    var el = under();
    if (down) {
      if (downButton >= 0 || !el) return;
      downButton = button;
      downEl = el;
      mouse('mousedown', el, button, button === 0 ? 1 : 2);
      if (el.matches && el.matches('input, select, textarea')) (el as HTMLElement).focus();
      return;
    }
    if (button !== downButton) return;
    downButton = -1;
    var from = downEl;
    downEl = null;
    if (!el) return;
    mouse('mouseup', el, button, 0);
    if (button === 2) { mouse('contextmenu', el, 2, 0); return; }
    var common = from;
    while (common && !common.contains(el)) common = common.parentElement;
    if (common) mouse('click', common, 0, 0);
  }
  function moveCursor(dx: number, dy: number) {
    if (!dx && !dy) return;
    cx = Math.max(0, Math.min(innerWidth - 1, cx + dx));
    cy = Math.max(0, Math.min(innerHeight - 1, cy + dy));
    showCursor();
    var el = under();
    if (el) mouse('mousemove', el, 0, downButton < 0 ? 0 : (downButton === 0 ? 1 : 2));
  }

  function stick(x: number, y: number) {
    var m = Math.hypot(x, y);
    if (m < 0.2) return [0, 0];
    var k = Math.pow((Math.min(m, 1) - 0.2) / 0.8, 1.6) / m;
    return [x * k, y * k];
  }

  return function drive(snap: GamepadSnap) {
    var dt = lastT ? Math.min(Math.max(snap.t - lastT, 0), 100) / 1000 : 0;
    lastT = snap.t;
    var count = Math.max(prev.length, snap.pads.length);
    var mx = 0, my = 0;
    for (var i = 0; i < count; i++) {
      var pad = snap.pads[i] || { b: [], a: [] };
      var map = gamepadMap(appId(), i);
      var was = prev[i] || {};
      var now: Record<string, boolean> = {};
      Object.keys(GP).forEach(function (n) { now[n] = !!pad.b[GP[n as GpButton]]; });
      var ls = stick(pad.a[0] || 0, pad.a[1] || 0);
      var rs = stick(pad.a[2] || 0, pad.a[3] || 0);
      if (map.stick) {
        now.up = now.up || ls[1] < -0.3;
        now.down = now.down || ls[1] > 0.3;
        now.left = now.left || ls[0] < -0.3;
        now.right = now.right || ls[0] > 0.3;
      } else {
        mx += ls[0];
        my += ls[1];
      }
      mx += rs[0];
      my += rs[1];
      // On the title screen any button presses Play, and the app never sees that press or its release.
      var play = document.documentElement.hasAttribute('data-title') && document.querySelector<HTMLButtonElement>('.shim-title-play');
      var eat = eaten[i] || (eaten[i] = {});
      Object.keys(now).forEach(function (n) {
        if (n === 'lb' || n === 'rb' || n === 'back') return;
        if (eat[n]) { if (!now[n]) delete eat[n]; return; }
        if (now[n] === !!was[n]) return;
        if (now[n] && play) {
          if (!play.disabled) play.click();
          eat[n] = true;
          return;
        }
        if (map[n as GpButton]) key(map[n as GpButton]![0], now[n]);
        else if (n === 'a' || n === 'rt') mouseButton(0, now[n]);
        else if (n === 'b' || n === 'lt') mouseButton(2, now[n]);
      });
      if (!map.stick) DIRS.forEach(function (d) {
        if (!map[d] && now[d]) { mx += d === 'left' ? -0.6 : d === 'right' ? 0.6 : 0; my += d === 'up' ? -0.6 : d === 'down' ? 0.6 : 0; }
      });
      prev[i] = now;
    }
    prev.length = eaten.length = snap.pads.length;
    moveCursor(mx * 1100 * dt, my * 1100 * dt);
  };
}

export function setPadLight(v: typeof padLight) { padLight = v; }

// Polls the controllers on a top-level page. LB / RB and Back are the page's; the rest drives the app, here
// or in the home page's player.
export function gamepads(isApp: boolean) {
  if (!navigator.getGamepads) return;
  var drive = isApp ? gamepadDriver() : null;
  if (embedded) {
    if (window.parent === window) return;
    window.addEventListener('message', function (e) {
      var s = e.data && e.data.oldjavaGamepad;
      if (e.origin === location.origin && e.source === window.parent && s && s.pads) drive!(s);
    });
    return;
  }
  var running = false, shown = false, held: Record<string, boolean> = {};
  var hintText = '', hintTimer: ReturnType<typeof setTimeout> | null = null;
  function currentApp() {
    if (isApp) return appId();
    var frame = document.getElementById('player-frame');
    var m = frame && /([A-Za-z0-9]+)\/(?:index|then)\.html/.exec(frame.getAttribute('src') || '');
    return m ? m[1] : null;
  }
  function chart() {
    var id = currentApp();
    if (!id) return;
    if (news) { news.show('gamepad', <strong className="shim-news-tag">Controller</strong>, <GamepadChart parts={gamepadChart(id)} />, null, 15000); return; }
    var tag = document.createElement('strong');
    tag.className = 'shim-news-tag';
    tag.textContent = 'Controller';
    var hint = document.getElementById('player-hint');
    if (!hint) return;
    // The app's hint comes back after a while, unless the player has put another app's hint there since.
    if (!hint.querySelector('.shim-gp-cap')) hintText = hint.innerHTML;
    hint.textContent = '';
    ([tag, document.createTextNode(' ')] as Node[]).concat(gamepadChartNodes(gamepadChart(id))).forEach(function (n) { hint!.appendChild(n); });
    clearTimeout(hintTimer as ReturnType<typeof setTimeout>);
    hintTimer = setTimeout(function () { if (hint!.querySelector('.shim-gp-cap')) hint!.innerHTML = hintText; }, 15000);
  }
  function step(by: number) {
    if (!isApp) {
      var b = document.getElementById(by < 0 ? 'player-prev' : 'player-next');
      if (!b) return;
      b.click();
      chart();   // the next app's buttons, in place of its hint
      return;
    }
    catalog().then(function (apps: OldjavaCatalogApp[]) {
      var i = indexOf(apps, appId());
      if (i < 0) return;
      location.href = root() + apps[(i + by + apps.length) % apps.length].id + '/index.html';
    });
  }
  function send(snap: GamepadSnap) {
    if (drive) { drive(snap); return; }
    var frame = document.getElementById('player-frame') as HTMLIFrameElement | null;
    if (frame && frame.contentWindow) frame.contentWindow.postMessage({ oldjavaGamepad: snap }, location.origin);
  }
  function poll() {
    var list: Gamepad[] = Array.prototype.filter.call(navigator.getGamepads(), function (p) { return p && p.connected; });
    var pads = list.map(function (p) {
      return { b: p.buttons.map(function (b) { return b.pressed || b.value > 0.5; }), a: Array.prototype.slice.call(p.axes, 0, 4) };
    });
    var any: Record<string, boolean> = { lb: false, rb: false, back: false };
    pads.forEach(function (p) { Object.keys(any).forEach(function (n) { any[n] = any[n] || !!p.b[GP[n as GpButton]]; }); });
    Object.keys(any).forEach(function (n) {
      if (any[n] && !held[n]) {
        if (n === 'back') chart(); else step(n === 'lb' ? -1 : 1);
      }
      held[n] = any[n];
    });
    send({ t: performance.now(), pads: document.hidden ? [] : pads });
    if (!list.length) { running = false; return; }
    requestAnimationFrame(poll);
  }
  function start() {
    if (!shown) { shown = true; chart(); }
    if (!running) { running = true; requestAnimationFrame(poll); }
  }
  window.addEventListener('gamepadconnected', start);
  // One already in use on the page before this one (the browser reports it after its first button press).
  if (Array.prototype.some.call(navigator.getGamepads(), function (p) { return p && p.connected; })) start();
}

export function init() {
 padLight = null;   // the app page's controller lights (padOnPage)
}
