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
- **Your own keyboard**: typing plays the drawn keyboard. A letter key goes down on it while you hold it, and E, D or R picks that shortcut. The first digits typed after a pick replace the count it offered, so E, 5, Enter presses Win+E five times (Backspace or the arrows in the field edit the count instead). The field keeps only digits. Typing presses nothing on Windows; only DO IT!!! does.
- **How many times**: the field right after the picked shortcut's keys at the top, so it reads as one line, such as Win + E × 10 (10 when E is picked, 1 for the others). Enter in the field does the same as DO IT!!!. While DO IT!!! or Preview runs, the field gives way to the step being pressed and its count.
- **"DO IT!!!"**: the blue button at the bottom right, the only one that presses keys (its tooltip says what it will press). It presses the picked shortcut that many times, 100 ms apart (on a Swing timer, so the window stays responsive). Each press holds the Windows key only around its own key (Windows down, E down, E up, Windows up), so between presses the Windows key is up and anything typed meanwhile stays an ordinary key; the Windows key alone is tapped. While it runs, the Windows key and the other key go down on the drawn keyboard with each press, a count shows above it, and the button reads **Stop**, which ends it early. If the count is not a whole number of 1 or more, the title says so.
- **Steps**: the list's buttons sit together at the bottom left (Add step, Clear and Side by side, with Send beside them on the full keyboard). **Add step** puts the picked shortcut, with the field's count, on the storyboard above the keyboard (up to 8 steps, every one shown), and DO IT!!! then presses the whole list in order instead, lighting the step being pressed. Each step is a frame of its own: a tiny screen as it is after that step, from a clear desktop, with the step's keys under it without "Win+" (E x3, Left), so the list reads as a strip of what happens. Faint empty frames show the room left. Point at a frame (or tap it, or Tab to it and press Space) and the little monitor shows the same screen bigger, labelled "After step 2 of 4", so moving along the list plays it there; the **x** in its top right corner (a 24 px corner, or Delete on it) takes just that step off the list. The full keyboard has the 8 frames on one row, the pocket pad on two rows of 4. **Clear** empties it. **Side by side** lists Win+E, Win+Left, Win+E, Win+Right: two File Explorer windows, one on each half of the screen.
- **The little monitor**: beside the keyboard, a small screen shows what DO IT!!! will do, starting from a clear desktop: the File Explorer windows it opens (cascaded, snapped to a half, maximized, minimized or hidden by Win+D), the Start menu, the Run box or task view, and under it the same in words ("2 File Explorer windows: 1 on the left half, 1 on the right half."). It follows the list, or the picked key and the count as they are typed, and while DO IT!!! runs it plays each press as it is sent. It follows the site's pretend Windows (at most 40 File Explorer windows, as there), so on the desktop it is a guess: the windows already open, and the arrows moving this window when no File Explorer window was opened, are not on it (it says when the arrows found no window to move). The DO IT!!! button's screen reader name ends with the same words.
- **Preview**: plays the list (or the picked key) on the little monitor at DO IT!!!'s pace without pressing any key, the drawn keys going down as they would; DO IT!!! turns to Stop meanwhile. On the desktop this is the way to see what Win+E x40 or a list of snaps does before it happens to the real screen.
- **Send**: puts the list (or the picked key and its count) on the clipboard to send a friend. In the browser it is a link to the page with the list in its address, such as `?keys=Ex10-Left-E-Right` (each shortcut without "Win+", the Windows key alone `Win`, `x` and a count when it is pressed more than once); on the desktop it is the code alone, which a friend opens with `./run.ps1 old WindowsE Ex10-Left-E-Right`. A friend's link or code opens the window with that list on it and the title says a friend sent it; nothing is pressed until they press DO IT!!! (Preview shows it first). A code with an unknown key, more than 8 steps or a count over 999 is ignored. The title says when it was copied, or shows the text to send when the clipboard could not take it.
- The window is not resizable. Title "Windows+E". One optional argument: a list's code (see Send), or the page's address in the browser.
- **Pocket pad**: on a screen narrower than 880 px (a phone, in the browser) the whole keyboard would be shrunk to about a third of its size, so the window opens as a pocket pad instead: only the nine shortcut keys, three by three and bigger (the Windows key, E and R; Tab, Up and D; Left, Down and Right), the little monitor beside them, what the picked shortcut does on its own lines above, and the buttons on two rows (the list's buttons, then Send, Preview and DO IT!!!). It is about 380 px wide instead of about 860 and works the same way.

On the desktop these are the real shortcuts, so the window in front is usually Windows+E itself: Win+Left snaps it, Win+D hides it with everything else.

## Code layout

- `src/windowse/WindowsEMain.java`: entry point; shows a `WindowsEWindow` that presses keys with `RobotKeys`.
- `src/windowse/WindowsEWindow.java`: the keyboard, the field, the list of steps and the buttons.
- `src/windowse/ShortcutBoard.java`: draws the keyboard (the keys without a shortcut, dimmed), the picked shortcut, the list of steps and the count, and places a `KeyCap` on each shortcut's key.
- `src/windowse/KeyCap.java`: one shortcut's key, a button that draws itself as a keycap (picked, pushed down, hovered), with a screen reader name.
- `src/windowse/Shortcut.java`: the shortcuts: key code, legend, caption, name, what it does and the count it offers.
- `src/windowse/StepPill.java`: one step on the list, a button drawn as a storyboard frame (the screen after it, its keys, its x): pointing at it shows that screen on the little monitor, the x or Delete takes it off.
- `src/windowse/Step.java`: one step of the list: a shortcut and how many times, and the codes Send writes and reads.
- `src/windowse/Screen.java`: what the shortcuts do to a screen (the little monitor's model) and the words for it.
- `src/windowse/MiniScreen.java`: draws a `Screen` small.
- `src/windowse/WindowsEPresser.java`: presses a list of steps on a Swing timer, and stops early.
- `src/windowse/Keys.java`, `src/windowse/RobotKeys.java`: where the presses go; `RobotKeys` sends them with `java.awt.Robot`.

## Self-check

`test/windowse/WindowsECheck.java` presses the shortcuts into a key list instead of `java.awt.Robot`, so nothing reaches the real keyboard: Win+E three times lets go of Windows after each E, the Windows key alone is tapped, Win+Tab and Win+Left send their keys, Stop after two presses stops there, Side by side and a mixed list are pressed in order, the done callback runs once, and every shortcut is described. It also checks what the little monitor says lists do: Side by side, Win+E stopping at 40 windows, Win+D hiding and bringing back, the arrows' snaps, restores and minimizes, an arrow with no window, the Start menu and Run box, and a huge count keeping a toggle's parity. It also checks that both keyboards, the full one and the pocket pad, give every shortcut one key, on the board and clear of the others, and that the pocket pad is at most 400 px wide. Typing is checked too: E, D and R pick their keys and other keys none, the field keeps only digits, a digit after a pick replaces the offered count, and typing presses nothing. With a display, the step frames are checked on both keyboards: a list of four gets four frames, a click on a frame's label keeps the step, its x and Delete take that step off, a list of 8 gets 8 frames on the board and clear of each other, and none of it presses a key. Send's codes read back as the same list and bad codes are refused; with a display, it also opens both windows with a friend's list (pressing nothing) and checks what Send copies, into a list in place of the clipboard. From `WindowsE/`:

```
javac -d ../out/check/WindowsE src/windowse/*.java test/windowse/*.java
java -cp ../out/check/WindowsE windowse.WindowsECheck
```

## Running

From the repo root: `./run.ps1 old WindowsE` (or `./run.ps1 old WindowsE -Build` to build a jar).

DO IT!!! presses the picked shortcut on the real keyboard, Win+E ten times by default.

## In the browser

The web build runs the same code on a page with a pretend Windows (see [web/README.md](../web/README.md), Pretend Windows), which answers every key on the keyboard:

- A link from Send (`WindowsE/?keys=...`, or the home page or play.html with `?keys=`) opens the app with the friend's list on it, ready to press on the pretend Windows.
- Win+E opens a pretend File Explorer window on the site's app folders, cascading like the real ones. Click a folder to read what the app is, double-click it to play it; drag the windows by their title bar, close them with their x, or use Close all in the tray at the bottom left. At most 40 are open at once.
- The Windows key opens a Start menu holding every app on the site.
- Win+D hides the File Explorer windows (and the Start menu, Run box and task view), and pressed again brings them back; the tray's Show desktop button does the same.
- Win+R opens a Run box at the bottom left: type an app's name (or the start of it) and Enter plays it; `explorer` opens File Explorer.
- Win+Tab shows task view: every File Explorer window as a small copy of itself, to bring to the front (minimized ones too) or close.
- Side by side opens two File Explorer windows and snaps them to the two halves of the stage.
- Win+Left and Win+Right snap the window in front to a half of the stage (the other way puts it back), Win+Up maximizes it, Win+Down puts a snapped or maximized window back and minimizes one that is not.
