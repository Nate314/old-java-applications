import { embedded } from './skin';
import { toolbar } from './install';
import { centerWindows, stageEdge } from './band-news';
import { bringBack } from './pop-out';
import { replay, setReplay, startReplay } from './replay';
import { setPhotoDesk } from './photo-booth';
import { passport, stampOnPlay, stampToast } from './passport';
import { watchApp, watchWanted } from './watch';
import { saverHost } from './saver';
import { padInPlayer } from './touch-pad';
import { gamepads } from './gamepads';
import { appId } from './catalog';
import { programFile } from './then-and-now';
import { exitScreen, howToPlay, sourceFromHash } from './source-viewer';
import { pretendWindows } from './pretend-windows';
import { AGAIN_KEY, followTitle } from './records';
import { clockFollows, clockOpen, clockPage } from './clock-follows';
import { preload, titleScreen } from './title-screen';
import { overlay } from './ui/overlay';
import { showConsole } from './system-out';
import { playSound } from './sounds';
import { whenShown } from './toolbar-more';



export function init() {
  document.addEventListener('DOMContentLoaded', function () {
    var body = document.body;
    var isApp = body.hasAttribute('data-app');
    if (isApp && !embedded) document.documentElement.setAttribute('data-stage', '');
    if (!embedded) toolbar(isApp);
    // The player's iframe leaves the toast to the home page around it, which hears the stamp too. An app page
    // shows only its own stamp; the home page shows any.
    if (!embedded) passport.onChange(function (id: string | null) { if (id && (!isApp || id === appId())) stampToast(id); });
    gamepads(isApp);
    if (!embedded) whenShown(clockFollows);   // a prerendered page must not take the bell from the one in view
    if (!isApp) return;
    if (!embedded) sourceFromHash();

    var desktop = document.createElement('div');
    desktop.id = 'desktop';
    body.insertBefore(desktop, body.firstChild);
    // <body data-own-layout> (CircleSpawner): the app places its windows on the screen itself, and recentering
    // them as a group while a shape grows in, one window at a time, would slide the whole group about.
    if (!document.documentElement.hasAttribute('data-shot') && !body.hasAttribute('data-own-layout')) centerWindows(desktop);
    if (body.hasAttribute('data-tab-title') && !embedded) followTitle(desktop);
    var saverMode = document.documentElement.hasAttribute('data-saver');
    if (embedded && window.parent !== window && !saverMode) padInPlayer();
    if (body.hasAttribute('data-console')) {
      var out = showConsole();
      if (!embedded) stageEdge(out, '--out-h');
    }

    var files = (body.getAttribute('data-files') || '').split(/\s+/).filter(Boolean);
    var args = (body.getAttribute('data-args') || '').split(/\s+/).filter(Boolean);
    // <body data-args-page> (UltimateTicTacToe, SynthesizerKeyboard): main also gets the page's address, to build
    // links to itself (a game with a friend travels in ?game=, a recorded tune in ?tune=, see the apps' READMEs) and
    // to read what a link brought.
    if (body.hasAttribute('data-args-page')) args.push(location.href);
    var extras = !document.documentElement.hasAttribute('data-shot') && !saverMode;
    var again = null;
    try {
      again = sessionStorage.getItem(AGAIN_KEY);
      sessionStorage.removeItem(AGAIN_KEY);
    } catch (e) { /* no Run again shortcut */ }
    // The player's iframe starts at once (the visitor's click on the home page started it).
    var watch = extras && watchWanted();
    var title = extras && !embedded && !watch && again !== appId() && typeof main === 'function' ? titleScreen(files) : null;
    // Prerendered (instant open): the page had its own copy of sessionStorage until it is opened, so the shortcut is
    // read again then, both to clear it (the copy's removal is gone) and because a link that skips the title screen
    // (clockbar's Clock chip) may have set it with the click, after this page had loaded.
    if (document.prerendering) document.addEventListener('prerenderingchange', function () {
      try {
        if (sessionStorage.getItem(AGAIN_KEY) !== appId()) return;
        sessionStorage.removeItem(AGAIN_KEY);
      } catch (e) { return; }
      if (title) title.skip();
    }, { once: true });
    var loading = !title && files.length ? overlay('Loading...') : null;
    var shell = body.hasAttribute('data-shell') ? pretendWindows() : null;
    window.__shimExited = function (msg) {
      bringBack();   // the app closed in its own window: its exit screen shows on the page
      if (saverMode) { var host = saverHost(); if (host) host.wake(); return; }   // the mouse moved: back to the page
      function show() { if (extras) exitScreen(msg); else overlay(msg, 'Run again'); }
      if (!shell || !shell.holdExit(show)) show();
    };

    function start() {
      // TeaVM hands main's outcome to this callback. System.exit unwinds the calling thread with an error
      // (web.rt.Exit); from main itself (Windows, which has no window) that is the end of the program, not a crash.
      main(args, function (result) {
        if (result instanceof Error && !/System\.exit$/.test(String(result))) throw result;
      });
      if (extras && !embedded) playSound('start');   // in the player, the home page plays it when it swaps an app in
      if (extras && !embedded) howToPlay();
      if (watch) watchApp(desktop); else if (extras) stampOnPlay();
      if (extras && !shell && !replay) setReplay(startReplay(desktop));
      if (extras && !shell) {   // the photo booth draws the app's windows (pretend Windows is drawn by the page)
        setPhotoDesk(desktop);
        document.documentElement.setAttribute('data-photo', '');
        if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaPhoto: true }, location.origin);
      }
      if (clockPage() && !saverMode) clockOpen();
    }
    Promise.all(files.map(function (f) {
      return title ? preload(f).then(function () { title!.loaded(f); }) : preload(f);
    })).then(function () {
      if (loading) loading.remove();
      if (typeof main !== 'function') {
        overlay(programFile() + ' is missing. Build the site with Maven first (see web/README.md).');
        return;
      }
      if (title) title.ready(start); else whenShown(start);   // no app runs unseen in a prerendered page
    });
  });
}
