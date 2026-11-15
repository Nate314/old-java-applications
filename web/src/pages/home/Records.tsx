// Your records (the home page's Records tab): what the apps saved in this browser, and ways to take it to another
// one: a file (Download / Load, which the desktop launcher reads and writes too) or a link, shown as a QR code to
// scan with a phone. Nothing is changed by opening a link or a file until the visitor picks what to bring in.
// It renders inside <section id="records">, which stays in Home.tsx: the arcade tabs (tabs.ts) show and hide that
// section, and badge the Records tab with the count it reports (onCount).
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { APPS } from '../../shared/catalog';
import { qr, type QrCode as Code } from '../../shared/qr';
import { QrCode } from '../../shared/QrCode';
import { byId } from '../../shared/dom';
import { stopPlayer } from './player';
import { PLAYED, PREFIX, check, collect, decode, encode, readJson, type Bundle, type Files } from './saves';

const MAX_QR_VERSION = 30;   // bigger codes are too dense to scan from a screen

const nameOf = (store: string) =>
  (APPS.find(a => a.id === store.split('.')[0])?.name ?? store.split('.')[0]) + (/\.then$/.test(store) ? ' (Then)' : '');
const shotOf = (store: string) => APPS.find(a => a.id === store.split('.')[0])?.shot;
function line(store: string, files: Files) {
  let text = /\.then$/.test(store) ? '' : window.oldjavaRecords.line(store, files);
  if (!text) text = 'Saved: ' + Object.keys(files).map(p => p.split('/').pop()).join(', ');
  return text.replace(/^Saved here from last time: /, 'Saved: ');
}
const kb = (bytes: number) => bytes < 1024 ? bytes + ' bytes' : (bytes / 1024).toFixed(bytes < 10240 ? 1 : 0) + ' KB';

interface Phone { link: string; code: Code | null; text: string }
interface Incoming { bundle: Bundle; from: string; stamps: string[]; key: number }

/** onCount hears how many apps have saves here, for the Records tab's badge. */
export function Records({ onCount }: { onCount?: (n: number) => void }) {
  const [saves, setSaves] = useState(() => collect().saves);
  const [status, say] = useState('');
  const [phone, setPhone] = useState<Phone | null>(null);
  const [incoming, setIncoming] = useState<Incoming | null>(null);
  const picker = useRef<HTMLInputElement>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const stores = Object.keys(saves);
  const empty = !stores.length;
  useEffect(() => { if (onCount) onCount(stores.length); }, [stores.length]);

  useEffect(() => {
    const refresh = () => setSaves(collect().saves);
    const onStorage = (e: StorageEvent) => { if (e.key === null || e.key.indexOf(PREFIX) === 0) refresh(); };
    window.addEventListener('storage', onStorage);
    // The player's app saves every second while it runs; keep the list in step without a storage event (same page).
    const timer = setInterval(() => { if (!document.hidden) refresh(); }, 5000);
    return () => { window.removeEventListener('storage', onStorage); clearInterval(timer); };
  }, []);

  function bringIn(bundle: Bundle, from: string) {
    const stamps = Object.keys(bundle.played).filter(id => !window.oldjavaPassport.has(id) && APPS.some(a => a.id === id));
    if (!Object.keys(bundle.saves).length && !stamps.length) { say('Nothing in ' + from + ' to bring in.'); return; }
    window.oldjavaArcade.show('records');   // the dialog is in this tab's panel, which must be on view
    setIncoming({ bundle, from, stamps, key: Date.now() });
  }
  useEffect(() => { if (incoming) dialog.current?.showModal(); }, [incoming]);

  // A link from Send to a phone: ?saves=... on this page. It is taken off the address at once, so a reload or a
  // bookmark does not offer it again.
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const payload = params.get('saves');
    if (!payload) return;
    params.delete('saves');
    const rest = params.toString();
    history.replaceState(null, '', location.pathname + (rest ? '?' + rest : '') + location.hash);
    decode(payload).then(bundle => {
      byId('records').scrollIntoView({ block: 'start' });
      bringIn(bundle, 'a link');
    }).catch(() => say('Could not read the saves in that link: it may have been cut short when it was copied.'));
  }, []);

  // Download: the bundle as a file. The desktop launcher reads the same file.
  function download() {
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([JSON.stringify(collect(), null, 1)], { type: 'application/json' }));
    a.download = 'oldjava-saves.json';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    say('Downloaded oldjava-saves.json. Load it here in another browser, or in the desktop launcher (Saves).');
  }

  // Load: a file from Download (or the desktop launcher's Saves), then the same choice as a link.
  function load() {
    const input = picker.current as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    file.text().then(text => check(JSON.parse(text)))
      .then(bundle => bringIn(bundle, file.name))
      .catch(e => say('Could not read ' + file.name + ': ' + e.message + '.'));
  }

  // Send to a phone: a link carrying the bundle, as a QR code.
  function send() {
    if (phone) { setPhone(null); return; }
    encode(collect()).then(payload => {
      const url = new URL('./', location.href);
      url.searchParams.set('saves', payload);
      const link = url.href, made = qr(link);
      const code = made && made.version <= MAX_QR_VERSION ? made : null;
      const local = /^(localhost|127\.|\[::1\])/.test(location.host);
      const text = !code
        ? 'Too much to fit in a code (' + kb(link.length) + '). Copy the link instead, or use Download.'
        : 'Point your phone\'s camera at the code and open the link. Nothing changes there until you pick what to bring in.' +
          (local ? ' This page is served from this computer (' + location.host + '), so a phone can only open the link if it can reach this address.' : '');
      setPhone({ link, code, text });
    });
  }
  function copy(field: HTMLInputElement) {
    (navigator.clipboard ? navigator.clipboard.writeText(field.value) : Promise.reject()).then(
      () => say('Link copied. Open it in the other browser.'),
      () => { field.select(); say('Select the link and copy it.'); });
  }

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const { bundle, stamps } = incoming as Incoming;
    const chosen = Array.from(e.currentTarget.querySelectorAll<HTMLInputElement>('input[type=checkbox]'))
      .filter(b => b.checked).map(b => b.value);
    dialog.current?.close();
    setIncoming(null);
    // The player's app keeps its own copy of its files in memory and writes them back when it closes, so it is
    // closed before anything is written, then started again on what was brought in.
    const frame = document.getElementById('player-frame') as HTMLIFrameElement | null;
    const done = () => {
      try {
        chosen.forEach(store => {
          const files = readJson(PREFIX + store);
          Object.keys(bundle.saves[store]).forEach(p => { files[p] = bundle.saves[store][p]; });
          localStorage.setItem(PREFIX + store, JSON.stringify(files));
        });
        if (stamps.length) {
          const played = readJson(PLAYED);
          stamps.forEach(id => { played[id] = bundle.played[id]; });
          localStorage.setItem(PLAYED, JSON.stringify(played));
          window.oldjavaPassport.reload();
        }
        say('Brought in ' + (chosen.length ? chosen.length + (chosen.length === 1 ? ' app\'s saves' : ' apps\' saves') : 'no saves') +
          (stamps.length ? ' and ' + stamps.length + ' passport ' + (stamps.length === 1 ? 'stamp' : 'stamps') : '') + '.');
      } catch (err) {
        say('Could not keep them: ' + (err as Error).message + '.');
      }
      setSaves(collect().saves);
      if (frame) byId('player-restart').click();
      if (window.oldjavaSound) window.oldjavaSound.play('saved');
    };
    if (!frame || !chosen.length) { done(); return; }
    frame.addEventListener('load', function once() { frame.removeEventListener('load', once); done(); });
    stopPlayer();
  }

  const when = incoming?.bundle.at ? new Date(incoming.bundle.at) : null;
  return (
    <>
      <div className="records-head">
        <div className="text">
          <h2 id="records-title">Your records</h2>
          <p>
            <span className="count" id="records-count">
              {empty ? 'Nothing saved yet' : stores.length + (stores.length === 1 ? ' app has' : ' apps have') + ' saves here'}
            </span>
            . Best times, scores, stars and progress the apps saved in this browser. Take them to your phone, another browser or the desktop apps.
          </p>
        </div>
        <div className="actions">
          <button className="btn primary" type="button" id="records-send" aria-expanded={!!phone} aria-controls="records-phone"
                  disabled={empty} onClick={send}>
            Send to my phone
          </button>
          {' '}
          <button className="btn" type="button" id="records-download" disabled={empty} onClick={download}>Download</button>
          {' '}
          <button className="btn" type="button" id="records-load" onClick={() => picker.current?.click()}>Load</button>
          {' '}
          <input type="file" id="records-file" accept=".json,application/json" hidden ref={picker} onChange={load} />
        </div>
      </div>
      <p className="status" id="records-status" role="status" hidden={!status}>{status}</p>
      <div className="phone" id="records-phone" hidden={!phone}>
        <div className="qr" id="records-qr" role="img" aria-label="QR code of the link that carries your saves" hidden={!!phone && !phone.code}
             style={phone?.code ? { width: Math.min(360, Math.max(200, (phone.code.size + 6) * 3.5)) + 'px' } : undefined}>
          {phone?.code && <QrCode code={phone.code} quiet={3} />}
        </div>
        <div className="text">
          <p id="records-qr-text">{phone?.text}</p>
          <div className="linkrow">
            <input type="text" id="records-link" readOnly aria-label="Link that carries your saves" value={phone?.link ?? ''} />
            {' '}
            <button className="btn" type="button" id="records-copy" data-sound="none"
                    onClick={e => copy((e.currentTarget.parentNode as HTMLElement).querySelector('input') as HTMLInputElement)}>
              Copy link
            </button>
          </div>
        </div>
      </div>
      <p className="empty" id="records-empty" hidden={!empty}>
        Play an app and what it keeps (Minesweeper's best times, your FlappyBird best, Rubik's Cube stars) shows up here.
      </p>
      <ul className="record-list" id="records-list">
        {stores.map(store => {
          const shot = shotOf(store);
          return (
            <li key={store}>
              <button type="button" title={'Play ' + nameOf(store)}
                      onClick={() => { const open = window.oldjavaPassport.open; if (open) open(store.split('.')[0]); }}>
                {shot && <img src={shot} alt="" loading="lazy" />}
                <span><strong>{nameOf(store)}</strong><span>{line(store, saves[store])}</span></span>
              </button>
            </li>
          );
        })}
      </ul>
      <p className="desktop">
        {"On the desktop: "}
        <code>./run.ps1</code>
        {" opens the launcher, whose Saves button loads a downloaded file into the desktop apps, or saves theirs as a file to Load here."}
      </p>
      <dialog className="records-in" id="records-in" aria-labelledby="records-in-title" ref={dialog} onClose={() => setIncoming(null)}>
        <form id="records-in-form" method="dialog" onSubmit={submit}>
          <h2 id="records-in-title">Bring your saves here?</h2>
          <p id="records-in-text">
            {incoming && 'From ' + incoming.from +
              (when && !isNaN(+when) ? ', saved ' + when.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '') +
              '. Each app you tick takes these files in place of the ones saved here.'}
          </p>
          <ul id="records-in-list" key={incoming?.key}>
            {incoming && Object.keys(incoming.bundle.saves).map(store => {
              const here = readJson(PREFIX + store), there = incoming.bundle.saves[store];
              const same = Object.keys(there).every(p => here[p] === there[p]);
              return (
                <li key={store}>
                  <label>
                    <input type="checkbox" value={store} defaultChecked={!same} disabled={same} />
                    <span>
                      <strong>{nameOf(store)}</strong>
                      <span>{'Bringing: ' + line(store, there)}</span>
                      <span className="here">
                        {same ? 'Already the same here' : Object.keys(here).length ? 'Here now: ' + line(store, here) : 'Nothing saved here yet'}
                      </span>
                    </span>
                  </label>
                </li>
              );
            })}
          </ul>
          <p id="records-in-stamps" hidden={!incoming?.stamps.length}>
            {incoming?.stamps.length ? 'Plus ' + incoming.stamps.length + ' passport ' + (incoming.stamps.length === 1 ? 'stamp' : 'stamps') +
              ' not collected here: ' + incoming.stamps.map(nameOf).join(', ') + '.' : ''}
          </p>
          <div className="buttons">
            <button className="btn" type="button" id="records-in-cancel" onClick={() => dialog.current?.close()}>Not now</button>
            {' '}
            <button className="btn primary" type="submit">Bring them in</button>
          </div>
        </form>
      </dialog>
    </>
  );
}
