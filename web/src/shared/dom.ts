import type { ReactNode } from 'react';
import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';

/** The element with this id; the pages render all of theirs before any code looks them up. */
export function byId<T extends HTMLElement = HTMLElement>(id: string): T {
  const el = document.getElementById(id);
  if (!el) throw new Error('#' + id + ' is missing');
  return el as T;
}

/**
 * Renders the page into #root at once, not on a later frame: the page's own code and the runtime after it
 * (boot.js on DOMContentLoaded) find the markup in place, as they did when it was static HTML.
 */
export function mount(page: ReactNode): void {
  const root = createRoot(byId('root'));
  flushSync(() => root.render(page));
}
