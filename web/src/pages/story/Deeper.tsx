// Chapter 6's "The apps, deeper", as cards to play: each change HISTORY.md names, on its app's picture (its preview
// clip while hovered or focused, or while the card crosses the middle of a touch screen), opening the app over the
// page (PlayHere) and linking the pull request that made it.
import { useEffect, useRef, useState } from 'react';
import { app, clipMoves, webp } from '../../shared/catalog';

// HISTORY.md, The continued run: "The apps, deeper".
const DEEPER: [string, string, number][] = [
  ['Windows', 'The on-screen Windows key', 97],
  ['ScreenSaver', 'The Maze saver', 98],
  ['MusicTheoryExpert', 'A guitar neck in the chord finder', 100],
  ['Minesweeper', 'Exact mine odds', 102],
  ['RubiksCube2D', 'My cube: paint it to match your own', 104],
  ['Clock', 'Westminster chimes and an alarm hand', 104],
  ['TankGame', 'The battlefield builder', 110],
  ['MusicTheoryExpert', 'Sheet music', 124],
  ['UltimateTicTacToe', 'Where each move sends you', 134],
];
const PR = 'https://github.com/Nate314/old-java-applications/pull/';

export function Deeper() {
  const [live, setLive] = useState<number | null>(null);
  const box = useRef<HTMLOListElement>(null);
  const moves = clipMoves();

  // Touch screens: the card crossing the middle of the screen plays.
  useEffect(() => {
    if (!moves || matchMedia('(hover: hover) and (pointer: fine)').matches || !box.current || !window.IntersectionObserver) return;
    const io = new IntersectionObserver(es => es.forEach(e => {
      const i = Number((e.target as HTMLElement).dataset.i);
      if (e.isIntersecting) setLive(i); else setLive(l => (l === i ? null : l));
    }), { rootMargin: '-45% 0px -45% 0px' });
    box.current.querySelectorAll('li').forEach(li => io.observe(li));
    return () => io.disconnect();
  }, []);

  return (
    <div className="panel deeper">
      <div className="panel-head"><strong>Play what it made</strong><span>opens here, over the story</span></div>
      <ol ref={box}>
        {DEEPER.map(([id, said, pr], i) => {
          const a = app(id);
          return (
            <li key={i} data-i={i} onMouseEnter={() => setLive(i)} onMouseLeave={() => setLive(l => (l === i ? null : l))}
                onFocus={() => setLive(i)} onBlur={() => setLive(l => (l === i ? null : l))}>
              <a href={id + '/index.html'}>
                <span className="deeper-pic">
                  <img src={webp(a.shot, true)} alt="" loading="lazy" />
                  {moves && a.clip && live === i && <video muted loop playsInline autoPlay aria-hidden="true" src={'clips/' + id + '.mp4'} />}
                  <span className="deeper-play" aria-hidden="true">▶</span>
                </span>
                <b>{said}</b>
                <span>{a.name}</span>
              </a>
              <a className="deeper-pr" href={PR + pr} aria-label={'Pull request ' + pr + ', ' + said}>{'#' + pr}</a>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
