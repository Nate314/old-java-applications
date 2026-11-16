import { useLayoutEffect, useRef, useState } from 'react';
import { siteRoot } from './install';
import { WATCH } from './watch';
import { openShare, shareBox } from './share';
import { playSound } from './sounds';
import type { CatalogApp } from './catalog';
import { lazyScript } from './lazy-script';

declare global {
  interface OldjavaShare {
    // Set by init: opens the Share panel with "Put it on your site" already open.
    embed?(id: string | null, opener?: Element): void;
  }
}

let embedOpenNext: boolean;

const embedScript = lazyScript(() => import('./lazy/embed'), () => window.oldjavaEmbed, 'embed.js');

/** Put it on your site, at the end of the Share panel: closed, and built (by embed.js) the first time it opens. */
export function EmbedSection({ id, apps }: { id: string | null; apps: CatalogApp[] }) {
  const box = useRef<HTMLDetailsElement>(null);
  const body = useRef<HTMLDivElement>(null);
  const built = useRef(false);
  const [open] = useState(() => { var o = embedOpenNext; embedOpenNext = false; return o; });   // Find's "Put an app on your site"
  const [embed, setEmbed] = useState<NonNullable<Window['oldjavaEmbed']> | null>(null);
  useLayoutEffect(() => {
    if (!embed) return;
    embed.build(body.current!, { apps: apps, app: id, siteRoot: siteRoot, watchable: Object.keys(WATCH), sound: playSound });
    box.current!.scrollIntoView({ block: 'nearest' });
  }, [embed]);
  return (
    <details className="shim-embed" open={open} ref={box} onToggle={() => {
      if (!box.current!.open || built.current) return;
      built.current = true;
      playSound('open');
      embedScript().then(setEmbed, function (e) { built.current = false; console.warn(e.message); });
    }}>
      <summary data-sound="none"><span aria-hidden="true">{'</>'}</span> Put it on your site</summary>
      {embed && <div className="shim-embed-body" ref={body} />}
    </details>
  );
}

export function init() {
  // ---- embed: put an app on your own site (play.html, built by embed.js) ----
  // The Share panel ends with "Put it on your site", closed: opening it adds embed.js, which builds the
  // <iframe> tag for play.html (the embeddable player) with a live preview. See web/README.md, Embed.
  embedOpenNext = false;
  // Opens the Share panel with "Put it on your site" already open (Find uses it).
  window.oldjavaShare.embed = function (id, opener) {
    if (shareBox) return;
    embedOpenNext = true;
    openShare(id, opener);
  };
}
