import { real } from './slow-mo';

export let audio: AudioContext | null;
let appGain: GainNode | null;
let appMuted: boolean;
let hushed = false;

interface Voice { osc: OscillatorNode; gain: GainNode; ch: number }

interface ShimSynth {
  noteOn(ch: number, note: number, vel: number, program: number, cents: number): void;
  noteOff(ch: number, note: number): void;
  bend(ch: number, cents: number): void;
  allOff(ch: number): void;
}

declare global {
  interface Window { __shimSynth: ShimSynth }
}

const voices: Record<string, Voice> = {};

const WAVES: OscillatorType[] = ['triangle', 'square', 'sawtooth', 'sine'];

export function ctx() {
  if (!audio) {
    var AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    audio = new AC();
  }
  if (audio.state === 'suspended' && !hushed) audio.resume();
  return audio;
}

function release(v: Voice, at: number) {
  v.gain.gain.cancelScheduledValues(at);
  v.gain.gain.setValueAtTime(v.gain.gain.value, at);
  v.gain.gain.linearRampToValueAtTime(0, at + 0.15);
  v.osc.stop(at + 0.2);
}

export function appOut(a: AudioContext) {
  if (!appGain) {
    appGain = a.createGain();
    appGain.gain.value = appMuted || hushed ? 0 : 1;
    appGain.connect(a.destination);
  }
  return appGain;
}

/** Glides the app's voices to their level: silent while the computer plays or the page is held, else full. */
function level(secs: number) {
  if (!audio || !appGain) return;
  var g = appGain.gain, t = audio.currentTime;
  g.cancelScheduledValues(t);
  g.setValueAtTime(g.value, t);
  g.linearRampToValueAtTime(appMuted || hushed ? 0 : 1, t + secs);
}

export function setAppMuted(on: boolean) {
  appMuted = on;
  level(0.005);
}

/** A held page's sound waits with it (hidden-hold.ts): a note playing when the visitor switched away (whose note
 *  off sits in a held timer) droned on unseen. Now the app's voices fade out and the audio stops where it is, and
 *  once the page shows again it carries on from there and fades back in. An app whose clock is not held (`keep`
 *  false: a synth, a music drill) runs on while hidden, so its notes are let go instead: each fades out as if its
 *  key came up, notes asked for while hidden stay silent, and the page comes back to quiet, not to a stuck note. */
export function hush(on: boolean, keep: boolean) {
  if (on === hushed) return;
  hushed = on;
  var a = audio;
  if (!a) return;
  if (!on && a.state === 'suspended') a.resume();
  level(on ? 0.08 : 0.15);
  if (on && !keep) Object.keys(voices).forEach(function (k) { window.__shimSynth.noteOff(voices[k].ch, +k.split(':')[1]); });
  // a let-go voice needs its 0.2 s release to finish before the audio stops, or its tail would wait for the return
  if (on) real.setTimeout(function () { if (hushed) a!.suspend(); }, keep ? 100 : 250);
}

export function init() {
  // ---- javax.sound.midi voices ----
  audio = null;
  // The app's voices meet in one gain on their way out, so instant replay can record them without the site's
  // own sounds.
  appGain = null;
  appMuted = false;   // while the computer plays (watch)
  window.__shimSynth = {
    noteOn: function (ch, note, vel, program, cents) {
      var a = ctx();
      if (!a || hushed) return;   // nobody can hear it, and its note off may come late or never
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
      osc.connect(gain).connect(appOut(a));
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
}
