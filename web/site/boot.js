/*
 * Page bootstrap shared by the app index and every app page.
 * - Applies the saved skin before first paint and adds the skin toggle.
 * - On an app page: preloads the files listed in <body data-files>, then calls the TeaVM main().
 * - Provides window.__shimSynth, the Web Audio voices behind javax.sound.midi.
 */
(function () {
  'use strict';

  var SKIN_KEY = 'oldjava.skin';
  var skin = 'classic';
  try {
    skin = localStorage.getItem(SKIN_KEY) || 'classic';
  } catch (e) { /* storage blocked: use the default */ }
  if (skin !== 'classic' && skin !== 'modern') skin = 'classic';
  document.documentElement.setAttribute('data-skin', skin);

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
    [['classic', 'Classic'], ['modern', 'Modern']].forEach(function (o) {
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
    toolbar(isApp);
    if (!isApp) return;

    var desktop = document.createElement('div');
    desktop.id = 'desktop';
    body.insertBefore(desktop, body.firstChild);

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
