import { useLayoutEffect, useRef, useState, type ReactNode, type RefObject } from 'react';
import { flushSync } from 'react-dom';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';

export interface BandNews {
  /** kind: 'hint', 'stamp', ...; lead: kept at the start; text: may scroll; actions: links or buttons kept in view after it. */
  show(k: string, lead: ReactNode, text: ReactNode, actions: ReactNode, ms: number): void;
  clear(): void;
  kind(): string | null;
  onKind(fn: (k: string | null) => void): void;
}

export let news: BandNews | null;

// ---- band news (app pages): nothing floats over the stage ----
// The toolbar band's name slot slides the app's name up and a news line in: How to play when the app starts
// (or on the band's ? button) and a new passport stamp. A line longer than the slot scrolls through like a
// marquee, twice, then the name comes back. Pointing at the line holds it; x or Escape closes it.
export function setNews(v: BandNews | null) { news = v; }

/** What the line says: [kind, lead, text, actions]; it keeps saying it, hidden, once the slot's data-news goes. */
type Said = [string, ReactNode, ReactNode, ReactNode];

/**
 * The parts of the line the band works with (the text's box and track exist once it has said something), and title(t),
 * which gives the text's box its title (the whole text, when it is too long to scroll) or takes it away.
 */
interface LineParts {
  line: RefObject<HTMLDivElement | null>; box: RefObject<HTMLSpanElement | null>; track: RefObject<HTMLSpanElement | null>;
  title(t: string | null): void;
}

// mark(on): the slot shows the line (its data-news, which the toolbar draws), at once, so the line can be measured.
export function bandNews(slot: Element, mark: (on: boolean) => void): BandNews {
  var on = false, timer: ReturnType<typeof setTimeout> | 0 = 0, deadline = 0, anim: Animation | null = null, listeners: ((k: string | null) => void)[] = [];
  var say: ((s: Said) => void) | null = null, parts: LineParts | null = null;
  function told() { var k = kind(); listeners.forEach(function (fn) { fn(k); }); }
  function kind() { return on ? parts!.line.current!.getAttribute('data-kind') : null; }
  function clear() {
    clearTimeout(timer);
    if (anim) anim.cancel();
    anim = null;
    if (!on) return;
    on = false;
    mark(false);
    told();
  }
  function wait(ms: number) {
    clearTimeout(timer);
    deadline = Date.now() + ms;
    timer = setTimeout(clear, ms);
  }
  function enter() {
    if (!kind()) return;
    clearTimeout(timer);
    deadline = Math.max(0, deadline - Date.now());   // now the time that was left
    if (anim) anim.pause();
  }
  function leave() {
    if (!kind()) return;
    if (anim) anim.play();
    wait(Math.max(deadline, 3000));
  }
  openLayer(<NewsLine onEnter={enter} onLeave={leave} onClear={clear} onMount={(s, p) => { say = s; parts = p; }} />, slot);
  return {
    show: function (k: string, lead: ReactNode, text: ReactNode, actions: ReactNode, ms: number) {
      clear();
      say!([k, lead, text, actions]);
      on = true;
      mark(true);
      told();
      var box = parts!.box.current!, track = parts!.track.current!;
      parts!.title(null);
      var over = track.scrollWidth - box.clientWidth;
      if (over > 4 && track.animate && !reducedMotion()) {
        var hold = 1500, ride = over / 45 * 1000, one = hold + ride + hold;
        anim = track.animate([
          { transform: 'translateX(0)', offset: 0 },
          { transform: 'translateX(0)', offset: hold / one },
          { transform: 'translateX(' + -over + 'px)', offset: (hold + ride) / one },
          { transform: 'translateX(' + -over + 'px)', offset: 1 }
        ], { duration: one, iterations: 2 });
        ms = Math.max(ms, 2 * one + 1000);
      } else if (over > 4) {
        parts!.title(track.textContent!);
      }
      wait(ms);
    },
    clear: clear,
    kind: kind,
    onKind: function (fn: (k: string | null) => void) { listeners.push(fn); }
  };
}

function NewsLine({ onEnter, onLeave, onClear, onMount }: {
  onEnter: () => void; onLeave: () => void; onClear: () => void; onMount: (say: (s: Said) => void, parts: LineParts) => void;
}) {
  const line = useRef<HTMLDivElement>(null);
  const box = useRef<HTMLSpanElement>(null);
  const track = useRef<HTMLSpanElement>(null);
  const [said, setSaid] = useState<Said | null>(null);
  const [title, setTitle] = useState<string | null>(null);
  useOwnKeys(line, onClear);
  useLayoutEffect(() => {
    // Drawn at once, so the band can measure the text for its marquee.
    onMount(s => flushSync(() => setSaid(s)), { line, box, track, title: t => flushSync(() => setTitle(t)) });
    // The pointer's own enter and leave (React's onPointerEnter is made from pointerover and pointerout).
    const el = line.current!;
    el.addEventListener('pointerenter', onEnter);
    el.addEventListener('pointerleave', onLeave);
    return () => { el.removeEventListener('pointerenter', onEnter); el.removeEventListener('pointerleave', onLeave); };
  }, []);
  return (
    <div className="shim-news" role="status" data-kind={said ? said[0] : undefined} ref={line}>
      {said && (
        <>
          {said[1]}
          <span className="shim-news-text" title={title ?? undefined} ref={box}><span className="shim-news-track" ref={track}>{said[2]}</span></span>
          {said[3]}
          <CloseButton onClick={onClear} />
        </>
      )}
    </div>
  );
}

export function reducedMotion() {
  return !!((window.matchMedia as typeof matchMedia | undefined) && matchMedia('(prefers-reduced-motion: reduce)').matches);
}

export function init() {
  news = null;
}
