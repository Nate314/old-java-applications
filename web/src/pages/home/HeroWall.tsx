// The hero's wall: all twenty apps' windows drifting up beside the title, in the room the lead leaves on a wide
// screen. A click on one plays it. The rail and the gallery are the keyboard's and screen reader's way to the same
// apps, so the wall is decoration for them (aria-hidden, out of the tab order).
import './hero-wall.css';
import { APPS, webp, type App } from '../../shared/catalog';
import { H } from './model';
import * as P from './player';

const COLUMNS = 3;

function Column({ apps }: { apps: App[] }) {
  // Twice over, so the column can move up by half its height and start again with no seam.
  return (
    <ol>
      {[...apps, ...apps].map((a, i) => (
        <li key={i}>
          <button type="button" tabIndex={-1} title={'Play ' + a.name} className={H.current === a ? 'on' : undefined} onClick={() => P.choose(a)}>
            <img src={webp(a.shot, true)} alt="" loading="lazy" />
            <span>{a.name}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

export function HeroWall() {
  return (
    <div className="hero-wall" aria-hidden="true">
      {Array.from({ length: COLUMNS }, (_, c) => <Column key={c} apps={APPS.filter((_, i) => i % COLUMNS === c)} />)}
    </div>
  );
}
