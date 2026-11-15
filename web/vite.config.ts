import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { pages } from './vite/pages';

// The web site: React pages (src/routes.tsx, written as HTML by vite/pages.ts) and the static files in public/
// (boot.js, shim.css, the app pages, pictures), copied as they are. The Java apps are compiled into the same folder
// by Maven (web/pom.xml, which runs this build with SITE_DIR set to its site.dir); the files are written next to
// theirs, not over the folder.
// Names carry no hash: sw.js and the home page's Save all for offline list them, and the 404 page imports its
// script from the site root it finds at run time.
export default defineConfig({
  base: './',
  plugins: [pages(), react()],
  build: {
    outDir: process.env.SITE_DIR || resolve(__dirname, 'target/site'),
    emptyOutDir: false,
    modulePreload: false,
    // The stylesheets ship as written (readable, and each rule exactly as the pages had it).
    cssMinify: false,
    rollupOptions: {
      output: {
        entryFileNames: 'assets/[name].js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name][extname]',
        // Two shared chunks with fixed names: React, and the pages' own shared code (src/shared). Shared styles
        // (src/shared/styles) are @import-ed by each page's stylesheet, so every page links one, in source order.
        manualChunks(id) {
          if (id.includes('node_modules')) return 'react';
          if (id.includes('/src/shared/')) return 'shared';
        },
      },
    },
  },
});
