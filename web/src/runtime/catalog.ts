import { fetchOk } from './fetch-ok';

// An app in apps.json, as catalog() hands it out.
export type CatalogApp = OldjavaCatalogApp;
// An entry as apps.json writes it.
interface AppsJsonEntry { id: string; cat: string; origin?: string; then?: boolean; clip?: boolean; name: string; hint: string; shot: string }

let catalogPromise: Promise<CatalogApp[]> | null;
export let catalogDoc: Document | null;

// ---- app catalog and the ways to the next app ----
// apps.json is the one list of apps (name, hint, category, thumbnail), in gallery order: the home page's gallery
// is rendered from it, and every page reads it here. The home page's share tags (catalogDoc) are its own head;
// on app pages homeDoc() fetches them from ../index.html, only when an arcade Share is about to open.
export function root() { return document.body.hasAttribute('data-app') ? '../' : ''; }

export function catalog() {
  if (!catalogPromise) {
    if (!root()) catalogDoc = document;
    catalogPromise = fetchOk(root() + 'apps.json').then(function (r) { return r.json(); }).then(function (apps: AppsJsonEntry[]) {
      return apps.map(function (a) {
        return {
          id: a.id,
          cat: a.cat,
          origin: a.origin || 'decompiled',
          then: !!a.then,
          clip: !!a.clip,
          name: a.name,
          hint: a.hint,
          // The WebP copy (web/shots.ps1), a third to a fifth of the PNG's bytes, for the title screen and photos.
          shot: root() + a.shot.replace(/\.png$/, '.webp'),
          // Its 240 px copy (about 40% of those bytes) for what shows it at 96 px or less: Find, toasts, cards and menus.
          pic: root() + a.shot.replace(/\.png$/, '-240.webp')
        };
      });
    }).catch(function (e) {
      console.warn('Could not read the app list from apps.json: ' + e.message);
      return [];
    });
  }
  return catalogPromise;
}

let homePromise: Promise<void> | null;
/** Reads the home page's head into catalogDoc (once) for the Share panel's arcade description; never rejects. */
export function homeDoc(): Promise<void> {
  if (!homePromise) homePromise = catalogDoc ? Promise.resolve() : fetch(root() + 'index.html').then(function (r) { return r.ok ? r.text() : ''; }).then(function (html) {
    catalogDoc = new DOMParser().parseFromString(html, 'text/html');
  }).catch(function () { /* the share panel then leaves out the home page's description */ });
  return homePromise;
}

export function appId() { return document.body.getAttribute('data-app'); }

export function init() {
  catalogPromise = null;
  homePromise = null;
  catalogDoc = null;   // the home page, for its own share tags
}
