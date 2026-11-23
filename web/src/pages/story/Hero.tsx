// The story's top bar and hero, written into story.html itself by the build (the story route's placeholder in
// src/routes.tsx), so a phone shows them before the page's scripts have run; Story.tsx renders the same markup over
// them. No CSS import and nothing that needs a browser at import time: the build reads this file in Node.
import { ShareStory } from './ShareStory';

export const CHAPTERS = [['#originals', 'The originals'], ['#recovery', 'The recovery'], ['#restoration', 'The first restoration'], ['#chain', 'The chain'], ['#numbers', 'In numbers'], ['#continued', 'Continued'], ['#third', 'Third run']];

/** The bar at the top: the chapters, the one you're in marked (at, once the page has measured it), and how far down you are. */
export function TopBar({ at }: { at: { chapter: number; read: number } | null }) {
  return (
    <header className="story-top">
      <a className="home" href="index.html">← Old Java Applications</a>
      <nav aria-label="Chapters">
        {CHAPTERS.map((c, i) => <a key={c[0]} href={c[0]} aria-current={at ? (i === at.chapter ? 'true' : 'false') : undefined}>{c[1]}</a>)}
      </nav>
      {at && at.chapter >= 0
        ? <ShareStory className="btn share" label="Share this chapter" hash={CHAPTERS[at.chapter][0]} name={CHAPTERS[at.chapter][1]} />
        : <ShareStory className="btn share" label="Share" />}
      <span className="bar" id="bar" aria-hidden="true" style={at ? { transform: 'scaleX(' + at.read + ')' } : undefined} />
    </header>
  );
}

/** The story's title, its lead and the chapter list. */
export function Hero() {
  return (
    <header className="hero">
      <p className="kicker">The story</p>
      <h1>Twenty old programs, brought back to play</h1>
      <p>
        These Java desktop programs were written more than ten years ago, and for most of them the source code was gone. This is how they were recovered from what was left, fixed up, and put in your browser, with the real code and the apps themselves along the way.
      </p>
      <p className="inspired">
        {"The source of inspiration for this project is the YouTube video "}
        <a href="https://www.youtube.com/watch?v=DByAR5Su1Yw">I Told Claude to “Make it better” 100 Times (It Went Too Far)</a>
        {" by Nate Herk | AI Automation."}
      </p>
      <ol className="chapters">
        <li><a href="#originals">The originals (2016 and earlier)</a></li>
        <li><a href="#recovery">The recovery (October 2026)</a></li>
        <li><a href="#restoration">The first restoration</a></li>
        <li><a href="#chain">The Claude Code chain</a></li>
        <li><a href="#numbers">The run in numbers</a></li>
        <li><a href="#continued">The continued run</a></li>
        <li><a href="#third">The third run</a></li>
      </ol>
    </header>
  );
}
