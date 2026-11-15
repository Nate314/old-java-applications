// The one list of apps (web/src/shared/apps.json): the home page renders it, and boot.js, play.html, the 404 page,
// web/cards.ps1 and the desktop launcher read the same file. The build writes each app's page from it
// (src/pages/app/AppPage.tsx).
import apps from './apps.json';

export interface App {
  id: string;
  name: string;
  hint: string;
  cat: 'game' | 'music' | 'toy' | 'tool';
  shot: string;
  alt: string;
  origin?: 'original';   // original Eclipse source; decompiled from a jar when missing
  then?: boolean;        // also plays as first restored (<App>/then.html)
  clip?: boolean;        // has a preview clip (clips/<App>.mp4)
  minWidth?: number;
  minHeight?: number;
}

export const APPS = apps as App[];

export function app(id: string): App {
  const found = APPS.find(a => a.id === id);
  if (!found) throw new Error('No app ' + id + ' in apps.json');
  return found;
}

/** The home page's lighter copies of an app's thumbnail (web/shots.ps1 Save-Webp): full size, or 240 px wide when small. */
export const webp = (shot: string, small?: boolean) => shot.replace(/\.png$/, small ? '-240.webp' : '.webp');
