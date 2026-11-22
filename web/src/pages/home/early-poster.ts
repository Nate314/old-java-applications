// The hand-over from the poster the HTML paints on a phone (Lead.tsx EarlyPoster) to the player's own (Home.tsx
// img.poster): same picture, same address (one download), same width. Where the early guess of the screen's top was
// off, the live poster glides from where the early one stood, so nothing jumps. The early one goes after the first
// frame with the live player over it, so that frame shows the live poster at once (home.css) and never a gap.

export function handOverPoster() {
  const early = document.getElementById('early-poster');
  if (!early) return;
  const live = document.querySelector<HTMLElement>('#screen .poster:not([data-gone])');
  const mid = (e: Element) => { const r = e.getBoundingClientRect(); return r.top + r.height / 2; };
  const dy = live ? mid(early) - mid(live) : 0;
  if (live && Math.abs(dy) >= 1 && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
    live.animate({ translate: ['0 ' + dy + 'px', '0 0'] }, { duration: 250, easing: 'ease-out' });
  }
  requestAnimationFrame(() => setTimeout(() => early.remove()));
}
