import { useEffect, useRef, useState, type KeyboardEvent as ReactKeyboardEvent } from 'react';

// ---- Keyboard: a phone's own keyboard for the apps ----
// Many apps take keys on a painted panel (SpriteFloatyStars' Sky writing, ScreenSaver's 0 to 9, Minesweeper's F,
// the Chord finder's letters), and a phone shows its keyboard only for a text box. So on a touch screen the band
// has a Keyboard button: it puts the focus in a box of its own, the phone's keyboard comes up, and what is typed
// there reaches the app as key presses, the way touch-pad.tsx's buttons send theirs.
// Phones mostly report typed letters as keyCode 229 ("Unidentified"), so the characters are read from what the
// box gained or lost (input events) and the box's own key events are kept from the app; only the keys that type
// nothing (Enter, Escape, Tab, the arrows) are passed on from their key events.

/** Kept in the box, so Backspace always has something to delete: deleting it is how a Backspace shows. */
const BLANK = ' ';
const SYMBOL: Record<string, number> = { ' ': 32, ',': 188, '.': 190, '/': 191, ';': 186, '=': 187, '-': 189, '[': 219, '\\': 220, ']': 221, "'": 222, '`': 192 };

// A key event the shim cannot tell from a real one (keyCode cannot be set through the constructor).
// touch-pad.tsx's fireKey (the controller, gamepads, Watch) sends through it too.
export function sendKey(type: string, init: KeyboardEventInit, which: number) {
  var e = new KeyboardEvent(type, { ...init, bubbles: true, cancelable: true });
  Object.defineProperty(e, 'keyCode', { get: function () { return which; } });
  Object.defineProperty(e, 'which', { get: function () { return which; } });
  document.body.dispatchEvent(e);
}
function press(key: string, which: number, shift: boolean) {
  sendKey('keydown', { key: key, shiftKey: shift }, which);
  sendKey('keyup', { key: key, shiftKey: shift }, which);
}
/** Presses the key that types `ch`, as a hardware keyboard reports it. */
export function typeChar(ch: string) {
  var up = ch.toUpperCase();
  press(ch, /^[A-Z0-9]$/.test(up) ? up.charCodeAt(0) : SYMBOL[ch] || 0, ch !== ch.toLowerCase());
}
/** The keys that turn the box's text `was` into `now`: Backspaces for what went, then what came. */
export function typeChange(was: string, now: string) {
  var same = 0;
  while (same < was.length && same < now.length && was[same] === now[same]) same++;
  for (var i = same; i < was.length; i++) press('Backspace', 8, false);
  Array.from(now.slice(same)).forEach(typeChar);
}

const PASSED = /^(Enter|Escape|Tab|ArrowUp|ArrowDown|ArrowLeft|ArrowRight|Delete|Home|End)$/;

/** The band's Keyboard button, shown where the main pointer is a finger. */
export function PhoneKeys() {
  const box = useRef<HTMLInputElement>(null);
  const was = useRef(BLANK);
  const composing = useRef(false);
  const [open, setOpen] = useState(false);
  // While the phone's keyboard is up, a tap on the app (a tile, a button, the window) would take the focus off the
  // box and drop the keyboard: the press still reaches the app, it just leaves the focus in the box. An app's own
  // text field still takes it, for its own typing.
  useEffect(() => {
    if (!open) return;
    function keep(e: MouseEvent) {
      var t = e.target as Element;
      if (t.closest && t.closest('.window') && !t.closest('input, textarea, select')) e.preventDefault();
    }
    document.addEventListener('mousedown', keep, true);
    return () => document.removeEventListener('mousedown', keep, true);
  }, [open]);
  if (!((window.matchMedia as typeof matchMedia | undefined) && matchMedia('(pointer: coarse)').matches)) return null;
  function reset() {
    var el = box.current!;
    el.value = was.current = BLANK;
    el.setSelectionRange(1, 1);
  }
  function changed() {
    var now = box.current!.value;
    typeChange(was.current, now);
    was.current = now;
    if (!composing.current) reset();
  }
  // The box's own key events stay out of the app (they would arrive twice, or as 229); the keys that type
  // nothing are passed on.
  function key(e: ReactKeyboardEvent) {
    e.stopPropagation();
    if (!PASSED.test(e.key)) return;
    e.preventDefault();
    sendKey(e.type, { key: e.key, shiftKey: e.shiftKey }, e.keyCode);
  }
  return (
    <span className="shim-keys">
      {/* pointerdown keeps the focus where it is, so a second tap closes the keyboard instead of reopening it */}
      <button type="button" className="shim-keys-open" aria-pressed={open} aria-label="Keyboard"
        title={open ? 'Hide the keyboard' : "Type: your phone's keyboard comes up, and what you type goes to the app"}
        onPointerDown={e => e.preventDefault()} onMouseDown={e => e.preventDefault()}
        onClick={() => {
          if (open) { box.current!.blur(); return; }
          // An app's own text box is focused: its keyboard is the one wanted.
          var field = document.activeElement as HTMLElement | null;
          if (field && field.matches('.window input, .window textarea')) { field.blur(); field.focus(); return; }
          box.current!.focus();
          reset();
        }}>
        {'⌨'}<span> Keyboard</span>
      </button>
      <input ref={box} className="shim-keys-box" aria-label="Type to the app" tabIndex={-1} defaultValue={BLANK}
        autoComplete="off" autoCorrect="off" autoCapitalize="off" spellCheck={false} enterKeyHint="done"
        onFocus={() => setOpen(true)} onBlur={() => setOpen(false)} onKeyDown={key} onKeyUp={key}
        onCompositionStart={() => { composing.current = true; }}
        onCompositionEnd={() => { composing.current = false; changed(); }}
        onInput={changed} />
    </span>
  );
}
