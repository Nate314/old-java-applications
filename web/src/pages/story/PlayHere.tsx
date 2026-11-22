// Play without leaving the story: a click on any link to an app's page (the play bar, the 20 tiles, the chain's bars,
// Pick one for me) opens that app over the story, in its own page in embed mode, as the home page's player does.
// Closing it puts the reader back where they were. A link to a place on the page (#source), a modified click
// (new tab) or a click the page already handled still goes to the app's page.
import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import { APPS, type App } from '../../shared/catalog';
import { frameFit } from '../../shared/frame-fit';

const PAGE = /^([A-Za-z0-9]+)\/index\.html$/;

export function PlayHere() {
  const [app, setApp] = useState<App | null>(null);
  const [size, setSize] = useState<CSSProperties | null>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const stage = useRef<HTMLDivElement>(null);
  const frame = useRef<HTMLIFrameElement>(null);
  const full = useRef<HTMLAnchorElement>(null);

  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      const link = (e.target as Element).closest<HTMLAnchorElement>('a[href]');
      if (!link || link.target || link.closest('.play-here')) return;
      const m = PAGE.exec(link.getAttribute('href')!);
      const a = m && APPS.find(x => x.id === m[1]);
      if (!a) return;
      e.preventDefault();
      setApp(a);
    }
    function onMessage(e: MessageEvent) {
      if (e.origin !== location.origin || e.source !== frame.current?.contentWindow || !e.data) return;
      const next = APPS.find(a => a.id === e.data.oldjavaPlay);   // a suggestion on the exit screen
      if (next) setApp(next);
      // Share and a replay clip belong to the full page: point at it, as play.html does.
      else if (typeof e.data.oldjavaShare === 'string' || e.data.oldjavaClip) full.current?.focus();
    }
    document.addEventListener('click', onClick);
    addEventListener('message', onMessage);
    return () => { document.removeEventListener('click', onClick); removeEventListener('message', onMessage); };
  }, []);

  function fit() {
    if (!app || !stage.current) return;
    setSize(frameFit(app, stage.current.clientWidth, stage.current.clientHeight));
  }
  useLayoutEffect(() => {
    const d = dialog.current!;
    if (app && !d.open) d.showModal();
    fit();
  }, [app]);
  useEffect(() => {
    addEventListener('resize', fit);
    return () => removeEventListener('resize', fit);
  });

  return (
    <dialog className="play-here" ref={dialog} aria-label={app ? 'Playing ' + app.name : 'Play'} onClose={() => { setApp(null); setSize(null); }}
      onClick={e => { if (e.target === dialog.current) dialog.current.close(); }}>
      <div className="play-here-bar">
        <b>{app ? app.name : null}</b>
        <a className="btn" ref={full} href={app ? app.id + '/index.html' : 'index.html'}>Full page</a>
        <button className="btn primary" type="button" onClick={() => dialog.current!.close()}>Back to the story</button>
      </div>
      <div className="play-here-stage" ref={stage}>
        {/* Gone when closed, so the app stops (and its sound with it). Loaded, it takes the keys from the bar's first link. */}
        {app && (
          <iframe key={app.id} ref={frame} title={app.name} src={app.id + '/index.html?embed'} allow="autoplay; clipboard-write; microphone"
            onLoad={e => e.currentTarget.focus()}
            style={size ?? undefined} />
        )}
      </div>
    </dialog>
  );
}
