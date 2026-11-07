import { embedded } from './skin';
import { news } from './band-news';
import { replay, replayNow } from './replay';
import { played } from './passport';
import { appId, catalog } from './catalog';
import { NO_QUIRKS, appCard, closeButton, eraPage, indexOf, isThen, ownKeys, passportLine, picks, quirkNodes, quirks, sourceDir, sourceJarLink } from './then-and-now';
import { openShare } from './share';
import { overlay } from './title-screen';
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

// One element per line, with its number (tokens that span lines, like block comments, are split).
// A class name that is a file of the app links to it.
function codeLines(text: string, linkFor: (word: string) => string | null) {
  var code = document.createElement('div');
  code.className = 'shim-source-code';
  var row: HTMLSpanElement | null = null;   // the current line's text cell
  var n = 0;
  function newLine() {
    var line = document.createElement('div');
    line.className = 'l';
    line.id = 'L' + (++n);
    var num = document.createElement('span');
    num.className = 'n';
    num.textContent = n as unknown as string;
    row = document.createElement('span');
    row.className = 't';
    line.appendChild(num);
    line.appendChild(row);
    code.appendChild(line);
  }
  newLine();
  javaTokens(text.replace(/\r\n?/g, '\n').replace(/\n$/, '')).forEach(function (t) {
    t[1].split('\n').forEach(function (part, i) {
      if (i) newLine();
      if (!part) return;
      var target = t[0] === 't' && linkFor(part);
      if (!t[0]) { row!.appendChild(document.createTextNode(part)); return; }
      var el = document.createElement(target ? 'a' : 'span') as HTMLAnchorElement;
      if (target) {
        el.href = '#source:' + target;
        el.setAttribute('data-file', target);
        el.title = 'Open ' + target;
      }
      el.className = 'tk-' + t[0];
      el.textContent = part;
      row!.appendChild(el);
    });
  });
  return { el: code, lines: n };
}

// The app READMEs use headings, paragraphs, two levels of lists, code fences, **bold** and `code`.
// A `code` span naming one of the app's files opens it.
function markdown(text: string, files: string[]) {
  var root = document.createElement('div');
  root.className = 'shim-source-md';
  function inline(el: Node, s: string) {
    s.split(/(`[^`]+`|\*\*[^*]+\*\*)/).forEach(function (part) {
      if (!part) return;
      var node;
      if (part[0] === '`') {
        var path = part.slice(1, -1);
        var isFile = files.indexOf(path) >= 0;
        node = document.createElement(isFile ? 'a' : 'code') as HTMLAnchorElement;
        if (isFile) {
          node.href = '#source:' + path;
          node.setAttribute('data-file', path);
          node.className = 'file';
        }
        node.textContent = path;
      } else if (part.slice(0, 2) === '**') {
        node = document.createElement('strong');
        node.textContent = part.slice(2, -2);
      } else {
        node = document.createTextNode(part);
      }
      el.appendChild(node);
    });
  }
  var lines = text.replace(/\r\n?/g, '\n').split('\n');
  var para = null;
  var lists: HTMLUListElement[] = [];   // the open <ul> elements, one per indent level
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    var m;
    if (/^```/.test(line)) {
      var pre = document.createElement('pre');
      var body = [];
      while (++i < lines.length && !/^```/.test(lines[i])) body.push(lines[i]);
      pre.textContent = body.join('\n');
      root.appendChild(pre);
      para = null; lists = [];
    } else if ((m = /^(#{1,3})\s+(.*)$/.exec(line))) {
      var h = document.createElement('h' + (m[1].length + 1));
      inline(h, m[2]);
      root.appendChild(h);
      para = null; lists = [];
    } else if ((m = /^(\s*)- (.*)$/.exec(line))) {
      var depth = Math.min(m[1].length >> 1, lists.length);
      lists.length = Math.min(lists.length, depth + 1);
      if (lists.length === depth) {
        var ul = document.createElement('ul');
        (depth ? lists[depth - 1].lastChild! : root).appendChild(ul);
        lists.push(ul);
      }
      var li = document.createElement('li');
      inline(li, m[2]);
      lists[depth].appendChild(li);
      para = null;
    } else if (!line.trim()) {
      para = null; lists = [];
    } else if (lists.length) {
      inline(lists[lists.length - 1].lastChild!, ' ' + line.trim());
    } else {
      if (!para) { para = document.createElement('p'); root.appendChild(para); } else para.appendChild(document.createTextNode(' '));
      inline(para, line);
    }
  }
  return root;
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
  var panel = document.createElement('aside');
  panel.className = 'shim-source';
  panel.setAttribute('aria-label', 'Source code');
  var head = document.createElement('div');
  head.className = 'shim-source-head';
  var title = document.createElement('div');
  title.className = 'shim-source-title';
  var name = document.createElement('strong');
  name.textContent = document.title;
  var origin = document.createElement('span');
  origin.className = 'shim-source-origin';
  origin.hidden = true;
  title.appendChild(name);
  title.appendChild(origin);
  var raw = document.createElement('a');
  raw.className = 'shim-source-raw';
  raw.target = '_blank';
  raw.rel = 'noopener';
  raw.textContent = 'Raw';
  var sub = document.createElement('p');
  sub.className = 'shim-source-sub';
  sub.textContent = 'Loading the source…';
  var body = document.createElement('div');
  body.className = 'shim-source-body';
  var nav = document.createElement('nav');
  nav.className = 'shim-source-files';
  nav.setAttribute('aria-label', 'Files');
  var view = document.createElement('div');
  view.className = 'shim-source-view';
  view.tabIndex = -1;
  var path = document.createElement('div');
  path.className = 'shim-source-path';
  var page = document.createElement('div');
  page.className = 'shim-source-page';
  view.appendChild(path);
  view.appendChild(page);
  body.appendChild(nav);
  body.appendChild(view);

  function shut() {
    playSound('close');
    panel.remove();
    document.documentElement.removeAttribute('data-source');
    sourcePanel = null;
    history.replaceState(null, '', location.pathname + location.search);
    if (opener && opener.focus && document.contains(opener)) opener.focus();
  }
  head.appendChild(title);
  if (!isThen()) {
    head.appendChild(sourceJarLink(appId()!));
  }
  head.appendChild(raw);
  head.appendChild(closeButton(shut));
  panel.appendChild(head);
  panel.appendChild(sub);
  panel.appendChild(body);
  ownKeys(panel, shut);
  document.body.appendChild(panel);
  document.documentElement.setAttribute('data-source', '');
  playSound('open');

  var files: string[] = [];
  var texts: Record<string, Promise<string>> = {};
  var current: string | null = null;
  function text(f: string) {
    if (!texts[f]) {
      texts[f] = fetch(sourceDir() + f).then(function (r) {
        if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
        return r.text();
      });
      texts[f].catch(function () { delete texts[f]; });
    }
    return texts[f];
  }
  // A class name in a file: the file of that class, preferring one in the same folder.
  function linkFor(from: string) {
    var dir = from.slice(0, from.lastIndexOf('/') + 1);
    return function (word: string) {
      var near = dir + word + '.java';
      if (files.indexOf(near) >= 0) return near === from ? null : near;
      for (var i = 0; i < files.length; i++) {
        if (files[i].slice(files[i].lastIndexOf('/') + 1) === word + '.java') return files[i];
      }
      return null;
    };
  }
  function mark(line: number) {
    var old = page.querySelector('.l.on');
    if (old) old.classList.remove('on');
    var row = line && page.querySelector('#L' + line);
    if (row) {
      row.classList.add('on');
      row.scrollIntoView({ block: 'center' });
    }
  }
  function address(f: string | null, line: number) {
    history.replaceState(null, '', location.pathname + location.search + '#source:' + f + (line ? ':' + line : ''));
  }
  function show(f: string, line: number) {
    if (!files.length) return;
    if (files.indexOf(f) < 0) f = files[0];
    Array.prototype.forEach.call(nav.querySelectorAll('button'), function (b) {
      b.setAttribute('aria-current', String(b.getAttribute('data-file') === f));
    });
    var tab = nav.querySelector<HTMLElement>('button[aria-current="true"]');
    if (tab) nav.scrollTo({ left: tab.offsetLeft - nav.offsetLeft - 8, top: tab.offsetTop - nav.offsetTop - 40 });
    raw.href = sourceDir() + f;
    address(f, line);
    if (current === f) { mark(line); return; }
    current = f;
    path.textContent = f;
    page.textContent = '';
    text(f).then(function (t) {
      if (current !== f) return;
      if (/\.md$/.test(f)) {
        page.appendChild(markdown(t, files));
      } else {
        var code = codeLines(t, linkFor(f));
        page.appendChild(code.el);
        path.textContent = f + ' · ' + code.lines + ' lines';
      }
      view.scrollTop = 0;
      mark(line);
    }, function (e) {
      if (current !== f) return;
      page.textContent = 'Could not load ' + f + ' (' + e.message + ').';
      current = null;
    });
  }
  panel.addEventListener('click', function (e) {
    var link = (e.target as Element).closest('[data-file]');
    if (link) {
      if (e.button || e.ctrlKey || e.metaKey || e.shiftKey) return;
      e.preventDefault();
      show(link.getAttribute('data-file')!, 0);
      if (link.tagName === 'A') view.focus({ preventScroll: true });
      return;
    }
    var num = (e.target as Element).closest('.shim-source-code .n');
    if (num) {
      var n = +num.textContent;
      mark(n);
      address(current, n);
    }
  });
  sourcePanel = { show: show };

  catalog().then(function (apps) {
    var app = apps[indexOf(apps, appId())];
    if (!app) return;
    name.textContent = app.name;
    var original = app.origin === 'original';
    origin.textContent = original ? 'Original source' : 'Decompiled';
    origin.title = original ? 'The author’s own Eclipse source' : 'Recovered by decompiling the app’s jar';
    origin.classList.add(original ? 'original' : 'decompiled');
    origin.hidden = false;
  });
  sourceFiles().then(function (list) {
    files = list;
    var java = files.filter(function (f) { return /\.java$/.test(f); });
    var dir = '';
    files.forEach(function (f) {
      var folder = f.indexOf('/') >= 0 ? f.slice(0, f.lastIndexOf('/')).replace(/^src\/?/, '') : '';
      if (folder && folder !== dir) {
        var label = document.createElement('span');
        label.className = 'dir';
        label.textContent = folder.replace(/\//g, '.');
        nav.appendChild(label);
      }
      dir = folder;
      var b = document.createElement('button');
      b.type = 'button';
      b.setAttribute('data-file', f);
      b.setAttribute('data-sound', 'none');
      b.title = f;
      b.textContent = f.slice(f.lastIndexOf('/') + 1);
      nav.appendChild(b);
    });
    Promise.all(java.map(text)).then(function (all) {
      var count = all.reduce(function (sum, t) { return sum + t.replace(/\r?\n$/, '').split('\n').length; }, 0);
      sub.textContent = java.length + ' Java file' + (java.length === 1 ? '' : 's') + ', ' + count +
        ' lines. This is the code running on this page: TeaVM compiled it to JavaScript.';
    }, function () { sub.textContent = 'The Java this page was compiled from.'; });
    show(file as string, line);
    view.focus({ preventScroll: true });
  }, function (e) {
    sub.textContent = 'Could not load the source list (' + e.message + '). Build the site with Maven (see web/README.md), or go online.';
  });
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
  var box = overlay(msg, 'Run again').firstChild as HTMLElement;
  box.classList.add('shim-exit');
  ownKeys(box);
  playSound('exit');
  if (!embedded) {
    var home = document.createElement('a');
    home.className = 'shim-exit-home';
    home.href = '../index.html';
    home.textContent = 'All apps';
    box.appendChild(home);
  }
  var tell = document.createElement('button');
  tell.type = 'button';
  tell.className = 'shim-exit-share';
  tell.setAttribute('data-sound', 'none');
  tell.textContent = '↗ Share';
  tell.addEventListener('click', function () { openShare(appId(), tell); });
  box.appendChild(tell);
  if (replay && replay.live()) {   // the last moments before it closed
    var again = document.createElement('button');
    again.type = 'button';
    again.className = 'shim-exit-share';
    again.setAttribute('data-sound', 'none');
    again.textContent = '⟲ Replay';
    again.title = 'Watch, save or share the last moments of play';
    again.addEventListener('click', function () { replayNow(again); });
    box.appendChild(again);
  }
  box.querySelector('button')!.focus();
  catalog().then(function (apps) {
    var app = apps[indexOf(apps, appId())];
    if (app) {
      var h = document.createElement('h2');
      h.textContent = app.name + ' has closed';
      box.insertBefore(h, box.firstChild);
      if (app.then && !embedded) {   // the player has its own Then | Now switch
        var era = document.createElement('a');
        era.className = 'shim-exit-era';
        era.href = eraPage(!isThen());
        era.textContent = isThen() ? 'Play today’s version' : 'Play it as first restored';
        box.insertBefore(era, tell.nextSibling);
      }
    }
    var next = picks(apps, appId());
    if (!next.length) return;
    var h3 = document.createElement('h3');
    h3.textContent = played[next[0].id] ? 'Play something else' : 'Play something new';
    var row = document.createElement('div');
    row.className = 'shim-picks';
    next.forEach(function (a) { row.appendChild(appCard(a, false, true)); });
    box.appendChild(h3);
    box.appendChild(row);
    box.appendChild(passportLine(apps));
  });
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
