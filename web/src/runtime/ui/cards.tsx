// The pieces the runtime's panels share: an app's card, the passport line and the desktop jar's link.
import type { CSSProperties, MouseEvent, ReactNode } from 'react';
import { embedded } from '../skin';
import { played } from '../passport';
import { root } from '../catalog';
import { JAR_JAVA, JAR_KEYS, pageOf } from '../then-and-now';

/**
 * A link to an app's page. In the home page's player it asks the page to swap the app in instead, so the player's
 * title, rail and address stay in step. Played apps get a check on their thumbnail; with showNew, the others get a
 * New badge. text replaces the line under the name; onOpen, given, is what a plain click does instead (the Side by
 * side picker docks the app), and current then marks it with aria-current="true".
 */
export function AppCard({ app, current = false, showNew = false, text, onOpen }: {
  app: OldjavaCatalogApp; current?: boolean; showNew?: boolean; text?: string; onOpen?: () => void;
}) {
  const was = !!played[app.id];
  const link = onOpen ? {
    href: pageOf(app),
    'data-sound': 'none',
    onClick(e: MouseEvent) {
      if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      onOpen();
    },
  } : playProps(app);
  return (
    <a className={'shim-app' + (current ? ' current' : '')} aria-current={current ? (onOpen ? 'true' : 'page') : undefined} {...link}>
      <span className="shim-app-pic">
        <img src={app.shot} alt="" />
        {(was || showNew) && (
          <span className={'shim-badge' + (was ? ' played' : ' new')} title={was ? 'Played' : 'Not played yet'}>{was ? '✓' : 'New'}</span>
        )}
      </span>
      <span className="shim-app-text">
        <strong>{app.name}</strong>
        <span>{text ?? (current ? 'Now playing' : app.hint)}</span>
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

/** Download .jar: the app for the desktop (jarLink in then-and-now.tsx), with a note when it presses real keys. */
export function JarLink({ id, className, children, keysNote }: { id: string; className: string; children: ReactNode; keysNote?: string }) {
  return (
    <a className={className} href={root() + 'downloads/' + id + '.jar'} download={id + '.jar'} data-sound="none"
      title={'The desktop version of this app: run it with java -jar ' + id + '.jar (' + JAR_JAVA + ')' + (JAR_KEYS[id] ? '. ' + JAR_KEYS[id] : '')}>
      {children}
      {keysNote && <span className="shim-source-jar-keys">{keysNote}</span>}
    </a>
  );
}
