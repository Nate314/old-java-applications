// The passport as a picture (1200 x 630, a link preview's shape): the page of stamps the visitor sees in the Passport
// tab, in the skin's colors, with its count and machine-readable lines, for the Share panel to save or share with
// the link (runtime/share.tsx). Drawn from the stamps in the page (Stamp.tsx), so it is always the page they see.
import { APPS } from '../../shared/catalog';

const W = 1200, H = 630;
// What passport.css gives a stamp's parts: copied onto each part, as a picture made from an SVG sees no stylesheet.
const PROPS = ['fill', 'stroke', 'stroke-width', 'stroke-opacity', 'stroke-dasharray', 'opacity', 'font-family', 'font-size',
  'font-weight', 'letter-spacing', 'text-anchor'];

/** A file as a data: address, for a picture inside an SVG picture (which loads nothing of its own). */
const dataUrl = (href: string) => fetch(new URL(href, document.baseURI).href).then(r => r.blob()).then(b => new Promise<string>((ok, no) => {
  const read = new FileReader();
  read.onload = () => ok(read.result as string);
  read.onerror = no;
  read.readAsDataURL(b);
}));

/** One stamp of the page, as a picture: its SVG with the stylesheet's colors and fonts written in, its app's picture inlined. */
async function stampPicture(svg: SVGSVGElement): Promise<HTMLImageElement> {
  const copy = svg.cloneNode(true) as SVGSVGElement, from = [svg, ...svg.querySelectorAll('*')], to = [copy, ...copy.querySelectorAll('*')];
  from.forEach((el, i) => {
    const css = getComputedStyle(el), out = (to[i] as SVGElement).style;
    if (el === svg) { out.cssText = 'color:' + css.color; return; }   // its tilt and fade are the canvas's (draw)
    PROPS.forEach(p => out.setProperty(p, css.getPropertyValue(p)));
    if (el.tagName === 'image') out.setProperty('filter', css.filter);   // a blank's gray; a stamp's worn ink stays its attribute
  });
  copy.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
  copy.setAttribute('width', '100');
  copy.setAttribute('height', '100');
  await Promise.all([...copy.querySelectorAll('image')].map(img => dataUrl(img.getAttribute('href')!).then(u => img.setAttribute('href', u), () => img.remove())));
  const pic = new Image();
  pic.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(new XMLSerializer().serializeToString(copy));
  await pic.decode();
  return pic;
}

/** The passport's picture, from the Passport tab in the page (#passport). */
export async function passportPicture(): Promise<Blob> {
  const page = document.getElementById('passport')!;
  // A custom property as the canvas can take it: light-dark() and var() resolved by the page, as a color.
  const v = (name: string) => {
    const probe = page.appendChild(document.createElement('i'));
    probe.style.color = 'var(' + name + ')';
    const color = getComputedStyle(probe).color;
    probe.remove();
    return color;
  };
  const paper = v('--paper'), ink = getComputedStyle(page).color, line = v('--paper-line'), accent = v('--accent');
  const buttons = [...page.querySelectorAll<HTMLButtonElement>('#stamps button')];
  const stamps = await Promise.all(buttons.map(b => stampPicture(b.querySelector('svg')!)));
  const count = buttons.filter(b => b.classList.contains('played')).length;

  const c = document.createElement('canvas');
  c.width = W; c.height = H;
  const g = c.getContext('2d')!;
  g.fillStyle = paper;
  g.fillRect(0, 0, W, H);
  // The page's guilloche: fine rings from below and above.
  g.strokeStyle = line;
  g.lineWidth = 1.5;
  for (let r = 16; r < 900; r += 16) for (const y of [H * 1.2, -H * 0.2]) { g.beginPath(); g.arc(W / 2, y, r, 0, 2 * Math.PI); g.stroke(); }

  const font = 'system-ui, -apple-system, "Segoe UI", sans-serif';
  g.fillStyle = ink;
  g.textBaseline = 'alphabetic';
  g.font = '800 46px ' + font;
  g.fillText('Arcade passport', 48, 72);
  g.globalAlpha = 0.75;
  g.font = '600 22px ' + font;
  g.fillText((count >= APPS.length ? 'Every app played. Arcade complete!' : count + ' of ' + APPS.length + ' old Java apps played') + '  ·  ' + location.host, 48, 106);
  g.globalAlpha = 1;
  // The count ring, as on the tab.
  const rx = W - 98, ry = 70;
  g.lineWidth = 12;
  g.strokeStyle = v('--card-border');
  g.beginPath(); g.arc(rx, ry, 48, 0, 2 * Math.PI); g.stroke();
  g.strokeStyle = accent;
  g.beginPath(); g.arc(rx, ry, 48, -Math.PI / 2, -Math.PI / 2 + 2 * Math.PI * count / APPS.length); g.stroke();
  g.font = '800 26px ' + font;
  g.textAlign = 'center';
  g.fillText(count + '/' + APPS.length, rx, ry + 9);

  // The stamps in three rows, in the page's order, each tilted and faded as the page has it.
  const cols = Math.ceil(stamps.length / 3), cell = (W - 64) / cols, size = Math.min(120, cell - 14), top = 132;
  g.font = '600 14px ' + font;
  stamps.forEach((pic, i) => {
    const svg = buttons[i].querySelector('svg')!, s = getComputedStyle(svg);
    const x = 32 + (i % cols) * cell + cell / 2, y = top + Math.floor(i / cols) * 136 + size / 2, k = size / 100;
    g.save();
    g.translate(x + (parseFloat(s.getPropertyValue('--dx')) || 0) * k, y + (parseFloat(s.getPropertyValue('--dy')) || 0) * k);
    g.rotate((parseFloat(s.getPropertyValue('--tilt')) || 0) * Math.PI / 180);
    g.globalAlpha = parseFloat(s.opacity) || 1;
    g.drawImage(pic, -size / 2, -size / 2, size, size);
    g.restore();
    g.globalAlpha = buttons[i].classList.contains('played') ? 0.9 : 0.5;
    g.fillText(buttons[i].querySelector('.label')!.textContent!, x, y + size / 2 + 18, cell - 6);
    g.globalAlpha = 1;
  });

  // The machine-readable lines along the foot.
  g.textAlign = 'left';
  g.font = '600 22px "OCR B", "OCR-B", Consolas, "Courier New", monospace';
  g.globalAlpha = 0.85;
  page.querySelectorAll('.mrz span').forEach((span, i) => g.fillText(span.textContent!, 48, H - 46 + i * 30, W - 96));
  return new Promise((ok, no) => c.toBlob(b => (b ? ok(b) : no(new Error('no picture'))), 'image/png'));
}
