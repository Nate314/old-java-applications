import type { QrCode as Code } from './qr';

/** A QR code as an SVG: one path of dark squares on a white field, with a quiet zone (in modules) around it. */
export function QrCode({ code, quiet = 4 }: { code: Code; quiet?: number }) {
  const full = code.size + quiet * 2;
  let d = '';
  for (let y = 0; y < code.size; y++) {
    for (let x = 0; x < code.size; x++) if (code.dark(x, y)) d += 'M' + (x + quiet) + ' ' + (y + quiet) + 'h1v1h-1z';
  }
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox={'0 0 ' + full + ' ' + full} shapeRendering="crispEdges">
      <rect width="100%" height="100%" fill="#fff" />
      <path d={d} fill="#000" />
    </svg>
  );
}
