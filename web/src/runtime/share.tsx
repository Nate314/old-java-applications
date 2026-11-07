import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { embedded } from './skin';
import { siteRoot } from './install';
import { played } from './passport';
import { catalog, catalogDoc, root, type CatalogApp } from './catalog';
import { indexOf } from './then-and-now';
import { embedSection } from './embed';
import { playSound } from './sounds';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';

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
  function shut() {
    playSound('close');
    close();
    shareBox = null;
    if (opener && opener.isConnected) (opener as HTMLElement).focus();
  }
  const close = openLayer(<SharePanel id={id} onClose={shut} onPanel={el => { shareBox = el; }} />);
  playSound('open');
}

/** What the panel says once the catalog is in: the link, its preview card's text and the message. */
function shareText(id: string | null, apps: CatalogApp[]) {
  const app = id ? apps[indexOf(apps, id)] : null;
  const count = apps.filter(a => played[a.id]).length;
  const n = apps.length;
  const homeTag = catalogDoc && catalogDoc.querySelector<HTMLMetaElement>('meta[property="og:description"]');
  return {
    app,
    url: siteRoot + (app ? app.id + '/' : '') + 'index.html' + (app && count ? '?shared=' + count : ''),
    name: app ? app.name : 'the arcade',
    message: app
      ? 'Play ' + app.name + ' in your browser: an old Java app, running on TeaVM.' +
        (count ? ' I have played ' + count + ' of ' + n + ' apps in this arcade.' : '')
      : count >= n ? 'I played all ' + n + ' old Java apps in this browser arcade. Arcade complete!'
      : count ? 'I have played ' + count + ' of ' + n + ' old Java apps in this browser arcade. How many can you play?'
      : n + ' old Java Swing apps and games, playable in the browser.',
    subject: app ? app.name + ', an old Java app in your browser' : 'Old Java Applications',
    title: app ? app.name + ' | Old Java Applications' : 'Old Java Applications',
    description: app ? app.hint : homeTag ? homeTag.content : '',
  };
}
type ShareText = ReturnType<typeof shareText>;

const e = encodeURIComponent;
/** Where the message and link can be posted: the link for each, from the message as it stands. */
const WAYS: [string, (m: string, s: ShareText) => string][] = [
  ['X', (m, s) => 'https://x.com/intent/tweet?text=' + e(m) + '&url=' + e(s.url)],
  ['Bluesky', (m, s) => 'https://bsky.app/intent/compose?text=' + e(m + ' ' + s.url)],
  ['Reddit', (m, s) => 'https://www.reddit.com/submit?url=' + e(s.url) + '&title=' + e(m)],
  ['Email', (m, s) => 'mailto:?subject=' + e(s.subject) + '&body=' + e(m + '\n\n' + s.url)],
];

/**
 * The Share panel: the link preview the page gets when it is posted (its card from cards/ with the title and
 * description of its share tags, both written by web/cards.ps1), a message with the visitor's passport count, the
 * link, and ways to send it. Its body fills in once the catalog is read.
 */
function SharePanel({ id, onClose, onPanel }: { id: string | null; onClose: () => void; onPanel: (el: HTMLElement) => void }) {
  const panel = useRef<HTMLDivElement>(null);
  const [apps, setApps] = useState<CatalogApp[] | null>(null);
  useOwnKeys(panel, onClose);
  useLayoutEffect(() => { onPanel(panel.current!); }, [onPanel]);
  useEffect(() => { catalog().then(setApps); }, []);
  const s = apps && shareText(id, apps);
  return (
    <>
      <div className="shim-scrim" onClick={onClose} />
      <div className="shim-share" role="dialog" aria-modal="true" aria-label={s ? 'Share ' + s.name : undefined} ref={panel}>
        <div className="shim-drawer-head">
          <strong>{s ? 'Share ' + s.name : ''}</strong>
          <CloseButton onClick={onClose} />
        </div>
        <div className="shim-share-body">{s && <ShareBody s={s} apps={apps!} />}</div>
      </div>
    </>
  );
}

function ShareBody({ s, apps }: { s: ShareText; apps: CatalogApp[] }) {
  const [message, setMessage] = useState(s.message);
  const [copied, setCopied] = useState<boolean | null>(null);
  const copy = useRef<HTMLButtonElement>(null);
  const ways = useRef<HTMLDivElement>(null);
  const link = useRef<HTMLInputElement>(null);
  const end = useRef<HTMLSpanElement>(null);
  const text = useRef<HTMLTextAreaElement>(null);
  useLayoutEffect(() => {
    text.current!.value = s.message;   // its value, not a text node in it
    // Put it on your site (embed.ts builds it) is the body's last child, in place of this marker.
    const marker = end.current!;
    if (apps.length) marker.replaceWith(embedSection(s.app ? s.app.id : null, apps));
    else marker.remove();
    ((('share' in navigator) && ways.current!.firstChild as HTMLElement) || copy.current!).focus();
  }, []);
  function onCopy() {
    function done(ok: boolean) {
      setCopied(ok);
      playSound(ok ? 'saved' : 'tap');
      setTimeout(function () { setCopied(null); }, 2000);
    }
    link.current!.select();
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(s.url).then(function () { done(true); }, function () { done(false); });
    } else {
      var ok = false;
      try { ok = document.execCommand('copy'); } catch (err) { /* select it by hand */ }
      done(ok);
    }
  }
  const m = message.trim();
  return (
    <>
      <p className="shim-share-label">How the link looks when it is posted</p>
      <a className="shim-share-preview" href={s.url} target="_blank" rel="noopener">
        <img src={root() + 'cards/' + (s.app ? s.app.id : 'index') + '.jpg'} alt="" width={1200} height={630} />
        <span className="shim-share-cap">
          <small>{location.host}</small>
          <strong>{s.title}</strong>
          <span>{s.description}</span>
        </span>
      </a>
      <label className="shim-share-msg">
        Message
        <textarea rows={3} ref={text} onChange={ev => setMessage(ev.target.value)} />
      </label>
      <div className="shim-share-link">
        <input readOnly value={s.url} aria-label="Link" ref={link} onFocus={() => link.current!.select()} />
        <button type="button" className="shim-share-copy" data-sound="none" ref={copy} onClick={onCopy}>
          {copied === null ? 'Copy link' : copied ? 'Copied ✓' : 'Press Ctrl+C'}
        </button>
      </div>
      <div className="shim-share-ways" ref={ways}>
        {'share' in navigator && (
          <button type="button" className="shim-share-way primary"
            onClick={() => navigator.share({ title: s.subject, text: m, url: s.url }).catch(function () { /* closed */ })}>
            {'Share…'}
          </button>
        )}
        {WAYS.map(([label, href]) => (
          <a key={label} className="shim-share-way" href={href(m, s)} target={label === 'Email' ? undefined : '_blank'} rel="noopener">
            {label}
          </a>
        ))}
      </div>
      <span ref={end} hidden />
    </>
  );
}

export function init() {
  // ---- share ----
  // Share opens a panel (SharePanel) with the link preview, a message and ways to send it: the system share sheet
  // where there is one, X, Bluesky, Reddit, email, or Copy. id is an app (its page is shared) or null (the arcade:
  // the home page). A shared app link carries ?shared=N, the sender's stamp count, and the title screen it opens
  // on says so.
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
