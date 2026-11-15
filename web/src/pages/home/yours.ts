// The gallery for a returning visitor: each card of an app with a record here says it (the line its title screen
// shows, records.ts), and the Yours filter keeps only the apps played here, the last one played first.
import { useEffect, useState } from 'react';
import type { App } from '../../shared/catalog';
import { PLAYED, PREFIX, collect, readJson } from './saves';

/** Whether a card shows under a filter: all, a category, or yours (stamped in the passport). */
export const inFilter = (a: App, f: string) => f === 'all' || (f === 'yours' ? window.oldjavaPassport.has(a.id) : a.cat === f);

/** Each played app's place in the Yours filter, last played first (oldjava.lastPlayed, else its first stamp). */
export function playedRank(): Record<string, number> {
  const last = readJson('oldjava.lastPlayed'), first = readJson(PLAYED);
  const when = (id: string) => +last[id] || +first[id] || 0;
  const rank: Record<string, number> = {};
  Object.keys(first).sort((a, b) => when(b) - when(a)).forEach((id, i) => { rank[id] = i; });
  return rank;
}

function lines() {
  const saves = collect().saves, out: Record<string, string> = {};
  Object.keys(saves).forEach(store => {
    const line = /\.then$/.test(store) ? '' : window.oldjavaRecords.line(store, saves[store]);
    // The fallback that only names files ("Saved here from last time: ...") is no record to show on a card.
    if (line && line.indexOf('Saved here') !== 0) out[store] = line;
  });
  return out;
}

/** The record line of each app with one, kept in step with saves from the player and from other pages. */
export function useRecordLines() {
  const [now, set] = useState(lines);
  useEffect(() => {
    const refresh = () => set(prev => { const next = lines(); return JSON.stringify(next) === JSON.stringify(prev) ? prev : next; });
    const onStorage = (e: StorageEvent) => { if (e.key === null || e.key.indexOf(PREFIX) === 0) refresh(); };
    window.addEventListener('storage', onStorage);
    // The player's app saves while it runs, on this page, so no storage event comes for it.
    const timer = setInterval(() => { if (!document.hidden) refresh(); }, 5000);
    return () => { window.removeEventListener('storage', onStorage); clearInterval(timer); };
  }, []);
  return now;
}
