/*
 * Your skin: the panel where a visitor makes the site's fourth skin, loaded by boot.js the first time it opens.
 * Five choices (light or dark, a color, a desk wallpaper, corners, window chrome) are put on as they are picked,
 * on this page and every other open page of the site, so the running app is the preview. Surprise me rolls all
 * five, the link shares the skin (?skin=<code>), and Undo goes back to the skin worn when the panel opened.
 * A page opened from such a link already wears the shared skin (boot.js); offer() asks whether to keep it.
 * boot.js makes the CSS (window.oldjavaSkin.own.css) and lends the rest through window.oldjavaSkin.own.
 */

declare global {
  interface Window {
  oldjavaSkinMaker?: { open(from?: Element | null): void; offer(): void; close(): void; isOpen(): boolean };
  }
}
// boot.js's skin-maker.ts has lent window.oldjavaSkin.own by the time this file runs.
type OwnApi = NonNullable<OldjavaSkin['own']>;
type OwnSkin = NonNullable<ReturnType<OwnApi['parse']>>;
// The panel's controls, kept for show().
interface Parts {
  base: HTMLButtonElement[]; colors: HTMLButtonElement[]; picker: HTMLInputElement; desks: HTMLButtonElement[];
  corners: HTMLButtonElement[]; chromes: HTMLButtonElement[]; link: HTMLInputElement; undo: HTMLButtonElement;
}
(function () {
  'use strict';

  var skins = window.oldjavaSkin as OldjavaSkin & { own: OwnApi }, own = skins.own, site = window.oldjavaSite || {};
  var COLORS = ['#2563eb', '#7c3aed', '#db2777', '#dc2626', '#ea580c', '#ca8a04', '#16a34a', '#0d9488', '#0891b2', '#475569'];
  var DESKS = [['plain', 'Plain'], ['fade', 'Fade'], ['grid', 'Grid'], ['dots', 'Dots'], ['stripes', 'Stripes'], ['aurora', 'Aurora']];
  var CORNERS = [['0', 'Square'], ['1', 'Soft'], ['2', 'Round']];
  var CHROMES = [['flat', 'Flat'], ['gloss', 'Glossy'], ['glow', 'Glow']];
  var NAMES: Record<string, string> = { modern: 'Modern', classic: 'Classic', arcade: 'Arcade', own: 'your skin' };
  var panel: HTMLElement | null = null, offerBox: HTMLElement | null = null, spec: OwnSkin = null!,
    before: { code: string; skin: string } | null = null, opener: HTMLElement | null = null, parts = {} as Parts;

  function el<K extends keyof HTMLElementTagNameMap>(tag: K, cls?: string | null, text?: string | null) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  }
  function sound(name: string) { if (site.sound) site.sound(name); }
  function deskOf(s: OwnSkin) { return (/--desk: ([^;]*);/.exec(own.css(s)) || [])[1] || ''; }
  function shareLink() { return location.origin + location.pathname + '?skin=' + own.code(spec); }

  // Puts the current choices on, here and on every open page, and shows them in the panel.
  function wear() {
    own.wear(own.code(spec));
    if (skins.get() !== 'own') skins.own.setSkin('own');
    show();
  }
  function show() {
    if (!panel) return;
    parts.base.forEach(function (b) { b.setAttribute('aria-pressed', String((b.value === 'd') === spec.dark)); });
    parts.colors.forEach(function (b) { b.setAttribute('aria-pressed', String(b.value === spec.color)); });
    parts.picker.value = spec.color;
    parts.desks.forEach(function (b) {
      b.setAttribute('aria-pressed', String(b.value === spec.desk));
      (b.firstChild as HTMLElement).style.background =deskOf({ dark: spec.dark, color: spec.color, desk: b.value, corners: spec.corners, chrome: spec.chrome });
    });
    parts.corners.forEach(function (b) { b.setAttribute('aria-pressed', String(+b.value === spec.corners)); });
    parts.chromes.forEach(function (b) { b.setAttribute('aria-pressed', String(b.value === spec.chrome)); });
    parts.link.value = shareLink();
  }

  // A labelled row of toggle buttons; pick(value) changes the spec.
  function row(title: string, options: string[][], cls: string, pick: (value: string) => void) {
    var group = el('div', 'shim-own-row');
    var label = el('div', 'shim-own-label', title);
    label.id = 'shim-own-' + title.toLowerCase();
    var buttons = el('div', 'shim-own-options ' + cls);
    buttons.setAttribute('role', 'group');
    buttons.setAttribute('aria-labelledby', label.id);
    var list = options.map(function (o) {
      var b = el('button', 'shim-own-option');
      b.type = 'button';
      b.value = o[0];
      if (cls === 'desks') {
        b.appendChild(el('span', 'shim-own-desk'));
        b.appendChild(el('span', 'shim-own-name', o[1]));
      } else if (cls === 'colors') {
        b.style.background = o[0];
        b.setAttribute('aria-label', o[1]);
        b.title = o[1];
      } else {
        b.textContent = o[1];
      }
      b.addEventListener('click', function () { pick(o[0]); wear(); });
      buttons.appendChild(b);
      return b;
    });
    group.appendChild(label);
    group.appendChild(buttons);
    return { node: group, buttons: list, box: buttons };
  }

  function build() {
    panel = el('section', 'shim-own');
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Your skin');
    var head = el('div', 'shim-own-head');
    var title = el('h2', 'shim-own-title', 'Your skin');
    var close = el('button', 'shim-own-x', '×');
    close.type = 'button';
    close.setAttribute('aria-label', 'Done');
    close.addEventListener('click', shut);
    head.appendChild(title);
    head.appendChild(close);
    panel.appendChild(head);
    panel.appendChild(el('p', 'shim-own-lead', 'Every page and app wears it as you choose. Make it yours, then send it to a friend.'));

    var base = row('Mode', [['l', 'Light'], ['d', 'Dark']], 'base', function (v) { spec.dark = v === 'd'; });
    var colors = row('Color', COLORS.map(function (c) { return [c, c]; }), 'colors', function (v) { spec.color = v; });
    var picker = document.createElement('input');
    picker.type = 'color';
    picker.className = 'shim-own-picker';
    picker.title = 'Any color';
    picker.setAttribute('aria-label', 'Any color');
    picker.addEventListener('input', function () { spec.color = picker.value.toLowerCase(); wear(); });
    colors.box.appendChild(picker);
    var desks = row('Desk', DESKS, 'desks', function (v) { spec.desk = v; });
    var corners = row('Corners', CORNERS, 'corners', function (v) { spec.corners = +v; });
    var chromes = row('Windows', CHROMES, 'chromes', function (v) { spec.chrome = v; });
    [base, colors, desks, corners, chromes].forEach(function (r) { panel!.appendChild(r.node); });

    var share = el('div', 'shim-own-share');
    var link = document.createElement('input');
    link.type = 'text';
    link.readOnly = true;
    link.className = 'shim-own-link';
    link.setAttribute('aria-label', 'Link to this skin');
    link.addEventListener('focus', function () { link.select(); });
    var copy = el('button', 'shim-own-copy', 'Copy link');
    copy.type = 'button';
    copy.addEventListener('click', function () {
      function said(text: string) { copy.textContent = text; setTimeout(function () { copy.textContent = 'Copy link'; }, 1800); }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(link.value).then(function () { said('Copied'); sound('saved'); },
          function () { link.focus(); said('Press Ctrl C'); });
      } else { link.focus(); said('Press Ctrl C'); }
    });
    share.appendChild(link);
    share.appendChild(copy);
    panel.appendChild(share);

    var foot = el('div', 'shim-own-foot');
    var surprise = el('button', 'shim-own-btn', '⚄ Surprise me');
    surprise.type = 'button';
    surprise.addEventListener('click', function () {
      function any<T>(list: T[]) { return list[Math.floor(Math.random() * list.length)]; }
      var hue = Math.floor(Math.random() * 360);
      spec = { dark: Math.random() < 0.5, color: hsl(hue, 0.55 + Math.random() * 0.35, 0.38 + Math.random() * 0.2),
        desk: any(DESKS)[0], corners: Math.floor(Math.random() * 3), chrome: any(CHROMES)[0] };
      wear();
      sound('skin');
    });
    var undo = el('button', 'shim-own-btn shim-own-undo', 'Undo');
    undo.type = 'button';
    undo.addEventListener('click', function () {
      own.wear(before!.code);
      skins.own.setSkin(before!.skin);
      shut();
    });
    var ok = el('button', 'shim-own-btn primary', 'Done');
    ok.type = 'button';
    ok.addEventListener('click', shut);
    foot.appendChild(surprise);
    foot.appendChild(undo);
    foot.appendChild(ok);
    panel.appendChild(foot);

    parts = { base: base.buttons, colors: colors.buttons, picker: picker, desks: desks.buttons, corners: corners.buttons,
      chromes: chromes.buttons, link: link, undo: undo };
  }

  function hsl(h: number, s: number, l: number) {
    function f(n: number) { var k = (n + h / 30) % 12; return l - s * Math.min(l, 1 - l) * Math.max(-1, Math.min(k - 3, 9 - k, 1)); }
    return '#' + [f(0), f(8), f(4)].map(function (v) { return ('0' + Math.round(v * 255).toString(16)).slice(-2); }).join('');
  }

  function open(from?: Element | null) {
    if (offerBox) dismissOffer();
    opener = (from || document.activeElement) as HTMLElement | null;
    if (panel && panel.isConnected) { panel.querySelector('button')!.focus(); return; }
    before = { code: own.saved(), skin: skins.get() };
    spec = own.parse(own.saved())!;
    if (!panel) build();
    parts.undo.textContent = before.skin === 'own' ? 'Undo' : 'Back to ' + NAMES[before.skin];
    document.body.appendChild(panel!);
    wear();
    sound('open');
    var first = panel!.querySelector<HTMLElement>('.shim-own-option[aria-pressed="true"]') || panel!.querySelector('button');
    first!.focus();
  }
  function shut() {
    if (!panel || !panel.isConnected) return;
    panel.remove();
    sound('close');
    if (opener && opener.isConnected && opener.focus) opener.focus();
  }

  // ---- a shared skin (?skin=): boot.js has put it on; keep it, change it, or go back ----
  function dismissOffer() {
    if (offerBox) offerBox.remove();
    offerBox = null;
    try { history.replaceState(history.state, '', location.href.replace(/([?&])skin=[^&#]*&?/, '$1').replace(/[?&](#|$)/, '$1')); } catch (e) { /* stays */ }
  }
  function offer() {
    var o = own.offer();
    if (!o || offerBox) return;
    offerBox = el('div', 'shim-own-offer');
    offerBox.setAttribute('role', 'status');
    offerBox.appendChild(el('strong', null, 'You are wearing a skin a friend made.'));
    offerBox.appendChild(el('span', null, ' Every page and app now looks like this.'));
    var row_ = el('div', 'shim-own-foot');
    function button(text: string, cls: string, fn: () => void) {
      var b = el('button', 'shim-own-btn' + (cls ? ' ' + cls : ''), text);
      b.type = 'button';
      b.addEventListener('click', fn);
      row_.appendChild(b);
    }
    button('Keep it', 'primary', function () { dismissOffer(); sound('saved'); });
    button('Change it', '', function () { dismissOffer(); open(); });
    button(o.skin === 'own' ? 'Back to mine' : 'Back to ' + NAMES[o.skin], '', function () {
      own.wear(o!.code);
      skins.own.setSkin(o!.skin);
      dismissOffer();
    });
    offerBox.appendChild(row_);
    document.body.appendChild(offerBox);
  }

  // A change made in another page (its own panel, or a shared link opened there) shows here too.
  window.addEventListener('storage', function (e) {
    if (e.key !== 'oldjava.ownSkin' || !panel || !panel.isConnected || !own.parse(e.newValue)) return;
    spec = own.parse(e.newValue)!;
    show();
  });

  window.oldjavaSkinMaker = { open: open, offer: offer, close: shut, isOpen: function () { return !!(panel && panel.isConnected); } };
})();

export {};   // a module: built on its own into skinmaker.js (vite/pages.ts)
