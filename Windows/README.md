# Windows

An on-screen Windows key. It began as a program with no window that pressed and released the Windows key once and exited; it still presses the key once when it starts, and then leaves a Windows key on the screen that works like the one on a keyboard. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **The key**: a small window at the lower right of the screen, above the taskbar, always on top. It never takes the keyboard, so it works on whatever window is in front.
  - **Click it**: the Windows key goes down with the mouse button and up with it, so a click opens (or closes) the Start menu.
  - **Hold it and press a key on the keyboard**: while the mouse button holds it down, the key reads "+ a key", and a key typed then makes Win+that key: E for File Explorer, D for the desktop, R for Run, an arrow to snap the window in front.
  - **Hold it and slide off it**: slide the pointer (or a finger) at least 28 px left, right, up or down from where it pressed, and the key reads "+ ←" (or that arrow); letting go there makes Win+that arrow, which snaps the window in front to that half of the screen (up fills the screen, down restores or minimizes it). It needs no keyboard, so it works on a touch screen. Slide back onto the key before letting go and it is a plain click again.
  - **Space or Enter**, when the key has the keyboard focus (in the browser, by Tab), taps it.
- **Shortcuts** (the **▴** in the strip above the key): a shelf of shortcuts opens above the key, the window growing upward so the key stays put. Each row is a keycap and what it does: **E** File Explorer, **D** Desktop, **R** Run, **Tab** Task view, and a row of the four arrows (snap left, fill the screen, snap right, restore or minimize). Clicking one presses Win+that key, so every shortcut the key knows works with a mouse or a finger alone, no keyboard needed, and the shelf shows what there is to press. Letting go off the shortcut that was pressed calls it off. With the keyboard focus (Tab, in the browser), the arrow keys pick a shortcut and Space or Enter presses it. **▾** closes the shelf.
- **The strip above the key** moves the window; its **x** closes the program.
- No command-line arguments.

It is useful where a keyboard has no Windows key, or where the key is taken: a laptop or a Mac keyboard on a Windows machine, a gaming keyboard with the key locked, a touch screen.

## Code layout

- `src/test/Test.java`: entry point; presses the Windows key via `java.awt.Robot` and waits 100 ms, as the original did, then builds and shows the key's window (`open`): the strip with the name and the x, and the key.
- `src/test/ShortcutShelf.java`: the shelf of shortcuts, drawn as rows of keycaps, each sending Win+its key to the same `Keys`.
- `src/test/WindowsKey.java`: the key, a button that draws itself as a keycap (lit and pushed down while held, with the arrow a slide picked) and sends its presses and releases to a `Keys` (the `Robot` in the app).

## Self-check

`test/test/WindowsKeyCheck.java` presses the key into a key list instead of `java.awt.Robot`, so nothing reaches the real keyboard: the mouse button down holds Windows and up lets go, a second press while held or a stray release sends nothing, the right button does nothing, Space taps it, a slide right makes Win+Right while a short slide or one back onto the key makes none, the key draws up, down and slid, every shortcut on the shelf lies inside it and a click on it makes Win+its key (letting go on another makes none; Down then Enter makes Win+D, Up wraps round), and (when not headless) the window is built always on top, borderless and without taking the keyboard, but not shown, and the shelf opens upward and closes again with the bottom edge kept. `-Dshot=<file.png>` also draws the window with the shelf open into that file. From `Windows/`:

```
javac -d ../out/check/Windows src/test/*.java test/test/*.java
java -cp ../out/check/Windows test.WindowsKeyCheck
```

## Running

From the repo root: `./run.ps1 old Windows` (or `./run.ps1 old Windows -Build` to build a jar).

It presses the Windows key via `java.awt.Robot` immediately on start, then shows the key, which presses the real Windows key when clicked.

## In the browser

The web build runs the same code on a page with a pretend Windows (see [web/README.md](../web/README.md), Pretend Windows): its Windows key press opens a Start menu holding every app on the site. Type to narrow it, Enter plays the best match, a tile plays that app, and Esc or the power button closes it. The key then stays at the lower right of the stage: click it to open the Start menu again (or close it), or hold it and press E, D, R, Tab or an arrow on the keyboard for the pretend Windows' File Explorer, desktop, Run box, task view and snapping, or slide off it to snap the window in front without a keyboard. The ▴ opens the shortcut shelf above the key, so a touch screen can open File Explorer, the desktop, the Run box and task view too. The x closes the program and shows its exit screen.

Then | Now: the band's Then plays Windows as it was first restored (`then.html`, see Then and now in web/README.md): it presses the Windows key once, so the pretend Windows opens its Start menu, and exits 100 ms later, so closing Start shows the exit screen instead of the key.
