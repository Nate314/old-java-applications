// How the runtime's React parts reach the page. A panel (Share, the drawers, the viewers) is rendered at once
// (flushSync, so it is in the page when the call that opens it returns, as callers expect) and straight into
// <body> through a portal, so its elements are body's own children, with no wrapper for the site's CSS to meet.
import type { ReactNode, RefObject } from 'react';
import { useLayoutEffect } from 'react';
import { createPortal, flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';

/** Puts node at the end of `into` (the page's body unless it says); the function returned takes it out again. */
export function openLayer(node: ReactNode, into: Element = document.body): () => void {
  const root = createRoot(document.createElement('div'));
  flushSync(() => root.render(createPortal(node, into)));
  return () => root.unmount();
}

/** The panel's × button. sound, given, is its data-sound (none: the click plays the panel's own sound instead). */
export function CloseButton({ onClick, label = 'Close', title, sound }: { onClick: () => void; label?: string; title?: string; sound?: string }) {
  return <button type="button" className="shim-close" aria-label={label} title={title} data-sound={sound} onClick={onClick}>{'×'}</button>;
}

/** Tab and Shift+Tab go round a modal panel's own controls, so the keyboard never ends up behind its scrim. */
function keepTab(el: HTMLElement, e: KeyboardEvent) {
  const all = [...el.querySelectorAll<HTMLElement>('a[href], button, input, select, textarea, [tabindex]')]
    .filter(c => c.tabIndex >= 0 && !(c as HTMLButtonElement).disabled && c.getClientRects().length > 0);
  if (!all.length) return;
  const first = all[0], last = all[all.length - 1];
  if (e.shiftKey ? document.activeElement === first || !el.contains(document.activeElement) : document.activeElement === last) {
    e.preventDefault();
    (e.shiftKey ? last : first).focus();
  }
}

/**
 * Keys pressed in a panel are its own: they stop at the panel, before the app's listeners and the title screen's
 * see them, and Escape closes it. They stop before
 * React's own listener on the body sees them too, so a key handler inside such a panel is onKeyDownCapture.
 */
export function useOwnKeys(ref: RefObject<HTMLElement | null>, onEscape?: () => void) {
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    const down = (e: KeyboardEvent) => {
      e.stopPropagation();
      if (e.key === 'Escape' && onEscape) onEscape();
      if (e.key === 'Tab' && el.getAttribute('aria-modal') === 'true') keepTab(el, e);
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
