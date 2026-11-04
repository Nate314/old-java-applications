import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';

// The React pages of the site (home, play, story, the 404 page's script). Everything else (boot.js, shim.css,
// the app pages, pictures) is static in ../site, which Maven copies into the same folder first. SITE_DIR is that
// folder (web/pom.xml passes its site.dir); the files are written next to the static ones, not over the folder.
// Names carry no hash: sw.js and the home page's Save all for offline list them, and the 404 page imports its
// script from the site root it finds at run time.
export default defineConfig({
  base: './',
  plugins: [react()],
  publicDir: false,
  server: { fs: { allow: ['..'] } },
  build: {
    outDir: process.env.SITE_DIR || resolve(__dirname, '../target/site'),
    emptyOutDir: false,
    modulePreload: false,
    rollupOptions: {
      input: {
        index: resolve(__dirname, 'index.html'),
        play: resolve(__dirname, 'play.html'),
        story: resolve(__dirname, 'story.html'),
        notfound: resolve(__dirname, 'src/notfound/main.tsx'),
      },
      output: {
        entryFileNames: 'assets/[name].js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name][extname]',
        // Two shared chunks with fixed names: React, and the pages' own shared code (src/shared, apps.json).
        manualChunks(id) {
          if (id.includes('node_modules')) return 'react';
          if (id.includes('/src/shared/') || id.endsWith('/site/apps.json')) return 'shared';
        },
      },
    },
  },
});
