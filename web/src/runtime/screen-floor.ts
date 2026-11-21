// ---- screen floor: ?embed&full in a frame smaller than an old screen ----
// These apps were made for screens of 640 x 480 and up, and many size their windows from the screen they are
// given (the shim reports #desktop as the screen). ?embed&full is an app page put straight into someone else's
// page, so its screen was whatever frame that page drew: in a 320 x 240 or 400 x 300 frame apps switched to
// their narrow phone layouts in a landscape frame (Number Guess, Bible Downloader), cut their text, or (Circle
// Spawner) left out their shape and hung their window past the frame's edge. Here #desktop becomes a pretend
// screen whose long side is at least FLOOR_LONG and short side at least FLOOR_SHORT, in the frame's shape,
// drawn scaled down into the frame, so the app lays out as on a small desktop and all of it shows. A phone-
// shaped frame (360 x 640) is left alone, so apps keep their upright phone layouts there. The site's own
// frames already keep a big enough screen their own way (play.html at least 640 x 520, the home page's
// player at least 900 x 520, or a phone's own screen on purpose), so plain ?embed, app pages, a popped-out
// desktop and the pretend Windows pages (their shell is drawn on the page, outside #desktop) are unchanged.
const FLOOR_LONG = 640;

const FLOOR_SHORT = 360;

export interface Floor {
  /** How far the pretend screen is scaled down into the frame (1: the frame is the screen). */
  k: number;
  /** The transform that fits it to the frame, put before any other on #desktop ('' when k is 1). */
  fit: string;
}

/** Sizes #desktop to the pretend screen for the frame's size now. */
export function screenFloor(desktop: HTMLElement): Floor {
  var html = document.documentElement;
  var on = desktop.ownerDocument === document && html.hasAttribute('data-full') && !document.body.hasAttribute('data-shell');
  if (!on) return unfloored(desktop);   // before reading the frame's size, which lays the page out
  var fw = innerWidth, fh = innerHeight;
  var big = fw > 0 && fh > 0 ? Math.max(1, FLOOR_LONG / Math.max(fw, fh), FLOOR_SHORT / Math.min(fw, fh)) : 1;
  var s = desktop.style;
  if (big === 1) return unfloored(desktop);
  var w = Math.round(fw * big), h = Math.round(fh * big), k = Math.min(fw / w, fh / h);
  s.right = s.bottom = 'auto';
  s.width = w + 'px';
  s.height = h + 'px';
  // About #desktop's center, the transform origin centerWindows also uses: its center lands on the frame's.
  return { k: k, fit: 'translate(' + (fw - w) / 2 + 'px, ' + (fh - h) / 2 + 'px) scale(' + k + ')' };
}

function unfloored(desktop: HTMLElement): Floor {
  var s = desktop.style;
  s.width = s.height = s.right = s.bottom = '';
  return { k: 1, fit: '' };
}

/** For apps that place their own windows (no centerWindows): keep the pretend screen fitted to the frame. */
export function keepScreenFloor(desktop: HTMLElement) {
  function fit() {
    var f = screenFloor(desktop);
    desktop.style.transform = f.fit;
    window.__shimScale = f.k;   // the shim divides mouse positions by it
  }
  fit();
  addEventListener('resize', fit);
}
