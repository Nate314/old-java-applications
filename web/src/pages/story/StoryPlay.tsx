// The story's play bar: always one tap from reading to playing. It names the app the story last scrolled past (a
// link to its page in the words or panels), or all 20 before the first one, and steps aside at the finale's own buttons.
import { useEffect, useRef, useState } from 'react';
import { APPS, webp, type App } from '../../shared/catalog';

const PAGE = /^([A-Za-z0-9]+)\/index\.html(#.*)?$/;   // its page, or its source on it

/** The app of the last link to an app page above the reading line (60% down the screen), if any. */
function current(): App | null {
  let found: App | null = null;
  const line = innerHeight * 0.6;
  for (const a of document.querySelectorAll<HTMLAnchorElement>('main section a[href*="/index.html"]')) {
    const m = PAGE.exec(a.getAttribute('href')!);
    const r = a.getBoundingClientRect();
    if (!m || !r.height || r.top > line) continue;
    found = APPS.find(x => x.id === m[1]) || found;
  }
  return found;
}

export function StoryPlay() {
  const [app, setApp] = useState<App | null>(null);
  const [away, setAway] = useState(false);   // the finale's buttons are on screen
  const [tuck, setTuck] = useState(false);   // a caption or tile is under it: only its round Play shows (wide screens, story.css)
  const bar = useRef<HTMLAnchorElement>(null);
  const room = useRef<DOMRect | null>(null);   // where the whole bar sits, measured while it is whole
  useEffect(() => {
    let queued = 0;
    function look() {
      queued = 0;
      setApp(current());
      const go = document.querySelector('.finale .go');
      setAway(!!go && go.getBoundingClientRect().top < innerHeight);
      const el = bar.current!;
      // Offsets, not the box on screen: they leave out its slide away and its entry animation.
      if (!el.classList.contains('tuck')) room.current = new DOMRect(el.offsetLeft, el.offsetTop, el.offsetWidth, el.offsetHeight);
      const b = room.current;
      if (!b) return;
      setTuck(Array.from(document.querySelectorAll('main .caption, main .panel-head, main .tile')).some(el => {
        const r = el.getBoundingClientRect();
        return r.bottom > b.top && r.top < b.bottom && r.right > b.left && r.left < b.right;
      }));
    }
    function onScroll() { if (!queued) queued = requestAnimationFrame(look); }
    look();
    addEventListener('scroll', onScroll, { passive: true });
    addEventListener('resize', onScroll);
    return () => { removeEventListener('scroll', onScroll); removeEventListener('resize', onScroll); cancelAnimationFrame(queued); };
  }, []);
  return (
    <a className={'story-play' + (away ? ' away' : '') + (tuck ? ' tuck' : '')} ref={bar} key={app ? app.id : ''} href={app ? app.id + '/index.html' : 'index.html#apps'}
      aria-hidden={away || undefined} tabIndex={away ? -1 : undefined}>
      {app && <img src={webp(app.shot, true)} alt="" />}
      <span className="what">
        <small>{app ? 'From this part of the story' : 'Every app runs right here'}</small>
        <b>{app ? 'Play ' + app.name : 'Play all 20 apps'}</b>
      </span>
      <span className="go" aria-hidden="true">▶</span>
    </a>
  );
}
