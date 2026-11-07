import { siteRoot } from './install';
import { reducedMotion } from './band-news';
import { drawDesk } from './replay';
import { appId, catalog } from './catalog';
import { closeButton, indexOf, ownKeys } from './then-and-now';
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
  var srcUrl = URL.createObjectURL(photo.blob), outUrl: string | null = null, outBlob: Blob | null = null;
  var img = new Image(), shot: HTMLImageElement | null = null;
  var info: PhotoInfo = { name: photo.app, at: photo.at, shot: null };
  var frame = 'instant';
  try { frame = localStorage.getItem(PHOTO_FRAME_KEY) || frame; } catch (e) { /* the first frame */ }
  if (!PHOTO_FRAMES.some(function (f) { return f[0] === frame; })) frame = 'instant';
  var scrim = document.createElement('div');
  scrim.className = 'shim-scrim';
  var panel = document.createElement('div');
  panel.className = 'shim-share shim-photo';
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-modal', 'true');
  var head = document.createElement('div');
  head.className = 'shim-drawer-head';
  var title = document.createElement('strong');
  function shut() {
    playSound('close');
    URL.revokeObjectURL(srcUrl);
    if (outUrl) URL.revokeObjectURL(outUrl);
    scrim.remove();
    panel.remove();
    photoBox = null;
    if (opener && opener.isConnected) opener.focus();
  }
  head.appendChild(title);
  head.appendChild(closeButton(shut));
  panel.appendChild(head);
  var body = document.createElement('div');
  body.className = 'shim-share-body';
  var stage = document.createElement('div');
  stage.className = 'shim-photo-stage';
  var print = document.createElement('img');
  print.className = 'shim-photo-print';
  stage.appendChild(print);
  var frames = document.createElement('div');
  frames.className = 'shim-photo-frames';
  frames.setAttribute('role', 'group');
  frames.setAttribute('aria-label', 'Frame');
  var frameButtons = PHOTO_FRAMES.map(function (f) {
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'shim-share-way';
    btn.textContent = f[1];
    btn.addEventListener('click', function () {
      frame = f[0];
      try { localStorage.setItem(PHOTO_FRAME_KEY, frame); } catch (e) { /* not remembered */ }
      develop();
    });
    frames.appendChild(btn);
    return btn;
  });
  var capLabel = document.createElement('label');
  capLabel.className = 'shim-share-msg';
  capLabel.textContent = 'Caption';
  var cap = document.createElement('input');
  cap.type = 'text';
  cap.maxLength = 60;
  cap.className = 'shim-photo-caption';
  capLabel.appendChild(cap);
  var ways = document.createElement('div');
  ways.className = 'shim-share-ways';
  var save = document.createElement('a');
  save.className = 'shim-share-way primary';
  save.textContent = 'Save picture';
  save.setAttribute('data-sound', 'none');
  save.addEventListener('click', function (e) {
    if (!outUrl) { e.preventDefault(); return; }
    playSound('saved');
  });
  ways.appendChild(save);
  var copy: HTMLButtonElement | null = null;
  if (navigator.clipboard && navigator.clipboard.write && window.ClipboardItem) {
    copy = document.createElement('button');
    copy.type = 'button';
    copy.className = 'shim-share-way';
    copy.textContent = 'Copy picture';
    copy.addEventListener('click', function () {
      if (!outBlob) return;
      navigator.clipboard.write([new ClipboardItem({ 'image/png': outBlob })]).then(function () {
        copy!.textContent = 'Copied';
        playSound('saved');
      }, function () { copy!.textContent = 'Could not copy'; });
    });
    ways.appendChild(copy);
  }
  var when = new Date(photo.at);   // the visitor's own clock
  var stamp = [when.getFullYear(), when.getMonth() + 1, when.getDate(), '-', when.getHours(), when.getMinutes()]
    .map(function (n) { return typeof n === 'number' && n < 10 ? '0' + n : n; }).join('');
  save.download = photo.app + '-photo-' + stamp + '.png';
  var native = null;
  var link = siteRoot + photo.app + '/index.html';
  function fileOf(blob: Blob) { return window.File ? new File([blob], save.download, { type: 'image/png' }) : null; }
  if (navigator.canShare && window.File && navigator.canShare({ files: [fileOf(photo.blob)!] })) {
    native = document.createElement('button');
    native.type = 'button';
    native.className = 'shim-share-way';
    native.textContent = 'Share…';
    native.addEventListener('click', function () {
      if (!outBlob) return;
      navigator.share({ files: [fileOf(outBlob)!], title: title.textContent, text: info.name + ', played in my browser: ' + link })
        .catch(function () { /* closed */ });
    });
    ways.appendChild(native);
  }
  var note = document.createElement('p');
  note.className = 'shim-clip-note';
  note.textContent = 'The picture stays on this device until you save, copy or share it.';
  [stage, frames, capLabel, ways, note].forEach(function (el) { body.appendChild(el); });
  panel.appendChild(body);
  scrim.addEventListener('click', shut);
  ownKeys(panel, shut);
  document.body.appendChild(scrim);
  document.body.appendChild(panel);
  photoBox = panel;
  playSound('open');
  var pending = 0;
  // Draws the print again (a new frame, a changed caption) and gets its file ready for the buttons.
  function develop() {
    frameButtons.forEach(function (btn, i) {
      var on = PHOTO_FRAMES[i][0] === frame;
      btn.setAttribute('aria-pressed', String(on));
      btn.classList.toggle('primary', on);
    });
    capLabel.hidden = frame === 'plain';
    if (!img.naturalWidth) return;
    var made = composePhoto(img, frame, cap.value.trim() || info.name, info);
    var mine = ++pending;
    made.toBlob(function (blob) {
      if (mine !== pending || !blob || photoBox !== panel) return;
      if (outUrl) URL.revokeObjectURL(outUrl);
      outBlob = blob;
      outUrl = URL.createObjectURL(blob);
      if (!print.src) print.classList.add('developing');   // the first print comes up as an instant print does
      print.src = outUrl;
      print.alt = 'Photo of ' + info.name + (frame === 'plain' ? '' : ': ' + (cap.value.trim() || info.name));
      save.href = outUrl;
      if (copy) copy.textContent = 'Copy picture';
    }, 'image/png');
  }
  var typing: number | ReturnType<typeof setTimeout> = 0;
  cap.addEventListener('input', function () {
    cap.dataset.touched = '1';
    clearTimeout(typing);
    typing = setTimeout(develop, 120);
  });
  function describe(name: string) {
    info.name = name;
    title.textContent = 'Photo: ' + name;
    panel.setAttribute('aria-label', title.textContent);
    if (!cap.dataset.touched) cap.value = name + ', ' + photoDay(photo.at);
  }
  describe(photo.app);
  develop();
  img.onload = develop;
  img.src = srcUrl;
  catalog().then(function (apps: OldjavaCatalogApp[]) {
    var app = apps[indexOf(apps, photo.app)];
    if (!app || photoBox !== panel) return;
    describe(app.name);
    shot = new Image();
    shot.onload = function () { info.shot = shot; if (photoBox === panel) develop(); };
    shot.src = app.shot;
    develop();
  });
  save.focus();
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
