// The story's live panels: reading progress, the 2016 quotes, the rename, Then and now, and the chain's numbers.
import { APPS } from '../shared/catalog';
import { all, byId, one } from '../shared/dom';
// Flappy Bird's Bird class: as decompiled in the repo's first commit (4fea701, b.java) and as #1 renamed it (b331e84, Bird.java).
import decompiled from './b-decompiled.java.txt?raw';
import renamedSource from './Bird-renamed.java.txt?raw';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
// Rename-only means the two files have the same tokens apart from the names, so splitting both on identifiers
// lines them up one to one. Each name that differs becomes a span that turns from its old name into its new one.
const KEYWORDS = /^(package|import|public|private|static|final|class|int|float|void|new|this|if|else|return)$/;
const ID = /([A-Za-z_][A-Za-z0-9_]*)/;
const CHARS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';

function el<K extends keyof HTMLElementTagNameMap>(tag: K, cls?: string, text?: string | null): HTMLElementTagNameMap[K] {
  const n = document.createElement(tag);
  if (cls) n.className = cls;
  if (text != null) n.textContent = text;
  return n;
}
function text(url: string): Promise<string> {
  return fetch(url).then(r => { if (!r.ok) throw new Error(url + ': ' + r.status); return r.text(); });
}
// Run once when the element first comes into view (at once without IntersectionObserver).
function whenSeen(node: Element, fn: () => void, margin?: string) {
  if (!('IntersectionObserver' in window)) { fn(); return; }
  const io = new IntersectionObserver(es => {
    if (es.some(e => e.isIntersecting)) { io.disconnect(); fn(); }
  }, { rootMargin: margin || '0px 0px -25% 0px' });
  io.observe(node);
}

export function initChapters(): void {
  const still = matchMedia('(prefers-reduced-motion: reduce)').matches;

  // ---- reading progress and the chapter you're in ----
  const bar = byId('bar');
  const navLinks = all<HTMLAnchorElement>('.story-top nav a');
  function onScroll() {
    const max = document.documentElement.scrollHeight - innerHeight;
    bar.style.transform = 'scaleX(' + (max > 0 ? Math.min(1, scrollY / max) : 0) + ')';
    let at: HTMLAnchorElement | null = null;
    navLinks.forEach(a => {
      const s = document.querySelector(a.getAttribute('href') as string);
      if (s && s.getBoundingClientRect().top < innerHeight * 0.4) at = a;
    });
    navLinks.forEach(a => a.setAttribute('aria-current', String(a === at)));
  }
  addEventListener('scroll', onScroll, { passive: true });
  addEventListener('resize', onScroll);
  onScroll();

  // ---- 1. the 2016 quotes ----
  text('StockChecker/stockCheck.csv').then(csv => {
    const body = one('#tape tbody');
    const lines = csv.split(/\r?\n/).filter(Boolean);
    const quotes = lines.map(line => line.split(',').map(s => s.replace(/^"|"$/g, '')))
      .filter(f => f.length >= 5 && isFinite(parseFloat(f[1])));
    quotes.slice(0, 8).forEach(f => {
      const tr = el('tr');
      const change = parseFloat(f[4]);
      [f[0].toUpperCase(), (+f[1]).toFixed(2), f[4], f[2], f[3]].forEach((v, i) => {
        tr.appendChild(el('td', i === 2 ? (change > 0 ? 'up' : change < 0 ? 'down' : '') : '', v));
      });
      body.appendChild(tr);
    });
    // The rest of the file is the app's empty slots ("null",N/A,...): say so instead of showing them.
    const empty = lines.length - quotes.length;
    if (empty > 0) {
      const tr = el('tr', 'rest'), td = el('td', '', '+ ' + empty + ' empty rows ("null", N/A)');
      td.colSpan = 5;
      tr.appendChild(td);
      body.appendChild(tr);
    }
    byId('tape-note').textContent = quotes.length + ' quotes, as the app saved them';
  }).catch(e => { console.warn('The story: ' + e.message); one('#originals .panel').hidden = true; });

  // ---- 2. the recovery: rename the decompiled class in place ----
  const code = byId('code'), goBtn = byId('code-go');
  const countEl = byId('code-count'), fileEl = byId('code-file');
  // (A checkout with CRLF line endings must show the code as the HTML page did, with plain newlines.)
  const oldParts = decompiled.replace(/\r\n/g, '\n').split(ID);
  const newParts = renamedSource.replace(/\r\n/g, '\n').split(ID);
  const renames: HTMLSpanElement[] = [];
  const aligned = oldParts.length === newParts.length && oldParts.every((p, i) => i % 2 === 1 || p === newParts[i]);
  function addPlain(s: string) {
    // numbers get their own color, the rest is plain text
    s.split(/(\b\d+(?:\.\d+)?F?\b)/).forEach((bit, j) => {
      if (!bit) return;
      if (j % 2) code.appendChild(el('span', 'num', bit)); else code.appendChild(document.createTextNode(bit));
    });
  }
  (aligned ? oldParts : newParts).forEach((p, i) => {
    if (i % 2 === 0) { addPlain(p); return; }
    if (KEYWORDS.test(p)) { code.appendChild(el('span', 'kw', p)); return; }
    if (aligned && p !== newParts[i]) {
      const s = el('span', 'id old', p);
      s.dataset.old = p; s.dataset.now = newParts[i];
      renames.push(s);
      code.appendChild(s);
    } else code.appendChild(document.createTextNode(p));
  });
  const key = (s: HTMLElement) => s.dataset.old + '>' + s.dataset.now;
  const total = new Set(renames.map(key)).size;
  let done = 0, timer = 0, renamed = !aligned;
  function setCount() {
    countEl.textContent = aligned ? done + ' of ' + total + ' names' : '';
  }
  setCount();
  if (!aligned) { goBtn.hidden = true; fileEl.textContent = 'flappyBird/Bird.java'; }
  function scramble(span: HTMLElement, to: string, then: () => void) {
    if (still) { span.textContent = to; then(); return; }
    const steps = 6;
    let k = 0;
    const t = setInterval(() => {
      k++;
      if (k >= steps) { clearInterval(t); span.textContent = to; then(); return; }
      let shown = to.slice(0, Math.floor(to.length * k / steps));
      while (shown.length < to.length) shown += CHARS[Math.floor(Math.random() * CHARS.length)];
      span.textContent = shown;
    }, 35);
  }
  // One distinct rename at a time, every place it appears together, so the visitor can follow each name.
  function rename() {
    clearTimeout(timer);
    renamed = true;
    fileEl.textContent = 'flappyBird/Bird.java';
    goBtn.textContent = 'Show decompiled';
    const order: string[] = [];
    renames.forEach(s => { if (order.indexOf(key(s)) < 0) order.push(key(s)); });
    done = 0;
    setCount();
    (function next() {
      if (!renamed || !order.length) return;
      const k = order.shift();
      const group = renames.filter(s => key(s) === k);
      let left = group.length;
      group.forEach(s => {
        s.className = 'id new fresh';
        scramble(s, s.dataset.now as string, () => {
          setTimeout(() => { if (s.classList.contains('new')) s.classList.remove('fresh'); }, 500);
          if (--left === 0) { done++; setCount(); timer = window.setTimeout(next, still ? 0 : 140); }
        });
      });
    })();
  }
  function unrename() {
    clearTimeout(timer);
    renamed = false;
    fileEl.textContent = 'flappyBird/b.java';
    goBtn.textContent = 'Rename';
    renames.forEach(s => { s.className = 'id old'; s.textContent = s.dataset.old as string; });
    done = 0;
    setCount();
  }
  goBtn.addEventListener('click', () => { if (renamed) unrename(); else rename(); });

  // ---- 3. the first restoration: Then and Now in the embeddable player ----
  const frame = byId<HTMLIFrameElement>('era-frame'), pick = byId<HTMLSelectElement>('era-app');
  const thenBtn = byId('era-then'), nowBtn = byId('era-now');
  const eraNote = byId('era-note');
  const era = { app: 'FlappyBird', name: 'Flappy Bird', then: true };
  function showEra() {
    thenBtn.setAttribute('aria-pressed', String(era.then));
    nowBtn.setAttribute('aria-pressed', String(!era.then));
    const src = 'play.html?app=' + encodeURIComponent(era.app) + (era.then ? '&then' : '');
    frame.title = era.name + (era.then ? ', then' : ', now');
    eraNote.textContent = era.then ? 'Then: the recovered code with light bug fixes, as first restored.'
      : 'Now: today\'s version, after every rework since.';
    if (frame.dataset.src) frame.dataset.src = src; else frame.src = src;
  }
  whenSeen(frame, () => { frame.src = frame.dataset.src as string; delete frame.dataset.src; }, '200px 0px');
  thenBtn.addEventListener('click', () => { era.then = true; showEra(); });
  nowBtn.addEventListener('click', () => { era.then = false; showEra(); });
  pick.addEventListener('change', () => {
    era.app = pick.value;
    era.name = pick.options[pick.selectedIndex].textContent as string;
    showEra();
  });
  const lucky = APPS[Math.floor(Math.random() * APPS.length)];
  if (lucky) byId<HTMLAnchorElement>('pick').href = lucky.id + '/index.html';

  // ---- 4. the chain, counted from history.txt (one "@@<time> <subject>" line per commit, then its folders) ----
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
    function day(t: number) { const d = new Date(t); return MONTHS[d.getMonth()] + ' ' + d.getDate() + ', ' + d.getFullYear(); }
    byId('n-changes').textContent = String(commits.length);
    byId('n-days').textContent = String(days);
    byId('n-days-label').textContent = days === 1 ? 'day' : 'days, first to latest';
    byId('n-apps').textContent = String(counts.length);
    byId('chain-note').textContent = 'Every commit from ' + day(first) + ' to ' + day(last) +
      ', merges left out. A change can touch several apps.';
    const list = byId('bars'), top = counts.length ? counts[0].n : 1;
    const fills: HTMLSpanElement[] = [];
    counts.forEach(c => {
      const li = el('li'), a = el('a');
      a.href = c.app.id + '/index.html';
      a.title = c.app.name + ': ' + c.n + (c.n === 1 ? ' change' : ' changes') + '. Play it';
      a.appendChild(el('span', 'name', c.app.name));
      const track = el('span', 'track'), fill = el('span', 'fill');
      fill.dataset.w = (100 * c.n / top).toFixed(1) + '%';
      track.appendChild(fill);
      fills.push(fill);
      a.appendChild(track);
      a.appendChild(el('span', 'n', String(c.n)));
      li.appendChild(a);
      list.appendChild(li);
    });
    whenSeen(list, () => fills.forEach(f => { f.style.width = f.dataset.w as string; }));
  }).catch(e => {
    console.warn('The story: ' + e.message);
    one('#chain .show').hidden = true;
  });
}
