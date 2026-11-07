import { embedded } from './skin';
import { playSound } from './sounds';

// ---- one-row toolbar: what does not fit waits under More ----
// An app page's band holds many controls, and which ones show depends on the width, the skin (Yours adds Edit),
// the app (Controls, Then | Now) and the moment (Replay and Photo once it runs, a Clock chip while one counts).
// Rather than wrap to a second row that pushes the stage down, the band keeps to one row (two on narrow pages,
// where the app's name has a row of its own): the least needed controls step out, last of BAND_ORDER first, and
// a More button at the end lists them in a menu. The controls stay where they are in the band, only hidden
// (.shim-more-out), so their own code keeps working; a menu entry clicks the real one.
const BAND_ORDER = [   // kept the longest first
  '.shim-find-open', 'a[href="../index.html"]', '.shim-switch-open', '.shim-controls-open', '.shim-era',
  '.shim-next', '.shim-share-open', '.shim-source-open', '.shim-sound', '.shim-zoom', 'label',
  '.shim-dock-open', '.shim-own-edit', '.shim-replay', '.shim-pop', '.shim-photo-open'
];

function bandMore(bar: Element) {
  var more = document.createElement('button');
  more.type = 'button';
  more.className = 'shim-more';
  more.hidden = true;
  more.title = 'More: the controls that do not fit in this band';
  more.setAttribute('aria-haspopup', 'menu');
  more.setAttribute('aria-expanded', 'false');
  more.innerHTML = '<span aria-hidden="true">⋯</span><span class="shim-more-label"> More</span>';
  var menu = document.createElement('div');
  menu.className = 'shim-more-menu';
  menu.setAttribute('role', 'menu');
  menu.setAttribute('aria-label', 'More controls');
  menu.hidden = true;
  bar.appendChild(more);
  bar.appendChild(menu);
  // The switcher has no class of its own; it is the band's only plain "Switch app" button.
  Array.prototype.forEach.call(bar.children, function (c) {
    if (c.tagName === 'BUTTON' && c.textContent === 'Switch app') c.classList.add('shim-switch-open');
  });
  var narrow: { matches: boolean; addEventListener?: MediaQueryList['addEventListener'] } = (window as Partial<Window>).matchMedia ? matchMedia('(max-width: 560px)') : { matches: false };

  function items() {   // the band's controls that can step out, in the order they leave
    var list: Element[] = [];
    BAND_ORDER.forEach(function (sel) {
      Array.prototype.forEach.call(bar.children, function (c) {
        if (c.matches(sel) && list.indexOf(c) < 0) list.push(c);
      });
    });
    return list.reverse();
  }
  function rows() {   // how many rows the band's visible controls take
    var centers: number[] = [];
    Array.prototype.forEach.call(bar.children, function (c) {
      if (c === menu) return;
      var r = c.getBoundingClientRect();
      if (!r.width) return;
      var y = r.top + r.height / 2;
      if (!centers.some(function (v) { return Math.abs(v - y) < 10; })) centers.push(y);
    });
    return centers.length;
  }
  function fit() {
    var list = items(), out = [], allowed = narrow.matches ? 2 : 1;
    list.forEach(function (c) { c.classList.remove('shim-more-out'); });
    more.hidden = true;
    if (rows() > allowed) {
      more.hidden = false;
      for (var i = 0; i < list.length && rows() > allowed; i++) {
        if (!list[i].getBoundingClientRect().width) continue;   // not showing anyway
        list[i].classList.add('shim-more-out');
        out.push(list[i]);
      }
    }
    if (!out.length) more.hidden = true;
    more.setAttribute('aria-label', 'More: ' + out.length + (out.length === 1 ? ' control' : ' controls'));
    if (!menu.hidden) build();
  }
  var queued = false;
  function refit() {
    if (queued) return;
    queued = true;
    requestAnimationFrame(function () { queued = false; fit(); });
  }

  // The menu: the hidden controls in the band's order, each drawn as it is in the band. The skin picker and
  // Then | Now become choices; a toggle shows its state.
  function entry(role: string, html: string, title: string, run: () => void, sound?: string | null) {
    var b = document.createElement('button');
    b.type = 'button';
    b.className = 'shim-more-item';
    b.setAttribute('role', role);
    b.tabIndex = -1;
    b.innerHTML = '<span class="shim-more-what">' + html + '</span>' + (title ? '<small>' + escapeMore(title) + '</small>' : '');
    if (sound) b.setAttribute('data-sound', sound);
    b.addEventListener('click', function () { close(false); run(); });
    menu.appendChild(b);
    return b;
  }
  function heading(text: string) {
    var h = document.createElement('div');
    h.className = 'shim-more-head';
    h.setAttribute('role', 'presentation');
    h.textContent = text;
    menu.appendChild(h);
  }
  function build() {
    menu.textContent = '';
    Array.prototype.forEach.call(bar.querySelectorAll('.shim-more-out'), function (c) {
      if (c.classList.contains('shim-era')) {
        heading('Version');
        Array.prototype.forEach.call(c.querySelectorAll('a'), function (a) {
          entry('menuitemradio', escapeMore(a.textContent), a.title, function () { a.click(); })
            .setAttribute('aria-checked', String(a.hasAttribute('aria-current')));
        });
      } else if (c.tagName === 'LABEL') {
        var select = c.querySelector('select');
        if (!select) return;
        heading('Skin');
        Array.prototype.forEach.call(select.options, function (o) {
          entry('menuitemradio', '<i class="shim-more-swatch" data-swatch="' + o.value + '"></i>' + escapeMore(o.textContent), '', function () {
            if (select.value === o.value) return;
            select.value = o.value;
            select.dispatchEvent(new Event('change', { bubbles: true }));
          }).setAttribute('aria-checked', String(select.value === o.value));
        });
      } else {
        var face = c.cloneNode(true);
        Array.prototype.forEach.call(face.querySelectorAll('kbd'), function (k) { k.remove(); });
        var key = c.querySelector('kbd');
        var pressed = c.getAttribute('aria-pressed');
        var b = entry(pressed ? 'menuitemcheckbox' : 'menuitem', face.innerHTML, c.title, function () { c.click(); },
          c.getAttribute('data-sound'));
        if (pressed) b.setAttribute('aria-checked', pressed);
        if (key) {
          var k = document.createElement('kbd');
          k.textContent = key.textContent;
          b.appendChild(k);
        }
      }
    });
  }
  function entries() { return Array.prototype.slice.call(menu.querySelectorAll('.shim-more-item')); }
  function open(byKey: boolean) {
    build();
    menu.hidden = false;
    more.setAttribute('aria-expanded', 'true');
    // Under the More button, its right edges lined up, and no taller than the page below the band.
    var barBox = bar.getBoundingClientRect(), box = more.getBoundingClientRect();
    menu.style.right = Math.max(4, Math.round(barBox.right - box.right)) + 'px';
    menu.style.maxHeight = Math.max(160, Math.round(innerHeight - barBox.bottom - 12)) + 'px';
    playSound('open');
    var first = entries()[0];
    if (byKey && first) first.focus();
  }
  function close(refocus: boolean) {
    if (menu.hidden) return;
    menu.hidden = true;
    more.setAttribute('aria-expanded', 'false');
    if (refocus) more.focus();
  }
  more.setAttribute('data-sound', 'none');   // open plays its own
  more.addEventListener('click', function (e) {
    if (menu.hidden) open(e.detail === 0); else close(false);
  });
  more.addEventListener('keydown', function (e) {
    if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && menu.hidden) {
      e.preventDefault();
      open(true);
      if (e.key === 'ArrowUp') { var l = entries(); if (l.length) l[l.length - 1].focus(); }
    }
  });
  menu.addEventListener('keydown', function (e) {
    var l = entries(), i = l.indexOf(document.activeElement);
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      var n = l.length;
      if (n) l[((i < 0 ? (e.key === 'ArrowDown' ? -1 : 0) : i) + (e.key === 'ArrowDown' ? 1 : -1) + n) % n].focus();
    } else if (e.key === 'Home' || e.key === 'End') {
      e.preventDefault();
      if (l.length) l[e.key === 'Home' ? 0 : l.length - 1].focus();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      close(true);
    } else if (e.key === 'Tab') {
      close(false);
    }
  });
  document.addEventListener('pointerdown', function (e) {
    if (!menu.hidden && !more.contains(e.target as Node) && !menu.contains(e.target as Node)) close(false);
  }, true);
  addEventListener('blur', function () { close(false); });   // a click in an app iframe (Side by side) or another window

  fit();
  addEventListener('resize', refit);
  if (narrow.addEventListener) narrow.addEventListener('change', refit);
  if (window.ResizeObserver) {
    // Any control changing its width (Fit's zoom chip, Muted, Next's name arriving, a Clock chip, Replay
    // showing once the app runs) refits the band. A control that steps out reports width 0 once; the refit
    // that follows lands on the same result, so the band settles.
    var watch = new ResizeObserver(refit);
    Array.prototype.forEach.call(bar.children, function (c) { if (c !== menu) watch.observe(c); });
    new MutationObserver(function (changes) {
      changes.forEach(function (m) {
        Array.prototype.forEach.call(m.addedNodes, function (c) { if (c.nodeType === 1 && c !== menu) watch.observe(c); });
      });
      refit();
    }).observe(bar, { childList: true });
  }
  new MutationObserver(refit).observe(document.documentElement,
    { attributes: true, attributeFilter: ['data-skin', 'data-replay', 'data-photo', 'data-title', 'data-source', 'data-dock'] });
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(refit);
}

function escapeMore(s: string) {
  return String(s).replace(/[&<>"]/g, function (ch) { return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' } as Record<string, string>)[ch]; });
}

// What has an effect beyond the page (a running app with its sounds, saves and replay recording, Clock's bell
// and its open beat) waits until a prerendered page is opened; a page that is never opened leaves no trace.
export function whenShown(fn: () => void) {
  if (document.prerendering) document.addEventListener('prerenderingchange', function () { fn(); }, { once: true });
  else fn();
}

export function init() {
  // After the toolbar and the controls other parts of this file add to it at load (Find, Side by side, Edit).
  if (!embedded) document.addEventListener('DOMContentLoaded', function () {
    var bar = document.querySelector('.shim-toolbar');
    if (bar && document.body.hasAttribute('data-app')) bandMore(bar);
  });
}
