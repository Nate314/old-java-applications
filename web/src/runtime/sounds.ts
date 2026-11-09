import { redrawBar } from './install';
import { ctx } from './midi';

declare global {
  interface OldjavaSound { on(): boolean; set(on: boolean): void }
}

/** A skin's voice for the site sounds. */
interface Voice { wave: OscillatorType; root: number; step: number; len: number; hold: number; vol: number; bell?: boolean; lowpass?: number; blip?: boolean }
/** Semitones above the voice's root, or one of the two non-note sounds. */
type Tune = (number | 'thump' | 'power')[];

let sfxOut: GainNode | null;
let noiseBuffer: AudioBuffer | null;
let tapPending: boolean;

// ---- site sounds (window.oldjavaSound) ----
// The site's own controls and moments (not the apps, which keep their own sound) get short synthesized
// sounds, in a voice that follows the skin: soft glassy bells in Modern, a PC-speaker beep in Classic,
// chiptune blips in Arcade, where a new app powers on with a tube thump and a rising whine. Every tune is a
// list of semitones above the voice's root note (the last note is held longer); 'thump' and 'power' are the
// two non-note sounds.
// No file is downloaded. Sound plays only after the visitor has clicked or pressed a key on the page.
const SOUND_KEY = 'oldjava.sound';

const VOICES: Record<string, Voice> = {
  modern: { wave: 'sine', root: 523.25, step: 0.075, len: 0.32, hold: 1.8, vol: 0.2, bell: true },
  classic: { wave: 'square', root: 440, step: 0.1, len: 0.085, hold: 2, vol: 0.045, lowpass: 2200 },
  arcade: { wave: 'square', root: 659.25, step: 0.055, len: 0.07, hold: 4, vol: 0.055, blip: true }
};

const TUNES: Record<string, Tune> = {
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
const ARCADE_TUNES: Record<string, Tune> = { start: ['power', 7, 12], stamp: ['thump', 7, 12] };

export function soundOn() {
  try { return localStorage.getItem(SOUND_KEY) !== 'off'; } catch (e) { return true; }
}

function tone(a: AudioContext, v: Voice, semi: number, at: number, len: number, vol: number) {
  var f = v.root * Math.pow(2, semi / 12);
  var gain = a.createGain();
  gain.gain.setValueAtTime(0.0001, at);
  gain.gain.exponentialRampToValueAtTime(vol, at + 0.004);
  if (v.bell) gain.gain.exponentialRampToValueAtTime(0.0001, at + len);
  else {
    gain.gain.setValueAtTime(vol, at + len - 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, at + len);
  }
  var sink: AudioNode = gain;
  if (v.lowpass) {
    var lp = a.createBiquadFilter();
    lp.frequency.value = v.lowpass;
    gain.connect(lp);
    sink = lp;
  }
  sink.connect(sfxOut!);
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
function thump(a: AudioContext, at: number, vol: number) {
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
  n.connect(lp).connect(g).connect(sfxOut!);
  n.start(at);
  var o = a.createOscillator();
  o.frequency.setValueAtTime(140, at);
  o.frequency.exponentialRampToValueAtTime(45, at + 0.12);
  var og = a.createGain();
  og.gain.setValueAtTime(vol * 0.9, at);
  og.gain.exponentialRampToValueAtTime(0.0001, at + 0.13);
  o.connect(og).connect(sfxOut!);
  o.start(at);
  o.stop(at + 0.15);
}

// The tube warming up, timed with the Arcade skin's power-on animation (shim.css, arcade-on).
function power(a: AudioContext, at: number) {
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
  o.connect(lp).connect(g).connect(sfxOut!);
  o.start(at);
  o.stop(at + 0.32);
  return 0.34;   // the notes after it wait for the line to open up
}

export function playSound(name: string) {
  if (name !== 'tap') tapPending = false;   // the click that caused this needs no tap of its own
  if (!soundOn() || document.documentElement.hasAttribute('data-shot')) return;
  if (navigator.userActivation && !navigator.userActivation.hasBeenActive) return;   // it could not start yet
  var skinName = document.documentElement.getAttribute('data-skin');
  var v = VOICES[skinName as string] || VOICES.modern;
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
    if (semi === 'thump') { thump(a!, at, 0.3); at += 0.07; return; }
    if (semi === 'power') { at += power(a!, at); return; }
    var last = i === tune.length - 1;
    tone(a!, v, semi, at, v.len * (name === 'tap' ? 0.5 : last ? v.hold : 1), v.vol * quiet);
    at += v.step;
  });
}

export function setSoundOn(on: boolean) {
  try { localStorage.setItem(SOUND_KEY, on ? 'on' : 'off'); } catch (e) { /* this page only */ }
  redrawBar();
  if (on) playSound('open');
}

export function init() {
  TUNES.photo = ['thump', 24];   // the photo booth's shutter: a knock and a high click
  sfxOut = null;   // the site sounds' master gain, on the same AudioContext as the app voices
  noiseBuffer = null;
  tapPending = false;
  window.addEventListener('storage', function (e) { if (e.key === SOUND_KEY || e.key === null) redrawBar(); });
  window.oldjavaSound = { play: playSound, on: soundOn, set: setSoundOn };
  // Any click on a site control (not in the app's windows or the controller, whose keys belong to the app)
  // taps, unless what it did made a sound of its own. data-sound="none" keeps a control silent.
  document.addEventListener('click', function (e) {
    var t = e.target && (e.target as Element).closest && (e.target as Element).closest('button, a[href], summary');
    if (!t || t.closest('#desktop, .shim-pad, [data-sound="none"]')) return;
    tapPending = true;
    setTimeout(function () {
      if (tapPending) playSound('tap');
      tapPending = false;
    }, 0);
  }, true);
}
