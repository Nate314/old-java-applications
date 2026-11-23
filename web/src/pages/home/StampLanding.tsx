// The stamp lands where the visitor is playing: the app's first stamp comes down on the corner of the player's
// screen, with the count and a way to the passport. Before, it landed only on the passport page under Your arcade,
// which a first visit keeps below the gallery on What's new, so a first play never saw it.
import { useEffect, useState } from 'react';
import { APPS } from '../../shared/catalog';
import { FIRST, H } from './model';
import { Stamp } from './Stamp';
import { stampCount, tabClick } from './tabs';
import './stamp-landing.css';

const SHOWN_MS = 7000;   // the CSS fades it over its last 0.5 s

/** Opens the passport tab, scrolls to it and lands the new stamp again there, once it is in view. */
export function toPassport() {
  tabClick('passport');
  const box = document.getElementById('passport')!;
  const land = () => box.querySelectorAll('.fresh .seal').forEach(s => s.getAnimations().forEach(an => { an.cancel(); an.play(); }));
  const r = box.getBoundingClientRect();
  if (r.top >= 0 && r.bottom <= innerHeight) { land(); return; }
  if ('onscrollend' in window) addEventListener('scrollend', land, { once: true }); else setTimeout(land, 600);
  box.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
}

export function StampLanding() {
  const a = APPS.find(x => x.id === H.fresh);
  // Each landing by its app and time, so a stamp collected again after a passport reset lands again.
  const id = a ? a.id + '@' + window.oldjavaPassport.when(a.id) : null;
  const [gone, setGone] = useState<string | null>(null);
  useEffect(() => {
    if (!id) return;
    const t = setTimeout(() => setGone(id), SHOWN_MS);
    return () => clearTimeout(t);
  }, [id]);
  // Only the app in the player: a stamp from another tab lands on the passport alone.
  if (!a || gone === id || (H.played || FIRST).id !== a.id) return null;
  const n = stampCount();
  return (
    <div className="stamp-landing" key={id}>
      <span className="land-seal" aria-hidden="true"><Stamp a={a} when={window.oldjavaPassport.when(a.id)} prefix="landing-" /></span>
      <span className="land-note">
        <strong>{'Stamp ' + n + ' of ' + APPS.length}</strong>
        <button type="button" className="btn" onClick={() => { setGone(id); toPassport(); }}>See your passport ↓</button>
      </span>
    </div>
  );
}
