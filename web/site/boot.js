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
    window.__shimExited = function (msg) { overlay(msg, 'Run again'); };

    Promise.all(files.map(preload)).then(function () {
      if (loading) loading.remove();
      if (typeof main !== 'function') {
        overlay('classes.js is missing. Build the site with Maven first (see web/README.md).');
        return;
      }
      main(args);
    });
  });
})();
