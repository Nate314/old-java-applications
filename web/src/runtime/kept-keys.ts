// A key the site kept (a panel, the toolbar's menu, the title screen stopped its press before the app's listener
// on the document) keeps its release too. Escape in Share or Photo closes the panel and moves the focus out of it,
// so the release used to land on the page and reach the app: on ScreenSaver it closed the saver.
// A press is "kept" when it set out from the window (capture) but never came back up to it (bubble).
const kept = new Map<string, EventTarget | null>();

export function init() {
  window.addEventListener('keydown', e => { if (e.isTrusted) kept.set(e.code, e.target); }, true);
  window.addEventListener('keydown', e => { kept.delete(e.code); });
  window.addEventListener('keyup', e => {
    if (!e.isTrusted || !kept.has(e.code)) return;
    const from = kept.get(e.code);
    kept.delete(e.code);
    // Released where it was pressed (the panel is still open): the panel's own key handling has it.
    if (e.target !== from) e.stopImmediatePropagation();
  }, true);
}
