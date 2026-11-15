/*
 * Page bootstrap shared by the app index and every app page.
 * - Applies the saved skin (Modern, Classic or Arcade) before first paint and adds the skin picker. A skin
 *   change in one page (the home page, say) reaches every other open page of the site through the storage event.
 * - An app page opened with ?embed (the home page's player) leaves out the toolbar and centers the app's
 *   windows, which most apps put at the top-left corner of the screen. ?embed&shot is the
 *   thumbnail mode web/shots.ps1 uses: square, shadowless windows on a flat magenta desktop, and
 *   ?embed&clip the mode web/clips.ps1 records the gallery's preview clips in.
 * - Any other app page is a stage: a toolbar band across the top with the app's name, and the app's windows
 *   centered in the space left between it and the controller (or output panel) at the bottom, which the shim
 *   reports to Java as the screen, so neither the toolbar nor the controller covers an app window.
 * - On an app page: preloads the files listed in <body data-files>, then calls the TeaVM main().
 * - Provides window.__shimSynth, the Web Audio voices behind javax.sound.midi, and window.__shimMic, the
 *   microphone behind javax.sound.sampled.
 * - Routes System.out to the console by line, and on pages with <body data-console> also to an output panel.
 * - Opens an app page on a title screen (thumbnail, hint, passport stamp and a live boot log of the files
 *   loading); the app starts on the visitor's Play click or key press, so its first sounds can play.
 *   The title screen also shows the app's story from the repo's history (news.js, added when needed), and the
 *   passport keeps when each app was last played (oldjava.lastPlayed) for the home page's What's new.
 * - Keeps visitors moving between apps: an app page's toolbar has a Switch app drawer and a Next link, a
 *   How to play line in the toolbar band (and its ? button) shows the app's hint, and when the app exits the Run again box suggests three apps to
 *   play next. The list of apps is read from apps.json, which the home page's gallery is rendered from too.
 * - Gives the keyboard games an on-screen controller (window.oldjavaPad) for touch screens. Its buttons send
 *   the app's own keys; the home page docks it under its player and passes the presses into the iframe.
 * - Lets a game controller play every app (GAMEPADS): its buttons press the app's keys where it has them, and
 *   a pointer drawn on the page clicks everything else; LB / RB go to the previous / next app.
 * - Keeps the arcade passport (window.oldjavaPassport): a stamp for every app the visitor has played, saved in
 *   localStorage and shared by every open page. A new stamp shows a toast pointing at an app not played yet
 *   (on an app page, in the toolbar band's news line, so nothing floats over the app).
 * - Links the web app manifest and icons and registers the service worker (sw.js), so the site can be
 *   installed and saved apps play offline. The home page's Take it with you panel uses both.
 * - Share (toolbar, exit screen, arcade-complete toast; the home page's player bar and passport) opens a panel
 *   with the page's link preview card (cards/, made by web/cards.ps1), a message with the passport count, the
 *   link and ways to send it. A shared app link (?shared=N) says so on its title screen.
 * - On <body data-shell> pages (Windows, WindowsE) plays the Windows their Robot key presses were meant for: the
 *   Windows key opens a Start menu of every app, Win+E a File Explorer window on the app folders.
 * - Keeps the last 10 to 20 seconds of a running app as video (instant replay): Replay in the toolbar, on the exit
 *   screen and in the home page's player bar shows the clip to watch, save or share (window.oldjavaReplay).
 * - Lets four games play themselves (?watch, WATCH): the app's own key starts its computer player, the app is
 *   muted, and the visitor's first click or key takes over. The home page's player opens on one.
 * - Uses Screen Saver as the site's screen saver (showSaver, window.oldjavaSaver): a page left alone for 3 minutes
 *   fades it in over everything, and the first input fades it out without reaching the app.
 * - Gives the site's own controls and moments short synthesized sounds in the skin's voice
 *   (window.oldjavaSound), with a Sound button in the toolbar to mute them on every open page.
 * - Lets Clock's timer, stopwatch and focus sessions follow the visitor round the site (clockFollows): while one
 *   runs, clockbar.js is added to every page and puts its time in the toolbar, rings when it ends and offers a
 *   game for a break (window.oldjavaSite is what it borrows from this file).
 * - Lets the visitor make a fourth skin, Yours (ownCss here, the maker panel in skinmaker.js), and share it as a
 *   ?skin= link.
 * - Gets the next page ready before the click (instant open, at the end): speculation rules prerender a pointed-at
 *   app page, and the home page's player warms the app it is pointed at into the browser's cache.
 */
import { init as skin } from './skin';
import { init as install } from './install';
import { init as bandNews } from './band-news';
import { init as popOut } from './pop-out';
import { init as replay } from './replay';
import { init as photoBooth } from './photo-booth';
import { init as passport } from './passport';
import { init as watch } from './watch';
import { init as saver } from './saver';
import { init as touchPad } from './touch-pad';
import { init as gamepads } from './gamepads';
import { init as catalog } from './catalog';
import { init as thenAndNow } from './then-and-now';
import { init as share } from './share';
import { init as embed } from './embed';
import { init as sourceViewer } from './source-viewer';
import { init as records } from './records';
import { init as clockFollows } from './clock-follows';
import { init as systemOut } from './system-out';
import { init as midi } from './midi';
import { init as mic } from './mic';
import { init as sounds } from './sounds';
import { init as start } from './start';
import { init as oneMachine } from './one-machine';
import { init as find } from './find';
import { init as skinMaker } from './skin-maker';
import { init as instantOpen } from './instant-open';
import { init as sideBySide } from './side-by-side';
import { init as toolbarMore } from './toolbar-more';

// In the order boot.js ran them.
skin();
install();
bandNews();
popOut();
replay();
photoBooth();
passport();
watch();
saver();
touchPad();
gamepads();
catalog();
thenAndNow();
share();
embed();
sourceViewer();
records();
clockFollows();
systemOut();
midi();
mic();
sounds();
start();
oneMachine();
find();
skinMaker();
instantOpen();
sideBySide();
toolbarMore();
