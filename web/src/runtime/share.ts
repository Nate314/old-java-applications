import { embedded } from './skin';
import { siteRoot } from './install';
import { played } from './passport';
import { catalog, catalogDoc, root } from './catalog';
import { closeButton, indexOf, ownKeys } from './then-and-now';
import { embedSection } from './embed';
import { playSound } from './sounds';

export let shareBox: HTMLElement | null;
export let SHARED: RegExpExecArray | null;
export let INVITE: string;

export function openShare(id: string | null, opener?: Element | null) {
  if (embedded && window.parent !== window) {   // the home page shows it over its player, at full size
    window.parent.postMessage({ oldjavaShare: id || '' }, location.origin);
    return;
  }
  if (shareBox) return;
  // The panel belongs to the page, which a fullscreen player (Big screen) would cover.
  if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(function () {});
  var scrim = document.createElement('div');
  scrim.className = 'shim-scrim';
  var panel = document.createElement('div');
  panel.className = 'shim-share';
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-modal', 'true');
  var head = document.createElement('div');
  head.className = 'shim-drawer-head';
  var title = document.createElement('strong');
  function shut() {
    playSound('close');
    scrim.remove();
    panel.remove();
    shareBox = null;
    if (opener && opener.isConnected) (opener as HTMLElement).focus();
  }
  head.appendChild(title);
  head.appendChild(closeButton(shut));
  panel.appendChild(head);
  var body = document.createElement('div');
  body.className = 'shim-share-body';
  panel.appendChild(body);
  scrim.addEventListener('click', shut);
  ownKeys(panel, shut);
  document.body.appendChild(scrim);
  document.body.appendChild(panel);
  shareBox = panel;
  playSound('open');

  catalog().then(function (apps) {
    var app = id ? apps[indexOf(apps, id)] : null;
    var count = apps.filter(function (a) { return played[a.id]; }).length;
    var n = apps.length;
    var url = siteRoot + (app ? app.id + '/' : '') + 'index.html' + (app && count ? '?shared=' + count : '');
    var name = app ? app.name : 'the arcade';
    title.textContent = 'Share ' + name;
    panel.setAttribute('aria-label', 'Share ' + name);
    var message = app
      ? 'Play ' + app.name + ' in your browser: an old Java app, running on TeaVM.' +
        (count ? ' I have played ' + count + ' of ' + n + ' apps in this arcade.' : '')
      : count >= n ? 'I played all ' + n + ' old Java apps in this browser arcade. Arcade complete!'
      : count ? 'I have played ' + count + ' of ' + n + ' old Java apps in this browser arcade. How many can you play?'
      : n + ' old Java Swing apps and games, playable in the browser.';
    var homeTag = catalogDoc && catalogDoc.querySelector<HTMLMetaElement>('meta[property="og:description"]');

    // The preview: what a chat or a feed shows for the link.
    var label = document.createElement('p');
    label.className = 'shim-share-label';
    label.textContent = 'How the link looks when it is posted';
    var preview = document.createElement('a');
    preview.className = 'shim-share-preview';
    preview.href = url;
    preview.target = '_blank';
    preview.rel = 'noopener';
    var img = document.createElement('img');
    img.src = root() + 'cards/' + (app ? app.id : 'index') + '.jpg';
    img.alt = '';
    img.width = 1200;
    img.height = 630;
    var cap = document.createElement('span');
    cap.className = 'shim-share-cap';
    var host = document.createElement('small');
    host.textContent = location.host;
    var t = document.createElement('strong');
    t.textContent = app ? app.name + ' | Old Java Applications' : 'Old Java Applications';
    var d = document.createElement('span');
    d.textContent = app ? app.hint : homeTag ? homeTag.content : '';
    cap.appendChild(host);
    cap.appendChild(t);
    cap.appendChild(d);
    preview.appendChild(img);
    preview.appendChild(cap);

    var msgLabel = document.createElement('label');
    msgLabel.className = 'shim-share-msg';
    msgLabel.textContent = 'Message';
    var text = document.createElement('textarea');
    text.rows = 3;
    text.value = message;
    msgLabel.appendChild(text);

    var linkRow = document.createElement('div');
    linkRow.className = 'shim-share-link';
    var link = document.createElement('input');
    link.readOnly = true;
    link.value = url;
    link.setAttribute('aria-label', 'Link');
    link.addEventListener('focus', function () { link.select(); });
    var copy = document.createElement('button');
    copy.type = 'button';
    copy.className = 'shim-share-copy';
    copy.textContent = 'Copy link';
    copy.setAttribute('data-sound', 'none');
    copy.addEventListener('click', function () {
      function done(ok: boolean) {
        copy.textContent = ok ? 'Copied ✓' : 'Press Ctrl+C';
        playSound(ok ? 'saved' : 'tap');
        setTimeout(function () { copy.textContent = 'Copy link'; }, 2000);
      }
      link.select();
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url).then(function () { done(true); }, function () { done(false); });
      } else {
        var ok = false;
        try { ok = document.execCommand('copy'); } catch (e) { /* select it by hand */ }
        done(ok);
      }
    });
    linkRow.appendChild(link);
    linkRow.appendChild(copy);

    var ways = document.createElement('div');
    ways.className = 'shim-share-ways';
    function way(label: string, href: (m: string) => string, cls?: string) {
      var a = document.createElement('a');
      a.className = 'shim-share-way' + (cls ? ' ' + cls : '');
      a.textContent = label;
      a.target = '_blank';
      a.rel = 'noopener';
      a.addEventListener('click', function () { a.href = href(text.value.trim()); });
      a.href = href(message);
      ways.appendChild(a);
      return a;
    }
    var e = encodeURIComponent;
    var subject = app ? app.name + ', an old Java app in your browser' : 'Old Java Applications';
    if (navigator.share) {
      var native = document.createElement('button');
      native.type = 'button';
      native.className = 'shim-share-way primary';
      native.textContent = 'Share…';
      native.addEventListener('click', function () {
        navigator.share({ title: subject, text: text.value.trim(), url: url }).catch(function () { /* closed */ });
      });
      ways.appendChild(native);
    }
    way('X', function (m) { return 'https://x.com/intent/tweet?text=' + e(m) + '&url=' + e(url); });
    way('Bluesky', function (m) { return 'https://bsky.app/intent/compose?text=' + e(m + ' ' + url); });
    way('Reddit', function (m) { return 'https://www.reddit.com/submit?url=' + e(url) + '&title=' + e(m); });
    way('Email', function (m) { return 'mailto:?subject=' + e(subject) + '&body=' + e(m + '\n\n' + url); }).removeAttribute('target');

    [label, preview, msgLabel, linkRow, ways].forEach(function (el) { body.appendChild(el); });
    if (apps.length) body.appendChild(embedSection(app ? app.id : null, apps));
    (((navigator.share as Navigator['share'] | undefined) && (ways.firstChild as HTMLElement)) || copy).focus();
  });
}

export function init() {
  // ---- share ----
  // Share opens a panel with the link preview the page gets when it is posted (its card from cards/ with the
  // title and description of its share tags, both written by web/cards.ps1), a message with the visitor's
  // passport count, the link, and ways to send it: the system share sheet where there is one, X, Bluesky,
  // Reddit, email, or Copy. id is an app (its page is shared) or null (the arcade: the home page). A shared
  // app link carries ?shared=N, the sender's stamp count, and the title screen it opens on says so.
  shareBox = null;
  SHARED = /[?&]shared=(\d{1,3})(?:&|$)/.exec(location.search);
  // A game a friend sent (UltimateTicTacToe's Friend by link) or a tune (SynthesizerKeyboard's Record): the title
  // screen says what the link brought.
  INVITE = /[?&]game=[a-i1-9]+(?:&|$)/i.test(location.search)
    ? 'A friend sent you a game, and it is your move. Press Play to answer it.'
    : /[?&]tune=(?:[a-m][1-8]){2,}(?:&|$)/i.test(location.search)
      ? 'A friend sent you a tune. Press Play to hear it, then play it back.'
      : '';
  window.oldjavaShare = { open: openShare };
}
