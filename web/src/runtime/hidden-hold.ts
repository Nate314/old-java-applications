import { appId } from './catalog';
import { embedded } from './skin';
import { APPS, hold, real } from './slow-mo';
import { popped } from './pop-out';
import { hush } from './midi';

// ---- An app in a frame nobody can see waits where it is ----
// compare.html on a phone shows one pane at a time, so an embedded app can sit in a frame that is display: none (or
// inside one). A real-time game there played on unseen: the dino could run into a cactus before the visitor flipped
// back. Now, while the frame (or any frame around
// it) is not rendered, the page's clock, timers and frames hold (slow-mo.tsx hold) and pick up where they stopped
// once it shows again. Only the apps that move on their own; a clock or a tool keeps its own time.
// The same goes for a whole page nobody can see: a phone visitor switching to a message, a locked screen, another
// tab, a minimised window. A hidden page's game played on unseen at the browser's slowed timer pace; now it waits, and the shim tells the app its window lost the keyboard (TankGame pauses). A popped out
// app is still in sight, whatever its page. Every other app (a synth, a music drill) keeps its own time, but its
// sound stops too: held notes are let go and the page comes back quiet.

/** Whether this page's frame, or a frame around it, is not rendered (display: none, visibility: hidden, no size). */
export function unseen(): boolean {
  if (innerWidth === 0 || innerHeight === 0) return true;
  try {
    for (var w: Window = window; w !== w.parent; w = w.parent) {
      var f = w.frameElement;   // null in a frame from another site, whose page we cannot look into
      if (f && !f.checkVisibility({ visibilityProperty: true })) return true;
    }
  } catch (e) { /* a page from another site around ours: what we could see says it shows */ }
  return false;
}

export function init() {
  // every app goes quiet unseen (midi.ts hush); only the movers also hold their clock
  var moves = APPS.indexOf(appId() || '') >= 0;
  function check() {
    var on = !popped && (document.hidden || unseen());
    if (moves) hold(on);
    hush(on, moves);
  }
  check();   // wraps the timers now, before the app's main sets any
  document.addEventListener('visibilitychange', check);
  if (!embedded || window.parent === window) return;
  // shortcut: polled four times a second (a hidden frame gets no layout events to hear), so a game runs on up to a
  // quarter second unseen; a message from the host page would make it instant, if a host ever needs that.
  real.setInterval(check, 250);
}
