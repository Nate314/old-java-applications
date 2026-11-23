// What sw.js (sw.ts) and the home page's Save all (pages/home/takeaway.ts) must agree on: the cache they share,
// the key a file is kept under, and which files a page names. Imports nothing, so it bundles into the worker too.

export const CACHE = 'oldjava-v1';

/** An app's program, the one file kept with its query: classes.js?v=<hash> names its build (vite/stamp-programs.mjs). */
export const isProgram = (url: URL) => /\/(classes|then)\.js$/.test(url.pathname);

/** The cache key of a URL: no hash, and no query except on a program, so App/index.html?embed and App/index.html share one entry. */
export function cacheKey(href: string): string {
  const url = new URL(href);
  if (!isProgram(url)) url.search = '';
  url.hash = '';
  return url.href;
}

/** What a page names, as same-origin absolute URLs: its scripts and stylesheets (its program with its ?v=), and its <body data-files>. */
export function pageNames(page: string, html: string): { code: string[]; data: string[] } {
  const own = (files: string[]) => files.map(f => new URL(f, page)).filter(u => u.origin === self.location.origin).map(u => u.href);
  const data = /<body[^>]*\sdata-files="([^"]*)"/.exec(html);
  return {
    code: own([...html.matchAll(/\s(?:src|href)="([^"]+\.(?:js|css)(?:\?[^"]*)?)"/g)].map(m => m[1])),
    data: own(data ? data[1].split(/\s+/).filter(Boolean) : []),
  };
}
