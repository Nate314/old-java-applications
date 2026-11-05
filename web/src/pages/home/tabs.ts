// ---- your arcade: one panel, four tabs ----
// What's new (news.js), the passport, your records (saves.js) and Take it with you each keep their own code and
// still show or hide themselves with `hidden`; this only picks which one is on view (the others get .off), names
// each tab's news in a badge, and follows the ARIA tabs pattern (arrows, Home and End move between tabs).
// The tab chosen last is remembered; fresh news (an app you played has changed) takes the panel once per visit.
import { all, byId } from '../../shared/dom';

const KEY = 'oldjava.arcadeTab';

export function initArcadeTabs(): void {
  const tabs = all<HTMLButtonElement>('#arcade [role="tab"]');
  let chosenByVisitor = false;
  const panel = (tab: HTMLElement) => byId(tab.getAttribute('aria-controls') as string);
  const tabFor = (id: string) => document.getElementById('tab-' + id);
  const visible = () => tabs.filter(t => !panel(t).hidden);
  function remembered() { try { return localStorage.getItem(KEY); } catch (e) { return null; } }

  function select(tab: HTMLElement, focus: boolean) {
    tabs.forEach(t => {
      const on = t === tab;
      t.setAttribute('aria-selected', String(on));
      t.tabIndex = on ? 0 : -1;
      panel(t).classList.toggle('off', !on);
    });
    if (focus) tab.focus();
    // On a narrow page the tabs scroll sideways: bring the chosen one into view (without scrolling the page).
    const strip = tab.parentNode as HTMLElement;
    if (tab.offsetLeft < strip.scrollLeft) strip.scrollLeft = tab.offsetLeft;
    else if (tab.offsetLeft + tab.offsetWidth > strip.scrollLeft + strip.clientWidth) {
      strip.scrollLeft = tab.offsetLeft + tab.offsetWidth - strip.clientWidth;
    }
  }
  const current = () => tabs.filter(t => t.getAttribute('aria-selected') === 'true')[0];

  function badge(tab: HTMLElement | null, text: string, isNew?: boolean) {
    const b = tab && tab.querySelector<HTMLElement>('.badge');
    if (!b) return;
    b.hidden = !text;
    b.textContent = text || '';
    b.classList.toggle('new', !!isNew);
  }

  // Keeps the tabs in step with the panels: a panel that hides itself takes its tab with it.
  function sync() {
    tabs.forEach(t => { t.hidden = panel(t).hidden; });
    const news = byId('news');
    const fresh = !news.hidden && news.classList.contains('fresh');
    badge(tabFor('news'), fresh ? 'New' : '', true);
    const passportCount = byId('passport-count').textContent || '';
    badge(tabFor('passport'), parseInt(passportCount, 10) > 0 ? passportCount : '');
    const saved = parseInt(byId('records-count').textContent || '', 10);
    badge(tabFor('records'), saved > 0 ? String(saved) : '');

    let on: HTMLElement | null | undefined = current();
    if (fresh && !chosenByVisitor) on = tabFor('news');
    if (!on || on.hidden) {
      const want = tabFor(remembered() || 'passport');
      on = want && !want.hidden ? want : visible()[0];
    }
    if (on) select(on, false);
  }

  tabs.forEach(t => {
    t.addEventListener('click', () => {
      chosenByVisitor = true;
      try { localStorage.setItem(KEY, t.getAttribute('aria-controls') as string); } catch (e) { /* not kept */ }
      select(t, false);
    });
    t.addEventListener('keydown', e => {
      const list = visible();
      const i = list.indexOf(t);
      const next = e.key === 'ArrowRight' ? list[(i + 1) % list.length]
        : e.key === 'ArrowLeft' ? list[(i - 1 + list.length) % list.length]
        : e.key === 'Home' ? list[0] : e.key === 'End' ? list[list.length - 1] : null;
      if (!next) return;
      e.preventDefault();
      next.click();
      next.focus();
    });
  });

  const watch = new MutationObserver(sync);
  tabs.forEach(t => watch.observe(panel(t), { attributes: true, attributeFilter: ['hidden', 'class'] }));
  ['passport-count', 'records-count'].forEach(id => {
    watch.observe(byId(id), { childList: true, characterData: true, subtree: true });
  });
  sync();

  // For code that sends the visitor to a panel (saves.js, with saves from a link).
  window.oldjavaArcade = { show: id => { const t = tabFor(id); if (t && !t.hidden) select(t, false); } };
}
