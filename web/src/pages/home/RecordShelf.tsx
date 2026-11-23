// Your records as a shelf of trophies: each app's record line (runtime/records.ts) split into what it is ("Your best
// times") and the record itself, set large on a wash of the app's own picture. With nothing saved yet the shelf
// keeps places for three apps that keep records, each a Play button, instead of a line of text.
import { APPS, webp } from '../../shared/catalog';
import './record-shelf.css';

/** "Your best: 12 pipes; today's course: 3" as what ("Your best"), the record ("12 pipes") and the rest. */
export function splitLine(line: string) {
  const semi = line.indexOf('; '), head = semi < 0 ? line : line.slice(0, semi), rest = semi < 0 ? '' : line.slice(semi + 2);
  const colon = head.indexOf(': ');
  return colon < 0 ? { what: '', record: head, rest } : { what: head.slice(0, colon), record: head.slice(colon + 2), rest };
}

// The apps the empty Records panel always named, with what each one keeps (records.ts's own words).
const WAITING: [string, string][] = [['Minesweeper', 'Your best times'], ['FlappyBird', 'Your best'], ['RubiksCube2D', 'Your levels']];

const play = (id: string) => { const open = window.oldjavaPassport.open; if (open) open(id); };

/** One trophy: the app's picture, its name, and its record (or, waiting, the place for one). */
export function Trophy({ id, name, line, waiting }: { id: string; name: string; line: string; waiting?: boolean }) {
  const shot = APPS.find(a => a.id === id)?.shot, { what, record, rest } = splitLine(line);
  return (
    <li className={'trophy' + (waiting ? ' waiting' : '')}>
      <button type="button" title={'Play ' + name} onClick={() => play(id)}>
        <span className="pic" aria-hidden="true">
          {shot && <img className="tone" src={webp(shot, true)} alt="" loading="lazy" />}
          {shot && <img src={webp(shot, true)} alt="" loading="lazy" />}
        </span>
        <span className="words">
          <strong>{name}</strong>
          {what && <span className="what">{what}</span>}
          <span className="record">{record}</span>
          {rest && <span className="rest">{rest}</span>}
        </span>
      </button>
    </li>
  );
}

/** The empty shelf: three apps' places, each a Play button. */
export function WaitingShelf() {
  return (
    <ul className="record-list shelf" id="records-waiting" aria-label="Records you could set">
      {WAITING.map(([id, what]) => {
        const app = APPS.find(a => a.id === id);
        return app ? <Trophy key={id} id={id} name={app.name} line={what + ': Not set yet; Play to set yours'} waiting /> : null;
      })}
    </ul>
  );
}
