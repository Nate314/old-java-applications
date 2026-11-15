export let audio: AudioContext | null;
let appGain: GainNode | null;
let appMuted: boolean;

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
  if (audio.state === 'suspended') audio.resume();
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
    appGain.gain.value = appMuted ? 0 : 1;
    appGain.connect(a.destination);
  }
  return appGain;
}

export function setAppMuted(on: boolean) {
  appMuted = on;
  if (appGain) appGain.gain.value = on ? 0 : 1;
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
