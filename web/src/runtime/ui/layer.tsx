// How the runtime's React parts reach the page. A panel (Share, the drawers, the viewers) is rendered at once
// (flushSync, so it is in the page when the call that opens it returns, as callers expect) and straight into
// <body> through a portal, so its elements are body's own children, with no wrapper for the site's CSS to meet.
import type { ReactNode, RefObject } from 'react';
import { useLayoutEffect } from 'react';
import { createPortal, flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';

/** Puts node in the page's body; the function returned takes it out again. */
export function openLayer(node: ReactNode): () => void {
  const root = createRoot(document.createElement('div'));
  flushSync(() => root.render(createPortal(node, document.body)));
  return () => root.unmount();
}

/** The panel's × button. */
export function CloseButton({ onClick, label = 'Close', title }: { onClick: () => void; label?: string; title?: string }) {
  return <button type="button" className="shim-close" aria-label={label} title={title} onClick={onClick}>{'×'}</button>;
}

/**
 * Keys pressed in a panel are its own: they stop at the panel, before the app's listeners and the title screen's
 * see them, and Escape closes it (ownKeys in then-and-now.ts, for the panels not yet in React).
 */
export function useOwnKeys(ref: RefObject<HTMLElement | null>, onEscape?: () => void) {
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    const down = (e: KeyboardEvent) => {
      e.stopPropagation();
      if (e.key === 'Escape' && onEscape) onEscape();
    };
    const up = (e: KeyboardEvent) => e.stopPropagation();
    el.addEventListener('keydown', down);
    el.addEventListener('keyup', up);
    return () => {
      el.removeEventListener('keydown', down);
      el.removeEventListener('keyup', up);
    };
  }, [ref, onEscape]);
}
