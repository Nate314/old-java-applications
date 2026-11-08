import { useLayoutEffect, useRef, useState } from 'react';
import { siteRoot } from './install';
import { reducedMotion } from './band-news';
import { drawDesk } from './replay';
import { appId, catalog } from './catalog';
import { indexOf } from './then-and-now';
import { CloseButton, openLayer, useOwnKeys } from './ui/layer';
import { playSound } from './sounds';

/** A photo of the app's windows: the PNG, the app's id and when it was taken. */
export type Photo = OldjavaPhotoPrint;
/** What a print says: the app's name, the day, and its thumbnail for the postcard's stamp. */
interface PhotoInfo { name: string; at: number; shot: HTMLImageElement | null }

declare global {
  interface OldjavaPhoto { open(photo: Photo, opener: HTMLElement | null): void }
}

let photoDesk: Element | null;
let photoBox: HTMLElement | null;

// ---- photo booth ----
// A finished Circle Spawner heart, a Paint picture on NathaNOS, a solved cube, a high score: some moments are a
// picture, not a video. Photo (the toolbar's camera, the home page's player bar) draws the app's windows the way
// instant replay does (drawDesk), at up to twice their size, with a shutter flash and click, and shows the print
// in a frame: Instant (a white instant-camera print with a handwritten caption), Postcard ("Greetings from" the
// app, with a stamp of its thumbnail and a postmark of the day) or Plain (the windows, with a camera's orange
// date stamp). Save picture downloads it as a PNG; Copy picture and Share... are offered where the browser can.
// The picture stays in the page unless the visitor saves, copies or shares it. The frame is remembered.
const PHOTO_MAX = 2400;   // the photo's longest side, in pixels

const PHOTO_FRAME_KEY = 'oldjava.photoFrame';

const PHOTO_FRAMES = [['instant', 'Instant'], ['postcard', 'Postcard'], ['plain', 'Plain']];

const PHOTO_HAND = '"Segoe Print", "Bradley Hand", "Comic Sans MS", cursive';

export function setPhotoDesk(v: Element | null) { photoDesk = v; }

// Resolves to { blob (PNG), app, at } of the app's windows now, or null while there is none.
export function takePhoto() {
  var canvas = document.createElement('canvas');
  canvas.width = canvas.height = 16;
  var g = canvas.getContext('2d')!;
  var box = photoDesk && drawDesk(g, canvas, photoDesk);
  if (!box) return Promise.resolve(null);
  var s = Math.min(2, PHOTO_MAX / Math.max(box[0], box[1]));
  canvas.width = Math.round(box[0] * s);
  canvas.height = Math.round(box[1] * s);
  drawDesk(g, canvas, photoDesk!);
  return new Promise<Photo | null>(function (done) {
    canvas.toBlob(function (blob) { done(blob ? { blob: blob, app: appId()!, at: Date.now() } : null); }, 'image/png');
  });
}

// A white flash over el (the app's stage, or the home page's player screen), as from a camera.
function photoFlash(el: Element | null) {
  if (!el || el.ownerDocument !== document || reducedMotion()) return;
  var r = el.getBoundingClientRect();
  var f = document.createElement('div');
  f.className = 'shim-photo-flash';
  f.style.cssText = 'left:' + r.left + 'px;top:' + r.top + 'px;width:' + r.width + 'px;height:' + r.height + 'px';
  document.body.appendChild(f);
  setTimeout(function () { f.remove(); }, 700);
}

// The shutter: flash, click, then the print. take: a function resolving to a photo (takePhoto, or the home page's
// player's); flashOn: the element the flash covers.
export function snapPhoto(take: () => Promise<Photo | null>, flashOn: Element | null, opener: HTMLElement | null) {
  if (photoBox) return;
  take().then(function (photo) {
    if (!photo) return;
    playSound('photo');
    photoFlash(flashOn);
    setTimeout(function () { openPhoto(photo, opener); }, reducedMotion() ? 0 : 260);
  });
}

// Text in a font of size px (shrunk until it fits width), drawn at x, y.
function fitText(g: CanvasRenderingContext2D, text: string, font: string, px: number, width: number) {
  do { g.font = font.replace('{px}', px + 'px'); px *= 0.92; } while (px > 6 && g.measureText(text).width > width);
}

function photoDay(at: number) {
  return new Date(at).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
}

// The stamp: the app's thumbnail on pale paper, with a perforated edge, size px square.
function photoStamp(size: number, shot: HTMLImageElement | null, name: string) {
  var c = document.createElement('canvas');
  c.width = c.height = Math.round(size);
  var g = c.getContext('2d')!, m = size * 0.09;
  g.fillStyle = '#fffaf0';
  g.fillRect(0, 0, size, size);
  g.fillStyle = '#f3d9a8';
  g.fillRect(m, m, size - 2 * m, size - 2 * m);
  if (shot && shot.naturalWidth) {
    var room = size - 2 * m - size * 0.08, h = size * 0.62;
    var s = Math.min(room / shot.naturalWidth, h / shot.naturalHeight);
    var w = shot.naturalWidth * s;
    g.drawImage(shot, (size - w) / 2, m + size * 0.05 + (h - shot.naturalHeight * s) / 2, w, shot.naturalHeight * s);
  }
  g.fillStyle = '#7a2e12';
  g.textAlign = 'center';
  g.textBaseline = 'middle';
  fitText(g, name.toUpperCase(), '800 {px} system-ui, "Segoe UI", sans-serif', size * 0.1, size - 2 * m - 8);
  g.fillText(name.toUpperCase(), size / 2, size - m - size * 0.11);
  // Perforations: half circles bitten out of every edge.
  g.globalCompositeOperation = 'destination-out';
  var n = 9, r = size / n / 3.2;
  for (var i = 0; i <= n; i++) {
    var p = i * size / n;
    [[p, 0], [p, size], [0, p], [size, p]].forEach(function (q) {
      g.beginPath();
      g.arc(q[0], q[1], r, 0, 2 * Math.PI);
      g.fill();
    });
  }
  return c;
}

// The print: img (the photo) in the frame, with the caption. Returns a canvas.
function composePhoto(img: HTMLImageElement, frame: string, caption: string, info: PhotoInfo) {
  var pw = img.naturalWidth, ph = img.naturalHeight;
  var c = document.createElement('canvas'), g = c.getContext('2d')!;
  var m = Math.round(Math.max(pw, ph) * 0.045) + 12;
  if (frame === 'plain') {
    c.width = pw;
    c.height = ph;
    g.drawImage(img, 0, 0);
    var d = new Date(info.at);
    var stamp = "'" + String(d.getFullYear()).slice(2) + ' ' + (d.getMonth() + 1) + ' ' + d.getDate();
    var px = Math.max(14, Math.round(Math.min(pw, ph) * 0.05));
    g.font = '700 ' + px + 'px "Consolas", "Courier New", monospace';
    g.textAlign = 'right';
    g.textBaseline = 'alphabetic';
    g.shadowColor = 'rgba(255, 120, 20, 0.9)';
    g.shadowBlur = px * 0.4;
    g.fillStyle = '#ffb24a';
    g.fillText(stamp, pw - px * 0.9, ph - px * 0.8);
    return c;
  }
  if (frame === 'instant') {
    var foot = Math.round(m * 3.4);
    c.width = pw + 2 * m;
    c.height = ph + m + foot;
    g.fillStyle = '#fbf8f1';
    g.fillRect(0, 0, c.width, c.height);
    g.drawImage(img, m, m);
    g.strokeStyle = 'rgba(0, 0, 0, 0.14)';
    g.lineWidth = 1;
    g.strokeRect(m - 0.5, m - 0.5, pw + 1, ph + 1);
    g.fillStyle = '#26306b';
    g.textAlign = 'center';
    g.textBaseline = 'middle';
    fitText(g, caption, '{px} ' + PHOTO_HAND, Math.round(foot * 0.34), c.width - 2 * m);
    g.fillText(caption, c.width / 2, ph + m + foot * 0.48);
    return c;
  }
  // Postcard: the photo on a cream card, "Greetings from" over its lower left, a stamp and a postmark at the top
  // right, the caption written along the bottom.
  var b = Math.round(m * 0.7), strip = Math.round(m * 1.9);
  c.width = pw + 2 * b;
  c.height = ph + b + strip;
  g.fillStyle = '#f7efdf';
  g.fillRect(0, 0, c.width, c.height);
  g.drawImage(img, b, b);
  var shade = g.createLinearGradient(0, b + ph * 0.55, 0, b + ph);
  shade.addColorStop(0, 'rgba(10, 8, 30, 0)');
  shade.addColorStop(1, 'rgba(10, 8, 30, 0.55)');
  g.fillStyle = shade;
  g.fillRect(b, b + ph * 0.55, pw, ph * 0.45);
  var big = Math.min(ph * 0.2, pw * 0.16);
  g.textAlign = 'left';
  g.textBaseline = 'alphabetic';
  g.fillStyle = '#fff';
  g.shadowColor = 'rgba(0, 0, 0, 0.6)';
  g.shadowBlur = big * 0.12;
  fitText(g, 'Greetings from', '{px} ' + PHOTO_HAND, Math.round(big * 0.42), pw * 0.6);
  g.fillText('Greetings from', b + big * 0.4, b + ph - big * 1.25);
  fitText(g, info.name.toUpperCase(), '900 {px} "Arial Black", "Segoe UI Black", Impact, sans-serif', Math.round(big), pw * 0.86);
  var top = b + ph - big * 0.3;
  var gold = g.createLinearGradient(0, top - big, 0, top);
  gold.addColorStop(0, '#ffe36e');
  gold.addColorStop(1, '#ff7a2f');
  g.lineJoin = 'round';
  g.lineWidth = Math.max(2, big * 0.09);
  g.strokeStyle = '#1b1240';
  g.strokeText(info.name.toUpperCase(), b + big * 0.4, top);
  g.shadowColor = 'transparent';
  g.fillStyle = gold;
  g.fillText(info.name.toUpperCase(), b + big * 0.4, top);
  var size = Math.min(pw, ph) * 0.22;
  g.save();
  g.shadowColor = 'rgba(0, 0, 0, 0.35)';
  g.shadowBlur = size * 0.06;
  g.shadowOffsetY = size * 0.02;
  g.drawImage(photoStamp(size, info.shot, info.name), b + pw - size - b * 0.6, b + b * 0.6);
  g.restore();
  // The postmark: two rings with the date, and wavy lines running off to the left over the photo.
  var cx = b + pw - size - b * 0.6, cy = b + b * 0.6 + size * 0.62, R = size * 0.42;
  g.strokeStyle = 'rgba(30, 24, 60, 0.72)';
  g.fillStyle = 'rgba(30, 24, 60, 0.72)';
  g.lineWidth = Math.max(1.5, size * 0.022);
  [R, R * 0.8].forEach(function (r) { g.beginPath(); g.arc(cx, cy, r, 0, 2 * Math.PI); g.stroke(); });
  g.textAlign = 'center';
  g.textBaseline = 'middle';
  var d2 = new Date(info.at);
  g.font = '700 ' + Math.round(R * 0.26) + 'px "Courier New", monospace';
  g.fillText(d2.toLocaleDateString('en', { month: 'short' }).toUpperCase() + ' ' + d2.getDate(), cx, cy - R * 0.17);
  g.fillText(String(d2.getFullYear()), cx, cy + R * 0.2);
  for (var w = 0; w < 4; w++) {
    var y = cy - R * 0.45 + w * R * 0.3;
    g.beginPath();
    for (var x = cx - R * 1.1; x >= cx - R * 3.4; x -= 2) g.lineTo(x, y + Math.sin(x / (R * 0.18)) * R * 0.06);
    g.stroke();
  }
  g.fillStyle = '#4a3a2a';
  g.textAlign = 'right';
  g.textBaseline = 'middle';
  fitText(g, caption, '{px} ' + PHOTO_HAND, Math.round(strip * 0.42), pw);
  g.fillText(caption, b + pw, b + ph + strip / 2);
  return c;
}

function openPhoto(photo: Photo, opener: HTMLElement | null) {
  if (photoBox) return;
  if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(function () {});
  var urls: { src: string; out: string | null } = { src: URL.createObjectURL(photo.blob), out: null };
  function shut() {
    playSound('close');
    URL.revokeObjectURL(urls.src);
    if (urls.out) URL.revokeObjectURL(urls.out);
    close();
    photoBox = null;
    if (opener && opener.isConnected) opener.focus();
  }
  var save: HTMLElement | null = null;
  const close = openLayer(<PhotoPanel photo={photo} urls={urls} onClose={shut} onPanel={(el, s) => { photoBox = el; save = s; }} />);
  playSound('open');
  save!.focus();
}

/** A developed print: its file, its address (Save picture) and what it shows. */
interface Print { blob: Blob; url: string; alt: string }

/**
 * The print's panel: the photo in its frame, the frames to pick from, the caption, Save picture, Copy picture and
 * Share... The print is drawn again (composePhoto) for a new frame, a changed caption, or once the app's thumbnail
 * (the postcard's stamp) arrives.
 */
function PhotoPanel({ photo, urls, onClose, onPanel }: {
  photo: Photo; urls: { src: string; out: string | null }; onClose: () => void; onPanel: (el: HTMLElement, save: HTMLElement) => void;
}) {
  const panel = useRef<HTMLDivElement>(null);
  const save = useRef<HTMLAnchorElement>(null);
  const cap = useRef<HTMLInputElement>(null);
  const [frame, setFrame] = useState(() => {
    var f = 'instant';
    try { f = localStorage.getItem(PHOTO_FRAME_KEY) || f; } catch (e) { /* the first frame */ }
    return PHOTO_FRAMES.some(function (o) { return o[0] === f; }) ? f : 'instant';
  });
  const [name, setName] = useState(photo.app);   // the app's name, once the catalog says
  const [print, setPrint] = useState<Print | null>(null);
  const [copied, setCopied] = useState<string | null>(null);
  // What drawing needs between renders: the photo, the stamp's thumbnail, the print's name and the latest request.
  const lab = useRef({ img: new Image(), info: { name: photo.app, at: photo.at, shot: null } as PhotoInfo, pending: 0, typing: 0 as number | ReturnType<typeof setTimeout>, frame }).current;
  lab.frame = frame;
  useOwnKeys(panel, onClose);
  useLayoutEffect(() => { onPanel(panel.current!, save.current!); }, []);

  // Draws the print again and gets its file ready for the buttons.
  function develop() {
    if (!lab.img.naturalWidth) return;
    var caption = cap.current!.value.trim() || lab.info.name;
    var made = composePhoto(lab.img, lab.frame, caption, lab.info);
    var mine = ++lab.pending, f = lab.frame;
    made.toBlob(function (blob) {
      if (mine !== lab.pending || !blob || !panel.current) return;
      if (urls.out) URL.revokeObjectURL(urls.out);
      urls.out = URL.createObjectURL(blob);
      setPrint({ blob, url: urls.out, alt: 'Photo of ' + lab.info.name + (f === 'plain' ? '' : ': ' + caption) });
      setCopied(null);
    }, 'image/png');
  }
  function describe(name: string) {
    lab.info.name = name;
    setName(name);
    if (!cap.current!.dataset.touched) cap.current!.value = name + ', ' + photoDay(photo.at);
  }
  useLayoutEffect(() => {
    describe(photo.app);
    lab.img.onload = develop;
    lab.img.src = urls.src;
    catalog().then(function (apps: OldjavaCatalogApp[]) {
      var app = apps[indexOf(apps, photo.app)];
      if (!app || !panel.current) return;
      describe(app.name);
      var shot = new Image();
      shot.onload = function () { lab.info.shot = shot; if (panel.current) develop(); };
      shot.src = app.shot;
      develop();
    });
  }, []);
  useLayoutEffect(develop, [frame]);

  function pick(f: string) {
    try { localStorage.setItem(PHOTO_FRAME_KEY, f); } catch (e) { /* not remembered */ }
    setFrame(f);
  }
  const when = new Date(photo.at);   // the visitor's own clock
  const stamp = [when.getFullYear(), when.getMonth() + 1, when.getDate(), '-', when.getHours(), when.getMinutes()]
    .map(function (n) { return typeof n === 'number' && n < 10 ? '0' + n : n; }).join('');
  const download = photo.app + '-photo-' + stamp + '.png';
  const fileOf = (blob: Blob) => window.File ? new File([blob], download, { type: 'image/png' }) : null;
  const canShare = useState(() => !!(navigator.canShare && window.File && navigator.canShare({ files: [fileOf(photo.blob)!] })))[0];
  const canCopy = !!(navigator.clipboard && 'write' in navigator.clipboard && window.ClipboardItem);
  const title = 'Photo: ' + name;
  return (
    <>
      <div className="shim-scrim" onClick={onClose} />
      <div className="shim-share shim-photo" role="dialog" aria-modal="true" aria-label={title} ref={panel}>
        <div className="shim-drawer-head">
          <strong>{title}</strong>
          <CloseButton onClick={onClose} />
        </div>
        <div className="shim-share-body">
          <div className="shim-photo-stage">
            {/* The first print comes up as an instant print does. */}
            <img className={'shim-photo-print' + (print ? ' developing' : '')} src={print ? print.url : undefined} alt={print ? print.alt : undefined} />
          </div>
          <div className="shim-photo-frames" role="group" aria-label="Frame">
            {PHOTO_FRAMES.map(([f, label]) => (
              <button key={f} type="button" className={'shim-share-way' + (f === frame ? ' primary' : '')} aria-pressed={f === frame} onClick={() => pick(f)}>{label}</button>
            ))}
          </div>
          <label className="shim-share-msg" hidden={frame === 'plain'}>
            Caption
            <input type="text" maxLength={60} className="shim-photo-caption" ref={cap} onInput={() => {
              cap.current!.dataset.touched = '1';
              clearTimeout(lab.typing);
              lab.typing = setTimeout(develop, 120);
            }} />
          </label>
          <div className="shim-share-ways">
            <a className="shim-share-way primary" href={print ? print.url : undefined} download={download} data-sound="none" ref={save}
              onClick={e => { if (!print) { e.preventDefault(); return; } playSound('saved'); }}>Save picture</a>
            {canCopy && (
              <button type="button" className="shim-share-way" onClick={() => {
                if (!print) return;
                navigator.clipboard.write([new ClipboardItem({ 'image/png': print.blob })]).then(function () {
                  setCopied('Copied');
                  playSound('saved');
                }, function () { setCopied('Could not copy'); });
              }}>{copied || 'Copy picture'}</button>
            )}
            {canShare && (
              <button type="button" className="shim-share-way" onClick={() => {
                if (!print) return;
                navigator.share({ files: [fileOf(print.blob)!], title, text: lab.info.name + ', played in my browser: ' + siteRoot + photo.app + '/index.html' })
                  .catch(function () { /* closed */ });
              }}>{'Share…'}</button>
            )}
          </div>
          <p className="shim-clip-note">The picture stays on this device until you save, copy or share it.</p>
        </div>
      </div>
    </>
  );
}

export function init() {
 photoDesk = null;   // the app's desktop, once it runs (not on pretend Windows pages)
  // The print's panel: the photo in its frame, the frames to pick from, the caption, Save picture, Copy picture
  // and Share... Opened by the app page's Photo, and by the home page with its player's photo.
  photoBox = null;
  window.oldjavaPhoto = {
    open: openPhoto,
    can: function () { return !!photoDesk; },
    take: takePhoto,
    snap: snapPhoto
  };
}
