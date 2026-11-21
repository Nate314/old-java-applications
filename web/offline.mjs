// An offline check of the built site in headless Chrome over the DevTools protocol, with Node's built-ins only (no
// npm packages). It serves the site with serve.ps1 (the same headers a visitor gets) and walks three flows:
//   1. first visit, then offline: home, story, play.html and an app page, visited once online, each load again with
//      no connection;
//   2. Save all for offline, then offline: several app pages, Then pages, and Find on the home page;
//   3. a second build arrives: a copy of the site under -ProfileRoot with one tiny change (boot.js renamed and marked
//      'B', every program marked 'B' and restamped), served in its place; once its service worker takes over, the
//      same pages offline must all run build B whole: B's runtime, B's program, only B's files.
// Offline means no network for the page AND the service worker (each gets Network.emulateNetworkConditions), with
// Chrome's HTTP cache cleared first, so answers can only come from the worker's cache. A page step fails on an
// uncaught error, a console error, a failed or 4xx/5xx request, the worker's "Offline" page, no window after Play or
// a one-color canvas. Not failures: a missing preview clip and Find's source lists (neither is saved offline), and in
// flow 1 a picture the page never showed online (a note).
// Sound is off (--mute-audio, __shimSynth.noteOn stubbed, new AudioContexts suspended), the clipboard and
// alert/confirm/prompt are stubbed, Chrome's profile and the copy live under -ProfileRoot and are deleted
// afterwards, and the server and Chrome are stopped by PID. Prints PASS or FAIL per step, exits 1 on any FAIL.
// Build first (mvn -f web/pom.xml package), then run from the repo root:
//   node web/offline.mjs -ProfileRoot C:\some\scratch
//   node web/offline.mjs -Site <site> -Port 8107 -DebugPort 9307 -ProfileRoot C:\some\scratch
import { spawn, execFileSync, execSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { appendFileSync, cpSync, existsSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const opt = { Site: join(here, 'target/site'), Port: '8101', DebugPort: '9302', ProfileRoot: tmpdir(),
  Chrome: join(process.env.ProgramFiles || 'C:\\Program Files', 'Google\\Chrome\\Application\\chrome.exe') };
for (let i = 2; i < process.argv.length; i += 2) {
  const k = process.argv[i].replace(/^-+/, '');
  if (!(k in opt) || process.argv[i + 1] === undefined) { console.error('Usage: node web/offline.mjs [-Site dir] [-Port n] [-DebugPort n] [-ProfileRoot dir] [-Chrome exe]'); process.exit(2); }
  opt[k] = process.argv[i + 1];
}
const site = resolve(opt.Site), PORT = +opt.Port, DEBUG = +opt.DebugPort;
if (!existsSync(join(site, 'index.html')) || !existsSync(join(site, 'sw.js'))) { console.error('No built site in ' + site + '. Run: mvn -f web/pom.xml package'); process.exit(2); }
mkdirSync(opt.ProfileRoot, { recursive: true });
// Its 8.3 short form, with backslashes: a long or forward-slash profile path makes Chrome's CacheStorage writes fail.
const root = execSync('cmd /c for %I in ("' + resolve(opt.ProfileRoot) + '") do @echo %~sI', { encoding: 'utf8' }).trim();
const name = 'offline-' + PORT + '-' + Math.random().toString(36).slice(2, 8);
const work = join(root, name);   // Chrome's profile goes here
const copy = join(resolve(opt.ProfileRoot), name, 'siteB');   // the same folder, by its long name: serve.ps1 answers 404 under an 8.3 root
const BASE = 'http://localhost:' + PORT + '/';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const total = Date.now();

// ---- the second build: a copy of the site with boot.js renamed (new content, so a new hashed name everywhere it is
// named, sw.js's file list too) and every program ending in a mark, restamped by vite/stamp-programs.mjs ----
function secondBuild(dst) {
  cpSync(site, dst, { recursive: true });
  const assets = join(dst, 'assets');
  const old = (/assets\/(boot-[A-Za-z0-9_-]{8}\.js)/.exec(readFileSync(join(dst, 'index.html'), 'utf8')) || [])[1];   // the one the pages name
  if (!old) throw new Error('index.html names no assets/boot-<hash>.js in ' + site);
  const body = readFileSync(join(assets, old), 'utf8') + "\n;window.__build = 'B';\n";
  const name = 'boot-' + createHash('sha256').update(body).digest('base64url').slice(0, 8) + '.js';
  writeFileSync(join(assets, old), body);
  renameSync(join(assets, old), join(assets, name));
  const walk = d => readdirSync(d, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(join(d, e.name)) : [join(d, e.name)]);
  for (const f of walk(dst)) {
    if (/[\\/](classes|then)\.js$/.test(f)) appendFileSync(f, "\n;(self.__programs = self.__programs || []).push('B');\n");
    else if (/\.(html|js|css|json|webmanifest)$/.test(f) && statSync(f).size < 4e6) {
      const text = readFileSync(f, 'utf8');
      if (text.includes(old)) writeFileSync(f, text.split(old).join(name));
    }
  }
  execFileSync(process.execPath, [join(here, 'vite/stamp-programs.mjs'), dst], { stdio: 'ignore' });
  return { dir: dst, boot: name };
}

// ---- server (serve.ps1) and Chrome ----
let server = null;
const kill = p => { try { execSync('taskkill /PID ' + p.pid + ' /T /F', { stdio: 'ignore' }); } catch { } };
async function serve(dir) {
  if (server) { kill(server); await sleep(800); }
  server = spawn('pwsh', ['-NoProfile', '-File', join(here, 'serve.ps1'), '-Port', String(PORT), '-Root', dir], { stdio: 'ignore' });
  for (let i = 0; i < 75; i++) { try { if ((await fetch(BASE + 'index.html')).ok) return; } catch { } await sleep(200); }
  throw new Error('serve.ps1 did not answer on port ' + PORT);
}

const GUARD = `(function () {
  var s; Object.defineProperty(window, '__shimSynth', { configurable: true, get: function () { return s; },
    set: function (v) { if (v) v.noteOn = function () {}; s = v; } });
  ['AudioContext', 'webkitAudioContext'].forEach(function (n) {
    var A = window[n]; if (!A) return;
    window[n] = function (o) { var c = new A(o); c.suspend(); c.resume = function () { return Promise.resolve(); }; return c; };
    window[n].prototype = A.prototype;
  });
  var ok = function () { return Promise.resolve(); };
  try { Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: ok, write: ok, readText: function () { return Promise.resolve(''); }, read: function () { return Promise.resolve([]); } } }); } catch (e) {}
  document.execCommand = function () { return false; };
  ['alert', 'confirm', 'prompt'].forEach(function (n) { window[n] = function () { console.error('offline check: page called ' + n + '()'); return null; }; });
})();`;

let chrome, ws, failed = 0, steps = 0;
function report(name, why, t0, note = '') {
  steps++;
  if (why) failed++;
  const line = (why ? 'FAIL ' : 'PASS ') + name.padEnd(44) + ((Date.now() - t0) / 1000).toFixed(1).padStart(5) + ' s' + (why || note ? '  ' + (why || note) : '');
  console.log(line);
  return !why;
}

try {
  mkdirSync(work, { recursive: true });
  const { dir: siteB, boot: bootB } = secondBuild(copy);
  await serve(site);
  chrome = spawn(opt.Chrome, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--mute-audio', '--no-first-run',
    '--remote-debugging-port=' + DEBUG, '--user-data-dir=' + join(work, 'p'), '--window-size=960,600', 'about:blank'], { stdio: 'ignore' });
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
    setTimeout(() => { if (waiting.delete(id)) no(new Error('timeout: ' + method)); }, 15000);
    ws.send(JSON.stringify({ id, method, params, sessionId }));
  });

  // Offline for the page and for every service worker of the site, as they start (a new build's worker too).
  let offline = false;
  const cond = () => ({ offline, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
  const swSessions = new Set();
  listeners.push(async msg => {
    if (msg.method !== 'Target.targetCreated') return;
    const t = msg.params.targetInfo;
    if (t.type !== 'service_worker' || !t.url.startsWith(BASE)) return;
    try {
      const { sessionId } = await send('Target.attachToTarget', { targetId: t.targetId, flatten: true });
      swSessions.add(sessionId);
      await send('Network.enable', {}, sessionId);
      await send('Network.emulateNetworkConditions', cond(), sessionId);
    } catch { }
  });
  await send('Target.setDiscoverTargets', { discover: true });
  const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
  const { sessionId: S } = await send('Target.attachToTarget', { targetId, flatten: true });
  for (const m of ['Page.enable', 'Runtime.enable', 'Network.enable', 'Log.enable']) await send(m, {}, S);
  await send('Page.addScriptToEvaluateOnNewDocument', { source: GUARD }, S);
  async function setOffline(on) {
    offline = on;
    if (on) await send('Network.clearBrowserCache', {}, S);   // days later: only the worker's cache is left
    await send('Network.emulateNetworkConditions', cond(), S);
    for (const s of swSessions) { try { await send('Network.emulateNetworkConditions', cond(), s); } catch { swSessions.delete(s); } }
  }

  let errors = [];
  const urls = {};
  listeners.push(msg => {
    if (msg.sessionId !== S) return;
    const p = msg.params;
    if (msg.method === 'Runtime.exceptionThrown') errors.push('uncaught: ' + (p.exceptionDetails.exception?.description || p.exceptionDetails.text));
    if (msg.method === 'Runtime.consoleAPICalled' && (p.type === 'error' || p.type === 'assert')) errors.push('console.error: ' + p.args.map(a => a.value ?? a.description).join(' '));
    if (msg.method === 'Log.entryAdded' && p.entry.level === 'error' && p.entry.source !== 'network') errors.push('log: ' + p.entry.text);
    if (msg.method === 'Network.requestWillBeSent') urls[p.requestId] = p.request.url;
    if (msg.method === 'Network.loadingFailed' && !p.canceled) errors.push('request failed: ' + p.errorText + ' ' + (urls[p.requestId] || '').replace(BASE, ''));
    if (msg.method === 'Network.responseReceived' && p.response.status >= 400) errors.push('HTTP ' + p.response.status + ' ' + p.response.url.replace(BASE, ''));
  });
  async function js(expr) {
    const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true }, S);
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
    return r.result.value;
  }
  async function until(expr, ms) { for (const end = Date.now() + ms; Date.now() < end; await sleep(200)) { try { if (await js(expr)) return true; } catch { } } return false; }
  const keep = es => [...new Set(es.map(e => e.split('\n')[0]))].filter(e => !/favicon\.ico|clips\/[^ ]*\.(mp4|webm)/.test(e));
  const picture = e => /^request failed: .*\.(png|webp|jpg|svg)$/.test(e);

  const UP = "!!document.querySelector('#desktop > *, .shim-shell > *, .shim-console:not(:empty)')";
  const PAINT = "(function(c){if(!c)return -1;var d=c.getContext('2d').getImageData(0,0,c.width,c.height).data,seen={},n=0,i;for(i=0;i<d.length&&n<2;i+=28){var k=d[i]+','+d[i+1]+','+d[i+2]+','+d[i+3];if(!seen[k]){seen[k]=1;n++}}return n})([].slice.call(document.querySelectorAll('#desktop canvas.paint')).sort(function(a,b){return b.width*b.height-a.width*a.height})[0])";
  // Which build ran (build: true, flow 3): the runtime's mark, the program's marks (an app), and any assets/ file
  // the page used that build B does not have (an old build's file).
  const bAssets = new Set(readdirSync(join(siteB, 'assets')));
  const BUILD = "JSON.stringify({ b: window.__build || 'A', p: (window.__programs || []).join('+'), used: performance.getEntriesByType('resource').map(function (e) { return e.name; }).concat([].map.call(document.querySelectorAll('script[src], link[href]'), function (e) { return e.src || e.href; })).filter(function (u) { return u.indexOf(location.origin + '/assets/') === 0; }).map(function (u) { return u.split('/assets/')[1].split('?')[0]; }) })";

  // One page step: loads path, waits for `ready` (a selector) or, for an app, presses Play (after `click`, in
  // `frame` when the app is in play.html's player) and checks its window and paint.
  // firstVisit: a picture the page never showed online (below the fold, lazy) is not kept, so its miss is a note.
  async function page(name, path, { ready, app, click, frame, build, firstVisit } = {}) {
    const t0 = Date.now();
    await send('Page.navigate', { url: 'about:blank' }, S);
    await sleep(150);
    errors = [];
    await send('Page.navigate', { url: BASE + path }, S);
    const inn = e => frame ? `(function(document, window){return ${e}})(document.querySelector('${frame}').contentDocument, document.querySelector('${frame}').contentWindow)` : e;
    let why = '';
    // The worker's "You are offline" page (in play.html: in its player frame) ends a wait at once.
    const top = "document.title==='Offline'", off = frame ? top + '||' + inn(top) : top;
    if (!app) { if (!await until(`${top}||document.readyState==='complete'&&!!document.querySelector('${ready}')`, 20000)) why = 'page did not finish loading (' + ready + ')'; else await sleep(1000); }
    else if (click && !await until(`${top}||(function(b){if(b)b.click();return !!b})(document.querySelector('${click}'))`, 20000)) why = 'no ' + click + ' to press';
    else if (!await until(off + '||' + inn("!!(document.querySelector('.shim-title-play:not([disabled])')||document.querySelector('#desktop > *'))"), 30000)) why = 'Play never became ready';
    else if (!await js(off).catch(() => false)) {
      await js(inn("(function(b){if(b)b.click()})(document.querySelector('.shim-title-play'))"));
      if (!await until(inn(UP), 20000)) why = 'no window after Play';
      else { await sleep(1500); if (!await until(inn(PAINT) + '!==1', 5000)) why = 'blank canvas (one flat color)'; }
    }
    if (await js(off).catch(() => false)) why = "the worker's Offline page (not saved)";
    if (build && !why) {
      const r = JSON.parse(await js(inn(BUILD)));
      const stale = r.used.filter(f => !bAssets.has(f));
      if (r.b !== 'B' || (app && r.p !== 'B') || stale.length) why = 'mixed builds: runtime ' + r.b + ', program ' + (r.p || 'A') + (stale.length ? ', old files ' + stale.slice(0, 3).join(' ') : '');
    }
    await sleep(100);
    const errs = keep(errors), unseen = firstVisit ? errs.filter(picture) : [], hard = errs.filter(e => !unseen.includes(e));
    if (hard.length) why = [why, ...hard.slice(0, 4)].filter(Boolean).join(' | ');
    return report(name, why, t0, unseen.length ? unseen.length + ' pictures not seen online, not kept' : '');
  }
  async function check(name, fn) { const t0 = Date.now(); let why; try { why = await fn(); } catch (e) { why = e.message; } return report(name, why, t0); }
  const home = { ready: '#player-frame' };

  // ---- flow 1: first visit, then offline ----
  console.log('== first visit, then offline');
  await page('home (online)', 'index.html', home);
  await check('service worker controls the home page', async () => await until('!!navigator.serviceWorker.controller', 15000) ? '' : 'no controller after 15 s');
  await page('story (online)', 'story.html', { ready: 'main' });
  await page('play.html?app=Minesweeper (online)', 'play.html?app=Minesweeper', { app: true, click: '#poster-play', frame: '#frame' });
  await page('Clock (online)', 'Clock/index.html', { app: true });
  await sleep(1500);   // the last page settles into the cache
  await setOffline(true);
  await page('home (offline)', 'index.html', { ...home, firstVisit: true });
  await page('story (offline)', 'story.html', { ready: 'main', firstVisit: true });
  await page('play.html?app=Minesweeper (offline)', 'play.html?app=Minesweeper', { app: true, click: '#poster-play', frame: '#frame', firstVisit: true });
  await page('Clock (offline)', 'Clock/index.html', { app: true, firstVisit: true });
  await setOffline(false);

  // ---- flow 2: Save all, then offline ----
  const SAVED = ['TankGame', 'NathaNOS', 'FlappyBird', 'SynthesizerKeyboard', 'StockChecker'];
  const THEN = ['Clock', 'TankGame', 'Minesweeper'];
  async function savedOffline(label, build) {
    await setOffline(true);
    await page('home ' + label, 'index.html', { ...home, build });
    await page('story ' + label, 'story.html', { ready: 'main' });
    for (const a of SAVED) await page(a + ' ' + label, a + '/index.html', { app: true, build });
    for (const a of THEN) await page(a + ' Then ' + label, a + '/then.html', { app: true, build });
    await page('play.html?app=Minesweeper ' + label, 'play.html?app=Minesweeper', { app: true, click: '#poster-play', frame: '#frame', build });
    await page('home ' + label + ', for Find', 'index.html', home);
    await check('Find ' + label + ' (Ctrl K, "tank")', async () => {
      await js("window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', code: 'KeyK', keyCode: 75, ctrlKey: true, bubbles: true }))");
      if (!await until("!!document.querySelector('.shim-find-input')", 10000)) return 'Find did not open';
      await js("(function (i) { i.value = 'tank'; i.dispatchEvent(new Event('input', { bubbles: true })); })(document.querySelector('.shim-find-input'))");
      if (!await until("[].some.call(document.querySelectorAll('.shim-find-item'), function (e) { return /TankGame|Tank/.test(e.textContent); })", 5000)) return 'no TankGame result';
      await sleep(300);
      // Find's source-file search reads <App>/source/files.txt, which Save all does not keep: those misses are expected.
      return keep(errors).filter(e => !/\/source\/files\.txt$/.test(e)).slice(0, 3).join(' | ');
    });
    await setOffline(false);
  }
  console.log('== Save all, then offline');
  await page('home (online)', 'index.html', home);
  await check('Save all for offline', async () => {
    const pressed = await until("(function(){var b=[].slice.call(document.querySelectorAll('button')).filter(function(b){return /Save all for offline|Save the other|Update saved apps/.test(b.textContent)})[0];if(b)b.click();return !!b})()", 20000);
    if (!pressed) return 'no Save all button';
    await sleep(500);
    if (!await until("![].some.call(document.querySelectorAll('button'), function (b) { return /Saving/.test(b.textContent); })", 300000)) return 'still saving after 300 s';
    await sleep(500);
    const text = await js("[].slice.call(document.querySelectorAll('p,div')).map(function(e){return e.textContent}).filter(function(t){return /saved on this device|Could not save/.test(t)}).sort(function(a,b){return a.length-b.length})[0]||''");
    return /^All \d+ apps saved/.test(text) ? '' : 'panel says: ' + (text || 'nothing');
  });
  await savedOffline('(saved, offline)', false);

  // ---- flow 3: a second build arrives ----
  console.log('== a second build arrives');
  await serve(siteB);
  await page('home (build B online)', 'index.html', home);
  await check("build B's service worker takes over", async () => {
    await js('navigator.serviceWorker.getRegistration().then(function (r) { return r && r.update(); })').catch(() => { });
    // activating prunes the files no kept page names, so the cache holds B's boot.js and not A's once B took over
    const name = bootB;
    const boots = "caches.open('oldjava-v1').then(function (c) { return c.keys(); }).then(function (ks) { return JSON.stringify(ks.map(function (k) { return k.url; }).filter(function (u) { return u.indexOf('/assets/boot-') > 0; }).map(function (u) { return u.split('/assets/')[1]; })); })";
    const ready = "navigator.serviceWorker.getRegistration().then(function (r) { return !!r && !!r.active && r.active.state === 'activated' && !r.waiting && !r.installing; })";
    if (!await until(`${ready}.then(function (ok) { return ok && ${boots}; }).then(function (b) { return b === ${JSON.stringify(JSON.stringify([name]))}; })`, 120000))
      return 'after 120 s the cache holds boot ' + await js(boots).catch(e => e.message) + ', build B is ' + name;
    return '';
  });
  await savedOffline('(build B, offline)', true);
} catch (e) {
  report('offline check ran', e.message, total);
} finally {
  if (ws) {
    ws.close();
  }
  if (chrome) kill(chrome);
  if (server) kill(server);
  await sleep(1000);
  try { rmSync(work, { recursive: true, force: true, maxRetries: 40, retryDelay: 500 }); } catch { }
  if (existsSync(work)) console.warn('Could not remove ' + work);
}
console.log((steps - failed) + ' of ' + steps + ' steps passed in ' + Math.round((Date.now() - total) / 1000) + ' s');
process.exit(failed ? 1 : 0);
