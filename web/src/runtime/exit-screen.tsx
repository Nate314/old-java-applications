// The exit screen: the box over the stage when the app called System.exit, with its recap (exit-recap.tsx) on top.
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { embedded } from './skin';
import { news } from './band-news';
import { replay } from './replay';
import { ExitRecap } from './exit-recap';
import { played } from './passport';
import { appId, catalog, type CatalogApp } from './catalog';
import { eraPage, indexOf, isThen, picks } from './then-and-now';
import { openShare } from './share';
import { Overlay } from './ui/overlay';
import { openLayer, useOwnKeys } from './ui/layer';
import { AppCard, PassportLine } from './ui/cards';
import { playSound } from './sounds';

// The app called System.exit (for most apps: its window was closed). Offer it again, and three others: only on the
// app's own page and in the home page's player, which swaps a pick in. Any other embed (play.html, ?embed&full on
// someone's page, Side by side) put this one app there, so its frame does not offer others.
function inHomePlayer() {
  try { return window.parent !== window && !!window.parent.document.getElementById('player'); } catch (e) { return false; }   // another site's page
}

export function exitScreen(msg: string) {
  if (news && news.kind() === 'hint') news.clear();
  openLayer(<ExitScreen msg={msg} replayLive={!!(replay && replay.live())} />);
  playSound('exit');
}

function ExitScreen({ msg, replayLive }: { msg: string; replayLive: boolean }) {
  const box = useRef<HTMLDivElement>(null);
  const share = useRef<HTMLButtonElement>(null);
  const [found, setFound] = useState<{ apps: CatalogApp[]; next: CatalogApp[] } | null>(null);
  useOwnKeys(box);
  useLayoutEffect(() => { box.current!.querySelector('button')!.focus(); }, []);
  useEffect(() => { catalog().then(apps => setFound({ apps, next: picks(apps, appId()) })); }, []);
  const app = found ? found.apps[indexOf(found.apps, appId())] : null;
  const next = found ? found.next : [];
  return (
    <Overlay text={msg} button="Run again" className="shim-exit" boxRef={box} before={<ExitRecap name={app && app.name} replayLive={replayLive} />}>
      {!embedded && <a className="shim-exit-home" href="../index.html">All apps</a>}
      <button type="button" className="shim-exit-share" data-sound="none" ref={share} onClick={() => openShare(appId(), share.current)}>{'↗ Share'}</button>
      {app && app.then && !embedded && (   // the player has its own Then | Now switch
        <a className="shim-exit-era" href={eraPage(!isThen())}>{isThen() ? 'Play today’s version' : 'Play it as first restored'}</a>
      )}
      {next.length > 0 && (!embedded || inHomePlayer()) && (
        <>
          <h3>{played[next[0].id] ? 'Play something else' : 'Play something new'}</h3>
          <div className="shim-picks">{next.map(a => <AppCard key={a.id} app={a} showNew />)}</div>
          <PassportLine apps={found!.apps} />
        </>
      )}
    </Overlay>
  );
}
