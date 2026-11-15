// The exit screen's recap of the run that just ended (exitScreen in source-viewer.tsx shows it at the top): the
// last moments on a muted loop from instant replay (a click opens them to save or share), how long the app ran
// since Play, and the visitor's record line, marked Updated when this run changed it.
import { useEffect, useState } from 'react';
import { replay, openClip, type ReplayClip } from './replay';
import { saveStore } from './then-and-now';
import { storedJson } from './stored';

let since = 0;        // when the app started: Play on the title screen, or the page's start when there is none
let before = '';      // the record line the page opened with

function recordLine() {
  const id = saveStore();
  return window.oldjavaRecords ? window.oldjavaRecords.line(id, storedJson('oldjava.saves.' + id)) : '';
}

/** "37 s", "4 min 12 s", "1 h 5 min". */
export function took(ms: number) {
  const s = Math.max(1, Math.round(ms / 1000)), m = Math.floor(s / 60), h = Math.floor(m / 60);
  return h ? h + ' h ' + (m % 60) + ' min' : m ? m + ' min ' + (s % 60) + ' s' : s + ' s';
}

export function ExitRecap({ name, replayLive }: { name: string | null; replayLive: boolean }) {
  const [clip, setClip] = useState<ReplayClip | null>(null);
  const [url, setUrl] = useState('');
  const [now] = useState(recordLine);
  const [ran] = useState(() => took(Date.now() - since));
  useEffect(() => {
    if (!replayLive || !replay) return;
    let gone = false, made = '';
    replay.take().then(c => {
      if (gone || !c) return;
      made = URL.createObjectURL(c.blob);
      setClip(c);
      setUrl(made);
    });
    return () => { gone = true; if (made) URL.revokeObjectURL(made); };
  }, []);
  const moved = !!now && now !== before;
  return (
    <div className={'shim-recap' + (url ? ' clip' : '')}>
      {url && (
        <button type="button" className="shim-recap-clip" data-sound="none" title="Watch, save or share the last moments of play"
          onClick={e => openClip(clip!, e.currentTarget)}>
          <video src={url} autoPlay muted loop playsInline aria-hidden="true" />
          <span>{'⟲ Your last ' + clip!.seconds + ' s: save or share'}</span>
        </button>
      )}
      <div className="shim-recap-text">
        <h2>{name ? name + ' has closed' : 'It has closed'}</h2>
        <p className="shim-recap-ran">{'You played for ' + ran + '.'}</p>
        {now && (
          <p className={'shim-recap-record' + (moved ? ' moved' : '')}>
            {moved && <strong>{before ? 'Updated' : 'New'}</strong>}
            {now}
          </p>
        )}
      </div>
    </div>
  );
}

export function init() {
  since = Date.now();
  before = recordLine();
  if (!window.MutationObserver) return;
  const html = document.documentElement;
  let titled = false;   // the title screen sets data-title once the program has loaded, after this
  new MutationObserver((_, watch) => {
    if (html.hasAttribute('data-title')) titled = true;
    else if (titled) { since = Date.now(); watch.disconnect(); }   // Play (or Run again's skip) started the app
  }).observe(html, { attributes: true, attributeFilter: ['data-title'] });
}
