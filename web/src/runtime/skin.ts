import { playSound } from './sounds';

let skin: string;
let ownStyle: HTMLStyleElement;
let sharedSkin: string | undefined;
export let skinOffer: { code: string; skin: string; shared: string } | null;
export let embedded: boolean;
export let clipMode: boolean;

// A "Your skin" code taken apart (ownParse).
export interface OwnSkin { dark: boolean; color: string; desk: string; corners: number; chrome: string }

'use strict';

const SKIN_KEY = 'oldjava.skin';

export const SKINS = [['modern', 'Modern'], ['classic', 'Classic'], ['arcade', 'Arcade']];

function isSkin(v: string | null) { return SKINS.some(function (o) { return o[0] === v; }); }

// ---- Your skin: a fourth skin the visitor makes (skinmaker.js) ----
// Five choices, kept as a short code in oldjava.ownSkin, e.g. "l-7c3aed-aurora-1-gloss": light or dark, a color,
// a desk wallpaper, corners (square, soft, round) and window chrome (flat, glossy, glow). This part runs on every
// page, before first paint: it turns the code into the skin's CSS variables in a <style> of its own, which
// follows a change made in any other page (the storage event) and is copied into a popped-out window with the
// other styles. A page opened with ?skin=<code> (a shared skin) puts it on and saves it at once, so the home
// page's player wears it too; skinmaker.js then offers to keep it or go back.
const OWN_KEY = 'oldjava.ownSkin';

const OWN_DEFAULT = 'l-7c3aed-aurora-1-gloss';

const OWN_CODE = /^([ld])-([0-9a-f]{6})-(plain|fade|grid|dots|stripes|aurora)-([012])-(flat|gloss|glow)$/;

export function ownParse(code: string | null | undefined): OwnSkin | null {
  var m = OWN_CODE.exec(String(code || '').toLowerCase());
  return m && { dark: m[1] === 'd', color: '#' + m[2], desk: m[3], corners: +m[4], chrome: m[5] };
}

export function ownCode(s: OwnSkin) {
  return (s.dark ? 'd' : 'l') + '-' + s.color.slice(1) + '-' + s.desk + '-' + s.corners + '-' + s.chrome;
}

function rgbOf(hex: string) { return [1, 3, 5].map(function (i) { return parseInt(hex.substr(i, 2), 16); }); }

function hexOf(c: number[]) {
  return '#' + c.map(function (v) { return ('0' + Math.round(Math.max(0, Math.min(255, v))).toString(16)).slice(-2); }).join('');
}

export function mix(a: string, b: string, t: number) { var x = rgbOf(a), y = rgbOf(b); return hexOf(x.map(function (v, i) { return v + (y[i] - v) * t; })); }

function rgba(hex: string, alpha: number) { return 'rgba(' + rgbOf(hex).join(', ') + ', ' + alpha + ')'; }

function luma(hex: string) {
  var c = rgbOf(hex).map(function (v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}

function contrast(a: string, b: string) { var x = luma(a), y = luma(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }

function hueTurn(hex: string, deg: number) {   // the same color with its hue turned, for the wallpaper's second color
  var c = rgbOf(hex), k = Math.cos(deg * Math.PI / 180), s = Math.sin(deg * Math.PI / 180), q = 1 / 3, r = Math.sqrt(q);
  var m = [k + (1 - k) * q, q * (1 - k) - r * s, q * (1 - k) + r * s];   // rotation about the gray axis
  return hexOf([c[0] * m[0] + c[1] * m[1] + c[2] * m[2], c[0] * m[2] + c[1] * m[0] + c[2] * m[1], c[0] * m[1] + c[1] * m[2] + c[2] * m[0]]);
}

// The skin's CSS variables. The color becomes the accent, nudged darker or lighter until white text on it and
// the accent as text on the page both read, so any color the visitor picks gives a readable site.
export function ownCss(s: OwnSkin) {
  var white = '#ffffff', page = s.dark ? mix('#0b0d12', s.color, 0.1) : mix(white, s.color, 0.05);
  var a = s.color, i;
  for (i = 0; i < 40 && contrast(a, white) < (s.dark ? 3 : 4.5); i++) a = mix(a, '#000000', 0.06);
  for (i = 0; i < 40 && s.dark && contrast(a, page) < 3 && contrast(mix(a, white, 0.06), white) >= 3; i++) a = mix(a, white, 0.06);
  var a2 = hueTurn(a, 50);
  var frame = s.dark ? mix('#13151c', a, 0.07) : white;
  var text = s.dark ? '#e8eaf0' : '#1c2130';
  var line = s.dark ? mix('#2b2f3b', a, 0.3) : mix('#d5d9e2', a, 0.25);
  var d1 = s.dark ? mix('#07080c', a, 0.3) : mix(white, a, 0.2), d2 = s.dark ? mix('#07080c', a2, 0.24) : mix(white, a2, 0.26);
  var desk = ({
    plain: d1,
    fade: 'linear-gradient(135deg, ' + d1 + ', ' + d2 + ')',
    grid: 'linear-gradient(' + rgba(a, 0.14) + ' 1px, transparent 1px) 0 0 / 32px 32px, linear-gradient(90deg, ' +
      rgba(a, 0.14) + ' 1px, transparent 1px) 0 0 / 32px 32px, linear-gradient(160deg, ' + d1 + ', ' + d2 + ')',
    dots: 'radial-gradient(' + rgba(a, s.dark ? 0.4 : 0.28) + ' 1.5px, transparent 2.2px) 0 0 / 20px 20px, ' + d1,
    stripes: 'repeating-linear-gradient(135deg, ' + d1 + ' 0 22px, ' + mix(d1, d2, 0.7) + ' 22px 44px)',
    aurora: 'radial-gradient(ellipse at 15% 0%, ' + rgba(a, s.dark ? 0.55 : 0.35) + ', transparent 60%), radial-gradient(ellipse at 90% 100%, ' +
      rgba(a2, s.dark ? 0.5 : 0.35) + ', transparent 60%), ' + (s.dark ? '#07080c' : '#f5f6fa')
  } as Record<string, string>)[s.desk];
  var v: Record<string, string> = {
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

export function ownSaved() {
  try { return ownParse(localStorage.getItem(OWN_KEY)) ? localStorage.getItem(OWN_KEY)! : OWN_DEFAULT; } catch (e) { return OWN_DEFAULT; }
}

function showOwn(code: string) { ownStyle.textContent = ownCss(ownParse(code) || ownParse(OWN_DEFAULT)!); }

// Puts a skin code on: saved for every page, worn here at once (the storage event tells the others).
export function wearOwn(code: string) {
  if (!ownParse(code)) return;
  showOwn(code);
  try { localStorage.setItem(OWN_KEY, code); } catch (e) { /* this page only */ }
}

function showSkin(value: string) {
  document.documentElement.setAttribute('data-skin', value);
  var select = document.querySelector<HTMLSelectElement>('.shim-toolbar select');
  if (select) select.value = value;
}

export function setSkin(value: string) {
  if (!isSkin(value)) return;
  showSkin(value);
  try { localStorage.setItem(SKIN_KEY, value); } catch (e) { /* not remembered */ }
  playSound('skin');   // in the new skin's voice
}

export function init() {
  SKINS.push(['own', 'Yours']);   // the visitor's own skin, made in skinmaker.js (see "Your skin" below)
  skin = 'modern';
  try {
    skin = localStorage.getItem(SKIN_KEY) || 'modern';
  } catch (e) { /* storage blocked: use the default */ }
  if (!isSkin(skin)) skin = 'modern';
  document.documentElement.setAttribute('data-skin', skin);
  ownStyle = document.createElement('style');
  ownStyle.id = 'oldjava-own-skin';
  showOwn(ownSaved());
  document.head.appendChild(ownStyle);
  window.addEventListener('storage', function (e) { if (e.key === OWN_KEY && ownParse(e.newValue)) showOwn(e.newValue!); });
  // Keys pressed in the maker panel are its own: this listener comes before the app's and the title screen's
  // (they are added later), so they never see them. Escape closes the panel.
  ['keydown', 'keyup'].forEach(function (type) {
    document.addEventListener(type, function (e) {
      if (!(e.target as Element).closest || !(e.target as Element).closest('.shim-own, .shim-own-offer')) return;
      e.stopImmediatePropagation();
      if (type === 'keydown' && (e as KeyboardEvent).key === 'Escape' && window.oldjavaSkinMaker) window.oldjavaSkinMaker.close();
    }, true);
  });
  sharedSkin = (/[?&]skin=([^&#]*)/.exec(location.search) || [])[1];
  skinOffer = null;   // what a shared skin replaced: { code, skin }, for skinmaker.js's Undo
  if (sharedSkin && ownParse(sharedSkin) && !/[?&]embed(=|&|$)/.test(location.search) &&
      !(skin === 'own' && sharedSkin.toLowerCase() === ownSaved())) {
    skinOffer = { code: ownSaved(), skin: skin, shared: sharedSkin.toLowerCase() };
    wearOwn(skinOffer.shared);
    skin = 'own';
    document.documentElement.setAttribute('data-skin', 'own');
    try { localStorage.setItem(SKIN_KEY, 'own'); } catch (e) { /* this page only */ }
  }
  // Fires in every other same-origin page, including the player iframe on the home page.
  window.addEventListener('storage', function (e) {
    if (e.key === SKIN_KEY && isSkin(e.newValue)) showSkin(e.newValue!);
  });
  embedded = /[?&]embed(=|&|$)/.test(location.search);
  if (embedded) document.documentElement.setAttribute('data-embed', '');
  if (embedded && /[?&]shot(=|&|$)/.test(location.search)) document.documentElement.setAttribute('data-shot', '');
  // ?embed&clip is the preview mode web/clips.ps1 records the gallery's moving thumbnails in: the instant replay
  // recorder (startReplay) makes small, silent, fixed-size clips (CLIP_SIZE) that fill a gallery thumbnail.
  clipMode = embedded && /[?&]clip(=|&|$)/.test(location.search);
  // The home page's Arcade mode button switches skins through this.
  window.oldjavaSkin = { set: setSkin, get: function () { return document.documentElement.getAttribute('data-skin')!; } };
}
