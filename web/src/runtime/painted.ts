// The shim sets window.__shimPainted whenever it draws on a canvas (Js.painted, many times a frame), to the canvas
// it drew on (or true). Here that only counts, so each watcher (replay, stage glow) can ask on its own whether
// anything drew since it last looked, and collects the canvases for a watcher that wants to know which (replay).
var paints = 0, sets: Set<Element>[] = [];
Object.defineProperty(window, '__shimPainted', { configurable: true, set: function (v) {
  if (!v) return;
  paints++;
  if (v.nodeType === 1) for (var i = 0; i < sets.length; i++) sets[i].add(v);
} });

/** Returns a check that says whether the shim drew on a canvas since the check last ran (or since this call). */
export function paintWatch() {
  var seen = paints;
  return function () { var drew = seen !== paints; seen = paints; return drew; };
}

/** A set that every page canvas the shim draws on from now is added to; the watcher empties it as it looks. */
export function paintedCanvases() {
  var set = new Set<Element>();
  sets.push(set);
  return set;
}
