// compare.html's top bar and intro, written into the page itself by the build (the compare route's placeholder in
// src/routes.tsx), so a phone shows them while React and the page's module load; Compare.tsx renders the rest after
// them. The page is one file for every ?app=, so it is written for the default app and the small script after the
// intro (introScript) puts the asked app's name in before the first paint. No CSS import and nothing that needs a
// browser at import time: the build reads this file in Node.
import { APPS, type App } from '../../shared/catalog';
import { SITE } from '../../shared/ShareTags';

export const WITH_THEN = APPS.filter(a => a.then);

/** The app compare.html?app=<asked> shows: the asked one if it has a Then version, else Flappy Bird. */
export const pick = (asked: string | null) =>
  WITH_THEN.find(a => a.id === asked) || WITH_THEN.find(a => a.id === 'FlappyBird') || WITH_THEN[0];

const ORIGIN = ['code recovered from the old jar', 'author’s own source'];
const origin = (a: App) => +(a.origin === 'original');

/** The page as written: the link home and to the story, then the kicker, title and lead (the app's name and origin
 *  marked for introScript) and #compare-rest, where Compare.tsx renders the players and the changes. */
export function Intro() {
  const app = pick(null), name = <span data-app="">{app.name}</span>;
  return (
    <>
      <header className="compare-top">
        <a className="home" href="index.html">{SITE}</a>
        {' '}
        <a href="story.html">The story</a>
      </header>
      <main className="compare">
        <div id="compare-intro" style={{ display: 'contents' }}>
          <p className="kicker">Then and now</p>
          <h1>{name}, then and now</h1>
          <p className="lead">
            <span className="wide">On the left, {name} as it ran when it was first brought back: the{' '}
              <span data-origin="">{ORIGIN[origin(app)]}</span> with light bug fixes. On the right, the same app today, over
              the same spot. One Play starts both: drag the line to wipe from one to the other, and click the side you want
              the keys.</span>
            <span className="narrow">Flip between {name} as it ran when it was first brought back (Then) and the same app
              today (Now). Each keeps its own game while you flip.</span>
          </p>
        </div>
        <script dangerouslySetInnerHTML={{ __html: introScript() }} />
        <div id="compare-rest" style={{ display: 'contents' }} />
      </main>
    </>
  );
}

/** Runs right after the written intro: swaps the default app's name (and origin) for the asked app's. */
export function introScript() {
  const names: Record<string, [string, number]> = {};
  WITH_THEN.forEach(a => { names[a.id] = [a.name, origin(a)]; });
  return '(function(){var m=' + JSON.stringify(names) + ',o=' + JSON.stringify(ORIGIN) +
    ',a=m[new URLSearchParams(location.search).get("app")];if(!a)return;' +
    'document.querySelectorAll("#compare-intro [data-app]").forEach(function(e){e.textContent=a[0]});' +
    'document.querySelectorAll("#compare-intro [data-origin]").forEach(function(e){e.textContent=o[a[1]]})})()';
}
