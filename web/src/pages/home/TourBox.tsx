// The tour on the home page: its box over the player and the button that starts it (the stops are in tour.ts,
// the tour itself runs in player.ts).
import type { CSSProperties } from 'react';
import { APPS, app } from '../../shared/catalog';
import { H } from './model';
import { TOUR } from './tour';
import * as P from './player';
import { stampCount } from './tabs';

const stopName = (k: number) => app(TOUR[k][0]).name;

/** One bar per tour stop; Back and Next stop are the keyboard's way along, these are for pointing. */
function TourSteps() {
  const t = H.tour;
  return (
    <ol className="tour-steps" id="tour-steps">
      {TOUR.map(([id], i) => {
        const label = 'Stop ' + (i + 1) + ': ' + app(id).name;
        return (
          <li key={id}>
            <button type="button" tabIndex={-1} title={label} aria-label={label} className={t.rendered && (t.done || i < t.stop) ? 'seen' : undefined}
              aria-current={t.rendered && i === t.stop && !t.done ? 'step' : undefined} onClick={() => P.tourGo(i, true)} />
          </li>
        );
      })}
    </ol>
  );
}

/** The tour's box over the player: where you are, the thing to try, and the way along. */
export function TourBox() {
  const t = H.tour, i = t.stop;
  const left = APPS.length - stampCount();
  const last = i === TOUR.length - 1;
  return (
    <div className={'tour' + (t.rendered && (t.touched || t.done) ? ' yours' : '')} id="tour" role="region" aria-label="Tour of the arcade" hidden={!t.on}>
      <TourSteps />
      <p className="tour-line">
        <span className="tour-count" id="tour-count">{!t.rendered ? null : t.done ? 'Tour complete.' : 'Stop ' + (i + 1) + ' of ' + TOUR.length}</span>
        {' '}
        <span id="tour-try" aria-live="polite">{!t.rendered ? null : t.done
          ? 'That was all ' + TOUR.length + ' apps. ' + (left === 0 ? 'You have played every one of them: the passport is full.'
            : 'You have played ' + (APPS.length - left) + ' of ' + APPS.length + '; ' + left + (left === 1 ? ' is' : ' are') + ' still waiting for you.')
          : t.touched ? 'Your turn: play ' + stopName(i) + ' as long as you like, then Next stop.' : TOUR[i][1]}</span>
      </p>
      <div className="tour-actions">
        <button className="btn" type="button" id="tour-back" aria-label="Previous stop" disabled={t.rendered && !t.done && i === 0} onClick={P.tourBackClick}>‹ Back</button>
        {' '}
        <button className="btn primary" type="button" id="tour-next" hidden={t.rendered && t.done} title={t.rendered ? (last ? 'End of the tour' : 'Next: ' + stopName(i + 1)) : undefined}
          onClick={P.tourNextClick}>{t.rendered && last ? 'Finish ›' : 'Next stop ›'}</button>
        {' '}
        <button className="btn" type="button" id="tour-unplayed" hidden={!(t.rendered && t.done && left > 0)} onClick={P.tourUnplayedClick}>Play one you haven't</button>
        {' '}
        <a className="btn" id="tour-story" href="story.html" hidden={!(t.rendered && t.done)}>Read the story</a>
        {' '}
        <button className="btn" type="button" id="tour-end" onClick={P.tourEndClick}>{t.rendered && t.done ? 'Close' : 'End tour'}</button>
      </div>
      <div className="tour-time" id="tour-time" aria-hidden="true">
        <span style={t.rendered ? { '--t': String(t.done || t.touched ? 0 : Math.min(1, t.spent / P.TOUR_MS)) } as CSSProperties : undefined} />
      </div>
    </div>
  );
}

/** Take the tour, and where a tour left off. */
export function TourStart() {
  const t = H.tour, resume = P.tourResume();
  const [label, note] = t.on
    ? ['▶ Back to the tour', t.done ? 'You have seen all ' + TOUR.length + '.' : 'Stop ' + (t.stop + 1) + ' of ' + TOUR.length + ': ' + stopName(t.stop) + '.']
    : P.tourSaved().done ? ['▶ Take the tour again', 'You have seen all ' + TOUR.length + ' apps. Start again from the first.']
    : resume ? ['▶ Continue the tour', 'Stop ' + (resume + 1) + ' of ' + TOUR.length + ': ' + stopName(resume) + ', where you left off.']
    : ['▶ Take the tour', 'All ' + TOUR.length + ' apps in the player, one at a time, with something to try in each.'];
  return (
    <p className="tour-start">
      <button className="btn primary" type="button" id="tour-start" onClick={P.tourStartClick}>{label}</button>
      {' '}
      <span id="tour-start-note">{note}</span>
    </p>
  );
}
