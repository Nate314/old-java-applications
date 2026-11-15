# WindowsE

A keyboard of Windows shortcuts that presses the one you pick as many times as you ask. It began as a small window that pressed Win+E a chosen number of times, which on Windows opens that many File Explorer windows, and Win+E ten times is still what it offers first. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **The keyboard**: a small keyboard is drawn with the Windows shortcuts on it as blue-edged keys. Click one (or Tab to it and press Space) to pick it; the line above the keyboard shows the shortcut and what Windows does with it. Double-click a key to pick it and press it at once.
  - **Windows key** (Start): opens the Start menu, or closes it.
  - **E** (Explorer): Win+E, opens a File Explorer window.
  - **D** (Desktop): Win+D, hides every window, and again brings them back.
  - **R** (Run): Win+R, opens the Run box.
  - **Tab** (Task view): Win+Tab, shows every open window.
  - **Left / Right** (Snap): Win+Left and Win+Right, snap the window in front to a half of the screen.
  - **Up** (Max): Win+Up, maximizes the window in front.
  - **Down**: Win+Down, restores the window in front, or minimizes it.
- **Press it ... times**: how many times (10 when E is picked, 1 for the others). Enter in the field does the same as the button.
- **"DO IT!!!"**: presses the picked shortcut that many times, 100 ms apart (on a Swing timer, so the window stays responsive). Each press holds the Windows key only around its own key (Windows down, E down, E up, Windows up), so between presses the Windows key is up and anything typed meanwhile stays an ordinary key; the Windows key alone is tapped. While it runs, the Windows key and the other key go down on the drawn keyboard with each press, a count shows above it, and the button reads **Stop**, which ends it early. If the count is not a whole number of 1 or more, the title says so.
- **Steps**: **Add step** puts the picked shortcut, with the field's count, on the list shown above the keyboard (up to 8 steps; past the window's edge it says "+2 more"), and DO IT!!! then presses the whole list in order instead, lighting the step being pressed. **Clear** empties it. **Side by side** lists Win+E, Win+Left, Win+E, Win+Right: two File Explorer windows, one on each half of the screen.
- **The little monitor**: beside the keyboard, a small screen shows what DO IT!!! will do, starting from a clear desktop: the File Explorer windows it opens (cascaded, snapped to a half, maximized, minimized or hidden by Win+D), the Start menu, the Run box or task view, and under it the same in words ("2 File Explorer windows: 1 on the left half, 1 on the right half."). It follows the list, or the picked key and the count as they are typed, and while DO IT!!! runs it plays each press as it is sent. It follows the site's pretend Windows (at most 40 File Explorer windows, as there), so on the desktop it is a guess: the windows already open, and the arrows moving this window when no File Explorer window was opened, are not on it (it says when the arrows found no window to move). The DO IT!!! button's screen reader name ends with the same words.
- **Preview**: plays the list (or the picked key) on the little monitor at DO IT!!!'s pace without pressing any key, the drawn keys going down as they would; DO IT!!! turns to Stop meanwhile. On the desktop this is the way to see what Win+E x40 or a list of snaps does before it happens to the real screen.
- The window is not resizable. Title "Windows+E". No command-line arguments.

On the desktop these are the real shortcuts, so the window in front is usually Windows+E itself: Win+Left snaps it, Win+D hides it with everything else.

## Code layout

- `src/windowse/WindowsEMain.java`: entry point; shows a `WindowsEWindow` that presses keys with `RobotKeys`.
- `src/windowse/WindowsEWindow.java`: the keyboard, the field, the list of steps and the buttons.
- `src/windowse/ShortcutBoard.java`: draws the keyboard (the keys without a shortcut, dimmed), the picked shortcut, the list of steps and the count, and places a `KeyCap` on each shortcut's key.
- `src/windowse/KeyCap.java`: one shortcut's key, a button that draws itself as a keycap (picked, pushed down, hovered), with a screen reader name.
- `src/windowse/Shortcut.java`: the shortcuts: key code, legend, caption, name, what it does and the count it offers.
- `src/windowse/Step.java`: one step of the list: a shortcut and how many times.
- `src/windowse/Screen.java`: what the shortcuts do to a screen (the little monitor's model) and the words for it.
- `src/windowse/MiniScreen.java`: draws a `Screen` small.
- `src/windowse/WindowsEPresser.java`: presses a list of steps on a Swing timer, and stops early.
- `src/windowse/Keys.java`, `src/windowse/RobotKeys.java`: where the presses go; `RobotKeys` sends them with `java.awt.Robot`.

## Self-check

`test/windowse/WindowsECheck.java` presses the shortcuts into a key list instead of `java.awt.Robot`, so nothing reaches the real keyboard: Win+E three times lets go of Windows after each E, the Windows key alone is tapped, Win+Tab and Win+Left send their keys, Stop after two presses stops there, Side by side and a mixed list are pressed in order, the done callback runs once, and every shortcut is described. It also checks what the little monitor says lists do: Side by side, Win+E stopping at 40 windows, Win+D hiding and bringing back, the arrows' snaps, restores and minimizes, an arrow with no window, the Start menu and Run box, and a huge count keeping a toggle's parity. From `WindowsE/`:

```
javac -d ../out/check/WindowsE src/windowse/*.java test/windowse/*.java
java -cp ../out/check/WindowsE windowse.WindowsECheck
```

## Running

From the repo root: `./run.ps1 old WindowsE` (or `./run.ps1 old WindowsE -Build` to build a jar).

DO IT!!! presses the picked shortcut on the real keyboard, Win+E ten times by default.

## In the browser

The web build runs the same code on a page with a pretend Windows (see [web/README.md](../web/README.md), Pretend Windows), which answers every key on the keyboard:

- Win+E opens a pretend File Explorer window on the site's app folders, cascading like the real ones. Click a folder to read what the app is, double-click it to play it; drag the windows by their title bar, close them with their x, or use Close all in the tray at the bottom left. At most 40 are open at once.
- The Windows key opens a Start menu holding every app on the site.
- Win+D hides the File Explorer windows (and the Start menu, Run box and task view), and pressed again brings them back; the tray's Show desktop button does the same.
- Win+R opens a Run box at the bottom left: type an app's name (or the start of it) and Enter plays it; `explorer` opens File Explorer.
- Win+Tab shows task view: every File Explorer window as a small copy of itself, to bring to the front (minimized ones too) or close.
- Side by side opens two File Explorer windows and snaps them to the two halves of the stage.
- Win+Left and Win+Right snap the window in front to a half of the stage (the other way puts it back), Win+Up maximizes it, Win+Down puts a snapped or maximized window back and minimizes one that is not.
