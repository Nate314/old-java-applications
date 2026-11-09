// What the apps saved in this browser, as a bundle that can go to another one (Records.tsx).
// Apps keep their files in localStorage (web.rt.Saves, oldjava.saves.<App>) and the passport its stamps
// (oldjava.played), so on their own they stay in the browser that made them. A saves bundle carries them:
//   { "oldjava": "saves", "v": 1, "at": ISO time, "saves": { "<App>[.then]": { "/.file.txt": contents } },
//     "played": { "<App>": first play, epoch ms } }
// as a file (Download / Load, also read and written by the desktop launcher, Launcher/SaveBundle.java) or as a
// link (?saves=..., deflate-raw then base64url) shown as a QR code to scan with a phone.

export const PREFIX = 'oldjava.saves.';
export const PLAYED = 'oldjava.played';
const STORE = /^[A-Za-z0-9]+(\.then)?$/;

export type Files = Record<string, string>;
export interface Bundle { at: string; saves: Record<string, Files>; played: Record<string, number> }

export function readJson(key: string): Record<string, any> {
  try { const v = JSON.parse(localStorage.getItem(key) || '{}'); return v && typeof v === 'object' ? v : {}; } catch (e) { return {}; }
}

function storeNames(): string[] {
  const names: string[] = [];
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i) as string;
      if (k.indexOf(PREFIX) === 0 && STORE.test(k.slice(PREFIX.length))) names.push(k.slice(PREFIX.length));
    }
  } catch (e) { /* no storage */ }
  return names.sort();
}

/** Everything saved here, as the bundle written to a file or a link. */
export function collect() {
  const saves: Record<string, Files> = {};
  storeNames().forEach(name => {
    const files = readJson(PREFIX + name);
    if (Object.keys(files).length) saves[name] = files;
  });
  return { oldjava: 'saves', v: 1, at: new Date().toISOString(), saves, played: readJson(PLAYED) };
}

/** A bundle from a file or link, checked: stores and paths by name, contents as strings of bytes (one char each). */
export function check(b: any): Bundle {
  if (!b || b.oldjava !== 'saves' || typeof b.saves !== 'object' || !b.saves) throw new Error('not an Old Java saves file');
  const saves: Record<string, Files> = {}, played: Record<string, number> = {};
  Object.keys(b.saves).forEach(name => {
    const files = b.saves[name];
    if (!STORE.test(name) || !files || typeof files !== 'object') return;
    Object.keys(files).forEach(path => {
      const text = files[path];
      if (/^\/[^\0]+$/.test(path) && path.indexOf('/Downloads/') !== 0 && typeof text === 'string' && !/[^\x00-\xff]/.test(text)) {
        (saves[name] = saves[name] || {})[path] = text;
      }
    });
  });
  Object.keys(b.played || {}).forEach(id => {
    if (STORE.test(id) && b.played[id] > 0) played[id] = +b.played[id];
  });
  return { at: typeof b.at === 'string' ? b.at : '', saves, played };
}

function b64url(bytes: Uint8Array): string {
  let s = '';
  for (let i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode.apply(null, Array.from(bytes.subarray(i, i + 0x8000)));
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function unb64url(text: string): Uint8Array {
  const s = atob(text.replace(/-/g, '+').replace(/_/g, '/'));
  const bytes = new Uint8Array(s.length);
  for (let i = 0; i < s.length; i++) bytes[i] = s.charCodeAt(i);
  return bytes;
}
function pipe(bytes: Uint8Array, stream: CompressionStream | DecompressionStream): Promise<Uint8Array> {
  return new Response(new Blob([bytes as BlobPart]).stream().pipeThrough(stream)).arrayBuffer().then(b => new Uint8Array(b));
}

/** The link's payload: "z" + deflated JSON, or "j" + plain JSON where the browser cannot compress. */
export function encode(bundle: object): Promise<string> {
  const json = new TextEncoder().encode(JSON.stringify(bundle));
  if (typeof CompressionStream === 'undefined') return Promise.resolve('j' + b64url(json));
  return pipe(json, new CompressionStream('deflate-raw')).then(z => 'z' + b64url(z));
}
export function decode(payload: string): Promise<Bundle> {
  return Promise.resolve().then(() => {
    const bytes = unb64url(payload.slice(1));
    if (payload[0] === 'j') return bytes;
    if (payload[0] !== 'z' || typeof DecompressionStream === 'undefined') throw new Error('this browser cannot read that link');
    return pipe(bytes, new DecompressionStream('deflate-raw'));
  }).then(json => check(JSON.parse(new TextDecoder().decode(json))));
}
