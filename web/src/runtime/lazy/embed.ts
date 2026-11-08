/*
 * Put it on your site: the Share panel's builder for the embeddable player (play.html).
 *
 * boot.js adds this file the first time "Put it on your site" is opened in a Share panel, then calls
 * window.oldjavaEmbed.build(box, opts). The visitor picks an app, Now or Then, how it opens (a poster with a
 * Play button, or the computer already playing, for the apps that can watch) and a size, sees the widget live
 * as it will look, and copies the <iframe> tag. The tag's address is absolute (the address this site is served
 * from), since it is pasted into someone else's page.
 */

declare global {
  interface Window {
  oldjavaEmbed?: { build(box: HTMLElement, opts: OldjavaEmbedOptions): void };
  }
  /** What boot.js's embed.ts hands build(). */
  interface OldjavaEmbedOptions {
    apps: OldjavaCatalogApp[]; app: string | null; siteRoot: string; watchable: string[]; sound?(name: string): void;
  }
}
// A row of choice() buttons, and the extras it puts on them.
type ChoiceButton = HTMLButtonElement & { _id: string };
type Choice = HTMLDivElement & { set(v: string): void; enable(id: string, on: boolean): void };
(function () {
  'use strict';

  var SIZES = [
    { id: 'fit', label: 'Fit width', w: 0, h: 0 },
    { id: 's', label: '480 × 320', w: 480, h: 320 },
    { id: 'm', label: '640 × 420', w: 640, h: 420 },
    { id: 'l', label: '900 × 560', w: 900, h: 560 }
  ];

  function el<K extends keyof HTMLElementTagNameMap>(tag: K, cls?: string | null, text?: string | null) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text) e.textContent = text;
    return e;
  }

  function escapeAttr(s: string) {
    return String(s).replace(/"/g, '&quot;').replace(/</g, '&lt;');   // a bare & before a word is fine in HTML
  }

  // A row of radio-like buttons (aria-pressed), the way the Share panel's other choices look.
  function choice(name: string, items: { id: string; label: string }[], value: string, onPick: (id: string) => void) {
    var group = el('div', 'shim-embed-choice') as Choice;
    group.setAttribute('role', 'group');
    group.setAttribute('aria-label', name);
    var buttons = items.map(function (item) {
      var b = el('button', null, item.label) as ChoiceButton;
      b.type = 'button';
      b.setAttribute('data-sound', 'none');
      b.addEventListener('click', function () {
        if (b.disabled) return;
        set(item.id);
        onPick(item.id);
      });
      b._id = item.id;
      group.appendChild(b);
      return b;
    });
    function set(v: string) {
      buttons.forEach(function (b) { b.setAttribute('aria-pressed', String(b._id === v)); });
    }
    set(value);
    group.set = set;
    group.enable = function (id: string, on: boolean) {
      buttons.forEach(function (b) { if (b._id === id) { b.disabled = !on; b.title = on ? '' : 'Not for this app'; } });
    };
    return group;
  }

  function build(box: HTMLElement, opts: OldjavaEmbedOptions) {
    var apps = opts.apps;
    var root = opts.siteRoot;   // absolute, ends in /
    var state = { app: opts.app || (apps[0] && apps[0].id), then: false, watch: false, size: 'fit' };
    function app() { return apps.filter(function (a) { return a.id === state.app; })[0] || apps[0]; }
    function watchable(a: OldjavaCatalogApp) { return opts.watchable.indexOf(a.id) >= 0; }

    var intro = el('p', 'shim-embed-intro',
      'Paste this tag into a blog post, a class page or your own site, and visitors play the app right there. ' +
      'It shows a picture and a Play button until they press it, and links back here.');

    var row = el('div', 'shim-embed-row');
    var pickLabel = el('label', 'shim-embed-pick', 'App');
    var pick = el('select');
    apps.forEach(function (a) {
      var o = el('option', null, a.name);
      o.value = a.id;
      pick.appendChild(o);
    });
    pick.value = state.app;
    pickLabel.appendChild(pick);
    row.appendChild(pickLabel);

    var era = choice('Version', [{ id: 'now', label: 'Now' }, { id: 'then', label: 'Then' }], 'now', function (v) {
      state.then = v === 'then';
      if (state.then) { state.watch = false; opens.set('play'); }
      update();
    });
    var opens = choice('Opens with', [{ id: 'play', label: 'Play button' }, { id: 'watch', label: 'Computer playing' }], 'play',
      function (v) { state.watch = v === 'watch'; update(); });
    var size = choice('Size', SIZES, 'fit', function (v) { state.size = v; update(); });

    function labelled(text: string, control: HTMLElement) {
      var wrap = el('div', 'shim-embed-field');
      wrap.appendChild(el('span', 'shim-embed-name', text));
      wrap.appendChild(control);
      return wrap;
    }
    row.appendChild(labelled('Version', era));
    var opts2 = el('div', 'shim-embed-row');
    opts2.appendChild(labelled('Opens with', opens));
    var sizes = el('div', 'shim-embed-row');
    sizes.appendChild(labelled('Size', size));

    var view = el('div', 'shim-embed-view');
    var frame = el('iframe');
    frame.setAttribute('loading', 'lazy');
    frame.setAttribute('allow', 'autoplay; fullscreen; clipboard-write');
    view.appendChild(frame);

    var code = el('textarea', 'shim-embed-code');
    code.readOnly = true;
    code.rows = 4;
    code.setAttribute('aria-label', 'Embed code');
    code.addEventListener('focus', function () { code.select(); });
    var copyRow = el('div', 'shim-share-link');
    var copy = el('button', 'shim-share-copy', 'Copy embed code');
    copy.type = 'button';
    copy.setAttribute('data-sound', 'none');
    copy.addEventListener('click', function () {
      function done(ok: boolean) {
        copy.textContent = ok ? 'Copied ✓' : 'Press Ctrl+C';
        if (opts.sound) opts.sound(ok ? 'saved' : 'tap');
        setTimeout(function () { copy.textContent = 'Copy embed code'; }, 2000);
      }
      code.select();
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code.value).then(function () { done(true); }, function () { done(false); });
      } else {
        var ok = false;
        try { ok = document.execCommand('copy'); } catch (e) { /* select it by hand */ }
        done(ok);
      }
    });
    var test = el('a', 'shim-share-way', 'Open it');
    test.target = '_blank';
    test.rel = 'noopener';
    test.title = 'Open the widget on its own, as visitors will see it';
    copyRow.appendChild(copy);
    copyRow.appendChild(test);

    function src() {
      var a = app();
      return root + 'play.html?app=' + encodeURIComponent(a.id) +
        (state.then && a.then ? '&then' : '') + (state.watch && watchable(a) && !state.then ? '&watch' : '');
    }

    var shown = '';
    function update() {
      var a = app();
      era.enable('then', a.then);
      if (!a.then && state.then) { state.then = false; era.set('now'); }
      opens.enable('watch', watchable(a) && !state.then);
      if ((!watchable(a) || state.then) && state.watch) { state.watch = false; opens.set('play'); }
      var s = SIZES.filter(function (x) { return x.id === state.size; })[0];
      var url = src();
      var title = a.name + (state.then ? ' (as first restored)' : '') + ', an old Java app you can play';
      var style = s.w ? 'border:0;border-radius:12px;max-width:100%' : 'width:100%;aspect-ratio:16/10;border:0;border-radius:12px';
      code.value = '<iframe src="' + escapeAttr(url) + '"' + (s.w ? ' width="' + s.w + '" height="' + s.h + '"' : '') +
        ' style="' + style + '" title="' + escapeAttr(title) + '" allow="autoplay; fullscreen; clipboard-write"' +
        ' loading="lazy"></iframe>';
      test.href = url;
      // The preview is the widget itself, at the chosen size scaled into the panel.
      var vw = s.w || 640;
      var vh = s.h || 400;
      var width = view.clientWidth || 480;
      var scale = Math.min(1, width / vw);
      frame.style.width = vw + 'px';
      frame.style.height = vh + 'px';
      frame.style.transform = scale < 1 ? 'scale(' + scale + ')' : '';
      view.style.height = Math.round(vh * scale) + 'px';
      frame.title = 'Preview: ' + title;
      if (url !== shown) { frame.src = url; shown = url; }
    }
    pick.addEventListener('change', function () { state.app = pick.value; update(); });

    [intro, row, opts2, sizes, view, code, copyRow].forEach(function (e) { box.appendChild(e); });
    update();
    // The panel had no width while the section was closed; size the preview once it has one.
    requestAnimationFrame(update);
  }

  window.oldjavaEmbed = { build: build };
})();

export {};   // a module, imported when it is needed (its chunk is assets/embed.js)
