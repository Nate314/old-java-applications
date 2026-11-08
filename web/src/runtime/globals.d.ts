// What the runtime shares through window besides the pages' APIs (src/shared/globals.d.ts): the hooks the Java
// shim (web/shim, web.rt.Js) reads or sets, and what boot.js lends the scripts it adds later (src/runtime/lazy,
// each of which declares the API it puts on window itself).

/** The TeaVM program (classes.js): its arguments, and a callback for main's outcome. */
declare function main(args: string[], done: (result: unknown) => void): void;

/** What boot.js lends the scripts it adds (window.oldjavaSite); find.ts and instant-open.ts add to it. */
interface OldjavaSite {
  news(): import('./band-news').BandNews | null;
  /** A toast (floatToast in passport.tsx); actions gets the function that closes it. */
  toast(shot: string, title: string, line: string, actions: (close: () => void) => import('react').ReactNode): { remove(): void };
  toastLink(close: () => void, app: OldjavaCatalogApp, label: string): import('react').ReactNode;
  pageOf(app: { id: string }): string;
  root(): string;
  appId(): string | null;
  clockPage(): boolean;
  skipTitle(id: string): void;
  catalog?(): Promise<OldjavaCatalogApp[]>;
  isThen?(): boolean;
  played?(id: string): boolean;
  watchable?: string[];
  sound?(name: string): void;
  ready?(id: string, then: boolean, player: boolean): void;
}

interface Window {
  oldjavaSite: OldjavaSite;
  // The shim's hooks (web.rt.Js and its @JSBody snippets).
  $rt_putStdoutCustom: (chunk: string) => void;
  __shimExited: (msg: string) => void;
  __shimFiles: Record<string, unknown>;
  __shimImages: Record<string, unknown>;
  __shimScale?: number;
  __shimRender?: number;
  __shimPainted?: boolean;
  __shimDoc: Document | null;
  __shimListen?: (doc: Document) => void;
  __shimKeyed?: { on: boolean };
  webkitAudioContext?: typeof AudioContext;
}

interface Document {
  /** Speculation rules: true while the page is prerendered and not yet shown. */
  prerendering?: boolean;
}
