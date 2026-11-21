import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { pages } from './vite/pages';

// The web site: React pages (src/routes.tsx, written as HTML by vite/pages.ts) and the static files in public/
// (boot.js, shim.css, the app pages, pictures), copied as they are. The Java apps are compiled into the same folder
// by Maven (web/pom.xml, which runs this build with SITE_DIR set to its site.dir); the files are written next to
// theirs, not over the folder.
// Every built script and stylesheet carries a hash of its content in its name, so a page always gets the files it was
// built with, never an older copy a cache kept. vite/pages.ts writes the names it needs (the app pages' boot.js,
// shim.css, sw.js's list, assets/files.json for Save all for offline); the HTML pages keep their addresses.
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
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
        // Shared chunks: React, the friend-link table, and the pages' own shared code (src/shared). Shared styles
        // (src/shared/styles) are @import-ed by each page's stylesheet, so every page links one, in source order.
        manualChunks(id) {
          if (id.includes('node_modules')) return 'react';
          if (id.includes('/src/shared/link-state')) return 'link-state';   // boot.js needs it too, not the rest of shared
          if (id.includes('/src/shared/menu-keys')) return 'menu-keys';     // the same for the More menu's keys
          if (id.includes('/src/shared/')) return 'shared';
        },
      },
    },
  },
});
