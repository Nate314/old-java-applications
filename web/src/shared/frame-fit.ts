// How play.html and the play sheet (story, lost page) size an app's page in a frame: the page keeps a viewport of at least
// 640 x 520 (so a small frame is the app's page scaled down, its notes and windows laid out as usual), or as big as
// its card asks for (CircleSpawner places its own 380 px window, Rubik's Cube's is 700 px tall), as in the home
// page's player. The viewport keeps the frame's shape.
import type { CSSProperties } from 'react';
import type { App } from './catalog';

const MIN_W = 640;
const MIN_H = 520;   // the home page player's height, which leaves the watch line room above the windows

/** The iframe's style to fill a w x h box with the app's page. */
export function frameFit(app: App, w: number, h: number): CSSProperties {
  const scale = Math.min(1, w / Math.max(MIN_W, app.minWidth || 0), h / Math.max(MIN_H, app.minHeight || 0));
  return { width: Math.round(w / scale) + 'px', height: Math.round(h / scale) + 'px', transform: scale < 1 ? 'scale(' + scale + ')' : undefined };
}
