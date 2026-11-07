import type { ReactNode } from 'react';
import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';

/** The element with this id; the pages render all of theirs before any code looks them up. */
export function byId<T extends HTMLElement = HTMLElement>(id: string): T {
  const el = document.getElementById(id);
  if (!el) throw new Error('#' + id + ' is missing');
  return el as T;
}

/** Every element matching the selector, as an array. */
export function all<T extends Element = HTMLElement>(selector: string, root: ParentNode = document): T[] {
  return Array.from(root.querySelectorAll<T>(selector));
}

/** The first match inside root; it must exist. */
export function one<T extends Element = HTMLElement>(selector: string, root: ParentNode = document): T {
  const el = root.querySelector<T>(selector);
  if (!el) throw new Error(selector + ' is missing');
  return el;
}

/**
 * Renders the page into #root at once, not on a later frame: the page's own code and the classic scripts after it
 * (boot.js on DOMContentLoaded, news.js) find the markup in place, as they did when it was static HTML.
 * The pages keep no React state after this, so nothing re-renders over what that code changes.
 */
export function mount(page: ReactNode): void {
  const root = createRoot(byId('root'));
  flushSync(() => root.render(page));
}
