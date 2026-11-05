// The site's pages. Each route is a React page (src/pages/<page>) with the head it is served with; the build
// (vite/pages.ts) writes <file> from it, so the HTML files exist only in the built site. Keep this file free of
// CSS and component imports: the build reads it in Node.
import type { ReactNode } from 'react';
import { SKIN_SCRIPT } from './shared/skin';
import apps from './shared/apps.json';

export interface Route {
  /** The file the build writes, at the site root. */
  file: string;
  /** The page's module (it renders the page and runs its behavior). */
  entry: string;
  /** What goes in <head>, before the page's own stylesheet, which the build adds. */
  head: ReactNode;
  /** Classic scripts that run after the page's module, in order (deferred). */
  after?: string[];
  /** Shown in #root until the page's module replaces it. */
  placeholder?: ReactNode;
  /** Served at any depth (the lost page): the build inlines its styles and loads its module from the site root. */
  anyDepth?: boolean;
}

const SITE = 'Old Java Applications';
const ICON = <link rel="icon" href="icons/icon.svg" type="image/svg+xml" />;
const skin = <script dangerouslySetInnerHTML={{ __html: SKIN_SCRIPT }} />;
const homeDescription = 'Java Swing and AWT programs recovered from old jars, compiled to JavaScript with TeaVM: play ' +
  apps.length + ' of them in the browser, installed as an app or offline.';

export const ROUTES: Route[] = [
  // The home page: the player, Your arcade and the gallery. boot.js (the site's shared runtime, as on every app
  // page) runs before it; qr.js, saves.js and news.js after it. The share tags point at the card web/cards.ps1
  // renders, as the app pages' do (src/pages/app/AppPage.tsx); -Dsite.url makes og:url and og:image absolute
  // (web/apps/pom.xml).
  {
    file: 'index.html',
    entry: 'src/pages/home/main.tsx',
    head: (
      <>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>{SITE}</title>
        <meta name="description" content={homeDescription} />
        <meta property="og:type" content="website" />
        <meta property="og:site_name" content={SITE} />
        <meta property="og:title" content={SITE} />
        <meta property="og:description" content={homeDescription} />
        <meta property="og:url" content="index.html" />
        <meta property="og:image" content="cards/index.jpg" />
        <meta property="og:image:width" content="1200" />
        <meta property="og:image:height" content="630" />
        <meta property="og:image:alt" content={'Thumbnails of six of the ' + apps.length + ' apps'} />
        <meta name="twitter:card" content="summary_large_image" />
        <link rel="stylesheet" href="shim.css" />
        {/* The first frame waits for the player, so an app page's stage can shrink into it (see boot.js, one machine). */}
        <link rel="expect" href="#player-frame" blocking="render" />
        <script src="boot.js" />
      </>
    ),
    after: ['qr.js', 'saves.js', 'news.js'],
  },
  // The embeddable player: play.html?app=<App> goes in an iframe on any other site (the Share panel's "Put it on
  // your site" writes the tag, see embed.js and web/README.md, Embed). It shows a poster first (the app's
  // thumbnail, name, hint and a Play button), so a page with the widget on it loads one picture and no Java until
  // the visitor asks; Play runs <App>/index.html?embed in its own frame. A slim bar names the arcade and links to
  // the app's full page and to all 20 apps, in a new tab. Options: &then (the app as first restored, where it has a
  // Then version) and &watch (no poster: the computer is already playing, for the apps that can watch). The app
  // pages talk to their parent the way they talk to the home page's player, so an exit screen's suggestion swaps
  // that app in here too. It has its own colors, so no shim.css.
  {
    file: 'play.html',
    entry: 'src/pages/play/main.tsx',
    head: (
      <>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>{SITE}</title>
        <meta name="description" content={'An old Java app, playable right here. Part of ' + SITE + ': 20 Java Swing and AWT programs recovered from old jars and running in the browser.'} />
        {ICON}
      </>
    ),
  },
  // The story: the repo's HISTORY.md told on the site, chapter by chapter, with the things it talks about next to
  // the words. The originals (the 2016 quotes StockChecker saved, read from StockChecker/stockCheck.csv), the
  // recovery (Flappy Bird's Bird class as decompiled, renamed in place token by token), the first restoration (the
  // embeddable player, play.html, on a Then version with a Then | Now switch), the chain (counted from history.txt,
  // the git log the build writes for What's new) and the run in numbers (agents-timeline.svg, drawn from the
  // session's usage numbers; the numbers in its text are copied from HISTORY.md). It wears the visitor's skin from
  // shim.css but does not load boot.js (that file is for the home and app pages). The facts in the text come from
  // HISTORY.md; keep the two in step. See web/README.md, The story.
  {
    file: 'story.html',
    entry: 'src/pages/story/main.tsx',
    head: (
      <>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>{'The story: ' + SITE}</title>
        <meta name="description" content="How 20 Java Swing and AWT programs written more than ten years ago were recovered from old jars, fixed up and brought to the browser, told with their real code and the apps themselves." />
        {ICON}
        <link rel="stylesheet" href="shim.css" />
        {skin}
      </>
    ),
  },
  // Lost and found: the page GitHub Pages (and any host that serves a 404.html) shows for an address with nothing
  // at it. Short links: an address that names an app sends the visitor straight to it, keeping the query and the
  // hash (src/pages/notfound/match.ts). Anything else shows the apps closest to what was typed, a search over all
  // 20, and Dino Game in the embeddable player. See web/README.md, Short links and the lost page.
  {
    file: '404.html',
    entry: 'src/pages/notfound/main.tsx',
    anyDepth: true,
    head: (
      <>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="robots" content="noindex" />
        <title>{'Not found: ' + SITE}</title>
        {skin}
      </>
    ),
    placeholder: <main><p className="finding" role="status">Looking for that page…</p></main>,
  },
];
