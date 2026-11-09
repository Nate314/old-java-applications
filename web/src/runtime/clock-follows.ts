import { setClockChip } from './install';
import { news } from './band-news';
import { floatToast, toastLink } from './passport';
import { appId, root } from './catalog';
import { isThen, pageOf } from './then-and-now';
import { AGAIN_KEY } from './records';

// ---- Clock follows you (clockbar.js) ----
// While Clock's timer, stopwatch or a focus session runs (its saved ~/.clock.txt says so), every other page gets
// clockbar.js: the time in the toolbar, a bell when it ends, and a game offered for a break. The Clock page gets
// it too, for the break's game; it is added once something starts, in this page or another.
const CLOCK_SAVES = 'oldjava.saves.Clock';

const CLOCK_OPEN = 'oldjava.clockOpen';

const CLOCK_RUNNING = /(?:focusEnd|timerEnd|watchStart)=[1-9]/;

export function clockFollows() {
  var added = false;
  function check() {
    var saved = '';
    try { saved = localStorage.getItem(CLOCK_SAVES) || ''; } catch (e) { return; }
    if (added || !CLOCK_RUNNING.test(saved)) return;
    added = true;
    import('./lazy/clockbar').catch(function () { console.warn('clockbar.js did not load'); });
  }
  check();
  window.addEventListener('storage', function (e) { if (e.key === CLOCK_SAVES) check(); });
  if (clockPage()) setInterval(check, 1000);   // this page's own saves raise no storage event
}

export function clockPage() { return appId() === 'Clock' && !isThen(); }   // the first restored Clock has no timer

// Clock running on this page (or in the home page's player) rings for itself, so the other pages stay quiet.
export function clockOpen() {
  function beat() { try { localStorage.setItem(CLOCK_OPEN, String(Date.now())); } catch (e) { /* they ring too */ } }
  beat();
  setInterval(beat, 5000);
  window.addEventListener('pagehide', function () { try { localStorage.removeItem(CLOCK_OPEN); } catch (e) { /* stale soon */ } });
}

export function init() {
  window.oldjavaSite = {
    news: function () { return news; },   // the app page's band news line (null on the home page)
    toast: function (shot, title, line, actions) { return floatToast(shot, title, line, null, actions); },
    toastLink: toastLink,
    pageOf: pageOf,
    root: root,
    appId: appId,
    clockPage: clockPage,
    // A link to an app page that skips its title screen, as Run again does.
    skipTitle: function (id) { try { sessionStorage.setItem(AGAIN_KEY, id); } catch (e) { /* it shows */ } },
    clockChip: setClockChip
  };
}
