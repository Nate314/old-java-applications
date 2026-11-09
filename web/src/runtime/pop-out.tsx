import { redrawBar } from './install';
import { openLayer } from './ui/layer';
import { windowsBox } from './band-news';
import { passport } from './passport';
import { appId } from './catalog';
import { playSound } from './sounds';

/** The app while it is popped out. */
export interface Popped { win: Window; desktop: HTMLElement; card: () => void; watch: MutationObserver }

export let popped: Popped | null;

// ---- pop out ----
// These were desktop apps, each in a window of its own. Pop out gives them one again: the browser's Document
// Picture-in-Picture (Chrome and Edge) opens a small window that stays on top of every other window on the
// visitor's screen, and the app's desktop moves into it, windows and all. It is the same running app, not a
// copy, so a timer, a song or a game goes on where it was, beside whatever else the visitor is doing. The
// shim follows the desktop (window.__shimDoc, window.__shimListen: see web.rt.Js), and centerWindows fits the
// windows to the small window as it does to the stage, zooming them up when the visitor makes it bigger.
// The page keeps a card in the stage with Bring it back; closing the small window brings the app back too.
const POP_MARGIN = 16;

const POP_BAR = 56;

// Windows and WindowsE draw their pretend Windows on the page, and CircleSpawner places its own windows on
// the screen it was given, so they stay in the page.
export function poppable() {
  var body = document.body;
  return 'documentPictureInPicture' in window && !body.hasAttribute('data-shell')
    && !body.hasAttribute('data-own-layout');
}

export function popOut() {
  if (popped) { bringBack(); return; }
  if (!document.getElementById('desktop')) return;
  if (document.documentElement.hasAttribute('data-title')) {   // still on the title screen: start it, straight into its window
    var play = document.querySelector<HTMLButtonElement>('.shim-title-play');
    if (!play || play.disabled) return;
    play.click();
  }
  documentPictureInPicture.requestWindow(popSize()).then(popInto, function (e) { console.warn('Pop out:', e); });
}

// Room for the windows at their actual size (Fit zooms them up if the visitor enlarges the window). Chrome
// counts its own title bar in the height asked for.
function popSize() {
  var desktop = document.getElementById('desktop');
  var box = desktop && windowsBox(desktop).box;
  var w = box ? box[2] - box[0] + 2 * POP_MARGIN : 640;
  var h = box ? box[3] - box[1] + 2 * POP_MARGIN : 480;
  return { width: Math.round(w), height: Math.round(h + POP_BAR) };
}

// Moves the app's desktop into win, a Picture-in-Picture window opened by this page or by the home page
// around the player's iframe (which may not open one itself).
function popInto(win: Window) {
  var html = document.documentElement;
  var desktop = document.getElementById('desktop');
  if (popped || !desktop) { win.close(); return; }
  var doc = win.document;
  doc.title = document.title;
  Array.prototype.forEach.call(document.querySelectorAll('link[rel="stylesheet"], style'), function (n) {
    var copy;
    if (n.href) {
      copy = doc.createElement('link');
      copy.rel = 'stylesheet';
      copy.href = n.href;   // absolute: the small window's document has no address of its own
    } else {
      copy = doc.createElement('style');
      copy.textContent = n.textContent;
    }
    doc.head.appendChild(copy);
  });
  // Styled like the home page's player: the desk on the page, the windows centered on it.
  var root = doc.documentElement;
  root.setAttribute('data-embed', '');
  function skin() { root.setAttribute('data-skin', html.getAttribute('data-skin')!); }
  skin();
  var watch = new MutationObserver(skin);
  watch.observe(html, { attributes: true, attributeFilter: ['data-skin'] });
  doc.body.appendChild(desktop);
  window.__shimDoc = doc;
  function listen() {
    if (!popped || popped.win !== win) return;
    if (window.__shimListen) window.__shimListen(doc);
    else setTimeout(listen, 100);   // the app has not shown a window yet
  }
  // The shim's focus ring follows the last kind of input (web.rt.Js.focusComponent).
  win.addEventListener('keydown', function () { if (window.__shimKeyed) window.__shimKeyed.on = true; }, true);
  win.addEventListener('pointerdown', function () { if (window.__shimKeyed) window.__shimKeyed.on = false; }, true);
  win.addEventListener('pagehide', bringBack);
  popped = { win: win, desktop: desktop, card: poppedCard(), watch: watch };
  html.setAttribute('data-popped', '');
  listen();
  passport.stamp(appId()!);
  playSound('open');
  redrawBar();
}

export function bringBack() {
  if (!popped) return;
  var p = popped;
  popped = null;
  window.__shimDoc = null;
  p.watch.disconnect();
  document.body.insertBefore(p.desktop, document.body.firstChild);
  p.card();
  document.documentElement.removeAttribute('data-popped');
  if (!p.win.closed) p.win.close();
  playSound('close');
  redrawBar();
}

// What the stage shows while the app is out: where it went, and the way back. Returns what takes it out again.
function poppedCard() {
  var name = document.querySelector('.shim-bar-name');
  return openLayer(<PoppedCard name={(name ? name.textContent : document.title) || ''} />);
}

function PoppedCard({ name }: { name: string }) {
  return (
    <div className="shim-popped">
      <div className="shim-popped-box">
        <div className="shim-popped-art" aria-hidden="true"><span className="screen" /><span className="float" /></div>
        <h2>{name + ' is in its own window'}</h2>
        <p>It stays on top of your other windows, even outside the browser, and keeps running while you do something else.
          {' '}Drag its corner to make it bigger. Close it, or press the button, to play it here again.</p>
        {/* bringBack plays its own sound */}
        <button type="button" data-sound="none" onClick={bringBack}>Bring it back</button>
      </div>
    </div>
  );
}

export function init() {
  popped = null;   // { win, desktop, card, watch } while the app is popped out
  // The home page's player bar pops out the app in its iframe with these.
  window.oldjavaPop = {
    can: poppable,
    size: popSize,
    into: popInto,
    back: bringBack,
    popped: function () { return !!popped; }
  };
}
