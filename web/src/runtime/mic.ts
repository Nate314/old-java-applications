import { audio, ctx } from './midi';

interface MicState {
  state: string;
  chunks: Float32Array[];
  queued: number;
  out: Float32Array;
  rate: number;
  stream?: MediaStream;
  source?: MediaStreamAudioSourceNode;
  node?: ScriptProcessorNode;
}

interface ShimMic {
  open(): void;
  state(): string;
  rate(): number;
  queued(): number;
  take(max: number): number;
  sample(i: number): number;
  close(): void;
}

declare global {
  interface Window { __shimMic: ShimMic }
}

let mic: MicState | null;



export function init() {
  // ---- javax.sound.sampled microphone (window.__shimMic) ----
  // The shim's TargetDataLine asks for the microphone here and drains its samples (floats from -1 to 1) from a
  // queue that keeps the last two seconds. Echo cancellation stays on (it keeps the app's own notes out); noise
  // suppression is off, since it can swallow a held note or a whistle.
  mic = null;
  window.__shimMic = {
    open: function () {
      if (mic) return;
      var m: MicState = mic = { state: 'asking', chunks: [], queued: 0, out: new Float32Array(0), rate: 44100 };
      var a = ctx() as AudioContext;
      var md = navigator.mediaDevices;
      if (!a || !md || !md.getUserMedia || !a.createScriptProcessor) { m.state = 'none'; return; }
      md.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: false, autoGainControl: true } }).then(function (stream) {
        if (mic !== m) { stream.getTracks().forEach(function (t) { t.stop(); }); return; }
        m.stream = stream;
        m.source = a.createMediaStreamSource(stream);
        m.node = a.createScriptProcessor(2048, 1, 1);
        m.node.onaudioprocess = function (e) {
          var data = new Float32Array(e.inputBuffer.getChannelData(0));
          m.chunks.push(data);
          m.queued += data.length;
          while (m.queued > a.sampleRate * 2) m.queued -= m.chunks.shift()!.length;
        };
        // A script node only runs while connected to the output; it writes silence there.
        m.source.connect(m.node);
        m.node.connect(a.destination);
        m.rate = a.sampleRate;
        if (a.state === 'suspended') a.resume();
        m.state = 'open';
      }, function (err) {
        if (mic === m) m.state = err && (err.name === 'NotAllowedError' || err.name === 'SecurityError') ? 'denied' : 'none';
      });
    },
    state: function () { return mic ? mic.state : 'none'; },
    rate: function () { return mic ? mic.rate : 44100; },
    queued: function () { return mic ? mic.queued : 0; },
    take: function (max) {
      var m = mic;
      if (!m || !m.queued) return 0;
      var n = Math.min(max, m.queued);
      if (m.out.length < n) m.out = new Float32Array(n);
      for (var i = 0; i < n;) {
        var chunk = m.chunks[0];
        var count = Math.min(chunk.length, n - i);
        m.out.set(chunk.subarray(0, count), i);
        i += count;
        if (count === chunk.length) m.chunks.shift(); else m.chunks[0] = chunk.subarray(count);
      }
      m.queued -= n;
      return n;
    },
    sample: function (i) { return mic ? mic.out[i] : 0; },
    close: function () {
      var m = mic;
      mic = null;
      if (!m) return;
      if (m.node) { m.node.onaudioprocess = null; m.node.disconnect(); }
      if (m.source) m.source.disconnect();
      if (m.stream) m.stream.getTracks().forEach(function (t) { t.stop(); });
    }
  };
  // Browsers only start audio after a user gesture.
  ['pointerdown', 'keydown'].forEach(function (type) {
    document.addEventListener(type, function () { if (audio && audio.state === 'suspended') audio.resume(); }, true);
  });
}
