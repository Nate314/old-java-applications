// What the home page shows, kept in one place: the player, the tour, the passport, the arcade's tabs and panels,
// and the gallery. The page's controllers (player.ts, tabs.ts, news.ts, takeaway.ts, clips.ts) change it and call
// draw(), which draws the page (Home.tsx) again from it at once, so what they do next can measure the result.
import { useLayoutEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { APPS, app, type App } from '../../shared/catalog';
import type { NewsView } from './news';

/** The app the player opens on when the address names none, with the computer playing. */
export const FIRST = app('Minesweeper');

/** One stop of the tour, and where the visitor is in it (tour.ts lists the stops). */
export interface Tour {
  on: boolean; stop: number; touched: boolean; spent: number; done: boolean; doc: Document | null; timer: number; last: number;
  rendered: boolean;   // its box has been drawn for a stop once (until then it keeps the words the page starts with)
}

/** A news item from history.txt (lazy/news.tsx reads it). */
export interface NewsItem { t: number; title: string; more: string; apps: OldjavaCatalogApp[] }

export const H = {
  // ---- the player ----
  current: null as App | null,   // the app the player has
  played: null as App | null,    // the app last started in it (none while the page's own first frame runs)
  then: false,                   // as first restored (<App>/then.html)
  /** The player's iframe: a new one (key) for each app, so the old app's timers and audio stop with it. */
  frame: { key: 0, src: FIRST.id + '/index.html?embed&watch', title: FIRST.name, focus: false, size: null as null | { w: number; h: number; scale: number } },
  screenH: null as number | null,   // the screen's height (none on the big screen, which takes the room it has)
  big: false,                       // the player covers the window (where real fullscreen is not allowed)
  hintOpen: false,                  // the pocket bar's hint, opened to its full length
  quirk: null as { app: string; items: string[][] } | null,   // a Then app's known quirks, once read
  chart: null as string[][] | null, // a controller's buttons, shown in the hint for a while (the runtime's gamepads)
  replay: false,                    // the app's page says it is recording (Replay shows)
  photo: false,                     // the app's page says it runs (Photo shows)
  pop: { shown: false, on: false, synced: false },   // Pop out, whether the app is out, and whether that has been asked
  filter: 'all',
  pad: { called: false, has: false, shown: false, drawn: false },   // the docked controller (called: set for an app; drawn: put in its box once)
  fresh: null as string | null,     // the stamp just collected
  resetAsk: false,                  // Reset asks once more
  tour: { on: false, stop: 0, touched: false, spent: 0, done: false, doc: null, timer: 0, last: 0, rendered: false } as Tour,
  // ---- your arcade ----
  tab: null as string | null,       // the panel on view: news, passport, records or takeaway
  news: { items: null as NewsItem[] | null, apps: [] as OldjavaCatalogApp[], all: false, view: null as NewsView | null },
  take: {
    shown: false, text: 'Install it as an app, and save all 20 to play with no connection.', install: false, note: false,
    meterShown: false, meterW: null as number | null, saving: false, saveText: 'Save all for offline', offline: false,
    saved: {} as Record<string, boolean>, checked: false,
  },
  recordsCount: 0,
  // ---- the gallery's preview clips ----
  clip: { current: null as string | null, playing: false, wanted: {} as Record<string, boolean>, broken: {} as Record<string, boolean> },
};

let redraw: ((sync: boolean) => void) | null = null, beforeDraw: (() => void) | null = null;
/** Draws the page again from H: at once (sync), or with React's next render when called while React renders. */
export function draw(sync = true) {
  if (beforeDraw) beforeDraw();
  if (redraw) redraw(sync);
}
/** What works out the rest of H from the parts the controllers set, before each draw (the tab on view: tabs.ts). */
export function setBeforeDraw(fn: () => void) { beforeDraw = fn; }

/** Home calls this: it is the page draw() draws. */
export function useHomeDraw() {
  const [, setTick] = useState(0);
  useLayoutEffect(() => {
    redraw = sync => { if (sync) flushSync(() => setTick(n => n + 1)); else setTick(n => n + 1); };
    return () => { redraw = null; };
  }, []);
}

export const indexOfApp = (id: string | null | undefined) => APPS.findIndex(a => a.id === id);
