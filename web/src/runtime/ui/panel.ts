// A panel the visitor opens (Share, Switch app, Side by side's picker, a photo, a clip, the source): it opens with
// the open sound, and its shut plays the close sound, runs the caller's clean-up, takes the panel out and gives
// the keyboard back to the button that opened it, if that is still in the page.
import type { ReactNode } from 'react';
import { playSound } from '../sounds';
import { openLayer } from './layer';

/** render gets shut (for the panel's onClose); the function returned takes the panel out without sound or focus. */
export function openPanel(render: (shut: () => void) => ReactNode, opener: Element | null | undefined, done: () => void) {
  function shut() {
    playSound('close');
    done();
    close();
    if (opener && opener.isConnected) (opener as HTMLElement).focus();
  }
  const close = openLayer(render(shut));
  playSound('open');
  return close;
}
