/*
 * Page bootstrap shared by the app index and every app page.
 * - Applies the saved skin before first paint and adds the skin toggle. A skin change in one page
 *   (the home page, say) reaches every other open page of the site through the storage event.
 * - An app page opened with ?embed (the home page's player) leaves out the toolbar and centers the app's
 *   windows, which most apps put at the top-left corner of the screen. ?embed&shot is the
 *   thumbnail mode web/shots.ps1 uses: square, shadowless windows on a flat magenta desktop.
 * - On an app page: preloads the files listed in <body data-files>, then calls the TeaVM main().
 * - Provides window.__shimSynth, the Web Audio voices behind javax.sound.midi.
 * - Routes System.out to the console by line, and on pages with <body data-console> also to an output panel.
 * - Keeps visitors moving between apps: an app page's toolbar has a Switch app drawer and a Next link, a
 *   How to play card shows the app's hint, and when the app exits the Run again box suggests three apps to
 *   play next. The list of apps is read from the home page's gallery (../index.html), so it lives in one place.
 * - Keeps the arcade passport (window.oldjavaPassport): a stamp for every app the visitor has played, saved in
 *   localStorage and shared by every open page. A new stamp shows a toast pointing at an app not played yet.
 */
(function () {
  'use strict';

  var SKIN_KEY = 'oldjava.skin';
  var skin = 'modern';
  try {
    skin = localStorage.getItem(SKIN_KEY) || 'modern';
  } catch (e) { /* storage blocked: use the default */ }
  if (skin !== 'classic' && skin !== 'modern') skin = 'modern';
  document.documentElement.setAttribute('data-skin', skin);

  // Fires in every other same-origin page, including the player iframe on the home page.
  window.addEventListener('storage', function (e) {
    if (e.key === SKIN_KEY && (e.newValue === 'classic' || e.newValue === 'modern')) {
      document.documentElement.setAttribute('data-skin', e.newValue);
      var select = document.querySelector('.shim-toolbar select');
      if (select) select.value = e.newValue;
    }
  });

  var embedded = /[?&]embed(=|&|$)/.test(location.search);
  if (embedded) document.documentElement.setAttribute('data-embed', '');
  if (embedded && /[?&]shot(=|&|$)/.test(location.search)) document.documentElement.setAttribute('data-shot', '');

  function setSkin(value) {
    document.documentElement.setAttribute('data-skin', value);
    try { localStorage.setItem(SKIN_KEY, value); } catch (e) { /* not remembered */ }
  }

  function toolbar(isApp) {
    var bar = document.createElement('div');
    bar.className = 'shim-toolbar';
    if (isApp) {
      var back = document.createElement('a');
      back.href = '../index.html';
      back.textContent = 'All apps';
      bar.appendChild(back);
      var switcher = document.createElement('button');
      switcher.type = 'button';
      switcher.textContent = 'Switch app';
      switcher.addEventListener('click', function () { openDrawer(switcher); });
      bar.appendChild(switcher);
      var next = document.createElement('a');
      next.className = 'shim-next';
      next.hidden = true;
      bar.appendChild(next);
      catalog().then(function (apps) {
        var i = indexOf(apps, appId());
        if (i < 0) return;
        var n = apps[(i + 1) % apps.length];
        next.href = pageOf(n);
        next.textContent = 'Next: ' + n.name + ' \u203a';
        next.hidden = false;
      });
    }
    var label = document.createElement('label');
    label.textContent = 'Skin ';
    var select = document.createElement('select');
    [['modern', 'Modern'], ['classic', 'Classic']].forEach(function (o) {
      var opt = document.createElement('option');
      opt.value = o[0];
      opt.textContent = o[1];
      select.appendChild(opt);
    });
    select.value = document.documentElement.getAttribute('data-skin');
    select.addEventListener('change', function () { setSkin(select.value); select.blur(); });
    label.appendChild(select);
    bar.appendChild(label);
    document.body.appendChild(bar);
  }

  // Embedded pages: keep the windows, as a group, centered in the viewport. Only a change in which windows
  // are open, their sizes or the viewport size recenters them, so dragging a window or moving it with keys
  // works as usual.
  function centerWindows(desktop) {
    var key = '';
    setInterval(function () {
      var x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity, k = desktop.clientWidth + 'x' + desktop.clientHeight + ':';
      for (var c = desktop.firstElementChild; c; c = c.nextElementSibling) {
        if (!c.offsetWidth) continue;
        k += c.offsetWidth + 'x' + c.offsetHeight + ',';
        x0 = Math.min(x0, c.offsetLeft); y0 = Math.min(y0, c.offsetTop);
        x1 = Math.max(x1, c.offsetLeft + c.offsetWidth); y1 = Math.max(y1, c.offsetTop + c.offsetHeight);
      }
      if (k === key) return;
      key = k;
      if (x1 < x0) return;
      var dx = Math.max(-x0, (desktop.clientWidth - (x1 - x0)) / 2 - x0);
      var dy = Math.max(-y0, (desktop.clientHeight - (y1 - y0)) / 2 - y0);
      desktop.style.transform = 'translate(' + Math.round(dx) + 'px, ' + Math.round(dy) + 'px)';
    }, 250);
  }

  // ---- arcade passport ----
  // { AppId: time of the first play } in localStorage. Every page keeps a copy and hears about stamps made in
  // other pages (the home page hears its player's iframe) through the storage event.
  var PLAYED_KEY = 'oldjava.played';
  function readPlayed() {
    try {
      var v = JSON.parse(localStorage.getItem(PLAYED_KEY) || '{}');
      return v && typeof v === 'object' ? v : {};
    } catch (e) { return {}; }
  }
  var played = readPlayed();
  var passportListeners = [];
  function passportChanged(id) { passportListeners.forEach(function (fn) { fn(id); }); }

  window.addEventListener('storage', function (e) {
    if (e.key !== PLAYED_KEY && e.key !== null) return;
    var before = played;
    played = readPlayed();
    var fresh = Object.keys(played).filter(function (k) { return !before[k]; });
    passportChanged(fresh[0] || null);
  });

  var passport = window.oldjavaPassport = {
    has: function (id) { return !!played[id]; },
    stamp: function (id) {
      played = readPlayed();   // another page may have stamped meanwhile
      if (played[id]) return;
      played[id] = Date.now();
      try { localStorage.setItem(PLAYED_KEY, JSON.stringify(played)); } catch (e) { /* kept for this page only */ }
      passportChanged(id);
    },
    reset: function () {
      played = {};
      try { localStorage.removeItem(PLAYED_KEY); } catch (e) { /* nothing saved */ }
      passportChanged(null);
    },
    // fn(id): id is the app just stamped, or null when the stamps changed some other way (a reset).
    onChange: function (fn) { passportListeners.push(fn); },
    // The home page sets this to swap an app into its player instead of following a link.
    open: null,
    catalog: function () { return catalog(); },
    // Apps not played yet, others of the same category as `id` first (see picks).
    unplayed: function (apps, id) {
      return picks(apps, id, apps.length).filter(function (a) { return !played[a.id]; });
    }
  };

  // A new stamp: a toast with the count and an app not played yet; the last one gets confetti.
  function stampToast(id) {
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, id)];
      if (!app) return;
      var count = apps.filter(function (a) { return played[a.id]; }).length;
      var old = document.querySelector('.shim-toast');
      if (old) old.remove();
      var hint = document.querySelector('.shim-hint');
      if (hint) hint.remove();
      var toast = document.createElement('div');
      toast.className = 'shim-toast';
      toast.setAttribute('role', 'status');
      var seal = document.createElement('span');
      seal.className = 'shim-toast-seal';
      var img = document.createElement('img');
      img.src = app.shot;
      img.alt = '';
      seal.appendChild(img);
      var text = document.createElement('div');
      text.className = 'shim-toast-text';
      var title = document.createElement('strong');
      var line = document.createElement('span');
      var done = count >= apps.length;
      title.textContent = done ? 'Arcade complete!' : 'Stamp collected: ' + app.name;
      line.textContent = done ? 'You have played all ' + apps.length + ' apps.'
        : count + ' of ' + apps.length + ' apps played.';
      text.appendChild(title);
      text.appendChild(line);
      var meter = document.createElement('span');
      meter.className = 'shim-meter';
      meter.style.setProperty('--p', String(count / apps.length));
      text.appendChild(meter);
      toast.appendChild(seal);
      toast.appendChild(text);
      var next = passport.unplayed(apps, id)[0];
      if (next) {
        var go = document.createElement('a');
        go.className = 'shim-toast-go';
        go.href = pageOf(next);
        go.textContent = 'Try ' + next.name + ' ›';
        go.addEventListener('click', function (e) {
          if (!passport.open || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
          e.preventDefault();
          toast.remove();
          passport.open(next.id);
        });
        toast.appendChild(go);
      }
      toast.appendChild(closeButton(function () { toast.remove(); }));
      ownKeys(toast);
      document.body.appendChild(toast);
      setTimeout(function () {
        toast.classList.add('gone');
        setTimeout(function () { toast.remove(); }, 600);
      }, 9000);
      if (done) confetti();
    });
  }

  function confetti() {
    if (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    var canvas = document.createElement('canvas');
    canvas.className = 'shim-confetti';
    var w = canvas.width = innerWidth, h = canvas.height = innerHeight;
    document.body.appendChild(canvas);
    var g = canvas.getContext('2d');
    var colors = ['#2563eb', '#22c55e', '#f59e0b', '#ef4444', '#a855f7', '#06b6d4'];
    var bits = [];
    for (var i = 0; i < 160; i++) {
      bits.push({ x: w / 2, y: h * 0.35, vx: (Math.random() - 0.5) * 14, vy: -Math.random() * 12 - 4,
                  r: Math.random() * 6.28, vr: (Math.random() - 0.5) * 0.4, c: colors[i % colors.length] });
    }
    var start = performance.now();
    (function frame(t) {
      g.clearRect(0, 0, w, h);
      bits.forEach(function (b) {
        b.vy += 0.35; b.x += b.vx; b.y += b.vy; b.r += b.vr;
        g.save(); g.translate(b.x, b.y); g.rotate(b.r); g.fillStyle = b.c; g.fillRect(-5, -3, 10, 6); g.restore();
      });
      if (t - start < 3000) requestAnimationFrame(frame);
      else canvas.remove();
    })(start);
  }

  // An app page is stamped by the first click or key press in the app itself (not in the site's own panels),
  // so the home page's player does not stamp Minesweeper just by loading it. A console program
  // (<body data-console>) has nothing to press: running it counts.
  var CHROME = '.shim-toolbar, .shim-drawer, .shim-scrim, .shim-hint, .shim-overlay, .shim-toast';
  function stampOnPlay() {
    var id = appId();
    if (document.body.hasAttribute('data-console')) { passport.stamp(id); return; }
    function onInput(e) {
      if (e.target && e.target.closest && e.target.closest(CHROME)) return;
      document.removeEventListener('pointerdown', onInput, true);
      document.removeEventListener('keydown', onInput, true);
      passport.stamp(id);
    }
    document.addEventListener('pointerdown', onInput, true);
    document.addEventListener('keydown', onInput, true);
  }

  // ---- app catalog and the ways to the next app ----
  // The home page's gallery is the one list of apps (name, hint, category, thumbnail), in gallery order.
  // App pages fetch it from ../index.html; the home page reads its own.
  function root() { return document.body.hasAttribute('data-app') ? '../' : ''; }

  var catalogPromise = null;
  function catalog() {
    if (!catalogPromise) {
      var source = root() ? fetch('../index.html').then(function (r) {
        if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
        return r.text();
      }).then(function (html) {
        return new DOMParser().parseFromString(html, 'text/html');
      }) : Promise.resolve(document);
      catalogPromise = source.then(function (doc) {
        return Array.prototype.map.call(doc.querySelectorAll('#apps li[data-app]'), function (li) {
          return {
            id: li.getAttribute('data-app'),
            cat: li.getAttribute('data-cat'),
            name: li.querySelector('strong').textContent,
            hint: li.querySelector('span').textContent,
            shot: root() + li.querySelector('img').getAttribute('src')
          };
        });
      }).catch(function (e) {
        console.warn('Could not read the app list from ../index.html: ' + e.message);
        return [];
      });
    }
    return catalogPromise;
  }

  function appId() { return document.body.getAttribute('data-app'); }

  function indexOf(apps, id) {
    for (var i = 0; i < apps.length; i++) if (apps[i].id === id) return i;
    return -1;
  }

  function pageOf(app) { return root() + app.id + '/index.html' + (embedded ? '?embed' : ''); }

  // Apps to try after this one (three unless `count` says otherwise): apps not played yet first, then the
  // played ones; within each, others of the same category first, each group shuffled.
  function picks(apps, id, count) {
    var self = apps[indexOf(apps, id)];
    function shuffled(list) {
      for (var i = list.length - 1; i > 0; i--) {
        var j = Math.floor(Math.random() * (i + 1));
        var t = list[i]; list[i] = list[j]; list[j] = t;
      }
      return list;
    }
    var result = [];
    [false, true].forEach(function (wasPlayed) {
      var others = apps.filter(function (a) { return a.id !== id && !!played[a.id] === wasPlayed; });
      var same = others.filter(function (a) { return self && a.cat === self.cat; });
      var rest = others.filter(function (a) { return !self || a.cat !== self.cat; });
      result = result.concat(shuffled(same), shuffled(rest));
    });
    return result.slice(0, count || 3);
  }

  // "4 of 16 apps played" and a bar, for the drawer and the exit screen.
  function passportLine(apps) {
    var count = apps.filter(function (a) { return played[a.id]; }).length;
    var p = document.createElement('div');
    p.className = 'shim-passport';
    var text = document.createElement('span');
    text.textContent = 'Passport: ' + count + ' of ' + apps.length + ' apps played';
    var meter = document.createElement('span');
    meter.className = 'shim-meter';
    meter.style.setProperty('--p', String(count / apps.length));
    p.appendChild(text);
    p.appendChild(meter);
    return p;
  }

  function closeButton(onClick) {
    var b = document.createElement('button');
    b.type = 'button';
    b.className = 'shim-close';
    b.setAttribute('aria-label', 'Close');
    b.textContent = '\u00d7';
    b.addEventListener('click', onClick);
    return b;
  }

  // A link to an app's page. In the home page's player it asks the page to swap the app in instead,
  // so the player's title, rail and address stay in step.
  // Played apps get a check on their thumbnail; with showNew, the others get a New badge.
  function appCard(app, current, showNew) {
    var a = document.createElement('a');
    a.className = 'shim-app' + (current ? ' current' : '');
    a.href = pageOf(app);
    if (current) a.setAttribute('aria-current', 'page');
    var pic = document.createElement('span');
    pic.className = 'shim-app-pic';
    var img = document.createElement('img');
    img.src = app.shot;
    img.alt = '';
    pic.appendChild(img);
    if (played[app.id] || showNew) {
      var badge = document.createElement('span');
      badge.className = 'shim-badge' + (played[app.id] ? ' played' : ' new');
      badge.textContent = played[app.id] ? '✓' : 'New';
      badge.title = played[app.id] ? 'Played' : 'Not played yet';
      pic.appendChild(badge);
    }
    var text = document.createElement('span');
    text.className = 'shim-app-text';
    var name = document.createElement('strong');
    name.textContent = app.name;
    var hint = document.createElement('span');
    hint.textContent = current ? 'Now playing' : app.hint;
    text.appendChild(name);
    text.appendChild(hint);
    a.appendChild(pic);
    a.appendChild(text);
    if (embedded && window.parent !== window) {
      a.addEventListener('click', function (e) {
        if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
        e.preventDefault();
        window.parent.postMessage({ oldjavaPlay: app.id }, location.origin);
      });
    }
    return a;
  }

  // Keys typed in the page chrome must not reach the app (the shim listens on the document).
  function ownKeys(el, onEscape) {
    el.addEventListener('keydown', function (e) {
      e.stopPropagation();
      if (e.key === 'Escape' && onEscape) onEscape();
    });
    el.addEventListener('keyup', function (e) { e.stopPropagation(); });
  }

  // Switch app: every app in gallery order, in a panel on the right.
  var drawer = null;
  function openDrawer(opener) {
    if (drawer) return;
    var scrim = document.createElement('div');
    scrim.className = 'shim-scrim';
    var panel = document.createElement('aside');
    panel.className = 'shim-drawer';
    panel.setAttribute('aria-label', 'Switch app');
    var head = document.createElement('div');
    head.className = 'shim-drawer-head';
    var title = document.createElement('strong');
    title.textContent = 'Switch app';
    var list = document.createElement('div');
    list.className = 'shim-drawer-list';
    function shut() {
      scrim.remove();
      panel.remove();
      drawer = null;
      opener.focus();
    }
    var close = closeButton(shut);
    head.appendChild(title);
    head.appendChild(close);
    panel.appendChild(head);
    panel.appendChild(list);
    scrim.addEventListener('click', shut);
    ownKeys(panel, shut);
    document.body.appendChild(scrim);
    document.body.appendChild(panel);
    drawer = panel;
    catalog().then(function (apps) {
      if (apps.length) panel.insertBefore(passportLine(apps), list);
      apps.forEach(function (app) { list.appendChild(appCard(app, app.id === appId())); });
      var here = list.querySelector('.current');
      if (here) here.scrollIntoView({ block: 'nearest' });
      (list.querySelector('.shim-app:not(.current)') || close).focus();
    });
  }

  // The app's hint from the gallery, top right under the toolbar, until it is closed or 12 seconds pass.
  function howToPlay() {
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (!app) return;
      var card = document.createElement('div');
      card.className = 'shim-hint';
      card.setAttribute('role', 'status');
      var title = document.createElement('strong');
      title.textContent = 'How to play ' + app.name;
      var text = document.createElement('p');
      text.textContent = app.hint;
      card.appendChild(closeButton(function () { card.remove(); }));
      card.appendChild(title);
      card.appendChild(text);
      ownKeys(card);
      document.body.appendChild(card);
      setTimeout(function () {
        card.classList.add('gone');
        setTimeout(function () { card.remove(); }, 600);
      }, 12000);
    });
  }

  // The app called System.exit (for most apps: its window was closed). Offer it again, and three others.
  function exitScreen(msg) {
    var hint = document.querySelector('.shim-hint');
    if (hint) hint.remove();
    var box = overlay(msg, 'Run again').firstChild;
    box.classList.add('shim-exit');
    ownKeys(box);
    if (!embedded) {
      var home = document.createElement('a');
      home.className = 'shim-exit-home';
      home.href = '../index.html';
      home.textContent = 'All apps';
      box.appendChild(home);
    }
    box.querySelector('button').focus();
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (app) {
        var h = document.createElement('h2');
        h.textContent = app.name + ' has closed';
        box.insertBefore(h, box.firstChild);
      }
      var next = picks(apps, appId());
      if (!next.length) return;
      var h3 = document.createElement('h3');
      h3.textContent = played[next[0].id] ? 'Play something else' : 'Play something new';
      var row = document.createElement('div');
      row.className = 'shim-picks';
      next.forEach(function (a) { row.appendChild(appCard(a, false, true)); });
      box.appendChild(h3);
      box.appendChild(row);
      box.appendChild(passportLine(apps));
    });
  }

  function overlay(text, button) {
    var o = document.createElement('div');
    o.className = 'shim-overlay';
    var box = document.createElement('div');
    box.className = 'shim-overlay-box';
    var p = document.createElement('p');
    p.textContent = text;
    box.appendChild(p);
    if (button) {
      var b = document.createElement('button');
      b.textContent = button;
      b.addEventListener('click', function () { location.reload(); });
      box.appendChild(b);
    }
    o.appendChild(box);
    document.body.appendChild(o);
    return o;
  }

  var IMAGE = /\.(png|jpe?g|gif|bmp|webp)$/i;

  function preload(name) {
    window.__shimFiles = window.__shimFiles || {};
    window.__shimImages = window.__shimImages || {};
    var bytes = fetch(name).then(function (r) {
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r.arrayBuffer();
    }).then(function (buf) {
      window.__shimFiles[name] = new Uint8Array(buf);
    });
    var image = !IMAGE.test(name) ? Promise.resolve() : new Promise(function (resolve, reject) {
      var img = new Image();
      img.onload = function () { window.__shimImages[name] = img; resolve(); };
      img.onerror = function () { reject(new Error('cannot decode')); };
      img.src = name;
    });
    return Promise.all([bytes, image]).catch(function (e) {
      console.warn('Could not preload ' + name + ': ' + e.message);
    });
  }

  // ---- System.out ----
  // TeaVM calls $rt_putStdoutCustom with raw chunks when it is defined before classes.js loads.
  var outLine = '';
  var outPanel = null;
  window.$rt_putStdoutCustom = function (chunk) {
    if (outPanel) {
      outPanel.appendChild(document.createTextNode(chunk));
      outPanel.scrollTop = outPanel.scrollHeight;
    }
    var lines = (outLine + chunk).split('\n');
    outLine = lines.pop();
    lines.forEach(function (line) { console.info(line); });
  };

  // ---- javax.sound.midi voices ----
  var audio = null;
  var voices = {};
  var WAVES = ['triangle', 'square', 'sawtooth', 'sine'];

  function ctx() {
    if (!audio) {
      var AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return null;
      audio = new AC();
    }
    if (audio.state === 'suspended') audio.resume();
    return audio;
  }

  function release(v, at) {
    v.gain.gain.cancelScheduledValues(at);
    v.gain.gain.setValueAtTime(v.gain.gain.value, at);
    v.gain.gain.linearRampToValueAtTime(0, at + 0.15);
    v.osc.stop(at + 0.2);
  }

  window.__shimSynth = {
    noteOn: function (ch, note, vel, program, cents) {
      var a = ctx();
      if (!a) return;
      var key = ch + ':' + note;
      if (voices[key]) release(voices[key], a.currentTime);
      var osc = a.createOscillator();
      osc.type = WAVES[Math.floor(program / 8) % WAVES.length];
      osc.frequency.value = 440 * Math.pow(2, (note - 69) / 12);
      osc.detune.value = cents;
      var gain = a.createGain();
      var peak = 0.25 * vel / 127;
      gain.gain.setValueAtTime(0, a.currentTime);
      gain.gain.linearRampToValueAtTime(peak, a.currentTime + 0.01);
      gain.gain.linearRampToValueAtTime(peak * 0.6, a.currentTime + 0.3);
      osc.connect(gain).connect(a.destination);
      osc.start();
      voices[key] = { osc: osc, gain: gain, ch: ch };
    },
    noteOff: function (ch, note) {
      var key = ch + ':' + note;
      var v = voices[key];
      if (v && audio) {
        release(v, audio.currentTime);
        delete voices[key];
      }
    },
    bend: function (ch, cents) {
      Object.keys(voices).forEach(function (k) {
        if (voices[k].ch === ch) voices[k].osc.detune.value = cents;
      });
    },
    allOff: function (ch) {
      Object.keys(voices).forEach(function (k) {
        if (voices[k].ch === ch) window.__shimSynth.noteOff(ch, +k.split(':')[1]);
      });
    }
  };
  // Browsers only start audio after a user gesture.
  ['pointerdown', 'keydown'].forEach(function (type) {
    document.addEventListener(type, function () { if (audio && audio.state === 'suspended') audio.resume(); }, true);
  });

  document.addEventListener('DOMContentLoaded', function () {
    var body = document.body;
    var isApp = body.hasAttribute('data-app');
    if (!embedded) toolbar(isApp);
    // The player's iframe leaves the toast to the home page around it, which hears the stamp too. An app page
    // shows only its own stamp; the home page shows any.
    if (!embedded) passport.onChange(function (id) { if (id && (!isApp || id === appId())) stampToast(id); });
    if (!isApp) return;

    var desktop = document.createElement('div');
    desktop.id = 'desktop';
    body.insertBefore(desktop, body.firstChild);
    if (embedded && !document.documentElement.hasAttribute('data-shot')) centerWindows(desktop);
    if (body.hasAttribute('data-console')) {
      outPanel = document.createElement('pre');
      outPanel.className = 'shim-console';
      body.appendChild(outPanel);
    }

    var files = (body.getAttribute('data-files') || '').split(/\s+/).filter(Boolean);
    var args = (body.getAttribute('data-args') || '').split(/\s+/).filter(Boolean);
    var loading = files.length ? overlay('Loading...') : null;
    var extras = !document.documentElement.hasAttribute('data-shot');
    window.__shimExited = function (msg) { if (extras) exitScreen(msg); else overlay(msg, 'Run again'); };

    Promise.all(files.map(preload)).then(function () {
      if (loading) loading.remove();
      if (typeof main !== 'function') {
        overlay('classes.js is missing. Build the site with Maven first (see web/README.md).');
        return;
      }
      main(args);
      if (extras && !embedded) howToPlay();
      if (extras) stampOnPlay();   // after the hint, so a stamp's toast replaces it rather than sitting under it
    });
  });
})();
