// An app's own page (<App>/index.html) and, for the apps with a Then version, the page that runs it as first
// restored (<App>/then.html): the shell its TeaVM program runs in. Each is static HTML, rendered from this file
// once per app when the site is built (vite/pages.ts), with no React on the page: boot.js (the site's shared
// runtime) builds the page around the app, and classes.js (then.js) is the app compiled with TeaVM.
// Keep this file free of CSS imports: the build reads it in Node.
import { renderToStaticMarkup } from 'react-dom/server';
import type { ReactElement } from 'react';
import type { App } from '../../shared/catalog';
import { EarlyScript, RuntimeScript } from '../../shared/EarlyScript';

const SITE = 'Old Java Applications';

/**
 * What an app's <body> says besides data-app, for boot.js:
 *  - data-files: files its program reads, preloaded from its folder before main() runs
 *  - data-args: its program's arguments (FlappyBird's frame rate, as run.ps1 passes it)
 *  - data-args-page: main also gets the page's address, to build links that reopen it (a tune, a game by link)
 *  - data-own-layout: it places its windows itself, so boot.js does not center them (CircleSpawner)
 *  - data-shell: its program presses Windows keys, so boot.js plays a pretend Windows for them (Windows, WindowsE)
 *  - data-tab-title: the browser tab follows its window title (Clock)
 * Values are attribute values; an empty string is an attribute without a value.
 */
const BODY: Record<string, Record<string, string>> = {
  CircleSpawner: { 'data-own-layout': '' },
  Clock: { 'data-tab-title': '' },
  FlappyBird: { 'data-files': 'bg.jpg bird.png bottomPipe.png ground.jpg topPipe.png', 'data-args': '120' },
  // NathaNOS: its own pictures, a jar of each app on its desktop (Clock.jar is the older one it shipped with), the
  // apps' icons, and the files those apps read from their own folder. Keep in step with web/nathanos/pom.xml.
  NathaNOS: {
    'data-files': [
      'assets/NOS.png assets/NathaNOS.jpg assets/NathaNOS.png assets/background.jpg assets/background.png assets/clock.png assets/cursor.png assets/transparent.png',
      'assets/Clock.jar assets/CircleSpawner.jar assets/FlappyBird.jar assets/Minesweeper.jar assets/RubiksCube2D.jar assets/ScreenSaver.jar assets/SpriteFloatyStars.jar assets/StockChecker.jar assets/SynthesizerKeyboard.jar assets/UltimateTicTacToe.jar assets/GroupCreator.jar assets/MusicTheoryExpert.jar assets/NumberGuess.jar assets/SimpleAlgorithms.jar assets/TankGame.jar assets/BibleDownloader.jar assets/DinoGame.jar',
      'assets/BibleDownloader.png assets/DinoGame.png assets/Minesweeper.png assets/NumberGuess.png assets/RubiksCube2D.png assets/StockChecker.png assets/SynthesizerKeyboard.png assets/UltimateTicTacToe.png',
      'bg.jpg bird.png bottomPipe.png ground.jpg topPipe.png myStocks.txt stockCheck.csv',
    ].join(' '),
  },
  StockChecker: { 'data-files': 'myStocks.txt stockCheck.csv' },
  SynthesizerKeyboard: { 'data-args-page': '' },
  UltimateTicTacToe: { 'data-args-page': '' },
  Windows: { 'data-shell': '' },
  WindowsE: { 'data-shell': '' },
};

/** The page's head: its name, the link preview tags (with the app's card from web/cards.ps1), skins and programs. */
function AppHead({ app }: { app: App }) {
  const from = app.origin === 'original' ? 'Built from the original Java source' : 'Recovered from an old Java jar';
  const description = app.hint + ' ' + from + ' and playable in the browser.';
  return (
    <>
      <meta charSet="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <title>{app.name}</title>
      <meta name="description" content={description} />
      <meta property="og:type" content="website" />
      <meta property="og:site_name" content={SITE} />
      <meta property="og:title" content={app.name + ' | ' + SITE} />
      <meta property="og:description" content={description} />
      <meta property="og:url" content={'../' + app.id + '/index.html'} />
      <meta property="og:image" content={'../cards/' + app.id + '.jpg'} />
      <meta property="og:image:width" content="1200" />
      <meta property="og:image:height" content="630" />
      <meta property="og:image:alt" content={app.name + ', running in the browser'} />
      <meta name="twitter:card" content="summary_large_image" />
      <link rel="stylesheet" href="../shim.css" />
      <EarlyScript />
      <RuntimeScript src="../assets/boot.js" />
      {/* Deferred, so it runs after the runtime module: TeaVM reads $rt_putStdoutCustom as it loads. */}
      <script defer src="classes.js" />
    </>
  );
}

/**
 * The body attributes of an app's Then page (web/then and web/then-nathanos compile its then.js): its program as
 * first restored needs fewer of them, and two of the apps printed to System.out then (data-console).
 */
const THEN_BODY: Record<string, Record<string, string>> = {
  CircleSpawner: { 'data-own-layout': '' },
  FlappyBird: BODY.FlappyBird,
  GroupCreator: { 'data-console': '' },
  // NathaNOS as first restored: its pictures, the jars on its desktop then, their icons and the files they read.
  NathaNOS: {
    'data-files': [
      'assets/NOS.png assets/NathaNOS.jpg assets/NathaNOS.png assets/background.jpg assets/background.png assets/clock.png assets/cursor.png assets/transparent.png',
      'assets/Clock.jar assets/CircleSpawner.jar assets/FlappyBird.jar assets/Minesweeper.jar assets/RubiksCube2D.jar assets/ScreenSaver.jar assets/SpriteFloatyStars.jar assets/StockChecker.jar assets/SynthesizerKeyboard.jar assets/UltimateTicTacToe.jar assets/GroupCreator.jar assets/MusicTheoryExpert.jar assets/NumberGuess.jar assets/SimpleAlgorithms.jar assets/TankGame.jar',
      'assets/Minesweeper.png assets/NumberGuess.png assets/RubiksCube2D.png assets/StockChecker.png assets/SynthesizerKeyboard.png assets/UltimateTicTacToe.png',
      'bg.jpg bird.png bottomPipe.png ground.jpg topPipe.png myStocks.txt stockCheck.csv',
    ].join(' '),
  },
  SimpleAlgorithms: { 'data-console': '' },
  StockChecker: BODY.StockChecker,
  WindowsE: { 'data-shell': '' },
};

/** The head of an app's Then page. */
function ThenHead({ app }: { app: App }) {
  return (
    <>
      <meta charSet="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <title>{app.name + ', as first restored'}</title>
      <meta name="description" content={app.name + ' as it was first restored in 2026: the recovered Java code with light bug fixes, before the later reworks, playable in the browser.'} />
      <link rel="stylesheet" href="../shim.css" />
      <EarlyScript />
      <RuntimeScript src="../assets/boot.js" />
      <script defer src="then.js" />
    </>
  );
}

function page(head: ReactElement, body: Record<string, string>): string {
  const attrs = Object.entries(body).map(([name, value]) => ' ' + name + (value ? '="' + value + '"' : '')).join('');
  return '<!doctype html>\n<html lang="en">\n<head>\n' + renderToStaticMarkup(head) + '\n</head>\n<body' + attrs + '>\n</body>\n</html>\n';
}

/** The whole document for one app. */
export function appPage(app: App): string {
  return page(<AppHead app={app} />, { 'data-app': app.id, ...BODY[app.id] });
}

/** The whole document for an app's Then page. */
export function thenPage(app: App): string {
  return page(<ThenHead app={app} />, { 'data-app': app.id, 'data-then': '', ...THEN_BODY[app.id] });
}
