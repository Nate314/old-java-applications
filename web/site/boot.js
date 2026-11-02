/*
 * Page bootstrap shared by the app index and every app page.
 * - Applies the saved skin (Modern, Classic or Arcade) before first paint and adds the skin picker. A skin
 *   change in one page (the home page, say) reaches every other open page of the site through the storage event.
 * - An app page opened with ?embed (the home page's player) leaves out the toolbar and centers the app's
 *   windows, which most apps put at the top-left corner of the screen. ?embed&shot is the
 *   thumbnail mode web/shots.ps1 uses: square, shadowless windows on a flat magenta desktop.
 * - Any other app page is a stage: a toolbar band across the top with the app's name, and the app's windows
 *   centered in the space left between it and the controller (or output panel) at the bottom, which the shim
 *   reports to Java as the screen, so neither the toolbar nor the controller covers an app window.
 * - On an app page: preloads the files listed in <body data-files>, then calls the TeaVM main().
 * - Provides window.__shimSynth, the Web Audio voices behind javax.sound.midi.
 * - Routes System.out to the console by line, and on pages with <body data-console> also to an output panel.
 * - Opens an app page on a title screen (thumbnail, hint, passport stamp and a live boot log of the files
 *   loading); the app starts on the visitor's Play click or key press, so its first sounds can play.
 * - Keeps visitors moving between apps: an app page's toolbar has a Switch app drawer and a Next link, a
 *   How to play card shows the app's hint, and when the app exits the Run again box suggests three apps to
 *   play next. The list of apps is read from the home page's gallery (../index.html), so it lives in one place.
 * - Gives the keyboard games an on-screen controller (window.oldjavaPad) for touch screens. Its buttons send
 *   the app's own keys; the home page docks it under its player and passes the presses into the iframe.
 * - Keeps the arcade passport (window.oldjavaPassport): a stamp for every app the visitor has played, saved in
 *   localStorage and shared by every open page. A new stamp shows a toast pointing at an app not played yet.
 * - Links the web app manifest and icons and registers the service worker (sw.js), so the site can be
 *   installed and saved apps play offline. The home page's Take it with you panel uses both.
 * - Share (toolbar, exit screen, arcade-complete toast; the home page's player bar and passport) opens a panel
 *   with the page's link preview card (cards/, made by web/cards.ps1), a message with the passport count, the
 *   link and ways to send it. A shared app link (?shared=N) says so on its title screen.
 * - Gives the site's own controls and moments short synthesized sounds in the skin's voice
 *   (window.oldjavaSound), with a Sound button in the toolbar to mute them on every open page.
 */
(function () {
  'use strict';

  var SKIN_KEY = 'oldjava.skin';
  var SKINS = [['modern', 'Modern'], ['classic', 'Classic'], ['arcade', 'Arcade']];
  function isSkin(v) { return SKINS.some(function (o) { return o[0] === v; }); }
  var skin = 'modern';
  try {
    skin = localStorage.getItem(SKIN_KEY) || 'modern';
  } catch (e) { /* storage blocked: use the default */ }
  if (!isSkin(skin)) skin = 'modern';
  document.documentElement.setAttribute('data-skin', skin);

  function showSkin(value) {
    document.documentElement.setAttribute('data-skin', value);
    var select = document.querySelector('.shim-toolbar select');
    if (select) select.value = value;
  }

  // Fires in every other same-origin page, including the player iframe on the home page.
  window.addEventListener('storage', function (e) {
    if (e.key === SKIN_KEY && isSkin(e.newValue)) showSkin(e.newValue);
  });

  var embedded = /[?&]embed(=|&|$)/.test(location.search);
  if (embedded) document.documentElement.setAttribute('data-embed', '');
  if (embedded && /[?&]shot(=|&|$)/.test(location.search)) document.documentElement.setAttribute('data-shot', '');

  function setSkin(value) {
    if (!isSkin(value)) return;
    showSkin(value);
    try { localStorage.setItem(SKIN_KEY, value); } catch (e) { /* not remembered */ }
    playSound('skin');   // in the new skin's voice
  }
  // The home page's Arcade mode button switches skins through this.
  window.oldjavaSkin = { set: setSkin, get: function () { return document.documentElement.getAttribute('data-skin'); } };

  // ---- installable and offline: the web app manifest, icons and the service worker (sw.js) ----
  // Every page links them, so the arcade can be installed from any page and every page can be kept offline.
  var siteRoot = new URL('.', document.currentScript.src).href;
  function headLink(rel, href, type) {
    var link = document.createElement('link');
    link.rel = rel;
    link.href = siteRoot + href;
    if (type) link.type = type;
    document.head.appendChild(link);
  }
  headLink('manifest', 'manifest.webmanifest');
  headLink('icon', 'icons/icon.svg', 'image/svg+xml');
  headLink('apple-touch-icon', 'icons/apple-touch-icon.png');
  var theme = document.createElement('meta');
  theme.name = 'theme-color';
  theme.content = '#170a2e';
  document.head.appendChild(theme);
  if ('serviceWorker' in navigator && window.isSecureContext) {
    navigator.serviceWorker.register(siteRoot + 'sw.js').catch(function (e) {
      console.warn('Offline play is not available: ' + e.message);
    });
  }
  // The home page shows its own Install button, so it keeps the browser's install prompt for that button.
  window.addEventListener('beforeinstallprompt', function (e) {
    if (document.body && document.body.hasAttribute('data-app')) return;
    e.preventDefault();
    window.oldjavaInstall = e;
    document.dispatchEvent(new Event('oldjava-installable'));
  });

  function toolbar(isApp) {
    var bar = document.createElement('div');
    bar.className = 'shim-toolbar';
    if (isApp) {
      var name = document.createElement('strong');
      name.className = 'shim-bar-name';
      name.textContent = document.title;
      bar.appendChild(name);
      var back = document.createElement('a');
      back.href = '../index.html';
      back.textContent = 'All apps';
      bar.appendChild(back);
      var switcher = document.createElement('button');
      switcher.type = 'button';
      switcher.textContent = 'Switch app';
      switcher.addEventListener('click', function () { openDrawer(switcher); });
      bar.appendChild(switcher);
      var source = document.createElement('a');
      source.href = '#source';
      source.className = 'shim-source-open';
      source.title = 'Read the Java source of this app while it runs';
      source.setAttribute('data-sound', 'none');   // the panel plays its own open sound
      source.innerHTML = '&lt;/&gt;<span> Source</span>';
      bar.appendChild(source);
      var share = document.createElement('button');
      share.type = 'button';
      share.className = 'shim-share-open';
      share.title = 'Share this app: its link, with a preview card';
      share.setAttribute('data-sound', 'none');   // the panel plays its own open sound
      share.innerHTML = '↗<span> Share</span>';
      share.addEventListener('click', function () { openShare(appId(), share); });
      bar.appendChild(share);
      var controls = null;
      if (pad.has(appId())) {
        controls = document.createElement('button');
        controls.type = 'button';
        controls.textContent = 'Controls';
        controls.title = 'Show or hide the on-screen controller';
        bar.appendChild(controls);
      }
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
    var sound = document.createElement('button');
    sound.type = 'button';
    sound.className = 'shim-sound';
    sound.setAttribute('data-sound', 'none');
    sound.addEventListener('click', function () { setSoundOn(!soundOn()); sound.blur(); });
    bar.appendChild(sound);
    var label = document.createElement('label');
    label.textContent = 'Skin ';
    var select = document.createElement('select');
    SKINS.forEach(function (o) {
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
    if (isApp) stageEdge(bar, '--bar-h');
    showSound();
    if (controls) padOnPage(controls);
  }

  // An app page's stage: #desktop fills the space between the toolbar band at the top and the controller and
  // the output panel at the bottom, so no app window sits under them. The shim reports the stage as the
  // screen, so apps that size or center their windows from the screen fit it, and centerWindows keeps the
  // windows centered on it. stageEdge keeps a CSS variable at an element's height.
  function stageEdge(el, name) {
    function set() { document.documentElement.style.setProperty(name, (el.isConnected && !el.hidden ? el.offsetHeight : 0) + 'px'); }
    set();
    if (window.ResizeObserver) new ResizeObserver(set).observe(el);
    else addEventListener('resize', set);
  }

  // Embedded pages and app pages: keep the windows, as a group, centered on the desktop, scaled down to fit it
  // when they are bigger (window.__shimScale, which the shim divides mouse positions by). Only a change in
  // which windows are open, their sizes or the desktop's size recenters them, so dragging a window or moving
  // it with keys works as usual.
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
      var w = desktop.clientWidth, h = desktop.clientHeight;
      var s = Math.min(1, w / (x1 - x0), h / (y1 - y0));
      // About the desktop's center (the default transform origin, which the Arcade power-on's scale shares).
      var dx = s * (w - x0 - x1) / 2;
      var dy = s * (h - y0 - y1) / 2;
      window.__shimScale = s;
      // Whole pixels keep unscaled windows crisp; a scaled group keeps the exact fit instead.
      desktop.style.transform = s < 1 ? 'translate(' + dx + 'px, ' + dy + 'px) scale(' + s + ')'
        : 'translate(' + Math.round(dx) + 'px, ' + Math.round(dy) + 'px)';
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
      if (done) {
        var brag = document.createElement('button');
        brag.type = 'button';
        brag.className = 'shim-toast-go';
        brag.setAttribute('data-sound', 'none');
        brag.textContent = 'Share it ›';
        brag.addEventListener('click', function () { toast.remove(); openShare(null); });
        toast.appendChild(brag);
      }
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
      playSound(done ? 'complete' : 'stamp');
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
  var CHROME = '.shim-toolbar, .shim-drawer, .shim-share, .shim-scrim, .shim-hint, .shim-overlay, .shim-toast, .shim-pad, .shim-title, .shim-source';
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

  // ---- touch controller ----
  // The keyboard games get an on-screen controller, so they can be played on a phone or tablet. Its buttons
  // press and release the same keys the app listens for (taken from the apps' sources), and they light up
  // when the real key is pressed, so the controller doubles as a key chart. The home page docks it under
  // its player (window.oldjavaPad.render); an app page floats it at the bottom.
  var PADS = {
    FlappyBird: [
      { buttons: [{ k: 'Space', label: 'Flap', big: true }] },
      { buttons: [{ k: 'C', label: 'Course' }, { k: 'P', label: 'Pause' }, { k: 'R', label: 'Reset' }] }
    ],
    TankGame: [
      { title: 'Red tank', tint: 'red', dpad: ['W', 'A', 'S', 'D'], fire: 'Q' },
      { title: 'Green tank', tint: 'green', dpad: ['ArrowUp', 'ArrowLeft', 'ArrowDown', 'ArrowRight'], fire: 'M' }
    ],
    SynthesizerKeyboard: [
      { piano: ['Z', 'S', 'X', 'D', 'C', 'V', 'G', 'B', 'H', 'N', 'J', 'M', ','] },
      { buttons: [{ k: 'T', label: 'Song' }, { k: 'L', label: 'Listen' }, { k: 'R', label: 'Restart' }] },
      { buttons: [{ k: 'ArrowLeft', label: 'Octave −' }, { k: 'ArrowRight', label: 'Octave +' },
                  { k: 'ArrowDown', label: 'Sound −' }, { k: 'ArrowUp', label: 'Sound +' }] }
    ],
    SpriteFloatyStars: [
      { title: 'Star Catcher', tint: 'gold', dpad: ['ArrowUp', 'ArrowLeft', 'ArrowDown', 'ArrowRight'], fire: 'Space', fireLabel: 'Catch' },
      { buttons: [{ k: 'N', label: 'Next' }, { k: 'R', label: 'Again' }, { k: 'M', label: 'Nights' },
                  { k: 'ArrowUp', label: 'Add star' }, { k: 'ArrowDown', label: 'Remove star' }] }
    ]
  };
  var ARROWS = { ArrowUp: [38, '↑'], ArrowDown: [40, '↓'], ArrowLeft: [37, '←'], ArrowRight: [39, '→'] };

  // A key name from PADS as the fields of a DOM key event, plus the label on its keycap.
  function keyOf(name) {
    if (name === 'Space') return { key: ' ', code: 'Space', which: 32, cap: 'Space' };
    if (ARROWS[name]) return { key: name, code: name, which: ARROWS[name][0], cap: ARROWS[name][1] };
    if (name === ',') return { key: ',', code: 'Comma', which: 188, cap: ',' };
    return { key: name.toLowerCase(), code: 'Key' + name, which: name.charCodeAt(0), cap: name };
  }

  // A key event the shim cannot tell from a real one (keyCode cannot be set through the constructor).
  function fireKey(type, name) {
    var k = keyOf(name);
    var e = new KeyboardEvent(type, { key: k.key, code: k.code, bubbles: true, cancelable: true });
    Object.defineProperty(e, 'keyCode', { get: function () { return k.which; } });
    Object.defineProperty(e, 'which', { get: function () { return k.which; } });
    document.body.dispatchEvent(e);
  }

  var PAD_KEY = 'oldjava.pad';
  var pad = window.oldjavaPad = {
    has: function (id) { return !!PADS[id]; },
    // Shown or not: the visitor's last choice, else `byDefault`.
    wanted: function (byDefault) {
      var v = null;
      try { v = localStorage.getItem(PAD_KEY); } catch (e) { /* not remembered */ }
      return v === 'on' || (v !== 'off' && byDefault);
    },
    remember: function (on) {
      try { localStorage.setItem(PAD_KEY, on ? 'on' : 'off'); } catch (e) { /* not remembered */ }
    },
    // Builds app `id`'s controller in `el`. send(type, keyName) delivers each press and release.
    // Returns light(keyCode, down), which shows a real key press on the matching buttons.
    render: function (el, id, send) {
      el.textContent = '';
      el.classList.add('shim-pad');
      el.setAttribute('aria-label', 'Controller');
      var buttons = [];
      function button(name, cls, label) {
        var k = keyOf(name);
        var b = document.createElement('button');
        b.type = 'button';
        b.tabIndex = -1;   // never takes focus, so the app keeps the keyboard
        b.className = cls;
        b.setAttribute('data-which', String(k.which));
        if (label) {
          var l = document.createElement('span');
          l.textContent = label;
          b.appendChild(l);
        }
        var cap = document.createElement('kbd');
        cap.textContent = k.cap;
        b.appendChild(cap);
        b.title = (label ? label + ': ' : '') + k.cap + ' key';
        var held = false;
        function up() {
          if (!held) return;
          held = false;
          b.classList.remove('on');
          send('keyup', name);
        }
        b.addEventListener('pointerdown', function (e) {
          e.preventDefault();   // no focus change, text selection or (on touch) emulated mouse events
          if (held) return;
          held = true;
          try { b.setPointerCapture(e.pointerId); } catch (x) { /* released by pointerup anyway */ }
          b.classList.add('on');
          send('keydown', name);
        });
        b.addEventListener('pointerup', up);
        b.addEventListener('pointercancel', up);
        b.addEventListener('lostpointercapture', up);
        b.addEventListener('contextmenu', function (e) { e.preventDefault(); });
        buttons.push(b);
        return b;
      }
      PADS[id].forEach(function (g) {
        var group = document.createElement('div');
        group.className = 'shim-pad-group' + (g.tint ? ' tint-' + g.tint : '');
        if (g.title) {
          var t = document.createElement('span');
          t.className = 'shim-pad-title';
          t.textContent = g.title;
          group.appendChild(t);
        }
        var body = document.createElement('div');
        body.className = 'shim-pad-body';
        if (g.dpad) {
          var cross = document.createElement('div');
          cross.className = 'shim-dpad';
          ['up', 'left', 'down', 'right'].forEach(function (dir, i) {
            cross.appendChild(button(g.dpad[i], 'shim-pad-btn ' + dir));
          });
          body.appendChild(cross);
          if (g.fire) body.appendChild(button(g.fire, 'shim-pad-btn fire', g.fireLabel || 'Fire'));
        }
        if (g.piano) {
          var keys = document.createElement('div');
          keys.className = 'shim-piano';
          var white = 0;
          g.piano.forEach(function (name, i) {
            var black = [1, 3, 6, 8, 10].indexOf(i % 12) >= 0;
            var b = button(name, 'shim-pad-btn ' + (black ? 'black' : 'white'));
            if (black) b.style.left = 'calc(' + (white * 12.5) + '% - 5%)';   // on the line between two white keys
            else white++;
            keys.appendChild(b);
          });
          body.appendChild(keys);
        }
        (g.buttons || []).forEach(function (o) {
          body.appendChild(button(o.k, 'shim-pad-btn' + (o.big ? ' big' : ''), o.label));
        });
        group.appendChild(body);
        el.appendChild(group);
      });
      return function light(which, down) {
        buttons.forEach(function (b) {
          if (b.getAttribute('data-which') === String(which)) b.classList.toggle('lit', down);
        });
      };
    }
  };

  // An app page's own controller, at the bottom of the page; Controls in the toolbar shows or hides it.
  // It starts shown where the main pointer is a finger.
  function padOnPage(toggle) {
    var el = document.createElement('div');
    var light = pad.render(el, appId(), fireKey);
    document.body.appendChild(el);
    function show(on) {
      el.hidden = !on;
      toggle.setAttribute('aria-pressed', String(on));
      // From its top edge to the bottom of the page, so the gap under it is kept off the stage too.
      document.documentElement.style.setProperty('--pad-h', on ? Math.ceil(innerHeight - el.getBoundingClientRect().top) + 'px' : '0px');
    }
    show(pad.wanted(!!(window.matchMedia && matchMedia('(pointer: coarse)').matches)));
    toggle.addEventListener('click', function () {
      show(el.hidden);
      pad.remember(!el.hidden);
      toggle.blur();   // else Space (FlappyBird's flap) would press it again
    });
    addEventListener('resize', function () { if (!el.hidden) show(true); });
    ['keydown', 'keyup'].forEach(function (type) {
      document.addEventListener(type, function (e) { if (e.isTrusted) light(e.keyCode, type === 'keydown'); }, true);
    });
  }

  // In the home page's player the controller lives on the home page: it sends presses here, and hears
  // about real key presses (which go to this frame) to light its buttons.
  function padInPlayer() {
    window.addEventListener('message', function (e) {
      var m = e.data && e.data.oldjavaKey;
      if (e.origin !== location.origin || e.source !== window.parent || !m) return;
      if ((m.type === 'keydown' || m.type === 'keyup') && typeof m.name === 'string') fireKey(m.type, m.name);
    });
    ['keydown', 'keyup'].forEach(function (type) {
      document.addEventListener(type, function (e) {
        if (!e.isTrusted) return;
        window.parent.postMessage({ oldjavaKeyLit: { which: e.keyCode, down: type === 'keydown' } }, location.origin);
      }, true);
    });
  }

  // ---- app catalog and the ways to the next app ----
  // The home page's gallery is the one list of apps (name, hint, category, thumbnail), in gallery order.
  // App pages fetch it from ../index.html; the home page reads its own.
  function root() { return document.body.hasAttribute('data-app') ? '../' : ''; }

  var catalogPromise = null;
  var catalogDoc = null;   // the home page, for its own share tags
  function catalog() {
    if (!catalogPromise) {
      var source = root() ? fetch('../index.html').then(function (r) {
        if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
        return r.text();
      }).then(function (html) {
        return new DOMParser().parseFromString(html, 'text/html');
      }) : Promise.resolve(document);
      catalogPromise = source.then(function (doc) {
        catalogDoc = doc;
        return Array.prototype.map.call(doc.querySelectorAll('#apps li[data-app]'), function (li) {
          return {
            id: li.getAttribute('data-app'),
            cat: li.getAttribute('data-cat'),
            origin: li.getAttribute('data-origin') || 'decompiled',
            name: li.querySelector('strong').textContent,
            hint: li.querySelector('.body > span').textContent,   // not the home page's New/Played badge
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
      a.setAttribute('data-sound', 'none');   // the home page plays the new app's start
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
      playSound('close');
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
    playSound('open');
    catalog().then(function (apps) {
      if (apps.length) panel.insertBefore(passportLine(apps), list);
      apps.forEach(function (app) { list.appendChild(appCard(app, app.id === appId())); });
      var here = list.querySelector('.current');
      if (here) here.scrollIntoView({ block: 'nearest' });
      (list.querySelector('.shim-app:not(.current)') || close).focus();
    });
  }

  // ---- share ----
  // Share opens a panel with the link preview the page gets when it is posted (its card from cards/ with the
  // title and description of its share tags, both written by web/cards.ps1), a message with the visitor's
  // passport count, the link, and ways to send it: the system share sheet where there is one, X, Bluesky,
  // Reddit, email, or Copy. id is an app (its page is shared) or null (the arcade: the home page). A shared
  // app link carries ?shared=N, the sender's stamp count, and the title screen it opens on says so.
  var shareBox = null;
  var SHARED = /[?&]shared=(\d{1,3})(?:&|$)/.exec(location.search);
  function openShare(id, opener) {
    if (embedded && window.parent !== window) {   // the home page shows it over its player, at full size
      window.parent.postMessage({ oldjavaShare: id || '' }, location.origin);
      return;
    }
    if (shareBox) return;
    // The panel belongs to the page, which a fullscreen player (Big screen) would cover.
    if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(function () {});
    var scrim = document.createElement('div');
    scrim.className = 'shim-scrim';
    var panel = document.createElement('div');
    panel.className = 'shim-share';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-modal', 'true');
    var head = document.createElement('div');
    head.className = 'shim-drawer-head';
    var title = document.createElement('strong');
    function shut() {
      playSound('close');
      scrim.remove();
      panel.remove();
      shareBox = null;
      if (opener && opener.isConnected) opener.focus();
    }
    head.appendChild(title);
    head.appendChild(closeButton(shut));
    panel.appendChild(head);
    var body = document.createElement('div');
    body.className = 'shim-share-body';
    panel.appendChild(body);
    scrim.addEventListener('click', shut);
    ownKeys(panel, shut);
    document.body.appendChild(scrim);
    document.body.appendChild(panel);
    shareBox = panel;
    playSound('open');

    catalog().then(function (apps) {
      var app = id ? apps[indexOf(apps, id)] : null;
      var count = apps.filter(function (a) { return played[a.id]; }).length;
      var n = apps.length;
      var url = siteRoot + (app ? app.id + '/' : '') + 'index.html' + (app && count ? '?shared=' + count : '');
      var name = app ? app.name : 'the arcade';
      title.textContent = 'Share ' + name;
      panel.setAttribute('aria-label', 'Share ' + name);
      var message = app
        ? 'Play ' + app.name + ' in your browser: an old Java app, running on TeaVM.' +
          (count ? ' I have played ' + count + ' of ' + n + ' apps in this arcade.' : '')
        : count >= n ? 'I played all ' + n + ' old Java apps in this browser arcade. Arcade complete!'
        : count ? 'I have played ' + count + ' of ' + n + ' old Java apps in this browser arcade. How many can you play?'
        : n + ' old Java Swing apps and games, playable in the browser.';
      var homeTag = catalogDoc && catalogDoc.querySelector('meta[property="og:description"]');

      // The preview: what a chat or a feed shows for the link.
      var label = document.createElement('p');
      label.className = 'shim-share-label';
      label.textContent = 'How the link looks when it is posted';
      var preview = document.createElement('a');
      preview.className = 'shim-share-preview';
      preview.href = url;
      preview.target = '_blank';
      preview.rel = 'noopener';
      var img = document.createElement('img');
      img.src = root() + 'cards/' + (app ? app.id : 'index') + '.jpg';
      img.alt = '';
      img.width = 1200;
      img.height = 630;
      var cap = document.createElement('span');
      cap.className = 'shim-share-cap';
      var host = document.createElement('small');
      host.textContent = location.host;
      var t = document.createElement('strong');
      t.textContent = app ? app.name + ' | Old Java Applications' : 'Old Java Applications';
      var d = document.createElement('span');
      d.textContent = app ? app.hint : homeTag ? homeTag.content : '';
      cap.appendChild(host);
      cap.appendChild(t);
      cap.appendChild(d);
      preview.appendChild(img);
      preview.appendChild(cap);

      var msgLabel = document.createElement('label');
      msgLabel.className = 'shim-share-msg';
      msgLabel.textContent = 'Message';
      var text = document.createElement('textarea');
      text.rows = 3;
      text.value = message;
      msgLabel.appendChild(text);

      var linkRow = document.createElement('div');
      linkRow.className = 'shim-share-link';
      var link = document.createElement('input');
      link.readOnly = true;
      link.value = url;
      link.setAttribute('aria-label', 'Link');
      link.addEventListener('focus', function () { link.select(); });
      var copy = document.createElement('button');
      copy.type = 'button';
      copy.className = 'shim-share-copy';
      copy.textContent = 'Copy link';
      copy.setAttribute('data-sound', 'none');
      copy.addEventListener('click', function () {
        function done(ok) {
          copy.textContent = ok ? 'Copied ✓' : 'Press Ctrl+C';
          playSound(ok ? 'saved' : 'tap');
          setTimeout(function () { copy.textContent = 'Copy link'; }, 2000);
        }
        link.select();
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(url).then(function () { done(true); }, function () { done(false); });
        } else {
          var ok = false;
          try { ok = document.execCommand('copy'); } catch (e) { /* select it by hand */ }
          done(ok);
        }
      });
      linkRow.appendChild(link);
      linkRow.appendChild(copy);

      var ways = document.createElement('div');
      ways.className = 'shim-share-ways';
      function way(label, href, cls) {
        var a = document.createElement('a');
        a.className = 'shim-share-way' + (cls ? ' ' + cls : '');
        a.textContent = label;
        a.target = '_blank';
        a.rel = 'noopener';
        a.addEventListener('click', function () { a.href = href(text.value.trim()); });
        a.href = href(message);
        ways.appendChild(a);
        return a;
      }
      var e = encodeURIComponent;
      var subject = app ? app.name + ', an old Java app in your browser' : 'Old Java Applications';
      if (navigator.share) {
        var native = document.createElement('button');
        native.type = 'button';
        native.className = 'shim-share-way primary';
        native.textContent = 'Share…';
        native.addEventListener('click', function () {
          navigator.share({ title: subject, text: text.value.trim(), url: url }).catch(function () { /* closed */ });
        });
        ways.appendChild(native);
      }
      way('X', function (m) { return 'https://x.com/intent/tweet?text=' + e(m) + '&url=' + e(url); });
      way('Bluesky', function (m) { return 'https://bsky.app/intent/compose?text=' + e(m + ' ' + url); });
      way('Reddit', function (m) { return 'https://www.reddit.com/submit?url=' + e(url) + '&title=' + e(m); });
      way('Email', function (m) { return 'mailto:?subject=' + e(subject) + '&body=' + e(m + '\n\n' + url); }).removeAttribute('target');

      [label, preview, msgLabel, linkRow, ways].forEach(function (el) { body.appendChild(el); });
      ((navigator.share && ways.firstChild) || copy).focus();
    });
  }
  window.oldjavaShare = { open: openShare };

  // ---- source viewer ----
  // The Java this page runs, next to it: Source (toolbar or title screen) docks a panel on the right with the
  // app's README and every source file it was compiled from (copied into <App>/source/ by the build, listed
  // in source/files.txt), highlighted, with line numbers. The app keeps running beside it. Class names that
  // are files of the app, and file paths in the README, open those files. The address follows the open file
  // and line (#source:src/minesweeper/GameFrame.java:42), so a link opens the same place.
  var JAVA_KEYWORDS = ('abstract assert boolean break byte case catch char class const continue default do double ' +
    'else enum extends final finally float for goto if implements import instanceof int interface long native new ' +
    'package private protected public return short static strictfp super switch synchronized this throw throws ' +
    'transient try void volatile while true false null var').split(' ');
  var JAVA_TOKEN = /(\/\*[\s\S]*?\*\/|\/\/[^\n]*)|("(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*')|(@[A-Za-z_]\w*)|(\b0[xX][0-9a-fA-F_]+[lL]?\b|\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[lLfFdD]?\b)|([A-Za-z_$][\w$]*)/g;
  var sourcePanel = null;
  var sourceList = null;

  function sourceFiles() {
    if (!sourceList) {
      sourceList = fetch('source/files.txt').then(function (r) {
        if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
        return r.text();
      }).then(function (text) {
        return text.split(/\r?\n/).filter(Boolean).sort(function (a, b) {
          return (a === 'README.md' ? -1 : b === 'README.md' ? 1 : 0) || (a < b ? -1 : a > b ? 1 : 0);
        });
      });
      sourceList.catch(function () { sourceList = null; });
    }
    return sourceList;
  }

  // Tokens as [class, text]; class '' is plain text.
  function javaTokens(text) {
    var out = [];
    var last = 0;
    var m;
    JAVA_TOKEN.lastIndex = 0;
    while ((m = JAVA_TOKEN.exec(text))) {
      if (m.index > last) out.push(['', text.slice(last, m.index)]);
      var cls = m[1] ? 'c' : m[2] ? 's' : m[3] ? 'a' : m[4] ? 'n'
        : JAVA_KEYWORDS.indexOf(m[5]) >= 0 ? 'k' : /^[A-Z]/.test(m[5]) ? 't' : '';
      out.push([cls, m[0]]);
      last = JAVA_TOKEN.lastIndex;
    }
    if (last < text.length) out.push(['', text.slice(last)]);
    return out;
  }

  // One element per line, with its number (tokens that span lines, like block comments, are split).
  // A class name that is a file of the app links to it.
  function codeLines(text, linkFor) {
    var code = document.createElement('div');
    code.className = 'shim-source-code';
    var row = null;   // the current line's text cell
    var n = 0;
    function newLine() {
      var line = document.createElement('div');
      line.className = 'l';
      line.id = 'L' + (++n);
      var num = document.createElement('span');
      num.className = 'n';
      num.textContent = n;
      row = document.createElement('span');
      row.className = 't';
      line.appendChild(num);
      line.appendChild(row);
      code.appendChild(line);
    }
    newLine();
    javaTokens(text.replace(/\r\n?/g, '\n').replace(/\n$/, '')).forEach(function (t) {
      t[1].split('\n').forEach(function (part, i) {
        if (i) newLine();
        if (!part) return;
        var target = t[0] === 't' && linkFor(part);
        if (!t[0]) { row.appendChild(document.createTextNode(part)); return; }
        var el = document.createElement(target ? 'a' : 'span');
        if (target) {
          el.href = '#source:' + target;
          el.setAttribute('data-file', target);
          el.title = 'Open ' + target;
        }
        el.className = 'tk-' + t[0];
        el.textContent = part;
        row.appendChild(el);
      });
    });
    return { el: code, lines: n };
  }

  // The app READMEs use headings, paragraphs, two levels of lists, code fences, **bold** and `code`.
  // A `code` span naming one of the app's files opens it.
  function markdown(text, files) {
    var root = document.createElement('div');
    root.className = 'shim-source-md';
    function inline(el, s) {
      s.split(/(`[^`]+`|\*\*[^*]+\*\*)/).forEach(function (part) {
        if (!part) return;
        var node;
        if (part[0] === '`') {
          var path = part.slice(1, -1);
          var isFile = files.indexOf(path) >= 0;
          node = document.createElement(isFile ? 'a' : 'code');
          if (isFile) {
            node.href = '#source:' + path;
            node.setAttribute('data-file', path);
            node.className = 'file';
          }
          node.textContent = path;
        } else if (part.slice(0, 2) === '**') {
          node = document.createElement('strong');
          node.textContent = part.slice(2, -2);
        } else {
          node = document.createTextNode(part);
        }
        el.appendChild(node);
      });
    }
    var lines = text.replace(/\r\n?/g, '\n').split('\n');
    var para = null;
    var lists = [];   // the open <ul> elements, one per indent level
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i];
      var m;
      if (/^```/.test(line)) {
        var pre = document.createElement('pre');
        var body = [];
        while (++i < lines.length && !/^```/.test(lines[i])) body.push(lines[i]);
        pre.textContent = body.join('\n');
        root.appendChild(pre);
        para = null; lists = [];
      } else if ((m = /^(#{1,3})\s+(.*)$/.exec(line))) {
        var h = document.createElement('h' + (m[1].length + 1));
        inline(h, m[2]);
        root.appendChild(h);
        para = null; lists = [];
      } else if ((m = /^(\s*)- (.*)$/.exec(line))) {
        var depth = Math.min(m[1].length >> 1, lists.length);
        lists.length = Math.min(lists.length, depth + 1);
        if (lists.length === depth) {
          var ul = document.createElement('ul');
          (depth ? lists[depth - 1].lastChild : root).appendChild(ul);
          lists.push(ul);
        }
        var li = document.createElement('li');
        inline(li, m[2]);
        lists[depth].appendChild(li);
        para = null;
      } else if (!line.trim()) {
        para = null; lists = [];
      } else if (lists.length) {
        inline(lists[lists.length - 1].lastChild, ' ' + line.trim());
      } else {
        if (!para) { para = document.createElement('p'); root.appendChild(para); } else para.appendChild(document.createTextNode(' '));
        inline(para, line);
      }
    }
    return root;
  }

  function parseSourceHash() {
    var m = /^#source(?::(.+?))?(?::(\d+))?$/.exec(decodeURIComponent(location.hash));
    return m ? { file: m[1] || null, line: m[2] ? +m[2] : 0 } : null;
  }

  function sourceFromHash() {
    var want = parseSourceHash();
    if (want) openSource(want.file, want.line);
  }
  window.addEventListener('hashchange', function () {
    if (document.body.hasAttribute('data-app') && !embedded) sourceFromHash();
  });

  function openSource(file, line) {
    if (sourcePanel) { sourcePanel.show(file, line); return; }
    var opener = document.activeElement;
    var panel = document.createElement('aside');
    panel.className = 'shim-source';
    panel.setAttribute('aria-label', 'Source code');
    var head = document.createElement('div');
    head.className = 'shim-source-head';
    var title = document.createElement('div');
    title.className = 'shim-source-title';
    var name = document.createElement('strong');
    name.textContent = document.title;
    var origin = document.createElement('span');
    origin.className = 'shim-source-origin';
    origin.hidden = true;
    title.appendChild(name);
    title.appendChild(origin);
    var raw = document.createElement('a');
    raw.className = 'shim-source-raw';
    raw.target = '_blank';
    raw.rel = 'noopener';
    raw.textContent = 'Raw';
    var sub = document.createElement('p');
    sub.className = 'shim-source-sub';
    sub.textContent = 'Loading the source…';
    var body = document.createElement('div');
    body.className = 'shim-source-body';
    var nav = document.createElement('nav');
    nav.className = 'shim-source-files';
    nav.setAttribute('aria-label', 'Files');
    var view = document.createElement('div');
    view.className = 'shim-source-view';
    view.tabIndex = -1;
    var path = document.createElement('div');
    path.className = 'shim-source-path';
    var page = document.createElement('div');
    page.className = 'shim-source-page';
    view.appendChild(path);
    view.appendChild(page);
    body.appendChild(nav);
    body.appendChild(view);

    function shut() {
      playSound('close');
      panel.remove();
      document.documentElement.removeAttribute('data-source');
      sourcePanel = null;
      history.replaceState(null, '', location.pathname + location.search);
      if (opener && opener.focus && document.contains(opener)) opener.focus();
    }
    head.appendChild(title);
    head.appendChild(raw);
    head.appendChild(closeButton(shut));
    panel.appendChild(head);
    panel.appendChild(sub);
    panel.appendChild(body);
    ownKeys(panel, shut);
    document.body.appendChild(panel);
    document.documentElement.setAttribute('data-source', '');
    playSound('open');

    var files = [];
    var texts = {};
    var current = null;
    function text(f) {
      if (!texts[f]) {
        texts[f] = fetch('source/' + f).then(function (r) {
          if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
          return r.text();
        });
        texts[f].catch(function () { delete texts[f]; });
      }
      return texts[f];
    }
    // A class name in a file: the file of that class, preferring one in the same folder.
    function linkFor(from) {
      var dir = from.slice(0, from.lastIndexOf('/') + 1);
      return function (word) {
        var near = dir + word + '.java';
        if (files.indexOf(near) >= 0) return near === from ? null : near;
        for (var i = 0; i < files.length; i++) {
          if (files[i].slice(files[i].lastIndexOf('/') + 1) === word + '.java') return files[i];
        }
        return null;
      };
    }
    function mark(line) {
      var old = page.querySelector('.l.on');
      if (old) old.classList.remove('on');
      var row = line && page.querySelector('#L' + line);
      if (row) {
        row.classList.add('on');
        row.scrollIntoView({ block: 'center' });
      }
    }
    function address(f, line) {
      history.replaceState(null, '', location.pathname + location.search + '#source:' + f + (line ? ':' + line : ''));
    }
    function show(f, line) {
      if (!files.length) return;
      if (files.indexOf(f) < 0) f = files[0];
      Array.prototype.forEach.call(nav.querySelectorAll('button'), function (b) {
        b.setAttribute('aria-current', String(b.getAttribute('data-file') === f));
      });
      var tab = nav.querySelector('button[aria-current="true"]');
      if (tab) nav.scrollTo({ left: tab.offsetLeft - nav.offsetLeft - 8, top: tab.offsetTop - nav.offsetTop - 40 });
      raw.href = 'source/' + f;
      address(f, line);
      if (current === f) { mark(line); return; }
      current = f;
      path.textContent = f;
      page.textContent = '';
      text(f).then(function (t) {
        if (current !== f) return;
        if (/\.md$/.test(f)) {
          page.appendChild(markdown(t, files));
        } else {
          var code = codeLines(t, linkFor(f));
          page.appendChild(code.el);
          path.textContent = f + ' · ' + code.lines + ' lines';
        }
        view.scrollTop = 0;
        mark(line);
      }, function (e) {
        if (current !== f) return;
        page.textContent = 'Could not load ' + f + ' (' + e.message + ').';
        current = null;
      });
    }
    panel.addEventListener('click', function (e) {
      var link = e.target.closest('[data-file]');
      if (link) {
        if (e.button || e.ctrlKey || e.metaKey || e.shiftKey) return;
        e.preventDefault();
        show(link.getAttribute('data-file'), 0);
        if (link.tagName === 'A') view.focus({ preventScroll: true });
        return;
      }
      var num = e.target.closest('.shim-source-code .n');
      if (num) {
        var n = +num.textContent;
        mark(n);
        address(current, n);
      }
    });
    sourcePanel = { show: show };

    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (!app) return;
      name.textContent = app.name;
      var original = app.origin === 'original';
      origin.textContent = original ? 'Original source' : 'Decompiled';
      origin.title = original ? 'The author’s own Eclipse source' : 'Recovered by decompiling the app’s jar';
      origin.classList.add(original ? 'original' : 'decompiled');
      origin.hidden = false;
    });
    sourceFiles().then(function (list) {
      files = list;
      var java = files.filter(function (f) { return /\.java$/.test(f); });
      var dir = '';
      files.forEach(function (f) {
        var folder = f.indexOf('/') >= 0 ? f.slice(0, f.lastIndexOf('/')).replace(/^src\/?/, '') : '';
        if (folder && folder !== dir) {
          var label = document.createElement('span');
          label.className = 'dir';
          label.textContent = folder.replace(/\//g, '.');
          nav.appendChild(label);
        }
        dir = folder;
        var b = document.createElement('button');
        b.type = 'button';
        b.setAttribute('data-file', f);
        b.setAttribute('data-sound', 'none');
        b.title = f;
        b.textContent = f.slice(f.lastIndexOf('/') + 1);
        nav.appendChild(b);
      });
      Promise.all(java.map(text)).then(function (all) {
        var count = all.reduce(function (sum, t) { return sum + t.replace(/\r?\n$/, '').split('\n').length; }, 0);
        sub.textContent = java.length + ' Java file' + (java.length === 1 ? '' : 's') + ', ' + count +
          ' lines. This is the code running on this page: TeaVM compiled it to JavaScript.';
      }, function () { sub.textContent = 'The Java this page was compiled from.'; });
      show(file, line);
      view.focus({ preventScroll: true });
    }, function (e) {
      sub.textContent = 'Could not load the source list (' + e.message + '). Build the site with Maven (see web/README.md), or go online.';
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
    playSound('exit');
    if (!embedded) {
      var home = document.createElement('a');
      home.className = 'shim-exit-home';
      home.href = '../index.html';
      home.textContent = 'All apps';
      box.appendChild(home);
    }
    var tell = document.createElement('button');
    tell.type = 'button';
    tell.className = 'shim-exit-share';
    tell.setAttribute('data-sound', 'none');
    tell.textContent = '↗ Share';
    tell.addEventListener('click', function () { openShare(appId(), tell); });
    box.appendChild(tell);
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

  // ---- title screen ----
  // An app page opens on a title screen while its files load: the app's thumbnail, name, category, hint and
  // passport stamp, a boot log of the files as they arrive (real names and sizes) and a Play button. The app
  // starts on the visitor's click or key press, so its first sounds (MusicTheoryExpert's demo, the site's
  // start sound) are allowed to play. Run again skips it (see overlay). Returns { loaded(file), ready(start) }.
  var AGAIN_KEY = 'oldjava.again';
  var CATS = { game: 'Game', music: 'Music', toy: 'Toy', tool: 'Tool' };
  function size(bytes) {
    return bytes >= 1048576 ? (bytes / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(bytes / 1024)) + ' KB';
  }
  // What an app saved on earlier visits: web.rt.Saves keeps the files it writes under oldjava.saves.<App>, each byte
  // one character. Apps with a score file get their record read out; any other saves are named.
  // NAME=stars,score rows (MusicTheoryExpert's drills, NumberGuess's ranges, Star Catcher's nights): how many have a
  // star, and the stars.
  function starRows(text) {
    var won = 0, stars = 0;
    text.split('\n').forEach(function (row) {
      var n = parseInt((row.split('=')[1] || '').split(',')[0], 10);
      if (/^[A-Z_]+=/.test(row) && n >= 0 && n <= 3) { won += n > 0 ? 1 : 0; stars += n; }
    });
    return { won: won, stars: stars };
  }
  var RECORDS = {
    // Today's course (.flappybird-today.txt) counts only on the day it was flown.
    FlappyBird: { file: '.flappybird-highscore.txt', show: function (text, saved) {
      var best = parseInt(text, 10);
      if (!(best > 0)) return '';
      var line = 'Your best: ' + best + (best === 1 ? ' pipe' : ' pipes');
      var today = /date=(\d+)\s+best=(\d+)/.exec(saved('.flappybird-today.txt') || '');
      var now = new Date();
      if (today && +today[1] === now.getFullYear() * 10000 + (now.getMonth() + 1) * 100 + now.getDate() && +today[2] > 0) {
        line += '; today\'s course: ' + today[2];
      }
      return line;
    } },
    Minesweeper: { file: '.minesweeper-best.txt', show: function (text) {
      var names = { BEGINNER: 'Beginner', CLASSIC: 'Classic', EXPERT: 'Expert' };
      var times = text.split('\n').map(function (row) {
        var kv = row.split('=');
        var ms = +kv[1];
        if (!names[kv[0]] || !(ms > 0)) return '';
        var tenths = Math.floor(ms / 100), secs = Math.floor(tenths / 10);
        return names[kv[0]] + ' ' + Math.floor(secs / 60) + ':' + (secs % 60 < 10 ? '0' : '') + secs % 60 + '.' + tenths % 10;
      }).filter(Boolean);
      return times.length ? 'Your best times: ' + times.join(', ') : '';
    } },
    UltimateTicTacToe: { file: '.ultimatetictactoe-record.txt', show: function (text) {
      var names = { EASY: 'Easy', HARD: 'Hard' };
      var rows = text.split('\n').map(function (row) {
        var kv = row.split('=');
        var n = (kv[1] || '').split(',').map(Number);
        if (!names[kv[0]] || n.length !== 3) return '';
        return names[kv[0]] + ' ' + n[0] + (n[0] === 1 ? ' win, ' : ' wins, ') + n[1] + (n[1] === 1 ? ' loss, ' : ' losses, ') + n[2] + (n[2] === 1 ? ' draw' : ' draws');
      }).filter(Boolean);
      return rows.length ? 'Your record vs the computer: ' + rows.join('; ') : '';
    } },
    SynthesizerKeyboard: { file: '.synthesizer-songs.txt', show: function (text) {
      var names = { MARY: 'Mary\'s Lamb', TWINKLE: 'Twinkle', ODE: 'Ode to Joy', ROW: 'Row Your Boat', JINGLE: 'Jingle Bells' };
      var rows = text.split('\n').map(function (row) {
        var kv = row.split('=');
        var stars = parseInt(kv[1], 10);
        if (!names[kv[0]] || !(stars > 0)) return '';
        return names[kv[0]] + ' ' + stars + (stars === 1 ? ' star' : ' stars');
      }).filter(Boolean);
      return rows.length ? 'Your songs: ' + rows.join(', ') : '';
    } },
    RubiksCube2D: { file: '.rubikscube-levels.txt', show: function (text) {
      var solved = 0, stars = 0;
      text.split('\n').forEach(function (row) {
        var kv = row.split('=');
        var n = parseInt(kv[1], 10);
        if (/^\d+$/.test(kv[0]) && n >= 1 && n <= 3) { solved++; stars += n; }
      });
      return solved ? 'Your levels: ' + solved + ' of 20 solved, ' + stars + ' of 60 stars' : '';
    } },
    MusicTheoryExpert: { file: '.musictheory-drills.txt', show: function (text) {
      var r = starRows(text);
      return r.won ? 'Your ear training: ' + r.won + ' of 6 drills passed, ' + r.stars + ' of 18 stars' : '';
    } },
    NumberGuess: { file: '.numberguess-record.txt', show: function (text) {
      var r = starRows(text);
      return r.won ? 'Your ranges: ' + r.won + ' of 5 won, ' + r.stars + ' of 15 stars' : '';
    } },
    SpriteFloatyStars: { file: '.spritefloatystars-nights.txt', show: function (text) {
      var r = starRows(text);
      return r.won ? 'Your Star Catcher nights: ' + r.won + ' of 6 caught, ' + r.stars + ' of 18 stars' : '';
    } },
    TankGame: { file: '.tankgame-record.txt', show: function (text) {
      var n = text.trim().split(',').map(Number);
      if (n.length !== 2 || !(n[0] + n[1] > 0)) return '';
      return 'Your matches vs the computer: ' + n[0] + (n[0] === 1 ? ' win, ' : ' wins, ') + n[1] + (n[1] === 1 ? ' loss' : ' losses');
    } }
  };

  function savedRecord(id) {
    var saves;
    try { saves = JSON.parse(localStorage.getItem('oldjava.saves.' + id) || '{}'); } catch (e) { return ''; }
    var paths = Object.keys(saves);
    var rule = RECORDS[id];
    // The text of a saved file by its name, or null.
    function saved(name) {
      for (var i = 0; i < paths.length; i++) {
        if (paths[i].split('/').pop() === name) return saves[paths[i]];
      }
      return null;
    }
    if (rule) {
      var text = saved(rule.file);
      return text === null ? '' : rule.show(text, saved);
    }
    return paths.length ? 'Saved here from last time: ' + paths.map(function (p) { return p.split('/').pop(); }).join(', ') : '';
  }

  function titleScreen(files) {
    var html = document.documentElement;
    html.setAttribute('data-title', '');
    var screen = document.createElement('div');
    screen.className = 'shim-title';
    screen.setAttribute('role', 'dialog');
    screen.setAttribute('data-sound', 'none');   // starting plays the start sound
    var card = document.createElement('div');
    card.className = 'shim-title-card';
    var art = document.createElement('div');
    art.className = 'shim-title-art';
    var img = document.createElement('img');
    img.alt = '';
    img.hidden = true;
    img.addEventListener('load', function () { img.hidden = false; });
    art.appendChild(img);
    var text = document.createElement('div');
    text.className = 'shim-title-text';
    var cat = document.createElement('span');
    cat.className = 'shim-title-cat';
    var name = document.createElement('h1');
    name.textContent = document.title;
    screen.setAttribute('aria-label', document.title);
    var hint = document.createElement('p');
    hint.className = 'shim-title-hint';
    var stamp = document.createElement('p');
    stamp.className = 'shim-title-stamp';
    var record = document.createElement('p');
    record.className = 'shim-title-record';
    var recordText = savedRecord(appId());
    record.hidden = !recordText;
    record.appendChild(document.createTextNode(recordText + ' '));
    // Forget clears the app's saves (a second click confirms), so it starts again from its bundled files.
    var forget = document.createElement('button');
    forget.type = 'button';
    forget.className = 'shim-title-forget';
    forget.textContent = 'Forget';
    forget.addEventListener('click', function () {
      if (!forget.classList.contains('confirm')) {
        forget.classList.add('confirm');
        forget.textContent = 'Really forget?';
        return;
      }
      try { localStorage.removeItem('oldjava.saves.' + appId()); } catch (e) { /* nothing was kept */ }
      record.hidden = true;
    });
    record.appendChild(forget);
    var play = document.createElement('button');
    play.type = 'button';
    play.className = 'shim-title-play';
    play.disabled = true;
    play.textContent = 'Loading…';
    var press = document.createElement('span');
    press.className = 'shim-title-press';
    press.textContent = 'or press any key';
    var meter = document.createElement('span');
    meter.className = 'shim-meter shim-title-meter';
    var log = document.createElement('pre');
    log.className = 'shim-title-log';
    log.setAttribute('aria-hidden', 'true');
    var read = document.createElement('a');
    read.className = 'shim-title-source';
    read.href = '#source';
    read.setAttribute('data-sound', 'none');
    read.textContent = 'Read the source';
    [cat, name, hint, stamp, record, play, press, read].forEach(function (el) { text.appendChild(el); });
    card.appendChild(art);
    card.appendChild(text);
    card.appendChild(meter);
    card.appendChild(log);
    screen.appendChild(card);
    document.body.appendChild(screen);

    function line(label, value, cls) {
      var row = document.createElement('span');
      row.className = 'row' + (cls ? ' ' + cls : '');
      var l = document.createElement('span');
      l.textContent = label;
      var v = document.createElement('span');
      v.textContent = value;
      row.appendChild(l);
      row.appendChild(v);
      log.appendChild(row);
      log.scrollTop = log.scrollHeight;
    }
    // classes.js has loaded by now (a blocking script in <head>): the app, compiled to JavaScript by TeaVM.
    var entry = window.performance && performance.getEntriesByName &&
      performance.getEntriesByName(new URL('classes.js', location.href).href)[0];
    line('classes.js', entry && entry.decodedBodySize ? size(entry.decodedBodySize) : 'ok');
    var total = files.length + 1;
    var done = 1;
    function progress() { meter.style.setProperty('--p', String(done / total)); }
    progress();

    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (!app) return;
      img.src = app.shot;
      cat.textContent = CATS[app.cat] || '';
      name.textContent = app.name;
      hint.textContent = app.hint;
      var count = apps.filter(function (a) { return played[a.id]; }).length;
      stamp.textContent = played[app.id]
        ? '✓ In your passport since ' + new Date(played[app.id]).toLocaleDateString()
        : 'New: playing it adds stamp ' + (count + 1) + ' of ' + apps.length + ' to your passport';
      stamp.classList.toggle('played', !!played[app.id]);
      if (SHARED) {
        var theirs = Math.min(+SHARED[1], apps.length);
        var gift = document.createElement('p');
        gift.className = 'shim-title-shared';
        gift.textContent = 'Shared with you by a player with ' + theirs + ' of ' + apps.length + ' stamps. ' +
          (count ? 'You have ' + count + '.' : 'Play to start your own passport.');
        text.insertBefore(gift, cat);
      }
    });

    var started = null;
    function go(e) {
      if (!started || screen.classList.contains('leaving')) return;
      if (e && e.type === 'keydown') {
        if (e.key === 'Tab' || e.key === 'Shift' || e.key === 'Control' || e.key === 'Alt' || e.key === 'Meta') return;
        if (e.target.closest && e.target.closest('.shim-toolbar, .shim-drawer, .shim-share, .shim-source, .shim-title-forget')) return;   // keys for those panels
        e.preventDefault();
        e.stopPropagation();
        // Its release must not reach the app either: it never saw the press.
        var code = e.code;
        document.addEventListener('keyup', function swallow(u) {
          if (u.code !== code) return;
          u.stopPropagation();
          document.removeEventListener('keyup', swallow, true);
        }, true);
      }
      document.removeEventListener('keydown', go, true);
      screen.classList.add('leaving');
      html.removeAttribute('data-title');
      var start = started;
      started = null;
      start();
      setTimeout(function () { screen.remove(); }, 400);
    }
    screen.addEventListener('click', function (e) { if (!e.target.closest('a, .shim-title-forget')) go(e); });

    return {
      loaded: function (file) {
        var bytes = window.__shimFiles && window.__shimFiles[file];
        done++;
        progress();
        line(file, bytes ? size(bytes.byteLength) : 'missing', bytes ? '' : 'bad');
      },
      ready: function (start) {
        started = start;
        line('Ready.', '', 'ready');
        screen.classList.add('ready');
        play.disabled = false;
        play.textContent = '▶ Play';
        if (!sourcePanel) play.focus({ preventScroll: true });   // keep the keyboard in an open source panel
        document.addEventListener('keydown', go, true);
      }
    };
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
      b.addEventListener('click', function () {
        // The visitor already pressed start once: the reloaded page skips the title screen.
        try { sessionStorage.setItem(AGAIN_KEY, appId()); } catch (e) { /* the title screen shows again */ }
        location.reload();
      });
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

  // ---- site sounds (window.oldjavaSound) ----
  // The site's own controls and moments (not the apps, which keep their own sound) get short synthesized
  // sounds, in a voice that follows the skin: soft glassy bells in Modern, a PC-speaker beep in Classic,
  // chiptune blips in Arcade, where a new app powers on with a tube thump and a rising whine. Every tune is a
  // list of semitones above the voice's root note (the last note is held longer); 'thump' and 'power' are the
  // two non-note sounds.
  // No file is downloaded. Sound plays only after the visitor has clicked or pressed a key on the page.
  var SOUND_KEY = 'oldjava.sound';
  var VOICES = {
    modern: { wave: 'sine', root: 523.25, step: 0.075, len: 0.32, hold: 1.8, vol: 0.2, bell: true },
    classic: { wave: 'square', root: 440, step: 0.1, len: 0.085, hold: 2, vol: 0.045, lowpass: 2200 },
    arcade: { wave: 'square', root: 659.25, step: 0.055, len: 0.07, hold: 4, vol: 0.055, blip: true }
  };
  var TUNES = {
    tap: [12],
    open: [7, 12],
    close: [12, 7],
    start: [0, 4, 7, 12],
    stamp: ['thump', 7, 16],
    complete: ['thump', 0, 4, 7, 12, 7, 12, 16, 19, 24],
    exit: [7, 3, 0],
    skin: [0, 5, 9, 12],
    saved: [0, 7, 12, 19],
    offline: [0, -6]
  };
  // Arcade's own tunes: a power-on for a new app and a coin for a stamp (B5 then E6, held).
  var ARCADE_TUNES = { start: ['power', 7, 12], stamp: ['thump', 7, 12] };

  function soundOn() {
    try { return localStorage.getItem(SOUND_KEY) !== 'off'; } catch (e) { return true; }
  }

  var sfxOut = null;   // the site sounds' master gain, on the same AudioContext as the app voices
  var noiseBuffer = null;
  var tapPending = false;

  function tone(a, v, semi, at, len, vol) {
    var f = v.root * Math.pow(2, semi / 12);
    var gain = a.createGain();
    gain.gain.setValueAtTime(0.0001, at);
    gain.gain.exponentialRampToValueAtTime(vol, at + 0.004);
    if (v.bell) gain.gain.exponentialRampToValueAtTime(0.0001, at + len);
    else {
      gain.gain.setValueAtTime(vol, at + len - 0.01);
      gain.gain.exponentialRampToValueAtTime(0.0001, at + len);
    }
    var sink = gain;
    if (v.lowpass) {
      var lp = a.createBiquadFilter();
      lp.frequency.value = v.lowpass;
      gain.connect(lp);
      sink = lp;
    }
    sink.connect(sfxOut);
    // A bell is the note plus a quieter partial two octaves up; a blip drops onto its note from a fifth above.
    (v.bell ? [[1, 1], [4, 0.18]] : [[1, 1]]).forEach(function (p) {
      var o = a.createOscillator();
      o.type = v.wave;
      o.frequency.setValueAtTime(f * p[0] * (v.blip ? 1.5 : 1), at);
      if (v.blip) o.frequency.exponentialRampToValueAtTime(f, at + 0.018);
      var g = gain;
      if (p[1] !== 1) {
        g = a.createGain();
        g.gain.value = p[1];
        g.connect(gain);
      }
      o.connect(g);
      o.start(at);
      o.stop(at + len + 0.02);
    });
  }

  // A low, filtered knock: the stamp pressed down (and the tube switching on).
  function thump(a, at, vol) {
    if (!noiseBuffer) {
      noiseBuffer = a.createBuffer(1, Math.round(a.sampleRate * 0.15), a.sampleRate);
      var d = noiseBuffer.getChannelData(0);
      for (var i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
    }
    var n = a.createBufferSource();
    n.buffer = noiseBuffer;
    var lp = a.createBiquadFilter();
    lp.frequency.value = 380;
    var g = a.createGain();
    g.gain.setValueAtTime(vol, at);
    g.gain.exponentialRampToValueAtTime(0.0001, at + 0.14);
    n.connect(lp).connect(g).connect(sfxOut);
    n.start(at);
    var o = a.createOscillator();
    o.frequency.setValueAtTime(140, at);
    o.frequency.exponentialRampToValueAtTime(45, at + 0.12);
    var og = a.createGain();
    og.gain.setValueAtTime(vol * 0.9, at);
    og.gain.exponentialRampToValueAtTime(0.0001, at + 0.13);
    o.connect(og).connect(sfxOut);
    o.start(at);
    o.stop(at + 0.15);
  }

  // The tube warming up, timed with the Arcade skin's power-on animation (shim.css, arcade-on).
  function power(a, at) {
    thump(a, at, 0.35);
    var o = a.createOscillator();
    o.type = 'sawtooth';
    o.frequency.setValueAtTime(70, at);
    o.frequency.exponentialRampToValueAtTime(1800, at + 0.28);
    var lp = a.createBiquadFilter();
    lp.frequency.value = 2600;
    var g = a.createGain();
    g.gain.setValueAtTime(0.0001, at);
    g.gain.exponentialRampToValueAtTime(0.05, at + 0.05);
    g.gain.exponentialRampToValueAtTime(0.0001, at + 0.3);
    o.connect(lp).connect(g).connect(sfxOut);
    o.start(at);
    o.stop(at + 0.32);
    return 0.34;   // the notes after it wait for the line to open up
  }

  function playSound(name) {
    if (name !== 'tap') tapPending = false;   // the click that caused this needs no tap of its own
    if (!soundOn() || document.documentElement.hasAttribute('data-shot')) return;
    if (navigator.userActivation && !navigator.userActivation.hasBeenActive) return;   // it could not start yet
    var skinName = document.documentElement.getAttribute('data-skin');
    var v = VOICES[skinName] || VOICES.modern;
    var tune = (skinName === 'arcade' && ARCADE_TUNES[name]) || TUNES[name];
    var a = ctx();
    if (!a || !tune) return;
    if (!sfxOut) {
      sfxOut = a.createGain();
      sfxOut.gain.value = 0.7;
      sfxOut.connect(a.destination);
    }
    var at = a.currentTime + 0.01;
    var quiet = name === 'tap' ? 0.45 : 1;
    tune.forEach(function (semi, i) {
      if (semi === 'thump') { thump(a, at, 0.3); at += 0.07; return; }
      if (semi === 'power') { at += power(a, at); return; }
      var last = i === tune.length - 1;
      tone(a, v, semi, at, v.len * (name === 'tap' ? 0.5 : last ? v.hold : 1), v.vol * quiet);
      at += v.step;
    });
  }

  function setSoundOn(on) {
    try { localStorage.setItem(SOUND_KEY, on ? 'on' : 'off'); } catch (e) { /* this page only */ }
    showSound();
    if (on) playSound('open');
  }

  function showSound() {
    var b = document.querySelector('.shim-toolbar .shim-sound');
    if (!b) return;
    var on = soundOn();
    b.setAttribute('aria-pressed', String(on));
    b.innerHTML = '♪ <span>' + (on ? 'Sound' : 'Muted') + '</span>';   // the word is hidden on narrow pages
    b.setAttribute('aria-label', 'Site sounds');
    b.title = on ? 'Site sounds are on (the apps keep their own sound). Click to mute.' : 'Site sounds are muted. Click to turn them on.';
  }
  window.addEventListener('storage', function (e) { if (e.key === SOUND_KEY || e.key === null) showSound(); });

  window.oldjavaSound = { play: playSound, on: soundOn, set: setSoundOn };

  // Any click on a site control (not in the app's windows or the controller, whose keys belong to the app)
  // taps, unless what it did made a sound of its own. data-sound="none" keeps a control silent.
  document.addEventListener('click', function (e) {
    var t = e.target && e.target.closest && e.target.closest('button, a[href], summary');
    if (!t || t.closest('#desktop, .shim-pad, [data-sound="none"]')) return;
    tapPending = true;
    setTimeout(function () {
      if (tapPending) playSound('tap');
      tapPending = false;
    }, 0);
  }, true);

  document.addEventListener('DOMContentLoaded', function () {
    var body = document.body;
    var isApp = body.hasAttribute('data-app');
    if (isApp && !embedded) document.documentElement.setAttribute('data-stage', '');
    if (!embedded) toolbar(isApp);
    // The player's iframe leaves the toast to the home page around it, which hears the stamp too. An app page
    // shows only its own stamp; the home page shows any.
    if (!embedded) passport.onChange(function (id) { if (id && (!isApp || id === appId())) stampToast(id); });
    if (!isApp) return;
    if (!embedded) sourceFromHash();

    var desktop = document.createElement('div');
    desktop.id = 'desktop';
    body.insertBefore(desktop, body.firstChild);
    if (!document.documentElement.hasAttribute('data-shot')) centerWindows(desktop);
    if (embedded && window.parent !== window) padInPlayer();
    if (body.hasAttribute('data-console')) {
      outPanel = document.createElement('pre');
      outPanel.className = 'shim-console';
      body.appendChild(outPanel);
      if (!embedded) stageEdge(outPanel, '--out-h');
    }

    var files = (body.getAttribute('data-files') || '').split(/\s+/).filter(Boolean);
    var args = (body.getAttribute('data-args') || '').split(/\s+/).filter(Boolean);
    var extras = !document.documentElement.hasAttribute('data-shot');
    var again = null;
    try {
      again = sessionStorage.getItem(AGAIN_KEY);
      sessionStorage.removeItem(AGAIN_KEY);
    } catch (e) { /* no Run again shortcut */ }
    // The player's iframe starts at once (the visitor's click on the home page started it).
    var title = extras && !embedded && again !== appId() && typeof main === 'function' ? titleScreen(files) : null;
    var loading = !title && files.length ? overlay('Loading...') : null;
    window.__shimExited = function (msg) { if (extras) exitScreen(msg); else overlay(msg, 'Run again'); };

    function start() {
      main(args);
      if (extras && !embedded) playSound('start');   // in the player, the home page plays it when it swaps an app in
      if (extras && !embedded) howToPlay();
      if (extras) stampOnPlay();   // after the hint, so a stamp's toast replaces it rather than sitting under it
    }
    Promise.all(files.map(function (f) {
      return title ? preload(f).then(function () { title.loaded(f); }) : preload(f);
    })).then(function () {
      if (loading) loading.remove();
      if (typeof main !== 'function') {
        overlay('classes.js is missing. Build the site with Maven first (see web/README.md).');
        return;
      }
      if (title) title.ready(start); else start();
    });
  });
})();
