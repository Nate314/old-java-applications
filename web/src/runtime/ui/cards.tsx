// The pieces the runtime's panels share: an app's card and the passport line. The DOM versions in
// then-and-now.ts (appCard, passportLine) stay for the parts not yet in React.
import type { CSSProperties, MouseEvent } from 'react';
import { embedded } from '../skin';
import { played } from '../passport';
import { pageOf } from '../then-and-now';

/**
 * A link to an app's page. In the home page's player it asks the page to swap the app in instead, so the player's
 * title, rail and address stay in step. Played apps get a check on their thumbnail; with showNew, the others get a
 * New badge.
 */
export function AppCard({ app, current = false, showNew = false }: { app: OldjavaCatalogApp; current?: boolean; showNew?: boolean }) {
  const was = !!played[app.id];
  return (
    <a className={'shim-app' + (current ? ' current' : '')} aria-current={current ? 'page' : undefined} {...playProps(app)}>
      <span className="shim-app-pic">
        <img src={app.shot} alt="" />
        {(was || showNew) && (
          <span className={'shim-badge' + (was ? ' played' : ' new')} title={was ? 'Played' : 'Not played yet'}>{was ? '✓' : 'New'}</span>
        )}
      </span>
      <span className="shim-app-text">
        <strong>{app.name}</strong>
        <span>{current ? 'Now playing' : app.hint}</span>
      </span>
    </a>
  );
}

/** What makes a link open the app (playLink in then-and-now.ts): in the home page's player, by asking it to. */
export function playProps(app: { id: string }) {
  if (!(embedded && window.parent !== window)) return { href: pageOf(app) };
  return {
    href: pageOf(app),
    'data-sound': 'none',   // the home page plays the new app's start
    onClick(e: MouseEvent) {
      if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      window.parent.postMessage({ oldjavaPlay: app.id }, location.origin);
    },
  };
}

/** "Passport: 4 of 16 apps played" and a bar, for the drawer and the exit screen. */
export function PassportLine({ apps }: { apps: { id: string }[] }) {
  const count = apps.filter(a => played[a.id]).length;
  return (
    <div className="shim-passport">
      <span>{'Passport: ' + count + ' of ' + apps.length + ' apps played'}</span>
      <span className="shim-meter" style={{ '--p': String(count / apps.length) } as CSSProperties} />
    </div>
  );
}
