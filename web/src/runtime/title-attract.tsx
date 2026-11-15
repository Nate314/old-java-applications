import { useEffect, useRef, useState, type MouseEvent } from 'react';
import { root } from './catalog';

// The title screen's attract mode: like an arcade cabinet showing its game before a coin goes in, the picture panel
// plays the app's preview clip (clips/<App>.mp4, the gallery's), silent and looping. It is fetched only once the
// app's own files are in (go), so it never slows the start. The still picture stays for reduced motion, Save-Data,
// apps without a clip, and when the clip does not load (offline, say). Moving content needs a way to stop it: the
// Pause chip in the corner (a click there does not start the app).
const moves = () => !matchMedia('(prefers-reduced-motion: reduce)').matches &&
  !(navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData &&
  !!document.createElement('video').canPlayType('video/mp4');

export function Attract({ id, go }: { id: string; go: boolean }) {
  const video = useRef<HTMLVideoElement>(null);
  const [live] = useState(moves);
  const [on, setOn] = useState(false);   // it has started: the picture under it fades out
  const [paused, setPaused] = useState(false);
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    if (go && live) video.current?.play().catch(() => setBroken(true));
  }, [go]);
  if (!live || broken) return null;

  function toggle(e: MouseEvent) {
    e.stopPropagation();   // the title screen starts the app on any other click
    const v = video.current!;
    if (v.paused) v.play().catch(() => setBroken(true)); else v.pause();
  }
  return (
    <>
      <video className={'shim-title-clip' + (on ? ' on' : '')} ref={video} muted loop playsInline preload="none" aria-hidden="true"
        src={root() + 'clips/' + id + '.mp4'} onPlaying={() => { setOn(true); setPaused(false); }} onPause={() => setPaused(true)} onError={() => setBroken(true)} />
      {on && (
        <button type="button" className="shim-title-clip-pause" onClick={toggle}>
          {paused ? '▶ Play preview' : '❚❚ Pause preview'}
        </button>
      )}
    </>
  );
}
