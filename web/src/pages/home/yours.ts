// The gallery for a returning visitor: each card of an app with a record here says it (the line its title screen
// shows, records.ts), and the Yours filter keeps only the apps played here, the last one played first.
import { useEffect, useState } from 'react';
import type { App } from '../../shared/catalog';
import { PLAYED, collect, onSavesChange, readJson } from './saves';
import { H } from './model';
import { LOST_APPS } from '../notfound/match';

/** Whether a card shows under a filter: all, a category, or yours (stamped in the passport). */
export const inFilter = (a: App, f: string) => f === 'all' || (f === 'yours' ? window.oldjavaPassport.has(a.id) : a.cat === f);

/** The gallery's filter chips, and the words a typed filter also finds each category by. */
export const FILTERS = [['all', 'All'], ['game', 'Games'], ['music', 'Music'], ['toy', 'Toys'], ['tool', 'Tools'], ['yours', 'Yours']];
// The lost page's short-link words count too ("piano" finds SynthesizerKeyboard).
const words = (a: App) => (a.id + ' ' + a.name + ' ' + a.hint + ' ' + a.alt + ' ' + FILTERS.find(f => f[0] === a.cat)![1] + ' '
  + LOST_APPS.find(l => l.id === a.id)!.words.join(' ')).toLowerCase();
/** Whether an app has every word typed into the gallery's filter field somewhere in its name, hint, picture, category or short links. */
export const matches = (a: App, q: string) => q.toLowerCase().split(/\s+/).every(w => words(a).includes(w));
/** Whether a card shows: under the chip picked and the words typed. */
export const onView = (a: App) => inFilter(a, H.filter) && matches(a, H.query);

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
    const line = window.oldjavaRecords.line(store, saves[store]);
    if (line) out[store] = line;
  });
  return out;
}

/** The record line of each app with one, kept in step with saves from the player and from other pages. */
export function useRecordLines() {
  const [now, set] = useState(lines);
  useEffect(() => onSavesChange(() => set(prev => { const next = lines(); return JSON.stringify(next) === JSON.stringify(prev) ? prev : next; })), []);
  return now;
}
