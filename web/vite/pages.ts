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
// assets/files.json, every file the build wrote besides the HTML, for the home page's Save all for offline, and the
// service worker (src/sw/sw.ts) as sw.js, a plain script at the site root. What the runtime adds to a page once it is
// needed (src/runtime/lazy) is imported, so Vite writes it as chunks of its own.
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { renderToStaticMarkup } from 'react-dom/server';
import { build } from 'esbuild';
import type { Plugin } from 'vite';
import type { OutputBundle, OutputChunk } from 'rollup';
import { ROUTES, type Route } from '../src/routes';
import { loaderScript } from '../src/pages/notfound/loader';
import { appPage, thenPage } from '../src/pages/app/AppPage';
import { APPS } from '../src/shared/catalog';

const root = resolve(__dirname, '..');

/** A classic script bundled from its TypeScript entry into one file. */
async function script(entry: string, minify = false): Promise<string> {
  const out = await build({ entryPoints: [resolve(root, entry)], bundle: true, format: 'iife', target: 'es2020', minify, write: false, logLevel: 'error' });
  return out.outputFiles[0].text;
}

let early: Promise<string> | null = null;
/** A page with its <EarlyScript /> filled in. */
async function withEarly(html: string): Promise<string> {
  early ??= script('src/runtime/early.ts', true);
  const code = (await early).trim();
  return html.replace('<script data-early=""></script>', () => '<script>' + code + '</script>');
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
      return withEarly(document(r, '<script type="module" src="/' + r.entry + '"></script>\n'));
    },
    // Vite joins a page's module scripts into one entry script; a page that runs the runtime (<RuntimeScript />)
    // keeps it render-blocking, as the runtime's own script is on the app pages.
    transformIndexHtml: {
      order: 'post',
      handler(html, ctx) {
        const r = byId.get(resolve(ctx.filename));
        if (!r || !renderToStaticMarkup(r.head).includes('src="/src/runtime/main.ts"')) return html;
        return html.replace(/<script type="module" crossorigin src="([^"]+)"><\/script>/, '<script type="module" crossorigin src="$1" blocking="render"></script>');
      },
    },
    async generateBundle(_options, bundle) {
      this.emitFile({ type: 'asset', fileName: 'apps.json', source: readFileSync(resolve(root, 'src/shared/apps.json')) });
      for (const app of APPS) this.emitFile({ type: 'asset', fileName: app.id + '/index.html', source: await withEarly(appPage(app)) });
      for (const app of APPS.filter(a => a.then)) this.emitFile({ type: 'asset', fileName: app.id + '/then.html', source: await withEarly(thenPage(app)) });
      this.emitFile({ type: 'asset', fileName: 'sw.js', source: await script('src/sw/sw.ts') });
      const files = Object.keys(bundle).filter(name => name.startsWith('assets/')).sort();
      this.emitFile({ type: 'asset', fileName: 'assets/files.json', source: JSON.stringify(files) + '\n' });
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
