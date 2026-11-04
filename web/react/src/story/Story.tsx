// The story (story.html): HISTORY.md told with the real code and the apps. chapters.ts runs its panels.
import { APPS } from '../shared/catalog';

// Where each app came from (HISTORY.md, The recovery).
const FROM: Record<string, string> = {
  Clock: 'site', FlappyBird: 'site', Minesweeper: 'site', NathaNOS: 'site', RubiksCube2D: 'site', ScreenSaver: 'site',
  SpriteFloatyStars: 'site', UltimateTicTacToe: 'site',
  CircleSpawner: 'history', DinoGame: 'history', StockChecker: 'history', SynthesizerKeyboard: 'history', Windows: 'history', WindowsE: 'history',
  MusicTheoryExpert: 'source', NumberGuess: 'source', SimpleAlgorithms: 'source', TankGame: 'source',
  BibleDownloader: 'files', GroupCreator: 'files',
};
const GROUPS = [['site', "Published on the author's site, downloaded and decompiled", 'jar on the site'],
                ['history', "Gone from the site, found in its repo's git history", 'jar in old git history'],
                ['source', "From the author's files: the original Eclipse source", 'original source'],
                ['files', "From the author's files: jars, decompiled", 'jar in old files']];

/** The 20 apps, grouped by where each came from. */
function Apps20() {
  return (
    <div className="apps20" id="apps20" aria-label="The 20 apps and where each came from">
      {GROUPS.map(([key, title, from]) => {
        const mine = APPS.filter(a => FROM[a.id] === key);
        return mine.length ? [
          <h3 key={key}>{title + ' (' + mine.length + ')'}</h3>,
          ...mine.map(a => (
            <a key={a.id} href={a.id + '/index.html'}>
              <img src={a.shot} alt="" loading="lazy" />
              <b>{a.name}</b>
              <span className="from">{from}</span>
            </a>
          )),
        ] : null;
      })}
    </div>
  );
}

/** The apps with a Then version, for the Then and now panel; Flappy Bird first shown. */
function EraPick() {
  const withThen = APPS.filter(a => a.then);
  return (
    <select id="era-app" aria-label="App" defaultValue="FlappyBird" hidden={!withThen.length}>
      {withThen.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
    </select>
  );
}

export function Story() {
  return (
    <>
      <header className="story-top">
        <a className="home" href="index.html">← Old Java Applications</a>
        <nav aria-label="Chapters">
          <a href="#originals">The originals</a>
          <a href="#recovery">The recovery</a>
          <a href="#restoration">The first restoration</a>
          <a href="#chain">The chain</a>
          <a href="#numbers">In numbers</a>
        </nav>
        <span className="bar" id="bar" aria-hidden="true" />
      </header>
      <main>
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
          </ol>
        </header>
        <section className="chapter" id="originals" aria-labelledby="h-originals">
          <div className="words">
            <p className="kicker">Chapter 1</p>
            <h2 id="h-originals">The originals</h2>
            <p>
              The author wrote them in high school, while learning programming concepts with Java, as Java Swing and AWT desktop programs: games, toys, music tools and small utilities.
            </p>
            <p>
              Fourteen of them were published as jars and zips on the author's site. The files inside carry timestamps from April 28 to May 16, 2016. ScreenSaver's are the oldest.
            </p>
            <p>
              The other six came from the author's own files, with no dates recorded. Four of those were still the original Eclipse source.
            </p>
            <p>One date survives inside an app: Stock Checker saved its last quotes to a file, and they are from Friday, May 13, 2016.</p>
          </div>
          <div className="show">
            <div className="panel">
              <div className="panel-head"><strong>StockChecker/stockCheck.csv</strong><span id="tape-note">as the app saved it</span></div>
              <div style={{ overflowX: "auto" }}>
                <table className="tape" id="tape">
                  <thead><tr><th>Symbol</th><th>Price</th><th>Change</th><th>Date</th><th>Time</th></tr></thead>
                  <tbody />
                </table>
              </div>
            </div>
            <Apps20 />
          </div>
        </section>
        <section className="chapter" id="recovery" aria-labelledby="h-recovery">
          <div className="words">
            <p className="kicker">Chapter 2</p>
            <h2 id="h-recovery">The recovery</h2>
            <p>
              With the source gone, the code was rebuilt from the compiled programs. The first eight were downloaded from the author's site and decompiled with Vineflower 1.10.1. Six more were no longer on the site, but turned up in the git history of its repo. The last six came from the author's own files.
            </p>
            <p>
              {"Most of the jars had been obfuscated, so the decompiled classes were called "}
              <code>a</code>
              {", "}
              <code>b</code>
              {", "}
              <code>c</code>
              {" and their locals "}
              <code>var1</code>
              {", "}
              <code>var2</code>
              . Every one of those names was replaced with a descriptive English one, without changing any logic.
            </p>
            <p>
              {"Here is Flappy Bird's bird, exactly as it came out of the decompiler. Press "}
              <b>Rename</b>
              {" to watch it become readable: the same code, token for token."}
            </p>
          </div>
          <div className="show">
            <div className="panel">
              <div className="panel-head">
                <strong id="code-file">flappyBird/b.java</strong>
                {' '}
                <span className="count" id="code-count" aria-live="polite" />
                {' '}
                <button className="btn primary" type="button" id="code-go">Rename</button>
              </div>
              <pre className="code" id="code" tabIndex={0} aria-label="Flappy Bird's Bird class" />
            </div>
            <p className="caption">
              {"From the repo's first commit (4fea701, decompiled) and "}
              <a href="https://github.com/Nate314/old-java-applications/pull/1">#1</a>
              {" (renamed). "}
              <a href="FlappyBird/index.html#source">Today's Flappy Bird source</a>
            </p>
          </div>
        </section>
        <section className="chapter" id="restoration" aria-labelledby="h-restoration">
          <div className="words">
            <p className="kicker">Chapter 3</p>
            <h2 id="h-restoration">The first restoration</h2>
            <ul>
              <li>
                <b>A README for every app</b>
                {", with its controls and its known quirks, and "}
                <code>run.ps1</code>
                {" to build and run any of them."}
              </li>
              <li>
                <b>Modernized all 20</b>
                : the bugs each README listed were fixed (Minesweeper's mine count and flood fill, Ultimate Tic-Tac-Toe's winner check and many more), keeping each app recognizably itself.
              </li>
              <li>
                <b>Into the browser</b>
                : 16 of the 20 were compiled to JavaScript with TeaVM, on a hand-written AWT, Swing and MIDI layer that draws the original windows in the page.
              </li>
            </ul>
            <p>
              {"That state is what every app's "}
              <b>Then</b>
              {" version plays today. Try one, then switch to "}
              <b>Now</b>
              {" to see how far it has come since."}
            </p>
          </div>
          <div className="show">
            <div className="panel">
              <div className="panel-head">
                <label htmlFor="era-app"><strong>Then and now</strong></label>
                {' '}
                <EraPick />
                {' '}
                <span className="era" role="group" aria-label="Version">
                  <button className="btn" type="button" id="era-then" aria-pressed="true">Then</button>
                  <button className="btn" type="button" id="era-now" aria-pressed="false">Now</button>
                </span>
              </div>
              <div className="screen">
                <iframe id="era-frame" title="Flappy Bird, then" loading="lazy" allow="autoplay; clipboard-write" data-src={"play.html?app=FlappyBird&then"} />
              </div>
            </div>
            <p className="caption" id="era-note">Then: the recovered code with light bug fixes, as first restored.</p>
          </div>
        </section>
        <section className="chapter" id="chain" aria-labelledby="h-chain">
          <div className="words">
            <p className="kicker">Chapter 4</p>
            <h2 id="h-chain">The Claude Code chain</h2>
            <p>
              From then on, the changes were made by a chain of Claude Code agents. Each got the same task: look at the project and make one bold change that improves the experience. Later they worked in rounds of three at once, one on a game or toy, one on a tool or music app and one on the site, merged by a fourth.
            </p>
            <ol className="phases">
              <li>
                <b>The site around the apps</b>
                a home page with a live player, a passport, a touch controller, skins, sounds, title screens, a source viewer and share cards.
              </li>
              <li>
                <b>The apps themselves</b>
                a deeper pass for every app, until all 20 ran in the browser, even the ones that press keys, on a pretend Windows.
              </li>
              <li>
                <b>Across apps and the whole site</b>
                Then and now, game controllers, screen readers, instant replay, the computer playing the games, and much more app work.
              </li>
            </ol>
            <p>The numbers on the right are counted from the repo's own history, the same one the home page's What's new reads.</p>
          </div>
          <div className="show">
            <div className="tiles">
              <div className="tile"><b id="n-changes">–</b><span>changes</span></div>
              <div className="tile"><b id="n-days">–</b><span id="n-days-label">days</span></div>
              <div className="tile"><b id="n-apps">–</b><span>apps changed</span></div>
            </div>
            <div className="panel">
              <div className="panel-head"><strong>Changes to each app's folder</strong><span>most first</span></div>
              <ol className="bars" id="bars" />
            </div>
            <p className="caption" id="chain-note" />
          </div>
        </section>
        <section className="chapter" id="numbers" aria-labelledby="h-numbers">
          <div className="words">
            <p className="kicker">Chapter 5</p>
            <h2 id="h-numbers">The run in numbers</h2>
            <p>
              The 100 attempts became pull requests #10 to #90: 69 made one at a time, then eleven rounds in which two or three agents built at once and an integrator merged their work, plus the one-off #83 that wrote the history.
            </p>
            <p>
              {"Every agent, and the orchestrator session that started them and read their reports, ran on "}
              <b>Claude Opus 5.5</b>
              {" ("}
              <code>claude-opus-5-5</code>
              ) in Claude Code. The usage of all 124 finished agents was found in their transcripts.
            </p>
            <p>
              Almost all of the tokens are the same context read again from the prompt cache on each turn; the output, what the agents actually wrote, is the smaller number. The orchestrator is counted on its own line, and the agent making the final pull request is not counted.
            </p>
            <p>
              {"That final pull request also replaced the pictures some apps had taken from the apps they imitated (Flappy Bird's art, a Bible app's icon, Chrome's dinosaur, a Windows mine) with original art, and added a "}
              <b>Download .jar</b>
              {" link to every app page, to run each app on the desktop with Java 17 or newer."}
            </p>
          </div>
          <div className="show">
            <div className="panel">
              <div className="panel-head"><strong>Who worked when</strong><a href="agents-timeline.svg">Full size</a></div>
              <a className="timeline" href="agents-timeline.svg">
                <img src="agents-timeline.svg" width="1200" height="640" loading="lazy" alt="Timeline from November 1 to 3, 2026: 69 agents one after another, then eleven rounds of builders and an integrator, the one-off #83, and the output tokens of each pull request, all on Claude Opus 5.5" />
              </a>
            </div>
            <div className="panel" style={{ marginTop: "14px" }}>
              <div className="panel-head"><strong>Tokens</strong><span>from the session's transcripts</span></div>
              <ul className="usage">
                <li><span>Agents (124)</span><b>7,769,877 output</b></li>
                <li><span>of which the 100 attempts (111 agents)</span><b>6,978,117 output</b></li>
                <li><span>Agents, all tokens incl. cache reads</span><b>1,551,728,877</b></li>
                <li><span>Orchestrator</span><b>522,249 output</b></li>
                <li><span>Orchestrator, all tokens incl. cache reads</span><b>181,962,955</b></li>
                <li><span>Tool calls by the agents</span><b>15,299</b></li>
                <li><span>From the first attempt to the last</span><b>about 36 hours</b></li>
              </ul>
            </div>
            <p className="caption">
              {"The 36 hours include a pause of about 7.7 hours while attempt 35's agent was stalled. The breakdown is in "}
              <a href="https://github.com/Nate314/old-java-applications/blob/master/HISTORY.md#the-run-in-numbers">HISTORY.md</a>
              .
            </p>
          </div>
        </section>
        <section className="finale" aria-labelledby="h-finale">
          <h2 id="h-finale">Now it's your turn</h2>
          <p>
            All 20 are on the home page, ready to play. Each one keeps its Then version, its Java source and a .jar for the desktop a click away.
          </p>
          <div className="go">
            <a className="btn primary" href="index.html#apps">Play all 20 apps</a>
            {' '}
            <a className="btn" id="pick" href="index.html">Pick one for me</a>
            {' '}
            <a className="btn" href="https://github.com/Nate314/old-java-applications">The code on GitHub</a>
          </div>
        </section>
      </main>
    </>
  );
}
