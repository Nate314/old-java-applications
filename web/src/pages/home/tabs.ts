// ---- your arcade: one panel, four tabs ----
// What's new (news.ts), the passport, your records (Records.tsx) and Take it with you (takeaway.ts) each say
// whether they have anything to show; this picks which one is on view (the others get .off), names each tab's
// news in a badge, and follows the ARIA tabs pattern (arrows, Home and End move between tabs).
// The tab chosen last is remembered; fresh news (an app you played has changed) takes the panel once per visit.
import type { KeyboardEvent as ReactKeyboardEvent } from 'react';
import { APPS } from '../../shared/catalog';
import { H, draw, setBeforeDraw } from './model';

const KEY = 'oldjava.arcadeTab';
export const TABS = ['news', 'passport', 'records', 'takeaway'];
let chosenByVisitor = false;

function remembered() { try { return localStorage.getItem(KEY); } catch (e) { return null; } }

/** Whether a tab's panel has anything to show. */
export function tabShown(id: string) {
  return id === 'news' ? !!H.news.view : id === 'takeaway' ? H.take.shown : true;
}
const visible = () => TABS.filter(tabShown);

/** The stamps collected, as the passport's ring counts them. */
export function stampCount() { return APPS.filter(a => window.oldjavaPassport.has(a.id)).length; }

/** A tab's badge: its text ('' for none) and whether it is news. */
export function badge(id: string): { text: string; isNew: boolean } {
  if (id === 'news') return { text: H.news.view && H.news.view.fresh ? 'New' : '', isNew: true };
  if (id === 'passport') { const n = stampCount(); return { text: n > 0 ? n + '/' + APPS.length : '', isNew: false }; }
  if (id === 'records') return { text: H.recordsCount > 0 ? String(H.recordsCount) : '', isNew: false };
  return { text: '', isNew: false };
}

// Keeps the tab on view in step with the panels: a panel that hides itself takes its tab with it, and fresh news
// takes the panel until the visitor picks a tab.
function sync() {
  let on = H.tab;
  if (H.news.view && H.news.view.fresh && !chosenByVisitor) on = 'news';
  if (!on || !tabShown(on)) {
    const want = remembered() || 'passport';
    on = TABS.indexOf(want) >= 0 && tabShown(want) ? want : visible()[0];
  }
  H.tab = on;
}

export function tabClick(id: string) {
  chosenByVisitor = true;
  try { localStorage.setItem(KEY, id); } catch (e) { /* not kept */ }
  H.tab = id;
  draw();
}

export function tabKey(e: ReactKeyboardEvent, id: string) {
  const list = visible();
  const i = list.indexOf(id);
  const next = e.key === 'ArrowRight' ? list[(i + 1) % list.length]
    : e.key === 'ArrowLeft' ? list[(i - 1 + list.length) % list.length]
    : e.key === 'Home' ? list[0] : e.key === 'End' ? list[list.length - 1] : null;
  if (!next) return;
  e.preventDefault();
  tabClick(next);
  document.getElementById('tab-' + next)!.focus();
}

export function initArcadeTabs(): void {
  setBeforeDraw(sync);
  draw();
  // For code that sends the visitor to a panel (Records.tsx, with saves from a link).
  window.oldjavaArcade = { show: id => { if (tabShown(id)) { H.tab = id; draw(); } } };
}
