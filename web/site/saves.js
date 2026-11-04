/*
 * Your records (home page only): what the apps saved in this browser, and ways to take it to another one.
 *
 * Apps keep their files in localStorage (web.rt.Saves, oldjava.saves.<App>) and the passport its stamps
 * (oldjava.played), so on their own they stay in the browser that made them. A saves bundle carries them:
 *   { "oldjava": "saves", "v": 1, "at": ISO time, "saves": { "<App>[.then]": { "/.file.txt": contents } },
 *     "played": { "<App>": first play, epoch ms } }
 * as a file (Download / Load, also read and written by the desktop launcher, Launcher/SaveBundle.java) or as a
 * link (?saves=..., deflate-raw then base64url) shown as a QR code to scan with a phone. Nothing is changed by
 * opening a link or a file until the visitor picks what to bring in.
 */
(function () {
  'use strict';
  var PREFIX = 'oldjava.saves.', PLAYED = 'oldjava.played';
  var STORE = /^[A-Za-z0-9]+(\.then)?$/;
  var MAX_QR_VERSION = 30;   // bigger codes are too dense to scan from a screen

  function readJson(key) {
    try { var v = JSON.parse(localStorage.getItem(key) || '{}'); return v && typeof v === 'object' ? v : {}; } catch (e) { return {}; }
  }
  function storeNames() {
    var names = [];
    try {
      for (var i = 0; i < localStorage.length; i++) {
        var k = localStorage.key(i);
        if (k.indexOf(PREFIX) === 0 && STORE.test(k.slice(PREFIX.length))) names.push(k.slice(PREFIX.length));
      }
    } catch (e) { /* no storage */ }
    return names.sort();
  }

  // ---- the bundle ----
  function collect() {
    var saves = {};
    storeNames().forEach(function (name) {
      var files = readJson(PREFIX + name);
      if (Object.keys(files).length) saves[name] = files;
    });
    return { oldjava: 'saves', v: 1, at: new Date().toISOString(), saves: saves, played: readJson(PLAYED) };
  }
  // A bundle from a file or link, checked: stores and paths by name, contents as strings of bytes (one char each).
  function check(b) {
    if (!b || b.oldjava !== 'saves' || typeof b.saves !== 'object' || !b.saves) throw new Error('not an Old Java saves file');
    var saves = {}, played = {};
    Object.keys(b.saves).forEach(function (name) {
      var files = b.saves[name];
      if (!STORE.test(name) || !files || typeof files !== 'object') return;
      Object.keys(files).forEach(function (path) {
        var text = files[path];
        if (/^\/[^\0]+$/.test(path) && path.indexOf('/Downloads/') !== 0 && typeof text === 'string' && !/[^\x00-\xff]/.test(text)) {
          (saves[name] = saves[name] || {})[path] = text;
        }
      });
    });
    Object.keys(b.played || {}).forEach(function (id) {
      if (STORE.test(id) && b.played[id] > 0) played[id] = +b.played[id];
    });
    return { at: typeof b.at === 'string' ? b.at : '', saves: saves, played: played };
  }

  function b64url(bytes) {
    var s = '';
    for (var i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
    return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }
  function unb64url(text) {
    var s = atob(text.replace(/-/g, '+').replace(/_/g, '/'));
    var bytes = new Uint8Array(s.length);
    for (var i = 0; i < s.length; i++) bytes[i] = s.charCodeAt(i);
    return bytes;
  }
  function pipe(bytes, stream) {
    return new Response(new Blob([bytes]).stream().pipeThrough(stream)).arrayBuffer().then(function (b) { return new Uint8Array(b); });
  }
  // The link's payload: "z" + deflated JSON, or "j" + plain JSON where the browser cannot compress.
  function encode(bundle) {
    var json = new TextEncoder().encode(JSON.stringify(bundle));
    if (typeof CompressionStream === 'undefined') return Promise.resolve('j' + b64url(json));
    return pipe(json, new CompressionStream('deflate-raw')).then(function (z) { return 'z' + b64url(z); });
  }
  function decode(payload) {
    return Promise.resolve().then(function () {
      var bytes = unb64url(payload.slice(1));
      if (payload[0] === 'j') return bytes;
      if (payload[0] !== 'z' || typeof DecompressionStream === 'undefined') throw new Error('this browser cannot read that link');
      return pipe(bytes, new DecompressionStream('deflate-raw'));
    }).then(function (json) { return check(JSON.parse(new TextDecoder().decode(json))); });
  }

  // ---- the page ----
  var section = document.getElementById('records');
  if (!section) return;
  var list = document.getElementById('records-list');
  var status = document.getElementById('records-status');
  var phone = document.getElementById('records-phone');
  var cards = Array.prototype.slice.call(document.querySelectorAll('#apps li[data-app]'));
  function cardOf(id) { return cards.filter(function (li) { return li.getAttribute('data-app') === id; })[0]; }
  function nameOf(store) {
    var li = cardOf(store.split('.')[0]);
    return (li ? li.querySelector('strong').textContent : store.split('.')[0]) + (/\.then$/.test(store) ? ' (Then)' : '');
  }
  function line(store, files) {
    var text = /\.then$/.test(store) ? '' : window.oldjavaRecords.line(store, files);
    if (!text) {
      var names = Object.keys(files).map(function (p) { return p.split('/').pop(); });
      text = 'Saved: ' + names.join(', ');
    }
    return text.replace(/^Saved here from last time: /, 'Saved: ');
  }
  function kb(bytes) { return bytes < 1024 ? bytes + ' bytes' : (bytes / 1024).toFixed(bytes < 10240 ? 1 : 0) + ' KB'; }
  function say(text) { status.textContent = text; status.hidden = !text; }

  function render() {
    var saves = collect().saves, stores = Object.keys(saves);
    list.textContent = '';
    stores.forEach(function (store) {
      var id = store.split('.')[0], li = document.createElement('li'), card = cardOf(id);
      var b = document.createElement('button');
      b.type = 'button';
      b.title = 'Play ' + nameOf(store);
      if (card) {
        var img = document.createElement('img');
        img.src = card.querySelector('img').getAttribute('src');
        img.alt = '';
        img.loading = 'lazy';
        b.appendChild(img);
      }
      var text = document.createElement('span');
      var strong = document.createElement('strong');
      strong.textContent = nameOf(store);
      var rec = document.createElement('span');
      rec.textContent = line(store, saves[store]);
      text.appendChild(strong);
      text.appendChild(rec);
      b.appendChild(text);
      b.addEventListener('click', function () { if (window.oldjavaPassport.open) window.oldjavaPassport.open(id); });
      li.appendChild(b);
      list.appendChild(li);
    });
    var empty = !stores.length;
    document.getElementById('records-empty').hidden = !empty;
    document.getElementById('records-download').disabled = empty;
    document.getElementById('records-send').disabled = empty;
    document.getElementById('records-count').textContent = empty ? 'Nothing saved yet'
      : stores.length + (stores.length === 1 ? ' app has' : ' apps have') + ' saves here';
  }
  window.addEventListener('storage', function (e) { if (e.key === null || e.key.indexOf(PREFIX) === 0) render(); });
  // The player's app saves every second while it runs; keep the list in step without a storage event (same page).
  setInterval(function () { if (!document.hidden) render(); }, 5000);
  render();

  // Download: the bundle as a file. The desktop launcher reads the same file.
  document.getElementById('records-download').addEventListener('click', function () {
    var blob = new Blob([JSON.stringify(collect(), null, 1)], { type: 'application/json' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'oldjava-saves.json';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
    say('Downloaded oldjava-saves.json. Load it here in another browser, or in the desktop launcher (Saves).');
  });

  // Load: a file from Download (or the desktop launcher's Saves), then the same choice as a link.
  var picker = document.getElementById('records-file');
  document.getElementById('records-load').addEventListener('click', function () { picker.click(); });
  picker.addEventListener('change', function () {
    var file = picker.files[0];
    picker.value = '';
    if (!file) return;
    file.text().then(function (text) { return check(JSON.parse(text)); })
      .then(function (bundle) { incoming(bundle, file.name); })
      .catch(function (e) { say('Could not read ' + file.name + ': ' + e.message + '.'); });
  });

  // Send to a phone: a link carrying the bundle, as a QR code.
  var sendButton = document.getElementById('records-send');
  sendButton.addEventListener('click', function () {
    if (!phone.hidden) { phone.hidden = true; sendButton.setAttribute('aria-expanded', 'false'); return; }
    encode(collect()).then(function (payload) {
      var url = new URL('./', location.href);
      url.searchParams.set('saves', payload);
      var link = url.href, code = window.oldjavaQr(link);
      var box = document.getElementById('records-qr');
      var fits = code && code.version <= MAX_QR_VERSION;
      box.innerHTML = fits ? window.oldjavaQr.svg(code, 3) : '';
      if (fits) box.style.width = Math.min(360, Math.max(200, (code.size + 6) * 3.5)) + 'px';   // about 3.5 px a module
      box.hidden = !fits;
      var local = /^(localhost|127\.|\[::1\])/.test(location.host);
      document.getElementById('records-qr-text').textContent = !fits
        ? 'Too much to fit in a code (' + kb(link.length) + '). Copy the link instead, or use Download.'
        : 'Point your phone\'s camera at the code and open the link. Nothing changes there until you pick what to bring in.' +
          (local ? ' This page is served from this computer (' + location.host + '), so a phone can only open the link if it can reach this address.' : '');
      document.getElementById('records-link').value = link;
      phone.hidden = false;
      sendButton.setAttribute('aria-expanded', 'true');
    });
  });
  document.getElementById('records-copy').addEventListener('click', function () {
    var field = document.getElementById('records-link');
    (navigator.clipboard ? navigator.clipboard.writeText(field.value) : Promise.reject()).then(function () {
      say('Link copied. Open it in the other browser.');
    }, function () { field.select(); say('Select the link and copy it.'); });
  });

  // ---- bringing saves in ----
  var dialog = document.getElementById('records-in');
  function incoming(bundle, from) {
    var stores = Object.keys(bundle.saves);
    var stamps = Object.keys(bundle.played).filter(function (id) { return !window.oldjavaPassport.has(id) && cardOf(id); });
    if (!stores.length && !stamps.length) { say('Nothing in ' + from + ' to bring in.'); return; }
    var when = bundle.at ? new Date(bundle.at) : null;
    document.getElementById('records-in-text').textContent = 'From ' + from +
      (when && !isNaN(when) ? ', saved ' + when.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '') +
      '. Each app you tick takes these files in place of the ones saved here.';
    var rows = document.getElementById('records-in-list');
    rows.textContent = '';
    stores.forEach(function (store) {
      var here = readJson(PREFIX + store), there = bundle.saves[store];
      var same = Object.keys(there).every(function (p) { return here[p] === there[p]; });
      var li = document.createElement('li');
      var label = document.createElement('label');
      var box = document.createElement('input');
      box.type = 'checkbox';
      box.value = store;
      box.checked = !same;
      box.disabled = same;
      var text = document.createElement('span');
      var strong = document.createElement('strong');
      strong.textContent = nameOf(store);
      var a = document.createElement('span');
      a.textContent = 'Bringing: ' + line(store, there);
      var b = document.createElement('span');
      b.className = 'here';
      b.textContent = same ? 'Already the same here' : Object.keys(here).length ? 'Here now: ' + line(store, here) : 'Nothing saved here yet';
      text.appendChild(strong);
      text.appendChild(a);
      text.appendChild(b);
      label.appendChild(box);
      label.appendChild(text);
      li.appendChild(label);
      rows.appendChild(li);
    });
    var stampText = document.getElementById('records-in-stamps');
    stampText.hidden = !stamps.length;
    stampText.textContent = stamps.length ? 'Plus ' + stamps.length + ' passport ' + (stamps.length === 1 ? 'stamp' : 'stamps') +
      ' not collected here: ' + stamps.map(nameOf).join(', ') + '.' : '';
    dialog.bundle = bundle;
    dialog.stamps = stamps;
    if (dialog.showModal) dialog.showModal(); else dialog.setAttribute('open', '');
  }
  document.getElementById('records-in-cancel').addEventListener('click', function () { dialog.close(); });
  document.getElementById('records-in-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var bundle = dialog.bundle, stamps = dialog.stamps;
    var chosen = Array.prototype.filter.call(dialog.querySelectorAll('input[type=checkbox]'), function (b) { return b.checked; })
      .map(function (b) { return b.value; });
    dialog.close();
    // The player's app keeps its own copy of its files in memory and writes them back when it closes, so it is
    // closed before anything is written, then started again on what was brought in.
    var frame = document.getElementById('player-frame');
    var done = function () {
      try {
        chosen.forEach(function (store) {
          var files = readJson(PREFIX + store);
          Object.keys(bundle.saves[store]).forEach(function (p) { files[p] = bundle.saves[store][p]; });
          localStorage.setItem(PREFIX + store, JSON.stringify(files));
        });
        if (stamps.length) {
          var played = readJson(PLAYED);
          stamps.forEach(function (id) { played[id] = bundle.played[id]; });
          localStorage.setItem(PLAYED, JSON.stringify(played));
          window.oldjavaPassport.reload();
        }
        say('Brought in ' + (chosen.length ? chosen.length + (chosen.length === 1 ? ' app\'s saves' : ' apps\' saves') : 'no saves') +
          (stamps.length ? ' and ' + stamps.length + ' passport ' + (stamps.length === 1 ? 'stamp' : 'stamps') : '') + '.');
      } catch (err) {
        say('Could not keep them: ' + err.message + '.');
      }
      render();
      if (frame) document.getElementById('player-restart').click();
      if (window.oldjavaSound) window.oldjavaSound.play('saved');
    };
    if (!frame || !chosen.length) { done(); return; }
    frame.addEventListener('load', function once() { frame.removeEventListener('load', once); done(); });
    frame.src = 'about:blank';
  });

  // A link from Send to a phone: ?saves=... on this page. It is taken off the address at once, so a reload or a
  // bookmark does not offer it again.
  var params = new URLSearchParams(location.search);
  var payload = params.get('saves');
  if (payload) {
    params.delete('saves');
    var rest = params.toString();
    history.replaceState(null, '', location.pathname + (rest ? '?' + rest : '') + location.hash);
    decode(payload).then(function (bundle) {
      if (window.oldjavaArcade) window.oldjavaArcade.show('records');
      section.scrollIntoView({ block: 'start' });
      incoming(bundle, 'a link');
    }).catch(function () { say('Could not read the saves in that link: it may have been cut short when it was copied.'); });
  }
})();
