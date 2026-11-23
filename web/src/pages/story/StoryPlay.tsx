// The story's play bar: always one tap from reading to playing. It names the app the story last scrolled past (a
// link to its page in the words or panels), or all 20 before the first one, and steps aside at the finale's own buttons.
// Scrolling measures nothing: intersection observers say which links are above the reading line, whether the finale's
// buttons are up, and (wide screens) what sits under the bar, so a phone's scroll never waits on a forced layout.
// shortcut: the rootMargin roots assume the page itself scrolls (main is not a scroll box); revisit if that changes.
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { APPS, webp, type App } from '../../shared/catalog';

const PAGE = /^([A-Za-z0-9]+)\/index\.html(#.*)?$/;   // its page, or its source on it
const ABOVE = '100000px 0px';   // a root reaching far above the screen: "intersecting" means "above this edge"
const WIDE = '(min-width: 761px)';   // where the bar tucks (story.css)

const appOf = (a: Element) => { const m = PAGE.exec(a.getAttribute('href')!); return m && APPS.find(x => x.id === m[1]) || null; };

/** Calls back with the elements matching sel (under main, found again as the page adds some) that intersect the
 *  screen's box grown or shrunk by margin. Returns the cleanup. */
function watch(sel: string, margin: string, back: (on: Set<Element>, all: Element[]) => void) {
  const on = new Set<Element>();
  let all: Element[] = [];
  const io = new IntersectionObserver(es => {
    for (const e of es) e.isIntersecting ? on.add(e.target) : on.delete(e.target);
    back(on, all);
  }, { root: document, rootMargin: margin });   // this page's own screen, also when it sits in a frame
  const scan = () => { all = Array.from(document.querySelectorAll(sel)); all.forEach(el => io.observe(el)); };
  const mo = new MutationObserver(scan);   // the chain panel's links come after a fetch
  scan();
  mo.observe(document.querySelector('main')!, { childList: true, subtree: true });
  return () => { io.disconnect(); mo.disconnect(); };
}

export function StoryPlay() {
  const [app, setApp] = useState<App | null>(null);
  const [away, setAway] = useState(false);   // the finale's buttons are on screen
  const [size, setSize] = useState(0);   // counts window resizes
  const [tuckAt, setTuckAt] = useState('');   // the bar (app, window size) last found over a caption or tile
  const at = (app ? app.id : '') + '|' + size;
  const tuck = tuckAt === at;   // only its round Play shows (wide screens, story.css)
  const bar = useRef<HTMLAnchorElement>(null);
  useEffect(() => {
    const stops = [
      // The last app link above the reading line (60% down the screen), if any.
      watch('main section a[href*="/index.html"]', ABOVE + ' -40% 0px', (on, all) => {
        let found: App | null = null;
        for (const a of all) if (on.has(a)) found = appOf(a) || found;
        setApp(found);
      }),
      watch('.finale .go', ABOVE + ' 0px 0px', on => setAway(on.size > 0)),
    ];
    let queued = 0;
    const onResize = () => { cancelAnimationFrame(queued); queued = requestAnimationFrame(() => setSize(n => n + 1)); };
    addEventListener('resize', onResize);
    return () => { stops.forEach(f => f()); removeEventListener('resize', onResize); cancelAnimationFrame(queued); };
  }, []);
  // Tucking watches a root the size of the whole bar (whole in this render: tuckAt names an older one), measured by
  // its offsets, which leave out its slide away and its entry animation.
  useLayoutEffect(() => {
    if (!matchMedia(WIDE).matches) return;
    const el = bar.current!;
    const px = (n: number) => -Math.round(n) + 'px';
    return watch('main .caption, main .panel-head, main .tile',
      [px(el.offsetTop), px(innerWidth - el.offsetLeft - el.offsetWidth), px(innerHeight - el.offsetTop - el.offsetHeight), px(el.offsetLeft)].join(' '),
      on => setTuckAt(on.size ? at : ''));
  }, [at]);
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
