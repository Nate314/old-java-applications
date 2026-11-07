import { embedded } from './skin';
import { windowsBox } from './band-news';
import { passport } from './passport';
import { fireKey, keyOf } from './touch-pad';
import { appId } from './catalog';
import { isThen } from './then-and-now';
import { setAppMuted } from './midi';

// ---- watch: the computer plays (?watch) ----
// Four games can play themselves: Minesweeper's solver and Ultimate Tic-Tac-Toe's computer on both sides (W),
// TankGame's computer driving both tanks (C) and DinoGame's original bot (B); the keys are the apps' own. A page
// opened with ?watch skips the title screen, presses that key once the app's window is up, keeps the app quiet and
// says so in a line over the stage. The visitor's first click or key (or press on the controller) takes over:
// the key is pressed again, so the computer stops, before the visitor's own press reaches the app, which makes
// that press their first move. The passport stamp waits for it too.
export const WATCH: Record<string, string> = { Minesweeper: 'W', UltimateTicTacToe: 'W', TankGame: 'C', DinoGame: 'B' };

const NOT_TAKING_OVER = '.shim-toolbar, .shim-drawer, .shim-share, .shim-scrim, .shim-overlay, .shim-toast, .shim-source';

export function watchWanted() {
  return /[?&]watch(=|&|$)/.test(location.search) && !!WATCH[appId()!] && !isThen();
}

export function watchApp(desktop: HTMLElement) {
  var key = WATCH[appId()!];
  var note = document.createElement('div');
  note.className = 'shim-watch';
  note.setAttribute('role', 'status');
  var dot = document.createElement('span');
  dot.className = 'shim-watch-dot';
  var strong = document.createElement('strong');
  strong.textContent = 'The computer is playing';
  var how = document.createElement('span');
  how.textContent = 'Click or press any key to take over';
  [dot, strong, how].forEach(function (el) { note.appendChild(el); });
  document.body.appendChild(note);
  setAppMuted(true);
  var pressed = false;
  function press() { fireKey('keydown', key); fireKey('keyup', key); }
  // Once the app's first window is on the desktop, and has had a moment to take the focus.
  var wait = setInterval(function () {
    if (!windowsBox(desktop).box) return;
    clearInterval(wait);
    wait = setTimeout(function () { press(); pressed = true; }, 400);
  }, 100);

  function take(ownKey: boolean) {
    document.removeEventListener('pointerdown', onInput, true);
    document.removeEventListener('keydown', onInput, true);
    window.removeEventListener('message', onMessage);
    clearInterval(wait);
    clearTimeout(wait);
    if (pressed && !ownKey) press();
    setAppMuted(false);
    note.classList.add('gone');
    setTimeout(function () { note.remove(); }, 400);
    // A reload now starts the app for the visitor.
    var search = location.search.replace(/([?&])watch(=[^&]*)?(&|$)/, function (m, lead, value, tail) { return tail ? lead : ''; });
    history.replaceState(history.state, '', location.pathname + search + location.hash);
    passport.stamp(appId()!);
    if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaWatch: 'taken' }, location.origin);
  }
  function onInput(e: Event) {
    if (!e.isTrusted || e.target && (e.target as Element).closest && (e.target as Element).closest(NOT_TAKING_OVER)) return;
    if (e.type === 'keydown' && /^(Shift|Control|Alt|Meta|Tab|Escape)$/.test((e as KeyboardEvent).key)) return;
    take(e.type === 'keydown' && (e as KeyboardEvent).code === keyOf(key).code);
  }
  // The home page's player asks first, before it passes its controller's presses in.
  function onMessage(e: MessageEvent) {
    if (e.origin === location.origin && e.source === window.parent && e.data && e.data.oldjavaWatchTake) take(false);
  }
  document.addEventListener('pointerdown', onInput, true);
  document.addEventListener('keydown', onInput, true);
  window.addEventListener('message', onMessage);
}

export function init() {
  window.oldjavaWatch = { has: function (id: string) { return !!WATCH[id]; } };
}
