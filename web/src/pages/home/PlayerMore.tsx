// The player bar's More: the site-wide looks and the less used tools, in a menu that drops from the bar, as
// More does on an app page's band (runtime/toolbar-more.tsx): the same ⋯ button, look (shim.css) and keys.
import { useEffect, useRef, useState, type KeyboardEvent, type MouseEvent } from 'react';
import { menuKeys } from '../../shared/menu-keys';
import { H } from './model';
import * as P from './player';

type Open = (open: boolean, byKey?: boolean | 'last') => void;
const items = () => Array.prototype.slice.call(document.querySelectorAll('#player-more .shim-more-item:not([hidden])')) as HTMLElement[];
const toggle = () => document.getElementById('player-more-toggle')!;

/** The ⋯ More button, at the end of the bar's buttons. */
export function MoreButton({ open, setOpen }: { open: boolean; setOpen: Open }) {
  return (
    <button className="btn" type="button" id="player-more-toggle" aria-haspopup="menu" aria-expanded={open} aria-controls="player-more" data-sound="none"
      title="More: Arcade mode, Your skin, Fit, Pop out and Source" onClick={(e: MouseEvent) => setOpen(!open, e.detail === 0)}
      onKeyDown={(e: KeyboardEvent) => {
        if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && !open) { e.preventDefault(); setOpen(true, e.key === 'ArrowUp' ? 'last' : true); }
      }}>
      <span aria-hidden="true">{'⋯'}</span> More
    </button>
  );
}

/** Opens and closes the menu, with the band's sound, focus and click-away. */
export function useMore(): [boolean, Open, { right: number; top: number }] {
  const [open, set] = useState(false);
  const [at, setAt] = useState({ right: 0, top: 0 });
  const focus = useRef<boolean | 'last'>(false);
  const setOpen: Open = (o, byKey = false) => {
    if (o === open) return;
    if (o) {
      // Under the button, its right edges lined up.
      const bar = toggle().closest('.player-bar')!.getBoundingClientRect(), box = toggle().getBoundingClientRect();
      setAt({ right: Math.max(4, Math.round(bar.right - box.right)), top: Math.round(box.bottom - bar.top + 6) });
      focus.current = byKey;
      if (window.oldjavaSound) window.oldjavaSound.play('open');
    }
    set(o);
  };
  useEffect(() => {
    if (!open) return;
    const l = items();
    if (focus.current && l.length) l[focus.current === 'last' ? l.length - 1 : 0].focus();
    const away = (e: Event) => {
      const t = e.target as Node;
      if (!toggle().contains(t) && !document.getElementById('player-more')!.contains(t)) set(false);
    };
    const shut = () => set(false);   // a click in the player's app, or another window
    document.addEventListener('pointerdown', away, true);
    addEventListener('blur', shut);
    return () => { document.removeEventListener('pointerdown', away, true); removeEventListener('blur', shut); };
  }, [open]);
  return [open, setOpen, at];
}

const What = ({ icon, text }: { icon?: string; text: string }) => (
  <span className="shim-more-what">{icon && <span aria-hidden="true">{icon}</span>}{text}</span>
);

/** The menu itself; each entry keeps the id the rest of the page (Find, the skin maker) knows it by. */
export function MoreMenu({ open, setOpen, at, zoom, arcade, source }: {
  open: boolean; setOpen: Open; at: { right: number; top: number }; zoom: boolean; arcade: boolean; source: string;
}) {
  const close = (refocus: boolean) => { setOpen(false); if (refocus) toggle().focus(); };
  return (
    <div className="shim-more-menu" id="player-more" role="menu" aria-label="More player controls" hidden={!open}
      style={{ right: at.right + 'px', top: at.top + 'px' }} onKeyDown={e => menuKeys(e, items(), close)} onClick={e => close(e.currentTarget.contains(document.activeElement))}>
      <button className="shim-more-item" type="button" role="menuitemcheckbox" tabIndex={-1} id="player-arcade" aria-checked={arcade}
        onClick={e => P.arcadeClick(e.currentTarget)}>
        <What text="Arcade mode" /><small>Neon CRT arcade look for every app (the Arcade skin)</small>
      </button>
      <button className="shim-more-item" type="button" role="menuitem" tabIndex={-1} id="player-own-skin" data-skin-maker="" data-sound="none">
        <What icon="🎨" text="Your skin" /><small>Make your own skin for every page and app: a color, a desk, corners and windows</small>
      </button>
      <button className="shim-more-item" type="button" role="menuitemcheckbox" tabIndex={-1} id="player-zoom" aria-checked={zoom} onClick={e => P.zoomClick(e.currentTarget)}>
        <What icon="⤢" text="Fit" />
        <small>{zoom ? 'Small apps are zoomed to fill the player. Click for their actual size.' : 'Apps are shown at their actual size. Click to zoom small ones to fill the player.'}</small>
      </button>
      <button className="shim-more-item" type="button" role="menuitemcheckbox" tabIndex={-1} id="player-pop" aria-checked={H.pop.on} data-sound="none" hidden={!H.pop.shown}
        onClick={P.popClick}>
        <What icon="⧉" text={H.pop.on ? 'Bring back' : 'Pop out'} />
        {H.pop.synced && <small>{H.pop.on ? 'Bring the app back into the player' : 'Play this app in its own small window that stays on top of your other windows'}</small>}
      </button>
      <a className="shim-more-item" role="menuitem" tabIndex={-1} id="player-source" href={source + '#source'}>
        <What icon="</>" text="Source" /><small>Open the app's page with its Java source beside it</small>
      </a>
    </div>
  );
}
