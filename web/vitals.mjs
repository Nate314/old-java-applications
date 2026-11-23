// Core Web Vitals of a built site on a throttled phone, in headless Chrome over the DevTools protocol, with Node's
// built-ins only (no npm packages). Lane E and lane D builders used to copy a scratch script for this every round.
// It serves the site with serve.ps1 (the headers visitors get) and loads each page cold (a fresh browser context: no
// HTTP cache, no service worker, no storage) on a 412x823 phone at 2x, Slow 4G (150 ms, 1.6 Mbit/s down, 750 kbit/s
// up, the page and its service worker both) and 4x CPU. Per load it records FCP, LCP (and its element), CLS, total
// blocking time from long tasks, the page's bytes and requests over the wire and, with -Tap <selector>, the INP of
// one real mouse press at that element's centre. Each page loads -Runs times (3) and the median is printed.
// Paired mode, -Before <site>: the same pages on a before build (served on -Port + 1) and the after build (-Site, on
// -Port), one load of each in turn (ABBA order, so a machine getting slower over the run counts against neither),
// printed as before, after and the change. Each server gets one unmeasured load of each page first: serve.ps1's first
// answers (PowerShell warming up) are slow enough to swing FCP by a second. Sound is off (--mute-audio, __shimSynth.noteOn stubbed, new AudioContexts
// suspended), the clipboard and alert/confirm/prompt are stubbed, Chrome's profile lives under -ProfileRoot and is
// deleted afterwards, and the servers and Chrome are stopped by PID. Exits 1 when a page does not load.
// Build first (mvn -f web/pom.xml package), then from the repo root:
//   node web/vitals.mjs -Port 8275 -DebugPort 9475 -ProfileRoot C:\some\scratch -Pages index.html,Minesweeper/index.html   (no defaults for -Port, -DebugPort, -ProfileRoot)
//   node web/vitals.mjs -Before C:\some\before\site -Site web/target/site -Pages story.html -Runs 5 ...
//   node web/vitals.mjs -Pages index.html -Tap "#gallery-filter" ...
//   node web/vitals.mjs -Pages index.html -Budget lcp=1500,cls=0.1 ...   (exit 1 when a median is over its number)
import { spawn, execSync } from 'node:child_process';
import { existsSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const opt = { Site: join(here, 'target/site'), Before: '', Pages: 'index.html', Runs: '3', Tap: '', Budget: '', Wait: '5000',
  Port: '', DebugPort: '', ProfileRoot: '',
  Chrome: join(process.env.ProgramFiles || 'C:\\Program Files', 'Google\\Chrome\\Application\\chrome.exe') };
for (let i = 2; i < process.argv.length; i += 2) {
  const k = process.argv[i].replace(/^-+/, '');
  if (!(k in opt) || process.argv[i + 1] === undefined) { console.error('Usage: node web/vitals.mjs [-Site dir] [-Before dir] [-Pages a.html,b/index.html] [-Runs n] [-Tap selector] [-Budget lcp=ms,cls=n,...] [-Wait ms] -Port n -DebugPort n -ProfileRoot dir [-Chrome exe]'); process.exit(2); }
  opt[k] = process.argv[i + 1];
}
// No defaults: two runs on one machine must never share a port or a Chrome profile folder.
for (const k of ['Port', 'DebugPort', 'ProfileRoot']) if (!opt[k]) { console.error('Pass -' + k + ': -Port, -DebugPort and -ProfileRoot have no defaults.'); process.exit(2); }
const sites = (opt.Before ? [opt.Before, opt.Site] : [opt.Site]).map(d => resolve(d));
for (const d of sites) if (!existsSync(join(d, 'index.html'))) { console.error('No built site in ' + d + '. Run: mvn -f web/pom.xml package'); process.exit(2); }
const PAGES = opt.Pages.split(',').map(p => p.trim().replace(/^\//, '')).filter(Boolean), RUNS = Math.max(1, +opt.Runs);
const PORT = +opt.Port, DEBUG = +opt.DebugPort, base = i => 'http://localhost:' + (PORT + sites.length - 1 - i) + '/';
mkdirSync(opt.ProfileRoot, { recursive: true });
// Its 8.3 short form, with backslashes: a long or forward-slash profile path makes Chrome's CacheStorage writes fail.
const root = execSync('cmd /c for %I in ("' + resolve(opt.ProfileRoot) + '") do @echo %~sI', { encoding: 'utf8' }).trim();
const work = join(root, 'vitals-' + PORT + '-' + Math.random().toString(36).slice(2, 8));
const sleep = ms => new Promise(r => setTimeout(r, ms));
const kill = p => { try { execSync('taskkill /PID ' + p.pid + ' /T /F', { stdio: 'ignore' }); } catch { } };

const servers = [];
async function serve(i) {
  const url = base(i);
  servers.push(spawn('pwsh', ['-NoProfile', '-File', join(here, 'serve.ps1'), '-Port', url.split(':')[2].replace('/', ''), '-Root', sites[i]], { stdio: 'ignore' }));
  for (let t = 0; t < 75; t++) { try { if ((await fetch(url + 'index.html')).ok) return; } catch { } await sleep(200); }
  throw new Error('serve.ps1 did not answer at ' + url);
}

// Observers from the first byte, plus the silent and clipboard stubs every page check in web/ uses.
const OBSERVE = `(function () {
  var v = window.__v = { fcp: 0, lcp: 0, lcpEl: '', cls: 0, tbt: 0, inp: 0, tapAt: 0 };
  function on(type, f, o) { try { new PerformanceObserver(function (l) { l.getEntries().forEach(f); }).observe(Object.assign({ type: type, buffered: true }, o)); } catch (e) {} }
  on('paint', function (e) { if (e.name === 'first-contentful-paint') v.fcp = e.startTime; });
  on('largest-contentful-paint', function (e) { var el = e.element; v.lcp = e.startTime;
    v.lcpEl = el ? el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + (typeof el.className === 'string' && el.className ? '.' + el.className.trim().split(/\\s+/).join('.') : '') : (e.url || '').split('/').pop(); });
  on('layout-shift', function (e) { if (!e.hadRecentInput) v.cls += e.value; });
  on('longtask', function (e) { if (!v.tapAt || e.startTime < v.tapAt) v.tbt += Math.max(0, e.duration - 50); });
  on('event', function (e) { if (e.interactionId && e.duration > v.inp) v.inp = e.duration; }, { durationThreshold: 16 });
  var s; Object.defineProperty(window, '__shimSynth', { configurable: true, get: function () { return s; }, set: function (x) { if (x) x.noteOn = function () {}; s = x; } });
  ['AudioContext', 'webkitAudioContext'].forEach(function (n) { var A = window[n]; if (!A) return;
    window[n] = function (o) { var c = new A(o); c.suspend(); c.resume = function () { return Promise.resolve(); }; return c; }; window[n].prototype = A.prototype; });
  var ok = function () { return Promise.resolve(); };
  try { Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: ok, write: ok, readText: function () { return Promise.resolve(''); }, read: function () { return Promise.resolve([]); } } }); } catch (e) {}
  document.execCommand = function () { return false; };
  ['alert', 'confirm', 'prompt'].forEach(function (n) { window[n] = function () { return null; }; });
})();`;
const NET = { offline: false, latency: 150, downloadThroughput: 1.6e6 / 8, uploadThroughput: 750e3 / 8 };

let chrome, ws, failed = 0;
const median = xs => { const s = [...xs].sort((a, b) => a - b), m = s.length >> 1; return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2; };
console.assert(median([3, 1, 2]) === 2 && median([4, 1, 3, 2]) === 2.5, 'median');
// A load that finished with under half the requests the page usually makes (one round 75 home load: 4 of ~58) measured
// only part of the page, so it is dropped and loaded again. usual = median of the site's loads, warm-up included.
const short = (req, usual) => req < usual / 2;
console.assert(short(4, median([58, 4, 57])) && !short(48, median([58, 57, 4])) && !short(58, median([4, 58])), 'short');
const METRICS = [['fcp', 'FCP ms'], ['lcp', 'LCP ms'], ['cls', 'CLS'], ['tbt', 'TBT ms'], ['kb', 'KB'], ['req', 'requests']].concat(opt.Tap ? [['inp', 'INP ms']] : []);
const fmt = (k, x) => k === 'cls' ? x.toFixed(3) : String(Math.round(x));
// -Budget: the highest median each named metric may have on every page (the after build's, in paired mode), so a
// regression like round 76's home LCP (812 -> 2740 ms) fails the run instead of being read past.
const BUDGET = Object.fromEntries(opt.Budget.split(',').filter(Boolean).map(b => b.split('=').map(x => x.trim().toLowerCase())));
for (const [k, v] of Object.entries(BUDGET)) if (!METRICS.some(m => m[0] === k) || !(v !== '' && isFinite(v))) { console.error('Bad -Budget entry ' + k + '=' + v + ' (metrics: ' + METRICS.map(m => m[0]).join(', ') + ')'); process.exit(2); }
const overBudget = (k, median) => k in BUDGET && !(median <= +BUDGET[k]);   // a page with no runs (NaN) is over too

try {
  mkdirSync(work, { recursive: true });
  chrome = spawn(opt.Chrome, ['--headless=new', '--mute-audio', '--no-first-run', '--no-default-browser-check', '--hide-scrollbars',
    '--remote-debugging-port=' + DEBUG, '--user-data-dir=' + join(work, 'p'), 'about:blank'], { stdio: 'ignore' });
  let ver;
  for (let i = 0; i < 75 && !ver; i++) { await sleep(200); try { ver = await (await fetch('http://127.0.0.1:' + DEBUG + '/json/version')).json(); } catch { } }
  if (!ver) throw new Error("Chrome's DevTools port " + DEBUG + ' did not answer');
  ws = new WebSocket(ver.webSocketDebuggerUrl);
  await new Promise((ok, no) => { ws.addEventListener('open', ok); ws.addEventListener('error', no); });
  let nid = 0;
  const waiting = new Map(), listeners = [];
  ws.addEventListener('message', m => {
    const msg = JSON.parse(m.data);
    if (msg.id && waiting.has(msg.id)) { const w = waiting.get(msg.id); waiting.delete(msg.id); msg.error ? w.no(new Error(msg.error.message)) : w.ok(msg.result); }
    else listeners.forEach(f => f(msg));
  });
  const send = (method, params = {}, sessionId) => new Promise((ok, no) => {
    const id = ++nid;
    waiting.set(id, { ok, no });
    setTimeout(() => { if (waiting.delete(id)) no(new Error('timeout: ' + method)); }, 90000);
    ws.send(JSON.stringify({ id, method, params, sessionId }));
  });
  // Every target that starts (the page, its service worker) gets the slow network before it runs.
  listeners.push(msg => {
    if (msg.method !== 'Target.attachedToTarget') return;
    const s = msg.params.sessionId;
    (async () => { await send('Network.enable', {}, s); await send('Network.emulateNetworkConditions', NET, s); })().catch(() => { })
      .finally(() => send('Runtime.runIfWaitingForDebugger', {}, s).catch(() => { }));
  });
  await send('Target.setAutoAttach', { autoAttach: true, waitForDebuggerOnStart: true, flatten: true });

  // One cold load of path; returns its numbers, or throws.
  async function load(url) {
    const { browserContextId } = await send('Target.createBrowserContext', {});
    try {
      const { targetId } = await send('Target.createTarget', { url: 'about:blank', browserContextId });
      const { sessionId: S } = await send('Target.attachToTarget', { targetId, flatten: true });
      const js = async e => { const r = await send('Runtime.evaluate', { expression: e, awaitPromise: true, returnByValue: true }, S); if (r.exceptionDetails) throw new Error(r.exceptionDetails.text); return r.result.value; };
      for (const m of ['Page.enable', 'Network.enable']) await send(m, {}, S);
      await send('Network.emulateNetworkConditions', NET, S);
      await send('Emulation.setCPUThrottlingRate', { rate: 4 }, S);
      await send('Emulation.setDeviceMetricsOverride', { width: 412, height: 823, deviceScaleFactor: 2, mobile: true }, S);
      await send('Page.addScriptToEvaluateOnNewDocument', { source: OBSERVE }, S);
      let bytes = 0, req = 0, loaded;
      const got = new Promise(r => { loaded = r; });
      const mine = msg => {
        if (msg.sessionId !== S) return;
        if (msg.method === 'Network.loadingFinished') { bytes += msg.params.encodedDataLength; req++; }
        if (msg.method === 'Page.loadEventFired') loaded();
      };
      listeners.push(mine);
      try {
        await send('Page.navigate', { url }, S);
        if (await Promise.race([got.then(() => true), sleep(90000).then(() => false)]) === false) throw new Error('no load event after 90 s');
        await sleep(+opt.Wait);   // late requests and paints
        let tapped = '';
        if (opt.Tap) {
          const box = await js(`(function (e) { if (!e) return null; e.scrollIntoView({ block: 'center' }); var r = e.getBoundingClientRect(); return { x: r.x + r.width / 2, y: r.y + r.height / 2 }; })(document.querySelector(${JSON.stringify(opt.Tap)}))`);
          if (!box) tapped = 'no ' + opt.Tap;
          else {
            await sleep(500);
            await js('__v.tapAt = performance.now()');
            for (const type of ['mousePressed', 'mouseReleased']) await send('Input.dispatchMouseEvent', { type, x: box.x, y: box.y, button: 'left', clickCount: 1 }, S);
            await sleep(3000);
          }
        }
        const v = JSON.parse(await js('JSON.stringify(__v)'));
        return { ...v, kb: bytes / 1024, req, tapped };
      } finally { listeners.splice(listeners.indexOf(mine), 1); }
    } finally { await send('Target.disposeBrowserContext', { browserContextId }).catch(() => { }); }
  }

  for (let i = 0; i < sites.length; i++) await serve(i);
  const t0 = Date.now();
  console.log('Phone 412x823 @2x, Slow 4G, 4x CPU, cold; ' + RUNS + ' run(s) per page' + (opt.Before ? ', before = ' + sites[0] + ', after = ' + sites[1] : ', ' + sites[0]));
  for (const path of PAGES) {
    const runs = sites.map(() => []), warm = [];
    for (let i = 0; i < sites.length; i++) warm[i] = await load(base(i) + path).catch(() => null);   // warm-up, not counted
    const name = (i, n) => (opt.Before ? (i ? 'after ' : 'before') + ' ' : '') + path + ' #' + n;
    const measure = async (i, n) => {
      try {
        const v = await load(base(i) + path);
        runs[i].push({ ...v, n });
        console.log('  ' + name(i, n) + ': ' + METRICS.map(([k, n]) => n + ' ' + fmt(k, v[k])).join(', ') + ', LCP ' + v.lcpEl + (v.tapped ? ' (' + v.tapped + ')' : ''));
      } catch (e) { failed++; console.log('  FAIL ' + path + ' #' + n + ': ' + e.message); }
    };
    for (let r = 0; r < RUNS; r++) {
      const order = sites.map((_, i) => i);
      if (r % 2) order.reverse();   // ABBA: before first, then after first
      for (const i of order) await measure(i, r + 1);
    }
    for (let i = 0; i < sites.length; i++) {
      const usual = median(runs[i].concat(warm[i] || []).map(v => v.req));
      for (let j = 0, n = RUNS; j < runs[i].length; j++) {
        if (!short(runs[i][j].req, usual)) continue;
        console.log('  dropped ' + name(i, runs[i][j].n) + ': ' + runs[i][j].req + ' requests, usually ' + Math.round(usual));
        runs[i].splice(j--, 1);
        if (n < 2 * RUNS) await measure(i, ++n);   // at most RUNS reloads per site, so a page that keeps loading short still ends
      }
    }
    console.log(path + ' (median)');
    for (const [k, n] of METRICS) {
      const m = runs.map(rs => rs.length ? median(rs.map(v => v[k])) : NaN);
      if (overBudget(k, m[m.length - 1])) { failed++; console.log('  OVER BUDGET ' + n + ' ' + fmt(k, m[m.length - 1]) + ' > ' + BUDGET[k]); }
      console.log('  ' + n.padEnd(9) + (opt.Before ? fmt(k, m[0]).padStart(8) + ' -> ' + fmt(k, m[1]).padStart(8) + '  (' + (m[1] >= m[0] ? '+' : '') + fmt(k, m[1] - m[0]) + (m[0] ? ', ' + (m[1] >= m[0] ? '+' : '') + Math.round((m[1] - m[0]) / m[0] * 100) + '%' : '') + ')' : fmt(k, m[0]).padStart(8)));
    }
  }
  console.log('Done in ' + Math.round((Date.now() - t0) / 1000) + ' s');
} catch (e) {
  failed++;
  console.log('FAIL vitals ran: ' + e.message);
} finally {
  if (ws) ws.close();
  if (chrome) kill(chrome);
  servers.forEach(kill);
  // Chrome can take a few seconds to let go of its profile after it is stopped.
  for (let t = 0; t < 30 && existsSync(work); t++) { await sleep(500); try { rmSync(work, { recursive: true, force: true }); } catch { } }
  if (existsSync(work)) console.warn('Could not remove ' + work);
}
process.exit(failed ? 1 : 0);
