// The lost page is served at the missing address, at any depth, so it has no relative links: this inline script
// finds the site's root (the nearest folder above the address that has boot.js, asked with HEAD requests in
// parallel) and imports the page's module from there. The page's styles are inlined in 404.html by the build.
export function loaderScript(entry: string): string {
  return `(function () {
  'use strict';
  var parts = location.pathname.split('/');
  parts.pop();   // the last part is a file (or empty after a trailing slash)
  var dirs = [];
  for (var n = parts.length; n >= 1; n--) dirs.push(parts.slice(0, n).join('/') + '/');
  Promise.all(dirs.map(function (dir) {
    return fetch(dir + 'boot.js', { method: 'HEAD', cache: 'no-store' })
      .then(function (r) { return r.ok && /javascript/.test(r.headers.get('content-type') || 'javascript'); })
      .catch(function () { return false; });
  })).then(function (found) {
    var root = '/';
    for (var i = 0; i < dirs.length; i++) if (found[i]) { root = dirs[i]; break; }
    window.oldjavaLostRoot = root;
    return import(root + ${JSON.stringify(entry)});
  }).catch(function (e) { console.warn('The lost page could not load: ' + e.message); });
})();`;
}
