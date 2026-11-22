// Builds the site's HTML pages from src/routes.tsx, so no HTML file is kept in the source.
//  - A page served at a fixed address becomes a Vite HTML entry: its document is generated here (the route's head,
//    rendered with React, a #root and its module) and Vite then adds the page's stylesheet
//    and bundles its module, as for a hand-written index.html.
//  - The page served at any depth (404.html) is written after bundling, with its styles inlined and an inline
//    loader that imports its module from the site root it finds (src/pages/notfound/loader.ts).
//  - Each app's own page (<App>/index.html, src/pages/app/AppPage.tsx), and its Then page (<App>/then.html) if it
//    has one, is static HTML with no React on it, written from apps.json after bundling.
// The site's runtime (src/runtime/main.ts) is one more entry, assets/boot.js: a module the home page, the app pages
// and their Then pages load from their head, sharing assets/react.js with the pages. Its head script
// (src/runtime/early.ts, the skin before first paint) is inlined in those pages where <EarlyScript /> stands.
// It also writes apps.json (src/shared/apps.json, which the pages import) for boot.js on the app pages,
// assets/files.json, every file the build wrote besides the HTML (and the preview clips), for the home page's Save all for offline, and the
// service worker (src/sw/sw.ts) as sw.js, a plain script at the site root. What the runtime adds to a page once it is
// needed (src/runtime/lazy) is imported, so Vite writes it as chunks of its own.
// Built names carry a hash of their content (vite.config.ts). public/shim.css (with the forced-colors.css it imports) is
// written once more as assets/shim-<hash>.css, which the pages link; the root copy stays for anyone who links it there.
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { createElement } from 'react';
import { renderToStaticMarkup, renderToString } from 'react-dom/server';
import { build } from 'esbuild';
import type { Plugin } from 'vite';
import type { OutputBundle, OutputChunk } from 'rollup';
import { ROUTES, type Route } from '../src/routes';
import { loaderScript } from '../src/pages/notfound/loader';
import { appPage, thenPage } from '../src/pages/app/AppPage';
import { APPS } from '../src/shared/catalog';
import { WITH_THEN, pick } from '../src/pages/compare/Intro';
import { Compare, renderFor } from '../src/pages/compare/Compare';

const root = resolve(__dirname, '..');

/** A classic script bundled from its TypeScript entry into one file. */
async function script(entry: string, minify = false, define?: Record<string, string>): Promise<string> {
  const out = await build({ entryPoints: [resolve(root, entry)], bundle: true, format: 'iife', target: 'es2020', minify, define, write: false, logLevel: 'error' });
  return out.outputFiles[0].text;
}

let early: Promise<string> | null = null;
/** A page with its <EarlyScript /> filled in. */
async function withEarly(html: string): Promise<string> {
  early ??= script('src/runtime/early.ts', true);
  const code = (await early).trim();
  // It is inlined in every page that runs the runtime (38 of them), uncached and before first paint, so it must stay
  // small: an import in skin.ts once bundled the whole runtime and React into it (200 KB a page).
  if (code.length > 16384) throw new Error('src/runtime/early.ts bundles to ' + code.length + ' bytes (limit 16384): does skin.ts import something now?');
  return html.replace('<script data-early=""></script>', () => '<script>' + code + '</script>');
}
const hash = (text: string) => createHash('sha256').update(text).digest('base64url').slice(0, 8);
/**
 * public/shim.css as a hashed asset ({ built name: source }), with forced-colors.css written into it under its media
 * query instead of @import-ed: an import is fetched (even when its query doesn't match) only once shim.css has
 * arrived, and every page waits for both before its first paint, so it was a round trip more on a slow phone.
 */
function skinFiles(): Record<string, string> {
  const forced = readFileSync(resolve(root, 'public/forced-colors.css'), 'utf8');
  const line = '@import url("forced-colors.css") (forced-colors: active);';
  const source = readFileSync(resolve(root, 'public/shim.css'), 'utf8');
  if (!source.includes(line) || forced.includes('@')) throw new Error('public/shim.css no longer imports forced-colors.css as ' + line + ', or forced-colors.css has an @-rule: update vite/pages.ts');
  const shim = source.replace(line, () => '@media (forced-colors: active) {\n' + forced + '}');
  return { ['assets/shim-' + hash(shim) + '.css']: shim };
}
/** A page with the skins' hashed name where it links shim.css (href="shim.css" or "../shim.css"). */
const withSkins = (html: string) => html.replace(/href="((?:\.\.\/)?)shim\.css"/g, (_, up) => 'href="' + up + Object.keys(skinFiles())[0] + '"');

/** A script that writes the render-blocking tag only on a trip from another page of the site (Painted early, below). */
const onTrip = (blockingTag: string) => '<script>try{sessionStorage.getItem("oldjava.trip")&&document.write(' +
  JSON.stringify(blockingTag).replace(/<\//g, '<\\/') + ')}catch(e){}</script>';
/** An app page, whose HTML holds its title card (AppPage.tsx, EarlyTitle), paints early as the home page does. */
const paintedEarly = (html: string) => html.replace(/<script type="module" src="[^"]+" blocking="render"><\/script>/,
  tag => onTrip(tag) + tag.replace(' blocking="render"', ''));

/**
 * compare.html with its players and changes as React first renders them for each app (one page serves every ?app=),
 * written in where #compare-rest stands by a script that picks the asked app's copy: the two play.html frames then
 * start loading as the page is read, not once React and the page's module have arrived (main.tsx hydrates them).
 */
function compareRest(html: string): string {
  const empty = '<div id="compare-rest" style="display:contents"></div>';
  if (!html.includes(empty)) throw new Error('compare.html has no empty #compare-rest: update compareRest in vite/pages.ts');
  const copies: Record<string, string> = {};
  for (const a of WITH_THEN) {
    renderFor(a.id);
    // React's preload of the cover picture would be a node the page never renders (hydration must match exactly).
    copies[a.id] = renderToString(createElement(Compare)).replace(/^<link rel="preload" as="image" href="[^"]*"\/>/, '');
    if (copies[a.id].includes('<link')) throw new Error('compare.html: React wrote a <link> into #compare-rest');
  }
  const json = (v: unknown) => JSON.stringify(v).replace(/</g, '\\u003c');
  return html.replace(empty, () => '<script>(function(){var m=' + json(copies) + ',a=new URLSearchParams(location.search).get("app");' +
    'document.write(' + json(empty.replace('></div>', '>')) + '+m[{}.hasOwnProperty.call(m,a)?a:' + json(pick(null).id) + ']+"</div>")})()</script>');
}

const htmlId = (r: Route) => resolve(root, r.file);
const inputName = (r: Route) => r.file.replace(/\.html$/, '').replace(/^404$/, 'notfound');

function document(r: Route, body: string, headExtra = ''): string {
  return '<!doctype html>\n<html lang="en">\n<head>\n' + renderToStaticMarkup(r.head) + headExtra + '\n</head>\n<body>\n' +
    '<div id="root" style="display: contents">' + (r.placeholder ? renderToStaticMarkup(r.placeholder) : '') + '</div>\n' +
    body + '</body>\n</html>\n';
}

/** Every stylesheet a chunk needs, its imports' first, in the order Vite would link them. */
function cssOf(bundle: OutputBundle, chunk: OutputChunk, seen = new Set<string>()): string[] {
  const out: string[] = [];
  for (const name of chunk.imports) {
    const dep = bundle[name];
    if (dep && dep.type === 'chunk' && !seen.has(name)) { seen.add(name); out.push(...cssOf(bundle, dep, seen)); }
  }
  for (const css of chunk.viteMetadata?.importedCss ?? []) if (!out.includes(css)) out.push(css);
  return out;
}

/**
 * A module preload for every chunk a chunk imports, theirs too, so they load beside it instead of after it has
 * arrived and been read (vite.config.ts turns Vite's own off): one round trip less before first paint on a slow phone.
 */
function preloadTags(bundle: OutputBundle, chunk: OutputChunk, up: string, seen = new Set<string>()): string {
  return chunk.imports.filter(name => !seen.has(name) && seen.add(name)).map(name => {
    const dep = bundle[name];
    return '<link rel="modulepreload" href="' + up + name + '">' + (dep && dep.type === 'chunk' ? preloadTags(bundle, dep, up, seen) : '');
  }).join('');
}

export function pages(): Plugin {
  const byId = new Map(ROUTES.filter(r => !r.anyDepth).map(r => [htmlId(r), r]));
  return {
    name: 'oldjava-pages',
    enforce: 'pre',
    config() {
      return {
        build: {
          rollupOptions: {
            input: {
              ...Object.fromEntries(ROUTES.map(r => [inputName(r), r.anyDepth ? resolve(root, r.entry) : htmlId(r)])),
              boot: resolve(root, 'src/runtime/main.ts'),
            },
          },
        },
      };
    },
    resolveId(id) {
      const full = resolve(root, id.replace(/^\//, ''));
      return byId.has(id) ? id : byId.has(full) ? full : null;
    },
    async load(id) {
      const r = byId.get(id);
      if (!r) return null;
      const html = document(r, '<script type="module" src="/' + r.entry + '"></script>\n');
      return withEarly(r.file === 'compare.html' ? compareRest(html) : html);
    },
    // Vite joins a page's module scripts into one entry script; a page that runs the runtime (<RuntimeScript />)
    // keeps it render-blocking, as the runtime's own script is on the app pages.
    transformIndexHtml: {
      order: 'post',
      handler(html, ctx) {
        html = withSkins(html);
        const r = byId.get(resolve(ctx.filename));
        const entry = /<script type="module" crossorigin src="([^"]+)"><\/script>/;
        if (ctx.bundle && ctx.chunk) html = html.replace(entry, tag => tag + preloadTags(ctx.bundle!, ctx.chunk!, './'));
        if (!r || !renderToStaticMarkup(r.head).includes('src="/src/runtime/main.ts"')) return html;
        const blocking = (src: string) => '<script type="module" crossorigin src="' + src + '" blocking="render"></script>';
        if (!r.placeholder) return html.replace(entry, (_, src) => blocking(src));
        // Painted early: a page whose placeholder already shows its largest text (the home page) paints it while its
        // scripts load. Only a trip from another page of the site (one-machine.ts's 'oldjava.trip', left by the page
        // before) still holds the first frame for them, so its pagereveal listener sees the player it animates into.
        // Written by the parser, the blocking copy runs like the tag it was; the module itself runs once.
        // Its scripts ask at low priority: at high they shared a slow phone's bandwidth with the render-blocking
        // stylesheets and held the first paint (throttled phone FCP ~1.2 -> ~0.9 s, round 72).
        return html.replace(entry, (tag, src) => onTrip(blocking(src)) + tag.replace('<script ', '<script fetchpriority="low" '))
          .replace(/<link rel="modulepreload" /g, '<link rel="modulepreload" fetchpriority="low" ');
      },
    },
    async generateBundle(_options, bundle) {
      const skins = skinFiles();
      for (const [fileName, source] of Object.entries(skins)) this.emitFile({ type: 'asset', fileName, source });
      // The app pages are written here, not by Vite, so they get the hashed names of what they load from here too.
      const chunk = (name: string) => {
        const c = Object.values(bundle).find((x): x is OutputChunk => x.type === 'chunk' && x.name === name);
        if (!c) throw new Error('No chunk ' + name);
        return c.fileName;
      };
      // boot.js's own imports (React first) are preloaded beside it: boot.js holds the first frame (blocking="render")
      // and the title screen no longer waits for classes.js, so asking for React only once boot.js had arrived put a
      // whole download in front of the first frame (about 0.3 s on a throttled phone, measured in round 55).
      const boot = bundle[chunk('boot')] as OutputChunk;
      const named = (html: string) => withSkins(html).replace(/"\.\.\/assets\/boot\.js"[^>]*><\/script>/, tag =>
        tag.replace('assets/boot.js', chunk('boot')) + preloadTags(bundle, boot, '../'))
        .replace('"../assets/news.js"', () => '"../' + chunk('news') + '"');
      this.emitFile({ type: 'asset', fileName: 'apps.json', source: readFileSync(resolve(root, 'src/shared/apps.json')) });
      for (const app of APPS) this.emitFile({ type: 'asset', fileName: app.id + '/index.html', source: await withEarly(paintedEarly(named(appPage(app)))) });
      for (const app of APPS.filter(a => a.then)) this.emitFile({ type: 'asset', fileName: app.id + '/then.html', source: await withEarly(named(thenPage(app))) });
      const files = [...new Set(Object.keys(bundle).concat(Object.keys(skins)))].filter(name => name.startsWith('assets/')).sort();
      // sw.js lists this build's files, so it changes with every build (the browser then installs it) and knows which to keep.
      this.emitFile({ type: 'asset', fileName: 'sw.js', source: await script('src/sw/sw.ts', false, { BUILT: JSON.stringify(files) }) });
      // Save all keeps the preview clips too (about 3 MB), so the gallery, play.html and title screens move offline.
      const clips = APPS.filter(a => a.clip).map(a => 'clips/' + a.id + '.mp4');
      this.emitFile({ type: 'asset', fileName: 'assets/files.json', source: JSON.stringify(files.concat(clips)) + '\n' });
      for (const r of ROUTES.filter(x => x.anyDepth)) {
        const chunk = Object.values(bundle).find((c): c is OutputChunk => c.type === 'chunk' && c.isEntry && c.name === inputName(r));
        if (!chunk) throw new Error('No bundle for ' + r.entry);
        const styles = cssOf(bundle, chunk).map(name => {
          const asset = bundle[name];
          return asset.type === 'asset' ? String(asset.source) : '';
        }).join('\n');
        const head = '<style>\n' + styles + '</style>\n<script>' + loaderScript(chunk.fileName) + '</script>';
        this.emitFile({ type: 'asset', fileName: r.file, source: document(r, '', head) });
      }
    },
  };
}
