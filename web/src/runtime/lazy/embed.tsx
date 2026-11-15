/*
 * Put it on your site: the Share panel's builder for the embeddable player (play.html).
 *
 * The runtime imports this file the first time "Put it on your site" is opened in a Share panel, then calls
 * window.oldjavaEmbed.build(box, opts). The visitor picks an app, Now or Then, how it opens (a poster with a
 * Play button, or the computer already playing, for the apps that can watch) and a size, sees the widget live
 * as it will look, and copies the <iframe> tag. The tag's address is absolute (the address this site is served
 * from), since it is pasted into someone else's page.
 */
import { useLayoutEffect, useRef, useState } from 'react';
import { openLayer } from '../ui/layer';

declare global {
  interface Window {
    oldjavaEmbed?: { build(box: HTMLElement, opts: OldjavaEmbedOptions): void };
  }
  /** What the runtime's embed.tsx hands build(). */
  interface OldjavaEmbedOptions {
    apps: OldjavaCatalogApp[]; app: string | null; siteRoot: string; watchable: string[]; sound?(name: string): void;
  }
}

const SIZES = [
  { id: 'fit', label: 'Fit width', w: 0, h: 0 },
  { id: 's', label: '480 × 320', w: 480, h: 320 },
  { id: 'm', label: '640 × 420', w: 640, h: 420 },
  { id: 'l', label: '900 × 560', w: 900, h: 560 }
];

function escapeAttr(s: string) {
  return String(s).replace(/"/g, '&quot;').replace(/</g, '&lt;');   // a bare & before a word is fine in HTML
}

/**
 * A row of radio-like buttons (aria-pressed), the way the Share panel's other choices look. off, given, says which
 * items can be turned off and whether each is now (those carry a title saying so).
 */
function Choice({ name, items, value, off = {}, onPick }: {
  name: string; items: { id: string; label: string }[]; value: string; off?: Record<string, boolean>; onPick: (id: string) => void;
}) {
  return (
    <div className="shim-embed-choice" role="group" aria-label={name}>
      {items.map(item => (
        <button key={item.id} type="button" data-sound="none" aria-pressed={item.id === value} disabled={!!off[item.id]}
          title={item.id in off ? (off[item.id] ? 'Not for this app' : '') : undefined}
          onClick={() => onPick(item.id)}>{item.label}</button>
      ))}
    </div>
  );
}

interface EmbedState { app: string; then: boolean; watch: boolean; size: string }

function EmbedBuilder({ opts }: { opts: OldjavaEmbedOptions }) {
  const apps = opts.apps;
  const root = opts.siteRoot;   // absolute, ends in /
  const app = (s: EmbedState) => apps.filter(a => a.id === s.app)[0] || apps[0];
  const watchable = (a: OldjavaCatalogApp) => opts.watchable.indexOf(a.id) >= 0;
  // A choice that does not fit the app picked is taken back, and stays so if the app changes again.
  function fitted(s: EmbedState): EmbedState {
    const a = app(s);
    if (!a.then && s.then) s = { ...s, then: false };
    if ((!watchable(a) || s.then) && s.watch) s = { ...s, watch: false };
    return s;
  }
  const [state, setState] = useState<EmbedState>(() => fitted({ app: opts.app || (apps[0] && apps[0].id), then: false, watch: false, size: 'fit' }));
  const [width, setWidth] = useState(0);
  const [copied, setCopied] = useState<boolean | null>(null);
  const view = useRef<HTMLDivElement>(null);
  const code = useRef<HTMLTextAreaElement>(null);
  const change = (to: Partial<EmbedState>) => setState(s => fitted({ ...s, ...to }));

  const a = app(state);
  const s = SIZES.filter(x => x.id === state.size)[0];
  const url = root + 'play.html?app=' + encodeURIComponent(a.id) +
    (state.then && a.then ? '&then' : '') + (state.watch && watchable(a) && !state.then ? '&watch' : '');
  const title = a.name + (state.then ? ' (as first restored)' : '') + ', an old Java app you can play';
  const style = s.w ? 'border:0;border-radius:12px;max-width:100%' : 'width:100%;aspect-ratio:16/10;border:0;border-radius:12px';
  const tag = '<iframe src="' + escapeAttr(url) + '"' + (s.w ? ' width="' + s.w + '" height="' + s.h + '"' : '') +
    ' style="' + style + '" title="' + escapeAttr(title) + '" allow="autoplay; fullscreen; clipboard-write"' +
    ' loading="lazy"></iframe>';
  // The preview is the widget itself, at the chosen size scaled into the panel.
  const vw = s.w || 640, vh = s.h || 400;
  const scale = Math.min(1, (width || 480) / vw);

  useLayoutEffect(() => { code.current!.value = tag; }, [tag]);   // its value, not a text node in it
  // The preview's room is measured again with each choice.
  useLayoutEffect(() => { setWidth(view.current!.clientWidth); }, [state]);
  useLayoutEffect(() => {
    // The panel had no width while the section was closed; size the preview once it has one.
    requestAnimationFrame(() => { if (view.current) setWidth(view.current.clientWidth); });
  }, []);

  function copy() {
    function done(ok: boolean) {
      setCopied(ok);
      if (opts.sound) opts.sound(ok ? 'saved' : 'tap');
      setTimeout(function () { setCopied(null); }, 2000);
    }
    code.current!.select();
    if (navigator.clipboard && 'writeText' in navigator.clipboard) {
      navigator.clipboard.writeText(code.current!.value).then(function () { done(true); }, function () { done(false); });
    } else {
      var ok = false;
      try { ok = document.execCommand('copy'); } catch (e) { /* select it by hand */ }
      done(ok);
    }
  }

  return (
    <>
      <p className="shim-embed-intro">
        Paste this tag into a blog post, a class page or your own site, and visitors play the app right there.
        {' '}It shows a picture and a Play button until they press it, and links back here.
      </p>
      <div className="shim-embed-row">
        <label className="shim-embed-pick">
          App
          <select value={state.app} onChange={e => change({ app: e.currentTarget.value })}>
            {apps.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
          </select>
        </label>
        <div className="shim-embed-field">
          <span className="shim-embed-name">Version</span>
          <Choice name="Version" items={[{ id: 'now', label: 'Now' }, { id: 'then', label: 'Then' }]} value={state.then ? 'then' : 'now'}
            off={{ then: !a.then }} onPick={v => change({ then: v === 'then', watch: v === 'then' ? false : state.watch })} />
        </div>
      </div>
      <div className="shim-embed-row">
        <div className="shim-embed-field">
          <span className="shim-embed-name">Opens with</span>
          <Choice name="Opens with" items={[{ id: 'play', label: 'Play button' }, { id: 'watch', label: 'Computer playing' }]}
            value={state.watch ? 'watch' : 'play'} off={{ watch: !(watchable(a) && !state.then) }} onPick={v => change({ watch: v === 'watch' })} />
        </div>
      </div>
      <div className="shim-embed-row">
        <div className="shim-embed-field">
          <span className="shim-embed-name">Size</span>
          <Choice name="Size" items={SIZES} value={state.size} onPick={v => change({ size: v })} />
        </div>
      </div>
      <div className="shim-embed-view" style={{ height: Math.round(vh * scale) + 'px' }} ref={view}>
        <iframe loading="lazy" allow="autoplay; fullscreen; clipboard-write" title={'Preview: ' + title} src={url}
          style={{ width: vw + 'px', height: vh + 'px', transform: scale < 1 ? 'scale(' + scale + ')' : undefined }} />
      </div>
      <textarea className="shim-embed-code" readOnly rows={4} aria-label="Embed code" ref={code} onFocus={e => e.currentTarget.select()} />
      <div className="shim-share-link">
        <button type="button" className="shim-share-copy" data-sound="none" onClick={copy}>
          {copied === null ? 'Copy embed code' : copied ? 'Copied ✓' : 'Press Ctrl+C'}
        </button>
        <a className="shim-share-way" href={url} target="_blank" rel="noopener" title="Open the widget on its own, as visitors will see it">Open it</a>
      </div>
    </>
  );
}

window.oldjavaEmbed = { build: function (box, opts) { openLayer(<EmbedBuilder opts={opts} />, box); } };

export {};   // a module, imported when it is needed (its chunk is assets/embed.js)
