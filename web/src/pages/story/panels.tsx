// The story's live panels: reading progress, the 2016 quotes, the rename, Then and now, and the chain's numbers.
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { APPS } from '../../shared/catalog';
// Flappy Bird's Bird class: as decompiled in the repo's first commit (4fea701, b.java) and as #1 renamed it (b331e84, Bird.java).
import decompiled from './b-decompiled.java.txt?raw';
import renamedSource from './Bird-renamed.java.txt?raw';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
// Rename-only means the two files have the same tokens apart from the names, so splitting both on identifiers
// lines them up one to one. Each name that differs becomes a span that turns from its old name into its new one.
const KEYWORDS = /^(package|import|public|private|static|final|class|int|float|void|new|this|if|else|return)$/;
const ID = /([A-Za-z_][A-Za-z0-9_]*)/;
const CHARS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
const still = matchMedia('(prefers-reduced-motion: reduce)').matches;

function text(url: string): Promise<string> {
  return fetch(url).then(r => { if (!r.ok) throw new Error(url + ': ' + r.status); return r.text(); });
}
// Runs once when the element first comes into view (at once without IntersectionObserver).
function whenSeen(node: Element, fn: () => void, margin?: string) {
  if (!('IntersectionObserver' in window)) { fn(); return () => {}; }
  const io = new IntersectionObserver(es => {
    if (es.some(e => e.isIntersecting)) { io.disconnect(); fn(); }
  }, { rootMargin: margin || '0px 0px -25% 0px' });
  io.observe(node);
  return () => io.disconnect();
}

const CHAPTERS = [['#originals', 'The originals'], ['#recovery', 'The recovery'], ['#restoration', 'The first restoration'], ['#chain', 'The chain'], ['#numbers', 'In numbers']];

/** The bar at the top: the chapters, the one you're in marked, and how far down the page you are. */
export function StoryTop() {
  const [at, setAt] = useState<{ chapter: number; read: number } | null>(null);
  useLayoutEffect(() => {
    function onScroll() {
      const max = document.documentElement.scrollHeight - innerHeight;
      let chapter = -1;
      CHAPTERS.forEach((c, i) => {
        const s = document.querySelector(c[0]);
        if (s && s.getBoundingClientRect().top < innerHeight * 0.4) chapter = i;
      });
      setAt({ chapter, read: max > 0 ? Math.min(1, scrollY / max) : 0 });
    }
    addEventListener('scroll', onScroll, { passive: true });
    addEventListener('resize', onScroll);
    onScroll();
    return () => { removeEventListener('scroll', onScroll); removeEventListener('resize', onScroll); };
  }, []);
  return (
    <header className="story-top">
      <a className="home" href="index.html">← Old Java Applications</a>
      <nav aria-label="Chapters">
        {CHAPTERS.map((c, i) => <a key={c[0]} href={c[0]} aria-current={at ? (i === at.chapter ? 'true' : 'false') : undefined}>{c[1]}</a>)}
      </nav>
      <span className="bar" id="bar" aria-hidden="true" style={at ? { transform: 'scaleX(' + at.read + ')' } : undefined} />
    </header>
  );
}

// ---- 1. the 2016 quotes ----
/** Stock Checker's saved quotes, as a ticker tape; the panel goes if the file cannot be read. */
export function Tape() {
  const [tape, setTape] = useState<{ quotes: string[][]; empty: number } | 'gone' | null>(null);
  useEffect(() => {
    text('StockChecker/stockCheck.csv').then(csv => {
      const lines = csv.split(/\r?\n/).filter(Boolean);
      const quotes = lines.map(line => line.split(',').map(s => s.replace(/^"|"$/g, '')))
        .filter(f => f.length >= 5 && isFinite(parseFloat(f[1])));
      setTape({ quotes, empty: lines.length - quotes.length });
    }).catch(e => { console.warn('The story: ' + e.message); setTape('gone'); });
  }, []);
  const t = tape && tape !== 'gone' ? tape : null;
  return (
    <div className="panel" hidden={tape === 'gone'}>
      <div className="panel-head"><strong>StockChecker/stockCheck.csv</strong><span id="tape-note">{t ? t.quotes.length + ' quotes, as the app saved them' : 'as the app saved it'}</span></div>
      <div style={{ overflowX: 'auto' }}>
        <table className="tape" id="tape">
          <thead><tr><th>Symbol</th><th>Price</th><th>Change</th><th>Date</th><th>Time</th></tr></thead>
          <tbody>
            {t && t.quotes.slice(0, 8).map((f, r) => {
              const change = parseFloat(f[4]);
              return (
                <tr key={r}>
                  {[f[0].toUpperCase(), (+f[1]).toFixed(2), f[4], f[2], f[3]].map((v, i) => (
                    <td key={i} className={i === 2 && change ? (change > 0 ? 'up' : 'down') : undefined}>{v}</td>
                  ))}
                </tr>
              );
            })}
            {/* The rest of the file is the app's empty slots ("null",N/A,...): say so instead of showing them. */}
            {t && t.empty > 0 && <tr className="rest"><td colSpan={5}>{'+ ' + t.empty + ' empty rows ("null", N/A)'}</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ---- 2. the recovery: rename the decompiled class in place ----
type Token = { kind: 'text' | 'num' | 'kw'; text: string } | { kind: 'id'; name: Rename };
/** A name that differs between the two files: what it shows now, and its class (old, or new and fresh). */
interface Rename { old: string; now: string; text: string; cls: string }

// (A checkout with CRLF line endings must show the code as the HTML page did, with plain newlines.)
const oldParts = decompiled.replace(/\r\n/g, '\n').split(ID);
const newParts = renamedSource.replace(/\r\n/g, '\n').split(ID);
const aligned = oldParts.length === newParts.length && oldParts.every((p, i) => i % 2 === 1 || p === newParts[i]);

function tokens(): { list: Token[]; renames: Rename[] } {
  const list: Token[] = [], renames: Rename[] = [];
  function addPlain(s: string) {
    // numbers get their own color, the rest is plain text
    s.split(/(\b\d+(?:\.\d+)?F?\b)/).forEach((bit, j) => {
      if (!bit) return;
      list.push({ kind: j % 2 ? 'num' : 'text', text: bit });
    });
  }
  (aligned ? oldParts : newParts).forEach((p, i) => {
    if (i % 2 === 0) { addPlain(p); return; }
    if (KEYWORDS.test(p)) { list.push({ kind: 'kw', text: p }); return; }
    if (aligned && p !== newParts[i]) {
      const r = { old: p, now: newParts[i], text: p, cls: 'id old' };
      renames.push(r);
      list.push({ kind: 'id', name: r });
    } else list.push({ kind: 'text', text: p });
  });
  return { list, renames };
}

/** Flappy Bird's bird as decompiled; Rename turns each name into the one #1 gave it, a name at a time. */
export function RenamePanel() {
  const [{ list, renames }] = useState(tokens);
  const [, setTick] = useState(0);
  const [renamed, setRenamed] = useState(!aligned);
  const [done, setDone] = useState(0);
  const run = useRef({ renamed: !aligned, timer: 0 });
  const draw = () => setTick(n => n + 1);
  const key = (r: Rename) => r.old + '>' + r.now;
  const total = new Set(renames.map(key)).size;

  function scramble(r: Rename, to: string, then: () => void) {
    if (still) { r.text = to; draw(); then(); return; }
    const steps = 6;
    let k = 0;
    const t = setInterval(() => {
      k++;
      if (k >= steps) { clearInterval(t); r.text = to; draw(); then(); return; }
      let shown = to.slice(0, Math.floor(to.length * k / steps));
      while (shown.length < to.length) shown += CHARS[Math.floor(Math.random() * CHARS.length)];
      r.text = shown;
      draw();
    }, 35);
  }
  // One distinct rename at a time, every place it appears together, so the visitor can follow each name.
  function rename() {
    clearTimeout(run.current.timer);
    run.current.renamed = true;
    setRenamed(true);
    const order: string[] = [];
    renames.forEach(r => { if (order.indexOf(key(r)) < 0) order.push(key(r)); });
    let count = 0;
    setDone(0);
    (function next() {
      if (!run.current.renamed || !order.length) return;
      const k = order.shift();
      const group = renames.filter(r => key(r) === k);
      let left = group.length;
      group.forEach(r => {
        r.cls = 'id new fresh';
        draw();
        scramble(r, r.now, () => {
          setTimeout(() => { if (r.cls.indexOf('new') >= 0) { r.cls = 'id new'; draw(); } }, 500);
          if (--left === 0) { setDone(++count); run.current.timer = window.setTimeout(next, still ? 0 : 140); }
        });
      });
    })();
  }
  function unrename() {
    clearTimeout(run.current.timer);
    run.current.renamed = false;
    setRenamed(false);
    renames.forEach(r => { r.cls = 'id old'; r.text = r.old; });
    setDone(0);
    draw();
  }

  return (
    <div className="panel">
      <div className="panel-head">
        <strong id="code-file">{renamed ? 'flappyBird/Bird.java' : 'flappyBird/b.java'}</strong>
        {' '}
        <span className="count" id="code-count" aria-live="polite">{aligned ? done + ' of ' + total + ' names' : ''}</span>
        {' '}
        <button className="btn primary" type="button" id="code-go" hidden={!aligned} onClick={() => { if (run.current.renamed) unrename(); else rename(); }}>
          {aligned && renamed ? 'Show decompiled' : 'Rename'}
        </button>
      </div>
      <pre className="code" id="code" tabIndex={0} aria-label="Flappy Bird's Bird class">
        {list.map((t, i) => t.kind === 'text' ? t.text
          : t.kind === 'id' ? <span key={i} className={t.name.cls} data-old={t.name.old} data-now={t.name.now}>{t.name.text}</span>
          : <span key={i} className={t.kind}>{t.text}</span>)}
      </pre>
    </div>
  );
}

// ---- 3. the first restoration: Then and Now in the embeddable player ----
/** The embeddable player on a Then version, with the app to pick and a Then | Now switch; it loads once in view. */
export function EraPanel() {
  const withThen = APPS.filter(a => a.then);
  const [era, setEra] = useState({ app: 'FlappyBird', name: 'Flappy Bird', then: true });
  const [seen, setSeen] = useState(false);
  const frame = useRef<HTMLIFrameElement>(null);
  useEffect(() => whenSeen(frame.current!, () => setSeen(true), '200px 0px'), []);
  const src = 'play.html?app=' + encodeURIComponent(era.app) + (era.then ? '&then' : '');
  return (
    <>
      <div className="panel">
        <div className="panel-head">
          <label htmlFor="era-app"><strong>Then and now</strong></label>
          {' '}
          <select id="era-app" aria-label="App" defaultValue="FlappyBird" hidden={!withThen.length} onChange={e => {
            const pick = e.currentTarget;
            setEra(x => ({ ...x, app: pick.value, name: pick.options[pick.selectedIndex].textContent as string }));
          }}>
            {withThen.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
          {' '}
          <span className="era" role="group" aria-label="Version">
            <button className="btn" type="button" id="era-then" aria-pressed={era.then} onClick={() => setEra(x => ({ ...x, then: true }))}>Then</button>
            <button className="btn" type="button" id="era-now" aria-pressed={!era.then} onClick={() => setEra(x => ({ ...x, then: false }))}>Now</button>
          </span>
        </div>
        <div className="screen">
          <iframe id="era-frame" title={era.name + (era.then ? ', then' : ', now')} loading="lazy" allow="autoplay; clipboard-write" ref={frame}
            src={seen ? src : undefined} data-src={seen ? undefined : src} />
        </div>
      </div>
      <p className="caption" id="era-note">
        {era.then ? 'Then: the recovered code with light bug fixes, as first restored.' : 'Now: today\'s version, after every rework since.'}
      </p>
    </>
  );
}

// ---- 4. the chain, counted from history.txt (one "@@<time> <subject>" line per commit, then its folders) ----
interface Chain { commits: number; days: number; counts: { app: typeof APPS[number]; n: number }[]; first: number; last: number }

/** The chain's numbers and each app's changes as bars that fill once in view; gone if history.txt cannot be read. */
export function ChainShow() {
  const [chain, setChain] = useState<Chain | 'gone' | null>(null);
  const [filled, setFilled] = useState(false);
  const bars = useRef<HTMLOListElement>(null);
  useEffect(() => {
    text('history.txt').then(history => {
      const commits: { t: number; dirs: Record<string, 1> }[] = [];
      let cur: { t: number; dirs: Record<string, 1> } | null = null;
      history.split(/\r?\n/).forEach(line => {
        const m = /^@@(\d+) /.exec(line);
        if (m) { cur = { t: +m[1] * 1000, dirs: {} }; commits.push(cur); }
        else if (cur && line.trim()) cur.dirs[line.trim()] = 1;
      });
      if (!commits.length) throw new Error('history.txt is empty');
      const counts = APPS.map(a => ({ app: a, n: commits.filter(c => c.dirs[a.id]).length }))
        .filter(c => c.n > 0).sort((a, b) => b.n - a.n || a.app.name.localeCompare(b.app.name));
      const first = Math.min(...commits.map(c => c.t));
      const last = Math.max(...commits.map(c => c.t));
      const dayMs = 86400000;
      const days = Math.floor(new Date(last).setHours(0, 0, 0, 0) / dayMs - new Date(first).setHours(0, 0, 0, 0) / dayMs + 0.5) + 1;
      setChain({ commits: commits.length, days, counts, first, last });
    }).catch(e => { console.warn('The story: ' + e.message); setChain('gone'); });
  }, []);
  const c = chain && chain !== 'gone' ? chain : null;
  useEffect(() => { if (c) return whenSeen(bars.current!, () => setFilled(true)); }, [!!c]);
  function day(t: number) { const d = new Date(t); return MONTHS[d.getMonth()] + ' ' + d.getDate() + ', ' + d.getFullYear(); }
  const top = c && c.counts.length ? c.counts[0].n : 1;
  return (
    <div className="show" hidden={chain === 'gone'}>
      <div className="tiles">
        <div className="tile"><b id="n-changes">{c ? String(c.commits) : '–'}</b><span>changes</span></div>
        <div className="tile"><b id="n-days">{c ? String(c.days) : '–'}</b><span id="n-days-label">{c ? (c.days === 1 ? 'day' : 'days, first to latest') : 'days'}</span></div>
        <div className="tile"><b id="n-apps">{c ? String(c.counts.length) : '–'}</b><span>apps changed</span></div>
      </div>
      <div className="panel">
        <div className="panel-head"><strong>Changes to each app's folder</strong><span>most first</span></div>
        <ol className="bars" id="bars" ref={bars}>
          {c && c.counts.map(x => {
            const w = (100 * x.n / top).toFixed(1) + '%';
            return (
              <li key={x.app.id}>
                <a href={x.app.id + '/index.html'} title={x.app.name + ': ' + x.n + (x.n === 1 ? ' change' : ' changes') + '. Play it'}>
                  <span className="name">{x.app.name}</span>
                  <span className="track"><span className="fill" data-w={w} style={filled ? { width: w } : undefined} /></span>
                  <span className="n">{String(x.n)}</span>
                </a>
              </li>
            );
          })}
        </ol>
      </div>
      <p className="caption" id="chain-note">{c ? 'Every commit from ' + day(c.first) + ' to ' + day(c.last) + ', merges left out. A change can touch several apps.' : null}</p>
    </div>
  );
}

/** Pick one for me: a link to an app chosen at random. */
export function PickLink() {
  const [lucky] = useState(() => APPS[Math.floor(Math.random() * APPS.length)]);
  return <a className="btn" id="pick" href={lucky ? lucky.id + '/index.html' : 'index.html'}>Pick one for me</a>;
}
