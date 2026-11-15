// The visitor's skin for the pages that do not load boot.js (the story, the lost page): the same localStorage key
// boot.js keeps, set on <html> before the first paint so shim.css (or the page's own styles) wear it at once.
// Yours is built by boot.js, which these pages do not load: Modern stands in for it there.
export const SKIN_SCRIPT = `(function () {
  var s = 'modern';
  try { s = localStorage.getItem('oldjava.skin') || 'modern'; } catch (e) { /* storage blocked */ }
  if (['modern', 'classic', 'arcade'].indexOf(s) < 0) s = 'modern';
  document.documentElement.setAttribute('data-skin', s);
})();`;
