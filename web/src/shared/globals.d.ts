// What the site's runtime (src/runtime, built as boot.js and loaded before the pages as a classic script) puts on
// window. Each API is a named interface, so the runtime module that builds it can add what only it uses
// (declare global { interface OldjavaSkin { ... } }).

/** An app in boot.js's catalog (read from apps.json). */
interface OldjavaCatalogApp { id: string; name: string; hint: string; cat: string; then: boolean; shot: string; origin: string }

interface OldjavaPassport {
  has(id: string): boolean;
  reload(): void;
  reset(): void;
  onChange(fn: (fresh: string | null) => void): void;
  open: ((id: string) => void) | null;
  catalog(): Promise<OldjavaCatalogApp[]>;
  unplayed(apps: OldjavaCatalogApp[], current: string | null): OldjavaCatalogApp[];
}

/** An app page's pop out (the player's iframe has one once its page has loaded). */
interface OldjavaPop {
  can(): boolean;
  size(): { width: number; height: number };
  into(win: Window): void;
  back(): void;
  popped(): boolean;
}

/** A recorded clip, passed between an app page and the page around it. */
type OldjavaClip = unknown;

interface OldjavaReplay {
  open(clip: OldjavaClip, opener: Element | null): void;
  live(): boolean;
  take(): Promise<OldjavaClip | null>;
}

/** A photo booth print: the picture, its app and when it was taken. */
interface OldjavaPhotoPrint { blob: Blob; app: string; at: number }

interface OldjavaPhoto {
  can(): boolean;
  take(): Promise<OldjavaPhotoPrint | null>;
  snap(take: () => Promise<OldjavaPhotoPrint | null>, flashOn: Element | null, opener: HTMLElement | null): void;
}

interface OldjavaInstallPrompt { prompt(): void; userChoice: Promise<unknown> }

interface DocumentPictureInPicture {
  requestWindow(options: { width: number; height: number }): Promise<Window>;
}

interface OldjavaSound { play(name: string): void }
interface OldjavaWatch { has(id: string): boolean }
interface OldjavaThen { quirks(id: string): Promise<unknown[]> }
interface OldjavaPad {
  has(id: string): boolean;
  wanted(byDefault: boolean): boolean;
  remember(on: boolean): void;
  /** Builds the app's controller in el; returns the function that lights a button. */
  render(el: HTMLElement, id: string, send: (type: string, key: string) => void): (which: number, down: boolean) => void;
}
interface OldjavaShare { open(id: string | null, opener: Element): void }
interface OldjavaZoom { fit(): boolean; set(on: boolean): void; onChange(fn: () => void): void }
interface OldjavaSkin { get(): string; set(skin: string): void }
interface OldjavaSaver { showing(): boolean }
/** The line an app's Records entry shows for its saved files ('' when it has none of its own). */
interface OldjavaRecords { line(store: string, files: Record<string, string>): string }

interface Window {
  oldjavaSound: OldjavaSound;
  oldjavaWatch: OldjavaWatch;
  oldjavaThen: OldjavaThen;
  oldjavaPad: OldjavaPad;
  oldjavaShare: OldjavaShare;
  oldjavaReplay: OldjavaReplay;
  oldjavaPhoto: OldjavaPhoto;
  oldjavaPassport: OldjavaPassport;
  oldjavaZoom: OldjavaZoom;
  oldjavaSkin: OldjavaSkin;
  oldjavaSaver?: OldjavaSaver;
  oldjavaInstall: OldjavaInstallPrompt | null | undefined;
  oldjavaPop?: OldjavaPop;
  oldjavaRecords: OldjavaRecords;
  // Set by the home page for boot.js, find.js and Records.tsx.
  oldjavaTour: { start(): void; on(): boolean };
  oldjavaArcade: { show(id: string): void };
  /** The home page's player hint shows a controller's buttons for a while (the runtime's gamepads). */
  oldjavaPlayerHint?: (parts: string[][]) => void;
}

interface Navigator {
  standalone?: boolean;   // iOS Safari, added to the home screen
  connection?: { saveData?: boolean };
}

declare var documentPictureInPicture: DocumentPictureInPicture;
