import { appId } from './catalog';
import { embedded } from './skin';
import { APPS, hold, real } from './slow-mo';

// ---- An app in a frame nobody can see waits where it is ----
// compare.html on a phone shows one pane at a time, so an embedded app can sit in a frame that is display: none (or
// inside one). A real-time game there played on unseen: the dino could run into a cactus before the visitor flipped
// back. Now, while the frame (or any frame around
// it) is not rendered, the page's clock, timers and frames hold (slow-mo.tsx hold) and pick up where they stopped
// once it shows again. Only the apps that move on their own; a clock or a tool keeps its own time.

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
  if (!embedded || window.parent === window || APPS.indexOf(appId() || '') < 0) return;
  // shortcut: polled four times a second (a hidden frame gets no layout events to hear), so a game runs on up to a
  // quarter second unseen; a message from the host page would make it instant, if a host ever needs that.
  hold(unseen());   // wraps the timers now, before the app's main sets any
  real.setInterval(function () { hold(unseen()); }, 250);
}
