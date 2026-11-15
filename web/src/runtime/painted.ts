// The shim sets window.__shimPainted = true whenever it draws on a canvas (Js.painted, many times a frame). Here
// that only counts, so each watcher (replay, stage glow) can ask on its own whether anything drew since it last looked.
var paints = 0;
Object.defineProperty(window, '__shimPainted', { configurable: true, set: function (v) { if (v) paints++; } });

/** Returns a check that says whether the shim drew on a canvas since the check last ran (or since this call). */
export function paintWatch() {
  var seen = paints;
  return function () { var drew = seen !== paints; seen = paints; return drew; };
}
