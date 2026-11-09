import { embedded, mix, ownCode, ownCss, ownParse, ownSaved, setSkin, skinOffer, wearOwn } from './skin';
import { root } from './catalog';
import { findOff } from './find';

type OldjavaSkinMaker = NonNullable<Window['oldjavaSkinMaker']>;

declare global {
  interface OldjavaSkin {
    /** What skinmaker.js borrows from skin.ts. */
    own?: {
      parse: typeof ownParse; code: typeof ownCode; saved: typeof ownSaved; wear: typeof wearOwn; css: typeof ownCss; mix: typeof mix;
      offer(): typeof skinOffer;
      setSkin(v: string): void;
    };
    make?(opener?: Element | null): void;
  }
}

let makerPromise: Promise<OldjavaSkinMaker> | null;

function makerScript() {
  if (!makerPromise) makerPromise = new Promise(function (resolve, reject) {
    import('./lazy/skinmaker').then(function () { resolve(window.oldjavaSkinMaker!); }, function () { makerPromise = null; reject(new Error('skinmaker.js did not load')); });
  });
  return makerPromise;
}

export function openMaker(opener?: Element | null) {
  if (embedded && window.parent !== window) { window.parent.postMessage({ oldjavaSkinMaker: true }, location.origin); return; }
  makerScript().then(function (maker) { maker.open(opener); }, function (e: Error) { console.warn(e.message); });
}

export function init() {
  // ---- Your skin (skinmaker.js): the maker panel, added the first time it opens ----
  // Choosing Yours in the toolbar's skin picker opens it, as do the Edit button beside the picker (shown while
  // Yours is worn), Find's "Make your own skin" and any [data-skin-maker] button (the home page has one). The
  // CSS itself is made at the top of this file (ownCss), so every page wears the skin without this script.
  makerPromise = null;
  // What skinmaker.js borrows from this file.
  window.oldjavaSkin.own = {
    parse: ownParse, code: ownCode, saved: ownSaved, wear: wearOwn, css: ownCss, mix: mix,
    offer: function () { return skinOffer; },
    setSkin: function (v: string) { setSkin(v); }
  };
  window.oldjavaSkin.make = openMaker;
  window.addEventListener('message', function (e) {
    if (e.origin !== location.origin || !e.data || !e.data.oldjavaSkinMaker || root()) return;
    openMaker(document.getElementById('player-frame'));
  });
  if (!findOff) document.addEventListener('DOMContentLoaded', function () {
    Array.prototype.forEach.call(document.querySelectorAll('[data-skin-maker]'), function (b: Element) {
      b.addEventListener('click', function () { openMaker(b); });
    });
    if (skinOffer) makerScript().then(function (maker) { maker.offer(); }, function (e: Error) { console.warn(e.message); });
  });
}
