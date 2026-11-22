/**
 * Runs fn once the browser has painted the next frame: work a click or key sets off but need not show in the frame
 * that answers it (an app's main, the tap sound, a passport stamp) waits for that frame instead of holding it.
 */
export function afterPaint(fn: () => void) {
  requestAnimationFrame(function () { setTimeout(fn); });
}
