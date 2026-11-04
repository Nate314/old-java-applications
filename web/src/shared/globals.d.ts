// What boot.js (loaded before the pages, as a classic script) puts on window, as the pages use it.
// The shapes follow boot.js; it stays plain JavaScript, shared with the 20 app pages.

/** An app in boot.js's catalog (read from apps.json). */
interface OldjavaCatalogApp { id: string; name: string; hint: string; cat: string; then: boolean; shot: string }

interface OldjavaPassport {
  has(id: string): boolean;
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

interface OldjavaPhoto {
  can(): boolean;
  take(): Promise<unknown>;
  snap(take: () => Promise<unknown>, flashOn: Element, opener: Element): void;
}

interface OldjavaInstallPrompt { prompt(): void; userChoice: Promise<unknown> }

interface DocumentPictureInPicture {
  requestWindow(options: { width: number; height: number }): Promise<Window>;
}

interface Window {
  oldjavaSound: { play(name: string): void };
  oldjavaWatch: { has(id: string): boolean };
  oldjavaThen: { quirks(id: string): Promise<unknown[]>; nodes(item: unknown): Node[] };
  oldjavaPad: {
    has(id: string): boolean;
    wanted(byDefault: boolean): boolean;
    remember(on: boolean): void;
    /** Builds the app's controller in el; returns the function that lights a button. */
    render(el: HTMLElement, id: string, send: (type: string, key: string) => void): (which: string, down: boolean) => void;
  };
  oldjavaShare: { open(id: string | null, opener: Element): void };
  oldjavaReplay: OldjavaReplay;
  oldjavaPhoto: OldjavaPhoto;
  oldjavaPassport: OldjavaPassport;
  oldjavaZoom: { fit(): boolean; set(on: boolean): void; onChange(fn: () => void): void };
  oldjavaSkin: { get(): string; set(skin: string): void };
  oldjavaSaver?: { showing(): boolean };
  oldjavaInstall: OldjavaInstallPrompt | null | undefined;
  oldjavaPop?: OldjavaPop;
  // Set by the home page for boot.js, find.js and saves.js.
  oldjavaTour: { start(): void; on(): boolean };
  oldjavaArcade: { show(id: string): void };
}

interface Navigator {
  standalone?: boolean;   // iOS Safari, added to the home screen
  connection?: { saveData?: boolean };
}

declare var documentPictureInPicture: DocumentPictureInPicture;
