// ---- preview clips: the gallery's thumbnails come alive ----
// A card with data-clip has a few seconds of its app at clips/<App>.mp4, recorded by web/clips.ps1 from the app
// running in the browser (the four games with a computer player play themselves). The clip is fetched only
// when it is wanted: while the mouse is over the card (after a short pause, so sweeping across the gallery
// does not start them all) or the keyboard focus is in it, and on touch screens while the card sits in the
// middle of the screen. One plays at a time, silent, looping. Visitors who ask for reduced motion, or for less
// data on touch screens, keep the still thumbnails.
import { all, one } from '../shared/dom';

export function initClips(): void {
  const cards = all<HTMLLIElement>('#apps li[data-clip]');
  if (!cards.length || !document.createElement('video').canPlayType('video/mp4')) return;
  const still = matchMedia('(prefers-reduced-motion: reduce)');
  const pointer = matchMedia('(hover: hover) and (pointer: fine)');
  let current: HTMLLIElement | null = null;
  let wait = 0;

  function clipOf(li: HTMLLIElement) {
    const thumb = one('a.thumb', li);
    let video = thumb.querySelector('video');
    if (video) return video;
    video = document.createElement('video');
    video.muted = true;
    video.loop = true;
    video.playsInline = true;
    video.preload = 'none';
    video.setAttribute('aria-hidden', 'true');
    video.addEventListener('playing', () => { if (current === li) li.classList.add('previewing'); });
    video.addEventListener('error', () => { li.removeAttribute('data-clip'); stop(li); });
    video.src = 'clips/' + li.getAttribute('data-app') + '.mp4';
    const cue = document.createElement('span');
    cue.className = 'clip-cue';
    cue.setAttribute('aria-hidden', 'true');
    cue.textContent = '▶ Preview';
    thumb.appendChild(video);
    thumb.appendChild(cue);
    return video;
  }
  function play(li: HTMLLIElement) {
    clearTimeout(wait);
    if (still.matches || li === current || !li.hasAttribute('data-clip') || li.hidden) return;
    if (current) stop(current);
    current = li;
    const video = clipOf(li);
    try { video.currentTime = 0; } catch (e) { /* not loaded yet */ }
    video.play().catch(() => { if (current === li) stop(li); });
  }
  function stop(li: HTMLLIElement) {
    li.classList.remove('previewing');
    const video = li.querySelector<HTMLVideoElement>('a.thumb video');
    if (video) video.pause();
    if (current === li) current = null;
  }
  function later(li: HTMLLIElement) {
    clearTimeout(wait);
    wait = window.setTimeout(() => play(li), 150);
  }

  cards.forEach(li => {
    li.addEventListener('mouseenter', () => { if (pointer.matches) later(li); });
    li.addEventListener('mouseleave', () => { clearTimeout(wait); if (pointer.matches) stop(li); });
    li.addEventListener('focusin', () => { if (li.querySelector(':focus-visible')) play(li); });
    li.addEventListener('focusout', e => { if (!li.contains(e.relatedTarget as Node | null)) stop(li); });
  });

  // Touch screens: the card crossing the middle fifth of the screen plays.
  const saveData = navigator.connection && navigator.connection.saveData;
  if (window.IntersectionObserver && !saveData) {
    const middle = new IntersectionObserver(entries => {
      if (pointer.matches) return;
      entries.forEach(entry => {
        const li = entry.target as HTMLLIElement;
        if (entry.isIntersecting) later(li);
        else if (li === current) stop(li);
      });
    }, { rootMargin: '-40% 0px -40% 0px' });
    cards.forEach(li => middle.observe(li));
  }
  // A filter that hides the playing card, or a hidden page, stops the clip.
  document.addEventListener('visibilitychange', () => { if (document.hidden && current) stop(current); });
  document.addEventListener('click', e => {
    const target = e.target as Element;
    if (current && target.closest && target.closest('.filters') && current.hidden) stop(current);
  });
}
