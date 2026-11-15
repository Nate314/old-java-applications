// An app in apps.json, as catalog() hands it out.
export type CatalogApp = OldjavaCatalogApp;
// An entry as apps.json writes it.
interface AppsJsonEntry { id: string; cat: string; origin?: string; then?: boolean; name: string; hint: string; shot: string }

let catalogPromise: Promise<CatalogApp[]> | null;
export let catalogDoc: Document | null;

// ---- app catalog and the ways to the next app ----
// apps.json is the one list of apps (name, hint, category, thumbnail), in gallery order: the home page's gallery
// is rendered from it, and every page reads it here. The home page's share tags (catalogDoc) are its own head,
// fetched from ../index.html on app pages.
export function root() { return document.body.hasAttribute('data-app') ? '../' : ''; }

export function catalog() {
  if (!catalogPromise) {
    if (!root()) catalogDoc = document;
    else fetch('../index.html').then(function (r) { return r.ok ? r.text() : ''; }).then(function (html) {
      catalogDoc = new DOMParser().parseFromString(html, 'text/html');
    }).catch(function () { /* the share panel then leaves out the home page's description */ });
    catalogPromise = fetch(root() + 'apps.json').then(function (r) {
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r.json();
    }).then(function (apps: AppsJsonEntry[]) {
      return apps.map(function (a) {
        return {
          id: a.id,
          cat: a.cat,
          origin: a.origin || 'decompiled',
          then: !!a.then,
          name: a.name,
          hint: a.hint,
          shot: root() + a.shot
        };
      });
    }).catch(function (e) {
      console.warn('Could not read the app list from apps.json: ' + e.message);
      return [];
    });
  }
  return catalogPromise;
}

export function appId() { return document.body.getAttribute('data-app'); }

export function init() {
  catalogPromise = null;
  catalogDoc = null;   // the home page, for its own share tags
}
