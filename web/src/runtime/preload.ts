import { fetchOk } from './fetch-ok';

const IMAGE = /\.(png|jpe?g|gif|bmp|webp)$/i;

/**
 * Loads one of the page's data-files before main() runs: its bytes for the shim's file reads (__shimFiles) and, for a
 * picture, a decoded image (__shimImages). The image is made from the same bytes (a blob: URL, kept for the page's
 * life because the shim hands img.src to CSS cursors and icons): an <img> of the file's own URL downloaded it a second
 * time, since data files are served no-cache, which on a slow phone doubled FlappyBird's wait for Play.
 */
export function preload(name: string) {
  window.__shimFiles = window.__shimFiles || {};
  window.__shimImages = window.__shimImages || {};
  return fetchOk(name).then(function (r) { return r.arrayBuffer(); }).then(function (buf) {
    window.__shimFiles[name] = new Uint8Array(buf);
    if (!IMAGE.test(name)) return;
    return new Promise<void>(function (resolve, reject) {
      var img = new Image();
      img.onload = function () { window.__shimImages[name] = img; resolve(); };
      img.onerror = function () { reject(new Error('cannot decode')); };
      img.src = URL.createObjectURL(new Blob([buf]));
    });
  }).catch(function (e) {
    console.warn('Could not preload ' + name + ': ' + e.message);
  });
}
