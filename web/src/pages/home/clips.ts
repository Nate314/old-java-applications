// ---- preview clips: the gallery's thumbnails come alive ----
// A card with a clip has a few seconds of its app at clips/<App>.mp4, recorded by web/clips.ps1 from the app
// running in the browser (the four games with a computer player play themselves). The clip is fetched only
// when it is wanted: while the mouse is over the card (after a short pause, so sweeping across the gallery
// does not start them all) or the keyboard focus is in it, and on touch screens while the card sits in the
// middle of the screen. One plays at a time, silent, looping. Visitors who ask for reduced motion, or for less
// data on touch screens, keep the still thumbnails. Which card plays, and which have their video, is in H.clip.
import type { FocusEvent as ReactFocusEvent } from 'react';
import { APPS } from '../../shared/catalog';
import { H, draw } from './model';

const still = matchMedia('(prefers-reduced-motion: reduce)');
const pointer = matchMedia('(hover: hover) and (pointer: fine)');
const canPlay = !!document.createElement('video').canPlayType('video/mp4');
const clip = H.clip;
let wait = 0;

/** Whether app id's card has a clip (until its clip turns out not to load). */
export function hasClip(id: string) { return canPlay && !!APPS.find(a => a.id === id)?.clip && !clip.broken[id]; }
const card = (id: string) => document.querySelector<HTMLLIElement>('#apps li[data-app="' + id + '"]')!;
const videoOf = (id: string) => card(id).querySelector<HTMLVideoElement>('a.thumb video');
const shown = (id: string) => H.filter === 'all' || APPS.find(a => a.id === id)!.cat === H.filter;

function play(id: string) {
  clearTimeout(wait);
  if (still.matches || id === clip.current || !hasClip(id) || !shown(id)) return;
  if (clip.current) stop(clip.current);
  clip.current = id;
  clip.wanted[id] = true;
  draw();
  const video = videoOf(id)!;
  try { video.currentTime = 0; } catch (e) { /* not loaded yet */ }
  video.play().catch(() => { if (clip.current === id) stop(id); });
}
function stop(id: string) {
  const video = videoOf(id);
  if (video) video.pause();
  if (clip.current === id) { clip.current = null; clip.playing = false; }
  draw();
}
function later(id: string) {
  clearTimeout(wait);
  wait = window.setTimeout(() => play(id), 150);
}

/** The clip's video has started: the card shows it. */
export function clipPlaying(id: string) { if (clip.current === id) { clip.playing = true; draw(); } }
/** The clip did not load: the card keeps its picture from now on. */
export function clipBroken(id: string) { clip.broken[id] = true; stop(id); }

// The card's own events.
export function cardEnter(id: string) { if (hasClip(id) && pointer.matches) later(id); }
export function cardLeave(id: string) { if (!hasClip(id)) return; clearTimeout(wait); if (pointer.matches) stop(id); }
export function cardFocus(e: ReactFocusEvent<HTMLLIElement>, id: string) { if (hasClip(id) && e.currentTarget.querySelector(':focus-visible')) play(id); }
export function cardBlur(e: ReactFocusEvent<HTMLLIElement>, id: string) {
  if (hasClip(id) && !e.currentTarget.contains(e.relatedTarget as Node | null)) stop(id);
}

export function initClips(): void {
  if (!canPlay || !APPS.some(a => a.clip)) return;
  // Touch screens: the card crossing the middle fifth of the screen plays.
  const saveData = navigator.connection && navigator.connection.saveData;
  if (window.IntersectionObserver && !saveData) {
    const middle = new IntersectionObserver(entries => {
      if (pointer.matches) return;
      entries.forEach(entry => {
        const id = (entry.target as HTMLElement).getAttribute('data-app') as string;
        if (entry.isIntersecting) later(id);
        else if (id === clip.current) stop(id);
      });
    }, { rootMargin: '-40% 0px -40% 0px' });
    APPS.filter(a => a.clip).forEach(a => middle.observe(card(a.id)));
  }
  // A filter that hides the playing card, or a hidden page, stops the clip.
  document.addEventListener('visibilitychange', () => { if (document.hidden && clip.current) stop(clip.current); });
  document.addEventListener('click', e => {
    const target = e.target as Element;
    if (clip.current && target.closest && target.closest('.filters') && !shown(clip.current)) stop(clip.current);
  });
}
