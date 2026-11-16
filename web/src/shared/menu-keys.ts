import type { KeyboardEvent } from 'react';

// The keys of a More menu, the same on the home player and an app page's band (toolbar-more.tsx): the arrows,
// Home and End move through its entries, Escape closes it and goes back to its button, Tab closes it.
export function menuKeys(e: KeyboardEvent, items: HTMLElement[], close: (refocus: boolean) => void) {
  var i = items.indexOf(document.activeElement as HTMLElement), n = items.length;
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault();
    if (n) items[((i < 0 ? (e.key === 'ArrowDown' ? -1 : 0) : i) + (e.key === 'ArrowDown' ? 1 : -1) + n) % n].focus();
  } else if (e.key === 'Home' || e.key === 'End') {
    e.preventDefault();
    if (n) items[e.key === 'Home' ? 0 : n - 1].focus();
  } else if (e.key === 'Escape') {
    e.preventDefault();
    e.stopPropagation();
    close(true);
  } else if (e.key === 'Tab') {
    close(false);
  }
}
