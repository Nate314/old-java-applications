/*
 * Clock follows you: Clock's timer, stopwatch and focus sessions, seen from every page of the site.
 *
 * Clock saves them as wall clock times (~/.clock.txt, kept by web.rt.Saves in localStorage under
 * oldjava.saves.Clock), so this file can read them from any page. boot.js adds it (clockFollows) once something
 * runs. It never writes Clock's file: Clock catches up on what happened when it opens again, as it does on the
 * desktop.
 *
 * - The toolbar gets a chip for each one running: a ring that drains (red for focus, green for a break, blue for
 *   the timer, violet for the stopwatch, once round a minute) and the time left. It links to Clock.
 * - A focus session that ends while the visitor is elsewhere rings the break phrase and offers a quick game for
 *   the break (one not played yet first); a break that ends rings back to work and links back to Clock, past its
 *   title screen; a timer that runs out rings its bell. On an app page the news shows in the toolbar band, on the
 *   home page as a toast, whose game link swaps the game into the player.
 * - Only one page rings for each moment (oldjava.clockBell keeps the last one rung), and none while Clock itself
 *   runs somewhere (oldjava.clockOpen, its page's heartbeat), since it rings for itself. A moment more than a minute
 *   old is not news, the rule Clock uses for what happened while it was closed.
 * - On the Clock page there is no chip (Clock shows its own time) and no bell, only the game for the break.
 */
// Clock's saved state: the fields of ~/.clock.txt (ClockState.java) as numbers.
interface ClockState {
  plan: number; phase: number; done: number; focusEnd: number;
  timer: number; timerEnd: number; timerDone: number; watchStart: number; watchBanked: number;
}
type Moment =
  | { kind: 'break'; at: number; phase: number; done: number; ends: number }
  | { kind: 'back'; at: number; next: number }
  | { kind: 'timer'; at: number; length: number };

(function () {
  'use strict';
  var site = window.oldjavaSite;
  var SAVES = 'oldjava.saves.Clock', BELL = 'oldjava.clockBell', OPEN = 'oldjava.clockOpen';
  var MINUTE = 60000, FRESH = MINUTE, OPEN_FOR = 75000;   // a hidden tab's timers may slow to once a minute
  var SET = 4, PLANS = [[25, 5], [50, 10], [15, 3]];    // Focus.java: sessions per set, focus / short break minutes
  var FOCUS = 0, SHORT = 1, LONG = 2;
  var QUICK = ['FlappyBird', 'DinoGame', 'Minesweeper', 'NumberGuess', 'SpriteFloatyStars'];
  var onClock = site.clockPage();
  var shown: Record<string, boolean> = {};   // moments this page has told, by kind and time

  // Clock's saved state, or null: the fields of ~/.clock.txt (ClockState.java) as numbers.
  function read(): ClockState | null {
    var saves: Record<string, unknown>;
    try { saves = JSON.parse(localStorage.getItem(SAVES) || '{}') || {}; } catch (e) { return null; }
    var path = Object.keys(saves).filter(function (p) { return p.split('/').pop() === '.clock.txt'; })[0];
    if (!path) return null;
    var v: Record<string, number> = {};
    String(saves[path]).split('\n').forEach(function (line) {
      var i = line.indexOf('=');
      if (i > 0) v[line.slice(0, i)] = Number(line.slice(i + 1).trim());
    });
    function n(key: string) { return v[key] > 0 ? v[key] : 0; }
    return {
      plan: PLANS[n('focusPlan')] ? n('focusPlan') : 0,
      phase: n('focusPhase') === SHORT || n('focusPhase') === LONG ? n('focusPhase') : FOCUS,
      done: Math.min(SET, n('focusDone')),
      focusEnd: n('focusEnd'),
      timer: n('timer'), timerEnd: n('timerEnd'), timerDone: n('timerDone'),
      watchStart: n('watchStart'), watchBanked: n('watchBanked')
    };
  }

  function length(s: ClockState, phase: number) {
    var m = PLANS[s.plan];
    return (phase === FOCUS ? m[0] : phase === SHORT ? m[1] : 3 * m[1]) * MINUTE;
  }

  // Focus.update without the day log: plays the sessions forward to now, collecting the moments on the way.
  function catchUp(s: ClockState, now: number) {
    var moments: Moment[] = [];
    while (s.focusEnd && now >= s.focusEnd) {
      var at = s.focusEnd;
      if (s.phase === FOCUS) {
        s.done = Math.min(SET, s.done + 1);
        s.phase = s.done >= SET ? LONG : SHORT;
        s.focusEnd = at + length(s, s.phase);
        moments.push({ kind: 'break', at: at, phase: s.phase, done: s.done, ends: s.focusEnd });
      } else {
        if (s.phase === LONG) s.done = 0;
        s.phase = FOCUS;
        s.focusEnd = 0;
        moments.push({ kind: 'back', at: at, next: s.done + 1 });
      }
    }
    var rang = s.timerEnd && now >= s.timerEnd ? s.timerEnd : s.timerDone;
    if (rang) moments.push({ kind: 'timer', at: rang, length: s.timer });
    return moments.filter(function (m) { return now - m.at < FRESH; });
  }

  function clock(ms: number) {
    var t = Math.max(0, Math.ceil(ms / 1000)), h = Math.floor(t / 3600), m = Math.floor(t / 60) % 60, sec = t % 60;
    return (h ? h + ':' + (m < 10 ? '0' : '') : '') + m + ':' + (sec < 10 ? '0' : '') + sec;
  }
  function at(ms: number) { return new Date(ms).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }); }
  function minutes(ms: number) { var m = Math.round(ms / MINUTE); return m + (m === 1 ? ' minute' : ' minutes'); }

  // ---- the chip ----
  var chip: HTMLAnchorElement | null = null;
  function segment(kind: string, label: string, text: string, part: number) {
    var seg = document.createElement('span');
    seg.className = 'shim-clock-seg';
    seg.setAttribute('data-kind', kind);
    var ring = document.createElement('i');
    ring.className = 'shim-clock-ring';
    ring.style.setProperty('--p', Math.max(0, Math.min(1, part)).toFixed(3));
    var name = document.createElement('span');
    name.className = 'shim-clock-name';
    name.textContent = label + ' ';
    var time = document.createElement('b');
    time.textContent = text;
    seg.appendChild(ring);
    seg.appendChild(name);
    seg.appendChild(time);
    return seg;
  }
  function showChip(s: ClockState, now: number) {
    var bar = document.querySelector('.shim-toolbar');
    if (!bar || onClock) return;
    var segs = [], words = [];
    if (s.focusEnd) {
      var len = length(s, s.phase), left = s.focusEnd - now;
      var focusing = s.phase === FOCUS;
      segs.push(segment(focusing ? 'focus' : 'break', focusing ? 'Focus' : 'Break', clock(left), left / len));
      words.push((focusing ? 'Focus session ' + (s.done + 1) + ' of ' + SET : s.phase === LONG ? 'Long break' : 'Break')
        + ': ' + clock(left) + ' left, until ' + at(s.focusEnd));
    }
    if (s.timerEnd > now) {
      segs.push(segment('timer', 'Timer', clock(s.timerEnd - now), (s.timerEnd - now) / (s.timer || MINUTE)));
      words.push('Timer: ' + clock(s.timerEnd - now) + ' left, rings at ' + at(s.timerEnd));
    }
    if (s.watchStart) {
      var ran = now - s.watchStart + s.watchBanked;
      segs.push(segment('watch', 'Stopwatch', clock(ran - ran % 1000), (ran % MINUTE) / MINUTE));
      words.push('Stopwatch: ' + clock(ran - ran % 1000));
    }
    if (!segs.length) {
      if (chip) { chip.remove(); chip = null; }
      return;
    }
    if (!chip) {
      chip = document.createElement('a');
      chip.className = 'shim-clock';
      chip.href = site.root() + 'Clock/index.html';
      chip.addEventListener('click', function () { site.skipTitle('Clock'); });
      bar.insertBefore(chip, bar.querySelector('.shim-sound'));
    }
    chip.textContent = '';
    segs.forEach(function (seg) { chip!.appendChild(seg); });
    var soon = (s.focusEnd && s.focusEnd - now <= 10000) || (s.timerEnd > now && s.timerEnd - now <= 10000);
    chip.toggleAttribute('data-soon', !!soon);
    var label = words.join('. ') + '. Open Clock.';
    if (chip.title !== label) { chip.title = label; chip.setAttribute('aria-label', label); }
  }

  // ---- the moments ----
  // Rings once across every open page, and not while Clock runs somewhere (it rings for itself).
  function ring(m: Moment) {
    if (onClock) return;
    var last = 0, open = 0;
    try {
      last = Number(localStorage.getItem(BELL)) || 0;
      open = Number(localStorage.getItem(OPEN)) || 0;
    } catch (e) { /* ring here */ }
    if (m.at <= last) return;
    try { localStorage.setItem(BELL, String(m.at)); } catch (e) { /* other pages may ring too */ }
    if (Date.now() - open < OPEN_FOR) return;
    window.oldjavaSound.play(m.kind === 'break' ? 'breakTime' : m.kind === 'back' ? 'backToWork' : 'ring');
  }

  // A game for the break: a quick one not played yet first, never the one on this page.
  function breakGame(apps: OldjavaCatalogApp[]) {
    var here = site.appId();
    var quick = apps.filter(function (a) { return QUICK.indexOf(a.id) >= 0 && a.id !== here; });
    var fresh = quick.filter(function (a) { return !window.oldjavaPassport.has(a.id); });
    var pool = fresh.length ? fresh : quick;
    return pool[Math.floor(Math.random() * pool.length)];
  }

  function tell(m: Moment, apps: OldjavaCatalogApp[]) {
    var here = site.appId(), app: OldjavaCatalogApp | null = null, title: string, line: string, label: string | undefined;
    var clockApp = apps.filter(function (a) { return a.id === 'Clock'; })[0];
    var playing = here && apps.some(function (a) { return a.id === here && a.cat === 'game'; });
    if (m.kind === 'break') {
      title = m.phase === LONG ? 'Four sessions done: long break' : 'Focus ' + m.done + ' of ' + SET + ' done: break time';
      line = minutes(m.ends - m.at) + ' until ' + at(m.ends) + '. ';
      if (playing) line += 'Play on: the clock calls you back.';
      else {
        app = breakGame(apps);
        if (app) { line += 'Spend it on ' + app.name + '? The clock keeps time and calls you back.'; label = 'Play ' + app.name; }
      }
    } else {
      if (onClock) return;   // Clock says these itself
      app = clockApp;
      title = m.kind === 'back' ? 'Break’s over' : 'Time’s up';
      line = m.kind === 'back' ? 'Focus session ' + m.next + ' of ' + SET + ' is ready to start.'
        : 'Your ' + Math.round(m.length / MINUTE) + '-minute timer rang at ' + at(m.at) + '.';
      label = m.kind === 'back' ? 'Back to focus' : 'Open Clock';
    }
    function link(toast: HTMLElement | null) {
      if (!app || !label) return [];
      var go = toast ? site.toastLink(toast, app, label) : document.createElement('a');
      if (!toast) {
        go.className = 'shim-news-go';
        go.href = site.pageOf(app);
        go.textContent = label;
      }
      // (the home page's link swaps Clock into its player instead, which has no title screen)
      if (app.id === 'Clock' && !toast) go.addEventListener('click', function () { site.skipTitle('Clock'); });
      return [go];
    }
    var news = site.news();
    if (news) {
      var tag = document.createElement('span');
      tag.className = 'shim-news-tag shim-clock-tag';
      tag.setAttribute('data-kind', m.kind);
      tag.textContent = m.kind === 'break' ? 'Break' : m.kind === 'back' ? 'Focus' : 'Timer';
      news.show('clock', [tag], [document.createTextNode(title + '. ' + line)], link(null), 15000);
    } else {
      var shot = (app || clockApp || {} as Partial<OldjavaCatalogApp>).shot;
      if (shot) site.toast(shot, title, line, link);
    }
  }

  // Clock, running in this page or another, saves what follows a moment as soon as it happens (the break already
  // running), so the moments are also played forward from what this page saw a second ago.
  var before: ClockState | null = null;
  function tick() {
    var s = read();
    if (!s) return;
    var now = Date.now();
    var moments = catchUp(s, now);
    if (before) catchUp(before, now).forEach(function (m) {
      if (!moments.some(function (o) { return o.kind === m.kind && o.at === m.at; })) moments.push(m);
    });
    before = s;
    showChip(s, now);
    moments.forEach(function (m) {
      ring(m);
      var id = m.kind + m.at;
      if (shown[id] || document.hidden) return;   // a hidden page tells it when it is looked at again
      shown[id] = true;
      window.oldjavaPassport.catalog().then(function (apps) { tell(m, apps); });
    });
  }

  // What happened before this page opened was told by the page before it (or is Clock's own to tell).
  var start = read();
  if (start) catchUp(start, Date.now()).forEach(function (m) { if (m.at < Date.now() - 2000) shown[m.kind + m.at] = true; });
  tick();
  setInterval(tick, 1000);
  document.addEventListener('visibilitychange', function () { if (!document.hidden) tick(); });
})();

export {};   // a module, imported when it is needed (its chunk is assets/clockbar.js)
