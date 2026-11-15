// The box over the stage: Loading..., a missing program, the app's exit screen (exitScreen in source-viewer.tsx)
// and its plain form in the player.
import type { ReactNode, Ref } from 'react';
import { appId } from '../catalog';
import { AGAIN_KEY } from '../records';
import { openLayer } from './layer';

/** The visitor already pressed start once: the reloaded page skips the title screen. */
export function runAgain() {
  try { sessionStorage.setItem(AGAIN_KEY, appId()!); } catch (e) { /* the title screen shows again */ }
  location.reload();
}

export function Overlay({ text, button, className, boxRef, before, children }: {
  text: string; button?: string; className?: string; boxRef?: Ref<HTMLDivElement>; before?: ReactNode; children?: ReactNode;
}) {
  return (
    <div className="shim-overlay">
      <div className={'shim-overlay-box' + (className ? ' ' + className : '')} ref={boxRef}>
        {before}
        <p>{text}</p>
        {button && <button onClick={runAgain}>{button}</button>}
        {children}
      </div>
    </div>
  );
}

/** Shows a message over the stage, with a Run again button if `button` names it; remove() takes it away. */
export function overlay(text: string, button?: string) {
  return { remove: openLayer(<Overlay text={text} button={button} />) };
}
