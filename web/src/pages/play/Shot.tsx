// The poster's picture, with the app's preview clip (clips/<App>.mp4, the gallery's) laid over it, playing silently
// while the widget is on screen: a page with the widget on it shows the app moving before anyone clicks. The clip is
// fetched only once the widget is in view. The picture stays still for reduced motion, Save-Data, the Then poster
// and the apps without a clip, and if the clip does not load.
import { useEffect, useRef, useState, type CSSProperties } from 'react';
import type { App } from '../../shared/catalog';

const moves = () => !matchMedia('(prefers-reduced-motion: reduce)').matches &&
  !(navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData &&
  !!document.createElement('video').canPlayType('video/mp4');

export function Shot({ app, still, shown, onPlay }: { app: App | null; still: boolean; shown: boolean; onPlay: () => void }) {
  const img = useRef<HTMLImageElement>(null);
  const video = useRef<HTMLVideoElement>(null);
  const [live] = useState(() => !!app?.clip && !still && moves());
  const [box, setBox] = useState<CSSProperties>({});
  const [on, setOn] = useState(false);
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    if (!live || broken || !shown) return;
    const i = img.current!;
    const v = video.current!;
    // The clip takes the picture's place, wherever the poster's layout puts it: centred on it and as big as fits in
    // it, keeping the clip's own shape (a 16:10 recording of the desk, the picture is the app's window).
    const place = () => {
      const w = i.offsetWidth, h = i.offsetHeight;
      const s = v.videoWidth ? Math.min(w / v.videoWidth, h / v.videoHeight) : 0;
      const cw = s ? Math.round(v.videoWidth * s) : w, ch = s ? Math.round(v.videoHeight * s) : h;
      setBox({ left: i.offsetLeft + (w - cw) / 2, top: i.offsetTop + (h - ch) / 2, width: cw, height: ch });
    };
    v.onloadedmetadata = place;
    const ro = new ResizeObserver(place);
    ro.observe(i);
    ro.observe(i.parentElement!);
    const io = new IntersectionObserver(([e]) => {
      if (e.isIntersecting) v.play().catch(() => {});
      else v.pause();
    }, { threshold: 0.5 });
    io.observe(i);
    return () => { ro.disconnect(); io.disconnect(); v.pause(); };
  }, [live, broken, shown]);

  return (
    <>
      <img className={'shot' + (on ? ' covered' : '')} id="poster-shot" alt={app ? app.alt : ''} src={app ? app.shot : undefined} ref={img}
        hidden={still} onClick={onPlay} />
      {live && !broken && (
        <video className={'live' + (on ? ' on' : '')} ref={video} muted loop playsInline preload="none" aria-hidden="true"
          src={'clips/' + app!.id + '.mp4'} style={box} onClick={onPlay}
          onPlaying={() => setOn(true)} onError={() => setBroken(true)} />
      )}
    </>
  );
}
