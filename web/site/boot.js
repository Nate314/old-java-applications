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
 *   play next. The list of apps is read from the home page's gallery (../index.html), so it lives in one place.
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
(function () {
  'use strict';

  var SKIN_KEY = 'oldjava.skin';
  var SKINS = [['modern', 'Modern'], ['classic', 'Classic'], ['arcade', 'Arcade']];
  SKINS.push(['own', 'Yours']);   // the visitor's own skin, made in skinmaker.js (see "Your skin" below)
  function isSkin(v) { return SKINS.some(function (o) { return o[0] === v; }); }
  var skin = 'modern';
  try {
    skin = localStorage.getItem(SKIN_KEY) || 'modern';
  } catch (e) { /* storage blocked: use the default */ }
  if (!isSkin(skin)) skin = 'modern';
  document.documentElement.setAttribute('data-skin', skin);

  // ---- Your skin: a fourth skin the visitor makes (skinmaker.js) ----
  // Five choices, kept as a short code in oldjava.ownSkin, e.g. "l-7c3aed-aurora-1-gloss": light or dark, a color,
  // a desk wallpaper, corners (square, soft, round) and window chrome (flat, glossy, glow). This part runs on every
  // page, before first paint: it turns the code into the skin's CSS variables in a <style> of its own, which
  // follows a change made in any other page (the storage event) and is copied into a popped-out window with the
  // other styles. A page opened with ?skin=<code> (a shared skin) puts it on and saves it at once, so the home
  // page's player wears it too; skinmaker.js then offers to keep it or go back.
  var OWN_KEY = 'oldjava.ownSkin', OWN_DEFAULT = 'l-7c3aed-aurora-1-gloss';
  var OWN_CODE = /^([ld])-([0-9a-f]{6})-(plain|fade|grid|dots|stripes|aurora)-([012])-(flat|gloss|glow)$/;
  function ownParse(code) {
    var m = OWN_CODE.exec(String(code || '').toLowerCase());
    return m && { dark: m[1] === 'd', color: '#' + m[2], desk: m[3], corners: +m[4], chrome: m[5] };
  }
  function ownCode(s) {
    return (s.dark ? 'd' : 'l') + '-' + s.color.slice(1) + '-' + s.desk + '-' + s.corners + '-' + s.chrome;
  }
  function rgbOf(hex) { return [1, 3, 5].map(function (i) { return parseInt(hex.substr(i, 2), 16); }); }
  function hexOf(c) {
    return '#' + c.map(function (v) { return ('0' + Math.round(Math.max(0, Math.min(255, v))).toString(16)).slice(-2); }).join('');
  }
  function mix(a, b, t) { var x = rgbOf(a), y = rgbOf(b); return hexOf(x.map(function (v, i) { return v + (y[i] - v) * t; })); }
  function rgba(hex, alpha) { return 'rgba(' + rgbOf(hex).join(', ') + ', ' + alpha + ')'; }
  function luma(hex) {
    var c = rgbOf(hex).map(function (v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
  }
  function contrast(a, b) { var x = luma(a), y = luma(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }
  function hueTurn(hex, deg) {   // the same color with its hue turned, for the wallpaper's second color
    var c = rgbOf(hex), k = Math.cos(deg * Math.PI / 180), s = Math.sin(deg * Math.PI / 180), q = 1 / 3, r = Math.sqrt(q);
    var m = [k + (1 - k) * q, q * (1 - k) - r * s, q * (1 - k) + r * s];   // rotation about the gray axis
    return hexOf([c[0] * m[0] + c[1] * m[1] + c[2] * m[2], c[0] * m[2] + c[1] * m[0] + c[2] * m[1], c[0] * m[1] + c[1] * m[2] + c[2] * m[0]]);
  }
  // The skin's CSS variables. The color becomes the accent, nudged darker or lighter until white text on it and
  // the accent as text on the page both read, so any color the visitor picks gives a readable site.
  function ownCss(s) {
    var white = '#ffffff', page = s.dark ? mix('#0b0d12', s.color, 0.1) : mix(white, s.color, 0.05);
    var a = s.color, i;
    for (i = 0; i < 40 && contrast(a, white) < (s.dark ? 3 : 4.5); i++) a = mix(a, '#000000', 0.06);
    for (i = 0; i < 40 && s.dark && contrast(a, page) < 3 && contrast(mix(a, white, 0.06), white) >= 3; i++) a = mix(a, white, 0.06);
    var a2 = hueTurn(a, 50);
    var frame = s.dark ? mix('#13151c', a, 0.07) : white;
    var text = s.dark ? '#e8eaf0' : '#1c2130';
    var line = s.dark ? mix('#2b2f3b', a, 0.3) : mix('#d5d9e2', a, 0.25);
    var d1 = s.dark ? mix('#07080c', a, 0.3) : mix(white, a, 0.2), d2 = s.dark ? mix('#07080c', a2, 0.24) : mix(white, a2, 0.26);
    var desk = {
      plain: d1,
      fade: 'linear-gradient(135deg, ' + d1 + ', ' + d2 + ')',
      grid: 'linear-gradient(' + rgba(a, 0.14) + ' 1px, transparent 1px) 0 0 / 32px 32px, linear-gradient(90deg, ' +
        rgba(a, 0.14) + ' 1px, transparent 1px) 0 0 / 32px 32px, linear-gradient(160deg, ' + d1 + ', ' + d2 + ')',
      dots: 'radial-gradient(' + rgba(a, s.dark ? 0.4 : 0.28) + ' 1.5px, transparent 2.2px) 0 0 / 20px 20px, ' + d1,
      stripes: 'repeating-linear-gradient(135deg, ' + d1 + ' 0 22px, ' + mix(d1, d2, 0.7) + ' 22px 44px)',
      aurora: 'radial-gradient(ellipse at 15% 0%, ' + rgba(a, s.dark ? 0.55 : 0.35) + ', transparent 60%), radial-gradient(ellipse at 90% 100%, ' +
        rgba(a2, s.dark ? 0.5 : 0.35) + ', transparent 60%), ' + (s.dark ? '#07080c' : '#f5f6fa')
    }[s.desk];
    var v = {
      '--desk': desk, '--page-bg': page, '--page-text': text,
      '--card-bg': s.dark ? mix('#151821', a, 0.08) : white, '--card-border': s.dark ? mix('#232733', a, 0.2) : mix('#e2e5ec', a, 0.2),
      '--control': s.dark ? mix('#1b1e27', a, 0.1) : mix('#f3f4f6', a, 0.06), '--control-text': text,
      '--frame-bg': frame, '--frame-border': frame, '--frame-border-inactive': frame,
      '--title-bg': frame, '--title-bg-inactive': frame, '--title-text': text,
      '--window-radius': [0, 8, 14][s.corners] + 'px', '--button-radius': [0, 6, 999][s.corners] + 'px',
      '--field-radius': [0, 6, 10][s.corners] + 'px',
      '--window-shadow': s.dark ? '0 12px 32px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(255, 255, 255, 0.08)'
        : '0 12px 32px rgba(15, 23, 42, 0.2), 0 0 0 1px rgba(15, 23, 42, 0.08)',
      '--button-bg': s.dark ? mix('#272b37', a, 0.14) : white, '--button-border': line,
      '--button-pressed': s.dark ? mix('#272b37', a, 0.35) : mix(white, a, 0.18),
      '--field-bg': s.dark ? mix('#0b0d12', a, 0.05) : white, '--field-text': text, '--field-border': line,
      '--field-focus': a, '--focus-ring': a, '--desktop-pane': s.dark ? mix('#1b1e27', a, 0.25) : mix(white, a, 0.16),
      '--close-bg': 'transparent', '--close-border': 'transparent', '--accent': a,
      'color-scheme': s.dark ? 'dark' : 'light'
    };
    if (s.chrome === 'gloss') {
      var top = mix(a, white, 0.35);
      v['--title-bg'] = 'linear-gradient(' + top + ', ' + a + ')';
      v['--title-text'] = contrast(a, white) >= 3 ? white : '#111111';
      v['--title-bg-inactive'] = 'linear-gradient(' + mix(frame, a, 0.1) + ', ' + mix(frame, a, 0.2) + ')';
      v['--frame-border'] = a;
      v['--frame-border-inactive'] = mix(frame, a, 0.3);
      v['--button-bg'] = s.dark ? 'linear-gradient(' + mix('#323746', a, 0.2) + ', ' + mix('#1f2330', a, 0.12) + ')'
        : 'linear-gradient(' + white + ', ' + mix(white, a, 0.14) + ')';
    } else if (s.chrome === 'glow') {
      v['--title-bg'] = 'linear-gradient(90deg, ' + mix(frame, a, s.dark ? 0.4 : 0.16) + ', ' + frame + ')';
      v['--title-text'] = s.dark ? mix(a, white, 0.55) : mix(a, '#000000', 0.25);
      v['--frame-border'] = a;
      v['--frame-border-inactive'] = mix(frame, a, 0.35);
      v['--window-shadow'] = '0 0 0 1px ' + rgba(a, 0.6) + ', 0 0 22px ' + rgba(a, 0.4) + ', 0 0 60px ' + rgba(a2, 0.15);
      v['--button-border'] = a;
    }
    var css = ':root[data-skin="own"] {\n' + Object.keys(v).map(function (k) { return '  ' + k + ': ' + v[k] + ';'; }).join('\n') + '\n}\n';
    if (s.dark) css += ':root[data-skin="own"] .shim-source { --src-bg: ' + mix('#0b0d12', a, 0.05) + '; --src-text: #e5e7eb; ' +
      '--src-gutter: #4b5263; --src-line: ' + rgba(a, 0.18) + '; --src-k: #c084fc; --src-s: #86efac; --src-c: #94a3b8; ' +
      '--src-n: #fbbf24; --src-t: #67e8f9; --src-a: #f9a8d4; }\n';
    return css;
  }
  function ownSaved() {
    try { return ownParse(localStorage.getItem(OWN_KEY)) ? localStorage.getItem(OWN_KEY) : OWN_DEFAULT; } catch (e) { return OWN_DEFAULT; }
  }
  var ownStyle = document.createElement('style');
  ownStyle.id = 'oldjava-own-skin';
  function showOwn(code) { ownStyle.textContent = ownCss(ownParse(code) || ownParse(OWN_DEFAULT)); }
  // Puts a skin code on: saved for every page, worn here at once (the storage event tells the others).
  function wearOwn(code) {
    if (!ownParse(code)) return;
    showOwn(code);
    try { localStorage.setItem(OWN_KEY, code); } catch (e) { /* this page only */ }
  }
  showOwn(ownSaved());
  document.head.appendChild(ownStyle);
  window.addEventListener('storage', function (e) { if (e.key === OWN_KEY && ownParse(e.newValue)) showOwn(e.newValue); });
  // Keys pressed in the maker panel are its own: this listener comes before the app's and the title screen's
  // (they are added later), so they never see them. Escape closes the panel.
  ['keydown', 'keyup'].forEach(function (type) {
    document.addEventListener(type, function (e) {
      if (!e.target.closest || !e.target.closest('.shim-own, .shim-own-offer')) return;
      e.stopImmediatePropagation();
      if (type === 'keydown' && e.key === 'Escape' && window.oldjavaSkinMaker) window.oldjavaSkinMaker.close();
    }, true);
  });
  var sharedSkin = (/[?&]skin=([^&#]*)/.exec(location.search) || [])[1];
  var skinOffer = null;   // what a shared skin replaced: { code, skin }, for skinmaker.js's Undo
  if (sharedSkin && ownParse(sharedSkin) && !/[?&]embed(=|&|$)/.test(location.search) &&
      !(skin === 'own' && sharedSkin.toLowerCase() === ownSaved())) {
    skinOffer = { code: ownSaved(), skin: skin, shared: sharedSkin.toLowerCase() };
    wearOwn(skinOffer.shared);
    skin = 'own';
    document.documentElement.setAttribute('data-skin', 'own');
    try { localStorage.setItem(SKIN_KEY, 'own'); } catch (e) { /* this page only */ }
  }

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
  // ?embed&clip is the preview mode web/clips.ps1 records the gallery's moving thumbnails in: the instant replay
  // recorder (startReplay) makes small, silent, fixed-size clips (CLIP_SIZE) that fill a gallery thumbnail.
  var clipMode = embedded && /[?&]clip(=|&|$)/.test(location.search);

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
      // The name's slot doubles as the band's news line (bandNews): How to play and new stamps show there.
      var slot = document.createElement('div');
      slot.className = 'shim-bar-slot';
      var name = document.createElement('strong');
      name.className = 'shim-bar-name';
      name.textContent = document.title;
      slot.appendChild(name);
      bar.appendChild(slot);
      news = bandNews(slot);
      var help = document.createElement('button');
      help.type = 'button';
      help.className = 'shim-help';
      help.title = 'How to play: show the hint in this band';
      help.setAttribute('aria-label', 'How to play');
      help.setAttribute('aria-pressed', 'false');
      help.textContent = '?';
      help.addEventListener('click', function () {
        if (news.kind() === 'hint') news.clear(); else howToPlay();
        help.blur();
      });
      news.onKind(function (kind) { help.setAttribute('aria-pressed', String(kind === 'hint')); });
      bar.appendChild(help);
      bar.appendChild(eraSwitch('shim-era'));
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
      var instant = document.createElement('button');   // shown once the app runs and replay is recording
      instant.type = 'button';
      instant.className = 'shim-replay';
      instant.title = 'Instant replay: the last 10 to 20 seconds of play, as a video to watch, save or share';
      instant.setAttribute('aria-label', 'Instant replay');
      instant.setAttribute('data-sound', 'none');   // the panel plays its own open sound
      instant.innerHTML = '<span class="shim-replay-dot" aria-hidden="true"></span><span> Replay</span>';
      instant.addEventListener('click', function () { replayNow(instant); });
      bar.appendChild(instant);
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
    if (isApp && !document.body.hasAttribute('data-own-layout')) {
      var zoom = document.createElement('button');
      zoom.type = 'button';
      zoom.className = 'shim-zoom';
      zoom.addEventListener('click', function () { setZoomFit(!zoomFit()); zoom.blur(); });
      bar.appendChild(zoom);
    }
    if (isApp && poppable() && window.top === window) {   // only a top-level page may open the window
      var pop = document.createElement('button');
      pop.type = 'button';
      pop.className = 'shim-pop';
      pop.setAttribute('data-sound', 'none');   // popOut and bringBack play their own
      pop.addEventListener('click', function () { popOut(); pop.blur(); });
      bar.appendChild(pop);
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
    showZoom();
    showPop();
    if (controls) padOnPage(controls);
  }

  // ---- band news (app pages): nothing floats over the stage ----
  // The toolbar band's name slot slides the app's name up and a news line in: How to play when the app starts
  // (or on the band's ? button) and a new passport stamp. A line longer than the slot scrolls through like a
  // marquee, twice, then the name comes back. Pointing at the line holds it; x or Escape closes it.
  var news = null;
  function bandNews(slot) {
    var line = document.createElement('div');
    line.className = 'shim-news';
    line.setAttribute('role', 'status');
    slot.appendChild(line);
    var timer = 0, deadline = 0, anim = null, listeners = [];
    function told() { var k = kind(); listeners.forEach(function (fn) { fn(k); }); }
    function kind() { return slot.hasAttribute('data-news') ? line.getAttribute('data-kind') : null; }
    function clear() {
      clearTimeout(timer);
      if (anim) anim.cancel();
      anim = null;
      if (!slot.hasAttribute('data-news')) return;
      slot.removeAttribute('data-news');
      told();
    }
    function wait(ms) {
      clearTimeout(timer);
      deadline = Date.now() + ms;
      timer = setTimeout(clear, ms);
    }
    line.addEventListener('pointerenter', function () {
      if (!kind()) return;
      clearTimeout(timer);
      deadline = Math.max(0, deadline - Date.now());   // now the time that was left
      if (anim) anim.pause();
    });
    line.addEventListener('pointerleave', function () {
      if (!kind()) return;
      if (anim) anim.play();
      wait(Math.max(deadline, 3000));
    });
    ownKeys(line, clear);
    return {
      // kind: 'hint' or 'stamp'; lead: nodes kept at the start; text: nodes that may scroll; actions: links or
      // buttons kept in view after it.
      show: function (k, lead, text, actions, ms) {
        clear();
        line.textContent = '';
        line.setAttribute('data-kind', k);
        lead.forEach(function (n) { line.appendChild(n); });
        var box = document.createElement('span');
        box.className = 'shim-news-text';
        var track = document.createElement('span');
        track.className = 'shim-news-track';
        text.forEach(function (n) { track.appendChild(n); });
        box.appendChild(track);
        line.appendChild(box);
        actions.forEach(function (n) { line.appendChild(n); });
        line.appendChild(closeButton(clear));
        slot.setAttribute('data-news', '');
        told();
        var over = track.scrollWidth - box.clientWidth;
        if (over > 4 && track.animate && !reducedMotion()) {
          var hold = 1500, ride = over / 45 * 1000, one = hold + ride + hold;
          anim = track.animate([
            { transform: 'translateX(0)', offset: 0 },
            { transform: 'translateX(0)', offset: hold / one },
            { transform: 'translateX(' + -over + 'px)', offset: (hold + ride) / one },
            { transform: 'translateX(' + -over + 'px)', offset: 1 }
          ], { duration: one, iterations: 2 });
          ms = Math.max(ms, 2 * one + 1000);
        } else if (over > 4) {
          box.title = track.textContent;
        }
        wait(ms);
      },
      clear: clear,
      kind: kind,
      onKind: function (fn) { listeners.push(fn); }
    };
  }
  function reducedMotion() {
    return !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches);
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
  // which windows are open or their sizes recenters them, so dragging a window or moving it with keys works as
  // usual. A change in the desktop's size alone (the source panel docking, the controller shown, the browser
  // resized) keeps the group's last centering and only moves and scales it, so a dragged window keeps its place
  // beside the others instead of snapping back.
  //
  // Fit (the toolbar's zoom button, on unless the visitor picks Actual size) also zooms small windows up to
  // fill the stage, up to ZOOM_MAX times, leaving a margin around them: most of these apps were made for an
  // 800 x 600 screen and would otherwise sit in a corner of today's. The shim's paint canvases then get more
  // pixels (window.__shimRender, announced with a shimrender event), so drawing stays sharp at the bigger size.
  var ZOOM_KEY = 'oldjava.zoom', ZOOM_MAX = 2.5, ZOOM_ROOM = 0.9;
  var zoomListeners = [];
  function zoomFit() {
    try { return localStorage.getItem(ZOOM_KEY) !== 'actual'; } catch (e) { return true; }
  }
  function setZoomFit(on) {
    try { localStorage.setItem(ZOOM_KEY, on ? 'fit' : 'actual'); } catch (e) { /* this page only */ }
    zoomChanged();
  }
  function zoomChanged() {
    zoomListeners.forEach(function (fn) { fn(zoomFit()); });
    showZoom();
  }
  // The toolbar's button says what Fit is doing: the zoom it picked for these windows, or that it has room only
  // for their actual size.
  function showZoom() {
    var b = document.querySelector('.shim-toolbar .shim-zoom');
    if (!b) return;
    var on = zoomFit(), s = window.__shimScale || 1;
    b.setAttribute('aria-pressed', String(on));
    b.innerHTML = '⤢ <span>' + (on ? 'Fit' : 'Actual size') + '</span>' + (on && s > 1 ? ' <b>' + s + '×</b>' : '');
    b.setAttribute('aria-label', on ? 'Fit to screen' + (s > 1 ? ', zoomed ' + s + ' times' : '') : 'Actual size');
    b.title = on ? 'The app is zoomed to fit the screen' + (s > 1 ? ' (' + s + '×)' : '') + '. Click for its actual size.'
      : 'The app is shown at its actual size. Click to zoom it to fit the screen.';
  }
  window.addEventListener('storage', function (e) { if (e.key === ZOOM_KEY || e.key === null) zoomChanged(); });
  // The home page's player bar has a Fit button too; its iframe hears the change through the storage event.
  window.oldjavaZoom = { fit: zoomFit, set: setZoomFit, onChange: function (fn) { zoomListeners.push(fn); } };

  // The open windows' sizes (a key that changes when one opens, closes or is resized) and the box around them
  // at their actual size ([x0, y0, x1, y1], or null when none shows).
  function windowsBox(desktop) {
    var x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity, k = '';
    for (var c = desktop.firstElementChild; c; c = c.nextElementSibling) {
      if (!c.offsetWidth) continue;
      k += c.offsetWidth + 'x' + c.offsetHeight + ',';
      x0 = Math.min(x0, c.offsetLeft); y0 = Math.min(y0, c.offsetTop);
      x1 = Math.max(x1, c.offsetLeft + c.offsetWidth); y1 = Math.max(y1, c.offsetTop + c.offsetHeight);
    }
    return { key: k, box: x1 < x0 ? null : [x0, y0, x1, y1] };
  }

  function centerWindows(desktop) {
    var key = '', size = '', box = null, fit = zoomFit();
    zoomListeners.push(function (on) { fit = on; size = ''; });
    setInterval(function () {
      var now = windowsBox(desktop), k = now.key;
      var z = desktop.clientWidth + 'x' + desktop.clientHeight + ',' + (window.devicePixelRatio || 1);
      if (k === key && z === size) return;
      if (k !== key) box = now.box;
      key = k;
      size = z;
      if (!box) return;
      var x0 = box[0], y0 = box[1], x1 = box[2], y1 = box[3];
      var w = desktop.clientWidth, h = desktop.clientHeight;
      var s = Math.min(w / (x1 - x0), h / (y1 - y0));
      // Zoomed up in quarter steps, so resizing the browser a little does not redraw every canvas.
      s = s < 1 ? s : fit ? Math.max(1, Math.min(ZOOM_MAX, Math.floor(s * ZOOM_ROOM * 4) / 4)) : 1;
      // About the desktop's center (the default transform origin, which the Arcade power-on's scale shares).
      var dx = s * (w - x0 - x1) / 2;
      var dy = s * (h - y0 - y1) / 2;
      window.__shimScale = s;
      // Whole pixels keep unscaled and zoomed windows crisp; a group scaled down keeps the exact fit instead.
      desktop.style.transform = s < 1 ? 'translate(' + dx + 'px, ' + dy + 'px) scale(' + s + ')'
        : 'translate(' + Math.round(dx) + 'px, ' + Math.round(dy) + 'px)' + (s > 1 ? ' scale(' + s + ')' : '');
      showZoom();
      var dpr = window.devicePixelRatio || 1;
      var render = Math.max(dpr, Math.min(dpr * s, ZOOM_MAX));   // capped: a canvas 9 times bigger paints slowly
      if (render !== (window.__shimRender || dpr)) {
        window.__shimRender = render;
        document.dispatchEvent(new Event('shimrender'));
      }
    }, 250);
  }

  // ---- pop out ----
  // These were desktop apps, each in a window of its own. Pop out gives them one again: the browser's Document
  // Picture-in-Picture (Chrome and Edge) opens a small window that stays on top of every other window on the
  // visitor's screen, and the app's desktop moves into it, windows and all. It is the same running app, not a
  // copy, so a timer, a song or a game goes on where it was, beside whatever else the visitor is doing. The
  // shim follows the desktop (window.__shimDoc, window.__shimListen: see web.rt.Js), and centerWindows fits the
  // windows to the small window as it does to the stage, zooming them up when the visitor makes it bigger.
  // The page keeps a card in the stage with Bring it back; closing the small window brings the app back too.
  var POP_MARGIN = 16, POP_BAR = 56;
  var popped = null;   // { win, desktop, card, watch } while the app is popped out

  // Windows and WindowsE draw their pretend Windows on the page, and CircleSpawner places its own windows on
  // the screen it was given, so they stay in the page.
  function poppable() {
    var body = document.body;
    return 'documentPictureInPicture' in window && !body.hasAttribute('data-shell')
      && !body.hasAttribute('data-own-layout');
  }

  function showPop() {
    var b = document.querySelector('.shim-toolbar .shim-pop');
    if (!b) return;
    b.setAttribute('aria-pressed', String(!!popped));
    b.innerHTML = '⧉ <span>' + (popped ? 'Bring back' : 'Pop out') + '</span>';
    b.setAttribute('aria-label', popped ? 'Bring back' : 'Pop out');
    b.title = popped ? 'Bring the app back into this page'
      : 'Pop out: play this app in its own small window that stays on top of your other windows';
  }

  function popOut() {
    if (popped) { bringBack(); return; }
    if (!document.getElementById('desktop')) return;
    if (document.documentElement.hasAttribute('data-title')) {   // still on the title screen: start it, straight into its window
      var play = document.querySelector('.shim-title-play');
      if (!play || play.disabled) return;
      play.click();
    }
    documentPictureInPicture.requestWindow(popSize()).then(popInto, function (e) { console.warn('Pop out:', e); });
  }

  // Room for the windows at their actual size (Fit zooms them up if the visitor enlarges the window). Chrome
  // counts its own title bar in the height asked for.
  function popSize() {
    var desktop = document.getElementById('desktop');
    var box = desktop && windowsBox(desktop).box;
    var w = box ? box[2] - box[0] + 2 * POP_MARGIN : 640;
    var h = box ? box[3] - box[1] + 2 * POP_MARGIN : 480;
    return { width: Math.round(w), height: Math.round(h + POP_BAR) };
  }

  // Moves the app's desktop into win, a Picture-in-Picture window opened by this page or by the home page
  // around the player's iframe (which may not open one itself).
  function popInto(win) {
    var html = document.documentElement;
    var desktop = document.getElementById('desktop');
    if (popped || !desktop) { win.close(); return; }
    var doc = win.document;
    doc.title = document.title;
    Array.prototype.forEach.call(document.querySelectorAll('link[rel="stylesheet"], style'), function (n) {
      var copy;
      if (n.href) {
        copy = doc.createElement('link');
        copy.rel = 'stylesheet';
        copy.href = n.href;   // absolute: the small window's document has no address of its own
      } else {
        copy = doc.createElement('style');
        copy.textContent = n.textContent;
      }
      doc.head.appendChild(copy);
    });
    // Styled like the home page's player: the desk on the page, the windows centered on it.
    var root = doc.documentElement;
    root.setAttribute('data-embed', '');
    function skin() { root.setAttribute('data-skin', html.getAttribute('data-skin')); }
    skin();
    var watch = new MutationObserver(skin);
    watch.observe(html, { attributes: true, attributeFilter: ['data-skin'] });
    doc.body.appendChild(desktop);
    window.__shimDoc = doc;
    function listen() {
      if (!popped || popped.win !== win) return;
      if (window.__shimListen) window.__shimListen(doc);
      else setTimeout(listen, 100);   // the app has not shown a window yet
    }
    // The shim's focus ring follows the last kind of input (web.rt.Js.focusComponent).
    win.addEventListener('keydown', function () { if (window.__shimKeyed) window.__shimKeyed.on = true; }, true);
    win.addEventListener('pointerdown', function () { if (window.__shimKeyed) window.__shimKeyed.on = false; }, true);
    win.addEventListener('pagehide', bringBack);
    popped = { win: win, desktop: desktop, card: poppedCard(), watch: watch };
    html.setAttribute('data-popped', '');
    listen();
    passport.stamp(appId());
    playSound('open');
    showPop();
  }

  // The home page's player bar pops out the app in its iframe with these.
  window.oldjavaPop = {
    can: poppable,
    size: popSize,
    into: popInto,
    back: bringBack,
    popped: function () { return !!popped; }
  };

  function bringBack() {
    if (!popped) return;
    var p = popped;
    popped = null;
    window.__shimDoc = null;
    p.watch.disconnect();
    document.body.insertBefore(p.desktop, document.body.firstChild);
    p.card.remove();
    document.documentElement.removeAttribute('data-popped');
    if (!p.win.closed) p.win.close();
    playSound('close');
    showPop();
  }

  // What the stage shows while the app is out: where it went, and the way back.
  function poppedCard() {
    var card = document.createElement('div');
    card.className = 'shim-popped';
    var art = document.createElement('div');
    art.className = 'shim-popped-art';
    art.setAttribute('aria-hidden', 'true');
    art.innerHTML = '<span class="screen"></span><span class="float"></span>';
    var h = document.createElement('h2');
    var name = document.querySelector('.shim-bar-name');
    h.textContent = (name ? name.textContent : document.title) + ' is in its own window';
    var p = document.createElement('p');
    p.textContent = 'It stays on top of your other windows, even outside the browser, and keeps running while you'
      + ' do something else. Drag its corner to make it bigger. Close it, or press the button, to play it here again.';
    var back = document.createElement('button');
    back.type = 'button';
    back.textContent = 'Bring it back';
    back.setAttribute('data-sound', 'none');   // bringBack plays its own
    back.addEventListener('click', bringBack);
    var box = document.createElement('div');
    box.className = 'shim-popped-box';
    [art, h, p, back].forEach(function (el) { box.appendChild(el); });
    card.appendChild(box);
    document.body.appendChild(card);
    return card;
  }

  // ---- instant replay ----
  // The best moments of a game are over before anyone thinks to press record. Every running app keeps its last
  // moments instead: its windows are drawn about 30 times a second into a canvas of their own (drawDesk walks
  // their elements: backgrounds, borders, the apps' paint canvases, pictures and text), and two MediaRecorders
  // take turns on that canvas's stream, each restarted every REPLAY_SEG ms, half a segment apart, so one of
  // them always holds between half a segment and a whole one. Replay (toolbar, exit screen, the home page's
  // player bar) stops that one and shows its video (openClip) with the app's own sound, if it made any
  // (appOut), to watch, save or share. The video stays in the page unless the visitor saves or shares it.
  // Only a changed app is drawn again: the shim flags every canvas it draws on (window.__shimPainted) and a
  // MutationObserver sees the rest (text, buttons, windows moving), so an app at rest (a solved cube, a clock
  // between ticks, a menu) costs one frame a second (REPLAY_IDLE), and the encoder has nothing new to encode.
  // Nothing is drawn or recorded while nobody can see the app: its tab is hidden, or the page has scrolled the
  // app out of view (the home page's player, once the visitor scrolls down to the gallery).
  var REPLAY_SEG = 20000;
  var REPLAY_FPS = 30;     // at most; fewer where drawing the windows costs more (REPLAY_SHARE)
  var REPLAY_IDLE = 1000;  // ms between frames while nothing changes, so the clip's last frame is never stale
  var REPLAY_SHARE = 0.1;  // of the page's time, at most, spent drawing them
  var REPLAY_MAX = 1280;   // the clip's longest side, in pixels
  var REPLAY_PAD = 16;     // desk around the windows, in page pixels
  var replay = null;       // the page's recorder (startReplay), once the app runs
  var CLIP_SIZE = [640, 400];   // clip mode (?embed&clip): the gallery thumbnail's 16:10, at twice its usual width
  var CLIP_BPS = 900000;

  // MP4 first: phones and most sites take it, and Chrome records it. Firefox records WebM.
  function replayType() {
    if (!window.MediaRecorder || !MediaRecorder.isTypeSupported || !window.MediaStream) return null;
    if (!HTMLCanvasElement.prototype.captureStream) return null;
    return ['video/mp4', 'video/webm'].filter(function (t) { return MediaRecorder.isTypeSupported(t); })[0] || null;
  }

  // A CSS background as a canvas fill: its color, or a gradient through the colors of its first gradient.
  function cssFill(g, image, color, x, y, w, h) {
    var stops = /gradient/.test(image) && image.match(/rgba?\([^)]*\)|#[0-9a-f]{3,8}\b/gi);
    if (stops && stops.length > 1) {
      var fill = /to (left|right)|\b(90|270)deg/.test(image) ? g.createLinearGradient(x, 0, x + w, 0)
        : /135deg|to bottom right/.test(image) ? g.createLinearGradient(x, y, x + w, y + h)
        : g.createLinearGradient(0, y, 0, y + h);
      stops.forEach(function (c, i) { fill.addColorStop(i / (stops.length - 1), c); });
      return fill;
    }
    if (color && !/^(transparent|rgba\(.*,\s*0\))$/.test(color)) return color;
    return stops ? stops[0] : null;
  }

  // Paints a text node where the page lays it out, a line box at a time.
  function drawText(g, node, cs, k) {
    var range = node.ownerDocument.createRange();
    range.selectNodeContents(node);
    var rects = range.getClientRects();
    if (!rects.length) return;
    var text = /^pre/.test(cs.whiteSpace) ? node.data : node.data.replace(/\s+/g, ' ').trim();
    if (cs.textTransform === 'uppercase') text = text.toUpperCase();
    else if (cs.textTransform === 'lowercase') text = text.toLowerCase();
    g.font = cs.fontStyle + ' ' + cs.fontWeight + ' ' + parseFloat(cs.fontSize) * k + 'px ' + cs.fontFamily;
    g.fillStyle = cs.color;
    g.textBaseline = 'middle';
    var lines = /^pre/.test(cs.whiteSpace) ? text.split('\n') : null;
    var words = text.split(' '), w = 0;
    for (var i = 0; i < rects.length; i++) {
      var r = rects[i], line;
      if (lines) line = lines[i] || '';
      else if (rects.length === 1) line = text;
      else {   // wrapped: as many words as fit each line box
        if (w >= words.length) break;
        line = words[w++];
        while (w < words.length && g.measureText(line + ' ' + words[w]).width <= r.width + 1) line += ' ' + words[w++];
      }
      g.fillText(line, r.left, r.top + r.height / 2);
    }
  }

  // Paints el and everything in it into g, at its place on the page (k: the desktop's CSS scale).
  function drawNode(g, el, cs, k) {
    if (cs.display === 'none' || cs.visibility === 'hidden') return;
    var r = el.getBoundingClientRect();
    if (!r.width || !r.height) return;
    g.save();
    g.globalAlpha *= +cs.opacity;
    var rad = (parseFloat(cs.borderTopLeftRadius) || 0) * k;
    function outline(inset) {
      g.beginPath();
      if (rad && g.roundRect) g.roundRect(r.left + inset, r.top + inset, r.width - 2 * inset, r.height - 2 * inset, Math.max(0, rad - inset));
      else g.rect(r.left + inset, r.top + inset, r.width - 2 * inset, r.height - 2 * inset);
    }
    var fill = cssFill(g, cs.backgroundImage, cs.backgroundColor, r.left, r.top, r.width, r.height);
    if (fill) { g.fillStyle = fill; outline(0); g.fill(); }
    var bw = parseFloat(cs.borderTopWidth) * k;
    if (bw && cs.borderTopColor === cs.borderLeftColor && parseFloat(cs.borderLeftWidth) * k === bw) {
      g.strokeStyle = cs.borderTopColor;
      g.lineWidth = bw;
      outline(bw / 2);
      g.stroke();
    } else {
      [['Top', 0, 0, 1, 0], ['Bottom', 0, 1, 1, 0], ['Left', 0, 0, 0, 1], ['Right', 1, 0, 0, 1]].forEach(function (s) {
        var w = parseFloat(cs['border' + s[0] + 'Width']) * k;
        if (!w) return;
        g.fillStyle = cs['border' + s[0] + 'Color'];
        g.fillRect(r.left + s[1] * (r.width - w), r.top + s[2] * (r.height - w), s[3] ? r.width : w, s[4] ? r.height : w);
      });
    }
    // Window frames are inset box shadows (computed as "color x y blur spread inset").
    var inset = /(rgba?\([^)]*\)|#\w+) -?[\d.]+px -?[\d.]+px [\d.]+px ([\d.]+)px inset/g, m;
    while ((m = inset.exec(cs.boxShadow))) {
      g.strokeStyle = m[1];
      g.lineWidth = m[2] * k;
      outline(m[2] * k / 2);
      g.stroke();
    }
    if (cs.overflow !== 'visible') { outline(0); g.clip(); }
    var tag = el.tagName;
    if (tag === 'CANVAS' && el.width) {
      g.drawImage(el, r.left, r.top, r.width, r.height);
    } else if (tag === 'IMG' && el.complete && el.naturalWidth) {
      g.drawImage(el, r.left, r.top, r.width, r.height);
    } else if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') {
      var value = tag === 'SELECT' ? (el.selectedOptions[0] || {}).text || '' : el.value;
      g.font = cs.fontWeight + ' ' + parseFloat(cs.fontSize) * k + 'px ' + cs.fontFamily;
      g.fillStyle = cs.color;
      g.textBaseline = 'middle';
      g.fillText(value, r.left + (parseFloat(cs.paddingLeft) + parseFloat(cs.borderLeftWidth)) * k, r.top + r.height / 2);
    } else {
      // Children in stacking order: z-index below 0 (the paint canvas, under its component's children), the
      // text, then the rest by z-index (the shim stacks a component's first child on top, as AWT does).
      var view = el.ownerDocument.defaultView, kids = [], texts = [];
      for (var n = el.firstChild; n; n = n.nextSibling) {
        if (n.nodeType === 3 && /\S/.test(n.data)) texts.push(n);
        else if (n.nodeType === 1) {
          var ks = view.getComputedStyle(n);
          kids.push({ el: n, cs: ks, z: parseInt(ks.zIndex, 10) || 0, i: kids.length });
        }
      }
      kids.sort(function (a, b) { return a.z - b.z || a.i - b.i; });
      kids.forEach(function (c) { if (c.z < 0) drawNode(g, c.el, c.cs, k); });
      texts.forEach(function (t) { drawText(g, t, cs, k); });
      kids.forEach(function (c) { if (c.z >= 0) drawNode(g, c.el, c.cs, k); });
    }
    g.restore();
  }

  // Draws the app's windows, on the skin's desk, fitted into canvas. Returns the size they and the desk around
  // them have at actual size (unzoomed), or null while there is no window.
  function drawDesk(g, canvas, desktop) {
    var view = desktop.ownerDocument.defaultView;
    var x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity, wins = [];
    for (var c = desktop.firstElementChild; c; c = c.nextElementSibling) {
      var r = c.getBoundingClientRect();
      if (!r.width || !r.height) continue;
      var cs = view.getComputedStyle(c);
      wins.push({ el: c, cs: cs, z: parseInt(cs.zIndex, 10) || 0, i: wins.length });
      x0 = Math.min(x0, r.left); y0 = Math.min(y0, r.top);
      x1 = Math.max(x1, r.right); y1 = Math.max(y1, r.bottom);
    }
    if (!wins.length) return null;
    var k = window.__shimScale || 1;
    x0 -= REPLAY_PAD * k; y0 -= REPLAY_PAD * k; x1 += REPLAY_PAD * k; y1 += REPLAY_PAD * k;
    var W = canvas.width, H = canvas.height;
    var s = Math.min(W / (x1 - x0), H / (y1 - y0));
    var root = view.getComputedStyle(desktop.ownerDocument.documentElement);
    g.setTransform(1, 0, 0, 1, 0, 0);
    g.globalAlpha = 1;
    g.fillStyle = cssFill(g, root.getPropertyValue('--desk'), root.getPropertyValue('--desk').trim(), 0, 0, W, H) || '#1e293b';
    g.fillRect(0, 0, W, H);
    g.setTransform(s, 0, 0, s, (W - (x1 - x0) * s) / 2 - x0 * s, (H - (y1 - y0) * s) / 2 - y0 * s);
    wins.sort(function (a, b) { return a.z - b.z || a.i - b.i; });
    wins.forEach(function (w) { drawNode(g, w.el, w.cs, k); });
    return [(x1 - x0) / k, (y1 - y0) / k];
  }

  // The clip's size for windows of that size: up to twice as big (small windows make small videos), at most
  // REPLAY_MAX on the longest side, in even pixels (which video encoders want). Clip mode's size is fixed.
  function clipSize(box) {
    if (clipMode) return CLIP_SIZE.slice();
    var s = Math.min(2, REPLAY_MAX / Math.max(box[0], box[1]));
    return [2 * Math.round(box[0] * s / 2), 2 * Math.round(box[1] * s / 2)];
  }

  // Starts keeping the last moments of the app on desktop. Returns { live(), take() } or null where the
  // browser cannot record a canvas.
  function startReplay(desktop) {
    var type = replayType();
    if (!type) return null;
    var canvas = document.createElement('canvas');
    var g = canvas.getContext('2d');
    var track = null, tap = null, recs = [], last = 0, cost = 0, moved = 0, changed = true, inView = true;
    function look() { changed = true; }
    if (window.MutationObserver) {
      new MutationObserver(look).observe(desktop, { subtree: true, childList: true, attributes: true, characterData: true });
      new MutationObserver(look).observe(document.documentElement, { attributes: true, attributeFilter: ['data-skin'] });
    }
    // The body, not the desktop: it stays in this page while the desktop is popped out. In the home page's
    // player it is the whole iframe, so this says whether the visitor has scrolled the player away.
    if (window.IntersectionObserver) {
      new IntersectionObserver(function (seen) {
        inView = seen[seen.length - 1].isIntersecting;
        if (inView) changed = true;
        sync();
      }).observe(document.body);
    }

    function begin() {
      if (!clipMode && audio && !tap && audio.createMediaStreamDestination) {   // the app's voices, not the site's sounds
        tap = audio.createMediaStreamDestination();
        appOut(audio).connect(tap);
      }
      var stream = new MediaStream([track].concat(tap ? tap.stream.getAudioTracks() : []));
      var r = { chunks: [], t0: Date.now() };
      try {
        r.rec = new MediaRecorder(stream, { mimeType: type, videoBitsPerSecond: clipMode ? CLIP_BPS : 5000000 });
        r.rec.ondataavailable = function (e) { if (e.data.size) r.chunks.push(e.data); };
        r.rec.start(1000);
      } catch (e) {
        console.warn('Instant replay:', e);
        return;
      }
      if (away()) {
        r.rec.pause();
        r.pausedAt = Date.now();
      }
      recs.push(r);
    }
    function away() { return !popped && (document.hidden || !inView); }   // a popped out app is still in sight
    // A paused recording leaves that time out of its video, so its start moves up by as much (the clip's seconds).
    function sync() {
      recs.forEach(function (r) {
        if (away() && r.rec.state === 'recording') {
          r.rec.pause();
          r.pausedAt = Date.now();
        } else if (!away() && r.rec.state === 'paused') {
          r.rec.resume();
          r.t0 += Date.now() - r.pausedAt;
        }
      });
    }
    function rotate() {
      if (away()) return;
      while (recs.length > 1) recs.shift().rec.stop();
      begin();
    }
    function frame() {
      (popped && !popped.win.closed ? popped.win : window).requestAnimationFrame(frame);
      var t = performance.now();   // not the frame's time, which a popped out window counts from its own start
      if (away()) return;
      if (window.__shimPainted) {
        window.__shimPainted = false;
        changed = true;
      }
      // An app of many widgets (Minesweeper's Expert board has 256 tiles) takes several ms to draw, so it is drawn
      // less often: the app keeps at least nine tenths of the time.
      if (t - last < (changed ? Math.max(1000 / REPLAY_FPS, cost / REPLAY_SHARE) : REPLAY_IDLE) - 4) return;
      last = t;
      changed = false;
      var t0 = performance.now(), box = drawDesk(g, canvas, desktop);
      cost = 0.8 * cost + 0.2 * (performance.now() - t0);
      if (!box) return;
      // A recording keeps the size it started with (Chrome's MP4 does not follow a resize), so windows much
      // bigger or smaller than it (a splash before the app's window, Minesweeper's Expert board) are fitted in at
      // first, and after a second at their new size the recording starts over at it.
      var want = clipSize(box);
      var off = Math.max(Math.abs(want[0] / canvas.width - 1), Math.abs(want[1] / canvas.height - 1));
      if (track && off < 0.1) { moved = 0; return; }
      if (track && !moved) moved = t;
      if (track && t - moved < 1000) return;
      moved = 0;
      recs.forEach(function (r) { r.rec.stop(); });
      recs = [];
      if (track) track.stop();
      canvas.width = want[0];
      canvas.height = want[1];
      drawDesk(g, canvas, desktop);
      var first = !track;
      track = canvas.captureStream(REPLAY_FPS).getVideoTracks()[0];
      begin();
      if (!first) return;
      setInterval(rotate, REPLAY_SEG / 2);
      document.documentElement.setAttribute('data-replay', '');
      if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaReplay: true }, location.origin);
    }
    requestAnimationFrame(frame);
    document.addEventListener('visibilitychange', sync);

    return {
      live: function () { return recs.length > 0; },
      // Stops the longest recording and starts a new one in its place; resolves to the clip, or null.
      take: function () {
        var r = recs.shift();
        if (!r) return Promise.resolve(null);
        var end = r.rec.state === 'paused' ? r.pausedAt : Date.now();
        return new Promise(function (done) {
          r.rec.onstop = function () {
            done({
              blob: new Blob(r.chunks, { type: (r.rec.mimeType || type).split(';')[0] }),
              seconds: Math.max(1, Math.floor((end - r.t0) / 1000)),
              app: appId()
            });
          };
          r.rec.stop();
          begin();
        });
      }
    };
  }

  function replayNow(opener) {
    if (replay) replay.take().then(function (clip) { if (clip) openClip(clip, opener); });
  }

  // The replay panel: the clip playing on a loop, Save video and Share... (the system share sheet, with the
  // video and the app's link, where the browser can share files).
  var clipBox = null;
  function openClip(clip, opener) {
    if (embedded && window.parent !== window) {   // the home page shows it over its player, at full size
      window.parent.postMessage({ oldjavaClip: clip }, location.origin);
      return;
    }
    if (clipBox) return;
    if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(function () {});
    var url = URL.createObjectURL(clip.blob);
    var scrim = document.createElement('div');
    scrim.className = 'shim-scrim';
    var panel = document.createElement('div');
    panel.className = 'shim-share shim-clip';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-modal', 'true');
    panel.setAttribute('aria-label', 'Instant replay');
    var head = document.createElement('div');
    head.className = 'shim-drawer-head';
    var title = document.createElement('strong');
    title.textContent = 'Instant replay';
    function shut() {
      playSound('close');
      video.pause();
      URL.revokeObjectURL(url);
      scrim.remove();
      panel.remove();
      clipBox = null;
      if (opener && opener.isConnected) opener.focus();
    }
    head.appendChild(title);
    head.appendChild(closeButton(shut));
    panel.appendChild(head);
    var body = document.createElement('div');
    body.className = 'shim-share-body';
    var video = document.createElement('video');
    video.className = 'shim-clip-video';
    video.src = url;
    video.controls = true;
    video.loop = true;
    video.playsInline = true;
    var note = document.createElement('p');
    note.className = 'shim-clip-note';
    var ways = document.createElement('div');
    ways.className = 'shim-share-ways';
    var stamp = new Date().toISOString().slice(0, 16).replace(/[-:]/g, '').replace('T', '-');
    var name = clip.app + '-replay-' + stamp + (/mp4/.test(clip.blob.type) ? '.mp4' : '.webm');
    var save = document.createElement('a');
    save.className = 'shim-share-way primary';
    save.href = url;
    save.download = name;
    save.textContent = 'Save video';
    save.setAttribute('data-sound', 'none');
    save.addEventListener('click', function () { playSound('saved'); });
    ways.appendChild(save);
    var file = window.File ? new File([clip.blob], name, { type: clip.blob.type }) : null;
    var link = siteRoot + clip.app + '/index.html';
    if (file && navigator.canShare && navigator.canShare({ files: [file] })) {
      var native = document.createElement('button');
      native.type = 'button';
      native.className = 'shim-share-way';
      native.textContent = 'Share…';
      native.addEventListener('click', function () {
        navigator.share({ files: [file], title: title.textContent, text: note.dataset.share + ' ' + link })
          .catch(function () { /* closed */ });
      });
      ways.appendChild(native);
    }
    [video, note, ways].forEach(function (el) { body.appendChild(el); });
    panel.appendChild(body);
    scrim.addEventListener('click', shut);
    ownKeys(panel, shut);
    document.body.appendChild(scrim);
    document.body.appendChild(panel);
    clipBox = panel;
    playSound('open');
    // Started by the visitor's click, so it may play with sound; muted if the browser says otherwise.
    video.play().catch(function () { video.muted = true; video.play().catch(function () {}); });
    save.focus();
    function describe(app) {
      title.textContent = 'Instant replay: ' + app;
      panel.setAttribute('aria-label', title.textContent);
      note.textContent = 'The last ' + clip.seconds + ' seconds of ' + app + '. The video stays on this device'
        + ' until you save or share it.';
      note.dataset.share = app + ', played in my browser:';
    }
    describe(clip.app);
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, clip.app)];
      if (app && clipBox === panel) describe(app.name);
    });
  }
  window.oldjavaReplay = {
    open: openClip,
    live: function () { return !!replay && replay.live(); },
    take: function () { return replay ? replay.take() : Promise.resolve(null); }
  };

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
      // When it was last played, for What's new (news.js): changes since then are news to this visitor.
      if (!isThen()) try {
        var last = JSON.parse(localStorage.getItem('oldjava.lastPlayed') || '{}') || {};
        last[id] = Date.now();
        localStorage.setItem('oldjava.lastPlayed', JSON.stringify(last));
      } catch (e) { /* not kept */ }
      played = readPlayed();   // another page may have stamped meanwhile
      if (played[id]) return;
      played[id] = Date.now();
      try { localStorage.setItem(PLAYED_KEY, JSON.stringify(played)); } catch (e) { /* kept for this page only */ }
      passportChanged(id);
    },
    // Stamps written to localStorage by this page (saves brought from another device): read them again.
    reload: function () { played = readPlayed(); passportChanged(null); },
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
      var next = passport.unplayed(apps, id)[0];
      var done = count >= apps.length;
      playSound(done ? 'complete' : 'stamp');
      if (done) confetti();
      if (news) { stampNews(app, count, apps.length, next); return; }
      var meter = document.createElement('span');
      meter.className = 'shim-meter';
      meter.style.setProperty('--p', String(count / apps.length));
      floatToast(app.shot, done ? 'Arcade complete!' : 'Stamp collected: ' + app.name,
        done ? 'You have played all ' + apps.length + ' apps.' : count + ' of ' + apps.length + ' apps played.',
        [meter], function (toast) {
          var actions = [];
          if (done) {
            var brag = document.createElement('button');
            brag.type = 'button';
            brag.className = 'shim-toast-go';
            brag.setAttribute('data-sound', 'none');
            brag.textContent = 'Share it ›';
            brag.addEventListener('click', function () { toast.remove(); openShare(null); });
            actions.push(brag);
          }
          if (next) actions.push(toastLink(toast, next, 'Try ' + next.name + ' ›'));
          return actions;
        });
    });
  }

  // A toast floating at the top of the home page (an app page uses its band's news line instead): an app's
  // thumbnail as a seal, a title, a line, more nodes under them, and the buttons or links actions(toast) makes.
  // It replaces the toast before it and fades away after 9 seconds.
  function floatToast(shot, titleText, lineText, more, actions) {
    var old = document.querySelector('.shim-toast');
    if (old) old.remove();
    var toast = document.createElement('div');
    toast.className = 'shim-toast';
    toast.setAttribute('role', 'status');
    var seal = document.createElement('span');
    seal.className = 'shim-toast-seal';
    var img = document.createElement('img');
    img.src = shot;
    img.alt = '';
    seal.appendChild(img);
    var text = document.createElement('div');
    text.className = 'shim-toast-text';
    var title = document.createElement('strong');
    var line = document.createElement('span');
    title.textContent = titleText;
    line.textContent = lineText;
    [title, line].concat(more).forEach(function (n) { text.appendChild(n); });
    toast.appendChild(seal);
    toast.appendChild(text);
    actions(toast).forEach(function (n) { toast.appendChild(n); });
    toast.appendChild(closeButton(function () { toast.remove(); }));
    ownKeys(toast);
    document.body.appendChild(toast);
    setTimeout(function () {
      toast.classList.add('gone');
      setTimeout(function () { toast.remove(); }, 600);
    }, 9000);
    return toast;
  }

  // A toast's link to an app: on the home page a plain click swaps the app into the player instead.
  function toastLink(toast, app, label) {
    var go = document.createElement('a');
    go.className = 'shim-toast-go';
    go.href = pageOf(app);
    go.textContent = label;
    go.addEventListener('click', function (e) {
      if (!passport.open || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      toast.remove();
      passport.open(app.id);
    });
    return go;
  }

  // The same news on an app page, in the band: the thumbnail pressed in as a seal, the count, and where to go.
  function stampNews(app, count, total, next) {
    var done = count >= total;
    var seal = document.createElement('span');
    seal.className = 'shim-news-seal';
    var img = document.createElement('img');
    img.src = app.shot;
    img.alt = '';
    seal.appendChild(img);
    var title = document.createElement('strong');
    title.textContent = done ? 'Arcade complete! ' : 'Stamp collected: ' + app.name + ' ';
    var line = document.createElement('span');
    line.textContent = done ? 'You have played all ' + total + ' apps.' : count + ' of ' + total + ' apps played.';
    var meter = document.createElement('span');
    meter.className = 'shim-meter';
    meter.style.setProperty('--p', String(count / total));
    var actions = [];
    if (done) {
      var brag = document.createElement('button');
      brag.type = 'button';
      brag.className = 'shim-news-go';
      brag.setAttribute('data-sound', 'none');
      brag.textContent = 'Share it \u203a';
      brag.addEventListener('click', function () { news.clear(); openShare(null, brag); });
      actions.push(brag);
    }
    if (next) {
      var go = document.createElement('a');
      go.className = 'shim-news-go';
      go.href = pageOf(next);
      go.textContent = 'Try ' + next.name + ' \u203a';
      actions.push(go);
    }
    news.show('stamp', [seal], [title, line, meter], actions, 9000);
  }

  function confetti() {
    if (reducedMotion()) return;
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
  var CHROME = '.shim-toolbar, .shim-drawer, .shim-share, .shim-scrim, .shim-overlay, .shim-toast, .shim-pad, .shim-title, .shim-source';
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

  // ---- watch: the computer plays (?watch) ----
  // Four games can play themselves: Minesweeper's solver and Ultimate Tic-Tac-Toe's computer on both sides (W),
  // TankGame's computer driving both tanks (C) and DinoGame's original bot (B); the keys are the apps' own. A page
  // opened with ?watch skips the title screen, presses that key once the app's window is up, keeps the app quiet and
  // says so in a line over the stage. The visitor's first click or key (or press on the controller) takes over:
  // the key is pressed again, so the computer stops, before the visitor's own press reaches the app, which makes
  // that press their first move. The passport stamp waits for it too.
  var WATCH = { Minesweeper: 'W', UltimateTicTacToe: 'W', TankGame: 'C', DinoGame: 'B' };
  var NOT_TAKING_OVER = '.shim-toolbar, .shim-drawer, .shim-share, .shim-scrim, .shim-overlay, .shim-toast, .shim-source';
  window.oldjavaWatch = { has: function (id) { return !!WATCH[id]; } };

  function watchWanted() {
    return /[?&]watch(=|&|$)/.test(location.search) && !!WATCH[appId()] && !isThen();
  }

  function watchApp(desktop) {
    var key = WATCH[appId()];
    var note = document.createElement('div');
    note.className = 'shim-watch';
    note.setAttribute('role', 'status');
    var dot = document.createElement('span');
    dot.className = 'shim-watch-dot';
    var strong = document.createElement('strong');
    strong.textContent = 'The computer is playing';
    var how = document.createElement('span');
    how.textContent = 'Click or press any key to take over';
    [dot, strong, how].forEach(function (el) { note.appendChild(el); });
    document.body.appendChild(note);
    setAppMuted(true);
    var pressed = false;
    function press() { fireKey('keydown', key); fireKey('keyup', key); }
    // Once the app's first window is on the desktop, and has had a moment to take the focus.
    var wait = setInterval(function () {
      if (!windowsBox(desktop).box) return;
      clearInterval(wait);
      wait = setTimeout(function () { press(); pressed = true; }, 400);
    }, 100);

    function take(ownKey) {
      document.removeEventListener('pointerdown', onInput, true);
      document.removeEventListener('keydown', onInput, true);
      window.removeEventListener('message', onMessage);
      clearInterval(wait);
      clearTimeout(wait);
      if (pressed && !ownKey) press();
      setAppMuted(false);
      note.classList.add('gone');
      setTimeout(function () { note.remove(); }, 400);
      // A reload now starts the app for the visitor.
      var search = location.search.replace(/([?&])watch(=[^&]*)?(&|$)/, function (m, lead, value, tail) { return tail ? lead : ''; });
      history.replaceState(history.state, '', location.pathname + search + location.hash);
      passport.stamp(appId());
      if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaWatch: 'taken' }, location.origin);
    }
    function onInput(e) {
      if (!e.isTrusted || e.target && e.target.closest && e.target.closest(NOT_TAKING_OVER)) return;
      if (e.type === 'keydown' && /^(Shift|Control|Alt|Meta|Tab|Escape)$/.test(e.key)) return;
      take(e.type === 'keydown' && e.code === keyOf(key).code);
    }
    // The home page's player asks first, before it passes its controller's presses in.
    function onMessage(e) {
      if (e.origin === location.origin && e.source === window.parent && e.data && e.data.oldjavaWatchTake) take(false);
    }
    document.addEventListener('pointerdown', onInput, true);
    document.addEventListener('keydown', onInput, true);
    window.addEventListener('message', onMessage);
  }

  // ---- the site's screen saver ----
  // Screen Saver is one of the apps, and it is also the site's own screen saver: when a page (the home page, an
  // app page) has had no input for saver.after (3 minutes), Screen Saver fades in over everything, in an iframe of
  // its page in saver mode (?embed&saver: no title screen, stamp or replay, as in thumbnail mode). It shares the
  // app's saves, so it shows the saver the visitor left the app on, cycling or not, at their speed. The first key
  // press, click, touch, real mouse move or controller press fades it out again; it never reaches the app beneath.
  // Then the page says what that was, with a link to pick a saver and a way to turn it off (localStorage
  // oldjava.saver = 'off'); Screen Saver's title screen turns it back on. It waits while a page is hidden or in
  // fullscreen, while the computer plays (watch), while the app is popped out or a video plays, and on the apps
  // that are read or watched without touching them (SAVER_SKIP).
  var SAVER_KEY = 'oldjava.saver';
  var SAVER_SKIP = { ScreenSaver: true, BibleDownloader: true, Clock: true };
  if (embedded && /[?&]saver(=|&|$)/.test(location.search)) document.documentElement.setAttribute('data-saver', '');
  var saver = null;   // a top page's saver (window.oldjavaSaver); embedded pages hand their input to the parent's
  function saverOn() {
    try { return localStorage.getItem(SAVER_KEY) !== 'off'; } catch (e) { return true; }
  }
  function setSaverOn(on) {
    try { localStorage.setItem(SAVER_KEY, on ? 'on' : 'off'); } catch (e) { /* this visit only */ }
  }
  function saverHost() {
    if (!embedded) return saver;
    try { return window.parent !== window && window.parent.oldjavaSaver || null; } catch (e) { return null; }
  }
  function saverMinutes() {
    var host = saverHost();
    return Math.round((host ? host.after : 180000) / 60000);
  }

  // Every page, the player's and the saver's frames included: input counts as activity, and input that woke the
  // saver (or fell on it) is kept from the app, its key's release too.
  var saverHeld = {};
  ['keydown', 'keyup', 'pointerdown', 'pointerup', 'pointercancel', 'pointermove', 'wheel'].forEach(function (type) {
    window.addEventListener(type, function (e) {
      if (!e.isTrusted) return;
      var host = saverHost();
      var mine = !!host && host.input(e);
      if (e.type === 'keyup' && saverHeld[e.code]) { delete saverHeld[e.code]; mine = true; }
      if (!mine) return;
      if (e.type === 'keydown') saverHeld[e.code] = true;
      if (e.type === 'keydown' || e.type === 'pointerdown') e.preventDefault();
      e.stopImmediatePropagation();
    }, true);
  });

  if (!embedded) {
    var saverBox = null, saverPrev = null, saverDown = false, saverFading = false, saverBase = null, saverPads = '';
    var saverLast = Date.now();
    saver = window.oldjavaSaver = {
      after: 3 * 60 * 1000,
      // An input event from this page or a frame in it; true when it belongs to the saver.
      input: function (e) {
        var t = e.type;
        if (t === 'pointerdown') saverDown = true;
        if (t === 'pointerup' || t === 'pointercancel') saverDown = false;
        if (!saverBox) { saverLast = Date.now(); return false; }
        if (saverFading) { if (!saverDown) saverDone(); return true; }
        if (t === 'pointermove') {
          // Showing the saver can send a move from a mouse that has not moved; a real one goes somewhere.
          if (!saverBase) { saverBase = [e.screenX, e.screenY]; return true; }
          if (Math.abs(e.screenX - saverBase[0]) + Math.abs(e.screenY - saverBase[1]) < 12) return true;
        }
        if (t !== 'pointerup' && t !== 'pointercancel') wakeSaver();
        return true;
      },
      show: showSaver,
      wake: function () { if (saverBox && !saverFading) wakeSaver(); },
      showing: function () { return !!saverBox; }
    };
    document.addEventListener('visibilitychange', function () { saverLast = Date.now(); });
    setInterval(function () {
      // A controller counts as input too: the Gamepad API stamps each pad with the time its state last changed.
      var pads = navigator.getGamepads ? [].slice.call(navigator.getGamepads()).map(function (p) { return p ? p.timestamp : 0; }).join() : '';
      if (pads !== saverPads) {
        saverPads = pads;
        saverLast = Date.now();
        if (saverBox && !saverFading) wakeSaver();
      }
      if (saverBox) return;
      if (!saverWanted()) { saverLast = Date.now(); return; }   // its minutes start when the reason ends
      if (Date.now() - saverLast >= saver.after) showSaver();
    }, 1000);
  }

  // The document of the app the visitor sees: this app page's own, or the home page player's.
  function shownAppDoc() {
    if (document.body.hasAttribute('data-app')) return document;
    var frame = document.getElementById('player-frame');
    try { return frame && frame.contentDocument; } catch (e) { return null; }
  }

  function saverWanted() {
    if (!saverOn() || !document.body || document.visibilityState !== 'visible' || document.fullscreenElement) return false;
    var doc = shownAppDoc();
    if (!doc || !doc.body) return true;
    if (SAVER_SKIP[doc.body.getAttribute('data-app')] || doc.documentElement.hasAttribute('data-popped')) return false;
    if (doc.querySelector('.shim-watch:not(.gone)')) return false;
    return ![document, doc].some(function (d) {
      return [].some.call(d.querySelectorAll('video'), function (v) { return !v.paused && !v.ended; });
    });
  }

  function showSaver() {
    if (saverBox || !document.body) return;
    saverPrev = document.activeElement;
    saverBase = null;
    saverFading = false;
    saverBox = document.createElement('div');
    saverBox.className = 'shim-saver';
    var frame = document.createElement('iframe');
    frame.src = siteRoot + 'ScreenSaver/index.html?embed&saver';
    frame.title = 'Screen Saver: press a key or move the mouse to come back';
    frame.tabIndex = -1;
    saverBox.appendChild(frame);
    document.body.appendChild(saverBox);
    requestAnimationFrame(function () { requestAnimationFrame(function () { if (saverBox) saverBox.classList.add('on'); }); });
  }

  function wakeSaver() {
    saverFading = true;
    saverBox.classList.remove('on');
    var box = saverBox;
    setTimeout(function () {
      if (saverBox !== box) return;
      box.setAttribute('data-faded', '');
      saverDone();
    }, 400);
  }

  // Gone once it has faded and no button is held (the release would otherwise land on the app beneath).
  function saverDone() {
    if (!saverBox || !saverBox.hasAttribute('data-faded') || saverDown) return;
    saverBox.remove();
    saverBox = null;
    saverFading = false;
    saverLast = Date.now();
    if (saverPrev && saverPrev.isConnected && saverPrev !== document.body && saverPrev.focus) saverPrev.focus({ preventScroll: true });
    welcomeBack();
  }

  // What that was, where to pick another saver, and how to turn it off: in the band on an app page, a toast on the
  // home page.
  function welcomeBack() {
    function strong(t) { var n = document.createElement('strong'); n.textContent = t; return n; }
    function span(t) { var n = document.createElement('span'); n.textContent = t; return n; }
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, 'ScreenSaver')];
      if (!app) return;
      var title = 'Welcome back! ';
      var line = 'That was ' + app.name + ', one of the ' + apps.length + ' apps here, minding the screen while you were away.';
      function off() {
        setSaverOn(false);
        var said = 'Screen saver off. ', how = app.name + '’s title screen turns it back on.';
        if (news) news.show('saver', [], [strong(said), span(how)], [], 6000);
        else floatToast(app.shot, said, how, [], function () { return []; });
      }
      function offButton(cls, close) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = cls;
        b.textContent = 'Turn it off';
        b.addEventListener('click', function () { close(); off(); });
        return b;
      }
      if (news) {
        var seal = document.createElement('span');
        seal.className = 'shim-news-seal';
        var img = document.createElement('img');
        img.src = app.shot;
        img.alt = '';
        seal.appendChild(img);
        var go = document.createElement('a');
        go.className = 'shim-news-go';
        go.href = pageOf(app);
        go.textContent = 'Pick your saver ›';
        news.show('saver', [seal], [strong(title), span(line)], [go, offButton('shim-news-go', news.clear)], 10000);
        return;
      }
      floatToast(app.shot, title, line, [], function (toast) {
        return [toastLink(toast, app, 'Pick your saver ›'), offButton('shim-toast-go', function () { toast.remove(); })];
      });
    });
  }

  // ---- touch controller ----
  // The keyboard games get an on-screen controller, so they can be played on a phone or tablet. Its buttons
  // press and release the same keys the app listens for (taken from the apps' sources), and they light up
  // when the real key is pressed, so the controller doubles as a key chart. The home page docks it under
  // its player (window.oldjavaPad.render); an app page floats it at the bottom.
  var PADS = {
    FlappyBird: [
      { buttons: [{ k: 'Space', label: 'Flap', big: true }] },
      { buttons: [{ k: 'Z', label: 'Rewind' }, { k: 'C', label: 'Course' }, { k: 'P', label: 'Pause' }, { k: 'R', label: 'Reset' }] }
    ],
    DinoGame: [
      { buttons: [{ k: 'Space', label: 'Jump', big: true }, { k: 'ArrowDown', label: 'Duck' }] },
      { buttons: [{ k: 'B', label: 'Bot' }, { k: 'X', label: 'Reset eye' }, { k: 'E', label: 'Evolve' }, { k: 'F', label: 'Speed' }] }
    ],
    TankGame: [
      { title: 'Red tank', tint: 'red', dpad: ['W', 'A', 'S', 'D'], fire: 'Q' },
      { title: 'Green tank', tint: 'green', dpad: ['ArrowUp', 'ArrowLeft', 'ArrowDown', 'ArrowRight'], fire: 'M' }
    ],
    SynthesizerKeyboard: [
      { piano: ['Z', 'S', 'X', 'D', 'C', 'V', 'G', 'B', 'H', 'N', 'J', 'M', ','] },
      { buttons: [{ k: 'T', label: 'Song' }, { k: 'L', label: 'Listen' }, { k: 'R', label: 'Restart' },
                  { k: 'Space', label: 'Record' }, { k: 'Enter', label: 'Copy link' }, { k: 'I', label: 'Mic' },
                  { k: 'O', label: 'Looper' }, { k: 'P', label: 'Play loop' }] },
      { buttons: [{ k: 'ArrowLeft', label: 'Octave −' }, { k: 'ArrowRight', label: 'Octave +' },
                  { k: 'ArrowDown', label: 'Sound −' }, { k: 'ArrowUp', label: 'Sound +' }] }
    ],
    SpriteFloatyStars: [
      { title: 'Star Catcher', tint: 'gold', dpad: ['ArrowUp', 'ArrowLeft', 'ArrowDown', 'ArrowRight'], fire: 'Space', fireLabel: 'Catch' },
      { buttons: [{ k: 'N', label: 'Next' }, { k: 'R', label: 'Again' }, { k: 'M', label: 'Nights' },
                  { k: 'ArrowUp', label: 'Add star' }, { k: 'ArrowDown', label: 'Remove star' }, { k: 'H', label: 'Help' }] }
    ]
  };
  var ARROWS = { ArrowUp: [38, '↑'], ArrowDown: [40, '↓'], ArrowLeft: [37, '←'], ArrowRight: [39, '→'] };

  // A key name from PADS as the fields of a DOM key event, plus the label on its keycap.
  function keyOf(name) {
    if (name === 'Space') return { key: ' ', code: 'Space', which: 32, cap: 'Space' };
    if (name === 'Enter') return { key: 'Enter', code: 'Enter', which: 13, cap: 'Enter' };
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
    padLight = light;
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

  // ---- game controllers ----
  // A game controller (the Gamepad API's standard layout) plays every app. Each app's map below gives its
  // buttons the keys the app listens for, with the label the chart shows (taken from PADS and the apps'
  // READMEs). What a map leaves free works a pointer drawn on the page: the left stick (unless the map gives
  // it the D-pad's keys) and the D-pad move it, A clicks (hold to drag) and B right-clicks. The right stick,
  // RT and LT point, click and right-click in every app, and LB / RB go to the previous / next app. Back
  // shows the chart again. A second controller gets the map's second entry (TankGame's green tank).
  var GAMEPADS = {
    FlappyBird: [{ a: ['Space', 'Flap'], up: ['Space', 'Flap'], b: ['Z', 'Rewind a crash'], x: ['C', 'Course'], y: ['R', 'Reset'], start: ['P', 'Pause'] }],
    DinoGame: [{ a: ['Space', 'Jump'], up: ['Space', 'Jump'], down: ['ArrowDown', 'Duck'], b: ['ArrowDown', 'Duck'],
                 x: ['B', 'Bot'], y: ['X', 'Reset eye'], start: ['E', 'Evolve bots'] }],
    TankGame: [
      { stick: true, up: ['W', 'Forward'], down: ['S', 'Back'], left: ['A', 'Turn left'], right: ['D', 'Turn right'],
        a: ['Q', 'Fire (on the title card: vs the computer)'], x: ['X', 'Supply crates on/off'] },
      { stick: true, up: ['ArrowUp', 'Forward'], down: ['ArrowDown', 'Back'], left: ['ArrowLeft', 'Turn left'],
        right: ['ArrowRight', 'Turn right'], a: ['M', 'Fire (on the title card: two players)'] }
    ],
    // The D-pad and the face buttons play the white keys, C to C, going round each cross.
    SynthesizerKeyboard: [{ left: ['Z', 'C'], down: ['X', 'D'], right: ['C', 'E'], up: ['V', 'F'],
                            x: ['B', 'G'], a: ['N', 'A'], b: ['M', 'B'], y: [',', 'High C'],
                            start: ['Space', 'Record'] }],
    SpriteFloatyStars: [{ stick: true, up: ['ArrowUp', 'Up'], down: ['ArrowDown', 'Down'], left: ['ArrowLeft', 'Left'],
                          right: ['ArrowRight', 'Right'], a: ['Space', 'Catch (stars: burst)'], b: ['M', 'Nights'],
                          x: ['N', 'Next (stars: next mode)'],
                          y: ['R', 'Again'] }],
    ScreenSaver: [{ stick: true, a: ['N', 'Next saver'], b: ['P', 'Previous'], x: ['C', 'Cycle'], y: ['H', 'Keys'],
                    left: ['ArrowLeft', 'Slower'], right: ['ArrowRight', 'Faster'], up: ['ArrowUp', 'Smaller tiles'],
                    down: ['ArrowDown', 'Bigger tiles'], start: ['F', 'Feed the fish'] }],
    RubiksCube2D: [{ up: ['W', 'Turn'], left: ['A', 'Turn'], down: ['S', 'Turn'], right: ['D', 'Turn'], b: ['Z', 'Undo'],
                     x: ['T', 'Teach me'], y: ['R', 'Retry'], start: ['N', 'Next level'] }],
    MusicTheoryExpert: [{ b: ['Space', 'Play / next'], up: ['ArrowUp', 'Previous drill'], down: ['ArrowDown', 'Next drill'],
                          x: ['R', 'Restart'], y: ['N', 'Next drill'] }],
    NumberGuess: [{ up: ['ArrowUp', 'Higher / yes'], down: ['ArrowDown', 'Lower / no'], b: ['Enter', 'Enter'],
                    x: ['S', 'How it works'] }],
    Clock: [{ b: ['Space', 'Start / pause'], x: ['R', 'Reset'], y: ['L', 'Lap / lengths'], start: ['N', 'Skip (Focus)'], up: ['ArrowUp', 'Minute +'],
              down: ['ArrowDown', 'Minute −'] }],
    StockChecker: [{ up: ['ArrowUp', 'Pick'], down: ['ArrowDown', 'Pick'], x: ['B', 'Buy 1'], y: ['S', 'Sell 1'],
                     b: ['Space', 'Pause'], start: ['R', 'Race'] }],
    GroupCreator: [{ x: ['N', 'New round'], y: ['R', 'Reshuffle'], b: ['Space', 'New round / spin'],
                     start: ['P', 'Pick someone / groups'] }],
    SimpleAlgorithms: [{ b: ['Space', 'Play / pause'], right: ['ArrowRight', 'One move'], x: ['R', 'Reset'], y: ['F', 'Speed'] }]
  };
  // Standard layout button numbers.
  var GP = { a: 0, b: 1, x: 2, y: 3, lb: 4, rb: 5, lt: 6, rt: 7, back: 8, start: 9, up: 12, down: 13, left: 14, right: 15 };
  var DIRS = ['up', 'down', 'left', 'right'];

  function gamepadMap(id, i) {
    var maps = GAMEPADS[id] || [{}];
    return maps[Math.min(i, maps.length - 1)];
  }

  // The chart: what each button does in app `id`, as <kbd> chips and labels.
  function gamepadChart(id) {
    var map = gamepadMap(id, 0);
    var parts = [];
    function add(cap, text) { parts.push([cap, text]); }
    var arrows = { up: '↑', down: '↓', left: '←', right: '→' };
    DIRS.forEach(function (d) { if (map[d]) add('D-pad ' + arrows[d], map[d][1]); });
    if (map.stick) add('Left stick', 'as the D-pad');
    ['a', 'b', 'x', 'y', 'start'].forEach(function (n) { if (map[n]) add(n === 'start' ? 'Start' : n.toUpperCase(), map[n][1]); });
    var free = !map.stick ? (DIRS.some(function (d) { return map[d]; }) ? 'Left stick' : 'Stick or D-pad') : 'Right stick';
    add(free, 'pointer');
    if (!map.a) add('A', 'click (hold to drag)');
    if (!map.b) add('B', 'right-click');
    if (map.a || map.b || map.stick) add('RT / LT', 'click / right-click');
    if ((GAMEPADS[id] || []).length > 1) add('2nd controller', 'player 2');
    add('LB / RB', 'other apps');
    add('Back', 'this chart');
    var nodes = [];
    parts.forEach(function (p, i) {
      if (i) nodes.push(document.createTextNode(' · '));
      var k = document.createElement('kbd');
      k.className = 'shim-gp-cap';
      k.textContent = p[0];
      nodes.push(k, document.createTextNode(' ' + p[1]));
    });
    return nodes;
  }

  // Works one page's app from controller snapshots ({ t, pads: [{ b: [pressed...], a: [axes...] }] }): presses
  // and releases the mapped keys, and moves and clicks the pointer. The page polls the controllers itself, or
  // (in the home page's player) gets the snapshots from the home page.
  function gamepadDriver() {
    var prev = [];       // per controller: the inputs held at the last snapshot
    var eaten = [];      // per controller: inputs that pressed Play on the title screen, ignored until released
    var holders = {};    // key name -> how many inputs hold it down
    var lastT = 0;
    var cursor = null, cx = innerWidth / 2, cy = innerHeight / 2, hideTimer = null;
    var downEl = null, downButton = -1;

    function lit(name, down) {
      var which = keyOf(name).which;
      if (padLight) padLight(which, down);
      if (embedded && window.parent !== window) {
        window.parent.postMessage({ oldjavaKeyLit: { which: which, down: down } }, location.origin);
      }
    }
    function key(name, down) {
      var n = (holders[name] || 0) + (down ? 1 : -1);
      holders[name] = Math.max(n, 0);
      if (down && n === 1) { fireKey('keydown', name); lit(name, true); }
      if (!down && n === 0) { fireKey('keyup', name); lit(name, false); }
    }

    function showCursor() {
      if (!cursor) {
        cursor = document.createElement('div');
        cursor.className = 'shim-gp-cursor';
        cursor.setAttribute('aria-hidden', 'true');
        document.body.appendChild(cursor);
      }
      cursor.hidden = false;
      cursor.style.transform = 'translate(' + cx + 'px, ' + cy + 'px)';
      clearTimeout(hideTimer);
      hideTimer = setTimeout(function () { if (cursor && downButton < 0) cursor.hidden = true; }, 5000);
    }
    // A real mouse takes over from the pointer.
    document.addEventListener('mousemove', function (e) {
      if (!e.isTrusted || !cursor) return;
      cx = e.clientX;
      cy = e.clientY;
      cursor.hidden = true;
    }, true);

    function mouse(type, el, button, buttons) {
      el.dispatchEvent(new MouseEvent(type, {
        bubbles: true, cancelable: true, view: window, detail: 1,
        clientX: cx, clientY: cy, screenX: cx, screenY: cy, button: button, buttons: buttons
      }));
    }
    function under() { return document.elementFromPoint(cx, cy); }
    // The same events, in the same order, as a mouse button: down, moves while held, up, then click (or contextmenu).
    function mouseButton(button, down) {
      showCursor();
      var el = under();
      if (down) {
        if (downButton >= 0 || !el) return;
        downButton = button;
        downEl = el;
        mouse('mousedown', el, button, button === 0 ? 1 : 2);
        if (el.matches && el.matches('input, select, textarea')) el.focus();
        return;
      }
      if (button !== downButton) return;
      downButton = -1;
      var from = downEl;
      downEl = null;
      if (!el) return;
      mouse('mouseup', el, button, 0);
      if (button === 2) { mouse('contextmenu', el, 2, 0); return; }
      var common = from;
      while (common && !common.contains(el)) common = common.parentElement;
      if (common) mouse('click', common, 0, 0);
    }
    function moveCursor(dx, dy) {
      if (!dx && !dy) return;
      cx = Math.max(0, Math.min(innerWidth - 1, cx + dx));
      cy = Math.max(0, Math.min(innerHeight - 1, cy + dy));
      showCursor();
      var el = under();
      if (el) mouse('mousemove', el, 0, downButton < 0 ? 0 : (downButton === 0 ? 1 : 2));
    }

    function stick(x, y) {
      var m = Math.hypot(x, y);
      if (m < 0.2) return [0, 0];
      var k = Math.pow((Math.min(m, 1) - 0.2) / 0.8, 1.6) / m;
      return [x * k, y * k];
    }

    return function drive(snap) {
      var dt = lastT ? Math.min(Math.max(snap.t - lastT, 0), 100) / 1000 : 0;
      lastT = snap.t;
      var count = Math.max(prev.length, snap.pads.length);
      var mx = 0, my = 0;
      for (var i = 0; i < count; i++) {
        var pad = snap.pads[i] || { b: [], a: [] };
        var map = gamepadMap(appId(), i);
        var was = prev[i] || {};
        var now = {};
        Object.keys(GP).forEach(function (n) { now[n] = !!pad.b[GP[n]]; });
        var ls = stick(pad.a[0] || 0, pad.a[1] || 0);
        var rs = stick(pad.a[2] || 0, pad.a[3] || 0);
        if (map.stick) {
          now.up = now.up || ls[1] < -0.3;
          now.down = now.down || ls[1] > 0.3;
          now.left = now.left || ls[0] < -0.3;
          now.right = now.right || ls[0] > 0.3;
        } else {
          mx += ls[0];
          my += ls[1];
        }
        mx += rs[0];
        my += rs[1];
        // On the title screen any button presses Play, and the app never sees that press or its release.
        var play = document.documentElement.hasAttribute('data-title') && document.querySelector('.shim-title-play');
        var eat = eaten[i] || (eaten[i] = {});
        Object.keys(now).forEach(function (n) {
          if (n === 'lb' || n === 'rb' || n === 'back') return;
          if (eat[n]) { if (!now[n]) delete eat[n]; return; }
          if (now[n] === !!was[n]) return;
          if (now[n] && play) {
            if (!play.disabled) play.click();
            eat[n] = true;
            return;
          }
          if (map[n]) key(map[n][0], now[n]);
          else if (n === 'a' || n === 'rt') mouseButton(0, now[n]);
          else if (n === 'b' || n === 'lt') mouseButton(2, now[n]);
        });
        if (!map.stick) DIRS.forEach(function (d) {
          if (!map[d] && now[d]) { mx += d === 'left' ? -0.6 : d === 'right' ? 0.6 : 0; my += d === 'up' ? -0.6 : d === 'down' ? 0.6 : 0; }
        });
        prev[i] = now;
      }
      prev.length = eaten.length = snap.pads.length;
      moveCursor(mx * 1100 * dt, my * 1100 * dt);
    };
  }

  var padLight = null;   // the app page's controller lights (padOnPage)

  // Polls the controllers on a top-level page. LB / RB and Back are the page's; the rest drives the app, here
  // or in the home page's player.
  function gamepads(isApp) {
    if (!navigator.getGamepads) return;
    var drive = isApp ? gamepadDriver() : null;
    if (embedded) {
      if (window.parent === window) return;
      window.addEventListener('message', function (e) {
        var s = e.data && e.data.oldjavaGamepad;
        if (e.origin === location.origin && e.source === window.parent && s && s.pads) drive(s);
      });
      return;
    }
    var running = false, shown = false, held = {};
    var hintText = '', hintTimer = null;
    function currentApp() {
      if (isApp) return appId();
      var frame = document.getElementById('player-frame');
      var m = frame && /([A-Za-z0-9]+)\/(?:index|then)\.html/.exec(frame.getAttribute('src') || '');
      return m ? m[1] : null;
    }
    function chart() {
      var id = currentApp();
      if (!id) return;
      var tag = document.createElement('strong');
      tag.className = 'shim-news-tag';
      tag.textContent = 'Controller';
      if (news) { news.show('gamepad', [tag], gamepadChart(id), [], 15000); return; }
      var hint = document.getElementById('player-hint');
      if (!hint) return;
      // The app's hint comes back after a while, unless the player has put another app's hint there since.
      if (!hint.querySelector('.shim-gp-cap')) hintText = hint.innerHTML;
      hint.textContent = '';
      [tag, document.createTextNode(' ')].concat(gamepadChart(id)).forEach(function (n) { hint.appendChild(n); });
      clearTimeout(hintTimer);
      hintTimer = setTimeout(function () { if (hint.querySelector('.shim-gp-cap')) hint.innerHTML = hintText; }, 15000);
    }
    function step(by) {
      if (!isApp) {
        var b = document.getElementById(by < 0 ? 'player-prev' : 'player-next');
        if (!b) return;
        b.click();
        chart();   // the next app's buttons, in place of its hint
        return;
      }
      catalog().then(function (apps) {
        var i = indexOf(apps, appId());
        if (i < 0) return;
        location.href = root() + apps[(i + by + apps.length) % apps.length].id + '/index.html';
      });
    }
    function send(snap) {
      if (drive) { drive(snap); return; }
      var frame = document.getElementById('player-frame');
      if (frame && frame.contentWindow) frame.contentWindow.postMessage({ oldjavaGamepad: snap }, location.origin);
    }
    function poll() {
      var list = Array.prototype.filter.call(navigator.getGamepads(), function (p) { return p && p.connected; });
      var pads = list.map(function (p) {
        return { b: p.buttons.map(function (b) { return b.pressed || b.value > 0.5; }), a: Array.prototype.slice.call(p.axes, 0, 4) };
      });
      var any = { lb: false, rb: false, back: false };
      pads.forEach(function (p) { Object.keys(any).forEach(function (n) { any[n] = any[n] || !!p.b[GP[n]]; }); });
      Object.keys(any).forEach(function (n) {
        if (any[n] && !held[n]) {
          if (n === 'back') chart(); else step(n === 'lb' ? -1 : 1);
        }
        held[n] = any[n];
      });
      send({ t: performance.now(), pads: document.hidden ? [] : pads });
      if (!list.length) { running = false; return; }
      requestAnimationFrame(poll);
    }
    function start() {
      if (!shown) { shown = true; chart(); }
      if (!running) { running = true; requestAnimationFrame(poll); }
    }
    window.addEventListener('gamepadconnected', start);
    // One already in use on the page before this one (the browser reports it after its first button press).
    if (Array.prototype.some.call(navigator.getGamepads(), function (p) { return p && p.connected; })) start();
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
            then: li.hasAttribute('data-then'),
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

  // ---- then and now ----
  // An app whose gallery card has data-then can also run as it was first restored: <App>/then.html
  // (<body data-then>) runs then.js, compiled by web/then (web/then-nathanos) from master as it was after #9, the
  // recovered code with the light bug fixes of "Modernize all 20 apps", before the reworks from #10 on. It reads
  // its source viewer files from then-source/ and keeps its own saves (oldjava.saves.<App>.then, as web.rt.Saves
  // does). The band's Then | Now switch and the title screens go between the two pages, and the title screen of
  // then.html lists the quirks its README still listed at that commit.
  function isThen() { return document.body.hasAttribute('data-then'); }
  function sourceDir() { return isThen() ? 'then-source/' : 'source/'; }
  function programFile() { return isThen() ? 'then.js' : 'classes.js'; }
  function saveStore() { return appId() + (isThen() ? '.then' : ''); }
  function eraPage(then) { return (then ? 'then.html' : 'index.html') + (embedded ? '?embed' : ''); }

  // The "## Known quirks" list of an app's README as first restored (this page's app unless `id` says otherwise):
  // one entry per bullet, as [text, code, text, ...].
  var quirkLists = {};
  function quirks(id) {
    id = id || appId();
    if (!quirkLists[id]) {
      quirkLists[id] = fetch(root() + id + '/then-source/README.md').then(function (r) {
        if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
        return r.text();
      }).then(function (text) {
        var section = /^## Known quirks[^\n]*\n([\s\S]*?)(?=^## |(?![\s\S]))/m.exec(text.replace(/\r\n?/g, '\n'));
        if (!section) return [];
        return section[1].split(/^- /m).slice(1).map(function (item) {
          return item.replace(/\*\*/g, '').replace(/\s+/g, ' ').trim().split('`');
        });
      }).catch(function (e) {
        console.warn('Could not read the known quirks: ' + e.message);
        return [];
      });
    }
    return quirkLists[id];
  }

  // A quirk as nodes: its `code` spans as <code>.
  function quirkNodes(parts) {
    return parts.map(function (part, i) {
      if (!(i % 2)) return document.createTextNode(part);
      var code = document.createElement('code');
      code.textContent = part;
      return code;
    });
  }

  // #6 fixed the quirks the READMEs listed before it, so most then.html pages have none to list.
  var NO_QUIRKS = 'None left: its README at that commit lists no known quirks.';

  // The home page's player shows the quirks of the app it plays as first restored.
  window.oldjavaThen = { quirks: quirks, nodes: quirkNodes };

  // Then | Now: two links, the current one marked. Hidden until the catalog says the app has a then.
  function eraSwitch(className) {
    var box = document.createElement('div');
    box.className = className;
    box.setAttribute('role', 'group');
    box.setAttribute('aria-label', 'Version');
    box.hidden = true;
    [[true, 'Then', 'As first restored in 2026: the recovered code with light bug fixes, before the later reworks'],
     [false, 'Now', 'Today, fixed and reworked']].forEach(function (o) {
      var a = document.createElement('a');
      a.href = eraPage(o[0]);
      a.textContent = o[1];
      a.title = o[2];
      if (o[0] === isThen()) a.setAttribute('aria-current', 'page');
      box.appendChild(a);
    });
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      box.hidden = !(app && app.then);
    });
    return box;
  }

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
    playLink(a, app);
    return a;
  }

  // Makes a link open the app: in the home page's player, by asking the page to swap it in.
  function playLink(a, app) {
    a.href = pageOf(app);
    if (embedded && window.parent !== window) {
      a.setAttribute('data-sound', 'none');   // the home page plays the new app's start
      a.addEventListener('click', function (e) {
        if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
        e.preventDefault();
        window.parent.postMessage({ oldjavaPlay: app.id }, location.origin);
      });
    }
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
  // A game a friend sent (UltimateTicTacToe's Friend by link) or a tune (SynthesizerKeyboard's Record): the title
  // screen says what the link brought.
  var INVITE = /[?&]game=[a-i1-9]+(?:&|$)/i.test(location.search)
    ? 'A friend sent you a game, and it is your move. Press Play to answer it.'
    : /[?&]tune=(?:[a-m][1-8]){2,}(?:&|$)/i.test(location.search)
      ? 'A friend sent you a tune. Press Play to hear it, then play it back.'
      : '';
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
      if (apps.length) body.appendChild(embedSection(app ? app.id : null, apps));
      ((navigator.share && ways.firstChild) || copy).focus();
    });
  }
  window.oldjavaShare = { open: openShare };

  // ---- embed: put an app on your own site (play.html, built by embed.js) ----
  // The Share panel ends with "Put it on your site", closed: opening it adds embed.js, which builds the
  // <iframe> tag for play.html (the embeddable player) with a live preview. See web/README.md, Embed.
  var embedPromise = null;
  var embedOpenNext = false;
  // Opens the Share panel with "Put it on your site" already open (Find uses it).
  window.oldjavaShare.embed = function (id, opener) {
    if (shareBox) return;
    embedOpenNext = true;
    openShare(id, opener);
  };
  function embedScript() {
    if (!embedPromise) embedPromise = new Promise(function (resolve, reject) {
      var tag = document.createElement('script');
      tag.src = root() + 'embed.js';
      tag.onload = function () { resolve(window.oldjavaEmbed); };
      tag.onerror = function () { embedPromise = null; reject(new Error('embed.js did not load')); };
      document.head.appendChild(tag);
    });
    return embedPromise;
  }
  function embedSection(id, apps) {
    var box = document.createElement('details');
    box.className = 'shim-embed';
    var summary = document.createElement('summary');
    summary.innerHTML = '<span aria-hidden="true">&lt;/&gt;</span> Put it on your site';
    summary.setAttribute('data-sound', 'none');
    box.appendChild(summary);
    var built = false;
    if (embedOpenNext) { embedOpenNext = false; box.open = true; }   // Find's "Put an app on your site"
    box.addEventListener('toggle', function () {
      if (!box.open || built) return;
      built = true;
      playSound('open');
      embedScript().then(function (embed) {
        var inner = document.createElement('div');
        inner.className = 'shim-embed-body';
        box.appendChild(inner);
        embed.build(inner, { apps: apps, app: id, siteRoot: siteRoot, watchable: Object.keys(WATCH), sound: playSound });
        box.scrollIntoView({ block: 'nearest' });
      }, function (e) { built = false; console.warn(e.message); });
    });
    return box;
  }

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
      sourceList = fetch(sourceDir() + 'files.txt').then(function (r) {
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
        texts[f] = fetch(sourceDir() + f).then(function (r) {
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
      raw.href = sourceDir() + f;
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

  // The app's hint from the gallery, on the band's news line (the band's ? button shows it again).
  // On then.html the line reads out the quirks its README still listed then instead (or that there are none).
  function howToPlay() {
    Promise.all([catalog(), isThen() ? quirks() : []]).then(function (r) {
      var app = r[0][indexOf(r[0], appId())];
      if (!app || !news) return;
      var tag = document.createElement('strong');
      tag.className = 'shim-news-tag';
      tag.textContent = isThen() ? 'Known quirks' : 'How to play';
      var text = document.createElement('span');
      if (isThen() && r[1].length) {
        r[1].forEach(function (parts, i) {
          if (i) text.appendChild(document.createTextNode(' · '));
          quirkNodes(parts).forEach(function (n) { text.appendChild(n); });
        });
      } else if (isThen()) text.textContent = NO_QUIRKS;   // today's hint would describe today's app
      else text.textContent = app.hint;
      news.show('hint', [tag], [text], [], 12000);
    });
  }

  // The app called System.exit (for most apps: its window was closed). Offer it again, and three others.
  function exitScreen(msg) {
    if (news && news.kind() === 'hint') news.clear();
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
    if (replay && replay.live()) {   // the last moments before it closed
      var again = document.createElement('button');
      again.type = 'button';
      again.className = 'shim-exit-share';
      again.setAttribute('data-sound', 'none');
      again.textContent = '⟲ Replay';
      again.title = 'Watch, save or share the last moments of play';
      again.addEventListener('click', function () { replayNow(again); });
      box.appendChild(again);
    }
    box.querySelector('button').focus();
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (app) {
        var h = document.createElement('h2');
        h.textContent = app.name + ' has closed';
        box.insertBefore(h, box.firstChild);
        if (app.then && !embedded) {   // the player has its own Then | Now switch
          var era = document.createElement('a');
          era.className = 'shim-exit-era';
          era.href = eraPage(!isThen());
          era.textContent = isThen() ? 'Play today’s version' : 'Play it as first restored';
          box.insertBefore(era, tell.nextSibling);
        }
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

  // ---- pretend Windows (<body data-shell>: Windows and Windows+E) ----
  // Those two apps do nothing but press keys with java.awt.Robot: the Windows key once, or Win+E over and
  // over. The shim's Robot hands Windows key shortcuts to window.__shimShell instead of the app's window, and
  // this plays the Windows they were written for, inside the stage: the Windows key alone opens a Start menu
  // of every app on the site (type to search, Enter to play), and Win+E opens a File Explorer window on the
  // folder of apps (double-click one to play it). Windows outlives the program that pressed its keys, so an
  // exit waits until the Start menu is closed. Returns { holdExit(show) }.
  var MAX_EXPLORERS = 40;
  function pretendWindows() {
    var layer = element('div', 'shim-shell');
    document.body.appendChild(layer);
    var menu = null, heldExit = null, explorers = [], opened = 0, front = 0, tray = null;

    function element(tag, cls, text) {
      var e = document.createElement(tag);
      if (cls) e.className = cls;
      if (text) e.textContent = text;
      return e;
    }
    function go(app) {
      var a = document.createElement('a');
      playLink(a, app);
      a.click();
    }
    function picture(app, cls) {
      var pic = element('span', cls);
      var img = element('img');
      img.src = app.shot;
      img.alt = '';
      pic.appendChild(img);
      return pic;
    }

    function closeMenu() {
      if (!menu) return;
      menu.remove();
      menu = null;
      document.removeEventListener('pointerdown', outside, true);
      playSound('close');
      if (heldExit) {
        var show = heldExit;
        heldExit = null;
        show();
      }
    }
    function outside(e) { if (menu && !menu.contains(e.target)) closeMenu(); }
    function openMenu() {
      var box = menu = element('div', 'shim-start');
      box.setAttribute('role', 'dialog');
      box.setAttribute('aria-label', 'Start');
      var search = element('input', 'shim-start-search');
      search.type = 'search';
      search.placeholder = 'Type here to search';
      search.setAttribute('aria-label', 'Search apps');
      var head = element('div', 'shim-start-head');
      var heading = element('strong', null, 'All apps');
      var count = element('span');
      head.appendChild(heading);
      head.appendChild(count);
      var grid = element('div', 'shim-start-grid');
      var none = element('p', 'shim-start-none');
      none.hidden = true;
      var foot = element('div', 'shim-start-foot');
      var who = element('span', 'shim-start-user', 'Visitor');
      var power = element('button', 'shim-start-power', '⏻');
      power.type = 'button';
      power.title = 'Close the Start menu';
      power.setAttribute('aria-label', 'Close the Start menu');
      power.setAttribute('data-sound', 'none');
      power.addEventListener('click', closeMenu);
      foot.appendChild(who);
      foot.appendChild(power);
      [search, head, grid, none, foot].forEach(function (n) { box.appendChild(n); });
      ownKeys(box, closeMenu);
      layer.appendChild(box);
      document.addEventListener('pointerdown', outside, true);
      playSound('open');
      search.focus({ preventScroll: true });
      catalog().then(function (apps) {
        if (menu !== box) return;
        var tiles = apps.map(function (app) {
          var a = element('a', 'shim-tile');
          playLink(a, app);
          a.title = app.hint;
          var pic = picture(app, 'shim-tile-pic');
          if (played[app.id]) {
            var badge = element('span', 'shim-badge played', '✓');
            badge.title = 'Played';
            pic.appendChild(badge);
          }
          a.appendChild(pic);
          a.appendChild(element('span', 'shim-tile-name', app.name));
          grid.appendChild(a);
          return { app: app, a: a };
        });
        function shown() { return tiles.filter(function (t) { return !t.a.hidden; }); }
        function filter() {
          var q = search.value.trim().toLowerCase();
          tiles.forEach(function (t) {
            t.a.hidden = !!q && (t.app.name + ' ' + t.app.id + ' ' + t.app.hint).toLowerCase().indexOf(q) < 0;
            t.a.classList.remove('first');
          });
          var hits = shown();
          if (q && hits.length) hits[0].a.classList.add('first');
          heading.textContent = !q ? 'All apps' : hits.length ? 'Best match' : 'Search';
          count.textContent = hits.length + (hits.length === 1 ? ' app' : ' apps');
          none.hidden = hits.length > 0;
          grid.hidden = !hits.length;
          none.textContent = 'No app matches "' + search.value.trim() + '".';
        }
        search.addEventListener('input', filter);
        search.addEventListener('keydown', function (e) {
          var hits = shown();
          if (e.key === 'Enter' && hits.length) hits[0].a.click();
        });
        filter();
        var n = apps.filter(function (a) { return played[a.id]; }).length;
        who.textContent = 'Visitor · ' + n + ' of ' + apps.length + ' apps played';
      });
    }

    function showTray() {
      if (!explorers.length) {
        if (tray) tray.remove();
        tray = null;
        return;
      }
      if (!tray) {
        tray = element('div', 'shim-shell-tray');
        tray.appendChild(element('span', 'shim-folder-mini'));
        tray.appendChild(element('span', 'shim-shell-count'));
        var all = element('button', null, 'Close all');
        all.type = 'button';
        all.addEventListener('click', function () {
          explorers.forEach(function (w) { w.remove(); });
          explorers = [];
          showTray();
        });
        tray.appendChild(all);
        layer.appendChild(tray);
      }
      tray.querySelector('.shim-shell-count').textContent = 'File Explorer: ' + explorers.length +
        (explorers.length === 1 ? ' window' : ' windows') + (explorers.length >= MAX_EXPLORERS ? ' (the most this page opens)' : '');
    }
    function raise(w) {
      w.style.zIndex = String(++front);
      explorers.forEach(function (o) { o.classList.toggle('active', o === w); });
    }
    function draggable(w, handle) {
      handle.addEventListener('pointerdown', function (e) {
        if (e.button !== 0 || e.target.closest('button')) return;
        var x = e.clientX - w.offsetLeft, y = e.clientY - w.offsetTop;
        handle.setPointerCapture(e.pointerId);
        function move(m) {
          w.style.left = Math.max(80 - w.offsetWidth, Math.min(layer.clientWidth - 80, m.clientX - x)) + 'px';
          w.style.top = Math.max(0, Math.min(layer.clientHeight - 32, m.clientY - y)) + 'px';
        }
        function up() {
          handle.removeEventListener('pointermove', move);
          handle.removeEventListener('pointerup', up);
          handle.removeEventListener('pointercancel', up);
        }
        handle.addEventListener('pointermove', move);
        handle.addEventListener('pointerup', up);
        handle.addEventListener('pointercancel', up);
      });
    }
    function openExplorer() {
      if (explorers.length >= MAX_EXPLORERS) return;
      var w = element('div', 'shim-explorer');
      w.setAttribute('role', 'dialog');
      w.setAttribute('aria-label', 'File Explorer: java-applications');
      var bar = element('div', 'shim-ex-title');
      bar.appendChild(element('span', 'shim-folder-mini'));
      bar.appendChild(element('span', 'shim-ex-name', 'java-applications'));
      var close = closeButton(function () {
        w.remove();
        explorers = explorers.filter(function (o) { return o !== w; });
        showTray();
        playSound('close');
      });
      close.setAttribute('data-sound', 'none');
      bar.appendChild(close);
      var address = element('div', 'shim-ex-address');
      ['This PC', 'java-applications'].forEach(function (crumb, i) {
        if (i) address.appendChild(element('span', 'shim-ex-sep', '›'));
        address.appendChild(element('span', null, crumb));
      });
      var items = element('div', 'shim-ex-items');
      var status = element('div', 'shim-ex-status');
      var info = element('span', 'shim-ex-info');
      var open = element('button', null, 'Open');
      open.type = 'button';
      open.hidden = true;
      status.appendChild(info);
      status.appendChild(open);
      [bar, address, items, status].forEach(function (n) { w.appendChild(n); });
      ownKeys(w);
      w.addEventListener('pointerdown', function () { raise(w); });
      draggable(w, bar);

      // Cascaded down and to the right from the top left, as Windows does, starting over when the next would
      // not fit. The bottom 44 px are left to the tray.
      var tall = layer.clientHeight - 44;
      var width = Math.min(560, layer.clientWidth - 16), height = Math.max(160, Math.min(380, tall - 16));
      var step = 28;
      var room = Math.max(1, Math.floor(Math.min(layer.clientWidth - width, tall - height) / step));
      var at = 8 + (opened++ % room) * step;
      w.style.left = at + 'px';
      w.style.top = at + 'px';
      w.style.width = width + 'px';
      w.style.height = height + 'px';
      layer.appendChild(w);
      explorers.push(w);
      raise(w);
      showTray();
      playSound('open');

      catalog().then(function (apps) {
        var selected = null;
        info.textContent = apps.length + ' items';
        open.addEventListener('click', function () { if (selected) go(selected); });
        apps.forEach(function (app) {
          var b = element('button', 'shim-ex-item');
          b.type = 'button';
          b.title = app.name + ': ' + app.hint;
          b.setAttribute('data-sound', 'none');
          b.appendChild(picture(app, 'shim-folder'));
          b.appendChild(element('span', 'shim-ex-label', app.id));
          b.addEventListener('click', function () {
            selected = app;
            Array.prototype.forEach.call(items.children, function (o) { o.classList.toggle('sel', o === b); });
            info.textContent = app.name + ': ' + app.hint;
            open.hidden = false;
          });
          b.addEventListener('dblclick', function () { go(app); });
          b.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') return;
            e.preventDefault();
            go(app);
          });
          items.appendChild(b);
        });
      });
    }

    window.__shimShell = function (keycode) {
      if (keycode === 524) {
        if (menu) closeMenu(); else openMenu();
      } else if (keycode === 69) {
        openExplorer();
      }
    };
    return { holdExit: function (show) { if (!menu) return false; heldExit = show; return true; } };
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
  // BibleDownloader's reading plan (audiobibledownloader.Plan): its name, days and chapters, the day it is on today
  // (planstart is its day 1, in local time) and the chapters read for it.
  var BIBLE_PLANS = { GOSPELS: ['The Gospels in 30 days', 30, 89], PROVERBS: ['Proverbs in a month', 31, 31],
    PSALMS: ['Psalms in 50 days', 50, 150], NEW: ['The New Testament in 90 days', 90, 260], YEAR: ['The Bible in a year', 365, 1189] };
  function biblePlan(text, count) {
    var id = /^plan=(\w+)$/m.exec(text), start = /^planstart=(\d+)-(\d+)-(\d+)$/m.exec(text);
    var plan = id && BIBLE_PLANS[id[1]];
    if (!plan || !start) return '';
    var now = new Date();
    var day = Math.round((new Date(now.getFullYear(), now.getMonth(), now.getDate()) - new Date(+start[1], start[2] - 1, +start[3])) / 864e5);
    var read = count(/^plandone=(.*)$/m.exec(text));
    return '; reading plan: ' + plan[0] + (day >= 0 && day < plan[1] ? ', day ' + (day + 1) + ' of ' + plan[1] : '')
      + ', ' + read + ' of ' + plan[2].toLocaleString('en-US') + ' chapters';
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
    GroupCreator: { file: '.groupcreator-class.txt', show: function (text) {
      var names = /^names=(.*)$/m.exec(text);
      var count = names ? names[1].split(',').filter(function (n) { return n.trim(); }).length : 0;
      var rounds = (text.match(/^round=/mg) || []).length;
      var picked = /^picked=(.*)$/m.exec(text);
      var turns = picked ? picked[1].split(',').filter(function (n) { return n.trim(); }).length : 0;
      if (!rounds && !turns) return '';
      var parts = [];
      if (rounds) parts.push(rounds + (rounds === 1 ? ' round' : ' rounds') + ' of groups remembered');
      if (turns) parts.push(turns + ' of ' + count + ' picked by the wheel');
      var apart = (text.match(/^apart=/mg) || []).length;
      if (apart) parts.push(apart + (apart === 1 ? ' pair' : ' pairs') + ' kept apart');
      return 'Your class: ' + count + (count === 1 ? ' name, ' : ' names, ') + parts.join(', ');
    } },
    TankGame: { file: '.tankgame-record.txt', show: function (text) {
      var n = text.trim().split('\n')[0].split(',').map(Number);
      if (n.length !== 2 || !(n[0] + n[1] > 0)) return '';
      return 'Your matches vs the computer: ' + n[0] + (n[0] === 1 ? ' win, ' : ' wins, ') + n[1] + (n[1] === 1 ? ' loss' : ' losses');
    } },
    // Your best run, the bot's best run and the bots you evolved (generations bred, their record).
    DinoGame: { file: '.dinogame.txt', show: function (text) {
      var best = /^best=(\d+)$/m.exec(text), bot = /^bot=(\d+)$/m.exec(text), evo = /^evolved=(\d+),(\d+)$/m.exec(text);
      var you = best ? +best[1] : 0, it = bot ? +bot[1] : 0;
      var bred = evo ? 'your evolved bots: ' + evo[2] + ' after ' + evo[1] + (evo[1] === '1' ? ' generation' : ' generations') : '';
      if (!you && !it) return bred ? bred.charAt(0).toUpperCase() + bred.slice(1) : '';
      return (you ? 'Your best: ' + you : 'No run of your own yet') + (it ? "; the bot's best: " + it : '') + (bred ? '; ' + bred : '');
    } },
    // The reader: the place it is open at, chapters read to the end and highlighted verses, and its reading plan.
    BibleDownloader: { file: '.bibledownloader.txt', show: function (text) {
      var place = /^place=(.+)$/m.exec(text);
      if (!place) return '';
      var read = /^read=(.*)$/m.exec(text);
      var marks = /^marks=(.*)$/m.exec(text);
      var count = function (m) { return m ? m[1].split(',').filter(function (r) { return r.trim(); }).length : 0; };
      var n = count(read), h = count(marks);
      // By heart: learn.<verse>=<translation>|<box>|<next review, yyyy-mm-dd>|<text>; box 0 is still being learned.
      var cards = text.match(/^learn\.\S+?=\w+\|\d+\|[\d-]+\|/mg) || [];
      var now = new Date(), today = now.getFullYear() + '-' + ('0' + (now.getMonth() + 1)).slice(-2) + '-' + ('0' + now.getDate()).slice(-2);
      var due = cards.filter(function (c) { var p = c.split('=')[1].split('|'); return p[1] === '0' || p[2] <= today; }).length;
      return 'Your Bible: open at ' + place[1].trim() + ', ' + n + ' of 1,189 chapters read'
        + (h ? ', ' + h + (h === 1 ? ' highlight' : ' highlights') : '') + biblePlan(text, count)
        + (cards.length ? '; by heart: ' + cards.length + (cards.length === 1 ? ' verse' : ' verses') + (due ? ', ' + due + ' to practice today' : '') : '');
    } },
    // The practice desk: the day it is on, the net worth at the last closing bell against the $10,000 start, and the rivals.
    StockChecker: { file: '.stockchecker-desk.txt', show: function (text) {
      var day = /^day=(\d+)$/m.exec(text);
      if (!day) return '';
      var closes = text.match(/^close=[\d.]+$/mg) || [];
      var line = 'Your trading desk: day ' + day[1];
      if (closes.length) {
        var last = +closes[closes.length - 1].slice(6);
        var pct = (last - 10000) / 100;
        line += ', closed day ' + closes.length + ' at $' + Math.round(last).toLocaleString('en-US') + ' (' + (pct >= 0 ? '+' : '') + pct.toFixed(1) + '% since day 1)';
        // The computer traders' last closes (rclose=Name:worth, stockchecker.Desk): how many you are ahead of.
        var rivals = {}, m, re = /^rclose=(\w+):([\d.]+)$/mg;
        while ((m = re.exec(text))) rivals[m[1]] = +m[2];
        var names = Object.keys(rivals);
        if (names.length) line += ', ahead of ' + names.filter(function (n) { return rivals[n] < last; }).length + ' of ' + names.length + ' rivals';
      }
      return line;
    } },
    // The desktop as it was left: its wallpaper (main.Wallpaper, "picture:<name>" for a Paint picture) and whether its
    // icons were arranged by hand.
    NathaNOS: { file: '.nathanos.txt', show: function (text) {
      var names = { photo: 'the original photo', night: 'Night sky', sunset: 'Sunset hills', ocean: 'Deep ocean', aurora: 'Aurora' };
      var wall = /^wallpaper=(.+)$/m.exec(text);
      var moved = /^icon=/m.test(text);
      if (!wall || (wall[1] === 'photo' && !moved)) return '';
      var painted = /^picture:(.+)$/.exec(wall[1]);
      return 'Your desktop: ' + (painted ? 'your painting ' + painted[1] + ' as the' : (names[wall[1]] || 'the original photo'))
        + ' wallpaper' + (moved ? ', icons arranged by hand' : '');
    } },
    // The saver it was left on, in the order of the app's list (screensaver.Main), and whether it cycles.
    ScreenSaver: { file: '.screensaver.txt', show: function (text) {
      var names = ['Green circles', 'Green squares', 'Bright circles', 'Starfield', 'Mystify', 'Pipes', 'Bouncing logo', 'Dot clock',
                   'Aquarium'];
      var saver = /^saver (\d+)$/m.exec(text);
      var name = saver && names[+saver[1]];
      if (!name) return '';
      // The aquarium's fish (screensaver.Tank): "fish <species> <day> <meals> <name>".
      var fish = text.match(/^fish \d+ \d+ \d+ \S+$/gm) || [];
      return 'Your screen saver: ' + name + (/^cycle off$/m.test(text) ? ', kept on screen' : ', then the next every 30 s')
        + (fish.length ? '; ' + fish.length + ' fish in your aquarium' : '');
    } },
    // The timer counts down against the wall clock, so a running one is still running (or has rung) on return.
    Clock: { file: '.clock.txt', show: function (text) {
      var v = {};
      text.split('\n').forEach(function (row) {
        var at = row.indexOf('=');
        if (at > 0) v[row.slice(0, at)] = row.slice(at + 1);
      });
      var parts = [];
      var end = +v.timerEnd, left = Math.ceil((end - Date.now()) / 1000);
      if (end > 0 && left > 0) parts.push('timer running, ' + Math.floor(left / 60) + ':' + (left % 60 < 10 ? '0' : '') + left % 60 + ' left');
      else if (end > 0 || +v.timerDone > 0) parts.push('your timer has rung');
      else if (+v.timer > 0) parts.push(Math.round(+v.timer / 60000) + '-minute timer set');
      var laps = (v.laps || '').split(',').filter(Boolean).length;
      if (+v.watchStart > 0) parts.push('stopwatch running');
      if (laps) parts.push(laps + (laps === 1 ? ' lap' : ' laps') + ' on the stopwatch');
      // Focus sessions: the one running, and today's log ("20261102:75:3" is a day, its minutes and its sessions).
      var focusEnd = +v.focusEnd, focusLeft = Math.ceil((focusEnd - Date.now()) / 60000);
      if (focusEnd > 0 && focusLeft > 0) parts.push((+v.focusPhase ? 'on a break, ' : 'focusing, ') + focusLeft + ' min left');
      var d = new Date(), today = String(d.getFullYear() * 10000 + (d.getMonth() + 1) * 100 + d.getDate());
      (v.focusLog || '').split(',').forEach(function (day) {
        var p = day.split(':');
        if (p[0] === today && +p[2] > 0) parts.push(p[2] + (+p[2] === 1 ? ' focus session' : ' focus sessions') + ' today');
      });
      return parts.length ? 'Your clock: ' + parts.join(', ') : '';
    } }
  };

  window.oldjavaRecords = { line: recordOf };

  // <body data-tab-title>: the browser tab shows the app's window title whenever it says more than the app's name
  // (Clock's "4:59 left - Clock"), so a running timer can be watched from another tab.
  function followTitle(desktop) {
    var base = document.title;
    new MutationObserver(function () {
      var bar = desktop.querySelector('.titlebar .title');
      var text = bar ? bar.textContent : '';
      var next = text && text !== base ? text : base;
      if (document.title !== next) document.title = next;
    }).observe(desktop, { subtree: true, childList: true, characterData: true });
  }

  function savedRecord(id) {
    var saves;
    try { saves = JSON.parse(localStorage.getItem('oldjava.saves.' + id) || '{}'); } catch (e) { return ''; }
    return recordOf(id, saves);
  }
  // The record line for an app's saved files ({ path: contents }), wherever they came from: this browser's store, or
  // saves brought from another device (the home page's Your records, saves.js).
  function recordOf(id, saves) {
    var paths = Object.keys(saves || {});
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

  // news.js (What's new), which the home page loads itself, added to an app page once.
  var newsPromise = null;
  function newsScript() {
    if (!newsPromise) newsPromise = new Promise(function (resolve, reject) {
      if (window.oldjavaNews) { resolve(window.oldjavaNews); return; }
      var tag = document.createElement('script');
      tag.src = root() + 'news.js';
      tag.onload = function () { resolve(window.oldjavaNews); };
      tag.onerror = function () { reject(new Error('news.js did not load')); };
      document.head.appendChild(tag);
    }).catch(function (e) { console.warn(e.message); return { story: function () {} }; });
    return newsPromise;
  }

  // ---- Clock follows you (clockbar.js) ----
  // While Clock's timer, stopwatch or a focus session runs (its saved ~/.clock.txt says so), every other page gets
  // clockbar.js: the time in the toolbar, a bell when it ends, and a game offered for a break. The Clock page gets
  // it too, for the break's game; it is added once something starts, in this page or another.
  var CLOCK_SAVES = 'oldjava.saves.Clock', CLOCK_OPEN = 'oldjava.clockOpen';
  var CLOCK_RUNNING = /(?:focusEnd|timerEnd|watchStart)=[1-9]/;
  function clockFollows() {
    var added = false;
    function check() {
      var saved = '';
      try { saved = localStorage.getItem(CLOCK_SAVES) || ''; } catch (e) { return; }
      if (added || !CLOCK_RUNNING.test(saved)) return;
      added = true;
      var tag = document.createElement('script');
      tag.src = root() + 'clockbar.js';
      document.head.appendChild(tag);
    }
    check();
    window.addEventListener('storage', function (e) { if (e.key === CLOCK_SAVES) check(); });
    if (clockPage()) setInterval(check, 1000);   // this page's own saves raise no storage event
  }
  function clockPage() { return appId() === 'Clock' && !isThen(); }   // the first restored Clock has no timer
  // Clock running on this page (or in the home page's player) rings for itself, so the other pages stay quiet.
  function clockOpen() {
    function beat() { try { localStorage.setItem(CLOCK_OPEN, String(Date.now())); } catch (e) { /* they ring too */ } }
    beat();
    setInterval(beat, 5000);
    window.addEventListener('pagehide', function () { try { localStorage.removeItem(CLOCK_OPEN); } catch (e) { /* stale soon */ } });
  }
  window.oldjavaSite = {
    news: function () { return news; },   // the app page's band news line (null on the home page)
    toast: function (shot, title, line, actions) { return floatToast(shot, title, line, [], actions); },
    toastLink: toastLink,
    pageOf: pageOf,
    root: root,
    appId: appId,
    clockPage: clockPage,
    // A link to an app page that skips its title screen, as Run again does.
    skipTitle: function (id) { try { sessionStorage.setItem(AGAIN_KEY, id); } catch (e) { /* it shows */ } }
  };

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
    var recordText = savedRecord(saveStore());
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
      try { localStorage.removeItem('oldjava.saves.' + saveStore()); } catch (e) { /* nothing was kept */ }
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
    read.textContent = isThen() ? 'Read the source as first restored' : 'Read the source';
    // Then and now: the other version of the app, once the catalog says it has one.
    var era = document.createElement('a');
    era.className = 'shim-title-era';
    era.href = eraPage(!isThen());
    era.textContent = isThen() ? 'Play today’s version' : 'Play it as first restored, before the reworks';
    era.hidden = true;
    // The games that play themselves: a look before playing (see watch).
    var watchLink = document.createElement('a');
    watchLink.className = 'shim-title-watch';
    watchLink.href = '?watch';
    watchLink.textContent = 'Watch the computer play it first';
    watchLink.hidden = !WATCH[appId()] || isThen();
    // Screen Saver is also the site's screen saver (see showSaver): its title screen turns that on and off.
    var saverRow = document.createElement('p');
    saverRow.className = 'shim-title-saver';
    saverRow.hidden = appId() !== 'ScreenSaver' || isThen();
    var saverText = document.createTextNode('');
    var saverToggle = document.createElement('button');
    saverToggle.type = 'button';
    saverToggle.className = 'shim-title-forget';   // kept from starting the app, like Forget
    function showSaverRow() {
      var on = saverOn();
      saverText.data = on ? 'Also this site’s screen saver, after ' + saverMinutes() + ' minutes away. '
        : 'This site’s screen saver is off. ';
      saverToggle.textContent = on ? 'Turn off' : 'Turn on';
    }
    saverToggle.addEventListener('click', function () { setSaverOn(!saverOn()); showSaverRow(); });
    showSaverRow();
    saverRow.appendChild(saverText);
    saverRow.appendChild(saverToggle);
    // What's new: the app's story, every change to it from the repo's history (news.js, loaded when needed).
    var story = document.createElement('details');
    story.className = 'shim-title-story';
    story.hidden = true;
    if (!isThen()) newsScript().then(function (news) { news.story(appId(), story); });
    [cat, name, hint, stamp, record, saverRow, play, press, read, watchLink, era, story].forEach(function (el) { text.appendChild(el); });
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
    // classes.js (then.js) has loaded by now (a blocking script in <head>): the app, compiled by TeaVM.
    var entry = window.performance && performance.getEntriesByName &&
      performance.getEntriesByName(new URL(programFile(), location.href).href)[0];
    line(programFile(), entry && entry.decodedBodySize ? size(entry.decodedBodySize) : 'ok');
    var total = files.length + 1;
    var done = 1;
    function progress() { meter.style.setProperty('--p', String(done / total)); }
    progress();

    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (!app) return;
      era.hidden = !app.then;
      name.textContent = app.name;
      if (isThen()) thenTitle(app); else {
        img.src = app.shot;
        cat.textContent = CATS[app.cat] || '';
        hint.textContent = app.hint;
      }
      var count = apps.filter(function (a) { return played[a.id]; }).length;
      stamp.textContent = played[app.id]
        ? '✓ In your passport since ' + new Date(played[app.id]).toLocaleDateString()
        : 'New: playing it adds stamp ' + (count + 1) + ' of ' + apps.length + ' to your passport';
      stamp.classList.toggle('played', !!played[app.id]);
      if (INVITE) {
        var invite = document.createElement('p');
        invite.className = 'shim-title-shared';
        invite.textContent = INVITE;
        text.insertBefore(invite, cat);
      }
      if (SHARED) {
        var theirs = Math.min(+SHARED[1], apps.length);
        var gift = document.createElement('p');
        gift.className = 'shim-title-shared';
        gift.textContent = 'Shared with you by a player with ' + theirs + ' of ' + apps.length + ' stamps. ' +
          (count ? 'You have ' + count + '.' : 'Play to start your own passport.');
        text.insertBefore(gift, cat);
      }
    });

    // then.html: the quirks its README still listed when it was first restored, where today's page shows its picture.
    function thenTitle(app) {
      screen.classList.add('then');
      cat.textContent = 'As first restored';
      hint.textContent = (app.origin === 'original'
        ? 'The author’s own source as it was found'
        : 'The code as it came out of the old jar') +
        ', with the light bug fixes of its 2026 restoration, before the later reworks.';
      var head = document.createElement('h2');
      head.textContent = 'Known quirks, from its README then';
      var list = document.createElement('ul');
      art.appendChild(head);
      art.appendChild(list);
      quirks().then(function (items) {
        if (!items.length) items = [[NO_QUIRKS]];
        items.forEach(function (parts) {
          var li = document.createElement('li');
          quirkNodes(parts).forEach(function (n) { li.appendChild(n); });
          list.appendChild(li);
        });
      });
    }

    var started = null;
    function go(e) {
      if (!started || screen.classList.contains('leaving')) return;
      if (e && e.type === 'keydown') {
        if (e.key === 'Tab' || e.key === 'Shift' || e.key === 'Control' || e.key === 'Alt' || e.key === 'Meta') return;
        if (e.target.closest && e.target.closest('.shim-toolbar, .shim-drawer, .shim-share, .shim-source, .shim-title-forget, .shim-title-story')) return;   // keys for those panels
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
    screen.addEventListener('click', function (e) { if (!e.target.closest('a, .shim-title-forget, .shim-title-story')) go(e); });

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

  // The app's voices meet in one gain on their way out, so instant replay can record them without the site's
  // own sounds.
  var appGain = null;
  var appMuted = false;   // while the computer plays (watch)
  function appOut(a) {
    if (!appGain) {
      appGain = a.createGain();
      appGain.gain.value = appMuted ? 0 : 1;
      appGain.connect(a.destination);
    }
    return appGain;
  }
  function setAppMuted(on) {
    appMuted = on;
    if (appGain) appGain.gain.value = on ? 0 : 1;
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
      osc.connect(gain).connect(appOut(a));
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
  // ---- javax.sound.sampled microphone (window.__shimMic) ----
  // The shim's TargetDataLine asks for the microphone here and drains its samples (floats from -1 to 1) from a
  // queue that keeps the last two seconds. Echo cancellation stays on (it keeps the app's own notes out); noise
  // suppression is off, since it can swallow a held note or a whistle.
  var mic = null;
  window.__shimMic = {
    open: function () {
      if (mic) return;
      var m = mic = { state: 'asking', chunks: [], queued: 0, out: new Float32Array(0), rate: 44100 };
      var a = ctx();
      var md = navigator.mediaDevices;
      if (!a || !md || !md.getUserMedia || !a.createScriptProcessor) { m.state = 'none'; return; }
      md.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: false, autoGainControl: true } }).then(function (stream) {
        if (mic !== m) { stream.getTracks().forEach(function (t) { t.stop(); }); return; }
        m.stream = stream;
        m.source = a.createMediaStreamSource(stream);
        m.node = a.createScriptProcessor(2048, 1, 1);
        m.node.onaudioprocess = function (e) {
          var data = new Float32Array(e.inputBuffer.getChannelData(0));
          m.chunks.push(data);
          m.queued += data.length;
          while (m.queued > a.sampleRate * 2) m.queued -= m.chunks.shift().length;
        };
        // A script node only runs while connected to the output; it writes silence there.
        m.source.connect(m.node);
        m.node.connect(a.destination);
        m.rate = a.sampleRate;
        if (a.state === 'suspended') a.resume();
        m.state = 'open';
      }, function (err) {
        if (mic === m) m.state = err && (err.name === 'NotAllowedError' || err.name === 'SecurityError') ? 'denied' : 'none';
      });
    },
    state: function () { return mic ? mic.state : 'none'; },
    rate: function () { return mic ? mic.rate : 44100; },
    queued: function () { return mic ? mic.queued : 0; },
    take: function (max) {
      var m = mic;
      if (!m || !m.queued) return 0;
      var n = Math.min(max, m.queued);
      if (m.out.length < n) m.out = new Float32Array(n);
      for (var i = 0; i < n;) {
        var chunk = m.chunks[0];
        var count = Math.min(chunk.length, n - i);
        m.out.set(chunk.subarray(0, count), i);
        i += count;
        if (count === chunk.length) m.chunks.shift(); else m.chunks[0] = chunk.subarray(count);
      }
      m.queued -= n;
      return n;
    },
    sample: function (i) { return mic ? mic.out[i] : 0; },
    close: function () {
      var m = mic;
      mic = null;
      if (!m) return;
      if (m.node) { m.node.onaudioprocess = null; m.node.disconnect(); }
      if (m.source) m.source.disconnect();
      if (m.stream) m.stream.getTracks().forEach(function (t) { t.stop(); });
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
    offline: [0, -6],
    // Clock's moments, heard on other pages of the site (clockbar.js): a falling phrase for a break, a rising one
    // back to work, and the timer's bell.
    breakTime: [12, 7, 4, 0],
    backToWork: [0, 4, 7, 12, 16],
    ring: [12, 7, 12, 7, 12]
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
    gamepads(isApp);
    if (!embedded) clockFollows();
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
      outPanel = document.createElement('pre');
      outPanel.className = 'shim-console';
      body.appendChild(outPanel);
      if (!embedded) stageEdge(outPanel, '--out-h');
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
      if (extras && !shell && !replay) replay = startReplay(desktop);
      if (clockPage() && !saverMode) clockOpen();
    }
    Promise.all(files.map(function (f) {
      return title ? preload(f).then(function () { title.loaded(f); }) : preload(f);
    })).then(function () {
      if (loading) loading.remove();
      if (typeof main !== 'function') {
        overlay(programFile() + ' is missing. Build the site with Maven first (see web/README.md).');
        return;
      }
      if (title) title.ready(start); else start();
    });
  });

  // ---- one machine: page-to-page transitions and prefetch ----
  // Going from one page of the site to another used to be a blank page and a fresh start. Now the browser's
  // cross-document view transitions (shim.css opts in with @view-transition) carry the visitor across: the thumbnail
  // they clicked (a Switch app card, an exit suggestion) or the home page's player grows into the
  // new app's title card, Next and Previous slide the stage sideways under a toolbar band that stays put, Then and Now
  // fade through sepia, All apps shrinks the stage back into the home page's player, and in Arcade every change is a
  // CRT changing channel. The old page says what kind of trip it was (TRIP_KEY in sessionStorage), the new page plays
  // it. Pages in the player (?embed) and the screen saver never animate their own navigations.
  var TRIP_KEY = 'oldjava.trip';
  var tripClick = { el: null, t: 0 };
  document.addEventListener('click', function (e) {
    var a = e.target && e.target.closest && e.target.closest('a[href]');
    if (a) tripClick = { el: a, t: Date.now() };
  }, true);
  var tripApps = null;   // the catalog, once read, so pageswap can place the next page synchronously
  document.addEventListener('DOMContentLoaded', function () { catalog().then(function (apps) { tripApps = apps; }); });
  // [app id or '' for the home page, then?] of a site address, or null for anything else.
  function tripPlace(url) {
    var u = new URL(url, location.href);
    if (u.origin !== location.origin || u.pathname.indexOf(new URL(siteRoot).pathname) !== 0) return null;
    var rest = u.pathname.slice(new URL(siteRoot).pathname.length);
    if (rest === '' || rest === 'index.html') return ['', false];
    var m = /^([A-Za-z0-9]+)\/(index|then)\.html$/.exec(rest);
    return m ? [m[1], m[2] === 'then'] : null;
  }
  function tripKind(from, to) {
    if (!to[0]) return 'home';
    if (from[0] === to[0]) return from[1] === to[1] ? 'app' : 'era';
    if (from[0] && tripApps) {
      var i = indexOf(tripApps, from[0]), j = indexOf(tripApps, to[0]), n = tripApps.length;
      if (i >= 0 && j >= 0 && (i + 1) % n === j) return 'forward';
      if (i >= 0 && j >= 0 && (j + 1) % n === i) return 'back';
    }
    return 'app';
  }
  function inView(el) {
    if (!el || !el.isConnected) return false;
    var r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0 && r.bottom > 0 && r.right > 0 && r.top < innerHeight && r.left < innerWidth;
  }
  // The element that becomes (or came from) the other page: an app page's title card or its stage, the home page's
  // player screen.
  function tripStage() {
    if (!document.body) return null;
    if (!document.body.hasAttribute('data-app')) return document.querySelector('#player .screen');
    return document.querySelector('.shim-title-card') || document.getElementById('desktop');
  }
  function tripName(el) {
    el.style.viewTransitionName = 'oldjava-hero';
    return function () { el.style.viewTransitionName = ''; };
  }
  window.addEventListener('pageswap', function (e) {
    if (!e.viewTransition) return;
    var to = e.activation && e.activation.entry && tripPlace(e.activation.entry.url);
    var from = tripPlace(location.href);
    if (embedded || !to || !from) { e.viewTransition.skipTransition(); return; }
    var kind = tripKind(from, to), hero = null;
    // Arcade changes the channel instead; a slide or a sepia fade moves the whole stage.
    if (document.documentElement.getAttribute('data-skin') !== 'arcade' && (kind === 'app' || kind === 'home')) {
      var clicked = Date.now() - tripClick.t < 2000 ? tripClick.el : null;
      var pic = clicked && clicked.querySelector('img');
      if (kind === 'app' && inView(pic)) hero = pic;
      else if (kind === 'home' || (clicked && clicked.closest('#player'))) hero = tripStage();
      if (!inView(hero)) hero = null;
    }
    if (hero) tripUnname = tripName(hero);   // taken off again if this page comes back (bfcache)
    try { sessionStorage.setItem(TRIP_KEY, JSON.stringify({ kind: kind, hero: !!hero, t: Date.now() })); } catch (err) { /* plain fade */ }
  });
  var tripUnname = null;
  window.addEventListener('pagereveal', function (e) {
    if (tripUnname) tripUnname();
    tripUnname = null;
    if (!e.viewTransition) return;
    if (embedded) { e.viewTransition.skipTransition(); return; }
    var trip = null;
    try {
      trip = JSON.parse(sessionStorage.getItem(TRIP_KEY));
      sessionStorage.removeItem(TRIP_KEY);
    } catch (err) { /* a plain fade */ }
    if (!trip || Date.now() - trip.t > 10000) trip = { kind: 'app', hero: false };
    var vt = e.viewTransition;
    try { vt.types.add(trip.kind); } catch (err) { /* no types: the default fade */ }
    if (!trip.hero) return;
    var unname = null;
    function name() {
      var el = tripStage();
      if (!unname && inView(el)) unname = tripName(el);
    }
    // The home page's player is parsed by now (its <link rel="expect"> holds the first frame for it); an app page's
    // title card is made on DOMContentLoaded, which may come before or after this event.
    name();
    if (!unname && document.readyState === 'loading') document.addEventListener('DOMContentLoaded', name);
    vt.finished.finally(function () { if (unname) unname(); });
  });

  // ---- Find (find.js): Ctrl+K anywhere, / on the home page ----
  // One box for the whole site: apps and their Then and Watch versions, things to do here (skins, sound, share,
  // replay, the passport...), and every Java source file of every app. find.js is added the first time it opens.
  // In the home page's player the keys are passed up, so the box opens over the whole home page.
  window.oldjavaSite.catalog = catalog;
  window.oldjavaSite.isThen = isThen;
  window.oldjavaSite.played = function (id) { return !!played[id]; };
  window.oldjavaSite.watchable = Object.keys(WATCH);
  window.oldjavaSite.sound = function (name) { playSound(name); };
  var findPromise = null;
  function findScript() {
    if (!findPromise) findPromise = new Promise(function (resolve, reject) {
      var tag = document.createElement('script');
      tag.src = root() + 'find.js';
      tag.onload = function () { resolve(window.oldjavaFind); };
      tag.onerror = function () { findPromise = null; reject(new Error('find.js did not load')); };
      document.head.appendChild(tag);
    });
    return findPromise;
  }
  function openFind(opener) {
    findScript().then(function (find) { find.open(opener); }, function (e) { console.warn(e.message); });
  }
  var findMac = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);
  var findOff = clipMode || document.documentElement.hasAttribute('data-shot') || document.documentElement.hasAttribute('data-saver');
  function findTyping(t) {
    return t && (t.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName));
  }
  if (!findOff) window.addEventListener('keydown', function (e) {
    var chord = (findMac ? e.metaKey : e.ctrlKey) && !e.altKey && !e.shiftKey && (e.key === 'k' || e.key === 'K');
    var slash = e.key === '/' && !root() && !e.ctrlKey && !e.metaKey && !e.altKey && !findTyping(e.target);
    if (!chord && !slash) return;
    if (!chord && window.oldjavaFind && window.oldjavaFind.isOpen()) return;   // a / typed in the box
    e.preventDefault();
    e.stopImmediatePropagation();
    if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaFind: true }, location.origin);
    else openFind(document.activeElement);
  }, true);
  // The home page's player asked for it.
  if (!findOff) window.addEventListener('message', function (e) {
    if (e.origin !== location.origin || !e.data || !e.data.oldjavaFind || root()) return;
    openFind(document.getElementById('player-frame'));
  });
  // A Find button in the toolbar, and any [data-find] button of the page (the home page has one by its gallery).
  if (!findOff) document.addEventListener('DOMContentLoaded', function () {
    var key = findMac ? '⌘K' : 'Ctrl K';
    Array.prototype.forEach.call(document.querySelectorAll('[data-find]'), function (b) {
      var shortcut = b.querySelector('kbd');
      if (shortcut) shortcut.textContent = key;
      b.addEventListener('click', function () { openFind(b); });
    });
    var bar = document.querySelector('.shim-toolbar');
    if (!bar || embedded) return;
    var find = document.createElement('button');
    find.type = 'button';
    find.className = 'shim-find-open';
    find.title = 'Find an app, something to do or a source file (' + key + ')';
    find.setAttribute('aria-keyshortcuts', findMac ? 'Meta+K' : 'Control+K');
    find.setAttribute('data-sound', 'none');   // the box plays its own open sound
    find.innerHTML = '<span aria-hidden="true">⌕</span><span class="shim-find-label"> Find</span><kbd>' + key + '</kbd>';
    find.addEventListener('click', function () { openFind(find); find.blur(); });
    var after = bar.querySelector('.shim-help') || bar.querySelector('.shim-bar-slot');
    if (after) bar.insertBefore(find, after.nextSibling); else bar.insertBefore(find, bar.firstChild);
  });

  // ---- Your skin (skinmaker.js): the maker panel, added the first time it opens ----
  // Choosing Yours in the toolbar's skin picker opens it, as do the Edit button beside the picker (shown while
  // Yours is worn), Find's "Make your own skin" and any [data-skin-maker] button (the home page has one). The
  // CSS itself is made at the top of this file (ownCss), so every page wears the skin without this script.
  var makerPromise = null;
  function makerScript() {
    if (!makerPromise) makerPromise = new Promise(function (resolve, reject) {
      var tag = document.createElement('script');
      tag.src = root() + 'skinmaker.js';
      tag.onload = function () { resolve(window.oldjavaSkinMaker); };
      tag.onerror = function () { makerPromise = null; reject(new Error('skinmaker.js did not load')); };
      document.head.appendChild(tag);
    });
    return makerPromise;
  }
  // What skinmaker.js borrows from this file.
  window.oldjavaSkin.own = {
    parse: ownParse, code: ownCode, saved: ownSaved, wear: wearOwn, css: ownCss, mix: mix,
    offer: function () { return skinOffer; },
    setSkin: function (v) { setSkin(v); }
  };
  function openMaker(opener) {
    if (embedded && window.parent !== window) { window.parent.postMessage({ oldjavaSkinMaker: true }, location.origin); return; }
    makerScript().then(function (maker) { maker.open(opener); }, function (e) { console.warn(e.message); });
  }
  window.oldjavaSkin.make = openMaker;
  window.addEventListener('message', function (e) {
    if (e.origin !== location.origin || !e.data || !e.data.oldjavaSkinMaker || root()) return;
    openMaker(document.getElementById('player-frame'));
  });
  if (!findOff) document.addEventListener('DOMContentLoaded', function () {
    Array.prototype.forEach.call(document.querySelectorAll('[data-skin-maker]'), function (b) {
      b.addEventListener('click', function () { openMaker(b); });
    });
    var select = document.querySelector('.shim-toolbar select');
    if (select && !embedded) {
      select.addEventListener('change', function () { if (select.value === 'own') openMaker(select); });
      var edit = document.createElement('button');
      edit.type = 'button';
      edit.className = 'shim-own-edit';
      edit.title = 'Change your skin: its color, desk, corners and windows';
      edit.setAttribute('data-sound', 'none');   // the panel plays its own open sound
      edit.innerHTML = '<span aria-hidden="true">✎</span><span class="shim-own-edit-label"> Edit</span>';
      edit.addEventListener('click', function () { openMaker(edit); edit.blur(); });
      select.parentNode.parentNode.insertBefore(edit, select.parentNode.nextSibling);
    }
    if (skinOffer) makerScript().then(function (maker) { maker.offer(); }, function (e) { console.warn(e.message); });
  });

  // ---- instant open: the next app is ready before the click ----
  // Pointing at a link to another page of the site (a gallery card, Switch app, Next, an exit suggestion, Find's
  // results, All apps) gets that page ready while the visitor decides. In Chrome and Edge, speculation rules
  // prerender it: the page loads in the background, fetches its program and files and shows its title screen with
  // Play already lit, so the click only swaps it in (the page transition still plays, and its boot log says how
  // much of a head start it had). The app itself waits for Play as always, and a page that is never opened is
  // thrown away. Elsewhere, and for the home page's player (an iframe, which a prerender cannot fill), the page's
  // files are fetched into the browser's cache instead (warm), so the app starts without waiting for the network.
  // Nothing is fetched ahead with Save-Data on or on a 2G connection.
  // Chrome throws a prerendered page away when it asks for the game controllers, which this file polls (the screen
  // saver's idle clock, the controller support): until the visitor opens the page there are none to ask about.
  if (document.prerendering && navigator.getGamepads) {
    var padsNow = navigator.getGamepads;
    navigator.getGamepads = function () { return document.prerendering ? [] : padsNow.call(navigator); };
  }
  var INSTANT_HOLD = 120;   // ms of pointing (or focus) before a page is warmed, so a sweep across the cards fetches nothing
  var net = navigator.connection || {};
  var instantOff = embedded || findOff || net.saveData || /(^|-)2g$/.test(net.effectiveType || '');
  var speculates = !!(window.HTMLScriptElement && HTMLScriptElement.supports && HTMLScriptElement.supports('speculationrules'));
  function speculate() {
    var base = new URL(siteRoot).pathname;
    function page(path) { return { href_matches: { pathname: base + path, baseURL: location.href } }; }
    var rules = { prerender: [{
      source: 'document',
      where: { and: [
        { or: [page(':app/index.html'), page(':app/then.html'), page('index.html'), page(''), page('story.html')] },
        { not: { href_matches: { pathname: location.pathname, baseURL: location.href } } },   // this page, ?watch or #source
        // the player's own pages, the games that start playing at once, and the gallery links that play in the player
        { not: { selector_matches: '[href*="embed"], [href*="watch"], [download], [target="_blank"], #apps a.play, #apps a.thumb' } }
      ] },
      eagerness: 'moderate'   // on hover (about 200 ms) or pointer down, the same as a tap on a phone
    }] };
    var tag = document.createElement('script');
    tag.type = 'speculationrules';
    tag.textContent = JSON.stringify(rules);
    document.head.appendChild(tag);
  }
  // Warm: an app page's HTML, its program and the files its <body data-files> lists, into the HTTP cache (and the
  // service worker's), each once.
  var warmed = {};
  function warmFile(url) {
    if (warmed[url]) return warmed[url];
    warmed[url] = fetch(url, { priority: 'low' }).then(function (r) {
      if (!r.ok) throw new Error(r.status);
      return r.headers.get('Content-Type') && /html/.test(r.headers.get('Content-Type')) ? r.text() : r.arrayBuffer();
    }).catch(function () { delete warmed[url]; return null; });
    return warmed[url];
  }
  function warmApp(id, then) {
    var dir = siteRoot + id + '/';
    warmFile(dir + (then ? 'then.js' : 'classes.js'));
    warmFile(dir + (then ? 'then.html' : 'index.html')).then(function (html) {
      if (typeof html !== 'string') return;
      var m = /<body[^>]*\sdata-files="([^"]*)"/.exec(html);
      if (m) m[1].split(/\s+/).filter(Boolean).forEach(function (f) { warmFile(dir + f); });
    });
  }
  // What a pointed-at element would open: { app, then, player } for a link to an app page, or for one of the home
  // page's player controls (its rail, whose buttons follow the gallery's order, and the gallery's Play and picture
  // links, which play in the player), whose app is warmed rather than prerendered.
  function instantTarget(el) {
    if (!el || !el.closest) return null;
    var rail = el.closest('#rail button');
    if (rail) {
      var li = document.querySelectorAll('#apps li[data-app]')[[].indexOf.call(document.querySelectorAll('#rail button'), rail)];
      return li ? { app: li.getAttribute('data-app'), then: false, player: true } : null;
    }
    var a = el.closest('a[href]');
    var place = a && !a.hasAttribute('download') && tripPlace(a.href);
    if (!place || !place[0]) return null;
    var here = tripPlace(location.href);
    if (here && here[0] === place[0] && here[1] === place[1]) return null;
    return { app: place[0], then: place[1], player: !!a.closest('#apps a.play, #apps a.thumb') };
  }
  // One page chosen ahead by the page itself (Find's highlighted result): a list rule of its own, replaced by the
  // next choice, which cancels the last one's prerender.
  var instantChosen = null;
  function prerenderOne(url) {
    if (instantChosen && instantChosen.url === url) return;
    if (instantChosen) instantChosen.tag.remove();
    var tag = document.createElement('script');
    tag.type = 'speculationrules';
    tag.textContent = JSON.stringify({ prerender: [{ source: 'list', urls: [url] }] });
    document.head.appendChild(tag);
    instantChosen = { url: url, tag: tag };
  }
  var instantSoon = 0;
  // The site's other scripts (find.js) get an app ready with this: on its own page, or in the home page's player.
  window.oldjavaSite.ready = function (id, then, player) {
    if (instantOff) return;
    clearTimeout(instantSoon);
    instantSoon = setTimeout(function () {
      if (speculates && !player) prerenderOne(siteRoot + id + (then ? '/then.html' : '/index.html'));
      else warmApp(id, then);
    }, INSTANT_HOLD);
  };
  if (!instantOff) {
    if (speculates) speculate();
    var instantTimer = 0;
    var instantArm = function (e) {
      var t = instantTarget(e.target);
      if (!t || (speculates && !t.player)) return;   // the speculation rules prerender it
      clearTimeout(instantTimer);
      if (e.type === 'pointerdown') warmApp(t.app, t.then);
      else instantTimer = setTimeout(function () { warmApp(t.app, t.then); }, INSTANT_HOLD);
    };
    document.addEventListener('pointerover', instantArm, true);
    document.addEventListener('focusin', instantArm, true);
    document.addEventListener('pointerdown', instantArm, { capture: true, passive: true });
    document.addEventListener('pointerout', function () { clearTimeout(instantTimer); }, true);
  }
  // A page that was prerendered says so in its title screen's boot log once the visitor opens it.
  function headStart() {
    var nav = performance.getEntriesByType && performance.getEntriesByType('navigation')[0];
    var ahead = nav && nav.activationStart;
    var log = ahead > 0 && document.querySelector('.shim-title-log');
    if (!log) return;
    var row = document.createElement('span');
    row.className = 'row ready';
    var l = document.createElement('span');
    l.textContent = 'Loaded ahead, while you chose it';
    var v = document.createElement('span');
    v.textContent = document.querySelector('.shim-title-play:not([disabled])') ? 'no wait'
      : (ahead < 1000 ? Math.round(ahead) + ' ms' : (ahead / 1000).toFixed(1) + ' s') + ' head start';
    row.appendChild(l);
    row.appendChild(v);
    log.appendChild(row);
    log.scrollTop = log.scrollHeight;
  }
  if (document.prerendering) document.addEventListener('prerenderingchange', headStart, { once: true });
})();
