import { clipMode, embedded } from './skin';
import { played } from './passport';
import { WATCH } from './watch';
import { catalog, root } from './catalog';
import { isThen } from './then-and-now';
import { playSound } from './sounds';

type OldjavaFind = NonNullable<Window['oldjavaFind']>;

let findPromise: Promise<OldjavaFind> | null;
let findMac: boolean;
export let findOff: boolean;
let findHeld: string[] | null;
let findHeldTimer: number | ReturnType<typeof setInterval>;

function findScript() {
  if (!findPromise) findPromise = new Promise(function (resolve, reject) {
    import('./lazy/find').then(function () { resolve(window.oldjavaFind!); }, function () { findPromise = null; reject(new Error('find.js did not load')); });
  });
  return findPromise;
}

export function openFind(opener: Element | null) {
  findScript().then(function (find) { find.open(opener); }, function (e: Error) { console.warn(e.message); });
}

function findTyping(t: EventTarget | null) {
  return t && ((t as HTMLElement).isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test((t as HTMLElement).tagName));
}

function findType(text: string) {
  var input = document.querySelector('.shim-find-input') as HTMLInputElement | null;
  if (!input || !text) return;
  input.value += text;
  input.dispatchEvent(new Event('input'));
}

function findLetGo() {
  clearInterval(findHeldTimer);
  var keys = findHeld;
  findHeld = null;
  if (!keys || !keys.length) return;
  if (embedded && window.parent !== window) window.parent.postMessage({ oldjavaFindKeys: keys.join('') }, location.origin);
  else findType(keys.join(''));
}

export function init() {
  // ---- Find (find.js): Ctrl+K anywhere, / on the home page ----
  // One box for the whole site: apps and their Then and Watch versions, things to do here (skins, sound, share,
  // replay, the passport...), and every Java source file of every app. find.js is added the first time it opens.
  // In the home page's player the keys are passed up, so the box opens over the whole home page.
  window.oldjavaSite.catalog = catalog;
  window.oldjavaSite.isThen = isThen;
  window.oldjavaSite.played = function (id) { return !!played[id]; };
  window.oldjavaSite.watchable = Object.keys(WATCH);
  window.oldjavaSite.sound = function (name) { playSound(name); };
  findPromise = null;
  findMac = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);
  findOff = clipMode || document.documentElement.hasAttribute('data-shot') || document.documentElement.hasAttribute('data-saver');
  // The first Ctrl+K of a page loads find.js before the box can take the focus: keys typed in that moment are held
  // here and typed into the box once it has the focus, instead of reaching the app behind it (a title screen
  // would start the app on them). In the home page's player, or beside another app, the frame holds them until the
  // page around it has the box open (the frame loses the focus), then sends them up.
  findHeld = null; findHeldTimer = 0;
  if (!findOff) window.addEventListener('keydown', function (e) {
    var chord = (findMac ? e.metaKey : e.ctrlKey) && !e.altKey && !e.shiftKey && (e.key === 'k' || e.key === 'K');
    if (chord) {
      if (window.oldjavaFind && window.oldjavaFind.isOpen()) return;   // this one closes it
      clearInterval(findHeldTimer);
      findHeld = [];
      var since = Date.now();
      findHeldTimer = setInterval(function () {
        var there = embedded && window.parent !== window ? !document.hasFocus()
          : !!(window.oldjavaFind && window.oldjavaFind.isOpen());
        if (there || Date.now() - since > 3000) findLetGo();
      }, 20);
      return;
    }
    if (!findHeld) return;
    if (findTyping(e.target)) { findLetGo(); return; }   // the box has the focus: this key goes in by itself
    e.preventDefault();
    e.stopImmediatePropagation();
    if (e.key === 'Backspace') findHeld.pop();
    else if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) findHeld.push(e.key);
  }, true);
  if (!findOff) window.addEventListener('keyup', function (e) {
    if (findHeld && !findTyping(e.target)) e.stopImmediatePropagation();
  }, true);
  if (!findOff) window.addEventListener('message', function (e) {
    if (e.origin === location.origin && e.source !== window && e.data && typeof e.data.oldjavaFindKeys === 'string') findType(e.data.oldjavaFindKeys);
  });
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
    Array.prototype.forEach.call(document.querySelectorAll('[data-find]'), function (b: Element) {
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
}
