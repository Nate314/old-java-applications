// The player: "Play" swaps the app in the iframe instead of leaving the page. #<App> in the address picks
// the app on load, so a link can point straight at one. Ctrl/Shift/middle clicks still open the full page.
// Also the passport, Arcade mode, Fit and the tour, which all drive the player.
import { all, byId, one } from '../../shared/dom';
import { TOUR } from './tour';

const MIN_WIDTH = 900;
const MIN_HEIGHT = 520;
const MIN_SCREEN = 320;   // shortest the screen gets on a short browser window, in page pixels
const ASPECT = 1.6;
// Demo: until the visitor picks an app or takes over, the player shows the games that play themselves
// (boot.js watch, ?watch), the computer at the controls, a new one every REEL_MS while the player is in view.
const REEL_MS = 40000;
// A tour stop moves on by itself after TOUR_MS unless the visitor touches the app.
const TOUR_MS = 30000;
const TOUR_KEY = 'oldjava.tour';

interface TourSaved { stop?: number; done?: boolean }

export function initPlayer(): void {
  const player = byId('player');
  const screen = byId('screen');
  const rail = byId('rail');
  const bigButton = byId('player-big');
  let frame = byId<HTMLIFrameElement>('player-frame');
  const cards = all<HTMLLIElement>('#apps li[data-app]');
  let current: HTMLLIElement | null = null;

  const appOf = (li: HTMLLIElement) => li.getAttribute('data-app') as string;
  const nameOf = (li: HTMLLIElement) => one('strong', li).textContent as string;
  function card(app: string | undefined): HTMLLIElement | null {
    return cards.find(li => appOf(li) === app) || null;
  }
  // Every caller below runs after the player has an app (the page always starts with one).
  const playing = () => current as HTMLLIElement;

  // The screen is 16:10 but no taller than the browser window leaves room for (so the whole app is in view
  // under the player bar). The iframe gets a viewport of at least MIN_WIDTH x MIN_HEIGHT (more for apps
  // with data-min-height, like Rubik's Cube's 700 px window) and is scaled down to the screen.
  // On the big screen it takes whatever room the bar and the rail leave.
  function isBig() { return document.fullscreenElement === player || player.classList.contains('big'); }

  // A phone (the pocket player, see the page's style) gives the iframe the screen's own size instead: scaled
  // down from 900 px, its apps were a third of their size (Minesweeper's tiles 7 px wide), while the app's
  // page fits its windows to a phone-sized screen itself, as it does when opened on a phone.
  const pocket = matchMedia('(max-width: 640px)');
  let fitted = [0, 0];   // the window's size at the last fit()
  let firstView = true;  // no app picked yet

  function fit() {
    const minHeight = Math.max(MIN_HEIGHT, current ? +(current.getAttribute('data-min-height') || 0) : 0);
    const width = screen.clientWidth;
    let height: number;
    fitted = [window.innerWidth, window.innerHeight];
    if (isBig()) {
      screen.style.height = '';
      height = screen.clientHeight;
    } else {
      // Room for the bar and the rail too, so the whole player fits in the window. A phone's screen is as
      // tall as that room, not 16:10, since the phone is taller than wide.
      const around = player.offsetHeight - screen.offsetHeight + (pocket.matches ? 12 : 24);
      let room = window.innerHeight - around;
      // Until the visitor picks an app (which scrolls the player to the top), a phone's screen ends where the
      // first view does, under the title, so the app that is already running is seen whole.
      if (pocket.matches && firstView) room = Math.min(room, window.innerHeight - 12 - (screen.getBoundingClientRect().top + window.scrollY));
      room = Math.max(MIN_SCREEN, room);
      height = Math.round(pocket.matches ? room : Math.min(width / ASPECT, room));
    }
    // (CircleSpawner places its own 380 px window, so its card asks for a wider screen with data-min-width.)
    const scale = pocket.matches ? Math.min(1, width / (current ? +(current.getAttribute('data-min-width') || 0) : 0))
      : Math.min(1, width / MIN_WIDTH, height / minHeight);
    frame.style.width = Math.round(width / scale) + 'px';
    frame.style.height = Math.round(height / scale) + 'px';
    frame.style.transform = scale < 1 ? 'scale(' + scale + ')' : '';
    screen.style.height = height + 'px';
  }

  // The rail's buttons, one per card in the same order (Home.tsx renders them).
  const railButtons = all<HTMLButtonElement>('button', rail);
  railButtons.forEach((b, i) => b.addEventListener('click', () => choose(cards[i])));

  function mark(li: HTMLLIElement) {
    if (current) current.classList.remove('playing');
    current = li;
    li.classList.add('playing');
    const i = cards.indexOf(li);
    railButtons.forEach((b, j) => b.setAttribute('aria-current', String(j === i)));
    // Scroll only the rail (scrollIntoView would also scroll the page).
    const b = railButtons[i];
    const left = b.offsetLeft - rail.offsetLeft;
    if (left < rail.scrollLeft || left + b.offsetWidth > rail.scrollLeft + rail.clientWidth) {
      rail.scrollTo({ left: left - (rail.clientWidth - b.offsetWidth) / 2, behavior: 'smooth' });
    }
  }

  // Then | Now: apps with data-then also play as they were first restored (<App>/then.html), before the reworks.
  const era = byId('player-era');
  const thenButton = byId('player-then');
  const nowButton = byId('player-now');
  let then = false;

  let demo = false;
  let reelTimer = 0;
  function reel() {
    clearTimeout(reelTimer);
    if (!demo) return;
    reelTimer = window.setTimeout(() => {
      const box = screen.getBoundingClientRect();
      if (document.hidden || box.bottom < 0 || box.top > window.innerHeight) { reel(); return; }
      for (let k = 1; k < cards.length; k++) {
        const li = cards[(cards.indexOf(playing()) + k) % cards.length];
        if (window.oldjavaWatch.has(appOf(li))) { play(li, false, false, true); return; }
      }
    }, REEL_MS);
  }
  function endDemo() {
    demo = false;
    clearTimeout(reelTimer);
  }

  function play(li: HTMLLIElement, focus: boolean, asThen?: boolean, watch?: boolean) {
    demo = !!watch;
    reel();
    mark(li);
    const app = appOf(li);
    const name = nameOf(li);
    then = !!asThen && li.hasAttribute('data-then');
    const page = app + (then ? '/then.html' : '/index.html');
    era.hidden = !li.hasAttribute('data-then');
    thenButton.setAttribute('aria-pressed', String(then));
    nowButton.setAttribute('aria-pressed', String(!then));
    player.classList.toggle('then', then);
    byId('player-name').textContent = name;
    const hint = byId('player-hint');
    hint.classList.remove('open');
    hint.textContent = then ? 'As first restored in 2026: the recovered code with light bug fixes, before the later reworks.' : one('.body > span', li).textContent;
    // As first restored: its first known quirk (if its README still listed any), and a link to its page, whose title screen lists them all.
    if (then) window.oldjavaThen.quirks(app).then(items => {
      if (!then || current !== li || !items.length) return;
      const q = document.createElement('span');
      q.className = 'quirk';
      q.appendChild(document.createTextNode(' Known quirk: '));
      window.oldjavaThen.nodes(items[0]).forEach(n => q.appendChild(n));
      hint.appendChild(q);
      if (items.length > 1) {
        const more = document.createElement('a');
        more.href = page;
        more.textContent = ' +' + (items.length - 1) + ' more';
        hint.appendChild(more);
      }
    });
    byId<HTMLAnchorElement>('player-full').href = page;
    byId<HTMLAnchorElement>('player-source').href = page + '#source';
    frame.title = name;
    showPad(app);
    fit();
    // A new iframe each time, so the old app's timers and audio stop with it. A popped out app comes back
    // first, so its window closes with it.
    const api = popApi();
    if (api) api.back();
    replayButton.hidden = true;   // until the new app's page says it is recording
    photoButton.hidden = true;   // until the new app's page says it runs (boot.js photo booth)
    const next = frame.cloneNode(false) as HTMLIFrameElement;
    next.src = page + '?embed' + (watch ? '&watch' : '');
    frame.replaceWith(next);
    frame = next;
    frame.addEventListener('load', syncPop);
    syncPop();
    if (focus) {
      window.oldjavaSound.play('start');
      frame.addEventListener('load', () => frame.focus());
      if (!isBig()) player.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  // Pop out: the app in the player moves, still running, into a small window of its own that stays on top of
  // the visitor's other windows (boot.js popOut). The player's iframe may not open that window, so this page
  // opens it and hands it to the app's page.
  const popButton = byId('player-pop');
  const replayButton = byId('player-replay');
  function popApi(): OldjavaPop | null {
    try { return frame.contentWindow?.oldjavaPop || null; } catch (e) { return null; }
  }
  function syncPop() {
    const api = popApi();
    const on = !!api && api.popped();
    popButton.hidden = !('documentPictureInPicture' in window) || window.top !== window || !api || !api.can();
    popButton.setAttribute('aria-pressed', String(on));
    popButton.textContent = on ? 'Bring back' : 'Pop out';
    popButton.title = on ? 'Bring the app back into the player'
      : 'Play this app in its own small window that stays on top of your other windows';
  }
  popButton.addEventListener('click', () => {
    const api = popApi();
    if (!api) return;
    if (api.popped()) {
      api.back();
      syncPop();
      return;
    }
    documentPictureInPicture.requestWindow(api.size()).then(win => {
      win.addEventListener('pagehide', () => { setTimeout(syncPop, 0); });
      api.into(win);
      syncPop();
    }, e => { console.warn('Pop out:', e); });
  });
  frame.addEventListener('load', syncPop);

  function choose(li: HTMLLIElement, asThen?: boolean) {
    if (tour.on && li !== current) tourEnd();   // picking another app leaves the tour (it can be continued)
    firstView = false;
    history.replaceState(null, '', '#' + appOf(li) + (asThen ? ':then' : ''));
    play(li, true, asThen);
  }
  thenButton.addEventListener('click', () => { if (!then) choose(playing(), true); });
  nowButton.addEventListener('click', () => { if (then) choose(playing(), false); });

  function step(by: number) { choose(cards[(cards.indexOf(playing()) + by + cards.length) % cards.length]); }

  byId('apps').addEventListener('click', e => {
    const a = (e.target as Element).closest('a.play, a.thumb');
    if (!a || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
    e.preventDefault();
    choose(a.closest('li') as HTMLLIElement);
  });

  byId('player-prev').addEventListener('click', () => step(-1));
  byId('player-next').addEventListener('click', () => step(1));
  byId('player-shuffle').addEventListener('click', () => {
    step(1 + Math.floor(Math.random() * (cards.length - 1)));   // any app but the current one
  });

  // Big screen: real fullscreen where the browser allows it, else the player covers the window.
  function setBig(on: boolean) {
    player.classList.toggle('big', on);
    document.body.classList.toggle('locked', on);
    bigButton.setAttribute('aria-pressed', String(isBig()));
    bigButton.textContent = isBig() ? 'Exit big screen' : 'Big screen';
    fit();
  }
  bigButton.addEventListener('click', () => {
    if (document.fullscreenElement) { document.exitFullscreen(); return; }
    if (player.classList.contains('big')) { setBig(false); return; }
    if (player.requestFullscreen) player.requestFullscreen().catch(() => setBig(true));
    else setBig(true);
  });
  document.addEventListener('fullscreenchange', () => { setBig(false); frame.focus(); });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && player.classList.contains('big')) setBig(false);
  });

  byId('player-restart').addEventListener('click', () => play(playing(), true, then));

  const filters = all<HTMLButtonElement>('.filters button');
  filters.forEach(b => {
    b.addEventListener('click', () => {
      const f = b.getAttribute('data-filter');
      filters.forEach(o => o.setAttribute('aria-pressed', String(o === b)));
      cards.forEach(li => { li.hidden = f !== 'all' && li.getAttribute('data-cat') !== f; });
    });
  });

  // The keyboard games get a controller docked under the screen (boot.js builds it). Its presses go into
  // the player's iframe; real key presses there come back to light its buttons. Shown unless turned off.
  const padBox = byId('player-pad');
  const padToggle = byId('player-pad-toggle');
  let padLight: ((which: number, down: boolean) => void) | null = null;
  function showPad(app: string) {
    const pad = window.oldjavaPad;
    const has = pad.has(app);
    padToggle.hidden = !has;
    padLight = has ? pad.render(padBox, app, (type, key) => {
      // A press on the controller takes over from the computer first (boot.js watch).
      if (demo) {
        endDemo();
        frame.contentWindow?.postMessage({ oldjavaWatchTake: true }, location.origin);
      }
      frame.contentWindow?.postMessage({ oldjavaKey: { type, name: key } }, location.origin);
    }) : null;
    padBox.hidden = !has || !pad.wanted(true);
    padToggle.setAttribute('aria-pressed', String(!padBox.hidden));
  }
  padToggle.addEventListener('click', () => {
    padBox.hidden = !padBox.hidden;
    padToggle.setAttribute('aria-pressed', String(!padBox.hidden));
    window.oldjavaPad.remember(!padBox.hidden);
    fit();
    frame.focus();
  });

  // An app's exit screen in the player suggests other apps; picking one asks this page to swap it in.
  window.addEventListener('message', e => {
    if (e.origin !== location.origin || e.source !== frame.contentWindow || !e.data) return;
    const lit = e.data.oldjavaKeyLit;
    if (lit) { if (padLight) padLight(lit.which, !!lit.down); return; }
    if (typeof e.data.oldjavaShare === 'string') { window.oldjavaShare.open(e.data.oldjavaShare || null, frame); return; }
    if (e.data.oldjavaReplay) { replayButton.hidden = false; return; }
    if (e.data.oldjavaPhoto === true) { photoButton.hidden = false; return; }
    if (e.data.oldjavaClip) { window.oldjavaReplay.open(e.data.oldjavaClip, frame); return; }
    if (e.data.oldjavaWatch === 'taken') { endDemo(); return; }
    const li = card(e.data.oldjavaPlay);
    if (li) choose(li);
  });
  // Instant replay: the player's app keeps its last moments (boot.js startReplay); this shows them here.
  replayButton.addEventListener('click', () => {
    let api: OldjavaReplay | undefined;
    try { api = frame.contentWindow?.oldjavaReplay; } catch (e) { /* not loaded */ }
    if (api) api.take().then(clip => { if (clip) window.oldjavaReplay.open(clip, replayButton); });
  });
  // Photo: the player's app draws its windows (boot.js takePhoto in the iframe); the print shows on this page.
  const photoButton = byId('player-photo');
  photoButton.addEventListener('click', () => {
    window.oldjavaPhoto.snap(() => {
      let api: OldjavaPhoto | undefined;
      try { api = frame.contentWindow?.oldjavaPhoto; } catch (e) { /* not loaded */ }
      return api && api.can() ? api.take() : Promise.resolve(null);
    }, screen, photoButton);
  });
  const shareButton = byId('player-share');
  shareButton.addEventListener('click', () => window.oldjavaShare.open(appOf(playing()), shareButton));
  const passportShare = byId('passport-share');
  passportShare.addEventListener('click', () => window.oldjavaShare.open(null, passportShare));

  // The arcade passport: a stamp per app played, here, in the player or on an app page (boot.js keeps them).
  // Unplayed apps are gray stamps and New badges on the cards; played ones get a check here and in the rail.
  const passport = window.oldjavaPassport;
  const stampButtons = all<HTMLButtonElement>('#stamps button');
  stampButtons.forEach((b, i) => b.addEventListener('click', () => choose(cards[i])));

  function renderPassport(fresh: string | null) {
    let count = 0;
    cards.forEach((li, i) => {
      const id = appOf(li);
      const name = nameOf(li);
      const done = passport.has(id);
      if (done) count++;
      li.classList.toggle('played', done);
      one('.badge', li).textContent = done ? 'Played ✓' : 'New';
      railButtons[i].classList.toggle('played', done);
      stampButtons[i].classList.toggle('played', done);
      stampButtons[i].classList.toggle('fresh', id === fresh);
      stampButtons[i].title = done ? name + ': played' : name + ': not played yet';
    });
    const left = cards.length - count;
    byId('passport-ring').style.setProperty('--p', String(count / cards.length));
    byId('passport-count').textContent = count + '/' + cards.length;
    byId('passport-text').textContent =
      count === 0 ? 'Play an app to collect its stamp. ' + cards.length + ' to collect.'
      : left === 0 ? 'Every app played. Arcade complete!'
      : count + ' of ' + cards.length + ' apps played. ' + left + ' to go.';
    byId('passport-new').hidden = left === 0;
  }
  passport.onChange(renderPassport);
  passport.open = id => { const li = card(id); if (li) choose(li); };
  renderPassport(null);

  byId('passport-new').addEventListener('click', () => {
    passport.catalog().then(apps => {
      const next = passport.unplayed(apps, current && appOf(current))[0];
      const li = next && card(next.id);
      if (li) choose(li);
    });
  });
  // Reset asks once more on the button itself, for 4 seconds.
  const resetButton = byId('passport-reset');
  let resetTimer = 0;
  resetButton.addEventListener('click', () => {
    if (resetTimer) {
      clearTimeout(resetTimer);
      resetTimer = 0;
      resetButton.textContent = 'Reset';
      passport.reset();
      return;
    }
    resetButton.textContent = 'Clear all stamps?';
    resetTimer = window.setTimeout(() => { resetTimer = 0; resetButton.textContent = 'Reset'; }, 4000);
  });

  // Fit: small app windows zoom up to fill the player (boot.js centerWindows, in the player's page).
  const zoomButton = byId('player-zoom');
  function syncZoom() {
    const on = window.oldjavaZoom.fit();
    zoomButton.setAttribute('aria-pressed', String(on));
    zoomButton.title = on ? 'Small apps are zoomed to fill the player. Click for their actual size.'
      : 'Apps are shown at their actual size. Click to zoom small ones to fill the player.';
  }
  window.oldjavaZoom.onChange(syncZoom);
  syncZoom();
  zoomButton.addEventListener('click', () => {
    window.oldjavaZoom.set(!window.oldjavaZoom.fit());
    zoomButton.blur();
    frame.focus();
  });

  // Arcade mode: the Arcade skin on this page, its player and every other open page, and back to the
  // skin from before. The button follows the skin however it changes (the picker, another tab).
  const arcadeButton = byId('player-arcade');
  let skinBefore = 'modern';
  function syncArcade() {
    const on = window.oldjavaSkin.get() === 'arcade';
    arcadeButton.setAttribute('aria-pressed', String(on));
    if (!on) skinBefore = window.oldjavaSkin.get();
  }
  new MutationObserver(syncArcade).observe(document.documentElement, { attributes: true, attributeFilter: ['data-skin'] });
  syncArcade();
  arcadeButton.addEventListener('click', () => {
    window.oldjavaSkin.set(window.oldjavaSkin.get() === 'arcade' ? skinBefore : 'arcade');
    arcadeButton.blur();
    frame.focus();
  });

  // Phone browsers resize the window as their address bar slides in and out while the page scrolls; refitting
  // then would resize the app under the visitor's finger, so a phone refits only for a real change.
  window.addEventListener('resize', () => {
    if (pocket.matches && !isBig() && window.innerWidth === fitted[0] && Math.abs(window.innerHeight - fitted[1]) < 160) return;
    fit();
  });

  // The pocket bar: two lines of hint until tapped, and a row of buttons that fades out at the right edge
  // while there are more to scroll to.
  const hintLine = byId('player-hint');
  hintLine.addEventListener('click', e => {
    if (pocket.matches && !(e.target as Element).closest('a')) hintLine.classList.toggle('open');
  });
  const actionRow = one('.player-bar .actions', player);
  function edge() {
    actionRow.classList.toggle('end', actionRow.scrollLeft + actionRow.clientWidth >= actionRow.scrollWidth - 4);
  }
  actionRow.addEventListener('scroll', edge, { passive: true });
  window.addEventListener('resize', edge);
  new MutationObserver(edge).observe(actionRow, { attributes: true, subtree: true, attributeFilter: ['hidden'] });
  edge();

  // The tour: Take the tour plays all 20 apps in the player, one stop at a time, each with one thing to try.
  // The games that play themselves start watched. A stop moves on by itself after TOUR_MS unless the visitor
  // touches the app, which hands it over to them: then the tour waits for Next stop. The stop reached is kept
  // (oldjava.tour), so the tour can be continued on a later visit, and #tour in the address starts it.
  const tour = { on: false, stop: 0, touched: false, spent: 0, done: false, doc: null as Document | null, timer: 0, last: 0 };
  const tourBox = byId('tour');
  const tourStart = byId('tour-start');
  const tourTime = one('span', byId('tour-time'));
  const tourNext = byId('tour-next');
  const tourBack = byId<HTMLButtonElement>('tour-back');
  const tourUnplayed = byId('tour-unplayed');
  const tourStory = byId('tour-story');
  const tourEndButton = byId('tour-end');
  function tourSaved(): TourSaved {
    try { return JSON.parse(localStorage.getItem(TOUR_KEY) as string) || {}; } catch (e) { return {}; }
  }
  function tourSave(o: TourSaved) {
    try { localStorage.setItem(TOUR_KEY, JSON.stringify(o)); } catch (e) { /* private mode: not kept */ }
  }
  function tourCard(i: number) { return card(TOUR[i][0]) as HTMLLIElement; }
  function tourName(i: number) { return nameOf(tourCard(i)); }
  const tourSteps = all<HTMLButtonElement>('#tour-steps button');
  tourSteps.forEach((b, i) => b.addEventListener('click', () => tourGo(i, true)));
  // The intro's button says where a tour left off.
  function tourLabel() {
    const saved = tourSaved();
    const note = byId('tour-start-note');
    if (tour.on) {
      tourStart.textContent = '▶ Back to the tour';
      note.textContent = tour.done ? 'You have seen all ' + TOUR.length + '.' : 'Stop ' + (tour.stop + 1) + ' of ' + TOUR.length + ': ' + tourName(tour.stop) + '.';
    } else if (saved.done) {
      tourStart.textContent = '▶ Take the tour again';
      note.textContent = 'You have seen all ' + TOUR.length + ' apps. Start again from the first.';
    } else if (saved.stop !== undefined && saved.stop > 0 && saved.stop < TOUR.length) {
      tourStart.textContent = '▶ Continue the tour';
      note.textContent = 'Stop ' + (saved.stop + 1) + ' of ' + TOUR.length + ': ' + tourName(saved.stop) + ', where you left off.';
    } else {
      tourStart.textContent = '▶ Take the tour';
      note.textContent = 'All ' + TOUR.length + ' apps in the player, one at a time, with something to try in each.';
    }
  }
  function tourRender() {
    const i = tour.stop;
    tourSteps.forEach((b, j) => {
      b.classList.toggle('seen', tour.done || j < i);
      if (j === i && !tour.done) b.setAttribute('aria-current', 'step'); else b.removeAttribute('aria-current');
    });
    tourBox.classList.toggle('yours', tour.touched || tour.done);
    tourNext.hidden = tour.done;
    tourBack.disabled = !tour.done && i === 0;
    tourEndButton.textContent = tour.done ? 'Close' : 'End tour';
    tourStory.hidden = !tour.done;
    if (tour.done) {
      const left = cards.filter(li => !passport.has(appOf(li))).length;
      byId('tour-count').textContent = 'Tour complete.';
      byId('tour-try').textContent = 'That was all ' + TOUR.length + ' apps. ' +
        (left === 0 ? 'You have played every one of them: the passport is full.'
          : 'You have played ' + (cards.length - left) + ' of ' + cards.length + '; ' + left + (left === 1 ? ' is' : ' are') + ' still waiting for you.');
      tourUnplayed.hidden = left === 0;
    } else {
      tourUnplayed.hidden = true;
      byId('tour-count').textContent = 'Stop ' + (i + 1) + ' of ' + TOUR.length;
      byId('tour-try').textContent = tour.touched
        ? 'Your turn: play ' + tourName(i) + ' as long as you like, then Next stop.'
        : TOUR[i][1];
      tourNext.textContent = i === TOUR.length - 1 ? 'Finish ›' : 'Next stop ›';
      tourNext.title = i === TOUR.length - 1 ? 'End of the tour' : 'Next: ' + tourName(i + 1);
    }
    tourTime.style.setProperty('--t', String(tour.done || tour.touched ? 0 : Math.min(1, tour.spent / TOUR_MS)));
    if (tour.on && !tour.done) document.documentElement.setAttribute('data-touring', '');
    else document.documentElement.removeAttribute('data-touring');
    tourLabel();
  }
  // The visitor touching the app (a click, key or touch inside the player's page, or the controller)
  // hands the stop over to them.
  function tourTouch(e?: Event) {
    if (!tour.on || tour.done || tour.touched || (e && e.isTrusted === false)) return;
    tour.touched = true;
    tourRender();
  }
  function tourHook() {
    let doc: Document | null = null;
    try { doc = frame.contentDocument; } catch (e) { /* not ours */ }
    if (!doc || doc === tour.doc || doc.readyState !== 'complete' || doc.URL === 'about:blank') return;
    tour.doc = doc;
    ['pointerdown', 'keydown', 'touchstart', 'wheel'].forEach(t => (doc as Document).addEventListener(t, tourTouch, true));
  }
  padBox.addEventListener('pointerdown', tourTouch);
  function tourTick() {
    tourHook();
    const now = Date.now();
    const dt = now - tour.last;
    tour.last = now;
    if (tour.done || tour.touched) return;
    const box = screen.getBoundingClientRect();
    const api = popApi();
    const saverUp = window.oldjavaSaver && window.oldjavaSaver.showing();
    if (document.visibilityState !== 'visible' || box.bottom < 0 || box.top > window.innerHeight || (api && api.popped()) || saverUp) return;
    tour.spent += dt;
    if (tour.spent >= TOUR_MS) { tourGo(tour.stop + 1, false); return; }
    tourTime.style.setProperty('--t', String(tour.spent / TOUR_MS));
  }
  // Stop i in the player (i === TOUR.length: the end card). byHand: a click, so a start sound may play.
  function tourGo(i: number, byHand: boolean) {
    const first = !tour.on;
    tour.on = true;
    tour.touched = false;
    tour.spent = 0;
    tour.last = Date.now();
    if (i >= TOUR.length) {
      tour.done = true;
      tour.stop = TOUR.length - 1;
      tourSave({ done: true });
      tourRender();
      return;
    }
    tour.done = false;
    tour.stop = Math.max(0, i);
    tourSave({ stop: tour.stop });
    firstView = false;
    tourBox.hidden = false;
    clearInterval(tour.timer);
    tour.timer = window.setInterval(tourTick, 250);
    history.replaceState(null, '', '#tour');
    const li = tourCard(tour.stop);
    play(li, false, false, !!(window.oldjavaWatch && window.oldjavaWatch.has(appOf(li))));
    clearTimeout(reelTimer);   // the tour moves on, not the demo reel
    tourRender();
    fit();
    if (byHand && window.oldjavaSound) window.oldjavaSound.play('start');
    if (first && !isBig()) player.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  function tourEnd() {
    tour.on = false;
    tour.done = false;
    clearInterval(tour.timer);
    tourBox.hidden = true;
    tourRender();
    fit();
    if (/^#tour$/.test(location.hash)) history.replaceState(null, '', '#' + appOf(playing()));
  }
  // Where a new start goes: the stop the last tour reached, or the first once it was finished.
  function tourResume() {
    const saved = tourSaved();
    return saved.done || !(saved.stop !== undefined && saved.stop > 0 && saved.stop < TOUR.length) ? 0 : saved.stop;
  }
  tourStart.addEventListener('click', () => {
    if (tour.on) { if (!isBig()) player.scrollIntoView({ behavior: 'smooth', block: 'start' }); tourNext.focus({ preventScroll: true }); return; }
    tourGo(tourResume(), true);
    tourNext.focus({ preventScroll: true });
  });
  tourNext.addEventListener('click', () => tourGo(tour.stop + 1, true));
  tourBack.addEventListener('click', () => tourGo(tour.done ? TOUR.length - 1 : tour.stop - 1, true));
  tourEndButton.addEventListener('click', () => tourEnd());
  tourUnplayed.addEventListener('click', () => byId('passport-new').click());
  passport.onChange(() => { if (tour.on) tourRender(); });
  window.oldjavaTour = { start: () => tourStart.click(), on: () => tour.on };
  tourLabel();

  const hash = /^#([^:]*)(:then)?$/.exec(decodeURIComponent(location.hash)) || [];
  const start = card(hash[1]);
  if (start) play(start, false, !!hash[2]);
  else if (hash[1] === 'tour') {   // #tour: the tour, from where it was left
    tourGo(tourResume(), false);
  } else { mark(card('Minesweeper') as HTMLLIElement); fit(); demo = true; reel(); }   // the page's own frame: Minesweeper, watched
}
