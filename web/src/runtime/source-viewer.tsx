import { createElement, Fragment, useEffect, useLayoutEffect, useRef, useState, type MouseEvent as ReactMouseEvent } from 'react';
import { embedded } from './skin';
import { news } from './band-news';
import { replay, replayNow } from './replay';
import { played } from './passport';
import { appId, catalog, type CatalogApp } from './catalog';
import { JAR_KEYS, JAR_KEYS_LABEL, NO_QUIRKS, eraPage, indexOf, isThen, picks, quirkNodes, quirks, sourceDir } from './then-and-now';
import { openShare } from './share';
import { Overlay } from './ui/overlay';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';
import { AppCard, JarLink, PassportLine } from './ui/cards';
import { playSound } from './sounds';

let JAVA_KEYWORDS: string[];
// The open source panel: show opens a file (the first when it is not one of the app's) at a line.
export let sourcePanel: { show(f: string, line: number): void } | null;
let sourceList: Promise<string[]> | null;

const JAVA_TOKEN = /(\/\*[\s\S]*?\*\/|\/\/[^\n]*)|("(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*')|(@[A-Za-z_]\w*)|(\b0[xX][0-9a-fA-F_]+[lL]?\b|\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[lLfFdD]?\b)|([A-Za-z_$][\w$]*)/g;

function sourceFiles() {
  if (!sourceList) {
    sourceList = fetch(sourceDir() + 'files.txt').then(function (r) {
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r.text();
    }).then(function (text) {
      return text.split(/\r?\n/).filter(Boolean).sort(function (a, b) {
        return (a === 'README.md' ? -1 : b === 'README.md' ? 1 : 0) || (a < b ? -1 : a > b ? 1 : 0);
      });
    });
    sourceList.catch(function () { sourceList = null; });
  }
  return sourceList;
}

// Tokens as [class, text]; class '' is plain text.
function javaTokens(text: string) {
  var out: [string, string][] = [];
  var last = 0;
  var m;
  JAVA_TOKEN.lastIndex = 0;
  while ((m = JAVA_TOKEN.exec(text))) {
    if (m.index > last) out.push(['', text.slice(last, m.index)]);
    var cls = m[1] ? 'c' : m[2] ? 's' : m[3] ? 'a' : m[4] ? 'n'
      : JAVA_KEYWORDS.indexOf(m[5]) >= 0 ? 'k' : /^[A-Z]/.test(m[5]) ? 't' : '';
    out.push([cls, m[0]]);
    last = JAVA_TOKEN.lastIndex;
  }
  if (last < text.length) out.push(['', text.slice(last)]);
  return out;
}

/** A source file's lines, each a list of tokens: [class ('' for plain text), text, the file it links to]. */
type CodeLine = [string, string, string | null][];

// One row per line, with its number (tokens that span lines, like block comments, are split).
// A class name that is a file of the app links to it.
function codeLines(text: string, linkFor: (word: string) => string | null): CodeLine[] {
  const lines: CodeLine[] = [[]];
  javaTokens(text.replace(/\r\n?/g, '\n').replace(/\n$/, '')).forEach(function (t) {
    t[1].split('\n').forEach(function (part, i) {
      if (i) lines.push([]);
      if (!part) return;
      lines[lines.length - 1].push([t[0], part, t[0] === 't' ? linkFor(part) : null]);
    });
  });
  return lines;
}

function Code({ lines, marked }: { lines: CodeLine[]; marked: number }) {
  return (
    <div className="shim-source-code">
      {lines.map((tokens, i) => (
        <div key={i} className={'l' + (i + 1 === marked ? ' on' : '')} id={'L' + (i + 1)}>
          <span className="n">{i + 1}</span>
          <span className="t">
            {tokens.map(([cls, part, target], j) => !cls ? part
              : target ? <a key={j} href={'#source:' + target} data-file={target} title={'Open ' + target} className={'tk-' + cls}>{part}</a>
              : <span key={j} className={'tk-' + cls}>{part}</span>)}
          </span>
        </div>
      ))}
    </div>
  );
}

/** A README taken apart: elements as [tag, attributes, children], text as strings. */
type MdNode = [string, Record<string, string>, (MdNode | string)[]];

// The app READMEs use headings, paragraphs, two levels of lists, code fences, **bold** and `code`.
// A `code` span naming one of the app's files opens it.
function markdown(text: string, files: string[]): MdNode {
  const root: MdNode = ['div', { className: 'shim-source-md' }, []];
  function inline(el: MdNode, s: string) {
    s.split(/(`[^`]+`|\*\*[^*]+\*\*)/).forEach(function (part) {
      if (!part) return;
      if (part[0] === '`') {
        var path = part.slice(1, -1);
        el[2].push(files.indexOf(path) >= 0 ? ['a', { href: '#source:' + path, 'data-file': path, className: 'file' }, [path]] : ['code', {}, [path]]);
      } else if (part.slice(0, 2) === '**') {
        el[2].push(['strong', {}, [part.slice(2, -2)]]);
      } else {
        el[2].push(part);
      }
    });
  }
  const last = (el: MdNode) => el[2][el[2].length - 1] as MdNode;
  var lines = text.replace(/\r\n?/g, '\n').split('\n');
  var para: MdNode | null = null;
  var lists: MdNode[] = [];   // the open <ul> elements, one per indent level
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    var m;
    if (/^```/.test(line)) {
      var body = [];
      while (++i < lines.length && !/^```/.test(lines[i])) body.push(lines[i]);
      root[2].push(['pre', {}, [body.join('\n')]]);
      para = null; lists = [];
    } else if ((m = /^(#{1,3})\s+(.*)$/.exec(line))) {
      var h: MdNode = ['h' + (m[1].length + 1), {}, []];
      inline(h, m[2]);
      root[2].push(h);
      para = null; lists = [];
    } else if ((m = /^(\s*)- (.*)$/.exec(line))) {
      var depth = Math.min(m[1].length >> 1, lists.length);
      lists.length = Math.min(lists.length, depth + 1);
      if (lists.length === depth) {
        var ul: MdNode = ['ul', {}, []];
        (depth ? last(lists[depth - 1]) : root)[2].push(ul);
        lists.push(ul);
      }
      var li: MdNode = ['li', {}, []];
      inline(li, m[2]);
      lists[depth][2].push(li);
      para = null;
    } else if (!line.trim()) {
      para = null; lists = [];
    } else if (lists.length) {
      inline(last(lists[lists.length - 1]), ' ' + line.trim());
    } else {
      if (!para) { para = ['p', {}, []]; root[2].push(para); } else para[2].push(' ');
      inline(para, line);
    }
  }
  return root;
}

function Markdown({ node }: { node: MdNode }) {
  const [tag, attrs, children] = node;
  return createElement(tag, attrs, ...children.map((c, i) => typeof c === 'string' ? c : <Markdown key={i} node={c} />));
}

function parseSourceHash() {
  var m = /^#source(?::(.+?))?(?::(\d+))?$/.exec(decodeURIComponent(location.hash));
  return m ? { file: m[1] || null, line: m[2] ? +m[2] : 0 } : null;
}

export function sourceFromHash() {
  var want = parseSourceHash();
  if (want) openSource(want.file, want.line);
}

function openSource(file: string | null, line: number) {
  if (sourcePanel) { sourcePanel.show(file as string, line); return; }
  var opener = document.activeElement as HTMLElement | null;
  function shut() {
    playSound('close');
    close();
    document.documentElement.removeAttribute('data-source');
    sourcePanel = null;
    history.replaceState(null, '', location.pathname + location.search);
    if (opener && opener.focus && document.contains(opener)) opener.focus();
  }
  const close = openLayer(<SourcePanel file={file} line={line} onClose={shut} onShow={show => { sourcePanel = { show }; }} />);
  document.documentElement.setAttribute('data-source', '');
  playSound('open');
}

/** What the page shows: a README, a Java file, why a file could not load, or nothing yet. */
type Page = { md: MdNode } | { code: CodeLine[] } | { error: string } | null;

function address(f: string | null, line: number) {
  history.replaceState(null, '', location.pathname + location.search + '#source:' + f + (line ? ':' + line : ''));
}

/**
 * The source panel: the app's README and every source file it was compiled from, with the files down the side
 * (in folders), the open file highlighted with line numbers, and the line in the address marked.
 */
function SourcePanel({ file, line, onClose, onShow }: {
  file: string | null; line: number; onClose: () => void; onShow: (show: (f: string, line: number) => void) => void;
}) {
  const panel = useRef<HTMLElement>(null);
  const nav = useRef<HTMLElement>(null);
  const view = useRef<HTMLDivElement>(null);
  const [app, setApp] = useState<CatalogApp | null>(null);
  const [files, setFiles] = useState<string[]>([]);
  const [sub, setSub] = useState('Loading the source…');
  // The file asked for (its button is marked, Raw opens it), and how many times, so its button is scrolled to each time.
  const [tab, setTab] = useState<{ f: string; n: number } | null>(null);
  const [path, setPath] = useState('');
  const [page, setPage] = useState<Page>(null);
  const [mark, setMark] = useState({ line: 0, seq: 0, top: false });   // top: the page is new, scroll it to the top first
  // What show needs between renders: the list, the file loaded (null when it failed) and the files read.
  const st = useRef({ files: [] as string[], current: null as string | null, texts: {} as Record<string, Promise<string>> }).current;
  useOwnKeys(panel, onClose);

  function text(f: string) {
    if (!st.texts[f]) {
      st.texts[f] = fetch(sourceDir() + f).then(function (r) {
        if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
        return r.text();
      });
      st.texts[f].catch(function () { delete st.texts[f]; });
    }
    return st.texts[f];
  }
  // A class name in a file: the file of that class, preferring one in the same folder.
  function linkFor(from: string) {
    var dir = from.slice(0, from.lastIndexOf('/') + 1);
    return function (word: string) {
      var near = dir + word + '.java';
      if (st.files.indexOf(near) >= 0) return near === from ? null : near;
      for (var i = 0; i < st.files.length; i++) {
        if (st.files[i].slice(st.files[i].lastIndexOf('/') + 1) === word + '.java') return st.files[i];
      }
      return null;
    };
  }
  function markLine(line: number, top = false) { setMark(m => ({ line, seq: m.seq + 1, top })); }
  function show(f: string, line: number) {
    if (!st.files.length) return;
    if (st.files.indexOf(f) < 0) f = st.files[0];
    setTab(t => ({ f, n: (t ? t.n : 0) + 1 }));
    address(f, line);
    if (st.current === f) { markLine(line); return; }
    st.current = f;
    setPath(f);
    setPage(null);
    text(f).then(function (t) {
      if (st.current !== f) return;
      if (/\.md$/.test(f)) {
        setPage({ md: markdown(t, st.files) });
      } else {
        var code = codeLines(t, linkFor(f));
        setPage({ code });
        setPath(f + ' · ' + code.length + ' lines');
      }
      markLine(line, true);
    }, function (e) {
      if (st.current !== f) return;
      setPage({ error: 'Could not load ' + f + ' (' + e.message + ').' });
      st.current = null;
    });
  }
  useLayoutEffect(() => { onShow(show); }, []);

  useEffect(() => {
    catalog().then(function (apps) {
      var app = apps[indexOf(apps, appId())];
      if (app) setApp(app);
    });
    sourceFiles().then(function (list) {
      st.files = list;
      setFiles(list);
      var java = list.filter(function (f) { return /\.java$/.test(f); });
      Promise.all(java.map(text)).then(function (all) {
        var count = all.reduce(function (sum, t) { return sum + t.replace(/\r?\n$/, '').split('\n').length; }, 0);
        setSub(java.length + ' Java file' + (java.length === 1 ? '' : 's') + ', ' + count +
          ' lines. This is the code running on this page: TeaVM compiled it to JavaScript.');
      }, function () { setSub('The Java this page was compiled from.'); });
      show(file as string, line);
      view.current!.focus({ preventScroll: true });
    }, function (e) {
      setSub('Could not load the source list (' + e.message + '). Build the site with Maven (see web/README.md), or go online.');
    });
  }, []);

  // The marked file's button in view in the file list.
  useLayoutEffect(() => {
    const b = nav.current!.querySelector<HTMLElement>('button[aria-current="true"]');
    if (b) nav.current!.scrollTo({ left: b.offsetLeft - nav.current!.offsetLeft - 8, top: b.offsetTop - nav.current!.offsetTop - 40 });
  }, [tab, files]);
  // A new page starts at the top; the marked line is scrolled to the middle.
  useLayoutEffect(() => {
    if (!mark.seq) return;
    if (mark.top) view.current!.scrollTop = 0;
    const row = mark.line && view.current!.querySelector('#L' + mark.line);
    if (row) row.scrollIntoView({ block: 'center' });
  }, [mark]);

  function onClick(e: ReactMouseEvent) {
    var link = (e.target as Element).closest('[data-file]');
    if (link) {
      if (e.button || e.ctrlKey || e.metaKey || e.shiftKey) return;
      e.preventDefault();
      show(link.getAttribute('data-file')!, 0);
      if (link.tagName === 'A') view.current!.focus({ preventScroll: true });
      return;
    }
    var num = (e.target as Element).closest('.shim-source-code .n');
    if (num) {
      var n = +num.textContent!;
      markLine(n);
      address(st.current, n);
    }
  }

  const original = app && app.origin === 'original';
  const id = appId()!;
  let dir = '';
  return (
    <aside className="shim-source" aria-label="Source code" ref={panel} onClick={onClick}>
      <div className="shim-source-head">
        <div className="shim-source-title">
          <strong>{app ? app.name : document.title}</strong>
          <span className={'shim-source-origin' + (app ? (original ? ' original' : ' decompiled') : '')} hidden={!app}
            title={app ? (original ? 'The author’s own Eclipse source' : 'Recovered by decompiling the app’s jar') : undefined}>
            {app ? (original ? 'Original source' : 'Decompiled') : ''}
          </span>
        </div>
        {!isThen() && (
          <JarLink id={id} className="shim-source-jar" keysNote={JAR_KEYS[id] ? ' (' + (JAR_KEYS_LABEL[id] || 'presses real keys') + ')' : undefined}>
            Download .jar
          </JarLink>
        )}
        <a className="shim-source-raw" target="_blank" rel="noopener" href={tab ? sourceDir() + tab.f : undefined}>Raw</a>
        <CloseButton onClick={onClose} />
      </div>
      <p className="shim-source-sub">{sub}</p>
      <div className="shim-source-body">
        <nav className="shim-source-files" aria-label="Files" ref={nav}>
          {files.map(f => {
            const folder = f.indexOf('/') >= 0 ? f.slice(0, f.lastIndexOf('/')).replace(/^src\/?/, '') : '';
            const label = folder && folder !== dir ? <span className="dir">{folder.replace(/\//g, '.')}</span> : null;
            dir = folder;
            return (
              <Fragment key={f}>
                {label}
                <button type="button" data-file={f} data-sound="none" title={f} aria-current={tab ? String(f === tab.f) as 'true' | 'false' : undefined}>
                  {f.slice(f.lastIndexOf('/') + 1)}
                </button>
              </Fragment>
            );
          })}
        </nav>
        <div className="shim-source-view" tabIndex={-1} ref={view}>
          <div className="shim-source-path">{path}</div>
          <div className="shim-source-page">
            {page && ('md' in page ? <Markdown node={page.md} /> : 'code' in page ? <Code lines={page.code} marked={mark.line} /> : page.error)}
          </div>
        </div>
      </div>
    </aside>
  );
}

// The app's hint from the gallery, on the band's news line (the band's ? button shows it again).
// On then.html the line reads out the quirks its README still listed then instead (or that there are none).
export function howToPlay() {
  Promise.all([catalog(), isThen() ? quirks() : []]).then(function (r) {
    var app = r[0][indexOf(r[0], appId())];
    if (!app || !news) return;
    var tag = document.createElement('strong');
    tag.className = 'shim-news-tag';
    tag.textContent = isThen() ? 'Known quirks' : 'How to play';
    var text = document.createElement('span');
    if (isThen() && r[1].length) {
      r[1].forEach(function (parts, i) {
        if (i) text.appendChild(document.createTextNode(' · '));
        quirkNodes(parts).forEach(function (n) { text.appendChild(n); });
      });
    } else if (isThen()) text.textContent = NO_QUIRKS;   // today's hint would describe today's app
    else text.textContent = app.hint;
    news.show('hint', [tag], [text], [], 12000);
  });
}

// The app called System.exit (for most apps: its window was closed). Offer it again, and three others.
export function exitScreen(msg: string) {
  if (news && news.kind() === 'hint') news.clear();
  openLayer(<ExitScreen msg={msg} replayLive={!!(replay && replay.live())} />);
  playSound('exit');
}

function ExitScreen({ msg, replayLive }: { msg: string; replayLive: boolean }) {
  const box = useRef<HTMLDivElement>(null);
  const share = useRef<HTMLButtonElement>(null);
  const again = useRef<HTMLButtonElement>(null);
  const [found, setFound] = useState<{ apps: CatalogApp[]; next: CatalogApp[] } | null>(null);
  useOwnKeys(box);
  useLayoutEffect(() => { box.current!.querySelector('button')!.focus(); }, []);
  useEffect(() => { catalog().then(apps => setFound({ apps, next: picks(apps, appId()) })); }, []);
  const app = found ? found.apps[indexOf(found.apps, appId())] : null;
  const next = found ? found.next : [];
  return (
    <Overlay text={msg} button="Run again" className="shim-exit" boxRef={box} before={app && <h2>{app.name + ' has closed'}</h2>}>
      {!embedded && <a className="shim-exit-home" href="../index.html">All apps</a>}
      <button type="button" className="shim-exit-share" data-sound="none" ref={share} onClick={() => openShare(appId(), share.current)}>{'↗ Share'}</button>
      {app && app.then && !embedded && (   // the player has its own Then | Now switch
        <a className="shim-exit-era" href={eraPage(!isThen())}>{isThen() ? 'Play today’s version' : 'Play it as first restored'}</a>
      )}
      {replayLive && (   // the last moments before it closed
        <button type="button" className="shim-exit-share" data-sound="none" title="Watch, save or share the last moments of play"
          ref={again} onClick={() => replayNow(again.current)}>{'⟲ Replay'}</button>
      )}
      {next.length > 0 && (
        <>
          <h3>{played[next[0].id] ? 'Play something else' : 'Play something new'}</h3>
          <div className="shim-picks">{next.map(a => <AppCard key={a.id} app={a} showNew />)}</div>
          <PassportLine apps={found!.apps} />
        </>
      )}
    </Overlay>
  );
}

export function init() {
  // ---- source viewer ----
  // The Java this page runs, next to it: Source (toolbar or title screen) docks a panel on the right with the
  // app's README and every source file it was compiled from (copied into <App>/source/ by the build, listed
  // in source/files.txt), highlighted, with line numbers. The app keeps running beside it. Class names that
  // are files of the app, and file paths in the README, open those files. The address follows the open file
  // and line (#source:src/minesweeper/GameFrame.java:42), so a link opens the same place.
  JAVA_KEYWORDS = ('abstract assert boolean break byte case catch char class const continue default do double ' +
    'else enum extends final finally float for goto if implements import instanceof int interface long native new ' +
    'package private protected public return short static strictfp super switch synchronized this throw throws ' +
    'transient try void volatile while true false null var').split(' ');
  sourcePanel = null;
  sourceList = null;
  window.addEventListener('hashchange', function () {
    if (document.body.hasAttribute('data-app') && !embedded) sourceFromHash();
  });
}
