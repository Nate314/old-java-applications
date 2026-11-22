// play.html with no app (or a wrong name): a channel guide. The app under the pointer or the keyboard fills the
// screen above the tiles, its preview clip playing (Shot.tsx), with its hint and a Play button; a tile or the Play
// plays it right here. The tiles scroll under the feature, which stays put.
import { useState } from 'react';
import { APPS, webp, type App } from '../../shared/catalog';
import { Shot } from './Shot';

export function Picker({ text, onPick }: { text: string; onPick: (a: App) => void }) {
  const [on, setOn] = useState(APPS[0]);
  return (
    <div className="oops" id="oops">
      <div className="poster feature">
        <img className="bg" alt="" src={webp(on.shot)} />
        {' '}
        <Shot key={on.id} app={on} still={false} shown onPlay={() => onPick(on)} />
        <div className="text">
          <p className="kicker" id="oops-text">{text}</p>
          <h1>{on.name}</h1>
          <p className="hint">{on.hint}</p>
          <button className="play" type="button" id="oops-play" onClick={() => onPick(on)}>Play {on.name}</button>
        </div>
      </div>
      <ul className="pick">
        {APPS.map(a => (
          <li key={a.id}>
            <button type="button" className={a === on ? 'on' : undefined} aria-current={a === on || undefined}
              onPointerEnter={() => setOn(a)} onFocus={() => setOn(a)} onClick={() => onPick(a)}>
              <img alt="" src={webp(a.shot, true)} loading="lazy" />
              <span>{a.name}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
