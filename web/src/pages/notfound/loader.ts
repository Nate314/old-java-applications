// The lost page is served at the missing address, at any depth, so it has no relative links: this inline script
// finds the site's root (the nearest folder above the address that has apps.json, asked with HEAD requests in
// parallel) and imports the page's module from there. The page's styles are inlined in 404.html by the build.
// The site root itself is the answer when no folder above says otherwise, so it is never asked (an address one level
// deep asks nothing), and the module's own imports (deps, from the build) are fetched beside it, not after it.
export function loaderScript(entry: string, deps: string[]): string {
  return `(function () {
  'use strict';
  var parts = location.pathname.split('/');
  parts.pop();   // the last part is a file (or empty after a trailing slash)
  var dirs = [];
  for (var n = parts.length; n > 1; n--) dirs.push(parts.slice(0, n).join('/') + '/');
  Promise.all(dirs.map(function (dir) {
    return fetch(dir + 'apps.json', { method: 'HEAD', cache: 'no-store' })
      .then(function (r) { return r.ok && /json/.test(r.headers.get('content-type') || 'json'); })
      .catch(function () { return false; });
  })).then(function (found) {
    var root = '/';
    for (var i = 0; i < dirs.length; i++) if (found[i]) { root = dirs[i]; break; }
    window.oldjavaLostRoot = root;
    var icon = document.createElement('link');   // the tab icon, once the root is known
    icon.rel = 'icon'; icon.type = 'image/svg+xml'; icon.href = root + 'icons/icon.svg';
    document.head.appendChild(icon);
    ${JSON.stringify(deps)}.forEach(function (f) {
      var link = document.createElement('link');
      link.rel = 'modulepreload'; link.href = root + f;
      document.head.appendChild(link);
    });
    return import(root + ${JSON.stringify(entry)});
  }).catch(function (e) { console.warn('The lost page could not load: ' + e.message); });
})();`;
}
