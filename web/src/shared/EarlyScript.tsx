/**
 * Where the runtime's head script goes (src/runtime/early.ts: the visitor's skin before first paint). The build
 * (vite/pages.ts) fills it with the bundled script, so every page that runs the runtime inlines the same code.
 */
export function EarlyScript() {
  return <script data-early="" />;
}

/** The site's runtime (src/runtime/main.ts, built as assets/boot.js), loaded from the page's head. */
export function RuntimeScript({ src }: { src: string }) {
  // blocking: the page waits for it before its first frame, so its page-transition listener (pagereveal) is there.
  return <script type="module" src={src} blocking="render" />;
}
