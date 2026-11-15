# Windows

An on-screen Windows key. It began as a program with no window that pressed and released the Windows key once and exited; it still presses the key once when it starts, and then leaves a Windows key on the screen that works like the one on a keyboard. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **The key**: a small window at the lower right of the screen, above the taskbar, always on top. It never takes the keyboard, so it works on whatever window is in front.
  - **Click it**: the Windows key goes down with the mouse button and up with it, so a click opens (or closes) the Start menu.
  - **Hold it and press a key on the keyboard**: while the mouse button holds it down, the key reads "+ a key", and a key typed then makes Win+that key: E for File Explorer, D for the desktop, R for Run, an arrow to snap the window in front.
  - **Hold it and slide off it**: slide the pointer (or a finger) at least 28 px left, right, up or down from where it pressed, and the key reads "+ ←" (or that arrow); letting go there makes Win+that arrow, which snaps the window in front to that half of the screen (up fills the screen, down restores or minimizes it). It needs no keyboard, so it works on a touch screen. Slide back onto the key before letting go and it is a plain click again.
  - **Space or Enter**, when the key has the keyboard focus (in the browser, by Tab), taps it.
- **Shortcuts** (the **▴** in the strip above the key): a shelf of shortcuts opens above the key, the window growing upward so the key stays put. Each row is a keycap and what it does: **E** File Explorer, **D** Desktop, **R** Run, **Tab** Task view, and a row of the four arrows (snap left, fill the screen, snap right, restore or minimize). Clicking one presses Win+that key, so every shortcut the key knows works with a mouse or a finger alone, no keyboard needed, and the shelf shows what there is to press. Letting go off the shortcut that was pressed calls it off. With the keyboard focus (Tab, in the browser), the arrow keys pick a shortcut and Space or Enter presses it. **▾** closes the shelf.
- **Hover to press** (the switch at the bottom of the shelf): with it on, resting the pointer on the key for a second taps it (Start), and resting on a shortcut for a second presses that one, so the key works for someone who can move a pointer but not click or hold a button: a head mouse, an eye tracker, a hand that tires. A ring round the key's logo, or a bar across the shortcut, fills while it waits; a real click calls the wait off. Each one presses once: move off it and back on to press it again. Resting on the switch itself turns it off again, so it needs no click either way. It is off until turned on, and a finger on a touch screen does not hover, so it does nothing there.
- **Size** (the **+** in the strip): makes the whole window bigger, key, strip and shelf alike, from Small to Medium (half again as big) to Large (twice as big); at Large the sign is **−** and takes it back to Small. The bottom right corner stays put, so the key grows up and to the left, away from the taskbar. A big key is easier to hit with a finger on a touch screen, or to see from further away.
- **The strip above the key** moves the window; its **x** closes the program.
- **It stays where you leave it**: where you dragged the key, its size, whether the shelf was open and whether hover to press was on are kept in `~/.windows-key.txt` and come back the next time it starts. A place that is no longer on the screen (a smaller screen since) is pulled back onto it, and a window dragged partly off the screen comes back whole when let go.
- No command-line arguments.

It is useful where a keyboard has no Windows key, or where the key is taken: a laptop or a Mac keyboard on a Windows machine, a gaming keyboard with the key locked, a touch screen.

## Code layout

- `src/test/Test.java`: entry point; presses the Windows key via `java.awt.Robot` and waits 100 ms, as the original did, then builds and shows the key's window (`open`): the strip with the name and the x, and the key.
- `src/test/ShortcutShelf.java`: the shelf of shortcuts, drawn as rows of keycaps, each sending Win+its key to the same `Keys`.
- `src/test/Dwell.java`: hover to press, a timer per component that presses what the pointer has rested on for a second, once.
- `src/test/Placement.java`: where the key was left, its size and the shelf, read from and written to `~/.windows-key.txt` (a missing file or a damaged line leaves the defaults).
- `src/test/WindowsKey.java`: the key, a button that draws itself as a keycap (lit and pushed down while held, with the arrow a slide picked) and sends its presses and releases to a `Keys` (the `Robot` in the app).

## Self-check

`test/test/WindowsKeyCheck.java` presses the key into a key list instead of `java.awt.Robot`, so nothing reaches the real keyboard: the mouse button down holds Windows and up lets go, a second press while held or a stray release sends nothing, the right button does nothing, Space taps it, a slide right makes Win+Right while a short slide or one back onto the key makes none, the key draws up, down and slid, every shortcut on the shelf lies inside it and a click on it makes Win+its key (letting go on another makes none; Down then Enter makes Win+D, Up wraps round), and (when not headless) the window is built always on top, borderless and without taking the keyboard, but not shown, and the shelf opens upward and closes again with the bottom edge kept. It times hover to press by hand: off, resting presses nothing; on, nothing until a second, then Win+D once on Desktop, again after leaving and coming back, none after a click, the key tapped after a second on it, and resting on the switch turns it off. It also checks the sizes and the saved place (hover to press kept on and off with it): the large shelf finds each shortcut where it draws it, a place saves and loads (damaged lines skipped), the size button makes the window half again and then twice as big with its bottom right corner kept, a window opened again from the file comes back as it was left, and a place off the screen is brought back onto it. The window's place goes to a temporary file, never your home folder. `-Dshot=<file.png>` also draws the window with the shelf open into that file, and `-Dbigshot=<file.png>` the Large one. From `Windows/`:

```
javac -d ../out/check/Windows src/test/*.java test/test/*.java
java -cp ../out/check/Windows test.WindowsKeyCheck
```

## Running

From the repo root: `./run.ps1 old Windows` (or `./run.ps1 old Windows -Build` to build a jar).

It presses the Windows key via `java.awt.Robot` immediately on start, then shows the key, which presses the real Windows key when clicked.

## In the browser

The web build runs the same code on a page with a pretend Windows (see [web/README.md](../web/README.md), Pretend Windows): its Windows key press opens a Start menu holding every app on the site. Type to narrow it, Enter plays the best match, a tile plays that app, and Esc or the power button closes it. The key then stays at the lower right of the stage: click it to open the Start menu again (or close it), or hold it and press E, D, R, Tab or an arrow on the keyboard for the pretend Windows' File Explorer, desktop, Run box, task view and snapping, or slide off it to snap the window in front without a keyboard. The ▴ opens the shortcut shelf above the key, so a touch screen can open File Explorer, the desktop, the Run box and task view too. Hover to press, the switch at the bottom of the shelf, lets a mouse resting a second on the key or on a shortcut press it, with no click. The + makes the key bigger, and where you dragged it, its size and the shelf are kept with the site's other saves for the next visit. The x closes the program and shows its exit screen.

Then | Now: the band's Then plays Windows as it was first restored (`then.html`, see Then and now in web/README.md): it presses the Windows key once, so the pretend Windows opens its Start menu, and exits 100 ms later, so closing Start shows the exit screen instead of the key.
